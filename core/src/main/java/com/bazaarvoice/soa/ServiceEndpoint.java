package com.bazaarvoice.soa;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import com.google.common.net.HostAndPort;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.io.StringWriter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class ServiceEndpoint
{
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

    private final String _serviceName;
    private final HostAndPort _address;
    private final DateTime _registrationTime;
    private final String _payload;

    public ServiceEndpoint(String serviceName, String hostname, int port) {
        this(serviceName, hostname, port, DateTime.now(), null);
    }

    public ServiceEndpoint(String serviceName, String hostname, int port, String payload) {
        this(serviceName, hostname, port, DateTime.now(), payload);
    }

    private ServiceEndpoint(String serviceName, String hostname, int port, DateTime registrationTime, String payload) {
        checkArgument(serviceName != null && serviceName.length() > 0);
        checkArgument(VALID_CHARACTERS.matchesAllOf(serviceName));
        checkArgument(hostname != null && hostname.length() > 0);

        _serviceName = serviceName;
        _address = HostAndPort.fromParts(hostname, port);
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
        return Objects.hashCode(_serviceName, _address, _payload);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ServiceEndpoint)) return false;

        ServiceEndpoint that = (ServiceEndpoint) obj;
        return Objects.equal(_serviceName, that._serviceName)
                && Objects.equal(_address, that._address)
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
            throw new AssertionError(e);
        }

        return writer.toString();
    }

    public static ServiceEndpoint fromJson(String json) {
        try {
            JsonNode root = new ObjectMapper().readTree(json);
            JsonNode nameNode = checkNotNull(root.get("name"));
            JsonNode hostNode = checkNotNull(root.get("host"));
            JsonNode portNode = checkNotNull(root.get("port"));
            JsonNode registrationTimeNode = checkNotNull(root.get("registration-time"));
            JsonNode payloadNode = checkNotNull(root.get("payload"));

            String name = nameNode.textValue();
            String hostname = hostNode.textValue();
            int port = portNode.intValue();
            DateTime registrationTime = ISO8601.parseDateTime(registrationTimeNode.textValue());
            String payload = !payloadNode.isNull() ? payloadNode.textValue() : null;

            return new ServiceEndpoint(name, hostname, port, registrationTime, payload);
        } catch (IOException e) {
            throw new AssertionError(e);  // Shouldn't get IO errors reading from a string
        }
    }
}
