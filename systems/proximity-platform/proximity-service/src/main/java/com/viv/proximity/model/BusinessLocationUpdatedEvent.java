package com.viv.proximity.model;

import java.time.Instant;
import java.util.UUID;

public record BusinessLocationUpdatedEvent(
        UUID businessId,
        UUID locationId,
        UUID categoryId,
        String businessName,
        String businessStatus,
        Double latitude,
        Double longitude,
        String locationStatus,
        Instant occurredAt
) {
}