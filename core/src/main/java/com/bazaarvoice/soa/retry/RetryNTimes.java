package com.bazaarvoice.soa.retry;

import com.bazaarvoice.soa.RetryPolicy;
import com.google.common.base.Preconditions;

/** A retry policy that permits a fixed number of attempts at executing an operation. */
public class RetryNTimes implements RetryPolicy {
    private final int _maxNumAttempts;

    public RetryNTimes(int maxNumAttempts) {
        Preconditions.checkArgument(maxNumAttempts > 0);
        _maxNumAttempts = maxNumAttempts;
    }

    @Override
    public boolean allowRetry(int numAttempts, long elapsedTimeMs) {
        return numAttempts < _maxNumAttempts;
    }
}
