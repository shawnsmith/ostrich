package com.bazaarvoice.soa.pool;

import com.bazaarvoice.soa.PartitionContext;
import com.bazaarvoice.soa.partition.PartitionKey;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AnnotationPartitionContextSupplierTest {
    @Test
    public void testImplExtendsIfc() {
        new AnnotationPartitionContextSupplier(List.class, ArrayList.class);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testImplDoesntExtendsIfc() {
        new AnnotationPartitionContextSupplier((Class) ArrayList.class, List.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testDuplicateKey() {
        new AnnotationPartitionContextSupplier(MyService.class, MyServiceDup.class);
    }

    @Test
    public void testNoArgs() throws Exception {
        PartitionContextSupplier contextSupplier =
                new AnnotationPartitionContextSupplier(MyService.class, MyServiceImpl.class);
        PartitionContext partitionContext = contextSupplier.forCall(MyService.class.getMethod("noArgs"));

        assertTrue(partitionContext.isEmpty());
    }

    @Test
    public void testSingleArg() throws Exception {
        PartitionContextSupplier contextSupplier =
                new AnnotationPartitionContextSupplier(MyService.class, MyServiceImpl.class);

        // Call oneArg(String)
        assertEquals(ImmutableMap.of("", "a"),
                contextSupplier.forCall(MyService.class.getMethod("oneArg", String.class), "a"));

        // Call oneArg(int)
        assertEquals(ImmutableMap.of("n", 5),
                contextSupplier.forCall(MyService.class.getMethod("oneArg", int.class), 5));

        // Call oneArg(boolean)
        assertTrue(contextSupplier.forCall(MyService.class.getMethod("oneArg", boolean.class), false).isEmpty());
    }

    @Test
    public void testTwoArg() throws Exception {
        PartitionContextSupplier contextSupplier =
                new AnnotationPartitionContextSupplier(MyService.class, MyServiceImpl.class);

        assertEquals(ImmutableMap.of("", "a"),
                contextSupplier.forCall(MyService.class.getMethod("twoArg", int.class, String.class), 1, "a"));
    }

    /**
     * Multiple arguments have @PartitionKey annotations.
     */
    @Test
    public void testMultiplePartitionKeys() throws Exception {
        PartitionContextSupplier contextSupplier =
                new AnnotationPartitionContextSupplier(MyService.class, MyServiceImpl.class);

        assertEquals(ImmutableMap.<String, Object>of("", "one", "b", "two", "c", "three"), contextSupplier.forCall(
                MyService.class.getMethod("threeArg", String.class, String.class, String.class),
                "one", "two", "three"));
    }

    /**
     * Implementation return type is more specific than the interface return type (ArrayList vs List)
     */
    @Test
    public void testCovariant() throws Exception {
        PartitionContextSupplier contextSupplier =
                new AnnotationPartitionContextSupplier(MyService.class, MyServiceImpl.class);
        PartitionContext partitionContext = contextSupplier.forCall(
                MyService.class.getMethod("covariant", String.class),
                "value");

        assertEquals(ImmutableMap.of("", "value"), partitionContext);
    }

    private static interface MyService {
        void noArgs();
        void oneArg(String string);
        void oneArg(int num);
        void oneArg(boolean flag);
        void twoArg(int num, String string);
        void threeArg(String a1, String a2, String a3);
        List covariant(String string);
    }

    private static class MyServiceImpl implements MyService {
        @Override
        public void noArgs() {}
        @Override
        public void oneArg(@PartitionKey String string) {}
        @Override
        public void oneArg(@PartitionKey("n") int num) {}
        @Override
        public void oneArg(boolean flag) {}
        @Override
        public void twoArg(int num, @PartitionKey String string) {}
        @Override
        public void threeArg(@PartitionKey String x, @PartitionKey("b") String y, @PartitionKey("c") String z) {}
        @Override
        public ArrayList covariant(@PartitionKey String string) {return null;}
    }

    private static class MyServiceDup extends MyServiceImpl {
        @Override
        public void twoArg(@PartitionKey int num, @PartitionKey String string) {}
    }
}
