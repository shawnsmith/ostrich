package com.bazaarvoice.soa.loadbalance;

import com.bazaarvoice.soa.LoadBalanceAlgorithm;
import com.bazaarvoice.soa.ServiceEndPoint;
import com.bazaarvoice.soa.ServicePoolStatistics;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.Collection;
import java.util.Collections;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A load balance algorithm that will first look for end points where a cached service instance is available. It will
 * then delegate the choice to another load balance algorithm.
 */
public class PreferCachedDelegatingAlgorithm implements LoadBalanceAlgorithm {
    private final LoadBalanceAlgorithm _cachedDelegate;
    private final LoadBalanceAlgorithm _nonCachedDelegate;
    private final ServicePoolStatistics _stats;

    /**
     * Constructs a {@code PreferCachedDelegatingAlgorithm} that prioritizes end points with cached service instances
     * available, then delegates the decision accordingly.
     *
     * @param cachedDelegate The delegate to use if cached instances are available.
     * @param nonCachedDelegate The delegate to use if no cached instances are available.
     * @param stats The source of cache statistics.
     */
    public PreferCachedDelegatingAlgorithm(LoadBalanceAlgorithm cachedDelegate,
                                           LoadBalanceAlgorithm nonCachedDelegate, ServicePoolStatistics stats) {
        _cachedDelegate = checkNotNull(cachedDelegate);
        _nonCachedDelegate = checkNotNull(nonCachedDelegate);
        _stats = checkNotNull(stats);

    }

    /**
     * Separates out end points with cached instances and then delegates load balancing accordingly.
     * <p/>
     * If end points with cached instances exist, those will be sorted in descending order by number of cached instances
     * and passed to the {@code cachedDelegate} algorithm to decide. If no end points have cached instances, or if the
     * {@code cachedDelegate} returns null, then all end points will be passed to the {@code nonCachedDelegate} to
     * decide.
     *
     *
     * @param endpoints The endpoints to choose from.
     * @return The end point chosen by the {@code cachedDelegate} algorithm from the set of end points with cached
     * service instances, if any, or the end point chosen by the {@code nonCachedDelegate} algorithm from all end
     * points.
     */
    @SuppressWarnings("unchecked")
    @Override
    public ServiceEndPoint choose(Iterable<ServiceEndPoint> endpoints) {
        checkNotNull(endpoints);
        // Create a map to hold end points sorted in descending order according to their number of cached instances.
        Multimap<Integer, ServiceEndPoint> cachedEndPoints = Multimaps.newMultimap(
                Maps.<Integer, Integer, Collection<ServiceEndPoint>>newTreeMap(Collections.<Integer>reverseOrder()),
                new Supplier<Collection<ServiceEndPoint>>() {
                    @Override
                    public Collection<ServiceEndPoint> get() {
                        return Lists.newLinkedList();
                    }
                }
        );
        for (ServiceEndPoint endPoint : endpoints) {
            int numCached = _stats.getNumIdleCachedInstances(endPoint);
            if (numCached > 0) {
                cachedEndPoints.put(numCached, endPoint);
            }
        }

        ServiceEndPoint cachedEndPoint = null;
        if (!cachedEndPoints.isEmpty()) {
            cachedEndPoint = _cachedDelegate.choose(cachedEndPoints.values());
        }
        if (cachedEndPoint != null) {
            return cachedEndPoint;
        } else {
            return _nonCachedDelegate.choose(endpoints);
        }
    }
}