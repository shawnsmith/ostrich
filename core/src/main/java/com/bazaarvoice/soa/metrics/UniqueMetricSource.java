package com.bazaarvoice.soa.metrics;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;

import java.io.Closeable;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class UniqueMetricSource implements Closeable {
    private final MetricsRegistry _registry;
    private final String _uniqueScope;
    private final Class _domain;

    public UniqueMetricSource(Class domain, String scope) {
        checkNotNull(domain);
        checkArgument(!Strings.isNullOrEmpty(scope));
        _domain = domain;
        _uniqueScope = UUID.randomUUID().toString() + "-" + scope;
        _registry = Metrics.defaultRegistry();
    }

    public UniqueMetricSource(Class domain) {
        checkNotNull(domain);
        _domain = domain;
        _uniqueScope = UUID.randomUUID().toString();
        _registry = Metrics.defaultRegistry();
    }

    public void close() {
        // We don't compare the name field of the MetricName, so the name doesn't matter.
        final MetricName reference = uniqueMetricName("reference");
        for (MetricName metric : Iterables.filter(Metrics.defaultRegistry().allMetrics().keySet(), new Predicate<MetricName>() {
            @Override
            public boolean apply(MetricName metricName) {
                return reference.getGroup().equals(metricName.getGroup())
                        && reference.getType().equals(metricName.getType())
                        && reference.getScope().equals(metricName.getScope());
            }
        })) {
            Metrics.defaultRegistry().removeMetric(metric);
        }
    }

    public <T> Gauge<T> newGauge(String name, Gauge<T> metric) {
        checkNotNull(metric);
        return _registry.newGauge(uniqueMetricName(name), metric);
    }

    public Counter newCounter(String name) {
        return _registry.newCounter(uniqueMetricName(name));
    }

    public Histogram newHistogram(String name, boolean biased) {
        return _registry.newHistogram(uniqueMetricName(name), biased);
    }

    public Histogram newHistogram(String name) {
        return newHistogram(name, false);
    }

    public Meter newMeter(String name, String eventType, TimeUnit unit) {
        checkArgument(!Strings.isNullOrEmpty(eventType));
        checkNotNull(unit);
        return _registry.newMeter(uniqueMetricName(name), eventType, unit);
    }

    public Meter newMeter(String name, String eventType) {
        checkArgument(!Strings.isNullOrEmpty(eventType));
        return newMeter(name, eventType, TimeUnit.SECONDS);
    }

    public Timer newTimer(String name) {
        return _registry.newTimer(_domain, name, _uniqueScope);
    }

    public Timer newTimer(String name, TimeUnit durationUnit, TimeUnit rateUnit) {
        checkNotNull(durationUnit);
        checkNotNull(rateUnit);
        return _registry.newTimer(uniqueMetricName(name), durationUnit, rateUnit);
    }

    public void removeMetric(String name) {
        _registry.removeMetric(uniqueMetricName(name));
    }

    @VisibleForTesting
    MetricName uniqueMetricName(String name) {
        checkArgument(!Strings.isNullOrEmpty(name));
        return new MetricName(_domain, name, _uniqueScope);
    }

    @VisibleForTesting
    MetricsRegistry getRegistry() {
        return _registry;
    }
}
