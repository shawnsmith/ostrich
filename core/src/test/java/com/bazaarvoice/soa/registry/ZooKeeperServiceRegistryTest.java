package com.bazaarvoice.soa.registry;

import com.bazaarvoice.soa.ServiceInstance;
import com.bazaarvoice.soa.test.ZooKeeperTest;
import com.bazaarvoice.soa.zookeeper.ZooKeeperConfiguration;
import com.google.common.net.HostAndPort;
import com.netflix.curator.framework.CuratorFramework;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ZooKeeperServiceRegistryTest extends ZooKeeperTest {
    private static final ServiceInstance FOO = new ServiceInstance("Foo", HostAndPort.fromString("server:8080"));

    /** All verifications are done using this curator instance to ensure session isolation from the registry. */
    private CuratorFramework _curator;

    private ZooKeeperServiceRegistry _registry;

    @Before
    public void setup() throws Exception {
        super.setup();
        _curator = newCurator();
        _registry = new ZooKeeperServiceRegistry(newCurator());
    }

    @Test(expected = NullPointerException.class)
    public void testNullConfig() throws Exception {
        new ZooKeeperServiceRegistry((ZooKeeperConfiguration) null);
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterNullService() throws Exception {
        _registry.register(null);
    }

    @Test(expected = NullPointerException.class)
    public void testUnregisterNullService() throws Exception {
        _registry.unregister(null);
    }

    @Test(expected = NullPointerException.class)
    public void testMakeInstancePathNullService() throws Exception {
        ZooKeeperServiceRegistry.makeInstancePath(null);
    }

    @Test(expected = NullPointerException.class)
    public void testMakeServicePathNullName() throws Exception {
        ZooKeeperServiceRegistry.makeServicePath(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMakeServicePathEmptyName() throws Exception {
        ZooKeeperServiceRegistry.makeServicePath("");
    }

    @Test
    public void testRegister() throws Exception {
        _registry.register(FOO);
        assertRegistered(FOO);
    }

    @Test
    public void testDuplicateRegister() throws Exception {
        _registry.register(FOO);
        _registry.register(FOO);
        assertRegistered(FOO);
    }

    @Test
    public void testUnregister() throws Exception {
        _registry.register(FOO);
        _registry.unregister(FOO);
        assertNotRegistered(FOO);
    }

    @Test
    public void testUnregisterWithoutFirstRegistering() throws Exception {
        _registry.unregister(FOO);
        assertNotRegistered(FOO);
    }

    @Test
    public void testDuplicateUnregister() throws Exception {
        _registry.register(FOO);
        _registry.unregister(FOO);
        _registry.unregister(FOO);
        assertNotRegistered(FOO);
    }

    @Test
    public void testServiceNodeIsDeletedWhenSessionDisconnects() throws Exception {
        String path = ZooKeeperServiceRegistry.makeInstancePath(FOO);
        _registry.register(FOO);

        CountDownLatch latch = new CountDownLatch(1);
        _curator.checkExists().usingWatcher(new CountDownWatcher(latch)).forPath(path);

        // Kill the registry's ZooKeeper session.  That should force the ephemeral node that it created to be
        // automatically cleaned up.
        killSession(_registry.getCurator());

        // Wait for the latch to be called up to 10 seconds.  This should be plenty of time for the node to be removed,
        // if it's not called by then, fail the test.
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testServiceNodeIsRecreatedWhenSessionReconnects() throws Exception {
        String path = ZooKeeperServiceRegistry.makeInstancePath(FOO);
        _registry.register(FOO);

        CountDownLatch deletionLatch = new CountDownLatch(1);
        _curator.checkExists().usingWatcher(new CountDownWatcher(deletionLatch)).forPath(path);

        // Kill the registry's session, thus cleaning up the node...
        killSession(_registry.getCurator());

        // Make sure the node ended up getting deleted...
        assertTrue(deletionLatch.await(10, TimeUnit.SECONDS));

        // Now put a watch in the background looking to see if it gets created...
        CountDownLatch creationLatch = new CountDownLatch(1);
        Stat stat = _curator.checkExists().usingWatcher(new CountDownWatcher(creationLatch)).forPath(path);

        // It's possible the node already got re-created so check if it exists right now before blocking to wait
        // for it to be created.
        if (stat != null) {
            // We're done, no need to wait for the latch...
            return;
        }

        // We didn't find it, so wait for the latch to fire off marking it's creation...
        assertTrue(creationLatch.await(10, TimeUnit.SECONDS));
    }

    private void assertRegistered(ServiceInstance instance) throws Exception {
        assertRegistered(instance, _curator);
    }

    private void assertRegistered(ServiceInstance instance, CuratorFramework curator) throws Exception {
        String path = ZooKeeperServiceRegistry.makeInstancePath(instance);
        Stat stat = curator.checkExists().forPath(path);
        assertNotNull(stat);
    }

    private void assertNotRegistered(ServiceInstance instance) throws Exception {
        assertNotRegistered(instance, _curator);
    }

    private void assertNotRegistered(ServiceInstance instance, CuratorFramework curator) throws Exception {
        String path = ZooKeeperServiceRegistry.makeInstancePath(instance);
        Stat stat = curator.checkExists().forPath(path);
        assertNull(stat);
    }

    private static final class CountDownWatcher implements Watcher {
        private final CountDownLatch _latch;

        public CountDownWatcher(CountDownLatch latch) {
            _latch = latch;
        }

        @Override
        public void process(WatchedEvent event) {
            _latch.countDown();
        }
    }
}
