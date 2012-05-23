package com.bazaarvoice.soa.retry;

import com.google.common.base.Preconditions;

import java.util.concurrent.TimeUnit;

/** A retry policy that permits a fixed number of attempts at executing an operation. */
public class RetryNTimes extends SleepingRetry {
    private final long _sleepTimeBetweenAttemptsMillis;

    public RetryNTimes(int maxNumAttempts) {
        this(maxNumAttempts, 0, TimeUnit.MILLISECONDS);
    }

    public RetryNTimes(int maxNumAttempts, long sleepTimeBetweenRetries, TimeUnit unit) {
        super(maxNumAttempts);

        Preconditions.checkArgument(sleepTimeBetweenRetries >= 0);
        Preconditions.checkNotNull(unit);
        _sleepTimeBetweenAttemptsMillis = unit.toMillis(sleepTimeBetweenRetries);
    }

    @Override
    protected long getSleepTimeMs(int numAttempts, long elapsedTimeMs) {
        return _sleepTimeBetweenAttemptsMillis;
    }
}
