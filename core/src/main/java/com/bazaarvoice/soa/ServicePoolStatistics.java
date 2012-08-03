package com.bazaarvoice.soa;

/**
 * A provider of statistics relating to the state of the {@link ServicePool}. Useful for making decisions for load
 * balancing, a {@code ServicePool} will pass an instance to the {@link ServiceFactory} when requesting a
 * {@link LoadBalanceAlgorithm}. Also provides general purpose statistics.
 */
public interface ServicePoolStatistics {
    /**
     * The number of cached service instances not currently in use for a single end point.
     * @param endPoint The end point to get cache data for.
     * @return The number of idle service instances in the cache for the given end point.
     */
    int getNumIdleCachedInstances(ServiceEndPoint endPoint);

    /**
     * The number of service instances in the pool currently being used to execute callbacks for a single end point.
     * Note that this only represents that activity between a single service pool and the end point, and does not in any
     * way represent activity of other service pools for the same service, other applications connected to the service,
     * or global overall load for the service.
     * @param endPoint The end point to get activity data for.
     * @return The number of service instances actively serving callbacks for the given end point.
     */
    int getNumActiveInstances(ServiceEndPoint endPoint);

    /**
     * Check if the pool has at least one healthy service end point. Will perform a health check against an end point,
     * so execution time is dependant on the latency of the health check.
     * @return {@code true} if the {@link ServicePool} has an end point that passes a health check.
     */
    boolean hasHealthyEndPoint();
}
