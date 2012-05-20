package com.bazaarvoice.soa.examples.calculator;

import com.bazaarvoice.soa.LoadBalanceAlgorithm;
import com.bazaarvoice.soa.ServiceEndpoint;
import com.bazaarvoice.soa.ServiceFactory;
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
    public CalculatorService create(ServiceEndpoint endpoint) {
        return new CalculatorClient(endpoint);
    }

    @Override
    public boolean isHealthy(ServiceEndpoint endpoint) {
        // CalculatorService has the convention that the admin port is always one greater than the service port,
        // ideally this information would be conveyed in the payload of the service endpoint.
        int adminPort = endpoint.getPort() + 1;
        Http http = new Http("http://" + endpoint.getHostname() + ":" + adminPort);

        return http.HEAD("/healthcheck") == 200;
    }
}
