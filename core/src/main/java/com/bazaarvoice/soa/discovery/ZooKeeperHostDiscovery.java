package com.bazaarvoice.soa.discovery;

import com.bazaarvoice.soa.HostDiscovery;
import com.bazaarvoice.soa.ServiceInstance;
import com.bazaarvoice.soa.registry.ZooKeeperServiceRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
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

/**
 * The <code>ZooKeeperHostDiscovery</code> class watches a service path in ZooKeeper and will monitor which hosts are
 * available.  As hosts come and go the results of calling the <code>#getHosts</code> method changes.
 */
public class ZooKeeperHostDiscovery implements HostDiscovery, Closeable {
    private final CuratorFramework _curator;
    private final PathChildrenCache _pathCache;
    private final ConcurrentMap<ServiceInstance, Boolean> _instanceMap = Maps.newConcurrentMap();
    private final Iterable<ServiceInstance> _instances = Iterables.unmodifiableIterable(_instanceMap.keySet());

    public ZooKeeperHostDiscovery(CuratorFramework curator, String serviceName) {
        Preconditions.checkNotNull(curator);
        Preconditions.checkNotNull(serviceName);
        Preconditions.checkArgument(curator.isStarted());
        Preconditions.checkArgument(!"".equals(serviceName));

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
    public Iterable<ServiceInstance> getHosts() {
        return _instances;
    }

    @Override
    public void close() throws IOException {
        _pathCache.close();
        _instanceMap.clear();
    }

    @VisibleForTesting
    CuratorFramework getCurator() {
        return _curator;
    }

    /**
     * A curator <code>PathChildrenCacheListener</code>
     */
    private final class ServiceListener implements PathChildrenCacheListener {
        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
            if (event.getType() == PathChildrenCacheEvent.Type.RESET) {
                _instanceMap.clear();
            }

            String json = new String(event.getData().getData(), Charsets.UTF_16);
            ServiceInstance instance = ServiceInstance.fromJson(json);

            switch (event.getType()) {
                case CHILD_ADDED:
                    _instanceMap.put(instance, Boolean.TRUE);
                    break;

                case CHILD_REMOVED:
                    _instanceMap.remove(instance);
                    break;

                case CHILD_UPDATED:
                    // TODO: This should never happen.  Assert?  Log?
                    break;
            }
        }
    }
}
