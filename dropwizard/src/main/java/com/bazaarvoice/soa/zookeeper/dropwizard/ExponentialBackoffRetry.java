package com.bazaarvoice.soa.zookeeper.dropwizard;

import org.codehaus.jackson.annotate.JsonProperty;

public class ExponentialBackoffRetry {
    @JsonProperty("baseSleepTimeMs")
    int baseSleepTimeMs;

    @JsonProperty("maxRetries")
    int maxRetries;
}
