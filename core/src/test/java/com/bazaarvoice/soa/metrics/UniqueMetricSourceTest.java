package com.bazaarvoice.soa.metrics;

import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.Timer;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class UniqueMetricSourceTest {
    private final static String NAME = "test";
    private final static String SCOPE = "test";

    private final UniqueMetricSource _source = new UniqueMetricSource(getClass());

    @After
    public void teardown() {
        _source.close();
    }

    @Test(expected = NullPointerException.class)
    public void testNullDomain() {
        new UniqueMetricSource(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullDomainScoped() {
        new UniqueMetricSource(null, SCOPE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullScope() {
        new UniqueMetricSource(getClass(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyScope() {
        new UniqueMetricSource(getClass(), "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullName() {
        _source.uniqueMetricName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyName() {
        _source.uniqueMetricName("");
    }

    @Test
    public void testUniqueNames() {
        // Two different sources should produce different names from the same input.
        UniqueMetricSource first = new UniqueMetricSource(getClass());
        UniqueMetricSource second = new UniqueMetricSource(getClass());
        assertThat(first.uniqueMetricName(NAME), not(equalTo(second.uniqueMetricName(NAME))));
    }

    @Test
    public void testUniqueNamesScoped() {
        // Two different sources should produce different names from the same input.
        UniqueMetricSource first = new UniqueMetricSource(getClass(), SCOPE);
        UniqueMetricSource second = new UniqueMetricSource(getClass(), SCOPE);
        assertThat(first.uniqueMetricName(NAME), not(equalTo(second.uniqueMetricName(NAME))));
    }

    @Test
    public void testSameName() {
        // One source should produce the same name from the same input.
        UniqueMetricSource source = new UniqueMetricSource(getClass());
        assertEquals(source.uniqueMetricName(NAME), source.uniqueMetricName(NAME));
    }

    @Test
    public void testSameNameScoped() {
        // One source should produce the same name from the same input.
        UniqueMetricSource source = new UniqueMetricSource(getClass(), SCOPE);
        assertEquals(source.uniqueMetricName(NAME), source.uniqueMetricName(NAME));
    }

    @Test(expected = NullPointerException.class)
    public void testNullGauge() {
        _source.newGauge(NAME, null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNewGauge() {
        assertRegistered(_source.newGauge(NAME, mock(Gauge.class)));
    }

    @Test
    public void testNewCounter() {
        assertRegistered(_source.newCounter(NAME));
    }

    @Test
    public void testNewHistogram() {
        assertRegistered(_source.newHistogram(NAME));
    }

    @Test
    public void testNewHistogramBiased() {
        assertRegistered(_source.newHistogram(NAME, true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewMeterNullEventType() {
        _source.newMeter(NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewMeterEmptyEventType() {
        _source.newMeter(NAME, "");
    }

    @Test
    public void testNewMeter() {
        assertRegistered(_source.newMeter(NAME, "events"));
    }

    @Test(expected = NullPointerException.class)
    public void testNewMeterWithUnitNullTimeUnit() {
        _source.newMeter(NAME, "events", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewMeterWithUnitNullEventType() {
        _source.newMeter(NAME, null, TimeUnit.MICROSECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewMeterWithUnitEmptyEventType() {
        _source.newMeter(NAME, "", TimeUnit.MICROSECONDS);
    }

    @Test
    public void testNewMeterWithUnit() {
        Meter meter = _source.newMeter(NAME, "events", TimeUnit.MICROSECONDS);
        assertEquals(TimeUnit.MICROSECONDS, meter.rateUnit());
        assertRegistered(meter);
    }

    @Test
    public void testNewTimer() {
        assertRegistered(_source.newTimer(NAME));
    }

    @Test(expected = NullPointerException.class)
    public void testNewTimerNullDurationUnit() {
        _source.newTimer(NAME, null, TimeUnit.MICROSECONDS);
    }

    @Test(expected = NullPointerException.class)
    public void testNewTimerNullRateUnit() {
        _source.newTimer(NAME, TimeUnit.MICROSECONDS, null);
    }

    @Test
    public void testNewTimerUnits() {
        Timer timer = _source.newTimer(NAME, TimeUnit.MICROSECONDS, TimeUnit.MICROSECONDS);
        assertEquals(TimeUnit.MICROSECONDS, timer.rateUnit());
        assertEquals(TimeUnit.MICROSECONDS, timer.durationUnit());
        assertRegistered(timer);
    }

    @Test
    public void testUnregister() {
        assertRegistered(_source.newCounter(NAME));
        _source.removeMetric(NAME);
        assertNotRegistered();
    }

    @Test
    public void testCloseUnregisters() {
        assertRegistered(_source.newCounter(NAME));
        _source.close();
        assertNotRegistered();
    }

    @Test
    public void testCloseOnlyUnregistersOwn() {
        Metric metric = _source.newCounter(NAME);
        UniqueMetricSource second = new UniqueMetricSource(getClass());
        second.newCounter(NAME);
        second.close();
        assertRegistered(metric);
    }

    private void assertRegistered(Metric metric) {
        assertEquals(metric, _source.getRegistry().allMetrics().get(_source.uniqueMetricName(NAME)));
    }

    private void assertNotRegistered() {
        assertNull(_source.getRegistry().allMetrics().get(_source.uniqueMetricName(NAME)));
    }
}
