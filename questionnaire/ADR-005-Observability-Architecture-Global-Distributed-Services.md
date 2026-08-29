# ADR-005: Observability Architecture for Global Distributed Services

- **Status:** Accepted
- **Date:** 2026-08-23
- **Decision Owners:** Solution Architecture / Platform Engineering
- **Scope:** Global order-management platform
- **Related ADRs:**
  - ADR-001: Globally Scalable Order-Management Architecture
  - ADR-002: Global Authentication & Authorization
  - ADR-003: Polyglot Database Strategy
  - ADR-004: Order Transactions, Lifecycle & Failure Management

---

## 1. Context

The platform consists of globally distributed services such as:

- API Gateway
- Order Service
- Inventory Service
- Payment Service
- Fulfillment Service
- Customer Service
- Notification Service
- Kafka
- Databases
- Redis/Valkey
- Kubernetes
- External payment and logistics providers

A failure in one component can propagate through several layers.

Traditional infrastructure monitoring alone is insufficient because:

```text
Infrastructure healthy
        ↓
CPU normal
Memory normal
Pods healthy
        ↓
Business still failing
```

For example, the Order API may be healthy while payment latency causes orders to remain stuck in `PENDING_PAYMENT`.

The architecture therefore requires unified observability across:

1. Logs
2. Metrics
3. Distributed traces
4. Business metrics
5. SLOs
6. Alerting
7. Infrastructure
8. Dependencies
9. Deployment events
10. Security and audit data

---

# 2. Problem Statement

We need to answer:

- What happened?
- Where did it happen?
- Why did it happen?
- How many users/orders are affected?
- Is the problem regional or global?
- Did a recent deployment cause it?
- Is the problem in our system or a dependency?
- Can operators identify and recover the issue quickly?

The observability platform must also:

- Scale with the platform
- Support multi-region deployments
- Correlate synchronous and asynchronous operations
- Avoid becoming a production dependency
- Control telemetry cost
- Protect sensitive information
- Support incident investigation
- Provide actionable alerts

---

# 3. Decision

We will implement observability using the following architecture:

```text
                    OBSERVABILITY
                         │
       ┌─────────────────┼─────────────────┐
       ↓                 ↓                 ↓
     LOGS             METRICS           TRACES
       │                 │                 │
       └─────────────────┼─────────────────┘
                         ↓
                 OpenTelemetry
                         │
                         ↓
                OTel Collector Layer
                         │
          ┌──────────────┼──────────────┐
          ↓              ↓              ↓
      Log Store      Metrics Store    Trace Store
          │              │              │
          └──────────────┼──────────────┘
                         ↓
                      Grafana
                         │
               ┌─────────┴─────────┐
               ↓                   ↓
          Dashboards             Alerts
                                   │
                                   ↓
                              Incident System
```

OpenTelemetry will be the preferred application instrumentation standard because it provides a vendor-neutral telemetry model and allows observability backends to evolve independently of application instrumentation.

---

# 4. Four Observability Signals

## 4.1 Logs

Answer:

> **What happened?**

Examples:

```text
PAYMENT_AUTHORIZATION_FAILED
INVENTORY_RESERVATION_FAILED
ORDER_STATE_TRANSITION
DATABASE_CONNECTION_TIMEOUT
```

---

## 4.2 Metrics

Answer:

> **Is the system healthy?**

Examples:

```text
Request rate
Error rate
p95/p99 latency
CPU
Memory
Database connections
Kafka consumer lag
```

---

## 4.3 Traces

Answer:

> **Where did the request spend time and where did it fail?**

Example:

```text
API Gateway
    ↓
Order Service
    ↓
Kafka
    ↓
Inventory Service
    ↓
Payment Service
    ↓
Payment Provider
```

---

## 4.4 Business Metrics

Answer:

> **Is the business transaction healthy?**

Examples:

```text
Orders created/min
Orders completed/min
Payment success rate
Payment failure rate
Inventory reservation failure rate
Orders stuck in PENDING_PAYMENT
Orders stuck in PENDING_INVENTORY
Refund failure rate
```

Business metrics are considered first-class observability signals.

---

# 5. OpenTelemetry

Application services will use OpenTelemetry instrumentation.

Conceptually:

```text
Application
     │
     ↓
OpenTelemetry SDK
     │
     ├── Logs
     ├── Metrics
     └── Traces
     │
     ↓
OpenTelemetry Collector
```

The Collector will provide:

- Batching
- Filtering
- Enrichment
- Sampling
- Transformation
- Routing
- Retry
- Export

This avoids coupling every service directly to a specific observability backend.

---

# 6. OpenTelemetry Collector Architecture

Telemetry should preferably flow through a Collector layer:

```text
Service
   │
   ↓
OTel SDK
   │
   ↓
Regional Collector
   │
   ├── Filter
   ├── Enrich
   ├── Batch
   ├── Sample
   └── Route
   │
   ├────────→ Logs
   ├────────→ Metrics
   └────────→ Traces
```

Collectors should be deployed regionally so observability does not depend on cross-region network connectivity.

---

# 7. Structured Logging

All application logs will use structured JSON.

### Bad

```text
Payment failed for order 123
```

### Good

```json
{
  "timestamp": "2026-08-23T10:20:31.123Z",
  "level": "ERROR",
  "service": "payment-service",
  "version": "2.4.1",
  "environment": "prod",
  "region": "ap-south-1",
  "traceId": "abc123",
  "spanId": "xyz789",
  "correlationId": "corr-456",
  "orderId": "ORD-123",
  "paymentId": "PAY-456",
  "event": "PAYMENT_AUTHORIZATION_FAILED",
  "errorCode": "PROVIDER_TIMEOUT",
  "durationMs": 1200
}
```

---

# 8. Standard Log Fields

## Infrastructure

```text
timestamp
host
pod
container
namespace
cluster
region
availabilityZone
```

## Application

```text
service
version
environment
thread
logLevel
```

## Distributed Tracing

```text
traceId
spanId
parentSpanId
```

## Business Context

```text
correlationId
orderId
customerId
paymentId
workflowId
eventId
```

---

# 9. Sensitive Data Protection

Logs must never contain sensitive credentials or payment secrets.

Do not log:

```text
Passwords
Access tokens
Refresh tokens
API keys
Secrets
CVV
Full payment card numbers
Sensitive PII
```

Apply:

```text
PII masking
Secret redaction
Data classification
Access control
Retention policies
Encryption
Audit logging
```

Example:

```text
cardNumber = **** **** **** 1234
```

where logging the last four digits is permitted by the applicable security policy.

---

# 10. Log Levels

Standardize:

```text
TRACE
DEBUG
INFO
WARN
ERROR
```

Production logging should avoid excessive DEBUG/TRACE volume.

Examples:

```text
INFO
Order created

WARN
Payment provider response delayed

ERROR
Payment authorization failed
```

---

# 11. Log Pipeline

```text
Application
     │
     ↓
stdout / OTel
     │
     ↓
OTel Collector / Fluent Bit
     │
     ↓
Regional Buffer
     │
     ↓
Central Log Store
     │
     ↓
Search / Dashboards
```

Telemetry collection must be asynchronous and non-blocking.

---

# 12. Metrics Strategy

Metrics will be divided into:

1. RED metrics
2. USE metrics
3. JVM/application metrics
4. Dependency metrics
5. Business metrics

---

# 13. RED Metrics

For every customer-facing API:

```text
Rate
Errors
Duration
```

Example:

```text
Order API

Request rate
Error %
p50 latency
p95 latency
p99 latency
```

---

# 14. USE Metrics

For infrastructure:

```text
Utilization
Saturation
Errors
```

Examples:

```text
CPU
Memory
Disk
Network
Connection pools
Thread pools
Executor queues
```

---

# 15. JVM Metrics

For Java/Spring Boot services:

```text
Heap
Non-heap
GC
Threads
CPU
Class loading
Memory pools
JIT
Allocation rate
```

Important GC metrics:

```text
GC pause time
GC frequency
Old-gen utilization
Heap utilization
Allocation rate
```

A production latency investigation should correlate:

```text
API p99
+
GC pause
+
Thread pool saturation
+
Database latency
+
Downstream latency
```

---

# 16. Thread Pool Metrics

Monitor:

```text
Active threads
Queue depth
Maximum threads
Rejected tasks
Task execution time
```

Example failure chain:

```text
Request rate ↑
     ↓
Executor queue ↑
     ↓
Thread pool saturated
     ↓
API latency ↑
     ↓
Timeouts
     ↓
Retries
```

---

# 17. Database Observability

Every database should expose:

```text
CPU
Memory
Storage
IOPS
Connections
Connection pool usage
Query latency
Slow queries
Locks
Deadlocks
Replication lag
Transaction rate
Error rate
Cache hit ratio
```

For PostgreSQL additionally monitor:

```text
Active connections
Waiting connections
Locks
Deadlocks
Vacuum
Bloat
Slow queries
Replication lag
```

---

# 18. HikariCP Monitoring

For Spring Boot:

```text
Spring Boot
     ↓
HikariCP
     ↓
Database
```

Monitor:

```text
Active connections
Idle connections
Pending threads
Maximum pool size
Connection acquisition time
Connection timeout
```

Important failure chain:

```text
Database slows
     ↓
Connections held longer
     ↓
Hikari pool exhaustion
     ↓
Requests wait
     ↓
API latency ↑
     ↓
Timeouts
     ↓
Retries
     ↓
Database load ↑
```

---

# 19. Kafka Observability

Kafka is a critical distributed component.

## Broker metrics

```text
CPU
Memory
Disk
Network
Request latency
ISR health
Under-replicated partitions
```

## Topic metrics

```text
Message rate
Bytes/sec
Partition distribution
Retention
```

## Consumer metrics

Most importantly:

```text
Consumer lag
```

Example:

```text
Producer:
50K events/sec

Consumer:
45K events/sec

Lag:
continuously increasing
```

This indicates that consumers are falling behind.

---

# 20. Consumer Lag Alerting

Do not rely only on a static lag threshold.

Use:

```text
Absolute lag
+
Lag growth rate
+
Processing latency
+
Business impact
```

Example:

```text
Lag:
10K → 20K → 50K → 100K
```

is more concerning than a temporary 10K backlog that is recovering.

---

# 21. Kubernetes Observability

Monitor:

```text
Cluster health
Node health
Pod health
Deployment health
CPU
Memory
Restarts
OOMKilled
Pending pods
Scheduling failures
HPA
Network
Ingress
```

Critical signals include:

```text
CrashLoopBackOff
OOMKilled
Pod restart rate
Deployment failures
Unschedulable pods
```

---

# 22. Distributed Tracing

A trace should follow a business request through the distributed system.

Example:

```text
POST /orders
     │
     ↓
API Gateway
     │
     ↓
Order Service
     │
     ↓
Kafka
     │
     ↓
Inventory Service
     │
     ↓
Payment Service
     │
     ↓
Payment Provider
     │
     ↓
Fulfillment
```

Example trace:

```text
API Gateway          5ms
 └─ Order Service   20ms
     └─ Kafka        3ms
         └─ Inventory 15ms
         └─ Payment   800ms
             └─ Provider 780ms
```

This immediately exposes the payment provider as the latency bottleneck.

---

# 23. Trace Context Propagation

Trace context must propagate through:

### HTTP

```text
traceparent
```

### Kafka

```text
Kafka message headers
```

Conceptually:

```text
Order Service
   │
   │ traceId = abc123
   ↓
Kafka
   │
   │ traceId = abc123
   ↓
Inventory Service
```

This allows synchronous and asynchronous operations to be correlated.

---

# 24. Trace ID vs Correlation ID vs Business ID

These serve different purposes.

### Trace ID

Technical distributed tracing.

```text
traceId = abc123
```

### Correlation ID

Request/business-operation correlation.

```text
correlationId = corr-456
```

### Business ID

Domain identifier.

```text
orderId = ORD-123
```

Where appropriate, all should be propagated.

---

# 25. Trace Sampling

At very high traffic volumes, 100% tracing can become prohibitively expensive.

Use sampling.

Example:

```text
Normal successful request → 1%
Error → 100%
Slow request → 100%
Payment failure → 100%
```

Tail sampling is preferred where the platform supports it because the decision can be made after observing the complete trace.

---

# 26. Business Observability

The platform must expose business-health metrics.

Example:

```text
Orders created/min
Orders completed/min
Orders cancelled/min
Payment success rate
Payment failure rate
Inventory reservation failure rate
Average order processing time
Orders stuck in PENDING_PAYMENT
Orders stuck in PENDING_INVENTORY
Refund failure rate
```

Business metrics are often more important than CPU and memory.

---

# 27. SLOs

Define SLOs around customer-visible behavior.

Example:

### Order API

```text
Availability: 99.99%

Latency:
99% < 200ms

Error rate:
< 0.1%
```

### Order Processing

```text
99% of successful orders
complete within defined business SLA
```

The actual values must be determined from business requirements.

---

# 28. Error Budget

For a 99.99% monthly availability SLO:

```text
Error budget ≈ 4.32 minutes/month
```

If the platform is consuming its error budget too quickly:

```text
Reduce risky deployments
        +
Prioritize reliability work
```

This connects observability to engineering decision-making.

---

# 29. Alerting Philosophy

Alerts should be actionable.

### Weak alert

```text
CPU > 80%
```

### Better

```text
Order API p99 > SLO
AND
error rate elevated
```

### Business-oriented

```text
Order completion rate ↓
+
Payment failure rate ↑
+
Payment latency ↑
```

The last alert is directly connected to customer impact.

---

# 30. Alert Severity

```text
P0 — Critical
Global outage / severe business impact

P1 — High
Major regional/service degradation

P2 — Medium
Service degradation without major customer impact

P3 — Low
Operational issue
```

Avoid alerting on every abnormal metric.

---

# 31. Dashboards

## Business Dashboard

```text
Orders/min
Revenue
Payment success rate
Order completion rate
Cancellation rate
```

## Platform Dashboard

```text
Availability
Latency
Error rate
Kafka
Databases
Kubernetes
```

## Service Dashboard

```text
Request rate
Errors
Latency
Dependencies
JVM
Thread pools
```

## Incident Dashboard

```text
Active alerts
Traces
Logs
Recent deployments
Dependency health
Business impact
```

---

# 32. Deployment Observability

Every deployment should attach:

```text
service
version
commit SHA
environment
region
deployment timestamp
```

Then dashboards can correlate:

```text
Deployment v2.7.1
        │
        ↓
p99 latency ↑
        │
        ↓
error rate ↑
```

This accelerates rollback decisions.

---

# 33. Canary Observability

For critical services:

```text
v2.6 → 95%
v2.7 → 5%
```

Compare:

```text
Error rate
p99 latency
Business metrics
CPU
Memory
Kafka lag
Database latency
```

If the canary performs worse:

```text
Rollback
```

---

# 34. Multi-Region Observability

```text
                         GLOBAL
                           │
             ┌─────────────┼─────────────┐
             ↓             ↓             ↓
          Region A       Region B      Region C
             │             │             │
          Metrics       Metrics       Metrics
          Logs          Logs          Logs
          Traces        Traces        Traces
             │             │             │
             └─────────────┼─────────────┘
                           ↓
                 Central Observability
```

However, raw telemetry should preferably be collected regionally first.

Benefits:

- Lower cross-region traffic
- Better resilience
- Lower cost
- Regional failure isolation

---

# 35. Observability Must Not Be a Single Point of Failure

If Grafana or the central log platform is unavailable:

```text
Order Service
     ↓
MUST CONTINUE OPERATING
```

Telemetry should therefore be:

```text
Asynchronous
Non-blocking
Buffered
Best-effort
```

Application transactions must never synchronously depend on the logging backend.

---

# 36. Security

Observability data can contain sensitive information.

Implement:

```text
RBAC
Encryption
Tenant isolation
PII masking
Secret redaction
Retention policies
Audit logs
```

Only authorized personnel should access sensitive production telemetry.

---

# 37. Cloud Implementation Options

The architecture remains vendor-neutral, but cloud-native implementations can use:

## AWS

```text
CloudWatch
OpenSearch
AWS X-Ray / OpenTelemetry
Amazon Managed Service for Prometheus
Amazon Managed Grafana
```

## Azure

```text
Azure Monitor
Application Insights
Log Analytics
Azure Monitor managed Prometheus
Azure Managed Grafana
```

## GCP

```text
Cloud Logging
Cloud Monitoring
Cloud Trace
Managed Service for Prometheus
Grafana-compatible tooling
```

Application instrumentation should remain as cloud-neutral as practical through OpenTelemetry.

---

# 38. End-to-End Incident Example

Suppose:

> Customers report that orders are taking two minutes.

Investigation:

### Step 1 — Business metrics

```text
Order completion latency ↑
```

### Step 2 — Service metrics

```text
Order API p99 → 2 sec
```

### Step 3 — Trace

```text
Order
  ↓
Inventory
  ↓
Payment
       ↑
    1.8 sec
```

### Step 4 — Payment metrics

```text
Payment provider latency ↑
```

### Step 5 — Logs

```text
PROVIDER_TIMEOUT
```

### Step 6 — Dependency

Payment provider is degraded.

Result:

```text
Business symptom
      ↓
Metrics
      ↓
Trace
      ↓
Logs
      ↓
Root cause
```

This is the intended observability workflow.

---

# 39. Failure Scenarios Observability Must Detect

| Failure | Primary Signals |
|---|---|
| API latency spike | p95/p99 + traces |
| Service errors | Error rate + logs |
| Kafka backlog | Consumer lag |
| Database saturation | Connections + latency + locks |
| Connection pool exhaustion | Hikari active/pending |
| JVM GC problem | GC pause + heap |
| Thread pool saturation | Queue + active threads |
| Pod crash | Restart/OOMKilled |
| Payment provider degradation | External dependency latency/error |
| Order workflow stuck | Business state age |
| Region failure | Regional SLO + traffic |
| Deployment regression | Version vs metrics |
| Event processing failure | Consumer errors + DLQ |

---

# 40. Alternatives Considered

## ELK/EFK as the Complete Observability Strategy

**Rejected as the complete architecture.**

Logging alone cannot provide:

- Distributed tracing
- Service-level metrics
- Business SLOs
- End-to-end latency analysis

It can remain part of the logging implementation.

---

## Vendor-Specific Instrumentation Everywhere

**Rejected.**

Reasons:

- Vendor lock-in
- Inconsistent instrumentation
- Difficult backend migration
- Different telemetry semantics between services

OpenTelemetry provides a more portable instrumentation layer.

---

## 100% Trace and Log Retention

**Rejected.**

Reasons:

- High storage cost
- High network volume
- High query cost
- Limited operational value for normal requests

Use sampling and retention policies.

---

## Infrastructure-Only Monitoring

**Rejected.**

CPU and memory can remain healthy while business workflows fail.

Business metrics are mandatory.

---

# 41. Consequences

## Positive

- End-to-end visibility
- Faster incident diagnosis
- Correlation across services
- Business-aware monitoring
- Better deployment safety
- Multi-region visibility
- Reduced vendor lock-in
- Proactive SLO management

## Negative

- Additional infrastructure
- Telemetry storage cost
- Instrumentation effort
- Sampling complexity
- Dashboard/alert maintenance
- Sensitive-data governance requirements

---

# 42. Final Architecture

```text
                         USERS
                           │
                           ↓
                    CDN / WAF / LB
                           │
                           ↓
                     API Gateway
                           │
                           ↓
                    Microservices
                           │
                    OpenTelemetry
                           │
              ┌────────────┼────────────┐
              ↓            ↓            ↓
            Logs        Metrics       Traces
              │            │            │
              └────────────┼────────────┘
                           ↓
                  Regional Collectors
                           │
              ┌────────────┼────────────┐
              ↓            ↓            ↓
          Log Store    Metrics Store   Trace Store
              │            │            │
              └────────────┼────────────┘
                           ↓
                        Grafana
                           │
              ┌────────────┼────────────┐
              ↓            ↓            ↓
          Dashboards     SLOs         Alerts
                                        │
                                        ↓
                                  Incident System
```

---

# 43. Interview-Ready Answer

> **"I would design observability around logs, metrics and distributed traces, with OpenTelemetry as the common instrumentation layer. Every request would carry a trace ID and correlation ID, while business identifiers such as orderId and paymentId would propagate across HTTP and Kafka boundaries.**
>
> **Logs would be structured JSON with standardized fields, and sensitive data such as credentials, tokens and payment information would be masked. Metrics would include RED and USE metrics, JVM, Kubernetes, database and Kafka metrics, but also business metrics such as order completion rate, payment failures and orders stuck in intermediate states.**
>
> **For distributed tracing, I would trace the complete path across the API gateway, services, Kafka, databases and external providers. An OpenTelemetry Collector would handle batching, enrichment, filtering and sampling. At high scale I would use tail sampling to retain errors and slow traces while controlling telemetry cost.**
>
> **For Kafka I would monitor broker health, under-replicated partitions and especially consumer lag. For databases I would monitor query latency, connections, locks, deadlocks, replication lag and connection-pool saturation. For Kubernetes and Java services I would monitor pod health, restarts, GC, heap, thread pools and executor queues.**
>
> **Finally, I would define SLOs around customer-visible behavior rather than arbitrary infrastructure thresholds. Alerts should be actionable and tied to business impact. Observability would be collected regionally so that the observability platform itself doesn't become a cross-region dependency or a single point of failure."**

---

# 44. Key Principle

> **"I don't monitor servers; I monitor the health of the business transaction across the entire distributed system."**
