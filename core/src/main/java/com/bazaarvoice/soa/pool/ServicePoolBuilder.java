package com.bazaarvoice.soa.pool;

import com.bazaarvoice.soa.HostDiscovery;
import com.bazaarvoice.soa.LoadBalanceAlgorithm;
import com.bazaarvoice.soa.RetryPolicy;
import com.bazaarvoice.soa.Service;
import com.bazaarvoice.soa.ServiceCallback;
import com.bazaarvoice.soa.ServiceException;
import com.bazaarvoice.soa.ServiceFactory;
import com.bazaarvoice.soa.ServiceInstance;
import com.bazaarvoice.soa.ServicePool;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.base.Ticker;
import com.google.common.util.concurrent.ListenableFuture;

import static com.google.common.base.Preconditions.checkNotNull;

public class ServicePoolBuilder<S extends Service> {
    private Ticker _ticker = Ticker.systemTicker();
    private HostDiscovery _hostDiscovery;
    private ServiceFactory<S> _serviceFactory;

    public ServicePoolBuilder<S> withTicker(Ticker ticker) {
        _ticker = checkNotNull(ticker);
        return this;
    }

    public ServicePoolBuilder<S> withHostDiscovery(HostDiscovery hostDiscovery) {
        _hostDiscovery = checkNotNull(hostDiscovery);
        return this;
    }

    public ServicePoolBuilder<S> withServiceFactory(ServiceFactory<S> serviceFactory) {
        _serviceFactory = checkNotNull(serviceFactory);
        return this;
    }

    public ServicePool<S> build() {
        checkNotNull(_hostDiscovery);
        checkNotNull(_serviceFactory);

        return new ServicePool<S>() {
            LoadBalanceAlgorithm _loadBalanceAlgorithm = checkNotNull(_serviceFactory.getLoadBalanceAlgorithm());

            // TODO: It feels like this implementation has too many concerns going on.  Specifically the flow of
            // TODO: ServiceInstance objects moving from HostDiscovery to the LoadBalanceAlgorithm to the
            // TODO: ServiceFactory.  Perhaps these can all be combined in some way?

            @Override
            public <R> R execute(RetryPolicy retry, ServiceCallback<S, R> callback) {
                Stopwatch sw = new Stopwatch(_ticker);
                sw.start();

                int numAttempts = 0;
                do {
                    Iterable<ServiceInstance> hosts = _hostDiscovery.getHosts();
                    ServiceInstance instance = _loadBalanceAlgorithm.choose(hosts);
                    S service = _serviceFactory.create(instance);

                    try {
                        return callback.call(service);
                    } catch (ServiceException e) {
                        // This is a known and supported exception.  Retry.
                    } catch (Exception e) {
                        throw Throwables.propagate(e);
                    }
                } while (retry.allowRetry(++numAttempts, sw.elapsedMillis()));

                throw new ServiceException();
            }

            @Override
            public <R> ListenableFuture<R> executeAsync(RetryPolicy retry, ServiceCallback<S, R> callback) {
                return null;
            }
        };
    }
}