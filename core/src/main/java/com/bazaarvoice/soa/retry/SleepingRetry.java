package com.bazaarvoice.soa.retry;

import com.bazaarvoice.soa.RetryPolicy;
import com.google.common.base.Preconditions;

public abstract class SleepingRetry implements RetryPolicy {
    private final int _maxNumAttempts;

    protected SleepingRetry(int maxNumAttempts) {
        Preconditions.checkArgument(maxNumAttempts >= 0);
        _maxNumAttempts = maxNumAttempts;
    }

    @Override
    public boolean allowRetry(int numAttempts, long elapsedTimeMs) {
        if (numAttempts >= _maxNumAttempts) {
            return false;
        }

        try {
            Thread.sleep(getSleepTimeMs(numAttempts, elapsedTimeMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return true;
    }

    protected abstract long getSleepTimeMs(int numAttempts, long elapsedTimeMs);
}
