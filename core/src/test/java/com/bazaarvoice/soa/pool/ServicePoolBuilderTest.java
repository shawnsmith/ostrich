package com.bazaarvoice.soa.pool;

import com.bazaarvoice.soa.HostDiscovery;
import com.bazaarvoice.soa.LoadBalanceAlgorithm;
import com.bazaarvoice.soa.Service;
import com.bazaarvoice.soa.ServiceFactory;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServicePoolBuilderTest {
    @Test(expected = NullPointerException.class)
    public void testNullHostDiscovery() {
        new ServicePoolBuilder<Service>().withHostDiscovery(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullServiceFactory() {
        new ServicePoolBuilder<Service>().withServiceFactory(null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void testBuildWithNoHostDiscovery() {
        ServiceFactory<Service> serviceFactory = (ServiceFactory<Service>) mock(ServiceFactory.class);
        new ServicePoolBuilder<Service>()
                .withServiceFactory(serviceFactory)
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void testBuildWithNoServiceFactory() {
        HostDiscovery hostDiscovery = mock(HostDiscovery.class);
        new ServicePoolBuilder<Service>()
                .withHostDiscovery(hostDiscovery)
                .build();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void testBuildWithNullLoadBalanceAlgorithm() {
        HostDiscovery hostDiscovery = mock(HostDiscovery.class);
        ServiceFactory<Service> serviceFactory = (ServiceFactory<Service>) mock(ServiceFactory.class);

        new ServicePoolBuilder<Service>()
                .withServiceFactory(serviceFactory)
                .withHostDiscovery(hostDiscovery)
                .build();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBuildWithNoTicker() {
        LoadBalanceAlgorithm loadBalanceAlgorithm = mock(LoadBalanceAlgorithm.class);
        HostDiscovery hostDiscovery = mock(HostDiscovery.class);

        ServiceFactory<Service> serviceFactory = (ServiceFactory<Service>) mock(ServiceFactory.class);
        when(serviceFactory.getLoadBalanceAlgorithm()).thenReturn(loadBalanceAlgorithm);

        new ServicePoolBuilder<Service>()
                .withServiceFactory(serviceFactory)
                .withHostDiscovery(hostDiscovery)
                .build();
    }
}
