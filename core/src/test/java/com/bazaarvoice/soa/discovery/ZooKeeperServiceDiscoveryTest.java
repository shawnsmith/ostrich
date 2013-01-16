package com.bazaarvoice.soa.discovery;

import com.bazaarvoice.zookeeper.ZooKeeperConnection;
import com.bazaarvoice.zookeeper.recipes.discovery.ZooKeeperNodeDiscovery;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZooKeeperServiceDiscoveryTest {
    private ZooKeeperServiceDiscovery _discovery;
    private ZooKeeperNodeDiscovery<Void> _nodeDiscovery;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception {
        _nodeDiscovery = mock(ZooKeeperNodeDiscovery.class);
        _discovery = new ZooKeeperServiceDiscovery(_nodeDiscovery);
    }

    @After
    public void teardown() throws Exception {
        Closeables.closeQuietly(_discovery);
    }

    @Test(expected = NullPointerException.class)
    public void testNullConnection() {
        new ZooKeeperServiceDiscovery((ZooKeeperConnection) null);
    }

    @Test
    public void testNoServices() {
        when(_nodeDiscovery.getNodes()).thenReturn(Maps.<String, Void>newHashMap());
        assertEquals(0, Iterables.size(_discovery.getServices()));
    }

    @Test
    public void testOneService() {
        registerServices("service");

        Iterable<String> services = _discovery.getServices();
        assertEquals(1, Iterables.size(services));
        assertEquals("service", Iterables.get(services, 0));
    }

    @Test
    public void testMultipleServices() {
        registerServices("service1", "service2");

        Iterable<String> services = _discovery.getServices();
        assertEquals(2, Iterables.size(services));
        assertTrue(Iterables.contains(services, "service1"));
        assertTrue(Iterables.contains(services, "service2"));
    }

    @Test
    public void testClosesNodeDiscovery() throws IOException {
        _discovery.close();
        verify(_nodeDiscovery).close();
    }

    private void registerServices(String... services) {
        Map<String, Void> serviceMap = Maps.newHashMap();
        for (String service : services) {
            serviceMap.put(service, null);
        }

        when(_nodeDiscovery.getNodes()).thenReturn(serviceMap);
    }
}
