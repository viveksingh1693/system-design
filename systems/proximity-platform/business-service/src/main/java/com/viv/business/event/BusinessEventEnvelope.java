package com.viv.business.event;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

public record BusinessEventEnvelope(
        UUID eventId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        int schemaVersion,
        Instant occurredAt,
        JsonNode payload
) {
}