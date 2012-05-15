package com.bazaarvoice.soa.retry;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RetryNTimesTest {
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeNumberOfTimes() {
        new RetryNTimes(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroNumberOfTimesTimes() {
        new RetryNTimes(0);
    }

    @Test
    public void testRetryOneTime() {
        RetryNTimes retry = new RetryNTimes(1);
        assertTrue(retry.allowRetry(0, 0));
        assertFalse(retry.allowRetry(1, 0));
    }

    @Test
    public void testRetryTwoTimes() {
        RetryNTimes retry = new RetryNTimes(2);
        assertTrue(retry.allowRetry(0, 0));
        assertTrue(retry.allowRetry(1, 0));
        assertFalse(retry.allowRetry(2, 0));
    }

    @Test
    public void testRetryRandomTimes() {
        Random rnd = new Random();
        int N = rnd.nextInt(1000) + 1;

        RetryNTimes retry = new RetryNTimes(N);
        for(int i = 0; i < N; i++) {
            assertTrue(retry.allowRetry(i, 0));
        }
        assertFalse(retry.allowRetry(N, 0));
    }
}
