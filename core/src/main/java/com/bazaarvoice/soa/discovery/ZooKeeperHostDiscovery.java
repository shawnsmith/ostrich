package com.bazaarvoice.soa.discovery;

import com.bazaarvoice.soa.HostDiscovery;
import com.bazaarvoice.soa.ServiceEndpoint;
import com.bazaarvoice.soa.internal.CuratorConfiguration;
import com.bazaarvoice.soa.registry.ZooKeeperServiceRegistry;
import com.bazaarvoice.soa.zookeeper.ZooKeeperConfiguration;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.recipes.cache.PathChildrenCache;
import com.netflix.curator.framework.recipes.cache.PathChildrenCacheEvent;
import com.netflix.curator.framework.recipes.cache.PathChildrenCacheListener;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The <code>ZooKeeperHostDiscovery</code> class watches a service path in ZooKeeper and will monitor which hosts are
 * available.  As hosts come and go the results of calling the <code>#getHosts</code> method changes.
 */
public class ZooKeeperHostDiscovery implements HostDiscovery, Closeable {
    private static final long WAIT_FOR_DATA_TIMEOUT_IN_SECONDS = 10;

    private final CuratorFramework _curator;
    private final Set<ServiceEndpoint> _endpoints;
    private final Set<EndpointListener> _listeners;
    private final PathChildrenCache _pathCache;

    public ZooKeeperHostDiscovery(ZooKeeperConfiguration config, String serviceName) {
        this(((CuratorConfiguration) checkNotNull(config)).getCurator(), serviceName, true);
    }

    @VisibleForTesting
    ZooKeeperHostDiscovery(CuratorFramework curator, String serviceName, boolean waitForData) {
        checkNotNull(curator);
        checkNotNull(serviceName);
        checkArgument(curator.isStarted());
        checkArgument(!"".equals(serviceName));

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(getClass().getSimpleName() + "(" + serviceName + ")-%d")
                .setDaemon(true)
                .build();
        String servicePath = ZooKeeperServiceRegistry.makeServicePath(serviceName);

        _curator = curator;
        _endpoints = Sets.newSetFromMap(Maps.<ServiceEndpoint, Boolean>newConcurrentMap());
        _listeners = Sets.newSetFromMap(Maps.<EndpointListener, Boolean>newConcurrentMap());

        _pathCache = new PathChildrenCache(_curator, servicePath, true, threadFactory);
        _pathCache.getListenable().addListener(new ServiceListener());

        PathChildrenCacheListener waitListener = null;
        CountDownLatch waitLatch = new CountDownLatch(1);
        if (waitForData) {
            // It takes a little bit of time before the Path Cache sees data.  We're going to attempt to block until we
            // know for certain that the cache has read some data from ZooKeeper, but we don't want to block
            // indefinitely because it's possible that there's just no data available.  We'll use a latch to wait just
            // until data is available or up to some maximum amount otherwise.  At that point host discovery should be
            // usable.
            waitListener = new LatchListener(waitLatch);
            _pathCache.getListenable().addListener(waitListener);
        }

        try {
            _pathCache.start();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        if (waitForData) {
            try {
                waitLatch.await(WAIT_FOR_DATA_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            }

            // Remove the listener we registered above...
            _pathCache.getListenable().removeListener(waitListener);
        }
    }

    @Override
    public Iterable<ServiceEndpoint> getHosts() {
        return Iterables.unmodifiableIterable(_endpoints);
    }

    @Override
    public void addListener(EndpointListener listener) {
        _listeners.add(listener);
    }

    @Override
    public void removeListener(EndpointListener listener) {
        _listeners.remove(listener);
    }

    @Override
    public void close() throws IOException {
        _pathCache.close();
        _endpoints.clear();
    }

    @VisibleForTesting
    CuratorFramework getCurator() {
        return _curator;
    }

    private void fireAddEvent(ServiceEndpoint endpoint) {
        for (EndpointListener listener : _listeners) {
            listener.onEndpointAdded(endpoint);
        }
    }

    private void fireRemoveEvent(ServiceEndpoint endpoint) {
        for (EndpointListener listener : _listeners) {
            listener.onEndpointRemoved(endpoint);
        }
    }

    /** A curator <code>PathChildrenCacheListener</code> */
    private final class ServiceListener implements PathChildrenCacheListener {
        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
            if (event.getType() == PathChildrenCacheEvent.Type.RESET) {
                Collection<ServiceEndpoint> endpoints = ImmutableList.copyOf(_endpoints);
                _endpoints.clear();
                for (ServiceEndpoint endpoint : endpoints) {
                    fireRemoveEvent(endpoint);
                }
                return;
            }

            String json = new String(event.getData().getData(), Charsets.UTF_16);
            ServiceEndpoint endpoint = ServiceEndpoint.fromJson(json);

            switch (event.getType()) {
                case CHILD_ADDED:
                    _endpoints.add(endpoint);
                    fireAddEvent(endpoint);
                    break;

                case CHILD_REMOVED:
                    _endpoints.remove(endpoint);
                    fireRemoveEvent(endpoint);
                    break;

                case CHILD_UPDATED:
                    // TODO: This should never happen.  Assert?  Log?
                    break;
            }
        }
    }

    private static final class LatchListener implements PathChildrenCacheListener {
        private final CountDownLatch _latch;

        public LatchListener(CountDownLatch latch) {
            _latch = checkNotNull(latch);
        }

        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
            if (event.getType() != PathChildrenCacheEvent.Type.RESET) {
                _latch.countDown();
            }
        }
    }
}
