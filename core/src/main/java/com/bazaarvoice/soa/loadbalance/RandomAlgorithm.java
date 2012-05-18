package com.bazaarvoice.soa.loadbalance;

import com.bazaarvoice.soa.LoadBalanceAlgorithm;
import com.bazaarvoice.soa.ServiceEndpoint;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class RandomAlgorithm implements LoadBalanceAlgorithm {
    private final Random _rnd = new Random();

    @Override
    public ServiceEndpoint choose(Iterable<ServiceEndpoint> endpoints) {
        Preconditions.checkNotNull(endpoints);

        Iterator<ServiceEndpoint> iter = endpoints.iterator();
        Preconditions.checkArgument(iter.hasNext());

        List<ServiceEndpoint> list = Lists.newArrayList(iter);
        return list.get(_rnd.nextInt(list.size()));
    }
}
