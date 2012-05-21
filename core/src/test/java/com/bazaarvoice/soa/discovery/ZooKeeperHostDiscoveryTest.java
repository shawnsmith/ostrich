package com.bazaarvoice.soa.discovery;

import com.bazaarvoice.soa.HostDiscovery;
import com.bazaarvoice.soa.ServiceEndpoint;
import com.bazaarvoice.soa.registry.ZooKeeperServiceRegistry;
import com.bazaarvoice.soa.test.ZooKeeperTest;
import com.bazaarvoice.soa.zookeeper.ZooKeeperConfiguration;
import com.google.common.collect.Iterables;
import com.google.common.io.Closeables;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ZooKeeperHostDiscoveryTest extends ZooKeeperTest {
    private static final ServiceEndpoint FOO = new ServiceEndpoint("Foo", "server", 8080);

    private ZooKeeperServiceRegistry _registry;
    private ZooKeeperHostDiscovery _discovery;

    @Override
    public void setup() throws Exception {
        super.setup();
        _registry = new ZooKeeperServiceRegistry(newZooKeeperConfiguration());
        _discovery = new ZooKeeperHostDiscovery(newCurator(), FOO.getServiceName());
    }

    @Override
    public void teardown() throws Exception {
        Closeables.closeQuietly(_discovery);
        super.teardown();
    }

    @Test(expected = NullPointerException.class)
    public void testNullConfiguration() {
        new ZooKeeperHostDiscovery((ZooKeeperConfiguration) null, FOO.getServiceName());
    }

    @Test(expected = NullPointerException.class)
    public void testNullServiceName() throws Exception {
        new ZooKeeperHostDiscovery(newCurator(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyServiceName() throws Exception {
        new ZooKeeperHostDiscovery(newCurator(), "");
    }

    @Test
    public void testRegisterService() {
        _registry.register(FOO);
        assertTrue(waitUntilSize(_discovery.getHosts(), 1));
    }

    @Test
    public void testUnregisterService() {
        _registry.register(FOO);
        assertTrue(waitUntilSize(_discovery.getHosts(), 1));

        _registry.unregister(FOO);
        assertTrue(waitUntilSize(_discovery.getHosts(), 0));
    }

    @Test
    public void testServiceRemovedWhenSessionKilled() throws Exception {
        _registry.register(FOO);
        assertTrue(waitUntilSize(_discovery.getHosts(), 1));

        killSession(_discovery.getCurator());

        // The entry gets cleaned up because we've lost contact with ZooKeeper
        assertTrue(waitUntilSize(_discovery.getHosts(), 0));
    }

    @Test
    public void testServiceReRegisteredWhenSessionKilled() throws Exception {
        _registry.register(FOO);
        assertTrue(waitUntilSize(_discovery.getHosts(), 1));

        killSession(_discovery.getCurator());

        // The entry gets cleaned up because we've lost contact with ZooKeeper
        assertTrue(waitUntilSize(_discovery.getHosts(), 0));

        // Then it automatically gets created when the connection is re-established with ZooKeeper
        assertTrue(waitUntilSize(_discovery.getHosts(), 1));
    }

    @Test
    public void testRegisterServiceCallsListener() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        _discovery.addListener(new CountDownListener(latch, null));

        _registry.register(FOO);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testUnregisterServiceCallsListener() throws Exception {
        CountDownLatch addLatch = new CountDownLatch(1);
        CountDownLatch removeLatch = new CountDownLatch(1);
        _discovery.addListener(new CountDownListener(addLatch, removeLatch));

        _registry.register(FOO);
        assertTrue(addLatch.await(10, TimeUnit.SECONDS));

        _registry.unregister(FOO);
        assertTrue(removeLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testRemovedListenerDoesNotSeeEvents() throws Exception {
        CountDownLatch addLatch = new CountDownLatch(1);
        CountDownLatch removeLatch = new CountDownLatch(1);
        _discovery.addListener(new CountDownListener(addLatch, removeLatch));

        HostDiscovery.EndpointListener listener = new FailListener();
        _discovery.addListener(listener);
        _discovery.removeListener(listener);

        _registry.register(FOO);
        assertTrue(addLatch.await(10, TimeUnit.SECONDS));

        _registry.unregister(FOO);
        assertTrue(removeLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testListenerCalledWhenSessionKilled() throws Exception {
        CountDownLatch addLatch = new CountDownLatch(1);
        CountDownLatch removeLatch = new CountDownLatch(1);
        _discovery.addListener(new CountDownListener(addLatch, removeLatch));

        _registry.register(FOO);
        assertTrue(addLatch.await(10, TimeUnit.SECONDS));

        killSession(_discovery.getCurator());

        // The entry gets cleaned up because we've lost contact with ZooKeeper
        assertTrue(removeLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testListenerCalledWhenServiceIsReregisteredAfterSessionKilled() throws Exception {
        CountDownLatch addLatch = new CountDownLatch(1);
        _discovery.addListener(new CountDownListener(addLatch, null));

        _registry.register(FOO);
        assertTrue(addLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch reAddLatch = new CountDownLatch(1);
        CountDownLatch removeLatch = new CountDownLatch(1);
        _discovery.addListener(new CountDownListener(reAddLatch, removeLatch));

        killSession(_discovery.getCurator());

        // The entry gets cleaned up because we've lost contact with ZooKeeper
        assertTrue(removeLatch.await(10, TimeUnit.SECONDS));

        // Then it automatically gets created when the connection is re-established with ZooKeeper
        assertTrue(reAddLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testMultipleListeners() throws Exception {
        CountDownLatch addLatch = new CountDownLatch(2);
        CountDownLatch removeLatch = new CountDownLatch(2);
        _discovery.addListener(new CountDownListener(addLatch, removeLatch));
        _discovery.addListener(new CountDownListener(addLatch, removeLatch));

        _registry.register(FOO);
        assertTrue(addLatch.await(10, TimeUnit.SECONDS));

        _registry.unregister(FOO);
        assertTrue(removeLatch.await(10, TimeUnit.SECONDS));
    }

    private static <T> boolean waitUntilSize(Iterable<T> iterable, int size, long timeout, TimeUnit unit) {
        long start = System.nanoTime();
        while (System.nanoTime() - start <= unit.toNanos(timeout)) {
            if (Iterables.size(iterable) == size) {
                return true;
            }

            Thread.yield();
        }

        return false;
    }

    private static <T> boolean waitUntilSize(Iterable<T> iterable, int size) {
        return waitUntilSize(iterable, size, 10, TimeUnit.SECONDS);
    }

    private static final class CountDownListener implements HostDiscovery.EndpointListener {
        private final CountDownLatch _addLatch;
        private final CountDownLatch _removeLatch;

        public CountDownListener(CountDownLatch addLatch, CountDownLatch removeLatch) {
            _addLatch = addLatch;
            _removeLatch = removeLatch;
        }

        @Override
        public void onEndpointAdded(ServiceEndpoint endpoint) {
            if (_addLatch != null) {
                _addLatch.countDown();
            }
        }

        @Override
        public void onEndpointRemoved(ServiceEndpoint endpoint) {
            if (_removeLatch != null) {
                _removeLatch.countDown();
            }
        }
    }

    private static final class FailListener implements HostDiscovery.EndpointListener {
        @Override
        public void onEndpointAdded(ServiceEndpoint endpoint) {
            fail();
        }

        @Override
        public void onEndpointRemoved(ServiceEndpoint endpoint) {
            fail();
        }
    }
}
