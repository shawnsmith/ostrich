package com.bazaarvoice.soa.zookeeper;

import org.junit.Test;

public class ZooKeeperConfigurationTest {
    private ZooKeeperConfiguration _config = new ZooKeeperConfiguration();

    @Test
    public void testNullExponentialBackoffRetry() {
        _config.withExponentialBackoffRetry(0, 0);
    }

    @Test
    public void testNullRetryNTimes() {
        _config.withRetryNTimes(0, 0);
    }

    @Test
    public void testNullRetryUntilElapsed() {
        _config.withRetryUntilElapsed(0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadExponentialBackoffRetry() {
        _config.withExponentialBackoffRetry(0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadRetryNTimes() {
        _config.withRetryNTimes(1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadRetryUntilElapsed() {
        _config.withRetryUntilElapsed(1, 0);
    }
}
