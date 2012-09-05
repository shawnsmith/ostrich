package com.bazaarvoice.soa.metrics;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;

import javax.management.ObjectName;
import java.io.Closeable;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Metrics implements Closeable {
    private final MetricsRegistry _registry;
    private final String _uniqueScope;
    private final Class _domain;

    public Metrics(Class domain, String scope) {
        checkNotNull(domain);
        checkArgument(!Strings.isNullOrEmpty(scope));
        _domain = domain;
        _uniqueScope =  quoteForObjectName(scope + "-" + UUID.randomUUID().toString());
        _registry = com.yammer.metrics.Metrics.defaultRegistry();
    }

    public Metrics(Class domain) {
        checkNotNull(domain);
        _domain = domain;
        _uniqueScope = UUID.randomUUID().toString();
        _registry = com.yammer.metrics.Metrics.defaultRegistry();
    }

    public void close() {
        final MetricName reference = uniqueMetricName("");
        for (MetricName metric : Iterables.filter(_registry.allMetrics().keySet(), new Predicate<MetricName>() {
            @Override
            public boolean apply(MetricName metricName) {
                return reference.getGroup().equals(metricName.getGroup())
                        && reference.getType().equals(metricName.getType())
                        && reference.getScope().equals(metricName.getScope());
            }
        })) {
            _registry.removeMetric(metric);
        }
    }

    public <T> Gauge<T> newGauge(String name, Gauge<T> metric) {
        checkArgument(!Strings.isNullOrEmpty(name));
        checkNotNull(metric);
        return _registry.newGauge(uniqueMetricName(name), metric);
    }

    public Counter newCounter(String name) {
        checkArgument(!Strings.isNullOrEmpty(name));
        return _registry.newCounter(uniqueMetricName(name));
    }

    public Histogram newHistogram(String name, boolean biased) {
        checkArgument(!Strings.isNullOrEmpty(name));
        return _registry.newHistogram(uniqueMetricName(name), biased);
    }

    public Histogram newHistogram(String name) {
        checkArgument(!Strings.isNullOrEmpty(name));
        // MetricsRegistry doesn't have a newHistogram(MetricName) method, so we have to work around it.
        return _registry.newHistogram(_domain, quoteForObjectName(name), _uniqueScope);
    }

    public Meter newMeter(String name, String eventType, TimeUnit unit) {
        checkArgument(!Strings.isNullOrEmpty(name));
        checkArgument(!Strings.isNullOrEmpty(eventType));
        checkNotNull(unit);
        return _registry.newMeter(uniqueMetricName(name), eventType, unit);
    }

    public Timer newTimer(String name) {
        checkArgument(!Strings.isNullOrEmpty(name));
        // MetricsRegistry doesn't have a newTimer(MetricName) method, so we have to work around it.
        return _registry.newTimer(_domain, quoteForObjectName(name), _uniqueScope);
    }

    public Timer newTimer(String name, TimeUnit durationUnit, TimeUnit rateUnit) {
        checkArgument(!Strings.isNullOrEmpty(name));
        checkNotNull(durationUnit);
        checkNotNull(rateUnit);
        return _registry.newTimer(uniqueMetricName(name), durationUnit, rateUnit);
    }

    public void removeMetric(String name) {
        checkArgument(!Strings.isNullOrEmpty(name));
        _registry.removeMetric(uniqueMetricName(name));
    }

    @VisibleForTesting
    MetricName uniqueMetricName(String name) {
        checkNotNull(name);
        return new MetricName(_domain, quoteForObjectName(name), _uniqueScope);
    }

    private String quoteForObjectName(String string) {
        return ObjectName.quote(string);
    }

    @VisibleForTesting
    MetricsRegistry getRegistry() {
        return _registry;
    }
}
