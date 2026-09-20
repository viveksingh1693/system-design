package com.viv.proximity.model;

import java.time.Instant;
import java.util.UUID;

public record BusinessUpdatedEvent(
        UUID businessId,
        UUID categoryId,
        String name,
        String status,
        Instant occurredAt
) {
}