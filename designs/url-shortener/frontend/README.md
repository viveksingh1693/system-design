# URL Shortener Frontend

React + Vite + TypeScript SPA for creating, inspecting, and disabling short URLs.

## Run locally

```bash
npm install
npm run dev
```

## Environment

Copy `.env.example` to `.env` if you want to override defaults.

- `VITE_API_BASE_URL`: optional gateway base URL
- `VITE_DOCS_URL`: optional Swagger UI URL

## Docker

```bash
docker build -t url-shortener-frontend .
docker run --rm -p 4173:80 url-shortener-frontend
```

The container serves the SPA with Nginx and proxies `/api`, `/actuator`, `/v3`, and Swagger UI requests to the `application-gateway` service when used in Docker Compose.
