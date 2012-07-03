package com.bazaarvoice.soa.zookeeper.dropwizard;

import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ZooKeeperConfigurationTest {
    private static final ObjectMapper JSON = new MappingJsonFactory().getCodec();

    @Test
    public void testJsonDefaults() throws IOException {
        ZooKeeperConfiguration config = fromJson("{}");
        assertEquals("localhost:2181", config.getConnectString());
        assertTrue(config.getRetryPolicy() instanceof com.netflix.curator.retry.RetryNTimes);
    }

    @Test
    public void testJsonConnectString() throws IOException {
        ZooKeeperConfiguration config = fromJson("{\"connectString\":\"prod-zk-1:12345\"}");
        assertEquals("prod-zk-1:12345", config.getConnectString());
    }

    @Test
    public void testJsonExponentialBackoffRetry() throws IOException {
        ZooKeeperConfiguration config = fromJson("{\"exponentialBackoffRetry\":{\"baseSleepTimeMs\":100,\"maxRetries\":3}}");
        assertTrue(config.getRetryPolicy() instanceof com.netflix.curator.retry.ExponentialBackoffRetry);
    }

    @Test
    public void testJsonRetryNTimes() throws IOException {
        ZooKeeperConfiguration config = fromJson("{\"retryNTimes\":{\"sleepMsBetweenRetries\":100,\"n\":3}}");
        assertTrue(config.getRetryPolicy() instanceof com.netflix.curator.retry.RetryNTimes);
    }

    @Test
    public void testJsonRetryUntilElapsed() throws IOException {
        ZooKeeperConfiguration config = fromJson("{\"retryUntilElapsed\":{\"sleepMsBetweenRetries\":100,\"maxElapsedTimeMs\":2000}}");
        assertTrue(config.getRetryPolicy() instanceof com.netflix.curator.retry.RetryUntilElapsed);
    }

    @Test
    public void testNamespace() throws IOException {
        ZooKeeperConfiguration config = fromJson("{\"namespace\":\"global\"}");
        assertEquals("global", config.getNamespace());
    }

    private ZooKeeperConfiguration fromJson(String json) throws IOException {
        return JSON.readValue(json, ZooKeeperConfiguration.class);
    }
}
