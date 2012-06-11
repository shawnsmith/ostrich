package com.bazaarvoice.soa;

/**
 * A registry for services.  The <code>ServiceRegistry</code> gives service providers a way to register their service
 * endpoints in order to make them available to consumers of the service across multiple JVMs.
 */
public interface ServiceRegistry {
    /**
     * Add an endpoint of a service to the service registry and make it available for discovery.
     *
     * @param endpoint The endpoint of the service to register.
     * @throws RuntimeException If there was a problem registering the endpoint.
     */
    void register(ServiceEndpoint endpoint);

    /**
     * Remove an endpoint of a service from the service registry.  This will make it no longer available
     * to be discovered.
     *
     * @param endpoint The endpoint of the service to unregister.
     * @throws RuntimeException If there was a problem de-registering the endpoint.
     */
    void unregister(ServiceEndpoint endpoint);
}
