package com.bazaarvoice.soa.healthcheck;

import com.bazaarvoice.soa.HealthCheckResult;
import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SetAggregateHealthCheckResultTest {
    private static final HealthCheckResult HEALTHY = mock(HealthCheckResult.class);
    private static final HealthCheckResult UNHEALTHY = mock(HealthCheckResult.class);

    private final SetAggregateHealthCheckResult aggregate = new SetAggregateHealthCheckResult();

    @Before
    public void setup() {
        when(HEALTHY.isHealthy()).thenReturn(true);
        when(UNHEALTHY.isHealthy()).thenReturn(false);
    }

    @Test
    public void testAllResultsStartEmpty() {
        assertTrue(Iterables.isEmpty(aggregate.getAllResults()));
    }

    @Test
    public void testAllResultsContainsHealthy() {
        aggregate.addHealthCheckResult(HEALTHY);

        assertTrue(Iterables.contains(aggregate.getAllResults(), HEALTHY));
    }

    @Test
    public void testAllResultsContainsUnhealthy() {
        aggregate.addHealthCheckResult(UNHEALTHY);

        assertTrue(Iterables.contains(aggregate.getAllResults(), UNHEALTHY));
    }

    @Test
    public void testHealthyResultsStartEmpty() {
        assertTrue(Iterables.isEmpty(aggregate.getHealthyResults()));
    }

    @Test
    public void testHealthyResultsContainHealthy() {
        aggregate.addHealthCheckResult(HEALTHY);

        assertTrue(Iterables.contains(aggregate.getHealthyResults(), HEALTHY));
    }

    @Test
    public void testHealthyResultsDoesNotContainUnhealthy() {
        aggregate.addHealthCheckResult(UNHEALTHY);

        assertFalse(Iterables.contains(aggregate.getHealthyResults(), UNHEALTHY));
    }

    @Test
    public void testUnhealthyResultsStartEmpty() {
        assertTrue(Iterables.isEmpty(aggregate.getUnhealthyResults()));
    }

    @Test
    public void testUnhealthyResultsContainUnhealthy() {
        aggregate.addHealthCheckResult(UNHEALTHY);

        assertTrue(Iterables.contains(aggregate.getUnhealthyResults(), UNHEALTHY));
    }

    @Test
    public void testUnhealthyResultsDoesNotContainHealthy() {
        aggregate.addHealthCheckResult(HEALTHY);

        assertFalse(Iterables.contains(aggregate.getUnhealthyResults(), HEALTHY));
    }

    @Test(expected = NullPointerException.class)
    public void testNullResult() {
        SetAggregateHealthCheckResult aggregate = new SetAggregateHealthCheckResult();

        aggregate.addHealthCheckResult(null);
    }
}
