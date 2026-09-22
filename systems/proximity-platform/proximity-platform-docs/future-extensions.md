# Future Extensions

## Multi-location
Use `locationId` as the GEO member so one business can have multiple searchable locations.

## Filtering
Potential filters: category, business status, location status, service type.

## Pagination
For large result sets, consider cursor/continuation-token pagination with stable distance ordering.

## Spatial partitioning
At larger scale evaluate geohash, S2 cells, regional routing, and partition ownership.

## Redis sharding
Potential strategies include geographic partitioning or spatial-cell ownership.

## Hot-area caching
Cache repeated query-cell/radius/filter combinations with carefully chosen TTLs.

## Business activation rebuild
Retained location metadata can be used to restore active locations to GEO after business activation.

## Full read-model rebuild
Support rebuilding Redis from authoritative event history, preferably into a new namespace followed by an atomic namespace switch.

## Observability
Track geo query latency, Redis latency, Kafka lag, event processing latency, failures, duplicates, and rebuild duration.

## Rate limiting and backpressure
Add API rate limits, consumer concurrency controls, Redis pool tuning, and admission control as traffic grows.

## Advanced ranking
A later ranking layer may combine distance with availability, relevance, popularity, or explicit user filters. Keep ranking separate from geo indexing.

## Availability-aware proximity
Future read models could incorporate opening hours, inventory/capacity, and service availability.

## Reconciliation
A periodic reconciler can detect missing/stale Redis state and repair it.

## Alternative spatial stores
Evaluate PostGIS, OpenSearch/Elasticsearch geo queries, or specialized spatial databases if query requirements outgrow Redis GEO.

## Regional deployment
For global scale, use regional business data, Kafka, Redis, and request routing.

## Security
Add authentication, authorization, tenant isolation where required, request validation, and abuse protection.
