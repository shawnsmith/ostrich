package com.bazaarvoice.soa.registry;

import com.bazaarvoice.soa.ServiceEndpoint;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.retry.RetryNTimes;
import com.netflix.curator.test.KillSession;
import com.netflix.curator.test.TestingServer;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ZooKeeperServiceRegistryStandaloneTest {
    public static void main(String[] args) throws Exception {
        while (true) {
            ZooKeeperServiceRegistryStandaloneTest test = new ZooKeeperServiceRegistryStandaloneTest();
            try {
                test.run();
            } finally {
                test.cleanup();
            }
        }
    }

    private static final ServiceEndpoint FOO = new ServiceEndpoint("Foo", "server", 8080);

    private final TestingServer _zooKeeperServer;
    private final ZooKeeperServiceRegistry _registry;
    private final CuratorFramework _curator;
    private final Set<CuratorFramework> _curatorInstances = Sets.newSetFromMap(Maps.<CuratorFramework, Boolean>newConcurrentMap());

    public ZooKeeperServiceRegistryStandaloneTest() throws Exception {
        _zooKeeperServer = new TestingServer();
        _registry = new ZooKeeperServiceRegistry(newCurator());
        _curator = newCurator();
    }

    private void run() throws Exception {
        System.out.println("STARTING...");
        try {
            String path = ZooKeeperServiceRegistry.makeEndpointPath(FOO);
            System.out.println("CONTINUING...");
            Assert.assertTrue(_registry.register(FOO));

            CountDownLatch deletionLatch = new CountDownLatch(1);
            _curator.checkExists().usingWatcher(new CountDownWatcher(deletionLatch)).forPath(path);

            // Kill the registry's session, thus cleaning up the node...
            killSession(_registry.getCurator());

            // Make sure the node ended up getting deleted...
            System.out.println("CONTINUING...");
            Assert.assertTrue(deletionLatch.await(10, TimeUnit.SECONDS));

            // Now put a watch in the background looking to see if it gets created...
            CountDownLatch creationLatch = new CountDownLatch(1);
            Stat stat = _curator.checkExists().usingWatcher(new CountDownWatcher(creationLatch)).forPath(path);

            // It's possible the node already got re-created so check if it exists right now before blocking to wait
            // for it to be created.
            if (stat != null) {
                // We're done, no need to wait for the latch...
                System.out.println("FINISHED EARLY.");
                return;
            }

            // We didn't find it, so wait for the latch to fire off marking it's creation...
            System.out.println("CONTINUING...");
            Assert.assertTrue(creationLatch.await(10, TimeUnit.SECONDS));
        } finally {
            System.out.println("FINISHED.");
        }
    }

    private void cleanup() throws Exception {
        for (CuratorFramework curator : _curatorInstances) {
            Closeables.closeQuietly(curator);
        }

        _curatorInstances.clear();
        Closeables.closeQuietly(_zooKeeperServer);
    }

    public void killSession(CuratorFramework curator) throws Exception {
        System.out.println("Killing session");
        KillSession.kill(curator.getZookeeperClient().getZooKeeper(), _zooKeeperServer.getConnectString());
        System.out.println("Session killed");
    }

    public CuratorFramework newCurator() throws Exception {
        return newCurator(new RetryNTimes(0, 0));
    }

    private CuratorFramework newCurator(com.netflix.curator.RetryPolicy retryPolicy) throws Exception {
        Assert.assertNotNull("ZooKeeper testing server is null, did you forget to call super.setup()", _zooKeeperServer);

        System.out.println("Building CuratorFramework...");
        final CuratorFramework curator = CuratorFrameworkFactory.builder()
                .connectString(_zooKeeperServer.getConnectString())
                .retryPolicy(retryPolicy)
                .build();
        System.out.println("Done building CuratorFramework.");

        /*
        final CountDownLatch latch = new CountDownLatch(1);
        curator.getConnectionStateListenable().addListener(new ConnectionStateListener() {
            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                if (newState == ConnectionState.CONNECTED) {
                    latch.countDown();
                    curator.getConnectionStateListenable().removeListener(this);
                }
            }
        });
        */

        System.out.println("Starting CuratorFramework...");
        curator.start();
        System.out.println("Done starting CuratorFramework.");

        /*
        // Wait until curator is connected...if it's not connected after 10 seconds we'll throw an exception
        latch.await(10, TimeUnit.SECONDS);
        */

        _curatorInstances.add(curator);

        return curator;
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
