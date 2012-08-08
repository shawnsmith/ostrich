package com.bazaarvoice.soa.healthcheck;

import com.bazaarvoice.soa.AggregateHealthCheckResult;
import com.bazaarvoice.soa.HealthCheckResult;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public final class SetAggregateHealthCheckResult implements AggregateHealthCheckResult {
    private final Set<HealthCheckResult> _healthyResults =
            Sets.newSetFromMap(Maps.<HealthCheckResult, Boolean>newConcurrentMap());
    private final Set<HealthCheckResult> _unhealthyResults =
            Sets.newSetFromMap(Maps.<HealthCheckResult, Boolean>newConcurrentMap());

    @Override
    public Iterable<HealthCheckResult> getAllResults() {
        return ImmutableSet.copyOf(Iterables.concat(_healthyResults, _unhealthyResults));
    }

    @Override
    public Iterable<HealthCheckResult> getHealthyResults() {
        return ImmutableSet.copyOf(_healthyResults);
    }

    @Override
    public Iterable<HealthCheckResult> getUnhealthyResults() {
        return ImmutableSet.copyOf(_unhealthyResults);
    }

    public void addHealthCheckResult(HealthCheckResult result) {
        checkNotNull(result);
        if (result.isHealthy()) {
            _healthyResults.add(result);
        } else {
            _unhealthyResults.add(result);
        }
    }
}