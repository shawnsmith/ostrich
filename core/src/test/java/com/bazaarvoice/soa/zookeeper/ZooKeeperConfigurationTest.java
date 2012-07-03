package com.bazaarvoice.soa.zookeeper;

import org.junit.Test;

public class ZooKeeperConfigurationTest {
    private ZooKeeperConfiguration _config = new ZooKeeperConfiguration();

    @Test
    public void testNoRetryBoundedExponentialBackoffRetry() {
        _config.withBoundedExponentialBackoffRetry(0, 0, 0);
    }

    @Test
    public void testBoundedExponentialBackoffRetry() {
        _config.withBoundedExponentialBackoffRetry(10, 1000, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeInitialSleepTime() {
        _config.withBoundedExponentialBackoffRetry(-1, 10, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroInitialSleepTime() {
        _config.withBoundedExponentialBackoffRetry(0, 10, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMaxSleepTime() {
        _config.withBoundedExponentialBackoffRetry(10, -1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroMaxSleepTime() {
        _config.withBoundedExponentialBackoffRetry(10, 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadBoundedExponentialBackoffRetry() {
        _config.withBoundedExponentialBackoffRetry(0, 0, 1);
    }
}
