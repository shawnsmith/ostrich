package com.bazaarvoice.soa;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ServiceEndPointJsonCodec {
    // All dates are represented in ISO-8601 format and in the UTC time zone.
    @VisibleForTesting
    static final DateTimeFormatter ISO8601 = ISODateTimeFormat.dateTime().withZoneUTC();

    public static String toJson(ServiceEndPoint endpoint) {
        DateTime now = DateTime.now().toDateTime(DateTimeZone.UTC);

        Map<String, Object> data = Maps.newLinkedHashMap();
        data.put("registration-time", ISO8601.print(now));
        data.put("name", endpoint.getServiceName());
        data.put("host", endpoint.getHostname());
        data.put("port", endpoint.getPort());
        data.put("payload", endpoint.getPayload());
        return JsonHelper.toJson(data);
    }

    public static ServiceEndPoint fromJson(String json) {
        Map<?, ?> data = JsonHelper.fromJson(json, Map.class);
        String name = (String) checkNotNull(data.get("name"));
        String host = (String) checkNotNull(data.get("host"));
        int port = ((Number) checkNotNull(data.get("port"))).intValue();
        String payload = (String) data.get("payload");

        return new ServiceEndPointBuilder()
                .withServiceName(name)
                .withHostname(host)
                .withPort(port)
                .withPayload(payload)
                .build();
    }

    // Private, not instantiable.
    private ServiceEndPointJsonCodec() {}
}
