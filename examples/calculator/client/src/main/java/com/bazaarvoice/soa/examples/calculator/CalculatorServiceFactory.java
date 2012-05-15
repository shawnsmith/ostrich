package com.bazaarvoice.soa.examples.calculator;

import com.bazaarvoice.soa.LoadBalanceAlgorithm;
import com.bazaarvoice.soa.ServiceFactory;
import com.bazaarvoice.soa.ServiceInstance;
import com.bazaarvoice.soa.loadbalance.RandomAlgorithm;

public class CalculatorServiceFactory implements ServiceFactory<CalculatorService> {
    @Override
    public String getServiceName() {
        return "calculator";
    }

    @Override
    public LoadBalanceAlgorithm getLoadBalanceAlgorithm() {
        return new RandomAlgorithm();
    }

    @Override
    public CalculatorService create(ServiceInstance instance) {
        return new CalculatorClient(instance);
    }
}
