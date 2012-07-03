package com.bazaarvoice.soa.zookeeper.dropwizard;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public class BoundedExponentialBackoffRetry {
    public final int baseSleepTimeMs;
    public final int maxSleepTimeMs;
    public final int maxRetries;

    @JsonCreator
    public BoundedExponentialBackoffRetry(@JsonProperty("baseSleepTimeMs") int baseSleepTimeMs,
                                          @JsonProperty("maxSleepTimeMs") int maxSleepTimeMs,
                                          @JsonProperty("maxRetries") int maxRetries) {
        this.baseSleepTimeMs = baseSleepTimeMs;
        this.maxSleepTimeMs = maxSleepTimeMs;
        this.maxRetries = maxRetries;
    }
}
