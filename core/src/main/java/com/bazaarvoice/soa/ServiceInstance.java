package com.bazaarvoice.soa;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.net.HostAndPort;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.io.StringWriter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

// TODO: ServiceInstance feels like the wrong name for this.  Maybe ServiceEndpoint instead?
public final class ServiceInstance {
    // Service names and versions have a restricted set of valid characters in them for simplicity.  These are the
    // characters that can appear in a URL without needing escaping.  This will let us refer to services with a URL
    // looking structure (e.g. prod://services/profile-v1)
    private static final CharMatcher VALID_CHARACTERS = CharMatcher.NONE
            .or(CharMatcher.inRange('a', 'z'))
            .or(CharMatcher.inRange('A', 'Z'))
            .or(CharMatcher.inRange('0', '9'))
            .or(CharMatcher.anyOf("._-"))
            .precomputed();

    // All dates are represented in ISO-8601 format and in the UTC time zone.
    @VisibleForTesting
    static final DateTimeFormatter ISO8601 = ISODateTimeFormat.dateTime().withZoneUTC();

    // ZooKeeper has a 1MB limit on the size of node data, so we need to make sure that our payload isn't too large.
    // We reserve 100KB for internal use by the SOA framework and allow the remaining 900KB to be used by our payload.
    // Strings in java are UTF-16 so each character takes 2 bytes.
    @VisibleForTesting
    static final int MAX_PAYLOAD_SIZE_IN_CHARACTERS = 900*1024/2;

    private final String _serviceName;
    private final HostAndPort _address;
    private final DateTime _registrationTime;
    private final String _payload;

    public ServiceInstance(String serviceName, HostAndPort address) {
        this(serviceName, address, null);
    }

    public ServiceInstance(String serviceName, HostAndPort address, String payload) {
        this(serviceName, address, DateTime.now(), payload);
    }

    private ServiceInstance(String serviceName, HostAndPort address, DateTime registrationTime, String payload) {
        checkArgument(serviceName != null && serviceName.length() > 0);
        checkArgument(VALID_CHARACTERS.matchesAllOf(serviceName));
        checkArgument(address != null && address.getHostText().length() > 0 && address.hasPort());
        checkArgument(payload == null || payload.length() <= MAX_PAYLOAD_SIZE_IN_CHARACTERS);

        _serviceName = serviceName;
        _address = address;
        _registrationTime = registrationTime.toDateTime(DateTimeZone.UTC);
        _payload = payload;
    }

    public String getServiceName() {
        return _serviceName;
    }

    public String getHostname() {
        return _address.getHostText();
    }

    public int getPort() {
        return _address.getPort();
    }

    public String getServiceAddress() {
        return _address.toString();
    }

    public DateTime getRegistrationTime() {
        return _registrationTime;
    }

    public String getPayload() {
        return _payload;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(_serviceName, _address, _registrationTime, _payload);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ServiceInstance)) return false;

        ServiceInstance that = (ServiceInstance) obj;
        return Objects.equal(_serviceName, that._serviceName)
                && Objects.equal(_address, that._address)
                && Objects.equal(_registrationTime, that._registrationTime)
                && Objects.equal(_payload, that._payload);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("name", _serviceName)
                .add("address", _address)
                .toString();
    }

    public String toJson() {
        StringWriter writer = new StringWriter();

        try {
            JsonGenerator generator = new JsonFactory().createJsonGenerator(writer);
            generator.writeStartObject();
            generator.writeStringField("registration-time", ISO8601.print(_registrationTime));
            generator.writeStringField("name", _serviceName);
            generator.writeStringField("host", _address.getHostText());
            generator.writeNumberField("port", _address.getPort());
            generator.writeStringField("payload", _payload);
            generator.writeEndObject();
            generator.close();
        } catch (IOException e) {
            // We're writing primitives to an in-memory stream -- we should never have an exception thrown.
            // TODO: Log?
            throw Throwables.propagate(e);
        }

        return writer.toString();
    }

    public static ServiceInstance fromJson(String json) {
        try {
            JsonNode root = new ObjectMapper().readTree(json);
            JsonNode nameNode = checkNotNull(root.get("name"));
            JsonNode hostNode = checkNotNull(root.get("host"));
            JsonNode portNode = checkNotNull(root.get("port"));
            JsonNode registrationTimeNode = checkNotNull(root.get("registration-time"));
            JsonNode payloadNode = checkNotNull(root.get("payload"));

            String name = nameNode.textValue();
            HostAndPort address = HostAndPort.fromParts(hostNode.textValue(), portNode.intValue());
            DateTime registrationTime = ISO8601.parseDateTime(registrationTimeNode.textValue());
            String payload = !payloadNode.isNull() ? payloadNode.textValue() : null;

            return new ServiceInstance(name, address, registrationTime, payload);
        } catch (IOException e) {
            // TODO: Is propagating the right thing to do here?
            throw Throwables.propagate(e);
        }
    }
}
