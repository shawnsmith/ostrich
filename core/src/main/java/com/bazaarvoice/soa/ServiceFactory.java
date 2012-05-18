package com.bazaarvoice.soa;

public interface ServiceFactory<S extends Service> {
    // TODO: getServiceName and getLoadBalanceAlgorithm don't feel right here.
    String getServiceName();
    LoadBalanceAlgorithm getLoadBalanceAlgorithm();

    S create(ServiceEndpoint endpoint);
    boolean isHealthy(ServiceEndpoint endpoint);
}