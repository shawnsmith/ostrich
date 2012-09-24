package com.bazaarvoice.soa.discovery;

import com.bazaarvoice.soa.HostDiscovery;
import com.bazaarvoice.soa.ServiceEndPoint;
import com.bazaarvoice.soa.ServiceEndPointBuilder;
import com.bazaarvoice.soa.ServiceEndPointJsonCodec;
import com.bazaarvoice.soa.registry.ZooKeeperServiceRegistry;
import com.bazaarvoice.zookeeper.ZooKeeperConnection;
import com.bazaarvoice.zookeeper.recipes.discovery.NodeDataParser;
import com.bazaarvoice.zookeeper.recipes.discovery.NodeListener;
import com.bazaarvoice.zookeeper.recipes.discovery.ZooKeeperNodeDiscovery;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.Closeables;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZooKeeperHostDiscoveryTest {
    private static final ServiceEndPoint FOO = new ServiceEndPointBuilder()
            .withServiceName("Foo")
            .withId("server:8080")
            .build();

    private ZooKeeperHostDiscovery _discovery;
    private NodeListener<ServiceEndPoint> _listener;
    private NodeDataParser<ServiceEndPoint> _parser;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception {
        ZooKeeperHostDiscovery.NodeDiscoveryFactory factory = mock(ZooKeeperHostDiscovery.NodeDiscoveryFactory.class);
        ArgumentCaptor<NodeListener<ServiceEndPoint>> listenerCaptor =
                (ArgumentCaptor) ArgumentCaptor.forClass(NodeListener.class);
        ArgumentCaptor<NodeDataParser<ServiceEndPoint>> parserCaptor =
                (ArgumentCaptor) ArgumentCaptor.forClass(NodeDataParser.class);
        ZooKeeperNodeDiscovery<ServiceEndPoint> nodeDiscovery = mock(ZooKeeperNodeDiscovery.class);
        ZooKeeperConnection connection = mock(ZooKeeperConnection.class);
        when(factory.create(Matchers.<ZooKeeperConnection>any(ZooKeeperConnection.class), anyString(),
                Matchers.<NodeDataParser<ServiceEndPoint>>any())).thenReturn(nodeDiscovery);
        _discovery = new ZooKeeperHostDiscovery(factory, connection, FOO.getServiceName());
        verify(factory).create(same(connection), eq(ZooKeeperServiceRegistry.makeServicePath("Foo")),
                parserCaptor.capture());
        verify(nodeDiscovery).addListener(listenerCaptor.capture());
        _listener = listenerCaptor.getValue();
        _parser = parserCaptor.getValue();
    }

    @After
    public void teardown() throws Exception {
        Closeables.closeQuietly(_discovery);
    }

    @Test (expected = NullPointerException.class)
    public void testNullConnection() {
        new ZooKeeperHostDiscovery(null, FOO.getServiceName());
    }

    @Test (expected = NullPointerException.class)
    public void testNullServiceName() throws Exception {
        new ZooKeeperHostDiscovery(mock(ZooKeeperConnection.class), null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testEmptyServiceName() throws Exception {
        new ZooKeeperHostDiscovery(mock(ZooKeeperConnection.class), "");
    }

    @Test
    public void testParser() {
        assertEquals(FOO, _parser.parse("path", ServiceEndPointJsonCodec.toJson(FOO).getBytes(Charsets.UTF_8)));
    }

    @Test
    public void testStartsEmpty() {
        assertTrue(Iterables.isEmpty(_discovery.getHosts()));
    }

    @Test
    public void testRegisterService() {
        _listener.onNodeAdded("path", FOO);

        assertEquals(ImmutableList.of(FOO), ImmutableList.copyOf(_discovery.getHosts()));
    }

    @Test
    public void testUnregisterService() {
        _listener.onNodeAdded("path", FOO);
        _listener.onNodeRemoved("path", FOO);

        assertTrue(Iterables.isEmpty(_discovery.getHosts()));
    }

    @Test
    public void testClose() throws IOException {
        // After closing, HostDiscovery returns no hosts so clients won't work if they accidentally keep using it.
        _listener.onNodeAdded("path", FOO);
        _discovery.close();

        assertTrue(Iterables.isEmpty(_discovery.getHosts()));
        _discovery = null;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExistingData() throws Exception {
        ZooKeeperHostDiscovery.NodeDiscoveryFactory factory = mock(ZooKeeperHostDiscovery.NodeDiscoveryFactory.class);
        ZooKeeperNodeDiscovery<ServiceEndPoint> nodeDiscovery = mock(ZooKeeperNodeDiscovery.class);
        final ArgumentCaptor<NodeListener<ServiceEndPoint>> listenerCaptor =
                (ArgumentCaptor) ArgumentCaptor.forClass(NodeListener.class);
        when(factory.create(Matchers.<ZooKeeperConnection>any(), anyString(),
                Matchers.<NodeDataParser<ServiceEndPoint>>any())).thenReturn(nodeDiscovery);
        doNothing().when(nodeDiscovery).addListener(listenerCaptor.capture());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                listenerCaptor.getValue().onNodeAdded("path", FOO);
                return null;
            }
        }).when(nodeDiscovery).start();
        HostDiscovery discovery = new ZooKeeperHostDiscovery(factory, mock(ZooKeeperConnection.class),
                FOO.getServiceName());
        assertEquals(ImmutableList.of(FOO), ImmutableList.copyOf(discovery.getHosts()));
    }

    @Test
    public void testDuplicateEntry() {
        CountingListener endPointListener = new CountingListener();
        _discovery.addListener(endPointListener);

        _listener.onNodeAdded("path-one", FOO);
        _listener.onNodeAdded("path-two", FOO);

        assertEquals(1, endPointListener.getNumAdds());
        assertEquals(ImmutableList.of(FOO), ImmutableList.copyOf(_discovery.getHosts()));
    }

    @Test
    public void testDuplicateEntrySingleRemoval() {
        CountingListener endPointListener = new CountingListener();
        _discovery.addListener(endPointListener);

        _listener.onNodeAdded("path-one", FOO);
        _listener.onNodeAdded("path-two", FOO);

        _listener.onNodeRemoved("path-one", FOO);

        assertEquals(0, endPointListener.getNumRemoves());
        assertEquals(ImmutableList.of(FOO), ImmutableList.copyOf(_discovery.getHosts()));
    }

    @Test
    public void testDuplicateEntryRemoval() {
        CountingListener endPointListener = new CountingListener();
        _discovery.addListener(endPointListener);

        _listener.onNodeAdded("path-one", FOO);
        _listener.onNodeAdded("path-two", FOO);

        _listener.onNodeRemoved("path-one", FOO);
        _listener.onNodeRemoved("path-two", FOO);

        assertEquals(1, endPointListener.getNumRemoves());
        assertTrue(Iterables.isEmpty(_discovery.getHosts()));
    }

    @Test
    public void testAlreadyExistingEndPointsDoNotFireEvents() throws Exception {
        _listener.onNodeAdded("path", FOO);

        CountingListener endPointListener = new CountingListener();
        _discovery.addListener(endPointListener);

        assertEquals(0, endPointListener.getNumEvents());
    }

    @Test
    public void testRegisterServiceCallsListener() throws Exception {
        CountingListener endPointListener = new CountingListener();
        _discovery.addListener(endPointListener);

        _listener.onNodeAdded("path", FOO);
        assertEquals(1, endPointListener.getNumAdds());
    }

    @Test
    public void testUnregisterServiceCallsListener() throws Exception {
        CountingListener endPointListener = new CountingListener();
        _discovery.addListener(endPointListener);

        _listener.onNodeAdded("path", FOO);
        _listener.onNodeRemoved("path", FOO);
        assertEquals(1, endPointListener.getNumRemoves());
    }

    @Test
    public void testRemovedListenerDoesNotSeeEvents() throws Exception {
        CountingListener eventCounter = new CountingListener();
        _discovery.addListener(eventCounter);
        _discovery.removeListener(eventCounter);

        _listener.onNodeAdded("path", FOO);
        _listener.onNodeRemoved("path", FOO);
        assertEquals(0, eventCounter.getNumEvents());
    }

    @Test
    public void testMultipleListeners() throws Exception {
        CountingListener endPointListener1 = new CountingListener();
        CountingListener endPointListener2 = new CountingListener();
        _discovery.addListener(endPointListener1);
        _discovery.addListener(endPointListener2);

        _listener.onNodeAdded("path", FOO);
        assertEquals(1, endPointListener1.getNumAdds());
        assertEquals(1, endPointListener2.getNumAdds());

        _listener.onNodeRemoved("path", FOO);
        assertEquals(1, endPointListener1.getNumRemoves());
        assertEquals(1, endPointListener2.getNumRemoves());
    }

    private static final class CountingListener implements HostDiscovery.EndPointListener {
        private int _numAdds;
        private int _numRemoves;

        @Override
        public void onEndPointAdded(ServiceEndPoint endPoint) {
            _numAdds++;
        }

        @Override
        public void onEndPointRemoved(ServiceEndPoint endPoint) {
            _numRemoves++;
        }

        public int getNumAdds() {
            return _numAdds;
        }

        public int getNumRemoves() {
            return _numRemoves;
        }

        public int getNumEvents() {
            return _numAdds + _numRemoves;
        }
    }
}
