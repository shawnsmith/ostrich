package com.bazaarvoice.soa.registry;

import com.bazaarvoice.soa.ServiceEndPoint;
import com.bazaarvoice.soa.ServiceEndPointJsonCodec;
import com.bazaarvoice.soa.ServiceRegistry;
import com.bazaarvoice.soa.internal.CuratorConnection;
import com.bazaarvoice.soa.zookeeper.ZooKeeperConnection;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A <code>ServiceRegistry</code> implementation that uses ZooKeeper as its backing data store.
 */
public class ZooKeeperServiceRegistry implements ServiceRegistry
{
    /** The root path in ZooKeeper for where service registrations are stored. */
    @VisibleForTesting
    static final String ROOT_SERVICES_PATH = "services";

    /** Maximum number of bytes that can be stored on a node in ZooKeeper. */
    @VisibleForTesting
    static final int MAX_DATA_SIZE = 1024 * 1024;

    private final CuratorFramework _curator;

    /** The ephemeral data that's been written to ZooKeeper.  Saved in case the connection is lost and then regained. */
    private final Map<String, ZooKeeperPersistentEphemeralNode> _nodes = Maps.newConcurrentMap();

    public ZooKeeperServiceRegistry(ZooKeeperConnection connection) {
        this(((CuratorConnection) checkNotNull(connection)).getCurator());
    }

    @VisibleForTesting
    ZooKeeperServiceRegistry(CuratorFramework curator) {
        checkNotNull(curator);
        checkArgument(curator.isStarted());
        _curator = curator;
    }

    /** {@inheritDoc} */
    @Override
    public void register(ServiceEndPoint endpoint) {
        checkNotNull(endpoint);

        byte[] data = ServiceEndPointJsonCodec.toJson(endpoint).getBytes(Charsets.UTF_8);
        checkState(data.length < MAX_DATA_SIZE, "Serialized form of ServiceEndpoint must be < 1MB.");

        String path = makeEndpointPath(endpoint);
        _nodes.put(path, new ZooKeeperPersistentEphemeralNode(_curator, path, data, CreateMode.EPHEMERAL));
    }

    /** {@inheritDoc} */
    @Override
    public void unregister(ServiceEndPoint endpoint) {
        checkNotNull(endpoint);

        String path = makeEndpointPath(endpoint);
        ZooKeeperPersistentEphemeralNode node = _nodes.remove(path);
        if (node != null) {
            node.close(10, TimeUnit.SECONDS);
        }
    }

    /** Return the curator instance used by this registry. */
    @VisibleForTesting
    CuratorFramework getCurator() {
        return _curator;
    }

    @VisibleForTesting
    String getRegisteredEndpointPath(ServiceEndPoint endpoint) {
        String path = makeEndpointPath(endpoint);
        ZooKeeperPersistentEphemeralNode node = _nodes.get(path);
        return (node != null) ? node.getActualPath() : null;
    }

    /**
     * Construct the path in ZooKeeper to where a service's children live.
     * @param serviceName The name of the service to get the ZooKeeper path for.
     * @return The ZooKeeper path.
     */
    public static String makeServicePath(String serviceName) {
        checkNotNull(serviceName);
        checkArgument(!"".equals(serviceName));
        return ZKPaths.makePath(ROOT_SERVICES_PATH, serviceName);
    }

    /**
     * Convert a <code>ServiceEndpoint</code> into the path in ZooKeeper where it will be registered.
     * @param endpoint The service endpoint to get the ZooKeeper path for.
     * @return The ZooKeeper path.
     */
    private static String makeEndpointPath(ServiceEndPoint endpoint) {
        String servicePath = makeServicePath(endpoint.getServiceName());
        String serviceAddress = endpoint.getServiceAddress();
        return ZKPaths.makePath(servicePath, serviceAddress);
    }
}
