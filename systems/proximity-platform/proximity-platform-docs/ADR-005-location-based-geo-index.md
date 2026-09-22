# ADR-005: Location-Based Geo Index for Multi-Location Businesses

- Status: Accepted
- Date: 2026-09-22
- Scope: Proximity Service read model

## Context

The original Redis model used:

```text
business:geo
    businessId -> GEO point
```

This effectively allowed only one searchable coordinate per business, while the domain supports `BusinessLocation` entities.

## Decision

Use `locationId` as the Redis GEO member:

```text
business:geo
    locationId -> coordinates
```

Store location metadata separately:

```text
business:location:{locationId}
```

Maintain a reverse business-to-location set:

```text
business:locations:{businessId}
```

Business Service remains the source of truth; Proximity Service remains an asynchronous derived read model.

## Event behavior

### Location created/active update
```text
event -> save metadata -> GEOADD -> SADD business/location
```

### Location deactivated
```text
event -> ZREM locationId -> mark metadata inactive -> SREM
```

### Business suspended/deactivated
```text
event -> enumerate business locations -> remove each from GEO -> update read-model state
```

## Rationale

This supports multiple physical locations per business and allows one location to be changed or removed without affecting other locations.

## Consistency and delivery

The model is eventually consistent. Kafka is treated as at-least-once delivery and the consumer is idempotent. Kafka keys use business/aggregate ID to preserve per-business ordering.

## Alternatives considered

### Keep businessId as GEO member
Rejected because a single GEO member cannot represent multiple independent locations.

### No reverse business-to-location index
Rejected because business-level lifecycle operations would lack an efficient way to discover all locations.

### Synchronous calls from proximity to business service
Rejected because proximity queries should remain low-latency and independently available.

### Custom application spatial index
Not selected for the current stage because Redis already provides the required geo-indexing primitive.

## Consequences

### Positive
- Native multi-location support.
- Location-level lifecycle operations.
- Simple radius queries.
- Existing response can expose both businessId and locationId.

### Negative
- Additional Redis data structures.
- More write operations.
- Business-level operations enumerate locations.
- Derived state needs rebuild/reconciliation capabilities.

## Future evolution
Evaluate spatial partitioning, Redis sharding, read-model rebuilds, advanced filtering/ranking, and alternative spatial stores as measured scale and query complexity require them.
