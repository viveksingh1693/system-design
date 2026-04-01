# Application Gateway

Spring Cloud Gateway edge service for the URL shortener workspace.

## Features

- Routes backend API, docs, actuator, and redirect traffic
- Secures non-public endpoints with HTTP Basic authentication
- Keeps redirects and health probes publicly accessible
- Applies CORS for local frontend development

## Run locally

```bash
mvn spring-boot:run
```

The gateway runs on `http://localhost:8081` by default and forwards requests to `http://localhost:8080`.

Default development credentials:

- Username: `gateway-admin`
- Password: `change-me-please`

Override them with `GATEWAY_USERNAME`, `GATEWAY_PASSWORD`, and `GATEWAY_ROLE`.

## Routed Endpoints

- `/api/**`
- `/v3/api-docs`
- `/v3/api-docs/**`
- `/swagger-ui.html`
- `/swagger-ui/**`
- `/actuator/**`
- `/{shortCode}`

## Docker

```bash
docker build -t application-gateway .
docker run --rm -p 8081:8081 \
  -e BACKEND_BASE_URL=http://host.docker.internal:8080 \
  -e GATEWAY_USERNAME=gateway-admin \
  -e GATEWAY_PASSWORD=change-me-please \
  application-gateway
```
