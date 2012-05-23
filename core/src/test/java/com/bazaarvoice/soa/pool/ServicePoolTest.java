package com.bazaarvoice.soa.pool;

import com.bazaarvoice.soa.HostDiscovery;
import com.bazaarvoice.soa.LoadBalanceAlgorithm;
import com.bazaarvoice.soa.RetryPolicy;
import com.bazaarvoice.soa.Service;
import com.bazaarvoice.soa.ServiceCallback;
import com.bazaarvoice.soa.ServiceEndpoint;
import com.bazaarvoice.soa.ServiceException;
import com.bazaarvoice.soa.ServiceFactory;
import com.bazaarvoice.soa.ServicePool;
import com.google.common.base.Ticker;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ServicePoolTest {
    private Ticker _ticker;

    @Before
    public void setup() {
        _ticker = mock(Ticker.class);
    }

    @Test
    public void testCallInvokedWithCorrectService() {
        final Service expectedService = mock(Service.class);

        // Don't leak service endpoints in real code!!!  This is just a test case.
        Service actualService = newPool(expectedService).execute(neverRetry(), new ServiceCallback<Service, Service>() {
            @Override
            public Service call(Service s) {
                return s;
            }
        });

        assertSame(expectedService, actualService);
    }

    @Test
    public void testDoesNotRetryOnCallbackSuccess() {
        RetryPolicy retry = neverRetry();
        newPool().execute(retry, new ServiceCallback<Service, Void>() {
            @Override
            public Void call(Service service) {
                return null;
            }
        });

        // Should have never called the retry strategy for anything.  This might change in the future if we implement
        // circuit breakers.
        verifyZeroInteractions(retry);
    }

    @Test
    public void testAttemptsToRetryOnServiceException() {
        RetryPolicy retry = neverRetry();
        ServicePool<Service> pool = newPool();
        try {
            pool.execute(retry, new ServiceCallback<Service, Void>() {
                @Override
                public Void call(Service service) {
                    throw new ServiceException();
                }
            });

            fail();
        } catch (ServiceException expected) {
            // We expect a service exception to happen since we're not going to be allowed to retry at all.
            // Make sure we asked the retry strategy if it was okay to retry one time (it said no).
            verify(retry).allowRetry(eq(1), anyLong());
        }
    }

    @Test
    public void testDoesNotAttemptToRetryOnNonServiceException() {
        RetryPolicy retry = neverRetry();
        ServicePool<Service> pool = newPool();
        try {
            pool.execute(retry, new ServiceCallback<Service, Object>() {
                @Override
                public Object call(Service service) throws ServiceException {
                    throw new NullPointerException();
                }
            });

            fail();
        } catch (NullPointerException expected) {
            verifyZeroInteractions(retry);
        }
    }

    @Test
    public void testKeepsRetryingUntilRetryPolicyReturnsFalse() {
        RetryPolicy retry = mock(RetryPolicy.class);
        when(retry.allowRetry(anyInt(), anyLong())).thenReturn(true, true, false);

        ServicePool<Service> pool = newPool(3);
        try {
            pool.execute(retry, new ServiceCallback<Service, Object>() {
                @Override
                public Object call(Service service) throws ServiceException {
                    throw new ServiceException();
                }
            });

            fail();
        } catch (ServiceException expected) {
            // Make sure we tried 3 times.
            verify(retry).allowRetry(eq(3), anyLong());
        }
    }

    @Test
    public void testRetriesWithDifferentServiceEndpoints() {
        RetryPolicy retry = mock(RetryPolicy.class);
        when(retry.allowRetry(anyInt(), anyLong())).thenReturn(true, true, false);

        // We don't have a great way to check which ServiceEndpoints are being used, but we know that a new service
        // is created for each one.  So let's return the services shown to the callback, and make sure they're all
        // different.
        final Set<Service> seenServices = Sets.newHashSet();

        ServicePool<Service> pool = newPool(3);
        try {
            pool.execute(retry, new ServiceCallback<Service, Set<Service>>() {
                @Override
                public Set<Service> call(Service service) throws ServiceException {
                    seenServices.add(service);
                    throw new ServiceException();
                }
            });

            fail();
        } catch (ServiceException expected) {
            // Make sure we saw 3 different service instances.
            assertEquals(3, seenServices.size());
        }
    }

    @Test
    public void testSubmitsHealthCheckOnServiceException() {
        ScheduledExecutorService healthCheckExecutor = mock(ScheduledExecutorService.class);

        ServicePool<Service> pool = newPool(healthCheckExecutor);
        try {
            pool.execute(neverRetry(), new ServiceCallback<Service, Object>() {
                @Override
                public Object call(Service service) throws ServiceException {
                    throw new ServiceException();
                }
            });

            fail();
        } catch (ServiceException expected) {
            // Expected exception
        }

        // Make sure we added a health check.
        verify(healthCheckExecutor).submit(any(com.bazaarvoice.soa.pool.ServicePool.HealthCheck.class));
    }

    @Test
    public void testDoesNotSubmitHealthCheckOnNonServiceException() {
        ScheduledExecutorService healthCheckExecutor = mock(ScheduledExecutorService.class);
        ServicePool<Service> pool = newPool(healthCheckExecutor);
        try {
            pool.execute(neverRetry(), new ServiceCallback<Service, Object>() {
                @Override
                public Object call(Service service) throws ServiceException {
                    throw new NullPointerException();
                }
            });

            fail();
        } catch (NullPointerException expected) {
            // Expected exception
        }

        // Make sure we didn't add a health check.
        verify(healthCheckExecutor, never()).submit(any(com.bazaarvoice.soa.pool.ServicePool.HealthCheck.class));
    }

    @Test
    public void testSchedulesHealthCheckUponCreation() {
        ScheduledExecutorService healthCheckExecutor = mock(ScheduledExecutorService.class);
        newPool(healthCheckExecutor);

        verify(healthCheckExecutor).scheduleAtFixedRate(
                any(com.bazaarvoice.soa.pool.ServicePool.BatchHealthChecks.class),
                eq(com.bazaarvoice.soa.pool.ServicePool.HEALTH_CHECK_POLL_INTERVAL_IN_SECONDS),
                eq(com.bazaarvoice.soa.pool.ServicePool.HEALTH_CHECK_POLL_INTERVAL_IN_SECONDS),
                eq(TimeUnit.SECONDS));
    }

    // TODO: I'm not very happy with the various newPool methods here, maybe a builder class would be better.

    private ServicePool<Service> newPool() {
        return newPool(1);
    }

    private ServicePool<Service> newPool(int n) {
        Service[] services = new Service[n];
        for (int i = 0; i < n; i++) {
            services[i] = mock(Service.class);
        }
        return newPool(services);
    }

    @SuppressWarnings("unchecked")
    private <S extends Service> ServicePool<S> newPool(S... services) {
        // A do-nothing executor service for dealing with health checks
        ScheduledExecutorService healthCheckExecutor = mock(ScheduledExecutorService.class);

        return newPool(healthCheckExecutor, services);
    }

    private ServicePool<Service> newPool(ScheduledExecutorService healthCheckExecutor) {
        Service service = mock(Service.class);
        return newPool(healthCheckExecutor, service);
    }

    @SuppressWarnings("unchecked")
    private <S extends Service> ServicePool<S> newPool(ScheduledExecutorService healthCheckExecutor, S... services) {
        ServiceEndpoint[] endpoints = new ServiceEndpoint[services.length];
        for (int i = 0; i < services.length; i++) {
            // TODO: If this were an interface we could mock it...
            endpoints[i] = new ServiceEndpoint("service", "server", 8080 + i);
        }

        // A host discovery implementation that only ever finds the endpoints corresponding to the passed in services
        HostDiscovery discovery = mock(HostDiscovery.class);
        when(discovery.getHosts()).thenReturn(Arrays.asList(endpoints));

        // A load balance algorithm that always returns our specific endpoint
        LoadBalanceAlgorithm loadBalanceAlgorithm = mock(LoadBalanceAlgorithm.class);
        when(loadBalanceAlgorithm.choose(anyCollection())).thenReturn(first(endpoints), rest(endpoints));

        // A service factory that only knows how to create our service endpoints
        ServiceFactory<S> factory = (ServiceFactory<S>) mock(ServiceFactory.class);
        when(factory.getLoadBalanceAlgorithm()).thenReturn(loadBalanceAlgorithm);
        for (int i = 0; i < services.length; i++) {
            when(factory.create(endpoints[i])).thenReturn(services[i]);
        }

        return new ServicePoolBuilder<S>()
                .withHostDiscovery(discovery)
                .withServiceFactory(factory)
                .withHealthCheckExecutor(healthCheckExecutor)
                .withTicker(_ticker)
                .build();
    }

    private RetryPolicy neverRetry() {
        RetryPolicy policy = mock(RetryPolicy.class);
        when(policy.allowRetry(anyInt(), anyLong())).thenReturn(false);
        return policy;
    }

    private <T> T first(T[] array) {
        return array[0];
    }

    private <T> T[] rest(T[] array) {
        return Arrays.copyOfRange(array, 1, array.length);
    }
}
