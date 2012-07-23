package com.bazaarvoice.soa;

import java.util.Map;

public interface LoadBalanceAlgorithm {
    /**
     * Selects an endpoint to use based on a load balancing algorithm.  If no endpoint can be chosen, then
     * <code>null</code> is returned.
     *
     * @param endpoints The endpoints to choose from.
     * @return Which endpoint to use or <code>null</code> if one couldn't be chosen.
     */
    ServiceEndPoint choose(Iterable<ServiceEndPoint> endpoints, Map<Enum, ServiceStatisticsProvider> stats);
}
