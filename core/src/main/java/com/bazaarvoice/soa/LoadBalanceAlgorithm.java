package com.bazaarvoice.soa;

public interface LoadBalanceAlgorithm {
    ServiceEndpoint choose(Iterable<ServiceEndpoint> endpoints);
}
