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

    public static String processedEvent(UUID eventId) {
        return "processed:event:" + eventId;
    }
}