package com.bazaarvoice.soa.loadbalance;

import com.bazaarvoice.soa.DefaultServiceStatisticsProviders;
import com.bazaarvoice.soa.LoadBalanceAlgorithm;
import com.bazaarvoice.soa.ServiceEndPoint;
import com.bazaarvoice.soa.ServiceStatisticsProvider;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A load balance algorithm that will first look for end points where a cached connection is available. It will then
 * delegate the choice to another load balance algorithm.
 */
public class PreferCachedDelegatingAlgorithm implements LoadBalanceAlgorithm {

    private final LoadBalanceAlgorithm _delegate;

    /**
     * Construcs a {@code PreferCachedDelegatingAlgorithm} that prioritizes end points with cached connections
     * available, but otherwise delegates the decision to another algorithm.
     * @param delegate The delegate to use.
     */
    public PreferCachedDelegatingAlgorithm(LoadBalanceAlgorithm delegate) {
        checkNotNull(delegate);
        _delegate = delegate;
    }

    @Override
    public ServiceEndPoint choose(Iterable<ServiceEndPoint> endpoints, Map<Enum, ServiceStatisticsProvider> stats) {
        final ServiceStatisticsProvider<Integer> statsProvider =
                stats.get(DefaultServiceStatisticsProviders.NUM_AVAILABLE_CACHED_CONNECTIONS);
        final ServiceEndPoint cachedEndPoint = _delegate.choose(Iterables.filter(endpoints,
                new Predicate<ServiceEndPoint>() {
                    @Override
                    public boolean apply(@Nullable ServiceEndPoint endPoint) {
                        return statsProvider != null && statsProvider.serviceStats(endPoint) > 0;
                    }
                }),
            stats);
        if (cachedEndPoint != null) {
            return cachedEndPoint;
        } else {
            return _delegate.choose(endpoints, stats);
        }
    }
}