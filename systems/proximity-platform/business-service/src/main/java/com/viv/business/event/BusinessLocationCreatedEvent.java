package com.viv.business.event;

import java.time.Instant;
import java.util.UUID;

public record BusinessLocationCreatedEvent(

        UUID businessId,

        UUID locationId,

        Double latitude,

        Double longitude,

        String status,

        Instant occurredAt
) {
}