package com.bazaarvoice.soa.healthcheck;

import com.bazaarvoice.soa.HealthCheckResults;
import com.bazaarvoice.soa.HealthCheckResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class DefaultHealthCheckResults implements HealthCheckResults {
    private final List<HealthCheckResult> _healthyResults = Lists.newArrayList();
    private final List<HealthCheckResult> _unhealthyResults = Lists.newArrayList();

    @Override
    public boolean hasHealthyResult() {
        return !_healthyResults.isEmpty();
    }

    @Override
    public Iterable<HealthCheckResult> getAllResults() {
        return ImmutableList.copyOf(Iterables.concat(_healthyResults, _unhealthyResults));
    }

    @Override
    public HealthCheckResult getHealthyResult() {
        return hasHealthyResult() ? _healthyResults.get(0) : null;
    }

    @Override
    public Iterable<HealthCheckResult> getUnhealthyResults() {
        return ImmutableList.copyOf(_unhealthyResults);
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