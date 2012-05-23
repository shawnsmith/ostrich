package com.bazaarvoice.soa.pool;

import com.bazaarvoice.soa.HostDiscovery;
import com.bazaarvoice.soa.LoadBalanceAlgorithm;
import com.bazaarvoice.soa.RetryPolicy;
import com.bazaarvoice.soa.Service;
import com.bazaarvoice.soa.ServiceCallback;
import com.bazaarvoice.soa.ServiceEndpoint;
import com.bazaarvoice.soa.ServiceException;
import com.bazaarvoice.soa.ServiceFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.base.Ticker;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

class ServicePool<S extends Service> implements com.bazaarvoice.soa.ServicePool<S> {
    // By default check every minute to see if an previously unhealthy endpoint has become healthy.
    @VisibleForTesting
    static final long HEALTH_CHECK_POLL_INTERVAL_IN_SECONDS = 60;

    private final Ticker _ticker;
    private final HostDiscovery _hostDiscovery;
    private final ServiceFactory<S> _serviceFactory;
    private final ScheduledExecutorService _healthCheckExecutor;
    private final LoadBalanceAlgorithm _loadBalanceAlgorithm;
    private final Set<ServiceEndpoint> _badEndpoints;
    private final Predicate<ServiceEndpoint> _badEndpointFilter;

    public ServicePool(Ticker ticker, HostDiscovery hostDiscovery, ServiceFactory<S> serviceFactory,
                       ScheduledExecutorService healthCheckExecutor) {
        _ticker = checkNotNull(ticker);
        _hostDiscovery = checkNotNull(hostDiscovery);
        _serviceFactory = checkNotNull(serviceFactory);
        _healthCheckExecutor = checkNotNull(healthCheckExecutor);
        _loadBalanceAlgorithm = checkNotNull(_serviceFactory.getLoadBalanceAlgorithm());
        _badEndpoints = Sets.newSetFromMap(Maps.<ServiceEndpoint, Boolean>newConcurrentMap());
        _badEndpointFilter = Predicates.not(Predicates.in(_badEndpoints));

        // Watch endpoints as they are removed from host discovery so that we can remove them from our set of bad
        // endpoints as well.  This will prevent the badEndpoints set from growing in an unbounded fashion.  There is a
        // minor race condition that could happen here, but it's not anything to be concerned about.  The HostDiscovery
        // component could lose its connection to its backing data store and then immediately regain it right
        // afterwards.  If that happens it could remove all of its endpoints only to re-add them right back again and we
        // will "forget" that an endpoint was bad and try to use it again.  This isn't fatal though because we'll just
        // rediscover that it's a bad endpoint again in the future.  Also in the future it might be useful to measure
        // how long an endpoint has been considered bad and potentially take action for endpoints that are bad for long
        // periods of time.
        _hostDiscovery.addListener(new HostDiscovery.EndpointListener() {
            @Override
            public void onEndpointAdded(ServiceEndpoint endpoint) {
                // If we wanted to assume that all new endpoints were bad until verified, we could add them to the
                // bad endpoints set here and schedule an immediate health check for them.  That way we wouldn't ever
                // use an endpoint until it was known to be good.
            }

            @Override
            public void onEndpointRemoved(ServiceEndpoint endpoint) {
                _badEndpoints.remove(endpoint);
            }
        });

        // Periodically wake up and check any badEndpoints to see if they're now healthy.
        _healthCheckExecutor.scheduleAtFixedRate(new BatchHealthChecks(),
                HEALTH_CHECK_POLL_INTERVAL_IN_SECONDS, HEALTH_CHECK_POLL_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public <R> R execute(RetryPolicy retry, ServiceCallback<S, R> callback) {
        Stopwatch sw = new Stopwatch(_ticker).start();
        int numAttempts = 0;
        do {
            Iterable<ServiceEndpoint> hosts = Iterables.filter(_hostDiscovery.getHosts(), _badEndpointFilter);
            if (Iterables.isEmpty(hosts)) {
                // There were no viable service endpoints available, we have no choice but to stop trying and just exit.
                break;
            }

            ServiceEndpoint endpoint = _loadBalanceAlgorithm.choose(hosts);
            S service = _serviceFactory.create(endpoint);
            try {
                return callback.call(service);
            } catch (ServiceException e) {
                // This is a known and supported exception indicating that something went wrong somewhere in the service
                // layer while trying to communicate with the endpoint.  These errors are often transient, so we enqueue
                // a health check for the endpoint and mark it as unavailable for the time being.
                _badEndpoints.add(endpoint);
                _healthCheckExecutor.submit(new HealthCheck(endpoint));
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        } while (retry.allowRetry(++numAttempts, sw.elapsedMillis()));

        throw new ServiceException();
    }

    @Override
    public <R> Future<R> executeAsync(RetryPolicy retry, ServiceCallback<S, R> callback) {
        throw new UnsupportedOperationException();
    }

    @VisibleForTesting
    final class HealthCheck implements Runnable {
        private final ServiceEndpoint _endpoint;

        public HealthCheck(ServiceEndpoint endpoint) {
            _endpoint = endpoint;
        }

        @Override
        public void run() {
            if (_serviceFactory.isHealthy(_endpoint)) {
                _badEndpoints.remove(_endpoint);
            }
        }
    }

    @VisibleForTesting
    final class BatchHealthChecks implements Runnable {
        @Override
        public void run() {
            for (ServiceEndpoint endpoint : _badEndpoints) {
                if (_serviceFactory.isHealthy(endpoint)) {
                    _badEndpoints.remove(endpoint);
                }
            }
        }
    }
}
