package com.bazaarvoice.soa.metrics;

import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.Timer;
import org.junit.After;
import org.junit.Test;

import javax.management.ObjectName;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class MetricsTest {
    private final static String NAME = "test";
    private final static String SCOPE = "test";

    private final Metrics _source = new Metrics(getClass());

    @After
    public void teardown() {
        _source.close();
    }

    @Test(expected = NullPointerException.class)
    public void testNullDomain() {
        new Metrics(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullDomainScoped() {
        new Metrics(null, SCOPE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullScope() {
        new Metrics(getClass(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyScope() {
        new Metrics(getClass(), "");
    }

    @Test(expected = NullPointerException.class)
    public void testNullName() {
        _source.uniqueMetricName(null);
    }

    @Test
    public void testUniqueNames() {
        // Two different sources should produce different names from the same input.
        Metrics first = new Metrics(getClass());
        Metrics second = new Metrics(getClass());
        assertThat(first.uniqueMetricName(NAME), not(equalTo(second.uniqueMetricName(NAME))));
    }

    @Test
    public void testUniqueNamesScoped() {
        // Two different sources should produce different names from the same input.
        Metrics first = new Metrics(getClass(), SCOPE);
        Metrics second = new Metrics(getClass(), SCOPE);
        assertThat(first.uniqueMetricName(NAME), not(equalTo(second.uniqueMetricName(NAME))));
    }

    @Test
    public void testSameName() {
        // One source should produce the same name from the same input.
        Metrics source = new Metrics(getClass());
        assertEquals(source.uniqueMetricName(NAME), source.uniqueMetricName(NAME));
    }

    @Test
    public void testSameNameScoped() {
        // One source should produce the same name from the same input.
        Metrics source = new Metrics(getClass(), SCOPE);
        assertEquals(source.uniqueMetricName(NAME), source.uniqueMetricName(NAME));
    }

    @Test(expected = NullPointerException.class)
    public void testNullGauge() {
        _source.newGauge(NAME, null);
    }
    
    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testNewGaugeNullName() {
        _source.newGauge(null, mock(Gauge.class));
    }
    
    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testNewGaugeEmptyName() {
        _source.newGauge("", mock(Gauge.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNewGauge() {
        assertRegistered(_source.newGauge(NAME, mock(Gauge.class)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewCounterNullName() {
        _source.newCounter(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNewCounterEmptyName() {
        _source.newCounter("");
    }
    
    @Test
    public void testNewCounter() {
        assertRegistered(_source.newCounter(NAME));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewHistogramNullName() {
        _source.newHistogram(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNewHistogramEmptyName() {
        _source.newHistogram("");
    }
    
    @Test
    public void testNewHistogram() {
        assertRegistered(_source.newHistogram(NAME));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNewHistogramBiasedNullName() {
        _source.newHistogram(null, true);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNewHistogramBiasedEmptyName() {
        _source.newHistogram("", true);
    }

    @Test
    public void testNewHistogramBiased() {
        assertRegistered(_source.newHistogram(NAME, true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewMeterNullName() {
        _source.newMeter(null, "events", TimeUnit.MICROSECONDS);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNewMeterEmptyName() {
        _source.newMeter("", "events", TimeUnit.MICROSECONDS);
    }
    
    @Test(expected = NullPointerException.class)
    public void testNewMeterNullTimeUnit() {
        _source.newMeter(NAME, "events", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewMeterNullEventType() {
        _source.newMeter(NAME, null, TimeUnit.MICROSECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewMeterEmptyEventType() {
        _source.newMeter(NAME, "", TimeUnit.MICROSECONDS);
    }

    @Test
    public void testNewMeter() {
        Meter meter = _source.newMeter(NAME, "events", TimeUnit.MICROSECONDS);
        assertEquals(TimeUnit.MICROSECONDS, meter.rateUnit());
        assertRegistered(meter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewTimerNullName() {
        _source.newTimer(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNewTimerEmptyName() {
        _source.newTimer("");
    }
    
    @Test
    public void testNewTimer() {
        assertRegistered(_source.newTimer(NAME));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewTimerWithUnitsNullName() {
        _source.newTimer(null, TimeUnit.MICROSECONDS, TimeUnit.MICROSECONDS);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNewTimerWithUnitsEmptyName() {
        _source.newTimer("", TimeUnit.MICROSECONDS, TimeUnit.MICROSECONDS);
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

    @Test(expected = IllegalArgumentException.class)
    public void testUnregisterNull() {
        _source.removeMetric(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnregisterEmpty() {
        _source.removeMetric("");
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
        Metrics second = new Metrics(getClass());
        second.newCounter(NAME);
        second.close();
        assertRegistered(metric);
    }

    @Test
    public void testPathologicalName() {
        // ,=:*? and newline are invalid characters in an ObjectName. Backslash and quote need to be escaped.
        String name = "\"\\,:=?*\n";
        assertEquals(name, ObjectName.unquote(_source.uniqueMetricName(name).getName()));
    }

    private void assertRegistered(Metric metric) {
        assertEquals(metric, _source.getRegistry().allMetrics().get(_source.uniqueMetricName(NAME)));
    }

    private void assertNotRegistered() {
        assertNull(_source.getRegistry().allMetrics().get(_source.uniqueMetricName(NAME)));
    }
}
