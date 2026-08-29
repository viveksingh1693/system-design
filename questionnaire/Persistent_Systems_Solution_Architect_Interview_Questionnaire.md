# Persistent Systems — Solution Architect Interview Questionnaire

## Role Context

**Role:** Solution Architect  

### Core Requirements

- Cloud-native microservices
- Java, Spring Boot, REST APIs
- Event-driven architecture
- Distributed systems
- YugabyteDB, NoSQL, RDBMS
- Kafka
- Dapr
- Temporal
- AWS + GCP hybrid cloud
- Kubernetes
- CI/CD
- Observability
- Stakeholder management
- Architecture documentation

---

# 1. Architecture & Solution Design

1. Walk me through your approach to designing a solution for a new business requirement.

	“When I receive a new business requirement, I generally follow a structured approach, starting with the business problem rather than the technology.
	
	First, I clarify the requirement and business objective. I try to understand what problem we are solving, who the users are, the expected business outcome, and how success will be measured. I also clarify functional and non-functional requirements such as expected traffic, latency, availability, security, compliance, scalability, and data requirements.
	
	Second, I identify the constraints. This includes existing systems, technology standards, budget, cloud environment, team capabilities, timelines, regulatory constraints, and integration dependencies. I also identify whether we need to integrate with legacy systems.
	
	Third, I break the problem into business capabilities and identify the major components and boundaries. At this stage, I decide whether the solution should be a monolith, modular monolith, or microservices. If microservices are appropriate, I define service boundaries based on business capabilities and data ownership rather than simply splitting the existing codebase.
	
	Fourth, I design the high-level architecture. I define how clients, APIs, services, databases, messaging systems, caches, and external systems interact. I decide where synchronous communication such as REST makes sense and where asynchronous communication such as Kafka is more appropriate.
	
	Fifth, I evaluate the critical quality attributes. I specifically look at scalability, availability, consistency, fault tolerance, security, observability, and disaster recovery. For example, if the requirement is high throughput and asynchronous processing, I may introduce Kafka, but I would also consider ordering, partitioning, delivery semantics, retries, idempotency, and failure recovery.
	
	Sixth, I evaluate technology choices and trade-offs. I don't start with a technology and try to fit the problem into it. I compare alternatives based on the requirements. For example, I might compare PostgreSQL, a distributed database, or NoSQL depending on consistency, query patterns, scalability, and availability requirements.
	
	Seventh, I validate the architecture with stakeholders and engineering teams. I document the important decisions using architecture diagrams and ADRs, explain the trade-offs, and make sure business and technical stakeholders agree on the approach.
	
	Finally, I define the implementation and migration strategy. For an existing system, I usually prefer incremental migration rather than a big-bang rewrite. I define phases, dependencies, rollout strategy, observability, rollback mechanisms, and success criteria.
	
	So, in summary, my approach is:
	
	Business problem → Requirements → Constraints → Architecture boundaries → High-level design → Quality attributes → Technology trade-offs → Stakeholder validation → Implementation and migration strategy.
	
	The key principle I follow is that architecture should be driven by business requirements and quality attributes, not by technology preference.”
	
	
	“For example, in one of my modernization initiatives, we had a legacy Java application using Oracle, SOAP and JMS. Rather than directly rewriting it into microservices, we first identified the business capabilities and integration boundaries. We then moved selected functionality toward Spring Boot and REST, replaced JMS-based asynchronous communication with Kafka, migrated the database toward PostgreSQL, and deployed the services on Kubernetes. The important part was that we had to maintain compatibility with the existing system during the transition, so the migration and rollout strategy was just as important as the target architecture.”

---

2. How do you convert business requirements into an architecture?
		same as first
		|Step|Question|
		|---|---|
		|**B — Business**|What business problem are we solving?|
		|**R — Requirements**|What must the system do?|
		|**C — Constraints**|What limitations/dependencies exist?|
		|**D — Design**|What architecture satisfies the requirements?|
		|**T — Trade-offs**|Why this design over alternatives?|
		|**O — Operations**|How will we deploy, monitor, scale and recover it?|

---	
3. What factors determine whether you choose a monolith, modular monolith, or microservices?
	I don't choose between a monolith, modular monolith, and microservices purely based on technical preference. I look at the business and operational requirements first.
	
	I evaluate several factors.
	
	First is domain complexity and business boundaries. If the domain is relatively simple and tightly coupled, a monolith can be the simplest and most effective solution. If the domain has clear boundaries but doesn't yet require independent deployment, a modular monolith can provide strong separation while keeping operational complexity low. Microservices become more attractive when there are genuinely independent business capabilities that need to evolve independently.
	
	Second is scalability. If the entire application scales uniformly, a monolith may be sufficient. If certain capabilities have very different scaling requirements, microservices can allow those components to scale independently.
	
	Third is deployment independence. If teams need to release different business capabilities independently and frequently, microservices can provide an advantage. If independent deployment isn't a real requirement, introducing microservices may add unnecessary complexity.
	
	Fourth is team structure and ownership. Microservices work better when teams can independently own services end-to-end. If we have a small team, splitting the system into many services can create more operational overhead than value.
	
	Fifth is operational maturity. Microservices introduce distributed-system problems—network failures, service discovery, observability, distributed tracing, retries, eventual consistency, deployment management and incident debugging. If the organization isn't ready to operate these systems, a modular monolith may be the better choice.
	
	Sixth is data and transaction boundaries. If most operations require strongly consistent transactions across the same data model, a monolith or modular monolith may be simpler. If business capabilities have clear data ownership and can tolerate asynchronous communication or eventual consistency, microservices become more viable.
	
	Seventh is performance and latency. A monolith avoids network hops between modules, so for tightly coupled, low-latency operations it can be advantageous. Microservices introduce network communication and serialization overhead.
	
	Finally, I consider cost, security, compliance, technology diversity, organizational constraints and the expected evolution of the system.
	
	So, broadly:
	
	Monolith when simplicity, speed of development and tight coupling are acceptable.
	
	Modular monolith when we need strong domain boundaries and maintainability but don't need independent deployment or scaling.
	
	Microservices when we have clear business boundaries, independent scaling/deployment, autonomous teams, and sufficient operational maturity to manage distributed systems.
	
	My default isn't ‘microservices first.’ My default is to choose the simplest architecture that can satisfy the current and reasonably foreseeable business requirements.
---
3. When should you **not** use microservices?
	I would not use microservices when the additional complexity of distributed systems doesn't provide enough business value.
	
	There are several situations where I would prefer a monolith or modular monolith.
	
	First, when the domain is small or relatively simple. If the application has limited functionality and doesn't have meaningful business boundaries, splitting it into multiple services just creates unnecessary complexity.
	
	Second, when the team is small or lacks distributed-systems and operational maturity. Microservices require capabilities around Kubernetes, CI/CD, service discovery, observability, distributed tracing, resilience, security and production operations. If the organization isn't prepared for that, a modular monolith may be safer.
	
	Third, when the components are highly coupled. If most business transactions require synchronous communication and shared data across multiple components, microservices can turn simple in-process calls and database transactions into distributed transactions and network calls.
	
	Fourth, when strong transactional consistency is required across the entire workflow. If many operations need atomic ACID transactions across the same data, keeping them within one application and transaction boundary may be significantly simpler.
	
	Fifth, when there is no requirement for independent deployment or scaling. If all components are released together and have similar scaling characteristics, independently deployable services may not provide enough benefit to justify their operational cost.
	
	Sixth, for an early-stage product where requirements are still changing rapidly. If business boundaries aren't understood yet, prematurely splitting the system into services can lock us into incorrect boundaries. I would often start with a modular monolith and extract services once the boundaries become clearer.
	
	Finally, when the operational and infrastructure cost outweighs the benefits. Every service introduces deployment, monitoring, networking, security, configuration and incident-management overhead.
	
	So my principle is: I don't introduce microservices because they are modern. I introduce them when independent business boundaries, scaling, deployment, ownership or organizational requirements justify the complexity.
---
4. How do you identify service boundaries?
I identify service boundaries primarily from the business domain, not from the existing technical structure of the application.

First, I understand the business capabilities and workflows. I identify the major capabilities the business needs—for example, Customer, Order, Payment, Inventory, and Notification.

Second, I use domain-driven design concepts to identify bounded contexts. I look at where business terminology, rules, models and ownership are different. If two areas have different business language and rules, that is often a strong indication that they belong to separate bounded contexts.

Third, I identify data ownership. Each service should ideally own the data required for its business capability. If two proposed services constantly need to modify the same database tables or participate in the same transaction, that is a warning that the boundary may be wrong.

Fourth, I analyze coupling. I look at both business and technical coupling. If two components always change together, communicate synchronously for almost every operation, or require shared transactions, separating them may create unnecessary distributed-system complexity.

Fifth, I look at cohesion. Functionality that changes for the same business reason should generally stay together. I want high cohesion within a service and loose coupling between services.

Sixth, I consider scalability and availability characteristics. If one capability has very different traffic patterns or availability requirements from another, that can be a reason to separate it.

Seventh, I consider team ownership. A service should ideally have a clear team responsible for its development, deployment, operations and data. This follows the principle of end-to-end ownership.

Finally, I validate the boundaries against real business workflows and failure scenarios. I don't consider the boundary final after drawing the first diagram. I test it against transaction requirements, communication patterns, deployment needs, scalability and failure handling.

So the key factors I use are:

Business capability → Bounded context → Data ownership → Cohesion → Coupling → Transaction boundaries → Scalability → Team ownership → Operational characteristics.

The goal is not to create the smallest possible services. The goal is to create independently understandable, deployable and operable business capabilities with minimal coupling.
Example

Suppose we're designing an e-commerce platform.

A naive decomposition might be:

UserService
ProductService
AddressService
OrderService
PaymentService
EmailService
DatabaseService
ValidationService

This is not necessarily good microservice architecture.

Instead, start with business capabilities:

                 E-Commerce
                     │
       ┌─────────────┼─────────────┐
       ↓             ↓             ↓
   Customer        Order         Catalog
       │             │             │
       │             ↓             │
       │          Payment           │
       │             │             │
       │             ↓             │
       │         Inventory         │
       │                           │
       └────────── Events ─────────┘

Then ask:

Customer
Owns customer profile
Owns customer preferences
Owns customer identity-related data
Order
Owns order lifecycle
Owns order state
Coordinates order creation
Payment
Owns payment lifecycle
Owns payment transaction state
Integrates with payment providers
Inventory
Owns stock availability
Owns reservation/release
Catalog
Owns product information
Owns product metadata
Now the boundaries have business meaning.
The most important test: "What happens if I separate these?"

Suppose someone proposes:
“Let's make Order and Payment separate services.”

Don't automatically say yes.
Ask:
	a. Do they have separate business ownership?
	b. Do they have separate data ownership?
	c. Do they need independent scaling?
	d. Do they need independent deployment?
	e. Can they communicate asynchronously?
	f. What happens if Payment is unavailable?
	g. Can the business tolerate eventual consistency?
	h. What happens when Order succeeds but Payment fails?

---
5. What is the difference between a business capability and a microservice?
6. How do you prevent microservices from becoming a distributed monolith?
7. How do you decide between synchronous REST and asynchronous messaging?
8. How do you design for scalability from day one?
9. How do you design for availability and fault tolerance?
10. How do you identify single points of failure?
11. How do you perform architecture trade-off analysis?
12. What architecture principles do you follow?
13. How do you document architecture decisions?
14. What is an ADR? Give an example from your experience.
	An ADR preserves not just the architectural decision, but the context and reasoning behind that decision, so future engineers can understand why the system looks the way it does.

	ADR stands for Architecture Decision Record. It is a lightweight document used to capture an important architectural decision, the context in which it was made, the alternatives considered, and the consequences of that decision.
	
	I use ADRs because architecture decisions often outlive individual projects and team members. Without documenting the reasoning, a future team may see the decision but not understand why it was made.
	
	Typically, an ADR contains:
	
	Context — what problem or requirement led to the decision
	Decision — what we decided to do
	Options considered — alternatives we evaluated
	Rationale / trade-offs — why we selected the option
	Consequences — benefits, risks and limitations
	Status — proposed, accepted, superseded, or deprecated
	
	For example, in one of my modernization projects, we had a legacy architecture using JMS for asynchronous communication. As part of the modernization, we needed a scalable event-driven communication mechanism that could work effectively with our Spring Boot microservices and Kubernetes-based deployment.
	
	We evaluated continuing with JMS, introducing RabbitMQ, and adopting Kafka.
	
	We decided to move toward Kafka because we needed durable event streaming, horizontal scalability, consumer-group based processing, replay capability, and better alignment with the target cloud-native architecture.
	
	The ADR would capture the context, the alternatives, why Kafka was selected, and the consequences — such as having to manage partitioning, consumer lag, ordering, retries, idempotency and operational monitoring.
	
	So the important part of the ADR isn't simply recording 'we chose Kafka.' It records why we chose Kafka at that point in time and what trade-offs we accepted.
	
	If the architecture changes later, we can create a new ADR that supersedes the previous decision rather than silently changing the architecture.
---
15. How do you perform architecture reviews?
16. How do you evaluate a proposed technology before introducing it?
17. How do you balance technical debt against delivery timelines?
18. How do you handle conflicting requirements from different stakeholders?
19. How do you convince a business stakeholder to accept an architectural decision?

---

# 2. System Design

## Large-Scale Platforms

21. Design a globally scalable order-management system.
22. Design a payment processing platform.
	I would first separate payment processing into payment state, financial ledger and settlement. The Payment Service owns the payment lifecycle, while a separate immutable ledger records financial movements and reconciliation compares our state against provider records.
	
	The payment lifecycle would be modeled as a state machine—INITIATED, PROCESSING, AUTHORIZED, CAPTURED, FAILED, UNKNOWN, REFUND_PENDING and REFUNDED. UNKNOWN is especially important because a provider timeout doesn't mean the payment failed; the provider may already have charged the customer.
	
	Every payment operation would require an idempotency key, scoped to the merchant, and that key would be propagated to the provider wherever supported. The Payment Service would persist payment state and an outbox event in the same local transaction, then publish events through Kafka.
	
	I would isolate provider-specific logic behind a PaymentProvider interface and use a Provider Router for currency, geography, payment method, health, cost and success rate. Provider failover would be conservative: after an ambiguous timeout, I would reconcile the original provider before attempting another charge to avoid double charging.
	
	For security, I'd prefer provider tokenization or hosted payment components rather than storing raw card data, reducing PCI scope. I'd use OAuth/JWT externally, workload identity or mTLS internally, and strong secrets management.
	
	Finally, I'd build reconciliation and settlement as first-class components. Payment success, ledger balance and merchant settlement are different concepts. The platform must continuously reconcile them and provide an auditable history.
	
	So my core architecture is: Payment State Machine + Idempotency + Provider Abstraction + Transactional Outbox + Kafka + Immutable Ledger + Reconciliation + Settlement + Strong Security.
21. Design an event-driven e-commerce platform.
22. Design a notification platform supporting email, SMS and push.
23. Design a distributed job/workflow processing system.
24. Design a real-time transaction processing system.
25. Design an API platform handling millions of requests per minute.
26. Design a multi-tenant SaaS platform.
27. Design a URL-shortening service.
28. Design a distributed file-processing platform.
29. Design a media processing pipeline.
                         GLOBAL DNS
                             |
              +--------------+--------------+
              |              |              |
              v              v              v
            REGION          REGION         REGION
              |              |              |
              v              v              v
           Upload          Upload         Upload
           Service         Service        Service
              |              |              |
              v              v              v
        +---------------------------------------+
        |             OBJECT STORAGE            |
        |        Original / Processed Media     |
        +-------------------+-------------------+
                            |
                       Object Event
                            |
                            v
                    +---------------+
                    | Kafka / Queue |
                    +-------+-------+
                            |
                            v
                   +------------------+
                   | Workflow Engine  |
                   | Temporal / SFN   |
                   +--------+---------+
                            |
          +-----------------+-----------------+
          |                 |                 |
          v                 v                 v
      Metadata          Thumbnail         Transcoding
       Workers            Workers          CPU / GPU
          |                 |                 |
          +-----------------+-----------------+
                            |
                            v
                    Processed Storage
                            |
                            v
                          CDN
                            |
                            v
                         CLIENT


I would design it as an asynchronous, event-driven pipeline. Clients first request an upload session and receive a pre-signed URL. They upload large files directly to object storage using multipart upload, so application servers never become a bandwidth bottleneck. Object-storage events then publish an asset-created event to Kafka or another event bus.

For orchestration, I'd use a workflow engine such as Temporal or a cloud-native equivalent. The workflow validates the media and fans out independent activities such as metadata extraction, thumbnail generation, moderation and transcoding. CPU- and GPU-intensive transcoding would run on independently scalable worker pools rather than normal API pods.

Each processing step would be idempotent using a deterministic key such as jobId + operation + inputVersion + profile. I'd use leases for worker ownership and fencing/version checks where stale workers could overwrite results. Failed transient operations use exponential backoff and jitter, while permanent failures go to a DLQ.

Outputs are stored in object storage using immutable, versioned paths. Once processing completes, the media is served through a CDN using signed URLs or cookies for private content. Metadata and workflow state live in a transactional database, while Kafka handles event distribution and the workflow engine owns process state.

For global scale, I'd deploy regional ingestion, storage and processing clusters close to users, with global routing and an appropriate metadata replication strategy. I'd implement backpressure, priority queues, autoscaling, tenant quotas and observability using metrics, logs and distributed tracing.

The key architectural principles are direct-to-object-storage upload, asynchronous processing, workflow orchestration, independently scalable workers, idempotent processing, durable state, CDN-based delivery and explicit failure/recovery handling.


---
# 3. Distributed Systems

30. Explain CAP theorem with a real-world example.
	CAP theorem states that a distributed system cannot simultaneously guarantee strong Consistency, Availability, and Partition tolerance when a network partition occurs.

	CAP says that in a distributed system, when a network partition occurs, we cannot simultaneously guarantee strong consistency and availability. Partition tolerance is generally unavoidable because network failures can happen. So the practical choice is between C and A during a partition.
	
	Consider a bank account replicated between Delhi and Mumbai. Suppose the balance is ₹1,000 and a customer withdraws ₹800 from Delhi, making the balance ₹200. Now the network between Delhi and Mumbai fails. If Mumbai continues processing withdrawals based on its stale ₹1,000 balance, the system remains available but can produce an incorrect financial outcome. That's an AP-style decision. If Mumbai rejects or delays the transaction until it can establish the correct balance, we sacrifice availability but preserve consistency—CP behavior.
	
	The important architectural point is that CAP is not necessarily a single choice for the entire application. Different business operations can have different consistency requirements. For example, payment and inventory reservation should generally favor consistency, while shopping carts, recommendations and social-media likes can favor availability and eventual consistency.
	
	So when designing a distributed system, I first identify the business invariant, then determine what can be eventually consistent and what must be strongly consistent, and finally choose the storage and replication strategy accordingly."
	
	The one line to remember
	
	CAP isn't "pick two"; it's "when the network partitions, what are you willing to sacrifice—consistency or availability?
---
	
31. CAP vs PACELC?
	CAP asks: > **What happens during a partition?**
	PACELC extends the thinking: > **If there is a Partition, choose Availability or Consistency; Else, choose Latency or Consistency.**
	CAP is not necessarily a property of the entire application. I make the consistency decision at the business-operation level.
	For example, in your **order-management system**:
	                    ORDER SYSTEM
                         |
			        +----------------+----------------+
			        |                |                |
			        v                v                v
			    Inventory          Payment           Cart
			       CP                CP               AP
			        |                |                |
			   correctness       correctness       availability
			                 Distributed System
                         |
                Partition occurs?
                   /           \
                 YES            NO
                  |              |
                  v              v
                A or C          L or C
			   
32. Strong consistency vs eventual consistency.
	Strong consistency gives you correctness immediately after a successful write. Eventual consistency gives you availability/latency benefits while allowing replicas to temporarily disagree, with the guarantee that they converge if updates stop.
	||Strong Consistency|Eventual Consistency|
	|---|---|---|
	|Read after write|Latest value|May see older value|
	|Replica disagreement|Minimized/avoided by the model|Temporarily allowed|
	|Latency|Usually higher|Usually lower|
	|Availability|Can be lower during failures|Often higher|
	|Scalability|More coordination|Easier horizontal scaling|
	|Complexity|Coordination complexity|Conflict/reconciliation complexity|
	|Typical use|Payments, inventory, reservations|Catalogs, feeds, analytics, likes|
	
	 Techniques for Strong Consistency
		ACID transactions
		Pessimistic locking
		Optimistic locking
		Quorum reads/writes
		Leader-based replication
		Consensus protocols
		Serializable isolation
		Unique constraints
	Techniques for Eventual Consistency
		Asynchronous replication
		Kafka/events
		CDC
		Caches
		Materialized views
		Read replicas
		Dynamo-style replication
		CQRS
		Event-driven architecture


	I don't choose strong or eventual consistency for the entire application. I define the consistency requirement around the business invariant.
	
	For example, in an order platform, payment, inventory reservation and seat allocation need strong consistency around the critical transaction because stale data can create an incorrect financial or business outcome. On the other hand, product catalogs, search indexes, recommendations, analytics and notifications can generally be eventually consistent.
	
	For global systems, I also avoid assuming that strong consistency means synchronous replication across every region. I can maintain a strongly consistent local transaction boundary and asynchronously propagate domain events globally.
	
	If I choose eventual consistency, I explicitly design for its consequences—idempotency, versioning, ordering, retries, conflict resolution, read-your-writes where required, and reconciliation. The key is to choose the weakest consistency model that still protects the business invariant.
	
	The line I'd remember
	Don't choose consistency based on technology; choose it based on the business invariant.

---

33. How does distributed consensus work?
	Distributed consensus is the process by which multiple nodes agree on a single value or sequence of decisions, despite node failures, message delays, and network problems.
		Agreement
		Leader election
		Replicated state machines
		Cluster membership
		Log ordering

	you'll encounter consensus in:
		Distributed databases
		Kubernetes control plane
		Service discovery
		Configuration stores
		Leader election
		Metadata management
		Distributed locks
		Cluster membership
	Examples include systems built around:
		Raft
		Paxos
		etcd
		ZooKeeper
		Consul
		CockroachDB
		TiDB
		Kafka KRaft

	Distributed consensus allows multiple nodes to agree on a single ordered decision despite failures and network delays. A common implementation is Raft, which uses a leader, followers and majority quorum.
	
	For example, with three nodes A, B and C, the majority is two. A leader receives a write, appends it to its log and replicates it to followers. Once the entry is safely replicated to a majority, it is considered committed and the leader can apply it and acknowledge the client.
	
	If the leader fails, the remaining nodes use an election to select a new leader. If there's a network partition, the side without a majority cannot commit new entries, which prevents split-brain decisions.
	
	The key idea is that quorum overlap ensures two conflicting majorities cannot both independently commit different decisions. The trade-off is that consensus introduces network coordination and therefore latency, so I would use it only where strong ordering or agreement is required—such as leader election, metadata, distributed locks or critical transaction state.

                  CLIENT
                     |
                     v
                  LEADER
                 /      \
                v        v
             NODE B    NODE C
                \        /
                 \      /
                  QUORUM
                    |
                    v
                 COMMIT
Consensus is essentially quorum-based agreement on an ordered sequence of decisions, with the guarantee that a committed decision cannot be replaced by a conflicting decision.

---

34. What is quorum?
	A quorum is the minimum number of nodes that must participate or agree for a distributed operation to be considered successful.
	Q=⌊N/2⌋+1
	A common quorum configuration aims for: R+W>N
	Quorum is the minimum number of nodes that must participate or agree for a distributed operation to be considered successful. In consensus systems, it's typically a majority: for 3 nodes the quorum is 2, and for 5 nodes it's 3.

	The key reason for quorum is preventing split-brain. For example, in a five-node cluster, if a network partition creates groups of 2 and 3, only the group of 3 has quorum and can continue committing decisions. The group of 2 must stop, preventing both sides from independently committing conflicting state.
	
	In replicated databases, read and write quorums can also be configured. If N is the number of replicas, a common consistency condition is R + W > N, because that guarantees overlap between the read and write quorums.
	
	The trade-off is that larger quorum requirements can improve consistency but may increase latency and reduce availability during failures.

	Quorum = "Enough nodes agree so that conflicting groups cannot both make authoritative decisions."

35. Explain leader election?
	Never rely on "there can only be one leader" because of timing alone.
	Term / Epoch
	+
	Quorum
	+
	Voting rules
	+
	Heartbeat
	+
	Log/State validation

	Leader election is the process by which nodes in a distributed cluster select one node as the coordinator while preventing conflicting leaders from making authoritative decisions. A common implementation is Raft. Nodes normally remain followers and receive heartbeats from the leader. If a follower doesn't receive a heartbeat within its randomized election timeout, it becomes a candidate, increments the term, votes for itself and requests votes from other nodes. If it gets a majority, it becomes the leader and starts sending heartbeats.
	
	If the leader fails, another node starts an election. Quorum is important because only a candidate with a majority can become leader. During a network partition, a minority side cannot elect a legitimate leader, which prevents split-brain. Terms or epochs ensure that an old leader cannot continue making decisions after a newer leader has been elected.
	
	The trade-off is that leader election improves consistency and coordination but requires quorum availability, so during a partition the system may sacrifice availability rather than allow conflicting leaders.

	Leader Election =
	Timeout
	   ↓
	Candidate
	   ↓
	Request Votes
	   ↓
	Majority
	   ↓
	Leader
	   ↓
	Heartbeats
---
36. What happens when network partitions occur?
	A network partition means nodes are alive but cannot communicate reliably with each other. The first concern is preventing split-brain. In a consensus-based system, nodes use quorum and leader election so that only the partition containing a majority can continue committing decisions. The minority side becomes unavailable for writes rather than risking conflicting state.
	
	For example, in a five-node cluster split into 2 and 3 nodes, the three-node side has quorum and can elect a leader; the two-node side cannot safely commit new writes. Terms or epochs prevent an old leader from continuing to act as authoritative after a new leader is elected.
	
	When the partition heals, replicas reconcile their state. Consensus systems reconcile logs and discard conflicting uncommitted entries, while eventually consistent systems perform conflict resolution and converge.
	
	The exact behavior depends on the business requirement. Payments, inventory and reservations usually favor consistency and may reject requests during a partition, while things like feeds, likes and recommendations can favor availability and reconcile later.
---
36. What is split-brain?
	How to Prevent Split-Brain

                 Split-Brain Prevention

                       |
        +--------------+--------------+
        |              |              |
        v              v              v
      Quorum       Leader Election   Epoch/Term
        |              |              |
        +--------------+--------------+
                       |
                       v
                  Fencing Token
                       |
                       v
              Reject Stale Leaders


	Split-brain is a condition where two or more parts of a distributed system independently believe they are authoritative and continue making decisions. It's particularly dangerous because both sides can accept conflicting writes.
	
	For example, suppose I have a five-node cluster and a network partition creates groups of two and three. If both groups could elect leaders and accept writes, they could create divergent state. A quorum-based consensus system prevents this because only the three-node majority has quorum; the two-node minority cannot commit decisions.
	
	Leader election with terms or epochs ensures that a newly elected leader supersedes the old one, and fencing tokens can prevent a stale worker or leader from continuing to perform writes after losing ownership. When the partition heals, the system reconciles state and brings the stale nodes back into the cluster.

---
36. How would you prevent duplicate processing?
	I wouldn't try to guarantee that a distributed message is executed exactly once. I'd assume at-least-once delivery and design the business operation to be idempotent.
	
	For example, if a Kafka consumer updates the database and crashes before acknowledging the message, Kafka will redeliver it. I would use the event ID as an idempotency key, store it with a unique constraint, and perform the deduplication record and business update in the same database transaction. The second delivery would detect that the event was already processed and become a no-op.
	
	For client APIs such as payments, I'd use an idempotency key supplied by the client and persist the key with the operation result. For external services, I'd propagate an idempotency key downstream because my database transaction cannot atomically include an external API call.
	
	For event publication, I'd use the transactional outbox pattern. The outbox itself may publish duplicates, so consumers still need idempotency. For long-running workers, I'd combine leases and heartbeats with idempotent processing and deterministic outputs.
	
	So my overall strategy is at-least-once delivery plus effectively-once business semantics using idempotency, unique constraints, transactions, versioning, deterministic operations, retries and reconciliation.

	Exactly-once delivery is a transport property; exactly-once business effect is an application design property.
---
37. Explain idempotency.
	Idempotency in Distributed Architecture

                       CLIENT
                          |
                          v
                    API Gateway
                          |
                          v
                   Payment Service
                          |
                   Idempotency Layer
                          |
              +-----------+-----------+
              |                       |
              v                       v
          Business DB           Payment Gateway
              |                       |
              |                  Idempotency Key
              |
              v
           Outbox
              |
              v
            Kafka
              |
       +------+------+
       |             |
       v             v
 Notification    Accounting
   Service         Service
       |             |
       v             v
 Idempotent       Idempotent
 Consumer         Consumer

Idempotency means that executing the same logical operation multiple times produces the same business outcome as executing it once. It's essential in distributed systems because retries and duplicate delivery are unavoidable.

For example, consider a payment API. The client sends a payment with an idempotency key such as PAY-123. The payment succeeds, but the response is lost due to a network timeout. The client retries PAY-123. Instead of charging again, the service detects that the operation has already been processed and returns the original payment result.

At the implementation level, I'd persist the idempotency key with a unique constraint and the operation result. The idempotency record and business state change should be committed atomically where possible. I'd also validate that retries with the same key contain the same request parameters.

For Kafka consumers, I'd use event IDs and a processed-event table, ideally in the same transaction as the business update. For event publishing, I'd use the transactional outbox pattern, while still keeping consumers idempotent because the outbox publisher can itself produce duplicates.

For external APIs such as payment gateways, I'd propagate the same idempotency key downstream because my database transaction cannot make an external API call atomic. If the downstream system doesn't support idempotency, I'd use status queries, unique transaction references and reconciliation rather than blindly retrying.

So I don't try to eliminate duplicate delivery. I design the system so duplicate execution produces no additional business effect.

---

38. How would you design an idempotent API?
                         CLIENT
                            |
                            | POST + Idempotency-Key
                            v
                    +----------------+
                    |  API Gateway   |
                    +-------+--------+
                            |
                            v
                    +----------------+
                    | Payment Service|
                    +-------+--------+
                            |
                            v
                +-----------------------+
                | Idempotency Repository|
                +-----------+-----------+
                            |
                  +---------+---------+
                  |                   |
              Existing              New
                  |                   |
                  v                   v
            Return Result       Create IN_PROGRESS
                                      |
                                      v
                               Business Operation
                                      |
                         +------------+------------+
                         |                         |
                         v                         v
                    Business DB             External Provider
                         |                         |
                         v                         |
                      Outbox <---------------------+
                         |
                         v
                       Kafka
                         |
              +----------+----------+
              |                     |
              v                     v
        Notification           Accounting
          Consumer               Consumer
              |                     |
              v                     v
          Idempotent             Idempotent

		1. Client supplies an idempotency key.
		2. Scope it appropriately:
			   tenant + idempotency key.
		3. Enforce uniqueness atomically.
		4. Store request hash to detect key reuse with different parameters.
		5. Persist the operation/result durably.
		6. Make downstream side effects idempotent too.
		7. Use transactions/outbox/reconciliation to handle crashes between steps.


I'd design the API around a client-provided idempotency key representing one logical business operation. For example, a payment request would contain Idempotency-Key: PAY-123. I would persist that key with the tenant, a canonical request hash, operation status and result, and enforce a unique constraint on the key.

When the first request arrives, I atomically create an IN_PROGRESS record and process the operation. Once successful, I persist the business result and mark the operation complete. If the same request is retried, I return the previously stored result instead of executing the operation again. If the same key is reused with different request parameters, I reject it.

The atomic uniqueness constraint is important because two identical requests can arrive concurrently; an application-level check-then-insert is subject to a race condition.

For external side effects such as payment gateways, I propagate the same idempotency key downstream because my database transaction cannot atomically include an external API. I'd also use transactional outbox for reliable event publication and make Kafka consumers idempotent because events can still be delivered more than once.

Finally, I'd define an appropriate retention period for idempotency records, handle IN_PROGRESS operations after crashes, and expose the idempotency key through tracing and audit metadata. The goal isn't to prevent retries; it's to ensure retries produce the same business outcome.


---
39. What is distributed locking?
	          Lock Service
	               |
	       +-------+-------+
	       |               |
	   Worker A         Worker B
	   Token 10         Token 11
	       |               |
	       v               v
	       +-------> Storage
	                   |
	             Accept only
	             latest token
	 A distributed lock answers who owns the work; idempotency answers what happens if the work happens again; fencing ensures a stale owner can't continue after losing ownership.

	Distributed locking provides exclusive ownership of a shared resource across multiple application instances. For example, if three service instances need to run a reconciliation job but only one should execute it, they can compete for a distributed lock.

	For a simple implementation, I could use Redis with an atomic SET NX and a lease/TTL, using a unique owner token. However, I wouldn't consider TTL alone sufficient for critical operations because a worker can pause or lose connectivity, the lease can expire, and another worker can acquire the lock while the original worker is still running.
	
	For critical operations, I'd use fencing tokens so every lock acquisition gets a monotonically increasing token, and the protected resource rejects operations from stale tokens. Lock release must also be ownership-checked atomically so an old worker can't accidentally release a newer owner's lock.
	
	I'd also combine locking with idempotency. The lock controls who can execute concurrently; idempotency ensures that retries or stale executions don't create duplicate business effects.
	
	Before introducing a distributed lock, I'd check whether the invariant can be enforced using an atomic database operation, optimistic concurrency, partitioning or idempotency, because distributed locks introduce coordination overhead and additional failure modes.

		                 Need exclusive access?
                          |
                          v
             Can DB enforce invariant?
                    /           \
                  YES            NO
                   |              |
                   v              v
             Atomic update    Need coordination
                                  |
                                  v
                         Distributed Lock
                                  |
                         +--------+--------+
                         |                 |
                      Lease            Fencing
                         |                 |
                         +--------+--------+
                                  |
                                  v
                            Idempotency
                                  |
                                  v
                         Safe failure/retry
---
40. Redis distributed lock vs database lock.
	I'd choose based on the resource and consistency boundary rather than simply choosing Redis because it's faster. Redis is a good choice for lightweight, high-throughput coordination such as singleton jobs or short-lived worker ownership. It gives us low latency and natural lease/TTL semantics. However, TTL alone doesn't solve stale-owner problems, so for correctness-critical operations I'd consider fencing tokens and make the business operation idempotent.
	
	A database lock is preferable when the resource being protected is already database state. For example, inventory reservation can use a transaction with row-level locking or, even better, an atomic conditional update. The advantage is that the lock and business state change can be part of the same transaction.
	
	I would avoid using database locks for high-frequency global coordination because lock contention can turn the database into a bottleneck. Conversely, I wouldn't use Redis as the sole source of truth for a critical financial invariant just because it's fast.
	
	For critical distributed coordination where stale ownership must be prevented, I'd consider a consensus-backed coordination system such as etcd or ZooKeeper, together with leases and fencing. Regardless of the locking mechanism, I'd still use idempotency because a lock controls concurrent ownership but doesn't eliminate retries or duplicate execution.

	***The mental model

                 Need coordination?
                         |
                         v
             What are you protecting?
                         |
              +----------+----------+
              |                     |
         DB-owned state       Independent work
              |                     |
              v                     v
       Atomic update /         Redis / Lease /
       DB transaction          Coordination
              |                     |
              +----------+----------+
                         |
                         v
                 Critical operation?
                         |
                    +----+----+
                   YES       NO
                    |         |
                    v         v
               Fencing +   Simpler
               Idempotency approach
---
41. What is a distributed transaction?
	A distributed transaction occurs when a business transaction spans multiple independent resources or services. The challenge is handling partial failure because we no longer have a single database transaction that can atomically commit or rollback everything.
	
	The traditional solution is Two-Phase Commit, where a coordinator first asks all participants to prepare and then tells them to commit. It provides atomic commit semantics but introduces coordination overhead, blocking, latency and coordinator failure issues. It also doesn't work well with external services that don't participate in the transaction protocol.
	
	For microservices, I generally prefer the Saga pattern. Each service performs a local ACID transaction, and the overall business workflow is coordinated either through events or a workflow orchestrator. If a later step fails, we execute compensating actions—for example, refund the payment and release inventory.
	
	I would combine Saga with idempotency, transactional outbox, retries, dead-letter handling and reconciliation. Idempotency protects against duplicate messages and retries, while the outbox guarantees that a committed business state change isn't separated from its event publication.
	
	Before introducing any distributed transaction, I first try to keep the invariant within a single service/database boundary. If strong atomic consistency across independent resources is absolutely required, then I would evaluate 2PC or a distributed transactional datastore, but I'd accept the availability and operational trade-offs."
	
	2PC gives atomic commit through coordination; Saga gives business-level consistency through local transactions and compensation. In microservices, I prefer Saga unless true atomicity is a hard business requirement.
---
42. Why are distributed transactions difficult?
43. Explain the Saga pattern.
44. Choreography vs orchestration.
45. Two-phase commit vs Saga.
46. How do you handle partial failures?
47. How do you handle retry storms?
48. What is exponential backoff?
49. What is a circuit breaker?
50. Bulkhead pattern?
51. Timeout vs retry?
52. How do you design a system that continues operating when downstream services fail?

---

# 4. Microservices Architecture

56. How do you decompose a monolith?
57. Explain the Strangler Fig pattern.
58. Database-per-service vs shared database.
59. How do microservices communicate?
60. REST vs gRPC vs messaging.
61. What are API Gateway responsibilities?
62. Explain service discovery.
63. Explain configuration management.
64. Centralized vs decentralized configuration.
65. How do you implement distributed tracing?
66. What are correlation IDs?
67. Health checks vs readiness checks.
68. Liveness vs readiness vs startup probes.
69. How do you handle backward compatibility?
70. How do you version APIs?
71. How do you perform zero-downtime deployments?
72. Blue-green vs canary deployment.
73. How do you handle schema evolution?
74. How do you migrate a legacy monolith without stopping business operations?
75. How do you prevent cascading failures?

---

# 5. Kafka — High Priority

76. Explain Kafka architecture.
77. Topic vs partition vs replica.
78. How does Kafka achieve scalability?
79. How does Kafka achieve fault tolerance?
80. Explain leader and follower replicas.
81. What happens when a broker fails?
82. What is ISR?
83. Explain `acks=0`, `acks=1`, and `acks=all`.
84. How does Kafka guarantee ordering?
85. Is Kafka exactly-once?
86. At-most-once vs at-least-once vs exactly-once.
87. Explain consumer group architecture.
88. What happens when a consumer crashes?
89. Explain consumer rebalancing.
90. Cooperative vs eager rebalancing.
91. Explain offset management.
92. Auto commit vs manual commit.
93. What happens if a consumer processes a message but crashes before committing the offset?
94. How do you achieve idempotent consumers?
95. Explain Kafka producer retries.
96. What is producer idempotence?
97. Explain transactional producers.
98. Explain Kafka transactions.
99. How do you handle poison messages?
100. Retry topic vs DLQ.
101. How would you implement delayed retries?
102. How do you choose partition count?
103. What happens if you need more partitions later?
104. How do you select a partition key?
105. What happens if your key creates a hot partition?
106. How do you handle Kafka consumer lag?
107. How do you troubleshoot increasing consumer lag?
108. Retention vs compaction.
109. What are log compaction use cases?
110. Kafka vs RabbitMQ.
111. Kafka vs traditional JMS.
112. When should Kafka not be used?
113. How would you design Kafka for millions of events per second?
114. How would you design multi-region Kafka?
115. How would you guarantee event processing during database failure?

---

# 6. Java — Architecture-Level Depth

116. Explain JVM architecture.
117. Heap vs stack vs metaspace.
118. How does garbage collection work?
119. Explain G1 GC architecture.
120. How would you investigate high GC pauses?

		I would investigate high GC pauses systematically rather than immediately changing JVM parameters. First I'd establish whether GC is correlated with the latency problem by looking at P99 latency, throughput, CPU, allocation rate and GC pause metrics. Then I'd identify the collector and analyze GC logs for frequency, pause duration, heap occupancy before and after GC, Full GC events, promotion behavior and, with G1, humongous allocations.
		
		Next I'd determine whether we're dealing with high allocation pressure or object-retention problems. If the heap drops significantly after GC but GC is frequent, I'd investigate allocation hotspots using JFR or profiling. If the heap doesn't drop and the old generation keeps growing, I'd take a heap dump and use the dominator tree and GC-root analysis to identify retained objects or leaks.
		
		I'd also check metaspace, direct/off-heap memory, thread stacks and container memory limits, especially in Kubernetes, because JVM memory isn't limited to the Java heap. I'd correlate GC events with traffic spikes and application behavior to determine whether the issue is caused by workload or a memory-retention problem.
		
		Only after identifying the root cause would I tune the JVM—heap sizing, GC settings, allocation behavior or application code. For example, high allocation pressure may require reducing temporary object creation, while a growing old generation may require fixing a cache or object-retention issue. The goal isn't simply lower GC time; it's meeting the application's latency SLO while maintaining acceptable throughput and memory utilization.


		GC tuning is the last step; first determine whether the pause is caused by allocation pressure, an oversized live set, memory retention, collector configuration, or non-heap memory.


				1. Confirm customer impact
				        ↓
				2. Check P99 latency + throughput
				        ↓
				3. Check GC pause/frequency
				        ↓
				4. Identify GC algorithm
				        ↓
				5. Analyze GC logs
				        ↓
				6. Check heap before/after GC
				        ↓
				7. Check allocation rate
				        ↓
				8. Check old-gen/live-set growth
				        ↓
				9. Check Full GC / humongous objects
				        ↓
				10. Capture JFR
				        ↓
				11. Heap dump if retention suspected
				        ↓
				12. Find GC roots
				        ↓
				13. Correlate with application behavior
				        ↓
				14. Tune application/JVM
				        ↓
				15. Validate against latency SLO


---
121. What causes memory leaks in Java?
	Common Memory Leak Categories


		|Cause|Example|
		|---|---|
		|Static reference|Static Map|
		|Unbounded cache|ConcurrentHashMap|
		|ThreadLocal|Missing `remove()`|
		|Listener|Never unregister|
		|Queue|Unbounded queue|
		|Executor|Outstanding tasks|
		|ClassLoader|Redeployment leak|
		|Hibernate|Huge persistence context|
		|Large objects|byte[] / JSON|
		|Async|Uncompleted futures|
		|Reactive|Missing backpressure|
		|Native memory|DirectByteBuffer|
		|Resources|Streams/connections|
		
              Memory Usage Increasing
                       |
                       v
                 Check GC Logs
                       |
                       v
             Heap after GC increasing?
                  /            \
                NO              YES
                |                |
                v                v
        High allocation      Retention problem
                |                |
                v                v
              JFR           Heap Dump
                |                |
                v                v
       Allocation hotspot    Dominator Tree
                                 |
                                 v
                            GC Roots
                                 |
                                 v
                          Find Retention Path
                                 |
                                 v
                              Fix Code
                                 |
                                 v
                            Re-test



A Java memory leak occurs when objects are no longer logically needed but remain reachable from a GC root, so the garbage collector cannot reclaim them. Common causes include static collections, unbounded caches, ThreadLocal values in long-lived thread pools, listeners that aren't unregistered, unbounded queues, outstanding asynchronous tasks, ClassLoader leaks, oversized Hibernate persistence contexts and large object graphs retained by application state.

I'd distinguish a memory leak from high allocation pressure. With high allocation, objects are created rapidly but the heap drops significantly after GC. With a leak, the post-GC heap baseline keeps increasing.

To investigate, I'd first look at GC logs and JVM metrics. If post-GC occupancy keeps growing, I'd take multiple heap dumps and analyze them using MAT or a similar tool, starting with the dominator tree and retained heap and then following references back to GC roots. I'd use JFR to identify allocation hotspots and correlate the problem with application behavior.

I'd also check off-heap memory such as direct buffers, metaspace, thread stacks and native allocations because a container OOM isn't necessarily a Java heap leak. Once I identify the retention path, I'd fix the application—for example, adding cache eviction, removing ThreadLocal values, bounding queues, clearing Hibernate persistence contexts, unregistering listeners or fixing ClassLoader references—and then validate the fix under realistic load.

A memory leak is fundamentally a reachability problem: the object is dead from the business perspective but still alive from the JVM's perspective.

122. Explain Java Memory Model.
123. `volatile` vs `synchronized`.
124. Atomic classes vs locks.
125. Explain CAS.
126. Explain `ConcurrentHashMap` internals.
127. Explain thread pools.
128. How do you size a thread pool?
129. CPU-bound vs I/O-bound workloads.
130. What happens when an ExecutorService queue becomes full?
131. Explain CompletableFuture.
132. Explain virtual threads in modern Java.
133. When would you use virtual threads?
134. How do you identify thread starvation?
135. How do you diagnose deadlocks?
136. How do you optimize a high-throughput Java service?

---

# 7. Spring Boot

137. Explain Spring Boot startup.
138. Explain dependency injection internals.
139. Explain the Spring bean lifecycle.
140. Singleton vs prototype.
141. Explain Spring Boot Actuator.
142. Explain Spring Security architecture.
143. Explain transaction management.
144. Explain `@Transactional` internals.
145. Explain transaction propagation.
146. Explain transaction isolation levels.
147. Optimistic vs pessimistic locking.
148. Spring Boot application performance tuning.
149. How would you design a resilient REST API?
150. Global exception handling.
151. Validation strategies.
152. Rate limiting.
153. Authentication vs authorization.
154. How would you secure microservices?

---

# 8. REST API Architecture

155. What makes an API RESTful?
156. PUT vs PATCH.
157. POST vs PUT.
158. Explain HTTP idempotency.
159. API versioning strategies.
160. Pagination.
161. Cursor vs offset pagination.
162. Rate limiting algorithms.
163. API throttling.
164. Authentication mechanisms.
165. JWT architecture.
166. OAuth 2.0 flow.
167. How do you handle token expiration?
168. How do you secure service-to-service communication?
169. How would you design an API capable of handling 100K requests/sec?

---

# 9. Database Architecture

170. How do you choose between SQL and NoSQL?
171. RDBMS vs document database.
172. What is normalization?
173. When would you deliberately denormalize?
174. Explain indexing strategy.
175. Explain composite indexes.
176. B-tree vs Hash index.
177. Query optimization.
178. Explain database partitioning.
179. Horizontal vs vertical partitioning.
180. Explain sharding.
181. How do you select a sharding key?
182. What are hot partitions?
183. Explain replication strategies.
184. Explain read replicas.
185. Database failover.
186. Connection pooling.
187. HikariCP tuning.
188. Database transactions.
189. Isolation levels.
190. Deadlocks.
191. Optimistic vs pessimistic locking.
192. Database migration strategies.
193. Zero-downtime database migration.
194. Oracle → PostgreSQL migration challenges.
195. SQL → NoSQL migration considerations.

---

# 10. YugabyteDB

196. What is YugabyteDB?
197. YugabyteDB vs PostgreSQL.
198. Explain YugabyteDB architecture.
199. YSQL vs YCQL.
200. How does YugabyteDB distribute data?
201. How does YugabyteDB provide high availability?
202. What happens when a node fails?
203. Explain replication in YugabyteDB.
204. Explain its consistency model.
205. Explain distributed transactions.
206. YugabyteDB vs traditional PostgreSQL.
207. YugabyteDB vs Cassandra.
208. When would you choose YugabyteDB?
209. When would you avoid YugabyteDB?
210. How would you migrate an existing PostgreSQL application to YugabyteDB?

---

# 11. NoSQL

211. Explain key-value databases.
212. Explain document databases.
213. Explain wide-column databases.
214. Explain graph databases.
215. Explain Cassandra architecture.
216. Explain MongoDB architecture.
217. Explain DynamoDB architecture.
218. How do you choose partition keys?
219. Explain eventual consistency.
220. Explain secondary indexes.
221. Explain NoSQL data modeling.
222. When should NoSQL be preferred over RDBMS?

---

# 12. Dapr

223. What is Dapr?
224. Why use Dapr?
225. Explain Dapr sidecar architecture.
226. Explain Dapr service invocation.
227. Explain Dapr pub/sub.
228. Explain Dapr state management.
229. Explain Dapr bindings.
230. Explain Dapr secrets.
231. Explain Dapr workflows.
232. Dapr vs Spring Cloud.
233. Dapr vs service mesh.
234. How would you deploy Dapr on Kubernetes?
235. What problems does Dapr solve?
236. What problems does Dapr introduce?

---

# 13. Temporal

237. What is Temporal?
238. Why use Temporal instead of Kafka?
239. Temporal workflow vs normal application code.
240. Workflow vs activity.
241. How does Temporal achieve durability?
242. How does Temporal handle retries?
243. How does Temporal handle failures?
244. Temporal vs AWS Step Functions.
245. Temporal vs Kafka.
246. Temporal vs traditional scheduler.
247. Design a payment workflow using Temporal.
248. How would you handle long-running business processes?

---

# 14. Kubernetes

249. Explain Kubernetes architecture.
250. Control plane vs worker nodes.
251. API Server.
252. Scheduler.
253. Controller Manager.
254. etcd.
255. Deployment vs StatefulSet.
256. Service types.
257. Ingress.
258. ConfigMap vs Secret.
259. Resource requests vs limits.
260. HPA.
261. Cluster autoscaling.
262. Pod disruption budget.
263. Rolling deployment.
264. Canary deployment.
265. Liveness/readiness probes.
266. How would you troubleshoot a pod repeatedly restarting?
267. How would you troubleshoot high CPU?
268. How would you troubleshoot memory OOM?
269. How would you design highly available Kubernetes workloads?

---

# 15. Cloud Architecture — AWS + GCP

270. Design a cloud-native application on AWS.
271. Design the same architecture on GCP.
272. AWS vs GCP architecture differences.
273. VPC architecture.
274. Public vs private subnet.
275. Load balancers.
276. IAM.
277. Secrets management.
278. Object storage.
279. Managed databases.
280. Managed Kubernetes.
281. AWS EKS vs GCP GKE.
282. Multi-region architecture.
283. Hybrid cloud architecture.
284. AWS + GCP active-active architecture.
285. Disaster recovery.
286. RTO vs RPO.
287. Cloud cost optimization.
288. How would you migrate an on-premise application to cloud?

---

# 16. CI/CD & Platform Engineering

289. Explain a modern CI/CD pipeline.
290. Explain GitOps.
291. Docker image optimization.
292. Jib vs Dockerfile.
293. Helm.
294. Kubernetes deployment strategies.
295. Infrastructure as Code.
296. Terraform.
297. Security scanning.
298. SAST vs DAST.
299. Dependency vulnerability scanning.
300. How do you implement automated rollback?
301. How would you design CI/CD for 100 microservices?

---

# 17. Observability

302. Logs vs metrics vs traces.
303. What should you monitor for a microservice?
304. RED methodology.
305. USE methodology.
306. Prometheus architecture.
307. Grafana.
308. OpenTelemetry.
309. Distributed tracing.
310. Correlation IDs.
311. How do you identify the root cause of a latency spike?
312. How do you troubleshoot intermittent production failures?
313. What are the most important SLOs for an API?
314. SLI vs SLO vs SLA.

---

# 18. Security

315. OAuth 2.0 architecture.
316. OpenID Connect.
317. JWT.
318. Access token vs refresh token.
319. API Gateway security.
320. mTLS.
321. Secrets management.
322. Encryption at rest vs transit.
323. RBAC.
324. Kubernetes security.
325. Zero Trust architecture.
326. How do you secure Kafka?
327. How do you secure microservice-to-microservice communication?

---

# 19. Architecture Modernization — Core Experience Area

328. Tell me about the most complex legacy modernization you have worked on.
329. Why did you migrate from SOAP to REST?
330. Why replace JMS with Kafka?
331. Why migrate Oracle to PostgreSQL?
332. What were the biggest risks during modernization?
333. How did you maintain backward compatibility?
334. How did you perform data migration?
335. How did you handle dual writes?
336. How did you validate migrated data?
337. How did you perform cutover?
338. How did you handle rollback?
339. What would you do differently if you redesigned the migration?
340. How did you measure modernization success?
341. How did you convince stakeholders to approve the modernization?
342. How did you deal with teams that wanted to keep the legacy system?

---

# 20. Architecture Governance & Stakeholder Management

343. How do you communicate architecture to non-technical stakeholders?
344. How do you handle disagreement with another architect?
345. What happens when the business rejects your preferred architecture?
346. How do you communicate technical risks?
347. How do you estimate architectural complexity?
348. How do you decide build vs buy?
349. How do you manage technical debt?
350. How do you conduct architecture review boards?
351. How do you document architecture?
352. What diagrams do you normally create?
353. Explain C4 architecture.
354. How do you create an Architecture Decision Record?
355. How do you ensure developers implement the intended architecture?
356. How do you handle architecture drift?
357. How do you mentor senior engineers?
358. How do you lead architecture without having direct authority?

---

# 21. Scenario-Based Questions

## Scenario 1 — Kafka Consumer Lag

> Your Kafka cluster is healthy, but consumer lag suddenly increases from 10K to 20 million messages. What do you do?

### Follow-ups

- How do you determine whether producer or consumer is the problem?
- What metrics do you check?
- Can you increase consumers?
- What if consumers are already equal to partition count?
- What if one partition has 90% of the traffic?
- How would you prevent this from happening again?

---

## Scenario 2 — Database Bottleneck

> Your API latency increases from 100ms to 3 seconds after traffic increases 5x.

Walk through your investigation.

### Expected Investigation Path

```text
Client
  ↓
API Gateway / Load Balancer
  ↓
Application
  ↓
Thread Pool
  ↓
Connection Pool
  ↓
Database
  ↓
Query
  ↓
Locks / I/O
  ↓
Network
```

---

## Scenario 3 — Distributed Transaction

> Order creation requires Inventory + Payment + Shipping.

Payment succeeds but Inventory fails.

### Discuss

- Saga
- Events
- Compensation
- Idempotency
- Retry
- Dead Letter Queue
- State management
- Failure recovery
- Observability

---

## Scenario 4 — Legacy Modernization

> You have a 15-year-old Java monolith with Oracle and JMS. Management wants microservices.

Design the migration strategy.

### Discuss

- Strangler Fig pattern
- Domain decomposition
- API façade
- Database migration
- JMS → Kafka
- SOAP → REST
- Incremental migration
- Dual running
- Data synchronization
- Validation
- Cutover
- Rollback
- Observability

---

## Scenario 5 — Multi-Cloud

> Your company requires AWS + GCP because of regulatory and business requirements.

Design the architecture.

### Discuss

- Kubernetes
- API Gateway
- Kafka
- Database
- DNS
- Traffic management
- Identity
- Observability
- Disaster recovery
- Data replication
- Security
- Cost

---

# 22. Leadership Questions

359. Tell me about yourself.
360. Why do you want to become a Solution Architect?
361. Why Persistent Systems?
362. Why are you leaving Amdocs?
363. What is the biggest architecture decision you have made?
364. Tell me about a failed architecture decision.
365. Tell me about a production incident you handled.
366. Tell me about a disagreement with a technical leader.
367. Tell me about a time you influenced stakeholders without authority.
368. Tell me about a difficult migration.
369. Tell me about a time you reduced system cost.
370. Tell me about a time you improved system performance.
371. Tell me about a time you mentored engineers.
372. How do you prioritize technical debt?
373. What does a good architect do differently from a senior developer?
374. What is your architecture philosophy?
375. Where do you see yourself in 3–5 years?

---

# 23. Top 15 — Must Master

If preparation time is limited, master these first:

1. Design a cloud-native microservices platform.
2. Explain your legacy → microservices modernization.
3. Explain your Oracle → PostgreSQL migration.
4. Explain JMS → Kafka migration and why Kafka was chosen.
5. Design an event-driven order/payment system.
6. Kafka architecture + failure scenarios.
7. Exactly-once vs at-least-once processing.
8. Saga + distributed transactions.
9. Microservice decomposition strategy.
10. AWS + GCP hybrid architecture.
11. Kubernetes architecture and production troubleshooting.
12. YugabyteDB architecture and when to use it.
13. Dapr + Temporal architecture and use cases.
14. Observability and production incident diagnosis.
15. Architecture trade-offs + stakeholder management.

---

# 24. Preparation Priority

| Area                                  | Priority |
| ------------------------------------- | -------: |
| Solution Architecture / System Design |    ★★★★★ |
| Distributed Systems                   |    ★★★★★ |
| Kafka                                 |    ★★★★★ |
| Microservices                         |    ★★★★★ |
| Cloud Architecture                    |    ★★★★☆ |
| Kubernetes                            |    ★★★★☆ |
| Java / Spring Boot                    |    ★★★★☆ |
| Database Architecture                 |    ★★★★☆ |
| YugabyteDB                            |    ★★★☆☆ |
| Dapr                                  |    ★★★☆☆ |
| Temporal                              |    ★★★☆☆ |
| Observability                         |    ★★★★☆ |
| Security                              |    ★★★☆☆ |
| Stakeholder / Architecture Leadership |    ★★★★★ |

---

# 25. Candidate-Specific Preparation Strategy

The strongest preparation strategy is **not** to memorize 375 isolated answers.

Build one deep architecture case study around your real modernization experience:

```text
Legacy Java Application
        │
        ├── Oracle
        ├── SOAP
        ├── JMS
        └── WebLogic
                │
                │ Modernization
                ↓
      Spring Boot Microservices
                │
        ┌───────┼────────┐
        ↓       ↓        ↓
      REST    Kafka    PostgreSQL
        │       │        │
        └───────┼────────┘
                ↓
        Kubernetes / AKS
                │
        ┌───────┼────────┐
        ↓       ↓        ↓
      CI/CD  Observability  Cloud
```

Be prepared to explain:

- Why each technology was selected
- Alternatives considered
- Architecture trade-offs
- Migration strategy
- Failure scenarios
- Scalability
- Availability
- Consistency
- Data migration
- Backward compatibility
- Security
- Observability
- Cost
- Rollback
- Stakeholder management
- Lessons learned

This single case study can support answers across **microservices, Kafka, distributed systems, databases, Kubernetes, cloud, modernization, architecture governance, and leadership**.

---

# Final Interview Focus

For this role, think like a **Solution Architect**, not only a Senior Java Engineer.

For every technical answer, try to cover:

```text
Business Requirement
        ↓
Constraints
        ↓
Architecture
        ↓
Technology Choice
        ↓
Trade-offs
        ↓
Failure Handling
        ↓
Scalability
        ↓
Security
        ↓
Observability
        ↓
Cost
        ↓
Migration / Rollout
        ↓
Business Outcome
```

The interviewer should come away believing:

> **"Vivek can take an ambiguous business problem, design the solution, explain the trade-offs, guide engineering teams, manage stakeholders, and take the architecture into production."**
