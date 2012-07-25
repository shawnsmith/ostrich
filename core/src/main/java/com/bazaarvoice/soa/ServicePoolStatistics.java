package com.bazaarvoice.soa;

public interface ServicePoolStatistics {

    int numIdleCachedInstances(ServiceEndPoint endPoint);

    int numActiveInstances(ServiceEndPoint endPoint);
}
