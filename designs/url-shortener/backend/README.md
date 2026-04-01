# URL Shortener Backend

Spring Boot backend for the URL shortener, paired with a React frontend in `../frontend`.

## Features

- Short link creation with generated or custom aliases
- Redirect resolution with redirect counts
- Disable support for links
- Redis-backed caching for short URL metadata lookups
- Validation and centralized error handling
- OpenAPI JSON/YAML plus Swagger UI
- Actuator health, metrics, and Prometheus scraping
- Request correlation IDs in logs

## Run locally

```bash
mvn spring-boot:run
```

The backend runs on `http://localhost:8080` by default and allows local frontend requests from `http://localhost:5173`.

## Redis Cache

- Cache provider: Redis
- Cached data: `GET /api/v1/urls/{shortCode}` metadata responses
- Cache name: `shortUrlByCode`
- Default TTL: `10m`
- Invalidation:
  - create: cache populated with the new response
  - fetch: served from cache when available
  - redirect: cache evicted so redirect count and last-accessed time do not go stale
  - disable: cache updated with the disabled response

Runtime Redis settings:

- `REDIS_HOST`
- `REDIS_PORT`
- `SHORT_URL_CACHE_TTL`

Tests use the `test` profile with Spring's simple in-memory cache, so Redis is not required for `mvn test`.

## API and Docs

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`
- OpenAPI YAML: `/v3/api-docs.yaml`
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## Example Create Request

```http
POST /api/v1/urls
Content-Type: application/json

{
  "originalUrl": "https://example.com",
  "customAlias": "docs2026",
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

## Docker

Build the backend image:

```bash
docker build -t url-shortener-backend .
```

Run it with Redis:

```bash
docker run --rm -p 8080:8080 \
  -e REDIS_HOST=host.docker.internal \
  -e REDIS_PORT=6379 \
  url-shortener-backend
```

This default container run uses the in-memory H2 database plus Redis caching. Add `SPRING_PROFILES_ACTIVE=prod` together with PostgreSQL environment variables when you want the production database profile.

## Production Notes

- Use the `prod` profile for PostgreSQL-backed deployments.
- Set `APP_BASE_URL`, `FRONTEND_BASE_URL`, `REDIS_HOST`, `REDIS_PORT`, `SHORT_URL_CACHE_TTL`, `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
