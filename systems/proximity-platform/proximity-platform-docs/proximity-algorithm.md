# Proximity Service — Algorithm

## Purpose
Find active business locations within a radius of a latitude/longitude using an asynchronous Redis read model.

## Architecture
```text
Business Service -> PostgreSQL/PostGIS -> Outbox -> Kafka
                                             |
                                             v
                                      Proximity Service
                                      |      |       |
                                      v      v       v
                                   Redis GEO Metadata Idempotency
                                             |
                                             v
                                      Proximity API
```

## Write/indexing algorithm

For every Kafka event:
1. Deserialize the envelope.
2. Check `processed:event:{eventId}`.
3. Claim with `processing:event:{eventId}` and a short TTL.
4. Apply the event to Redis.
5. Mark the event processed.
6. Acknowledge Kafka.
7. On failure, release the claim and allow redelivery.

For an active location:
```text
GEOADD business:geo locationId coordinates
HSET business:location:{locationId} metadata
SADD business:locations:{businessId} locationId
```

For location deactivation:
```text
ZREM business:geo locationId
update location metadata status
SREM business:locations:{businessId} locationId
```

For business suspension/deactivation:
```text
SMEMBERS business:locations:{businessId}
ZREM each location from business:geo
update location read-model status
```

## Read/query algorithm

Input: latitude, longitude, radius, limit.

```text
1. Validate request
2. Redis GEO search
3. Receive locationId + distance
4. Load location metadata
5. Build NearbyBusinessResponse
6. Return results ordered by distance
```

The application does not scan every business and calculate distance itself; spatial filtering is delegated to Redis GEO.

## Complexity

Let N be indexed locations and K be the requested result limit. The query avoids an application-level scan of N locations. Redis performs the geo-index lookup; application work is primarily proportional to returned candidates/metadata lookups. Exact Redis internals are version-dependent and should not be treated as a hard API guarantee.

## Consistency

The system is eventually consistent:

```text
PostgreSQL -> Outbox -> Kafka -> Redis
```

A successful database transaction does not mean the change is immediately visible to proximity queries.

## Ordering

Kafka uses `businessId`/aggregate ID as the message key, preserving per-business ordering within Kafka partitioning. The event payload identifies the exact `locationId`.

## Failure handling

Kafka is treated as at-least-once delivery. Event IDs make consumer processing idempotent. Processing claims use TTL so an abandoned claim does not block an event forever.

## Current assumptions

- Business Service is the source of truth.
- Proximity is a derived read model.
- Location IDs are stable UUIDs.
- Normal proximity queries do not synchronously call Business Service.
- Current Redis GEO members are location IDs.
