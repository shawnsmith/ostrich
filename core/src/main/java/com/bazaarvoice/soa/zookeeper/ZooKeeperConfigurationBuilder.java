package com.bazaarvoice.soa.zookeeper;

import com.bazaarvoice.soa.internal.CuratorConfiguration;
import com.google.common.base.Preconditions;
import com.netflix.curator.RetryPolicy;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import com.netflix.curator.retry.RetryNTimes;
import com.netflix.curator.retry.RetryOneTime;
import com.netflix.curator.retry.RetryUntilElapsed;

/** Builds a <code>ZooKeeperConfiguration</code>. */
public class ZooKeeperConfigurationBuilder {
    private String _connectString;
    private RetryPolicy _retryPolicy;

    public ZooKeeperConfigurationBuilder withConnectString(String connectString) {
        _connectString = Preconditions.checkNotNull(connectString);
        return this;
    }

    public ZooKeeperConfigurationBuilder withExponentialBackoffRetry(int baseSleepTimeMs, int maxRetries) {
        _retryPolicy = new ExponentialBackoffRetry(baseSleepTimeMs, maxRetries);
        return this;
    }

    public ZooKeeperConfigurationBuilder withRetryNTimes(int n, int sleepMsBetweenRetries) {
        _retryPolicy = new RetryNTimes(n, sleepMsBetweenRetries);
        return this;
    }

    public ZooKeeperConfigurationBuilder withRetryOneTime(int sleepMsBetweenRetries) {
        _retryPolicy = new RetryOneTime(sleepMsBetweenRetries);
        return this;
    }

    public ZooKeeperConfigurationBuilder withRetryUntilElapsed(int maxElapsedTimeMs, int sleepMsBetweenRetries) {
        _retryPolicy = new RetryUntilElapsed(maxElapsedTimeMs, sleepMsBetweenRetries);
        return this;
    }

    public ZooKeeperConfiguration build() {
        Preconditions.checkNotNull(_connectString);
        Preconditions.checkNotNull(_retryPolicy);
        return new CuratorConfiguration(_connectString, _retryPolicy);
    }
}
