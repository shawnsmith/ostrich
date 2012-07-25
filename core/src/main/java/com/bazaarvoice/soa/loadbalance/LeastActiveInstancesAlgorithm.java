package com.bazaarvoice.soa.loadbalance;

import com.bazaarvoice.soa.LoadBalanceAlgorithm;
import com.bazaarvoice.soa.ServiceEndPoint;
import com.bazaarvoice.soa.ServicePoolStatistics;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A load balance algorithm that chooses the end point with the least current instances in use.
 */
public class LeastActiveInstancesAlgorithm implements LoadBalanceAlgorithm {
    private final ServicePoolStatistics _stats;

    public LeastActiveInstancesAlgorithm(ServicePoolStatistics stats) {
        _stats = checkNotNull(stats);
    }

    @Override
    public ServiceEndPoint choose(Iterable<ServiceEndPoint> endPoints) {
        checkNotNull(endPoints);
        List<ServiceEndPoint> sortedList = Lists.newArrayList(endPoints);
        Collections.sort(sortedList, new Comparator<ServiceEndPoint>() {
            @Override
            public int compare(ServiceEndPoint endPoint1, ServiceEndPoint endPoint2) {
                return _stats.getNumActiveInstances(endPoint1) - _stats.getNumActiveInstances(endPoint2);
            }
        });
        if (sortedList.size() == 0) {
            return null;
        } else {
            return sortedList.get(0);
        }
    }
}
