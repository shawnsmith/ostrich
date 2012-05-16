package com.bazaarvoice.soa.zookeeper;

import com.netflix.curator.RetryPolicy;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import com.netflix.curator.retry.RetryNTimes;
import com.netflix.curator.retry.RetryOneTime;
import com.netflix.curator.retry.RetryUntilElapsed;

/**
 * Encapsulates the configuration of ZooKeeper in a way that doesn't expose any underlying details of the ZooKeeper
 * library that's being used.  This way we don't leak any information in our public API about the library, and can then
 * change libraries at will.
 */
public class ZooKeeperConfiguration {
    private String _connectString;
    private RetryPolicy _retry;

    public ZooKeeperConfiguration withConnectString(String connectString) {
        _connectString = connectString;
        return this;
    }

    public ZooKeeperConfiguration withExponentialBackoffRetry(int baseSleepTimeMs, int maxRetries) {
        _retry = new ExponentialBackoffRetry(baseSleepTimeMs, maxRetries);
        return this;
    }

    public ZooKeeperConfiguration withRetryNTimes(int n, int sleepMsBetweenRetries) {
        _retry = new RetryNTimes(n, sleepMsBetweenRetries);
        return this;
    }

    public ZooKeeperConfiguration withRetryOneTime(int sleepMsBetweenRetries) {
        _retry = new RetryOneTime(sleepMsBetweenRetries);
        return this;
    }

    public ZooKeeperConfiguration withRetryUntilElapsed(int maxElapsedTimeMs, int sleepMsBetweenRetries) {
        _retry = new RetryUntilElapsed(maxElapsedTimeMs, sleepMsBetweenRetries);
        return this;
    }
}
