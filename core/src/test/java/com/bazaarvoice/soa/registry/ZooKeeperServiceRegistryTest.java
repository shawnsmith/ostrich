package com.bazaarvoice.soa.registry;

import com.bazaarvoice.soa.ServiceEndPoint;
import com.bazaarvoice.soa.ServiceEndPointBuilder;
import com.bazaarvoice.soa.ServiceEndPointJsonCodec;
import com.bazaarvoice.zookeeper.ZooKeeperConnection;
import com.bazaarvoice.zookeeper.recipes.ZooKeeperPersistentEphemeralNode;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import java.util.concurrent.TimeUnit;

import static com.bazaarvoice.soa.registry.ZooKeeperServiceRegistry.MAX_DATA_SIZE;
import static com.bazaarvoice.soa.registry.ZooKeeperServiceRegistry.makeEndPointPath;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZooKeeperServiceRegistryTest {
    private static final ServiceEndPoint FOO = newEndPoint("Foo", "server:80", "");
    private static final String FOO_PATH = makeEndPointPath(FOO);

    private ZooKeeperServiceRegistry.NodeFactory _factoryMock;
    private ZooKeeperPersistentEphemeralNode _nodeMock;
    private ZooKeeperServiceRegistry _registry;

    @Before
    public void setup() {
        _factoryMock = mock(ZooKeeperServiceRegistry.NodeFactory.class);
        _nodeMock = mock(ZooKeeperPersistentEphemeralNode.class);
        ZooKeeperPersistentEphemeralNode subsequent = mock(ZooKeeperPersistentEphemeralNode.class);
        when(_factoryMock.create(anyString(), any(byte[].class))).thenReturn(_nodeMock, subsequent);
        _registry = new ZooKeeperServiceRegistry(_factoryMock);
    }

    @After
    public void teardown() throws Exception {
        _registry.close();
    }

    @Test(expected = NullPointerException.class)
    public void testNullConnection() throws Exception {
        new ZooKeeperServiceRegistry((ZooKeeperConnection) null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullFactory() {
        new ZooKeeperServiceRegistry((ZooKeeperServiceRegistry.NodeFactory) null);
    }

    @Test
    public void testConstructor() {
        new ZooKeeperServiceRegistry(mock(ZooKeeperConnection.class));
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterNullService() throws Exception {
        _registry.register(null);
    }

    @Test(expected = NullPointerException.class)
    public void testUnregisterNullService() throws Exception {
        _registry.unregister(null);
    }

    @Test(expected = IllegalStateException.class)
    public void testRegisterAfterClose() throws Exception {
        _registry.close();
        _registry.register(FOO);
    }

    @Test(expected = IllegalStateException.class)
    public void testUnregisterAfterClose() throws Exception {
        _registry.close();
        _registry.unregister(FOO);
    }

    @Test(expected = IllegalStateException.class)
    public void testLargePayloadSize() {
        int padding = ServiceEndPointJsonCodec.toJson(FOO).getBytes(Charsets.UTF_8).length;
        String payload = Strings.repeat("x", MAX_DATA_SIZE - padding);
        _registry.register(newEndPoint(FOO.getServiceName(), FOO.getId(), payload), false);
    }

    @Test
    public void testMediumPayloadSize() {
        int padding = ServiceEndPointJsonCodec.toJson(FOO).getBytes(Charsets.UTF_8).length;
        String payload = Strings.repeat("x", MAX_DATA_SIZE - padding - 1);
        _registry.register(newEndPoint(FOO.getServiceName(), FOO.getId(), payload), false);
    }

    @Test
    public void testEmptyPayload() {
        _registry.register(newEndPoint(FOO.getServiceName(), FOO.getId(), ""), false);
    }

    @Test
    public void testRegister() throws Exception {
        _registry.register(FOO);

        ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);

        verify(_factoryMock).create(eq(FOO_PATH), dataCaptor.capture());
        assertEquals(FOO, ServiceEndPointJsonCodec.fromJson(new String(dataCaptor.getValue())));
        verify(_nodeMock, never()).close(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testDuplicateRegister() throws Exception {
        _registry.register(FOO);
        _registry.register(FOO);

        verify(_factoryMock, times(2)).create(eq(FOO_PATH), Matchers.<byte[]>any());
        verify(_nodeMock).close(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testUnregister() throws Exception {
        _registry.register(FOO);

        _registry.unregister(FOO);

        verify(_nodeMock).close(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testUnregisterWithoutFirstRegistering() throws Exception {
        _registry.unregister(FOO);

        verify(_factoryMock, never()).create(eq(FOO_PATH), Matchers.<byte[]>any());
    }

    @Test
    public void testDuplicateUnregister() throws Exception {
        _registry.register(FOO);

        _registry.unregister(FOO);
        _registry.unregister(FOO);
    }

    @Test
    public void testServiceNodeIsDeletedWhenRegistryIsClosed() throws Exception {
        _registry.register(FOO);

        _registry.close();

        verify(_nodeMock).close(anyLong(), any(TimeUnit.class));
    }

    private static ServiceEndPoint newEndPoint(String serviceName, String id, String payload) {
        return new ServiceEndPointBuilder()
                .withServiceName(serviceName)
                .withId(id)
                .withPayload(payload)
                .build();
    }
}
