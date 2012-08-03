package com.bazaarvoice.soa;

/**
 * A provider of {@link ServicePoolStatistics}. All {@link ServicePool}s should provide statistics about themselves, and
 * service pool proxies (such as those provided by {@link com.bazaarvoice.soa.pool.ServicePoolBuilder#buildProxy})
 * should provide statistics about their backing pools.
 */
public interface ServicePoolStatisticsProvider {
    /**
     * Retrieve statistics about a service pool. The returned statistics object will provide a live view into the
     * statistics, rather than a snapshot.
     *
     * @return A {@code ServicePoolStatistics} object for a pool.
     */
    ServicePoolStatistics getServicePoolStatistics();
}
