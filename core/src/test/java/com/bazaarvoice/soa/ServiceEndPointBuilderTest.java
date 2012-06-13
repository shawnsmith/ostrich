package com.bazaarvoice.soa;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ServiceEndPointBuilderTest {
    @Test(expected = IllegalStateException.class)
    public void testMissingServiceName() {
        new ServiceEndPointBuilder()
                .withHostname("localhost")
                .withPort(80)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingHostname() {
        new ServiceEndPointBuilder()
                .withServiceName("service")
                .withPort(80)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingPort() {
        new ServiceEndPointBuilder()
                .withServiceName("service")
                .withHostname("localhost")
                .build();
    }

    @Test
    public void testServiceName() {
        ServiceEndPoint endpoint = new ServiceEndPointBuilder()
                .withServiceName("service")
                .withHostname("localhost")
                .withPort(80)
                .build();
        assertEquals("service", endpoint.getServiceName());
    }

    @Test
    public void testHostname() {
        ServiceEndPoint endpoint = new ServiceEndPointBuilder()
                .withServiceName("service")
                .withHostname("localhost")
                .withPort(80)
                .build();
        assertEquals("localhost", endpoint.getHostname());
    }

    @Test
    public void testPort() {
        ServiceEndPoint endpoint = new ServiceEndPointBuilder()
                .withServiceName("service")
                .withHostname("localhost")
                .withPort(80)
                .build();
        assertEquals(80, endpoint.getPort());
    }

    @Test
    public void testServiceAddress() {
        ServiceEndPoint endpoint = new ServiceEndPointBuilder()
                .withServiceName("service")
                .withHostname("localhost")
                .withPort(80)
                .build();
        assertEquals("localhost:80", endpoint.getServiceAddress());
    }

    @Test
    public void testNoPayload() {
        ServiceEndPoint endpoint = new ServiceEndPointBuilder()
                .withServiceName("service")
                .withHostname("localhost")
                .withPort(80)
                .build();
        assertNull(endpoint.getPayload());
    }

    @Test
    public void testEmptyPayload() {
        ServiceEndPoint endpoint = new ServiceEndPointBuilder()
                .withServiceName("service")
                .withHostname("localhost")
                .withPort(80)
                .withPayload("")
                .build();
        assertEquals("", endpoint.getPayload());
    }

    @Test
    public void testPayload() {
        ServiceEndPoint endpoint = new ServiceEndPointBuilder()
                .withServiceName("service")
                .withHostname("localhost")
                .withPort(80)
                .withPayload("payload")
                .build();
        assertEquals("payload", endpoint.getPayload());
    }

    @Test
    public void testInvalidServiceNames() {
        String[] invalidNames = new String[] {"Foo$Bar", "%", "a:b", "a@b", "!", null, ""};

        for (String name : invalidNames) {
            try {
                new ServiceEndPointBuilder().withServiceName(name);
                fail(name + " was allowed");
            } catch (IllegalArgumentException e) {
                // Expected
            } catch (Throwable t) {
                fail(name + " threw " + t.getMessage());
            }
        }
    }

    @Test
    public void testInvalidHostNames() {
        String[] invalidHostNames = new String[] {null, ""};

        for (String hostname : invalidHostNames) {
            try {
                new ServiceEndPointBuilder().withHostname(hostname);
                fail(hostname + " was allowed");
            } catch (IllegalArgumentException e) {
                // Expected
            } catch (Throwable t) {
                fail(hostname + " threw " + t.getMessage());
            }
        }
    }

    @Test
    public void testInvalidPorts() {
        int[] invalidPorts = new int[] {-2, -1, 0, 65536};

        for (int port : invalidPorts) {
            try {
                new ServiceEndPointBuilder().withPort(port);
                fail(port + " was allowed");
            } catch (IllegalArgumentException e) {
                // Expected
            } catch (Throwable t) {
                fail(port + " threw " + t.getMessage());
            }
        }
    }
}
