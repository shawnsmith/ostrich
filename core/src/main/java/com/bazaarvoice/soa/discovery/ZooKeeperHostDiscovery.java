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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.api.CuratorEvent;
import com.netflix.curator.framework.api.CuratorListener;
import com.netflix.curator.framework.recipes.cache.ChildData;
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
import static com.google.common.base.Preconditions.checkState;

/**
 * The <code>ZooKeeperHostDiscovery</code> class watches a service path in ZooKeeper and will monitor which hosts are
 * available.  As hosts come and go the results of calling the <code>#getHosts</code> method changes.
 */
public class ZooKeeperHostDiscovery implements HostDiscovery, Closeable {
    private static final int SYNC_TIMEOUT_IN_SECONDS = 10;

    private final CuratorFramework _curator;
    private final Set<ServiceEndpoint> _endpoints;
    private final Set<EndpointListener> _listeners;
    private final PathChildrenCache _pathCache;

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
                .setDaemon(true)
                .build();
        String servicePath = ZooKeeperServiceRegistry.makeServicePath(serviceName);

        _curator = curator;
        _endpoints = Sets.newSetFromMap(Maps.<ServiceEndpoint, Boolean>newConcurrentMap());
        _listeners = Sets.newSetFromMap(Maps.<EndpointListener, Boolean>newConcurrentMap());

        _pathCache = new PathChildrenCache(_curator, servicePath, true, threadFactory);
        try {
            _pathCache.getListenable().addListener(new ServiceListener());
            reload(true);
        } catch (Throwable t) {
            Closeables.closeQuietly(this);
            throw Throwables.propagate(t);
        }
    }

    private synchronized void reload(boolean firstTime) {
        // This must be synchronized so async remove events aren't processed between the rebuild() and adding endpoints.
        // Use synchronous rebuild() instead of asynchronous refresh() so we can tell when it's done.
        // Note: rebuild() doesn't fire events OR remove endpoints.  We'll fire add events ourselves,
        // and just document that removed endpoints aren't necessarily removed...
        try {
            if (firstTime) {
                _pathCache.start(true);  // true means call rebuild() after other startup activity is done
            } else {
                _pathCache.rebuild();
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        for (ChildData childData : _pathCache.getCurrentData()) {
            addEndpoint(toEndpoint(childData));
        }
    }

    @Override
    public void refresh() {
        // Call sync() to ensure reads retrieve the most current state.
        sync();
        reload(false);
    }

    private void sync() {
        // Curator sync() is always a background operation.  Setup a listener to determine when it finishes.
        // Note that the sync() callback executes on the event thread that is also used for watcher callbacks
        // which is NOT the same thread that's used for PatchChildrenCache listener callbacks.  Beware races.
        final CountDownLatch latch = new CountDownLatch(1);
        final Object context = new Object();
        CuratorListener listener = new CuratorListener() {
            @Override
            public void eventReceived(CuratorFramework client, CuratorEvent event) throws Exception {
                if (event.getContext() == context) {
                    latch.countDown();
                }
            }
        };
        _curator.getCuratorListenable().addListener(listener);
        try {
            // The path arg to sync is ignored.  It's a placeholder to allow future functionality.
            _curator.sync("/", context);

            // Wait for sync to complete.
            boolean success;
            try {
                success = latch.await(SYNC_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                success = false;
            }
            checkState(success);
        } finally {
            _curator.getCuratorListenable().removeListener(listener);
        }
    }

    @Override
    public Iterable<ServiceEndpoint> getHosts() {
        return _endpoints;
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
    }

    @VisibleForTesting
    CuratorFramework getCurator() {
        return _curator;
    }

    private synchronized void addEndpoint(ServiceEndpoint endpoint) {
        // synchronize the modification of _endpoints and firing of events so listeners always receive events in the
        // order they occur.
        if (_endpoints.add(endpoint)) {
            fireAddEvent(endpoint);
        }
    }

    private synchronized void removeEndpoint(ServiceEndpoint endpoint) {
        // synchronize the modification of _endpoints and firing of events so listeners always receive events in the
        // order they occur.
        if (_endpoints.remove(endpoint)) {
            fireRemoveEvent(endpoint);
        }
    }

    private synchronized void clearEndpoints() {
        // synchronize the modification of _endpoints and firing of events so listeners always receive events in the
        // order they occur.
        Collection<ServiceEndpoint> endpoints = ImmutableList.copyOf(_endpoints);
        _endpoints.clear();
        for (ServiceEndpoint endpoint : endpoints) {
            fireRemoveEvent(endpoint);
        }
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

    private ServiceEndpoint toEndpoint(ChildData data) {
        String json = new String(data.getData(), Charsets.UTF_8);
        return ServiceEndpoint.fromJson(json);
    }

    /** A curator <code>PathChildrenCacheListener</code> */
    private final class ServiceListener implements PathChildrenCacheListener {
        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
            if (event.getType() == PathChildrenCacheEvent.Type.RESET) {
                clearEndpoints();
                return;
            }

            ServiceEndpoint endpoint = toEndpoint(event.getData());

            switch (event.getType()) {
                case CHILD_ADDED:
                    addEndpoint(endpoint);
                    break;

                case CHILD_REMOVED:
                    removeEndpoint(endpoint);
                    break;

                case CHILD_UPDATED:
                    // TODO: This should never happen.  Assert?  Log?
                    break;
            }
        }
    }
}
