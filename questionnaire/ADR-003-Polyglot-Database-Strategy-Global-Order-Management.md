# ADR-003: Polyglot Database Strategy for a Global Order-Management Platform

- **Status:** Accepted
- **Date:** 2026-08-23
- **Decision Owners:** Solution Architecture / Engineering
- **Scope:** Global order-management platform
- **Related ADRs:** ADR-001 Globally Scalable Order-Management Architecture

---

## 1. Context

The global order-management platform is expected to support approximately 100M+ users, 10M orders/day, high peak traffic, multiple geographic regions, and different workloads including transactional order processing, inventory, catalog, customer data, caching, and analytics.

A single database technology should not be forced onto every workload.

The architecture therefore needs a database strategy that evaluates:

- Relational vs NoSQL requirements
- ACID transactions
- Consistency
- Global distribution
- Latency
- Horizontal scalability
- Data ownership
- Query patterns
- Availability
- Operational complexity
- Cost
- Cloud portability

The major cloud platforms provide different managed database choices. AWS provides services including Aurora, RDS, DynamoDB, ElastiCache, Keyspaces, Neptune, Timestream and DocumentDB. AWS specifically positions Aurora for relational workloads such as inventory and order processing. citeturn0search15turn0search16

Azure provides managed relational and distributed database options including Azure Database for PostgreSQL, Azure SQL and Cosmos DB. Azure's current guidance states that **Azure Cosmos DB for PostgreSQL is on a retirement path and is not recommended for new projects**; new distributed PostgreSQL workloads should use Azure Database for PostgreSQL Elastic Clusters, while NoSQL workloads should use Azure Cosmos DB for NoSQL. citeturn0search3turn0search14

Google Cloud provides Cloud SQL, AlloyDB, Spanner, Bigtable, Firestore and Memorystore. Spanner is positioned for globally consistent, mission-critical relational workloads; Bigtable for high-throughput, low-latency wide-column workloads; AlloyDB for demanding PostgreSQL-compatible workloads; and Firestore for scalable document workloads. citeturn0search4turn0search11

---

# 2. Problem Statement

We need a database architecture that can support different workload characteristics without introducing unnecessary complexity.

The system must support:

1. Strong transactional guarantees for critical order and payment workflows.
2. High-throughput inventory operations.
3. Global low-latency reads.
4. Horizontal scaling for high-volume workloads.
5. Flexible data models where appropriate.
6. Regional resilience.
7. Independent service-level data ownership.
8. Efficient caching.
9. Future cloud migration flexibility where practical.

---

# 3. Decision

We will adopt a **polyglot persistence strategy**.

Each service will select its database technology based on its business requirements rather than using a single database platform for the entire system.

The default principle is:

> **Use relational databases for transactional business state, NoSQL for workloads where flexible schema and horizontal scale are primary requirements, and in-memory stores for latency-sensitive ephemeral/read-heavy data.**

We will avoid selecting a database solely because it is the organization's preferred cloud product.

---

# 4. Database Decision Framework

Database selection will follow this decision process:

```text
                    WORKLOAD
                       │
          ┌────────────┼────────────┐
          ↓            ↓            ↓
      Relational      NoSQL        Cache
          │            │            │
          ↓            ↓            ↓
       ACID / joins  Access       In-memory
       transactions  pattern      data
          │            │            │
     ┌────┴────┐   ┌───┼────┐      ↓
     ↓         ↓   ↓   ↓    ↓    Redis/
 Regional    Global KV  Doc Wide  Valkey
     │         │
 Managed    Distributed
 SQL        SQL
```

The primary questions are:

1. What consistency model is required?
2. Are multi-row/multi-entity transactions required?
3. What are the dominant access patterns?
4. Is the workload read-heavy or write-heavy?
5. Does it require global distribution?
6. What is the expected throughput?
7. What is the latency requirement?
8. What is the partition/sharding strategy?
9. What are the recovery requirements?
10. What operational skills are available?

---

# 5. Cloud Database Options

## 5.1 AWS

| Workload | AWS Option | Typical Use |
|---|---|---|
| General relational | Amazon RDS | Managed PostgreSQL/MySQL/SQL Server/Oracle/etc. |
| High-performance PostgreSQL/MySQL | Amazon Aurora | Transactional applications |
| Key-value / document | DynamoDB | Massive-scale predictable access |
| Document | DocumentDB | MongoDB-compatible workloads |
| Wide-column | Keyspaces | Cassandra-compatible workloads |
| Graph | Neptune | Graph relationships |
| Cache | ElastiCache | Redis/Valkey-style caching |
| Durable in-memory | MemoryDB | Low-latency data structures |
| Time-series | Timestream | Time-series workloads |

AWS documentation describes RDS as managed relational database infrastructure and Aurora as a cloud-built managed relational engine compatible with PostgreSQL and MySQL. citeturn0search1

---

## 5.2 Azure

| Workload | Azure Option | Typical Use |
|---|---|---|
| SQL Server | Azure SQL Database | Enterprise SQL workloads |
| PostgreSQL | Azure Database for PostgreSQL | PostgreSQL applications |
| Distributed PostgreSQL | PostgreSQL Elastic Clusters | Horizontally scaled PostgreSQL |
| Distributed NoSQL | Azure Cosmos DB for NoSQL | Global NoSQL workloads |
| Cache | Azure Managed Redis | In-memory workloads |
| Cassandra-style workloads | Managed Cassandra options | Wide-column workloads |

**Important:** Azure Cosmos DB for PostgreSQL should not be selected for a new project because Microsoft currently lists it as being on a retirement path. For new distributed PostgreSQL workloads, use Azure Database for PostgreSQL Elastic Clusters. citeturn0search3turn0search14

---

## 5.3 GCP

| Workload | GCP Option | Typical Use |
|---|---|---|
| General relational | Cloud SQL | Managed MySQL/PostgreSQL/SQL Server |
| High-performance PostgreSQL | AlloyDB | Demanding PostgreSQL workloads |
| Global distributed SQL | Spanner | Global transactional systems |
| Wide-column | Bigtable | Massive low-latency workloads |
| Document | Firestore | Mobile/web/document workloads |
| Cache | Memorystore | Redis/Valkey/in-memory workloads |

Google Cloud currently lists Cloud SQL, AlloyDB, Spanner, Bigtable, Firestore and Memorystore as core managed database services. citeturn0search4turn0search5

---

# 6. Service-Level Database Selection

## 6.1 Order Service

### Decision

Use a relational database.

Candidates:

```text
AWS:
  Aurora PostgreSQL

Azure:
  Azure Database for PostgreSQL

GCP:
  AlloyDB / Cloud SQL
```

### Rationale

Orders require:

- ACID transactions
- Strong business consistency
- Relationships
- State transitions
- Query flexibility
- Transactional integrity

AWS itself identifies Aurora as a suitable option for inventory and order-processing workloads where relational data models and transaction support are required. citeturn0search16

---

# 7. Inventory Service

Inventory is a special case.

### Requirements

- Very high write concurrency
- Atomic reservation
- Strong correctness
- Avoid overselling
- Regional ownership
- Potentially global inventory visibility

Possible options:

```text
AWS:
  Aurora / DynamoDB depending access pattern

Azure:
  Azure Database for PostgreSQL /
  Cosmos DB where the consistency and data model fit

GCP:
  Spanner for globally distributed transactional requirements
```

### Decision Principle

If inventory is primarily regional:

```text
Regional relational database
+
Atomic reservation
+
Partitioning
```

If inventory requires globally distributed transactional writes:

```text
Evaluate distributed SQL
such as Spanner or YugabyteDB
```

The choice must be driven by the actual inventory consistency and latency requirements.

---

# 8. Product Catalog

### Characteristics

- Read-heavy
- Large dataset
- Frequently accessed
- Less transactionally sensitive
- Global distribution desirable

Possible options:

```text
AWS      → DynamoDB / Aurora
Azure    → Cosmos DB
GCP      → Firestore / Bigtable depending access pattern
```

Add:

```text
CDN
+
Redis/Valkey
+
Regional replicas
```

for low-latency reads.

---

# 9. Shopping Cart

Shopping carts generally fit a key-value/document model.

Possible options:

```text
AWS      → DynamoDB
Azure    → Cosmos DB
GCP      → Firestore
```

Typical key:

```text
customerId / cartId
```

This provides efficient retrieval without requiring complex relational joins.

---

# 10. Customer Profile

The customer profile can use either relational or document storage depending on requirements.

Use relational when:

- Strong relationships are required.
- Complex queries are common.
- ACID transactions matter.

Use document/NoSQL when:

- Schema flexibility matters.
- Access is primarily by customer ID.
- Horizontal scale dominates.

---

# 11. Payment

Payment data is highly sensitive and transactionally important.

Default:

```text
Relational database
+
Strong consistency
+
Idempotency
+
Audit trail
```

The database should not be selected only for scale.

Financial correctness, compliance, auditability and transaction semantics are higher priorities.

---

# 12. Cache

Use a managed in-memory database/cache for:

- Product data
- Pricing
- Session information
- Frequently accessed order summaries
- Rate limiting
- Short-lived authorization data where appropriate

Cloud mapping:

```text
AWS      → ElastiCache / MemoryDB
Azure    → Azure Managed Redis
GCP      → Memorystore
```

The cache is not the source of truth for critical transactional state.

---

# 13. YugabyteDB

YugabyteDB should be considered when we need:

- PostgreSQL compatibility
- Distributed SQL
- Horizontal scale
- Multi-region deployment
- Strong consistency requirements
- Distributed transactional semantics

Potential use cases:

```text
Global orders
Global inventory
Multi-region transactional workloads
```

However, introducing YugabyteDB also introduces:

- Distributed database operational complexity
- Data distribution decisions
- Latency considerations
- Partitioning concerns
- More complex failure scenarios

Therefore:

> **YugabyteDB should be selected because the workload requires distributed SQL capabilities, not simply because the system is large.**

---

# 14. Spanner

Spanner becomes particularly attractive when the requirement is:

```text
Global scale
+
Relational model
+
Strong consistency
+
Multi-region
+
High availability
```

For example:

```text
Global Inventory
Global Financial Ledger
Global Order State
```

Spanner is explicitly positioned by Google for mission-critical, global-scale, globally consistent workloads. citeturn0search11

The trade-off is that the application and organization become more closely aligned with GCP's distributed SQL model.

---

# 15. DynamoDB

DynamoDB is preferred when:

```text
Massive scale
+
Predictable access patterns
+
Key-value/document model
+
Low latency
```

Examples:

```text
Shopping cart
Session
User preferences
Order lookup projections
Idempotency records
```

It is not the default choice for complex relational order workflows requiring joins and rich transactional queries.

---

# 16. Cosmos DB

Cosmos DB is attractive when:

```text
Global distribution
+
NoSQL
+
Low latency
+
Flexible schema
+
Multi-region requirements
```

Potential use cases:

- Catalog
- Customer preferences
- Shopping cart
- Global application metadata

However, consistency and partition-key design must be explicitly defined.

---

# 17. Database-per-Service

Each service should own its data.

```text
Order Service
      │
      ↓
   Order DB

Inventory Service
      │
      ↓
 Inventory DB

Payment Service
      │
      ↓
 Payment DB
```

We will avoid:

```text
                 Shared DB
                    │
        ┌───────────┼───────────┐
        ↓           ↓           ↓
      Order      Inventory    Payment
```

because this creates:

- Tight coupling
- Shared schema ownership
- Deployment dependencies
- Scaling constraints
- Distributed-monolith characteristics

---

# 18. Data Access Pattern

The database schema should be designed from access patterns.

Example:

```text
GET /orders/{orderId}
```

should have a direct lookup path.

Avoid designs requiring:

```text
Order
 ↓
Customer
 ↓
Payment
 ↓
Inventory
 ↓
Shipment
```

on every request.

Instead, maintain appropriate read models or projections.

---

# 19. Read Scaling

For read-heavy workloads:

```text
                 Application
                     │
                     ↓
                   Cache
                  /                  HIT /       \ MISS
                ↓         ↓
              Return     Database
                           │
                           ↓
                      Read Replica
```

Use:

- Caching
- Read replicas
- Database indexes
- Query optimization
- Materialized/read models
- CDN for suitable data

---

# 20. Write Scaling

For write-heavy workloads:

```text
Application
    │
    ↓
Partition / Shard Key
    │
 ┌──┼──┬──┐
 ↓  ↓  ↓  ↓
P1 P2 P3 P4
```

Partition keys must avoid hotspots.

Bad:

```text
partitionKey = country
```

if one country receives most traffic.

Better:

```text
partitionKey = customerId
```

or another high-cardinality domain key, depending on the workload.

---

# 21. Global Data Strategy

The architecture will not force every database into global active-active mode.

Instead:

```text
              GLOBAL SYSTEM
                    │
        ┌───────────┼───────────┐
        ↓           ↓           ↓
      Region A    Region B    Region C
        │           │           │
      Local       Local       Local
      Data        Data        Data
        │           │           │
        └──────── Replication ─┘
```

The strategy will be selected per domain.

Example:

| Domain | Global Strategy |
|---|---|
| Catalog | Replicated / eventually consistent |
| Customer preferences | Multi-region NoSQL |
| Orders | Regional ownership + replication |
| Inventory | Region/warehouse ownership or distributed SQL |
| Payment | Strong transactional model |
| Analytics | Event-driven aggregation |

---

# 22. Consistency Strategy

Not every piece of data requires strong consistency.

| Data | Consistency |
|---|---|
| Payment transaction | Strong |
| Inventory reservation | Strong |
| Order state | Strong business correctness |
| Customer profile | Usually strong within owner region |
| Product catalog | Eventual acceptable |
| Recommendations | Eventual |
| Notifications | Eventual |
| Analytics | Eventual |

This avoids paying the cost of global strong consistency where it is unnecessary.

---

# 23. CAP / PACELC Consideration

Database decisions must explicitly consider:

- Consistency
- Availability
- Partition tolerance
- Latency
- Replication cost

The architecture should not simply claim:

> "We need consistency and availability."

Instead, identify the business requirement.

For example:

```text
Payment
→ Correctness > availability

Catalog
→ Availability + latency > immediate global consistency

Inventory
→ Correctness > unconstrained availability
```

---

# 24. Backup and Disaster Recovery

Every database must have:

- Automated backups
- Point-in-time recovery where supported
- Replication strategy
- Disaster recovery plan
- Restore testing
- Encryption
- Access controls
- Monitoring

RPO/RTO should be defined by business criticality.

---

# 25. Observability

Database observability should include:

```text
Latency
Throughput
Connections
Connection pool utilization
CPU
Memory
Disk / storage
IOPS
Locks
Deadlocks
Replication lag
Query latency
Slow queries
Cache hit ratio
Partition hotspots
```

---

# 26. Connection Pooling

Application-level connection pooling is required.

For Java/Spring Boot:

```text
Spring Boot
    │
    ↓
HikariCP
    │
    ↓
Database
```

Connection pool size must be calculated from:

- Database capacity
- Application instance count
- Query latency
- CPU
- Concurrency

More connections do not automatically mean more throughput.

---

# 27. Alternatives Considered

## Option 1 — One Database Everywhere

**Rejected.**

Problems:

- Different workloads require different data models.
- Scaling becomes coupled.
- One database becomes a bottleneck.
- Global distribution becomes difficult.

---

## Option 2 — PostgreSQL Everywhere

**Not selected as the universal strategy.**

PostgreSQL remains the default relational choice where appropriate, but it is not necessarily optimal for:

- Massive key-value workloads
- Globally distributed NoSQL workloads
- Huge wide-column workloads
- In-memory workloads

---

## Option 3 — NoSQL Everywhere

**Rejected.**

NoSQL does not automatically replace:

- ACID transactions
- Complex joins
- Relational constraints
- Strong transactional workflows

---

## Option 4 — One Globally Distributed Database

**Rejected as a blanket architecture.**

Different domains have different:

- Consistency
- Latency
- Query
- Availability
- Scaling requirements

---

# 28. Cloud Portability

Where practical, applications should minimize unnecessary coupling to provider-specific database APIs.

For relational workloads:

```text
PostgreSQL
```

can provide portability across:

```text
AWS Aurora PostgreSQL
Azure PostgreSQL
GCP AlloyDB / Cloud SQL
YugabyteDB
```

However, cloud-native services such as:

```text
DynamoDB
Cosmos DB
Spanner
```

provide capabilities that may justify deliberate vendor coupling.

The decision should be based on business value rather than portability as an absolute goal.

---

# 29. Final Database Mapping

For the global order-management system:

| Domain | Preferred Pattern | AWS | Azure | GCP |
|---|---|---|---|---|
| Order | Relational | Aurora PostgreSQL | Azure PostgreSQL | AlloyDB / Cloud SQL |
| Inventory | Distributed/relational | Aurora / DynamoDB | PostgreSQL / Cosmos DB | Spanner / AlloyDB |
| Payment | Relational | Aurora / RDS | Azure SQL / PostgreSQL | AlloyDB / Cloud SQL |
| Catalog | NoSQL/read-heavy | DynamoDB | Cosmos DB | Firestore / Bigtable |
| Customer | Relational/NoSQL | Aurora / DynamoDB | PostgreSQL / Cosmos DB | Cloud SQL / Firestore |
| Cart | Key-value/document | DynamoDB | Cosmos DB | Firestore |
| Cache | In-memory | ElastiCache / MemoryDB | Managed Redis | Memorystore |
| Analytics | Warehouse | Redshift | Fabric / Synapse | BigQuery |

This table is a **starting point**, not a fixed technology mandate.

---

# 30. Decision Summary

The architecture adopts:

```text
              POLYGLOT PERSISTENCE
                       │
       ┌───────────────┼────────────────┐
       ↓               ↓                ↓
   Relational         NoSQL            Cache
       │               │                │
       ↓               ↓                ↓
 Orders/Payment    Catalog/Cart      Redis/Valkey
 Inventory          Profiles
       │
       ↓
Distributed SQL
where global transactional
requirements justify it
```

The primary decision principle is:

> **Choose the database based on business invariants, access patterns, consistency, scale and geographic requirements—not based on which database is most familiar or fashionable.**

---

# 31. Interview-Ready Answer

If asked:

> **"How would you choose a database for a global order-management system?"**

Answer:

> "I would use polyglot persistence rather than one database for everything. For transactional domains such as orders and payments, I would start with a relational database because ACID transactions, constraints and query flexibility are important. Depending on the cloud, that could be Aurora PostgreSQL, Azure Database for PostgreSQL, or AlloyDB/Cloud SQL.
>
> For globally distributed transactional workloads such as inventory, I would evaluate distributed SQL such as YugabyteDB or Spanner if the consistency and multi-region requirements justify the complexity.
>
> For workloads with predictable key-value or document access patterns, such as carts, profiles or some catalog data, I would consider DynamoDB, Cosmos DB or Firestore.
>
> For low-latency read-heavy data, I would use Redis/Valkey-based caching.
>
> The most important point is that I would make the decision domain by domain. I would evaluate consistency, transaction boundaries, access patterns, scale, partitioning, latency, availability, geographic ownership, recovery requirements and operational maturity before selecting the technology."

---

# 32. Key Principle

> **Database architecture is a business decision expressed through technology.**

A good Solution Architect should be able to explain not only:

> **"Why PostgreSQL?"**

but also:

> **"Why PostgreSQL instead of DynamoDB?"**

> **"Why Spanner instead of PostgreSQL?"**

> **"Why YugabyteDB instead of Spanner?"**

> **"Why Cosmos DB instead of PostgreSQL?"**

and, most importantly:

> **"What business requirement makes the additional complexity worthwhile?"**
