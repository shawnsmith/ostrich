package com.bazaarvoice.soa;

import java.util.concurrent.TimeUnit;

/**
 * The result of a health check on an end point. In some cases it makes sense to have a {@code HealthCheckResult} when
 * there are no end points available, in which case {@link #isHealthy} should return {@code false} and
 * {@link #getEndPointId} should return an empty string.
 */
public interface HealthCheckResult {
    /**
     * @return {@code true} if result is from a healthy end point, {@code false} otherwise.
     */
    boolean isHealthy();

    /**
     * Gets the ID of the end point the health check was run on. Sometimes it is desirable to have a
     * {@code HealthCheckResult} even if there are no end points to run a health check against. In this case, the empty
     * string will be returned.
     * @return The ID of the end point this result is for, or the empty string if no end point.
     */
    String getEndPointId();

    /**
     * Gets the amount of time the health check took to run or until it failed.
     * @param unit The {@code TimeUnit} the response time should be in.
     * @return The execution time of the health check in the units specified.
     */
    long getResponseTime(TimeUnit unit);
}
