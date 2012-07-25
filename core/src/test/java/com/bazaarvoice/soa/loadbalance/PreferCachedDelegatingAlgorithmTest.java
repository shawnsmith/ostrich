package com.bazaarvoice.soa.loadbalance;

import com.bazaarvoice.soa.LoadBalanceAlgorithm;
import com.bazaarvoice.soa.ServiceEndPoint;
import com.bazaarvoice.soa.ServicePoolStatistics;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import java.util.List;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PreferCachedDelegatingAlgorithmTest {
    private static final ServiceEndPoint CACHED_ENDPOINT = mock(ServiceEndPoint.class);
    private static final ServiceEndPoint NON_CACHED_ENDPOINT = mock(ServiceEndPoint.class);
    private static final List<ServiceEndPoint> END_POINTS = ImmutableList.of(CACHED_ENDPOINT, NON_CACHED_ENDPOINT);

    private final ServicePoolStatistics _stats = mock(ServicePoolStatistics.class);
    private final LoadBalanceAlgorithm _cachedAlgorithm = mock(LoadBalanceAlgorithm.class);
    private final LoadBalanceAlgorithm _nonCachedAlgorithm = mock(LoadBalanceAlgorithm.class);

    @Before
    public void setup() {
        when(_stats.getNumIdleCachedInstances(CACHED_ENDPOINT)).thenReturn(1);
        when(_stats.getNumIdleCachedInstances(NON_CACHED_ENDPOINT)).thenReturn(0);
        when(_cachedAlgorithm.choose(Matchers.<Iterable<ServiceEndPoint>>any())).thenReturn(CACHED_ENDPOINT);
        when(_nonCachedAlgorithm.choose(Matchers.<Iterable<ServiceEndPoint>>any())).thenReturn(NON_CACHED_ENDPOINT);
    }

    @Test(expected = NullPointerException.class)
    public void testNullCachedDelegate() {
        new PreferCachedDelegatingAlgorithm(null, _nonCachedAlgorithm, _stats);
    }

    @Test(expected = NullPointerException.class)
    public void testNullNonCachedDelegate() {
        new PreferCachedDelegatingAlgorithm(_cachedAlgorithm, null, _stats);
    }

    @Test(expected = NullPointerException.class)
    public void testNullStats() {
        new PreferCachedDelegatingAlgorithm(_cachedAlgorithm, _nonCachedAlgorithm, null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCacheDelegateCalledWhenCachedInstanceAvailable() {
        ArgumentCaptor<Iterable<ServiceEndPoint>> captor = (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);

        PreferCachedDelegatingAlgorithm balancer = new PreferCachedDelegatingAlgorithm(_cachedAlgorithm,
                _nonCachedAlgorithm, _stats);

        ServiceEndPoint endPoint = balancer.choose(END_POINTS);

        verify(_cachedAlgorithm).choose(captor.capture());
        assertTrue(Iterables.getOnlyElement(captor.getValue()) == CACHED_ENDPOINT);
        assertSame(CACHED_ENDPOINT, endPoint);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNonCacheDelegateCalledWhenNoCachedInstanceAvailable() {
        ArgumentCaptor<Iterable<ServiceEndPoint>> captor = (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);

        PreferCachedDelegatingAlgorithm balancer = new PreferCachedDelegatingAlgorithm(_cachedAlgorithm,
                _nonCachedAlgorithm, _stats);

        ServiceEndPoint endPoint = balancer.choose(ImmutableList.of(NON_CACHED_ENDPOINT));

        verify(_nonCachedAlgorithm).choose(captor.capture());
        assertTrue(Iterables.getOnlyElement(captor.getValue()) == NON_CACHED_ENDPOINT);
        assertSame(NON_CACHED_ENDPOINT, endPoint);
    }

    @Test
    public void testBothDelegatesCalledWhenCachedReturnsNull() {
        when(_cachedAlgorithm.choose(Matchers.<Iterable<ServiceEndPoint>>any())).thenReturn(null);
        when(_nonCachedAlgorithm.choose(Matchers.<Iterable<ServiceEndPoint>>any())).thenReturn(NON_CACHED_ENDPOINT);

        PreferCachedDelegatingAlgorithm balancer = new PreferCachedDelegatingAlgorithm(_cachedAlgorithm,
                _nonCachedAlgorithm, _stats);

        assertSame(NON_CACHED_ENDPOINT, balancer.choose(END_POINTS));
    }

    @Test(expected = NullPointerException.class)
    public void testNullIterable() {
        PreferCachedDelegatingAlgorithm balancer = new PreferCachedDelegatingAlgorithm(_cachedAlgorithm,
                _nonCachedAlgorithm, _stats);

        balancer.choose(null);
    }
}
