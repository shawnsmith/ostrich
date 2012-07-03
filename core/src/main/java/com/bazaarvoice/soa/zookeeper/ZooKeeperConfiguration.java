package com.bazaarvoice.soa.zookeeper;

import com.bazaarvoice.soa.internal.CuratorConnection;
import com.google.common.annotations.VisibleForTesting;
import com.netflix.curator.RetryPolicy;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import com.netflix.curator.retry.RetryNTimes;
import com.netflix.curator.retry.RetryUntilElapsed;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * ZooKeeper connection configuration class.
 * <p/>
 * NOTE: If you modify this class then you <b>must</b> also modify the corresponding class in the soa-dropwizard
 * artifact.  If you don't then you've just exposed configuration options that can't be easily used by people writing
 * dropwizard services.
 */
public class ZooKeeperConfiguration {
    private String _connectString = "localhost:2181";
    private RetryPolicy _retryPolicy = new RetryNTimes(3, 100);
    private String _namespace;

    /**
     * Returns a new {@link ZooKeeperConnection} with the current configuration settings.
     * @return A new {@link ZooKeeperConnection} with the current configuration settings.
     */
    public ZooKeeperConnection connect() {
        return new CuratorConnection(_connectString, _retryPolicy, _namespace);
    }

    @VisibleForTesting
    protected String getConnectString() {
        return _connectString;
    }

    /**
     * Sets a ZooKeeper connection string that looks like "host:port,host:port,...".  The
     * connection string must list at least one live member of the ZooKeeper ensemble, and
     * should list all members of the ZooKeeper ensemble in case any one member is temporarily
     * unavailable.
     * @param connectString A ZooKeeper connection string.
     */
    public ZooKeeperConfiguration withConnectString(String connectString) {
        _connectString = connectString;
        return this;
    }

    @VisibleForTesting
    protected RetryPolicy getRetryPolicy() {
        return _retryPolicy;
    }

    /**
     * Sets a retry policy that retries a set number of times with increasing sleep time between retries.
     */
    public ZooKeeperConfiguration withExponentialBackoffRetry(int baseSleepTimeMs, int maxRetries) {
        checkArgument(maxRetries >= 0);
        checkArgument(maxRetries == 0 || baseSleepTimeMs > 0);

        _retryPolicy = new ExponentialBackoffRetry(baseSleepTimeMs, maxRetries);
        return this;
    }

    /**
     * Sets a retry policy that retries a set number of times with a constant sleep time between retries.
     */
    public ZooKeeperConfiguration withRetryNTimes(int n, int sleepMsBetweenRetries) {
        checkArgument(n >= 0);
        checkArgument(n == 0 || sleepMsBetweenRetries > 0);

        _retryPolicy = new RetryNTimes(n, sleepMsBetweenRetries);
        return this;
    }

    /**
     * Sets a retry policy that retries until a specified time has elapsed, with a constant sleep time between retries.
     */
    public ZooKeeperConfiguration withRetryUntilElapsed(int maxElapsedTimeMs, int sleepMsBetweenRetries) {
        checkArgument(maxElapsedTimeMs >= 0);
        checkArgument(maxElapsedTimeMs == 0 || sleepMsBetweenRetries > 0);

        _retryPolicy = new RetryUntilElapsed(maxElapsedTimeMs, sleepMsBetweenRetries);
        return this;
    }

    @VisibleForTesting
    protected String getNamespace() {
        return _namespace;
    }

    /**
     * Sets a namespace that will be prefixed to every path used by the ZooKeeperConnection.
     * Typically the namespace will be "/global" or the name of the local data center.
     */
    public ZooKeeperConfiguration withNamespace(String namespace) {
        _namespace = namespace;
        return this;
    }
}
