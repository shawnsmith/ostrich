package com.bazaarvoice.soa.discovery;

import com.bazaarvoice.soa.discovery.ZooKeeperServiceDiscovery.ServiceNameParser;
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
    private ZooKeeperNodeDiscovery<String> _nodeDiscovery;

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
        setServices();
        assertEquals(0, Iterables.size(_discovery.getServices()));
    }

    @Test
    public void testOneService() {
        setServices("service");

        Iterable<String> services = _discovery.getServices();
        assertEquals(1, Iterables.size(services));
        assertEquals("service", Iterables.get(services, 0));
    }

    @Test
    public void testMultipleServices() {
        setServices("service1", "service2");

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

    @Test
    public void testServiceNameParserWithNoNamespace() {
        ServiceNameParser parser = new ServiceNameParser("");

        String service = parser.parse("/ostrich/service", null);
        assertEquals("service", service);
    }

    @Test
    public void testServiceNameParserWithRootNamespace() {
        ServiceNameParser parser = new ServiceNameParser("/");

        String service = parser.parse("/ostrich/service", null);
        assertEquals("service", service);
    }


    @Test
    public void testServiceNameParserWithNamespace() {
        ServiceNameParser parser = new ServiceNameParser("/namespace");

        String service = parser.parse("/namespace/ostrich/service", null);
        assertEquals("service", service);
    }

    private void setServices(String... services) {
        Map<String, String> serviceMap = Maps.newHashMap();
        for (String service : services) {
            serviceMap.put(service, service);
        }

        when(_nodeDiscovery.getNodes()).thenReturn(serviceMap);
    }
}
