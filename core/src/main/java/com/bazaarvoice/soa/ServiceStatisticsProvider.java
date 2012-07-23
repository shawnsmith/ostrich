package com.bazaarvoice.soa;

public interface ServiceStatisticsProvider<T> {

    T serviceStats(ServiceEndPoint endPoint);
}
