package com.bazaarvoice.soa;

/**
 * An enum with an entry for each {@link ServiceStatisticsProvider} included in the default {@link ServicePool}
 * implementation.
 */
public enum DefaultServiceStatisticsProviders {
    /**
     * Represents the number of active connections for an end point. Note that these are only the number of connections
     * from a single {@link ServicePool}, and not globally.
     */
    NUM_ACTIVE_CONNECTIONS,

    /**
     * Represents the number of cached connections not currently in use.
     */
    NUM_AVAILABLE_CACHED_CONNECTIONS
}
