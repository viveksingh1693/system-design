# ADR-006: Global Payment Processing Platform Architecture

- **Status:** Accepted
- **Date:** 2026-08-24
- **Scope:** Global multi-merchant payment processing platform
- **Related ADRs:** Global Order Management, Authentication & Authorization, Database Strategy, Order Transactions & Failure Management, Observability

---

## 1. Context

A global payment platform must support:

- Payment creation, authorization, capture and void
- Full and partial refunds
- Multiple payment methods and currencies
- Multiple payment service providers (PSPs)
- Webhooks
- Fraud/risk checks
- Settlement
- Reconciliation
- Chargebacks/disputes
- Multi-region availability

Payment systems require stronger correctness guarantees than ordinary distributed applications. The primary risks are:

```text
Duplicate charge
Lost payment event
Incorrect payment state
Incorrect refund
Provider timeout with unknown outcome
Duplicate webhook
Concurrent capture/refund
Settlement mismatch
Ledger inconsistency
Provider outage
```

The architecture therefore prioritizes:

```text
Correctness
Idempotency
Auditability
Recoverability
Security
Availability
Scalability
```

---

# 2. Decision

We will build the payment platform around:

```text
Payment API
     |
     v
Payment Service
     |
     +-- Idempotency
     +-- Risk/Fraud
     +-- Payment State Machine
     +-- Provider Router
     +-- Provider Adapters
     +-- Ledger
     +-- Outbox
     |
     v
    Kafka
     |
 +---+----------+----------+
 |              |          |
 v              v          v
Orders      Settlement  Analytics

External PSPs
     |
     v
Webhook Processor
     |
     v
Payment Service

Payment DB
     |
     v
Reconciliation
     |
     +-- Provider APIs
     +-- Settlement files
     +-- Ledger
```

---

# 3. Core Architectural Decisions

1. Explicit payment state machine.
2. Idempotency for every mutating payment operation.
3. Stable provider idempotency keys.
4. Provider abstraction through adapters.
5. Provider routing based on business and health signals.
6. Transactional Outbox for reliable event publication.
7. Immutable double-entry ledger where the platform owns accounting.
8. Reconciliation as a first-class subsystem.
9. `UNKNOWN` is a valid payment state.
10. Conservative provider failover.
11. Tokenization instead of storing raw card data wherever possible.
12. Deterministic regional ownership/single-writer processing for payment writes.

---

# 4. Payment State Machine

```text
                  +-------------+
                  |  INITIATED  |
                  +------+------+
                         |
                         v
                  +-------------+
                  | PROCESSING  |
                  +------+------+
                         |
              +----------+----------+
              |          |           |
              v          v           v
         AUTHORIZED    FAILED     UNKNOWN
              |
        +-----+------+
        |            |
        v            v
     CAPTURED       VOIDED
        |
        v
 PARTIALLY_REFUNDED
        |
        v
    REFUNDED
```

Additional states:

```text
REQUIRES_ACTION
CANCELLED
DISPUTED
CHARGEBACK
```

---

# 5. Critical Decision: UNKNOWN

A provider timeout must **not** automatically become `FAILED`.

Example:

```text
Payment Service
      |
      v
External PSP
      |
      v
Charge succeeds
      |
      X
Network timeout
```

Our system sees:

```text
TIMEOUT
```

but the provider may have:

```text
SUCCESS
```

Therefore:

```text
TIMEOUT != FAILED
```

Persist:

```text
UNKNOWN
```

Then query/reconcile the provider:

```text
UNKNOWN
   |
   v
Provider Status Query
   |
 +--+---------+
 |            |
 v            v
SUCCESS      FAILED
 |            |
 v            v
CAPTURED     FAILED
```

This is the primary protection against double charging.

---

# 6. API Design

## Create Payment

```http
POST /v1/payments
Authorization: Bearer <token>
Idempotency-Key: ORD-123-PAYMENT
```

```json
{
  "orderId": "ORD-123",
  "amount": 4999.00,
  "currency": "INR",
  "paymentMethod": {
    "type": "CARD",
    "token": "pm_123"
  }
}
```

## Capture

```http
POST /v1/payments/{paymentId}/capture
Idempotency-Key: PAY-123-CAPTURE
```

## Refund

```http
POST /v1/payments/{paymentId}/refund
Idempotency-Key: PAY-123-REFUND-001
```

```json
{
  "amount": 1000.00,
  "reason": "CUSTOMER_RETURN"
}
```

---

# 7. Database Model

## payments

```sql
CREATE TABLE payments (
    payment_id          UUID PRIMARY KEY,
    merchant_id         UUID NOT NULL,
    order_id            UUID NOT NULL,
    amount              DECIMAL(19,4) NOT NULL,
    currency            CHAR(3) NOT NULL,
    status              VARCHAR(40) NOT NULL,
    payment_method_type VARCHAR(30),
    provider            VARCHAR(50),
    provider_reference  VARCHAR(200),
    version             BIGINT NOT NULL,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL
);
```

## Payment Attempts

A logical payment is separate from provider attempts:

```text
payments
    |
    +-- payment_attempts
          |
          +-- provider
          +-- attempt_number
          +-- provider_reference
          +-- status
          +-- response_code
          +-- latency
```

Example:

```text
PAY-123
  |
  +-- Attempt 1 -> Provider A -> TIMEOUT
  +-- Attempt 2 -> Provider A -> FAILED
  +-- Attempt 3 -> Provider B -> SUCCESS
```

---

# 8. Idempotency

Table:

```text
payment_idempotency
----------------------------
merchant_id
idempotency_key
request_hash
payment_id
response
status
created_at
expires_at
```

Constraint:

```text
UNIQUE(merchant_id, idempotency_key)
```

Flow:

```text
Request
   |
   v
Idempotency Key
   |
   +---- Existing ---> Return previous result
   |
   +---- New --------> Process
```

The same logical request must produce the same business outcome.

The request hash prevents reusing a key with a different request.

---

# 9. Provider Abstraction

Business logic must not depend directly on a specific PSP.

```java
interface PaymentProvider {

    AuthorizationResult authorize(PaymentRequest request);

    CaptureResult capture(CaptureRequest request);

    RefundResult refund(RefundRequest request);

    PaymentStatus getStatus(String providerReference);
}
```

Implementations:

```text
StripeProvider
AdyenProvider
ProviderX
ProviderY
```

---

# 10. Provider Router

Provider selection considers:

```text
Currency
Country
Payment Method
Merchant
Provider Availability
Success Rate
Latency
Cost
Regulatory Constraints
```

Example:

```text
India + UPI  -> Provider A
Europe + Card -> Provider B
US + Card -> Provider C
```

Provider health can be incorporated into routing.

---

# 11. Provider Failover

Unsafe:

```text
Provider A timeout
      |
      v
Provider B charge
```

Provider A may already have charged the customer.

Safe:

```text
Provider A timeout
      |
      v
UNKNOWN
      |
      v
Query / Reconcile Provider A
      |
      v
Determine whether retry is safe
      |
      v
Provider B if appropriate
```

**Failover must not create a second financial effect.**

---

# 12. Authorization and Capture

Authorization:

```text
Verify funds
+
Reserve funds
```

Capture:

```text
Actually collect funds
```

Example:

```text
Order Created
      |
      v
Authorize
      |
      v
Inventory / Fulfillment
      |
      v
Capture
```

The platform should also support immediate authorize-and-capture flows.

---

# 13. Capture and Refund Invariants

Track:

```text
authorized_amount
captured_amount
refunded_amount
```

Invariants:

```text
captured_amount <= authorized_amount

refunded_amount <= captured_amount
```

Concurrent capture/refund requests are protected using:

```text
Idempotency
+
Optimistic Versioning
+
State Machine
+
Database Constraints
```

---

# 14. Ledger

Payment state must be separated from financial accounting.

```text
Payment State
      +
Financial Ledger
      +
Settlement
```

Double-entry ledger:

```text
ledger_transaction
-----------------------
transaction_id
reference_id
currency
created_at
```

```text
ledger_entry
-----------------------
entry_id
transaction_id
account_id
debit
credit
currency
```

Invariant:

```text
SUM(debits) = SUM(credits)
```

The ledger should be:

```text
Append-only
Immutable
Auditable
```

Corrections use compensating entries rather than modifying historical entries.

---

# 15. Payment vs Ledger vs Settlement

These are different lifecycles.

```text
Payment Lifecycle
AUTHORIZED
CAPTURED
REFUNDED

Financial Lifecycle
Authorization
Capture
Fees
Refund
Chargeback

Settlement Lifecycle
Captured
   |
   v
Provider Settlement
   |
   v
Merchant Bank
```

Therefore:

```text
Payment State != Ledger State != Settlement State
```

---

# 16. Transactional Outbox

Payment state and the corresponding event are committed locally:

```text
BEGIN

UPDATE payment
SET status = AUTHORIZED

INSERT ledger transaction

INSERT outbox event

COMMIT
```

Then:

```text
Outbox
   |
   v
Kafka
```

This prevents:

```text
Payment DB = AUTHORIZED
Kafka Event = LOST
```

---

# 17. Payment Events

```text
PaymentCreated
PaymentAuthorized
PaymentCaptured
PaymentFailed
PaymentUnknown
PaymentRefunded
PaymentReconciled
ChargebackCreated
```

Event envelope:

```json
{
  "eventId": "EVT-123",
  "eventType": "PaymentAuthorized",
  "eventVersion": 1,
  "aggregateType": "Payment",
  "aggregateId": "PAY-123",
  "occurredAt": "2026-08-24T10:00:00Z",
  "producer": "payment-service",
  "traceId": "abc123",
  "correlationId": "corr123",
  "payload": {}
}
```

Kafka key:

```text
paymentId
```

This preserves per-payment ordering within a partition.

---

# 18. Webhook Architecture

```text
External PSP
     |
     v
Webhook Gateway
     |
     +-- Signature Validation
     +-- Authentication
     +-- Rate Limit
     |
     v
Webhook Processor
     |
     v
Payment Service
```

Provider events may include:

```text
payment.authorized
payment.captured
payment.failed
payment.refunded
chargeback.created
```

---

# 19. Webhook Idempotency

Store:

```text
provider
provider_event_id
processed_at
```

Constraint:

```text
UNIQUE(provider, provider_event_id)
```

Flow:

```text
Webhook
   |
   v
Already processed?
   |
 +--+--+
 |     |
YES   NO
 |     |
Ignore Process
```

Verify webhook signatures and validate the provider event before applying state changes.

---

# 20. Reconciliation

Reconciliation is a first-class subsystem.

Compare:

```text
Our Payment DB
       |
       +---- Provider API
       |
       +---- Provider Settlement File
       |
       +---- Ledger
       |
       +---- Bank / Settlement
```

Example:

```text
Our system:
PAY-123 = FAILED

Provider:
PAY-123 = CAPTURED
```

Reconciliation detects the discrepancy and either repairs it automatically or sends it to manual review.

---

# 21. Settlement

Settlement is separate from capture.

```text
Customer
   |
   v
PSP
   |
   v
CAPTURED
   |
   v
SETTLEMENT
   |
   v
Merchant Bank
```

Settlement tracks:

```text
Gross Amount
Provider Fees
Platform Fees
Refunds
Chargebacks
Reserve
Net Amount
Settlement Date
```

---

# 22. Risk / Fraud

```text
Payment
   |
   v
Risk Engine
   |
 +--+------+
 |         |
LOW       HIGH
 |         |
 v         v
PSP      Review / 3DS
```

Possible signals:

```text
Velocity
Device
IP Reputation
Amount
Merchant Risk
Historical Behavior
Geolocation
Payment Method
```

A payment requiring customer authentication enters:

```text
REQUIRES_ACTION
```

rather than being marked as failed.

---

# 23. Security

Prefer tokenized payment data:

```text
Customer
   |
   v
Provider Hosted UI / Tokenization
   |
   v
Payment Token
   |
   v
Our Platform
```

Avoid storing:

```text
Raw PAN
CVV
Sensitive card data
```

unless required and explicitly designed for the corresponding PCI scope.

External security:

```text
OAuth / JWT
WAF
Rate Limiting
Threat Protection
```

Internal security:

```text
mTLS
Workload Identity
Secrets Manager / Vault
Network Policies
RBAC
```

---

# 24. Multi-Region

```text
                       GLOBAL
                         |
          +--------------+--------------+
          |              |              |
          v              v              v
       Region A       Region B       Region C
          |              |              |
     Payment API    Payment API    Payment API
          |              |              |
       Local DB       Local DB       Local DB
          |              |              |
          +--------------+--------------+
                         |
                       Kafka
```

Do not blindly use active-active multi-writer for payment state.

Prefer:

```text
paymentId
    |
    v
Home Region
    |
    v
Single Authoritative Writer
```

Reads can be globally distributed where consistency permits.

---

# 25. Failure Matrix

| Failure | Handling |
|---|---|
| Invalid payment | Fail |
| Insufficient funds | Fail |
| Provider 4xx | Usually fail |
| Provider 5xx | Controlled retry |
| Provider timeout | UNKNOWN |
| Provider unavailable | UNKNOWN / safe retry |
| Duplicate request | Return existing result |
| Duplicate webhook | Ignore |
| Consumer crash | Redelivery + idempotency |
| DB failure | Fail safely / retry |
| Kafka failure | Outbox retry |
| Reconciliation mismatch | Auto-repair / manual review |
| Ambiguous provider result | Query/reconcile before failover |

---

# 26. Observability

Technical metrics:

```text
Payment API latency
Payment API error rate
Provider latency
Provider error rate
Database latency
Kafka lag
Webhook latency
Queue depth
```

Business metrics:

```text
Authorization success rate
Capture success rate
Refund success rate
Unknown payment count
Duplicate request count
Reconciliation mismatch count
Settlement mismatch
Chargeback rate
```

Provider metrics:

```text
Provider success rate
Provider p99
Provider availability
Provider error rate
```

---

# 27. Alternatives Considered

## Direct PSP Calls From Every Service

**Rejected.**

Creates provider coupling, duplicated logic, inconsistent idempotency and poor reconciliation.

---

## Timeout = Failure

**Rejected.**

A timeout has an ambiguous financial outcome.

```text
TIMEOUT != FAILED
```

---

## Blind Provider Failover

**Rejected.**

Could create a double charge.

---

## Distributed Two-Phase Commit

**Rejected.**

External PSPs cannot reliably participate in a global distributed transaction.

Use:

```text
Local ACID
+
Outbox
+
Idempotency
+
Workflow
+
Reconciliation
```

---

## Payment Table as Ledger

**Rejected.**

Payment lifecycle, accounting and settlement have different semantics.

---

## 100% Synchronous Processing

**Rejected as the universal model.**

3DS, webhooks, provider retries, refunds, settlement and reconciliation are naturally asynchronous.

---

# 28. Consequences

## Positive

- Strong protection against duplicate charges
- Explicit payment lifecycle
- Provider independence
- Controlled failover
- Strong auditability
- Reconciliation support
- Horizontal scalability
- Multi-region support
- Better PCI posture through tokenization
- Clear failure recovery

## Negative

- Higher architectural complexity
- Multiple persistence models
- Reconciliation infrastructure
- Provider-specific edge cases
- Eventual consistency between subsystems
- Complex multi-region payment ownership
- Ledger governance requirements

---

# 29. Critical Payment Invariants

```text
amount > 0

captured_amount <= authorized_amount

refunded_amount <= captured_amount

SUM(debits) = SUM(credits)

Only valid state transitions are allowed

Same logical request
    ->
Same business outcome
```

---

# 30. Interview-Ready Answer

> **"I would design the payment platform around correctness rather than simply throughput. The Payment Service owns an explicit state machine with states such as INITIATED, PROCESSING, AUTHORIZED, CAPTURED, FAILED, UNKNOWN, REFUND_PENDING and REFUNDED.**
>
> **Every mutating operation requires an idempotency key. The logical payment is separated from individual provider attempts, because one payment can have multiple provider attempts without creating multiple business transactions.**
>
> **Provider integrations are hidden behind a PaymentProvider interface, and a Provider Router chooses providers based on geography, currency, payment method, health, latency, cost and regulatory constraints. Provider failover is conservative: if a provider times out, I do not immediately charge through another provider because the original provider may already have succeeded. I first reconcile the ambiguous attempt.**
>
> **Payment state and financial accounting are separated. The payment service owns the lifecycle, while an immutable ledger records financial movements and settlement is handled separately. State changes and outbox events use local transactions, while Kafka distributes events asynchronously.**
>
> **Webhooks are authenticated and deduplicated, and reconciliation continuously compares our records with provider APIs and settlement files.**
>
> **For security, I prefer tokenization or provider-hosted payment collection rather than storing raw card data, reducing PCI scope. Externally I use OAuth/JWT and internally workload identity or mTLS.**
>
> **The most important principle is that a timeout is not a failure. UNKNOWN must be a first-class state because the biggest financial failure we need to prevent is charging the customer twice."**

---

# 31. Core Principle

> **A payment platform is successful when it can prove where every payment is, prevent duplicate financial effects, recover from ambiguous failures, reconcile itself with external systems, and produce an auditable financial history.**
