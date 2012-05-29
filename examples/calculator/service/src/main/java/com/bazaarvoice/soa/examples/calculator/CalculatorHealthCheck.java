package com.bazaarvoice.soa.examples.calculator;

import com.yammer.metrics.core.HealthCheck;

public class CalculatorHealthCheck extends HealthCheck {
    protected CalculatorHealthCheck() {
        super("calculator");
    }

    @Override
    protected Result check() throws Exception {
        if (CalculatorService.IS_HEALTHY) {
            return Result.healthy();
        } else {
            return Result.unhealthy("unhealthy by toggle");
        }
    }
}
