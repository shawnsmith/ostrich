package com.bazaarvoice.soa.pool;

import com.bazaarvoice.soa.RetryPolicy;
import com.bazaarvoice.soa.ServicePool;
import com.bazaarvoice.soa.dropwizard.pool.ManagedServicePoolProxy;
import com.yammer.dropwizard.lifecycle.Managed;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

// Not in the com.bazaarvoice.soa.dropwizard.pool package because it needs package-protected access to the
// ServicePoolProxy class to create valid dynamic service proxies.
public class ManagedServicePoolProxyTest {
    @Test(expected = IllegalArgumentException.class)
    public void testNull() {
        new ManagedServicePoolProxy(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotProxy() {
        new ManagedServicePoolProxy(mock(Service.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStart() throws Exception {
        ServicePool<Service> pool = mock(ServicePool.class);
        Service service = ServicePoolProxy.create(Service.class, mock(RetryPolicy.class), pool,
                mock(PartitionContextSupplier.class), true);
        Managed managed = new ManagedServicePoolProxy(service);

        managed.start();
        verifyZeroInteractions(pool);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStop() throws Exception {
        ServicePool<Service> pool = mock(ServicePool.class);
        Service service = ServicePoolProxy.create(Service.class, mock(RetryPolicy.class), pool,
                mock(PartitionContextSupplier.class), true);
        Managed managed = new ManagedServicePoolProxy(service);

        managed.stop();
        verify(pool).close();
        verifyNoMoreInteractions(pool);
    }

    // A dummy interface for testing...
    private static interface Service {}
}
