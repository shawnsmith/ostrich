package com.bazaarvoice.soa;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ServiceEndPointTest {
    @Test
    public void testEqualsSame() {
        ServiceEndPoint endpoint = endpoint("Foo", "server:80");
        assertEquals(endpoint, endpoint);
    }

    @Test
    public void testEqualsEquivalent() {
        ServiceEndPoint endpoint1 = endpoint("Foo", "server:80");
        ServiceEndPoint endpoint2 = endpoint("Foo", "server:80");
        assertEquals(endpoint1, endpoint2);
    }

    @Test
    public void testEqualsNull() {
        ServiceEndPoint endpoint = endpoint("Foo", "server:80");
        assertNotEquals(endpoint, null);
    }

    @Test
    public void testEqualsServiceName() {
        ServiceEndPoint endpoint1 = endpoint("Foo", "server:80");
        ServiceEndPoint endpoint2 = endpoint("Bar", "server:80");
        assertNotEquals(endpoint1, endpoint2);
    }

    @Test
    public void testEqualsId() {
        ServiceEndPoint endpoint1 = endpoint("Foo", "server:80");
        ServiceEndPoint endpoint2 = endpoint("Foo", "server:81");
        assertNotEquals(endpoint1, endpoint2);
    }

    @Test
    public void testEqualsEmptyPayload() {
        ServiceEndPoint endpoint1 = endpoint("Foo", "server:80");
        ServiceEndPoint endpoint2 = endpoint("Foo", "server:80", "");
        assertNotEquals(endpoint1, endpoint2);
    }

    @Test
    public void testEqualsNonEmptyPayload() {
        ServiceEndPoint endpoint1 = endpoint("Foo", "server:80");
        ServiceEndPoint endpoint2 = endpoint("Foo", "server:80", "payload");
        assertNotEquals(endpoint1, endpoint2);
    }

    @Test
    public void testHashCodeEquals() {
        ServiceEndPoint endpoint1 = endpoint("Foo", "server:80");
        ServiceEndPoint endpoint2 = endpoint("Foo", "server:80");
        assertEquals(endpoint1.hashCode(), endpoint2.hashCode());
    }

    private ServiceEndPoint endpoint(String serviceName, String id) {
        return endpoint(serviceName, id, null);
    }

    private ServiceEndPoint endpoint(String serviceName, String id, String payload) {
        return new ServiceEndPointBuilder()
                .withServiceName(serviceName)
                .withId(id)
                .withPayload(payload)
                .build();
    }

    private void assertNotEquals(Object a, Object b) {
        assertFalse(a.equals(b));
    }
}
