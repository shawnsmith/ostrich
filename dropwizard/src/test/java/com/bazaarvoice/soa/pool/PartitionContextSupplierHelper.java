package com.bazaarvoice.soa.pool;

import org.mockito.Mockito;

/**
 * Test helper that exposes package-private methods on {@link PartitionContextSupplier}.
 */
public class PartitionContextSupplierHelper {
    public static PartitionContextSupplier mock() {
        return Mockito.mock(PartitionContextSupplier.class);
    }
}
