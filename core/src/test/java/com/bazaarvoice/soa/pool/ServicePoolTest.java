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
import com.google.common.base.Ticker;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
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

        // Don't leak service instances in real code!!!  This is just a test case.
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
    public void testAttemptsToRetryOnException() {
        RetryPolicy retry = neverRetry();

        ServicePool<Service> pool = newPool();
        try {
            pool.execute(retry, new ServiceCallback<Service, Void>() {
                @Override
                public Void call(Service service) {
                    throw new ServiceException();
                }
            });
        } catch (ServiceException expected) {
            // We expect a service exception to happen since we're not going to be allowed to retry at all.
            // Make sure we asked the retry strategy if it was okay to retry one time (it said no).
            verify(retry).allowRetry(eq(1), anyLong());
            return;
        }

        fail();
    }

    private ServicePool<Service> newPool() {
        Service service = mock(Service.class);
        return newPool(service);
    }

    @SuppressWarnings("unchecked")
    private <S extends Service> ServicePool<S> newPool(S service) {
        // TODO: If this were an interface we could mock it...
        ServiceInstance instance = new ServiceInstance("service", "server", 8080);

        // A host discovery implementation that only ever finds a single service instance -- the one backing the
        // service provided by our caller
        HostDiscovery discovery = mock(HostDiscovery.class);
        when(discovery.getHosts()).thenReturn(Collections.singleton(instance));

        // A load balance algorithm that always returns our specific instance
        LoadBalanceAlgorithm loadBalanceAlgorithm = mock(LoadBalanceAlgorithm.class);
        when(loadBalanceAlgorithm.choose(anyCollection())).thenReturn(instance);

        // A service factory that only knows how to create our service instance
        ServiceFactory<S> factory = (ServiceFactory<S>) mock(ServiceFactory.class);
        when(factory.getLoadBalanceAlgorithm()).thenReturn(loadBalanceAlgorithm);
        when(factory.create(instance)).thenReturn(service);

        return new ServicePoolBuilder<S>()
                .withHostDiscovery(discovery)
                .withServiceFactory(factory)
                .withTicker(_ticker)
                .build();
    }

    private RetryPolicy neverRetry() {
        RetryPolicy policy = mock(RetryPolicy.class);
        when(policy.allowRetry(anyInt(), anyLong())).thenReturn(false);
        return policy;
    }
}
