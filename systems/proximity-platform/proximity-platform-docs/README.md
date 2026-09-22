# Proximity Platform

Production-oriented proximity platform for finding nearby business locations.

## Architecture
```text
Business Service
  -> PostgreSQL/PostGIS
  -> Transactional Outbox
  -> Kafka
  -> Proximity Service
  -> Redis GEO + metadata
  -> Proximity API
```

## Responsibilities

### Business Service
- Source of truth for businesses and locations.
- Persists PostgreSQL/PostGIS state.
- Writes transactional outbox events.

### Proximity Service
- Consumes business events.
- Maintains a Redis geo read model.
- Provides low-latency proximity queries.
- Does not synchronously call Business Service for normal queries.

## Redis model
```text
business:geo
    locationId -> GEO coordinates

business:location:{locationId}
    businessId
    locationId
    categoryId
    name
    status
    latitude
    longitude

business:locations:{businessId}
    locationId set

processed:event:{eventId}
processing:event:{eventId}
```

## Query API
```http
GET /api/v1/proximity/businesses?latitude=28.4595&longitude=77.0266&radius=5000&limit=20
```

Response remains location-aware:
```json
{
  "businessId": "...",
  "locationId": "...",
  "categoryId": "...",
  "name": "The Food Corner",
  "status": "ACTIVE",
  "latitude": 28.4592,
  "longitude": 77.0272,
  "distanceMeters": 67.4119
}
```

## Supported events
```text
BUSINESS_CREATED
BUSINESS_UPDATED
BUSINESS_ACTIVATED
BUSINESS_DEACTIVATED
BUSINESS_SUSPENDED
BUSINESS_LOCATION_CREATED
BUSINESS_LOCATION_UPDATED
BUSINESS_LOCATION_DEACTIVATED
```

## Guarantees
- At-least-once event delivery is expected.
- Event IDs provide idempotent processing.
- Kafka key uses business/aggregate ID for per-business ordering.
- Redis is eventually consistent with PostgreSQL.

## Observability
Actuator, Micrometer, OpenTelemetry, Jaeger, Prometheus and Grafana are used for operational visibility.

## Documentation
- `proximity-algorithm.md` — indexing/query algorithm
- `future-extensions.md` — future scale and feature options
- `ADR-005-location-based-geo-index.md` — architectural decision
- `proximity-state-diagram.md` — lifecycle/state diagrams

## Roadmap
```text
Multi-location geo index
        |
Integration tests
        |
Business activation restore
        |
Full read-model rebuild
        |
Filtering / pagination
        |
Observability improvements
        |
Spatial partitioning / sharding
        |
Advanced ranking
```
