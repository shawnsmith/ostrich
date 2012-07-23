package com.bazaarvoice.soa.loadbalance;

import com.bazaarvoice.soa.DefaultServiceStatisticsProviders;
import com.bazaarvoice.soa.LoadBalanceAlgorithm;
import com.bazaarvoice.soa.ServiceEndPoint;
import com.bazaarvoice.soa.ServiceStatisticsProvider;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * A load balance algorithm that chooses the end point with the least current connections according to {@link }
 */
public class LeastActiveConnectionsAlgorithm implements LoadBalanceAlgorithm {

    @Override
    public ServiceEndPoint choose(Iterable<ServiceEndPoint> endpoints, Map<Enum, ServiceStatisticsProvider> stats) {
        final ServiceStatisticsProvider<Integer> statsProvider =
                stats.get(DefaultServiceStatisticsProviders.NUM_ACTIVE_CONNECTIONS);
        List<ServiceEndPoint> sortedList = Lists.newArrayList(endpoints);
        Collections.sort(sortedList, new Comparator<ServiceEndPoint>() {
            @Override
            public int compare(ServiceEndPoint endPoint1, ServiceEndPoint endPoint2) {
                return statsProvider.serviceStats(endPoint1) - statsProvider.serviceStats(endPoint2);
            }
        });
        if (sortedList.size() == 0) {
            return null;
        } else {
            return sortedList.get(0);
        }
    }
}
