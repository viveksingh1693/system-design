# ADR-004: Order Transactions, Lifecycle, and Failure Management

- **Status:** Accepted
- **Date:** 2026-08-23
- **Decision Owners:** Solution Architecture / Engineering
- **Scope:** Global order-management platform
- **Related ADRs:**
  - ADR-001: Globally Scalable Order-Management Architecture
  - ADR-002: Global Authentication & Authorization
  - ADR-003: Polyglot Database Strategy

---

## 1. Context

The global order-management platform spans multiple independently deployable services:

- Order
- Inventory
- Payment
- Fulfillment
- Notification
- Customer
- Pricing

A single customer order can therefore involve multiple databases and services.

A traditional distributed ACID transaction across all services would introduce significant coupling, latency, availability dependencies, and operational complexity.

The system must instead provide **business-level consistency** while remaining resilient to:

- Duplicate requests
- Service failures
- Database failures
- Kafka failures
- Consumer crashes
- Payment timeouts
- Out-of-order events
- Regional failures
- Retry storms
- Poison messages
- Compensation failures

---

# 2. Problem Statement

We need to manage the complete order lifecycle while ensuring:

1. No duplicate orders from client retries.
2. Inventory is not oversold.
3. Payments are not accidentally charged twice.
4. Each service maintains transactional integrity over its own data.
5. Cross-service workflows recover from partial failures.
6. Events are not lost between database commits and Kafka publication.
7. Duplicate event delivery does not corrupt business state.
8. Long-running workflows survive service and regional failures.
9. Failed operations can be compensated or reconciled.
10. Operators can identify and recover exceptional states.

---

# 3. Decision

We will implement order processing using:

- **Explicit order state machine**
- **Local ACID transactions**
- **Saga pattern**
- **Workflow orchestration**
- **Transactional Outbox**
- **Kafka for durable event transport**
- **Idempotency keys**
- **Idempotent consumers**
- **Optimistic concurrency / state versioning**
- **Retry with exponential backoff**
- **Circuit breakers**
- **Retry topics and DLQs**
- **Compensating transactions**
- **Durable workflow state**
- **Operational reconciliation for exceptional cases**

We will **not** attempt to implement a single distributed ACID transaction across Order, Inventory, Payment and Fulfillment.

---

# 4. Core Principle

> **Atomicity is guaranteed locally; business consistency is coordinated globally.**

Each service owns its own database transaction.

Cross-service consistency is achieved through:

```text
Local ACID
     +
Durable Events
     +
Saga
     +
Idempotency
     +
Compensation
     +
Reconciliation
```

---

# 5. Order State Machine

The Order Service will maintain an explicit state machine.

```text
                         ┌──────────────┐
                         │    CREATED   │
                         └──────┬───────┘
                                │
                                ↓
                         ┌──────────────┐
                         │    PENDING   │
                         │  INVENTORY   │
                         └──────┬───────┘
                                │
                    ┌───────────┴───────────┐
                    │                       │
                  SUCCESS                 FAILURE
                    │                       │
                    ↓                       ↓
             INVENTORY_RESERVED        CANCELLED
                    │
                    ↓
             ┌──────────────┐
             │   PENDING    │
             │   PAYMENT    │
             └──────┬───────┘
                    │
             ┌──────┴───────┐
             │              │
           SUCCESS        FAILURE
             │              │
             ↓              ↓
       PAYMENT_AUTHORIZED  RELEASE
             │             INVENTORY
             │              │
             ↓              ↓
        ORDER_CONFIRMED  CANCELLED
             │
             ↓
       FULFILLMENT_PENDING
             │
             ↓
          SHIPPED
             │
             ↓
         DELIVERED
```

Additional states may include:

```text
INVENTORY_FAILED
PAYMENT_FAILED
FULFILLMENT_FAILED
REFUND_PENDING
REFUNDED
CANCELLED
COMPENSATION_PENDING
MANUAL_REVIEW
```

---

# 6. State Transition Rules

Order state transitions must be explicit.

Conceptually:

```text
Current State
     +
Event
     +
Expected Version
     ↓
Validate Transition
     ↓
New State
     +
Version + 1
```

Example:

```text
PENDING_PAYMENT
        +
PaymentAuthorized
        ↓
CONFIRMED
```

An invalid transition such as:

```text
PENDING_PAYMENT
        +
InventoryReserved
```

must not silently modify the order.

---

# 7. Order Database Model

A simplified Order table:

```text
orders
--------------------------------
order_id
customer_id
status
version
total_amount
currency
created_at
updated_at
```

The `version` field supports optimistic concurrency.

Example:

```sql
UPDATE orders
SET status = :new_status,
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE order_id = :order_id
  AND version = :expected_version;
```

If zero rows are updated, the caller knows that another transition occurred.

---

# 8. Order Creation Transaction

The Order Service uses a local ACID transaction.

```text
BEGIN TRANSACTION

    INSERT INTO orders
        status = PENDING_INVENTORY

    INSERT INTO outbox_events
        event_type = OrderCreated
        aggregate_id = order_id
        event_id = UUID

COMMIT
```

Both the order and its event are committed atomically.

---

# 9. Transactional Outbox

We will use the Transactional Outbox pattern to prevent the database/event consistency problem.

### Problem without Outbox

```text
DB COMMIT
    ↓
Kafka Publish
    ↓
Kafka FAILURE
```

The database says:

```text
Order Created
```

but Kafka has no corresponding event.

### With Outbox

```text
                 SAME DB TRANSACTION
                         │
               ┌─────────┴─────────┐
               ↓                   ↓
          Order Table         Outbox Table
               │                   │
               └─────────┬─────────┘
                         ↓
                      COMMIT
                         │
                         ↓
                  Outbox Publisher
                         │
                         ↓
                       Kafka
```

If Kafka is unavailable, the event remains in the outbox and is retried.

---

# 10. Outbox Publisher

The publisher will:

1. Read unpublished outbox events.
2. Publish them to Kafka.
3. Confirm successful publication.
4. Mark the event as published.
5. Retry failures.

The publisher must itself be idempotent.

Example:

```text
Outbox Event
    ↓
Publish Kafka
    ↓
Mark Published
```

A crash between these operations can cause duplicate publication, therefore consumers must be idempotent.

---

# 11. Saga Pattern

The order workflow will use a Saga.

```text
Create Order
     │
     ↓
Reserve Inventory
     │
     ├── Failure → Cancel Order
     │
     ↓
Authorize Payment
     │
     ├── Failure → Release Inventory
     │                         │
     │                         ↓
     │                    Cancel Order
     │
     ↓
Confirm Order
     │
     ↓
Create Shipment
     │
     ↓
Order Complete
```

---

# 12. Saga Orchestration

For a complex global order workflow, orchestration is preferred over purely event-driven choreography.

Conceptually:

```text
                  Order Workflow
                        │
          ┌─────────────┼─────────────┐
          ↓             ↓             ↓
     Inventory       Payment      Fulfillment
       Service        Service        Service
```

The workflow maintains the business sequence, timeout rules, retry rules and compensation logic.

A workflow engine such as **Temporal** may be used for long-running workflows requiring durable state, timers and automatic retry behavior.

---

# 13. Why Orchestration?

Choreography can become difficult to reason about as the workflow grows:

```text
Order
 ↓
Inventory
 ↓
Payment
 ↓
Fulfillment
 ↓
Refund
 ↓
Notification
```

With orchestration:

```text
Order Workflow
    │
    ├── Reserve Inventory
    ├── Authorize Payment
    ├── Confirm Order
    └── Start Fulfillment
```

Benefits:

- Explicit workflow
- Centralized business process
- Easier timeout management
- Easier compensation
- Better visibility
- Easier operational recovery

The orchestrator does **not** own the business data of the individual services.

---

# 14. Kafka's Responsibility

Kafka provides:

- Durable event transport
- Event ordering within partitions
- Consumer groups
- Event retention
- Replay
- Decoupling

Kafka is **not** the order state machine.

```text
Kafka answers:
"How do we reliably transport events?"

Workflow answers:
"What should happen next?"
```

---

# 15. Kafka Partitioning

Events for a particular order should normally use:

```text
partitionKey = orderId
```

This preserves ordering for events belonging to the same order within Kafka.

Example:

```text
OrderCreated
InventoryReserved
PaymentAuthorized
OrderConfirmed
```

Global ordering is not required.

---

# 16. Idempotency

Distributed systems naturally produce duplicate requests and messages.

Every critical operation must therefore be idempotent.

## API Idempotency

Clients provide:

```http
Idempotency-Key: abc123
```

The system stores the result associated with the key.

```text
Request
   │
   ↓
Idempotency Check
   │
   ├── Existing → Return previous result
   │
   └── New → Process request
```

A database uniqueness constraint provides an additional safety boundary.

Example:

```text
UNIQUE(customer_id, idempotency_key)
```

---

# 17. Idempotent Consumers

Example:

```text
OrderCreated
      ↓
Inventory Consumer
      ↓
Reserve Inventory
      ↓
DB COMMIT
      ↓
Consumer crashes before offset commit
      ↓
Kafka redelivers event
```

The consumer must detect that the event was already processed.

Possible table:

```text
processed_events
-------------------------
event_id
consumer_name
processed_at
```

Processing:

```text
event received
     │
     ↓
Already processed?
     │
 ┌───┴────┐
YES       NO
 │         │
Ignore    Process
           │
           ↓
      Mark Processed
```

---

# 18. Payment Idempotency

Payment requires additional protection.

Use:

```text
paymentId
+
business idempotency key
+
provider idempotency key
```

Example:

```text
Order ID:  ORD-123
Payment ID: PAY-456
Idempotency Key: ORD-123-PAYMENT
```

If the payment request times out, the system must not create a new payment attempt with a different key.

It should:

```text
Query payment status
        OR
Retry with same idempotency key
```

---

# 19. Failure Modes

## 19.1 Duplicate Order Request

```text
Client Retry
     ↓
Idempotency Key
     ↓
Existing Order
     ↓
Return Existing Result
```

---

## 19.2 Inventory Failure

```text
OrderCreated
     ↓
Reserve Inventory
     ↓
FAIL
     ↓
InventoryFailed
     ↓
Cancel Order
```

Payment should not be initiated if inventory reservation has failed.

---

## 19.3 Payment Failure

```text
Inventory Reserved
     ↓
Payment Failed
     ↓
Release Inventory
     ↓
Cancel Order
```

The inventory release is a compensating transaction.

---

## 19.4 Payment Timeout

This is different from payment failure.

```text
Payment Request
     ↓
Timeout
     ↓
UNKNOWN STATE
```

The system must not immediately assume failure.

Instead:

```text
Query Payment Status
       │
   ┌───┴─────────┐
   ↓             ↓
SUCCESS        FAILED
   ↓             ↓
Confirm        Release
Order          Inventory
```

---

## 19.5 Kafka Failure

```text
Order DB
   ↓
Outbox
   ↓
Kafka DOWN
   ↓
Retry
   ↓
Kafka UP
   ↓
Publish Event
```

The order event is not lost because it is persisted in the outbox.

---

## 19.6 Consumer Crash

```text
DB COMMIT
   ↓
Consumer crashes
   ↓
Offset not committed
   ↓
Kafka redelivery
   ↓
Idempotent Consumer
   ↓
No duplicate business effect
```

---

## 19.7 Database Failure

The service should:

- Fail fast
- Use bounded retries
- Apply exponential backoff
- Use circuit breakers
- Keep workflow state durable
- Retry later

Avoid unlimited retries.

---

## 19.8 Service Failure

For a failed service:

```text
Timeout
  ↓
Retry
  ↓
Retry
  ↓
Circuit Breaker
  ↓
Compensation / Pending
```

Retry policies should define:

- Maximum attempts
- Backoff
- Jitter
- Timeout
- Retryable errors
- Non-retryable errors

---

## 19.9 Poison Message

```text
Kafka
  ↓
Consumer
  ↓
Failure
  ↓
Retry Topic
  ↓
Retry
  ↓
Failure
  ↓
DLQ
  ↓
Alert / Manual Intervention
```

A poison message should not block normal processing indefinitely.

---

## 19.10 Out-of-Order Event

The Order Service validates:

```text
Current State
+
Event Type
+
Expected Version
```

If the event is invalid:

```text
Reject
or
Buffer
or
Reconcile
```

Kafka partitioning by `orderId` reduces ordering problems for events from the same order.

---

## 19.11 Region Failure

```text
Region A
   X
   │
   ↓
Global Traffic Management
   │
   ↓
Region B
```

However, in-flight workflows must be recoverable.

Workflow state must therefore be durable and accessible/reconstructable in the failover region.

---

## 19.12 Compensation Failure

Example:

```text
Payment succeeded
       ↓
Release Inventory
       ↓
Inventory Service fails
```

The workflow enters:

```text
COMPENSATION_PENDING
```

and retries.

If repeated failures occur:

```text
Retry
  ↓
Retry
  ↓
Alert
  ↓
Manual Reconciliation
```

Compensating operations must themselves be idempotent.

---

# 20. Failure Matrix

| Failure | Detection | Recovery |
|---|---|---|
| Duplicate order | Idempotency key | Return existing order |
| Inventory failure | Event/result | Cancel order |
| Payment failure | Payment event | Release inventory + cancel |
| Payment timeout | Timeout/status check | Query/retry with same key |
| Kafka unavailable | Producer failure | Outbox retry |
| Consumer crash | Offset/redelivery | Idempotent processing |
| Database failure | Health/connection errors | Backoff + retry |
| Service unavailable | Timeout | Retry/circuit breaker |
| Poison message | Repeated processing failure | Retry topic → DLQ |
| Out-of-order event | State/version validation | Buffer/reconcile |
| Region failure | Health checks | Regional failover |
| Compensation failure | Workflow state | Retry + reconciliation |

---

# 21. Retry Strategy

Retries must be selective.

### Retryable

- Network timeout
- Temporary database unavailable
- HTTP 429
- HTTP 503
- Temporary Kafka failure

### Usually non-retryable

- Invalid request
- Authentication failure
- Authorization failure
- Invalid order state
- Invalid payment details
- Schema validation failure

Use:

```text
Exponential Backoff
+
Jitter
+
Maximum Attempts
+
Timeout
+
Circuit Breaker
```

Example:

```text
1 sec
  ↓
2 sec
  ↓
4 sec
  ↓
8 sec
  ↓
DLQ / Compensation
```

Actual limits must be workload-specific.

---

# 22. Preventing Retry Storms

If a dependency is unavailable:

```text
100K requests
     ↓
100K retries
     ↓
Dependency becomes even more overloaded
     ↓
More failures
     ↓
More retries
```

This creates a feedback loop.

Mitigations:

- Exponential backoff
- Jitter
- Circuit breakers
- Bounded retries
- Rate limiting
- Bulkheads
- Queue-based buffering

---

# 23. Exactly-Once Processing

We will **not rely on exactly-once delivery as the primary correctness mechanism**.

Instead:

```text
At-least-once delivery
        +
Idempotent processing
        +
Conditional state transitions
        +
Unique constraints
        +
Business idempotency
```

This is more robust.

For financial operations:

```text
Payment
+
Idempotency key
+
Provider idempotency
+
Reconciliation
```

---

# 24. Order Cancellation

Cancellation depends on the order state.

Example:

```text
CREATED
   ↓
Can cancel

PENDING_INVENTORY
   ↓
Can cancel

INVENTORY_RESERVED
   ↓
Cancel → Release Inventory

PAYMENT_AUTHORIZED
   ↓
Cancel → Void/Refund Payment
             +
          Release Inventory

SHIPPED
   ↓
Cannot directly cancel
   ↓
Return workflow
```

Therefore cancellation itself is a Saga.

---

# 25. Refund Workflow

```text
Return Requested
      ↓
Return Approved
      ↓
Inventory / Warehouse
      ↓
Refund Initiated
      ↓
Payment Provider
      ↓
Refund Completed
```

Again:

```text
Refund
+
Idempotency
+
State Machine
+
Retry
+
Reconciliation
```

---

# 26. Reconciliation

No distributed system is perfect.

A reconciliation process should periodically compare:

```text
Order State
      vs
Payment State
      vs
Inventory State
      vs
Fulfillment State
```

Examples:

```text
Order = CONFIRMED
Payment = FAILED
```

or:

```text
Payment = SUCCESS
Order = PAYMENT_PENDING
```

These discrepancies should trigger:

```text
Automated repair
or
Manual review
```

This is particularly important for financial operations.

---

# 27. Observability

Track every workflow using:

```text
correlationId
orderId
eventId
paymentId
workflowId
```

Example:

```text
orderId = ORD-123

API
 ↓
Order Service
 ↓
Kafka
 ↓
Inventory
 ↓
Payment
 ↓
Fulfillment
```

All logs/traces should be correlated to the same business transaction.

Important metrics:

```text
Orders created/sec
Orders failed/sec
Order processing latency
Saga duration
Saga failure rate
Compensation rate
Payment timeout rate
Inventory reservation failure rate
Kafka consumer lag
Outbox backlog
DLQ size
Reconciliation discrepancies
```

---

# 28. Complete Architecture

```text
                         CLIENT
                           │
                           ↓
                     API Gateway
                           │
                    Idempotency Check
                           │
                           ↓
                     Order Service
                           │
                 ┌─────────┴─────────┐
                 ↓                   ↓
             Order DB           Outbox Table
                 │                   │
                 └─────────┬─────────┘
                           │
                        COMMIT
                           │
                           ↓
                   Outbox Publisher
                           │
                           ↓
                         Kafka
                           │
                           ↓
                    Order Workflow
                           │
             ┌─────────────┼─────────────┐
             ↓             ↓             ↓
        Inventory       Payment      Fulfillment
          Service        Service        Service
             │             │             │
             ↓             ↓             ↓
          Local DB      Local DB      Local DB
             │             │             │
             └─────────────┼─────────────┘
                           ↓
                       Domain Events
                           │
                           ↓
                    Order State Machine
                           │
                           ↓
                 Notification / Analytics
```

---

# 29. Alternatives Considered

## Distributed Two-Phase Commit

**Rejected.**

Reasons:

- Blocking behavior
- Higher latency
- Tight coupling
- Coordinator dependency
- Poor fit for independent microservices
- Difficult global failure handling

---

## Shared Database Transaction

**Rejected.**

Reasons:

- Violates service ownership
- Creates tight coupling
- Limits independent scaling
- Creates deployment dependencies

---

## Pure Choreography

**Not selected as the primary strategy.**

Choreography is useful for simple event-driven interactions, but the order lifecycle contains enough steps, timeouts and compensations that explicit orchestration provides better control.

---

## Synchronous REST Chain

**Rejected for the complete workflow.**

Example:

```text
Order
 ↓
Inventory
 ↓
Payment
 ↓
Fulfillment
```

Problems:

- Cascading failures
- Higher latency
- Tight runtime coupling
- Poor resilience
- Difficult long-running workflows

REST remains appropriate for synchronous queries and commands where immediate response is required.

---

# 30. Consequences

## Positive

- Strong local transactional integrity
- Resilient distributed workflow
- Safe retries
- Clear order lifecycle
- Fault isolation
- Replayable events
- Better operational visibility
- Recovery from partial failures

## Negative

- Eventual consistency
- Saga complexity
- More infrastructure
- Workflow management overhead
- More complicated debugging
- Need for reconciliation
- Duplicate events must be handled
- Compensation logic must be maintained

---

# 31. Key Architectural Principles

### Principle 1

> **Do not use distributed ACID transactions as the default solution for microservice workflows.**

### Principle 2

> **Every critical business operation must be idempotent.**

### Principle 3

> **Persist state before publishing events using the Transactional Outbox pattern.**

### Principle 4

> **Every Saga step must have a timeout, retry policy and failure path.**

### Principle 5

> **Every compensation must itself be idempotent and recoverable.**

### Principle 6

> **Unknown outcomes must not be treated as failures automatically.**

This is particularly important for payment.

### Principle 7

> **The order state machine is the source of truth for order lifecycle.**

### Principle 8

> **Kafka transports events; it does not own business workflow state.**

---

# 32. Interview-Ready Answer

If asked:

> **"How would you manage transactions and order lifecycle in a globally distributed system?"**

Use this answer:

> "I would model the order lifecycle as an explicit state machine and avoid a distributed ACID transaction across Order, Inventory, Payment and Fulfillment.
>
> Each service would own its data and use local ACID transactions. The overall business transaction would be coordinated using a Saga, preferably an orchestrated workflow for a complex order lifecycle.
>
> For reliable event publication, I would use the Transactional Outbox pattern: the service commits its business state and the corresponding event in the same database transaction, and an outbox publisher sends the event to Kafka.
>
> Every operation would be idempotent. The order API would use an idempotency key, Kafka consumers would deduplicate events, and payment operations would use a stable business idempotency key, ideally propagated to the payment provider.
>
> Failures would be handled using bounded retries, exponential backoff, circuit breakers, compensation and DLQs. For example, if inventory succeeds but payment fails, the Saga releases the inventory and cancels the order. If payment times out, I don't assume failure—I query or retry using the same idempotency key.
>
> Finally, I'd maintain durable workflow state and reconciliation processes so that regional failures or partial failures can be recovered rather than losing the business transaction.
>
> So the overall model is: **local ACID + transactional outbox + Kafka + Saga + idempotency + compensation + reconciliation.**"

---

# 33. The One-Line Principle to Remember

> **“We don't try to make a distributed system behave like one database transaction; we make every step atomic, every message replayable, every operation idempotent, and every failure recoverable.”**
