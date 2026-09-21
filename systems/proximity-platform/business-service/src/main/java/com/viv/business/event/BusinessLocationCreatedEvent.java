package com.viv.business.event;

import java.time.Instant;
import java.util.UUID;

public record BusinessLocationCreatedEvent(

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