package com.bazaarvoice.soa.zookeeper.dropwizard;

import org.codehaus.jackson.annotate.JsonProperty;

public class RetryUntilElapsed {
    @JsonProperty("maxElapsedTimeMs")
    int maxElapsedTimeMs;

    @JsonProperty("sleepMsBetweenRetries")
    int sleepMsBetweenRetries;
}
