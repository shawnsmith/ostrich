package com.bazaarvoice.soa.pool;

import com.bazaarvoice.soa.RetryPolicy;

/**
 * Test helper that exposes package-private methods on {@link ServicePoolProxy}.
 */
public class ServicePoolProxyHelper {
    public static <S> S create(Class<S> serviceType, RetryPolicy retryPolicy, com.bazaarvoice.soa.ServicePool<S> pool,
                        PartitionContextSupplier partitionContextSupplier, boolean shutdownPoolOnClose) {
        return ServicePoolProxy.create(serviceType, retryPolicy, pool, partitionContextSupplier, shutdownPoolOnClose);
    }
}
