package com.bazaarvoice.soa.healthcheck.dropwizard;

import com.bazaarvoice.soa.AggregateHealthCheckResult;
import com.bazaarvoice.soa.HealthCheckResult;
import com.bazaarvoice.soa.ServicePool;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.yammer.metrics.core.HealthCheck;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A simple health check that verifies a pool has a healthy end point. Will perform a health check on at least one end
 * point, so beware the possibility of overloading your services with health checks if this is run excessively.
 */
public class ContainsHealthyEndPointCheck extends HealthCheck {
    private ServicePool<?> _pool;

    /**
     * Constructs a health check for the given pool that will show as healthy if it has at least one healthy end point.
     * @param pool The {@code ServicePool} to look for healthy end points in.
     * @param serviceName The name of the service. May not be empty.
     */
    public ContainsHealthyEndPointCheck(ServicePool<?> pool, String serviceName) {
        super(serviceName + "-service-pool-contains-healthy-end-point");
        checkArgument(!Strings.isNullOrEmpty(serviceName));
        _pool = checkNotNull(pool);
    }

    @Override
    public Result check() throws Exception {
        AggregateHealthCheckResult aggregate = _pool.findFirstHealthyEndPoint();
        HealthCheckResult healthyResult = Iterables.getFirst(aggregate.getHealthyResults(), null);
        boolean healthy = healthyResult != null;

        // Get stats about any failed health checks
        int numUnhealthy = 0;
        long totalUnhealthyResponseTimeInMicros = 0;
        for (HealthCheckResult unhealthy : aggregate.getUnhealthyResults()) {
            ++numUnhealthy;
            totalUnhealthyResponseTimeInMicros += unhealthy.getResponseTime(TimeUnit.MICROSECONDS);
        }

        if (!healthy && numUnhealthy == 0) {
            return Result.unhealthy("No end points.");
        }

        String unhealthyMessage = numUnhealthy + " failures in " + totalUnhealthyResponseTimeInMicros + "µs";
        if (!healthy) {
            return Result.unhealthy(unhealthyMessage);
        }
        return Result.healthy(healthyResult.getEndPointId() + " succeeded in " +
                healthyResult.getResponseTime(TimeUnit.MICROSECONDS) + "µs; " + unhealthyMessage);
    }
}
