package com.bazaarvoice.soa;

/**
 * A container for multiple {@link HealthCheckResult}s with convenience methods to view only healthy or unhealthy
 * results. May also be empty, in which case all methods will return empty {@link Iterable}s.
 */
public interface AggregateHealthCheckResult {
    /**
     * @return All results in the aggregate, regardless of health.
     */
    Iterable<HealthCheckResult> getAllResults();

    /**
     * @return Results in the aggregate whose {@link HealthCheckResult#isHealthy} method returns {@code true}.
     */
    Iterable<HealthCheckResult> getHealthyResults();

    /**
     * @return Results in the aggregate whose {@link HealthCheckResult#isHealthy} method returns {@code false}.
     */
    Iterable<HealthCheckResult> getUnhealthyResults();
}
