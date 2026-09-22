package com.viv.proximity.model;
import java.time.Instant;
import java.util.UUID;

public record BusinessLocationDeactivatedEvent(
        UUID businessId,
        UUID locationId,
        Instant occurredAt
) {
}