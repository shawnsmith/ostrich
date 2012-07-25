package com.bazaarvoice.soa.loadbalance;

import com.bazaarvoice.soa.ServiceEndPoint;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

public class FirstEndPointAlgorithmTest {
    private final FirstEndPointAlgorithm _balancer = new FirstEndPointAlgorithm();

    @Test
    public void testFirstEndPointReturned() {
        ServiceEndPoint first = mock(ServiceEndPoint.class);
        ServiceEndPoint second = mock(ServiceEndPoint.class);

        assertSame(first, _balancer.choose(ImmutableList.of(first, second)));
    }

    @Test
    public void testEmptyList() {
        assertNull(_balancer.choose(Collections.<ServiceEndPoint>emptyList()));
    }

    @Test(expected = NullPointerException.class)
    public void testNullIterable() {
        _balancer.choose(null);
    }
}
