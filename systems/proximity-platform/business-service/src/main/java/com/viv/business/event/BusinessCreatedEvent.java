package com.viv.business.event;

import java.time.Instant;
import java.util.UUID;

public record BusinessCreatedEvent(

        UUID businessId,

        UUID categoryId,

        String name,

        String status,

        Instant occurredAt
) {
}