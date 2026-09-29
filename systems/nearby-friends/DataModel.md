# Data Model

## PostgreSQL: `friendship`

Flyway creates and seeds the table in `nearby-service/src/main/resources/db/migration`.

| Column | Type | Constraints / meaning |
| --- | --- | --- |
| `user_id` | `VARCHAR(64)` | Required; owner of the directed friendship row. |
| `friend_id` | `VARCHAR(64)` | Required; other user in the relationship. |
| `status` | `VARCHAR(20)` | Required; defaults to `ACTIVE`; allowed values are `ACTIVE`, `BLOCKED`, and `REMOVED`. |
| `created_at` | `TIMESTAMP` | Required; defaults to the current timestamp. |

The composite primary key is `(user_id, friend_id)`. A check constraint prohibits self-friendship. An index on `(user_id, status)` supports retrieval of active friends for one requester.

Rows are directional. Mutual friendship requires one row in each direction; the nearby query reads only rows where `user_id` equals the requesting user and `status = 'ACTIVE'`.

The Flyway seed data includes active relationships for `u1`, `u2`, and `u3`. It is intended for local development, not production initialization.

## Redis: Location Index

| Key | Redis structure | Member/value | Use |
| --- | --- | --- | --- |
| `nearby:user:geo` | GEO sorted set | Member is `userId`; coordinates are longitude and latitude. | Current positions and radius searches. |
| `nearby:user:last-seen` | Sorted set | Member is `userId`; score is last-seen epoch milliseconds. | Location freshness and expiry. |

Redis GEO coordinates use longitude as the x-coordinate and latitude as the y-coordinate. Distances in nearby query responses are reported in kilometers.

The location processor updates the position and last-seen score from Kafka events, rejecting an event if its timestamp is older than the stored timestamp. The expiry job removes a user's member from both indexes after two minutes without a refresh; it checks every 30 seconds.

Redis is an ephemeral index, not the friendship source of truth. The nearby service queries Redis for geographic candidates and PostgreSQL for active friend IDs, then intersects the two result sets.

## Location Event Shape

Location updates are published to Kafka topic `location-updates` as JSON:

```json
{
  "userId": "u1",
  "latitude": 28.4595,
  "longitude": 77.0266,
  "accuracyMeters": 8.5,
  "timestamp": "2026-09-25T06:30:00Z"
}
```

`timestamp` is an ISO-8601 instant. `accuracyMeters` is carried in the event but is not used by the nearby radius query.
