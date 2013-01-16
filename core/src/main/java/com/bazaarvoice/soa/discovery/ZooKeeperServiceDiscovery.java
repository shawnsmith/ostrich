package com.bazaarvoice.soa.discovery;

import com.bazaarvoice.soa.ServiceDiscovery;
import com.bazaarvoice.soa.registry.ZooKeeperServiceRegistry;
import com.bazaarvoice.zookeeper.ZooKeeperConnection;
import com.bazaarvoice.zookeeper.recipes.discovery.NodeDataParser;
import com.bazaarvoice.zookeeper.recipes.discovery.ZooKeeperNodeDiscovery;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;

import java.io.IOException;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The <code>ZooKeeperHostDiscovery</code> class encapsulates ZooKeeperNodeDiscovery which
 * watches a the root service path in ZooKeeper and will monitor which services are
 * known.  As services come and go the results of calling the <code>#getServices</code>
 * method changes.
 * <p/>
 * NOTE: It's possible that a returned service doesn't have any {@code ServiceEndPoint}
 * instances currently registered.
 */
public class ZooKeeperServiceDiscovery implements ServiceDiscovery {
    /** The path in ZooKeeper where ostrich services are registered. */
    private static final String SERVICE_PATH = ZooKeeperServiceRegistry.rootServicePath();

    /** Node data parser that always returns null.  Ostrich service nodes don't have data in them. */
    private static final NodeDataParser<Void> NULL_NODE_DATA_PARSER = new NodeDataParser<Void>() {
        @Override
        public Void parse(String path, byte[] nodeData) {
            return null;
        }
    };

    private final ZooKeeperNodeDiscovery<?> _nodeDiscovery;

    public ZooKeeperServiceDiscovery(ZooKeeperConnection connection) {
        this(new ZooKeeperNodeDiscovery<Void>(connection, SERVICE_PATH, NULL_NODE_DATA_PARSER));
    }

    @VisibleForTesting
    ZooKeeperServiceDiscovery(ZooKeeperNodeDiscovery<?> nodeDiscovery) {
        _nodeDiscovery = checkNotNull(nodeDiscovery);
        _nodeDiscovery.start();
    }

    @Override
    public Iterable<String> getServices() {
        Map<String, ?> nodes = _nodeDiscovery.getNodes();
        return Iterables.unmodifiableIterable(nodes.keySet());
    }

    @Override
    public void close() throws IOException {
        _nodeDiscovery.close();
    }
}
