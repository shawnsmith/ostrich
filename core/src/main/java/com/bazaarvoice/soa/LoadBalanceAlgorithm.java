package com.bazaarvoice.soa;

public interface LoadBalanceAlgorithm {
    ServiceInstance choose(Iterable<ServiceInstance> instances);
}
