package com.bazaarvoice.soa.loadbalance;

import com.bazaarvoice.soa.DefaultServiceStatisticsProviders;
import com.bazaarvoice.soa.ServiceEndPoint;
import com.bazaarvoice.soa.ServiceStatisticsProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LeastActiveConnectionsAlgorithmTest {
    private final ServiceEndPoint FOO_ENDPOINT = mock(ServiceEndPoint.class);
    private final ServiceEndPoint BAR_ENDPOINT = mock(ServiceEndPoint.class);
    private final ServiceEndPoint BAZ_ENDPOINT = mock(ServiceEndPoint.class);
    private final List<ServiceEndPoint> _endPointList = ImmutableList.of(FOO_ENDPOINT, BAR_ENDPOINT, BAZ_ENDPOINT);

    @Test
    public void testLeastActiveChosen() {
        final ServiceStatisticsProvider<Integer> statsProvider = mock(ServiceStatisticsProvider.class);
        when(statsProvider.serviceStats(FOO_ENDPOINT)).thenReturn(3);
        when(statsProvider.serviceStats(BAR_ENDPOINT)).thenReturn(2);
        when(statsProvider.serviceStats(BAZ_ENDPOINT)).thenReturn(1);
        final Map<Enum, ServiceStatisticsProvider> statsMap = ImmutableMap.<Enum, ServiceStatisticsProvider>of(DefaultServiceStatisticsProviders.NUM_ACTIVE_CONNECTIONS, statsProvider);

        final LeastActiveConnectionsAlgorithm loadBalancer = new LeastActiveConnectionsAlgorithm();
        final ServiceEndPoint endPoint = loadBalancer.choose(_endPointList, statsMap);

        assertSame(endPoint, BAZ_ENDPOINT);
    }

    @Test
    public void testEmptyListReturnsNull() {
        final ServiceStatisticsProvider<Integer> statsProvider = mock(ServiceStatisticsProvider.class);
        final Map<Enum, ServiceStatisticsProvider> statsMap = ImmutableMap.<Enum, ServiceStatisticsProvider>of(DefaultServiceStatisticsProviders.NUM_ACTIVE_CONNECTIONS, statsProvider);

        final LeastActiveConnectionsAlgorithm loadBalancer = new LeastActiveConnectionsAlgorithm();
        final ServiceEndPoint endPoint = loadBalancer.choose(Collections.<ServiceEndPoint>emptyList(), statsMap);

        assertNull(endPoint);
    }

}