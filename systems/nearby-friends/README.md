# Nearby Friends

A Spring Boot system for finding a user's active friends within a geographic radius. Location updates are accepted by the location service, published through Kafka, and indexed in Redis GEO. The nearby service combines that index with active friendship records in PostgreSQL.

## Services

| Service | Port | Responsibility |
| --- | ---: | --- |
| `location-service` | 8080 | Accepts location updates and publishes events to Kafka. |
| `location-processor` | 8082 | Consumes location events and maintains the Redis GEO index. |
| `nearby-service` | 8081 | Finds nearby users and filters them to the requester's active friends. |

PostgreSQL, Redis, Kafka, and Kafka UI are defined in [`docker-compose.yml`](docker-compose.yml). See [ARCHITECTURE.md](ARCHITECTURE.md) for the request flow and [DataModel.md](DataModel.md) for storage details.

## Run Locally

Start the dependencies from the repository root:

```powershell
docker compose up -d
```

In separate terminals, start the services:

```powershell
cd location-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd location-processor
.\mvnw.cmd spring-boot:run
```

```powershell
cd nearby-service
.\mvnw.cmd spring-boot:run
```

The nearby service runs Flyway migrations at startup to create and seed the `friendship` table. Redis GEO data is populated asynchronously after a location update is accepted, so allow the Kafka consumer a moment to process the event before querying nearby friends.

## API

### Find Nearby Friends

```http
GET http://localhost:8081/api/v1/nearby-friends?userId=u1&radiusKm=5
```

`userId` is required. `radiusKm` is optional and defaults to `5`. The response is an array ordered by ascending distance:

```json
[
  {
    "userId": "u2",
    "distanceKm": 0.3
  }
]
```

If the requesting user has no current location in Redis, the service returns an empty array. The query only includes users within the radius who have an `ACTIVE` friendship row where the requester is `user_id`.

### Submit a Location Update

```http
POST http://localhost:8080/api/v1/locations
Content-Type: application/json
```

```json
{
  "userId": "u1",
  "latitude": 28.4595,
  "longitude": 77.0266,
  "accuracyMeters": 8.5,
  "timestamp": "2026-09-25T06:30:00Z"
}
```

The location API responds with `202 Accepted`. The event is processed asynchronously before it becomes searchable. The `nearby-service` does not provide a location-write endpoint.

## Postman

Import [`postman/nearby.postman_collection.json`](postman/nearby.postman_collection.json) into Postman. Set the collection's `baseUrl`, `userId`, and `radiusKm` variables as needed. A location for the user and active friendship rows must already exist for results to be returned.
