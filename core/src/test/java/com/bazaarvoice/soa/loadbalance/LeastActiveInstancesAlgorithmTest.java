package com.bazaarvoice.soa.loadbalance;

import com.bazaarvoice.soa.ServiceEndPoint;
import com.bazaarvoice.soa.ServicePoolStatistics;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LeastActiveInstancesAlgorithmTest {
    private final ServicePoolStatistics _stats = mock(ServicePoolStatistics.class);

    @Test(expected = NullPointerException.class)
    public void testConstructWithNullStats() {
        new LeastActiveInstancesAlgorithm(null);
    }

    @Test
    public void testLeastActiveChosen() {
        ServiceEndPoint fooEndpoint = mock(ServiceEndPoint.class);
        ServiceEndPoint barEndpoint = mock(ServiceEndPoint.class);
        ServiceEndPoint bazEndpoint = mock(ServiceEndPoint.class);

        when(_stats.numActiveInstances(fooEndpoint)).thenReturn(3);
        when(_stats.numActiveInstances(barEndpoint)).thenReturn(2);
        when(_stats.numActiveInstances(bazEndpoint)).thenReturn(1);

        List<ServiceEndPoint> endPointList = ImmutableList.of(fooEndpoint, barEndpoint, bazEndpoint);

        LeastActiveInstancesAlgorithm loadBalancer = new LeastActiveInstancesAlgorithm(_stats);
        ServiceEndPoint endPoint = loadBalancer.choose(endPointList);

        assertSame(bazEndpoint, endPoint);
    }

    @Test
    public void testEmptyIterable() {
        LeastActiveInstancesAlgorithm loadBalancer = new LeastActiveInstancesAlgorithm(_stats);
        ServiceEndPoint endPoint = loadBalancer.choose(Collections.<ServiceEndPoint>emptyList());

        assertNull(endPoint);
    }

    @Test(expected = NullPointerException.class)
    public void testNullIterable() {
        LeastActiveInstancesAlgorithm loadBalancer = new LeastActiveInstancesAlgorithm(_stats);
        loadBalancer.choose(null);
    }
}