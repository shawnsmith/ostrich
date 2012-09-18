package com.bazaarvoice.soa.retry;

import com.bazaarvoice.soa.RetryPolicy;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExponentialBackoffRetryTest {
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeNumberOfTimes() {
        new ExponentialBackoffRetry(-1, 1, 1, TimeUnit.MILLISECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeBaseSleepTime() {
        new ExponentialBackoffRetry(1, -1, 1, TimeUnit.MILLISECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMaxSleepTime() {
        new ExponentialBackoffRetry(1, 1, -1, TimeUnit.MILLISECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroNAttempts() {
        RetryPolicy retry = new ExponentialBackoffRetry(0, 0, 0, TimeUnit.MILLISECONDS);
        retry.allowRetry(0, 0);
    }

    @Test
    public void testRetryMaxZeroAttempts() {
        RetryPolicy retry = new ExponentialBackoffRetry(0, 0, 0, TimeUnit.MILLISECONDS);
        assertFalse(retry.allowRetry(1, 0));
    }

    @Test
    public void testRetryMaxOneAttempt() {
        RetryPolicy retry = new ExponentialBackoffRetry(1, 0, 0, TimeUnit.MILLISECONDS);
        assertFalse(retry.allowRetry(1, 0));
    }

    @Test
    public void testRetryMaxTwoAttempts() {
        RetryPolicy retry = new ExponentialBackoffRetry(2, 0, 0, TimeUnit.MILLISECONDS);
        assertTrue(retry.allowRetry(1, 0));
        assertFalse(retry.allowRetry(2, 0));
    }

    @Test
    public void testBaseSleepTimeUnits() {
        SleepingRetry retry = new ExponentialBackoffRetry(10, 20, 1000, TimeUnit.SECONDS);
        long sleepTimeMs = retry.getSleepTimeMs(1, 0);

        assertTrue(sleepTimeMs == 20000 || sleepTimeMs == 40000);
    }

    @Test
    public void testMaxSleepTimeUnits() {
        SleepingRetry retry = new ExponentialBackoffRetry(10, 50, 20, TimeUnit.SECONDS);
        long sleepTimeMs = retry.getSleepTimeMs(1, 0);

        assertTrue(sleepTimeMs == 20000);
    }

    @Test
    public void testFirstRetrySleepTime() {
        SleepingRetry retry = new ExponentialBackoffRetry(10, 20, 1000, TimeUnit.MILLISECONDS);
        long sleepTimeMs = retry.getSleepTimeMs(1, 0);

        assertTrue(sleepTimeMs == 20 || sleepTimeMs == 40);
    }

    @Test
    public void testSecondRetrySleepTime() {
        SleepingRetry retry = new ExponentialBackoffRetry(10, 20, 1000, TimeUnit.MILLISECONDS);
        long sleepTimeMs = retry.getSleepTimeMs(2, 0);

        assertTrue(sleepTimeMs == 20 || sleepTimeMs == 40 || sleepTimeMs == 60 || sleepTimeMs == 80);
    }

    @Test
    public void testMaxRetrySleepTime() {
        SleepingRetry retry = new ExponentialBackoffRetry(10, 20, 50, TimeUnit.MILLISECONDS);
        for (int i = 0; i < 10; i++) {
            long sleepTimeMs = retry.getSleepTimeMs(4, 0);
            assertTrue(sleepTimeMs <= 50);
        }
    }
}
