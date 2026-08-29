# ADR-001: Globally Scalable Order-Management Architecture

- **Status:** Accepted
- **Date:** 2026-08-22
- **Decision Owners:** Solution Architecture / Engineering
- **Scope:** Global order-management platform

## 1. Context

We need a globally scalable order-management platform for an e-commerce business operating across multiple geographic regions. It must support order creation, inventory reservation, payment, fulfillment, tracking, notifications, returns, and refunds while maintaining high availability and correctness.

Initial assumptions (to be validated):

| Metric              |                        Assumption |
| ------------------- | --------------------------------: |
| Global users        |                             100M+ |
| Orders/day          |                               10M |
| Peak order creation |                    50K orders/sec |
| Read/write ratio    |                             ~10:1 |
| Availability        |                           99.99%+ |
| API latency         |                      p99 < 300 ms |
| Regions             |                          Multiple |
| RTO                 |                      < 30 minutes |
| RPO                 | Near-zero for critical order data |

## 2. Problem Statement

The platform must scale horizontally, support multiple regions, survive regional failures, prevent inventory overselling, maintain correct order/payment state, handle distributed transactions and traffic spikes, and provide strong observability.

## 3. Decision

Use a **multi-region microservices architecture** organized around business capabilities, with:

- Global DNS/CDN/WAF and traffic management
- API Gateway
- Spring Boot microservices
- Kubernetes
- Kafka for event streaming
- Database-per-service
- Redis or equivalent caching
- Saga-based distributed workflows
- Idempotency for safe retries
- OpenTelemetry-based observability
- Active-active stateless services
- Domain-specific strategies for transactional data

Transactional databases will **not** be forced into global active-active mode. Data ownership and replication will be selected per business domain.

## 4. High-Level Architecture

```text
                         GLOBAL USERS
                              |
                              v
                       DNS / CDN / WAF
                              |
                              v
                     Global Load Balancer
                              |
            +-----------------+-----------------+
            v                 v                 v
         REGION A          REGION B          REGION C
            |                 |                 |
       API Gateway       API Gateway       API Gateway
            |                 |                 |
            +-----------------+-----------------+
                              |
                    +---------+---------+
                    |   Microservices   |
                    |                   |
                    | Order             |
                    | Inventory         |
                    | Payment           |
                    | Fulfillment       |
                    | Customer          |
                    | Pricing           |
                    | Notification      |
                    +---------+---------+
                              |
              +---------------+----------------+
              v               v                v
           Kafka            Redis          Databases
                                             |
                                      +------+------+
                                      v      v      v
                                    Order Inventory Payment
                                      DB      DB      DB
```

## 5. Service Boundaries

Services are based on business capabilities and data ownership:

- **Order Service:** order creation, state, lifecycle, history
- **Inventory Service:** stock, availability, reservation, release
- **Payment Service:** authorization, capture, payment state, refund
- **Fulfillment Service:** warehouse assignment, shipment, delivery
- **Pricing Service:** pricing, discounts, promotions
- **Customer Service:** profile, addresses, preferences
- **Notification Service:** email, SMS, push

## 6. Communication Strategy

### Synchronous

Use REST when an immediate response is required and the interaction is request/response oriented.

### Asynchronous

Use Kafka for durable events, high-throughput processing, loose coupling, multiple consumers, and replayability.

Example events:

```text
OrderCreated
InventoryReserved
InventoryReservationFailed
PaymentAuthorized
PaymentFailed
OrderConfirmed
OrderCancelled
ShipmentCreated
ShipmentDelivered
RefundInitiated
RefundCompleted
```

## 7. Order Creation Workflow

```text
Customer
   |
   | POST /orders
   v
API Gateway
   |
   v
Order Service
   |
   +-- Validate request
   +-- Create PENDING order
   +-- Publish OrderCreated
             |
             v
           Kafka
             |
       +-----+----------+
       v     v          v
 Inventory Payment   Notification
 Service   Service
```

## 8. Distributed Transaction Strategy

Use the **Saga pattern** instead of a distributed ACID transaction across Order, Inventory, Payment, and Fulfillment.

```text
Order Created
      |
      v
Reserve Inventory
      |
      +-- Failed --> Cancel Order
      |
      v
Authorize Payment
      |
      +-- Failed --> Release Inventory --> Cancel Order
      |
      v
Confirm Order
      |
      v
Start Fulfillment
```

## 9. Idempotency

Distributed delivery can result in duplicate events. Consumers must be idempotent.

```text
ProcessedEvents
-------------------------
event_id
consumer_name
processed_at
```

Business APIs such as order creation and payment operations should also use idempotency keys.

## 10. Kafka Strategy

Kafka is the primary event-streaming platform.

- Use appropriate partition keys.
- Use `orderId` where per-order ordering is required.
- Use replication for fault tolerance.
- Use consumer groups for independent processing.
- Monitor consumer lag.
- Use retry topics and DLQs where appropriate.
- Govern event schemas.

Kafka ordering is guaranteed within a partition, not globally.

## 11. Database Strategy

Each service owns its transactional data:

```text
Order Service       -> Order DB
Inventory Service   -> Inventory DB
Payment Service     -> Payment DB
Customer Service    -> Customer DB
```

A shared database is avoided because it creates tight coupling, shared schema ownership, scaling constraints, and deployment dependencies.

## 12. Global Data Strategy

Consistency and replication are selected by domain.

- **Catalog:** regional replicas, CDN/cache, eventual consistency where acceptable
- **Order:** regional ownership plus replication; strong consistency where required
- **Inventory:** regional/warehouse ownership, atomic reservation, concurrency control
- **Customer:** strategy driven by latency, privacy, and data-residency requirements
- **Payment:** provider, compliance, regional, and financial-correctness requirements

## 13. Inventory Reservation

Inventory is a critical consistency boundary. Atomic reservation, optimistic concurrency control, partition ownership, reservation records, and short TTLs are candidate mechanisms.

For one available unit:

```text
Customer A -> Reserve 1 -> SUCCESS
Customer B -> Reserve 1 -> FAILURE
```

The system must never allow both requests to succeed and produce negative inventory.

## 14. Caching

Redis or equivalent may cache product data, pricing, suitable order summaries, and session information. Transactional state must not be cached without explicit consistency semantics.

## 15. Multi-Region Strategy

Stateless services use active-active deployment:

```text
Region A ----+
Region B ----+---- Global Traffic Management
Region C ----+
```

Transactional data uses domain-specific strategies rather than universal active-active writes.

## 16. Failure Handling

The design must handle service, broker, database, network, and regional failures.

Examples:

```text
Payment unavailable
-> PENDING_PAYMENT
-> bounded retry
-> timeout
-> cancel/manual review
```

```text
Inventory unavailable
-> PENDING_INVENTORY
-> bounded retry
-> failure
-> cancel order
```

Critical operations must not be acknowledged as successfully persisted when their required durable event/state has not been recorded.

## 17. Traffic Scaling

Stateless services scale horizontally through Kubernetes. Scaling signals may include CPU, memory, request rate, custom metrics, and Kafka consumer lag.

Kafka can absorb bursts between producers and downstream consumers.

## 18. Observability

Use logs, metrics, and traces with capabilities such as OpenTelemetry, Prometheus, Grafana, centralized logging, and distributed tracing.

Important metrics include:

- Order creation/failure rate
- Order processing latency
- Payment failures
- Inventory reservation failures
- Kafka consumer lag and throughput
- Database latency
- Connection-pool utilization
- API p95/p99

Propagate a correlation ID through the full workflow.

## 19. Security

```text
Client
  |
  v
WAF
  |
  v
API Gateway
  |
  v
Authentication / Authorization
  |
  v
Services
```

Controls include OAuth 2.0/OIDC, TLS/mTLS where appropriate, secrets management, encryption, RBAC, least privilege, audit logging, and payment-data isolation.

## 20. Disaster Recovery

Business-defined RTO/RPO targets drive the final design. Example target for critical order data:

```text
RPO -> near zero
RTO -> < 30 minutes
```

Recovery must cover databases, Kafka, services, traffic failover, data validation, idempotent replay, and operational runbooks.

## 21. Alternatives Considered

| Option | Decision | Rationale |
|---|---|---|
| Monolith | Rejected for target scale | Limits independent scaling/deployment and creates stronger coupling |
| Shared database | Rejected | Creates tight coupling and shared schema ownership |
| REST everywhere | Rejected | Creates runtime coupling, cascading failures, and poor burst handling |
| Distributed ACID transactions | Rejected | High complexity; Saga better fits cross-service workflows |
| Global active-active DB everywhere | Rejected | Unnecessary conflict and consistency complexity across domains |
| Microservices + Kafka + domain-specific data strategy | **Selected** | Best balance of scalability, resilience, autonomy, and business correctness |

## 22. Key Architectural Decisions

| Decision | Rationale |
|---|---|
| Microservices | Independent business capabilities |
| REST | Synchronous request/response |
| Kafka | Durable asynchronous event streaming |
| Saga | Distributed workflow coordination |
| Idempotency | Safe retries and duplicate delivery |
| Database-per-service | Clear data ownership |
| Redis | Low-latency read-heavy workloads |
| Kubernetes | Horizontal scaling and orchestration |
| Multi-region | Availability and latency |
| Active-active stateless services | Regional resilience |
| Domain-specific data strategy | Avoid unnecessary global consistency complexity |
| Observability | Distributed-system diagnosis |
| Global traffic management | Regional failover |

## 23. Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Inventory overselling | Atomic reservation / concurrency control |
| Duplicate orders | API idempotency keys |
| Duplicate events | Idempotent consumers |
| Kafka lag | Monitoring and autoscaling |
| Regional outage | Multi-region deployment |
| Data inconsistency | Domain-specific consistency strategy |
| Cascading failure | Timeouts, retries, circuit breakers |
| Retry storms | Exponential backoff and bounded retries |
| Event schema incompatibility | Schema governance |
| Database hotspot | Partitioning and appropriate ownership |
| Operational complexity | Platform automation and observability |

## 24. Consequences

### Positive

- Global scalability
- Regional resilience
- Independent service scaling and deployment
- Event-driven decoupling
- Fault isolation
- High throughput
- Clear domain ownership
- Event replay capability

### Negative

- Distributed-system complexity
- Higher operational overhead
- Network and broker failures
- Eventual consistency
- Distributed tracing requirements
- Kafka operational complexity
- Data replication complexity
- Saga compensation complexity

## 25. Success Criteria

The architecture is successful when it provides:

- Required global throughput
- 99.99%+ availability target
- Predictable p95/p99 latency
- No incorrect inventory reservation
- No duplicate financial transactions
- Controlled failure recovery
- Regional failover capability
- End-to-end observability
- Independent service scaling
- Acceptable operational cost

## 26. Key Architecture Principle

> **Do not make the entire platform uniformly active-active just because it is a global system.**
>
> Global scalability should be designed domain by domain. Business correctness comes first, followed by scale and availability, while avoiding unnecessary distributed-system complexity.

## 27. Interview Summary

> “I would design the order-management platform as a multi-region architecture with stateless microservices, global traffic management, Kafka for asynchronous event streaming, database-per-service ownership, Redis for appropriate read-heavy workloads, and Saga-based workflow coordination.
>
> The most important design considerations are inventory consistency, payment correctness, idempotency, distributed transactions, regional failure, and data ownership. I would use active-active deployment for stateless services, but domain-specific strategies for transactional data rather than forcing every database into global active-active mode.”
