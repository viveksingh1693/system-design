# Ticketmaster

Spring Boot 4 sample application that models a simplified ticket-booking system with clear service boundaries for catalog, inventory, booking, and payment.

It now also includes a modular React frontend under `frontend/` for discovery, operations, and booking workflows.

## What This Project Shows

- `CatalogService` for event creation and discovery
- `InventoryService` for show creation and seat reservation
- `BookingService` for ticket holds, confirmation, and cancellation
- `PaymentService` for payment authorization simulation
- H2-backed persistence with pessimistic locking on seat inventory
- OpenAPI and Swagger UI for exploring the flow end to end

## Tech Stack

- Java 21
- Spring Boot 4.0.5
- Spring Data JPA
- H2
- Spring Validation
- springdoc-openapi
- React 18
- Vite
- TypeScript

## Domain Model

- `Event`: artist, venue, category, and city metadata
- `Show`: dated performance with price, capacity, and available seats
- `Booking`: customer hold or confirmed reservation against a show

## Service Boundaries

- `catalog`: event onboarding and search
- `inventory`: show management plus seat locking and release
- `booking`: orchestration around holds, checkout, expiry, and cancellation
- `payment`: isolated payment authorization simulation

This runs as a modular monolith, but the packages are separated so they can be discussed as independent services in a system design interview.

## Architecture Diagram

```mermaid
flowchart LR
    U[Client or Admin]
    API[Spring Boot API Layer]

    subgraph TM[Ticketmaster Modular Monolith]
        CAT[Catalog Service]
        INV[Inventory Service]
        BOOK[Booking Service]
        PAY[Payment Service]
        ERR[Global Exception and OpenAPI Layer]
    end

    DB[(H2 / Relational Store)]

    U --> API
    API --> CAT
    API --> INV
    API --> BOOK
    API --> ERR

    CAT --> DB
    INV --> DB
    BOOK --> DB
    BOOK --> PAY
    PAY --> BOOK
```

## Booking Flow Diagram

```mermaid
sequenceDiagram
    actor User
    participant BookingAPI as Booking API
    participant BookingService as Booking Service
    participant InventoryService as Inventory Service
    participant PaymentService as Payment Service
    participant DB as Database

    User->>BookingAPI: POST /bookings/hold
    BookingAPI->>BookingService: createHold(showId, seatCount, customerEmail)
    BookingService->>DB: find expired holds for show
    BookingService->>InventoryService: reserveSeats(showId, seatCount)
    InventoryService->>DB: lock show row and decrement seats
    BookingService->>DB: save HELD booking with expiry
    BookingService-->>User: hold created

    User->>BookingAPI: POST /bookings/{id}/confirm
    BookingAPI->>BookingService: confirmBooking(bookingId, paymentToken)
    BookingService->>DB: load booking and validate hold window
    BookingService->>PaymentService: authorizePayment()
    PaymentService-->>BookingService: payment reference
    BookingService->>DB: mark booking CONFIRMED
    BookingService-->>User: confirmed booking
```

## API Documentation

After starting the application:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Catalog group: `http://localhost:8080/v3/api-docs/catalog`
- Inventory group: `http://localhost:8080/v3/api-docs/inventory`
- Bookings group: `http://localhost:8080/v3/api-docs/bookings`

## Main Endpoints

- `POST /events`
- `GET /events?city=Bengaluru&q=Coldplay`
- `POST /events/{eventId}/shows`
- `GET /shows?city=Bengaluru&q=Coldplay`
- `GET /shows/{showId}`
- `POST /bookings/hold`
- `POST /bookings/{bookingId}/confirm`
- `POST /bookings/{bookingId}/cancel`
- `GET /bookings/{bookingId}`

## Frontend Modules

- `Discovery`: show search with abortable requests and short-lived client caching
- `Operations`: separate event onboarding and show launch workflows
- `Bookings`: hold, confirm, cancel, and inspect booking flows
- `Shared API client`: typed request helpers, centralized error handling, cache invalidation hooks

## Frontend Run

Start the backend first:

```bash
./mvnw spring-boot:run
```

In another terminal:

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server runs on `http://localhost:5173` and proxies API traffic to the backend on `http://localhost:8080`.

## Production Build Integration

The frontend build outputs directly to `src/main/resources/static`, so the backend can serve the SPA and API from the same origin:

```bash
cd frontend
npm run build
```

After that, Spring Boot serves:

- `/` for discovery
- `/operations` for the operations console
- `/bookings` for the booking desk
- `/assets/*` with long-lived immutable cache headers

## 1000 QPS Considerations

- Static frontend assets are hashed and can sit behind a CDN or edge cache.
- Client-side discovery reads are debounced via deferred updates, abort stale requests, and use short-lived in-memory caching.
- Spring Boot static delivery enables compression, HTTP/2, graceful shutdown, and higher Tomcat connection/thread ceilings for burstier traffic.
- Write-heavy booking actions invalidate relevant caches immediately to keep operators from acting on stale inventory.

## Example Flow

1. Create an event with `POST /events`.
2. Create a show for that event with `POST /events/{eventId}/shows`.
3. Hold seats with `POST /bookings/hold`.
4. Confirm the booking with `POST /bookings/{bookingId}/confirm`.
5. Fetch the booking or show to inspect the final state.

## Local Run

```bash
./mvnw spring-boot:run
```

The application uses an in-memory H2 database, so no external setup is required.

## Interview Preparation

See `INTERVIEW_PREPARATION.md` for senior-level interview questions, scalability tradeoffs, and discussion points for explaining this design in an interview.
