package com.bazaarvoice.soa;

public interface ServiceEndPoint {
    /** The name of the service. */
    String getServiceName();

    /** The hostname that this end point is running on. */
    String getHostname();

    /** The port on the host that this end point bound to. */
    int getPort();

    /** The complete address (hostname and port) that this end point is bound to. */
    String getServiceAddress();

    /** An optional payload provided by the user that registered the service. */
    String getPayload();
}
