package com.bazaarvoice.soa;

/**
 * A registry for services.  The <code>ServiceRegistry</code> gives service providers a way to register their service
 * instances in order to make them available to consumers of the service across multiple JVMs.
 */
public interface ServiceRegistry {
    /**
     * Add an instance of a service to the service registry and make it available for discovery.
     *
     * @param instance The instance of the service to register.
     * @return Whether or not the registration operation succeeded.
     * @throws RuntimeException If there was a problem registering the instance.
     */
    boolean register(ServiceInstance instance);

    /**
     * Remove an instance of a service from the service registry.  This will make it no longer available
     * to be discovered.
     *
     * @param instance The instance of the service to unregister.
     * @return Whether or not the unregister operation succeeded.
     * @throws RuntimeException If there was a problem de-registering the instance.
     */
    boolean unregister(ServiceInstance instance);
}
