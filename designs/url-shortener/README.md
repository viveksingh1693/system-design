# URL Shortener Workspace

This workspace is split into three sibling apps:

- `backend`: Spring Boot API using the `com.viv` base package
- `application-gateway`: Spring Cloud Gateway edge service with Spring Security
- `frontend`: React + Vite + TypeScript management UI
- `redis`: cache service used by the backend in normal runtime environments

## Architecture

- Spring Boot provides the URL management API, redirect endpoint, OpenAPI docs, and Redis-backed metadata caching.
- Spring Cloud Gateway provides edge routing, HTTP Basic authentication for protected endpoints, and frontend-facing CORS handling.
- React provides the dashboard and redirect helper UI.
- Redis stores cached short URL metadata to reduce repeated database lookups for management fetches.

## Run locally

### Backend

```bash
cd backend
mvn spring-boot:run
```

### Application Gateway

```bash
cd application-gateway
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Redis is optional for local tests because the test profile uses an in-memory cache. For normal local runtime, start Redis first:

```bash
docker run --rm -p 6379:6379 redis:7.4-alpine
```

## Docker Compose

From the workspace root:

```bash
docker compose up --build
```

Services:

- Frontend: `http://localhost:4173`
- Application Gateway: `http://localhost:8081`
- Backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- Redis: `localhost:6379`

## Docs

- Gateway Swagger UI: `http://localhost:8081/swagger-ui.html`
- Gateway OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- Backend Swagger UI: `http://localhost:8080/swagger-ui.html`
