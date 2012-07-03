package com.bazaarvoice.soa.zookeeper.dropwizard;

import org.codehaus.jackson.annotate.JsonProperty;

public class RetryNTimes {
    @JsonProperty("n")
    int n;

    @JsonProperty("sleepMsBetweenRetries")
    int sleepMsBetweenRetries;
}
