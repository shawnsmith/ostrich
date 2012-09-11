package com.bazaarvoice.soa;

import java.util.Map;

/**
 * A map of key/value pairs that a service pool can use to choose among available back-end service end points.
 */
public interface PartitionContext extends Map<String, Object> {
    /**
     * Returns the partition context value associated with the default key, the empty string.
     */
    Object get();
}
