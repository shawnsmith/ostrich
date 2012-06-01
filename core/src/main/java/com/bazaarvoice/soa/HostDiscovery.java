package com.bazaarvoice.soa;

/**
 * The <code>HostDiscovery</code> class is used to encapsulate the strategy that provides a set of hosts for use.
 * Users of this class shouldn't cache the results of discovery as subclasses can choose to change the set of available
 * hosts based on some external mechanism (ex. using ZooKeeper).
 */
public interface HostDiscovery {
    /**
     * Retrieve the available hosts.
     *
     * @return The available hosts.
     */
    Iterable<ServiceEndpoint> getHosts();

    /**
     * Returns true if the specified endpoint is a member of the set returned by {@link #getHosts()}.
     *
     * @param endpoint The endpoint to test.
     * @return True if the specified endpoint is a member of the set returned by {@link #getHosts()}.
     */
    boolean contains(ServiceEndpoint endpoint);

    /**
     * Add an endpoint listener.
     *
     * @param listener The endpoint listener to add.
     */
    void addListener(EndpointListener listener);

    /**
     * Remove an endpoint listener.
     *
     * @param listener The endpoint listener to remove.
     */
    void removeListener(EndpointListener listener);

    /**
     * Ensure the set of hosts is up-to-date with respect to new hosts.  This method does not guarantee that old hosts
     * have been removed.
     * <p>
     * It's not normally necessary to call this method.  The HostDiscovery object will automatically track changes to
     * the set of available hosts.  You should only call the <code>refresh()</code> method when you really must
     * guarantee that the {@link #getHosts()} method returns the most accurate picture possible.
     */
    void refresh();

    /** Listener interface that is notified when endpoints are added and removed. */
    interface EndpointListener {
        void onEndpointAdded(ServiceEndpoint endpoint);
        void onEndpointRemoved(ServiceEndpoint endpoint);
    }
}
