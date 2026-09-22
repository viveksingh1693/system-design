package com.viv.proximity.redis;

import java.util.UUID;

public final class RedisKeyBuilder {

    private RedisKeyBuilder() {
    }

    public static String businessGeoIndex() {
        return "business:geo";
    }

    public static String business(UUID businessId) {
        return "business:" + businessId;
    }

    public static String businessLocation(UUID locationId) {
        return "business:location:" + locationId;
    }

    public static String businessLocations(UUID businessId) {
        return "business:locations:" + businessId;
    }

    public static String processedEvent(UUID eventId) {
        return "processed:event:" + eventId;
    }

    public static String processingEvent(UUID eventId) {
        return "processing:event:" + eventId;
    }
}