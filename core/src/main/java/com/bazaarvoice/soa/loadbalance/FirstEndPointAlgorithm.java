package com.bazaarvoice.soa.loadbalance;

import com.bazaarvoice.soa.LoadBalanceAlgorithm;
import com.bazaarvoice.soa.ServiceEndPoint;
import com.google.common.collect.Iterables;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A naive {@code LoadBalanceAlgorithm}. Simply returns the first of the end points provided to it, or {@code null} if
 * given an empty list. Useful as a delegate if something has sorted the end points by some weight, like in
 * {@link PreferCachedDelegatingAlgorithm}.
 */
public class FirstEndPointAlgorithm implements LoadBalanceAlgorithm {
    @Override
    public ServiceEndPoint choose(Iterable<ServiceEndPoint> endPoints) {
        checkNotNull(endPoints);
        return Iterables.getFirst(endPoints, null);
    }
}
