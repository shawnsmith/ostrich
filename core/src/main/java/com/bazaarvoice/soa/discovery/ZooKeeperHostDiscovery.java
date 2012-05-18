package com.bazaarvoice.soa.discovery;

import com.bazaarvoice.soa.HostDiscovery;
import com.bazaarvoice.soa.ServiceEndpoint;
import com.bazaarvoice.soa.internal.CuratorConfiguration;
import com.bazaarvoice.soa.registry.ZooKeeperServiceRegistry;
import com.bazaarvoice.soa.zookeeper.ZooKeeperConfiguration;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.recipes.cache.PathChildrenCache;
import com.netflix.curator.framework.recipes.cache.PathChildrenCacheEvent;
import com.netflix.curator.framework.recipes.cache.PathChildrenCacheListener;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The <code>ZooKeeperHostDiscovery</code> class watches a service path in ZooKeeper and will monitor which hosts are
 * available.  As hosts come and go the results of calling the <code>#getHosts</code> method changes.
 */
public class ZooKeeperHostDiscovery implements HostDiscovery, Closeable {
    private final CuratorFramework _curator;
    private final PathChildrenCache _pathCache;
    private final ConcurrentMap<ServiceEndpoint, Boolean> _endpointMap = Maps.newConcurrentMap();
    private final Iterable<ServiceEndpoint> _endpoints = Iterables.unmodifiableIterable(_endpointMap.keySet());

    public ZooKeeperHostDiscovery(ZooKeeperConfiguration config, String serviceName) {
        this(((CuratorConfiguration) checkNotNull(config)).getCurator(), serviceName);
    }

    @VisibleForTesting
    ZooKeeperHostDiscovery(CuratorFramework curator, String serviceName) {
        checkNotNull(curator);
        checkNotNull(serviceName);
        checkArgument(curator.isStarted());
        checkArgument(!"".equals(serviceName));

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(getClass().getSimpleName() + "(" + serviceName + ")-%d")
                .build();
        String servicePath = ZooKeeperServiceRegistry.makeServicePath(serviceName);

        _curator = curator;
        _pathCache = new PathChildrenCache(_curator, servicePath, true, threadFactory);
        _pathCache.getListenable().addListener(new ServiceListener());

        try {
            _pathCache.start();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Iterable<ServiceEndpoint> getHosts() {
        return _endpoints;
    }

    @Override
    public void close() throws IOException {
        _pathCache.close();
        _endpointMap.clear();
    }

    @VisibleForTesting
    CuratorFramework getCurator() {
        return _curator;
    }

    /** A curator <code>PathChildrenCacheListener</code> */
    private final class ServiceListener implements PathChildrenCacheListener {
        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
            if (event.getType() == PathChildrenCacheEvent.Type.RESET) {
                _endpointMap.clear();
            }

            String json = new String(event.getData().getData(), Charsets.UTF_16);
            ServiceEndpoint endpoint = ServiceEndpoint.fromJson(json);

            switch (event.getType()) {
                case CHILD_ADDED:
                    _endpointMap.put(endpoint, Boolean.TRUE);
                    break;

                case CHILD_REMOVED:
                    _endpointMap.remove(endpoint);
                    break;

                case CHILD_UPDATED:
                    // TODO: This should never happen.  Assert?  Log?
                    break;
            }
        }
    }
}
