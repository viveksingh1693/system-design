 # Payment Gateway Master Questionnaire (Staff Engineer Level)

## 1. Foundations

1. What problem does a payment gateway solve?
	A payment gateway securely connects merchants, customers, banks, and payment networks to authorize, process, and manage electronic payments. It abstracts the complexity of financial networks while providing security, reliability, compliance, fraud protection, routing, and transaction lifecycle management.
	A payment gateway solves the problem of **secure and standardized communication between merchants and the financial ecosystem**.
		Financial systems are highly regulated.
		Every request must be:
		- authenticated
		- encrypted
		- PCI compliant (Payment Card Industry Data Security Standards)
		- fraud checked
		- auditable
		- routed correctly
		- **Abstraction:** Hide the complexity of diverse banks, payment methods, and protocols behind a stable interface.
		- **Reliability:** Design for retries, idempotency, failover, and graceful degradation because external financial systems are inherently unreliable.
		- **Security & Compliance:** Enforce encryption, tokenization, authentication, and regulatory requirements centrally rather than pushing them to every merchant.
		- **Scalability:** Support millions of transactions with low latency while remaining stateless where possible and carefully managing stateful financial records.
		- **Observability:** Every transaction must be traceable end-to-end with logs, metrics, and audits to diagnose failures and satisfy regulators.
		- **Evolution:** The gateway should allow new payment methods, acquiring banks, and fraud detection capabilities to be added with minimal impact on merchants.

	 A payment gateway is a secure transaction orchestration platform that sits between merchants and the financial ecosystem. It standardizes communication with banks and payment networks, authenticates merchants, validates requests, protects sensitive payment data, performs routing and fraud checks, manages the payment lifecycle, and ensures reliable, compliant, and observable payment processing. Rather than moving money itself, it coordinates the authorization, capture, settlement, and reporting processes across multiple financial institutions.

---
	
2. Who are the primary stakeholders?
	A payment gateway connects multiple independent stakeholders in the payment ecosystem. The primary stakeholders are the **Customer**, **Merchant**, **Payment Gateway**, **Payment Processor/Acquirer**, **Acquiring Bank**, **Card Network**, **Issuing Bank**, and **Regulators**. Each participant has distinct responsibilities, incentives, and constraints, and the gateway coordinates interactions among them.

                Customer
                    │
         Initiates Payment
                    │
                    ▼
               Merchant
                    │
      Payment Request (API)
                    │
                    ▼
            Payment Gateway
     (Validation, Fraud, Routing)
                    │
                    ▼
      Payment Processor / Acquirer
                    │
                    ▼
            Acquiring Bank
                    │
                    ▼
             Card Network
                    │
                    ▼
             Issuing Bank
        (Approve / Decline)
                    │
                    ▲
              Authorization
                    │
                    ▲
             Card Network
                    ▲
            Acquiring Bank
                    ▲
      Payment Processor / Acquirer
                    ▲
            Payment Gateway
                    ▲
               Merchant
                    ▲
               Customer
	A payment gateway operates in a multi-party ecosystem. The primary stakeholders are the customer, merchant, payment gateway, payment processor, acquiring bank, card network, issuing bank, and regulators. The customer initiates the payment, the merchant requests it, the gateway securely orchestrates it, the processor and acquiring bank route it, the card network connects banks, the issuing bank authorizes or declines it, and regulators define the security and compliance requirements. Understanding each stakeholder's responsibilities and incentives is essential for designing scalable and reliable payment systems.
---
3. Why can't merchants communicate directly with banks?
	 Merchants cannot communicate directly with banks because banks are not designed to expose public payment APIs to millions of businesses. Direct integration would create massive security, compliance, scalability, and operational challenges. Payment gateways and processors provide a standardized, secure, and compliant abstraction layer that connects merchants to thousands of financial institutions.
	- **Integration Abstraction:** Convert an `M × N` integration problem into `M + N`, making the ecosystem scalable.
	- **Protocol Translation:** Shield merchants from heterogeneous banking protocols and evolving payment methods.
	- **Centralized Security:** Enforce encryption, tokenization, authentication, and compliance in one place.
	- **Operational Resilience:** Provide retries, routing, failover, and observability so merchants don't have to build them.
	- **Extensibility:** Allow new banks, acquiring partners, and payment methods to be added without requiring merchant code changes.
	
		This is a classic example of introducing an intermediary to reduce coupling, centralize cross-cutting concerns, and enable independent evolution of both merchants and financial institutions.
		> Merchants don't communicate directly with banks because the banking ecosystem is fragmented, highly regulated, and operationally complex. Direct integration would require every merchant to support thousands of banks, multiple protocols, stringent security standards, fraud detection, and high availability. A payment gateway acts as a standardized abstraction layer that centralizes security, compliance, routing, protocol translation, retries, and observability, reducing the integration complexity from **M × N** to **M + N** while allowing merchants to integrate once and reach the entire payment ecosystem.

---

4. What existed before modern payment gateways?
>	Before modern payment gateways, payments evolved through cash, cheques, manual credit card processing, direct merchant–bank integrations, and payment processors. As e-commerce grew, direct integrations became difficult because every bank had different protocols and security requirements. Payment processors reduced this complexity, and modern payment gateways extended the model by providing standardized APIs, security, fraud detection, routing, retries, settlement, and compliance, allowing merchants to integrate once while supporting the entire payment ecosystem.
>---
5. What are the business goals of a payment gateway?
6. What types of payments are supported?
	A modern payment gateway supports multiple payment methods such as credit and debit cards, bank transfers, UPI, digital wallets, net banking, BNPL, recurring payments, QR code payments, and international payments. Each payment method has its own authentication flow, settlement model, and operational characteristics, but the gateway abstracts these differences behind a unified API. Internally, this is typically implemented using an orchestration layer with payment-method-specific adapters, allowing the platform to add new payment rails without impacting merchants or the core payment processing logic.
	Supporting multiple payment types means the gateway cannot hard-code a single payment flow.
	 A common design is:
				         Payment API
		                     │
		                     ▼
		            Payment Orchestrator
		                     │
		      ┌──────────────┼──────────────┐
		      ▼              ▼              ▼
		 Card Adapter    UPI Adapter   Wallet Adapter
		      ▼              ▼              ▼
		 External Network External Rail External Provider

---
8. What scale is a payment gateway designed for?
	
	Design Implications by System Dimension

| **Dimension**              | **Design Implication**                                                                                                                      |
| -------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| **Transaction Throughput** | Design stateless services that can scale horizontally to handle increasing payment volumes.                                                 |
| **Concurrent Users**       | Implement efficient connection pooling, session management, and resource utilization to support millions of simultaneous users.             |
| **Merchant Growth**        | Build scalable merchant onboarding, configuration, and payment routing services that can grow without impacting existing merchants.         |
| **Data Growth**            | Use database partitioning (sharding), archival policies, and data retention strategies to efficiently manage ever-growing transaction data. |
| **Event Throughput**       | Employ durable message brokers (e.g., Kafka) and asynchronous processing to decouple services and handle high event volumes reliably.       |
| **Geographic Expansion**   | Support multi-region deployments with regional routing, data locality, and compliance with country-specific regulations.                    |
| **Availability**           | Design for redundancy, automatic failover, disaster recovery, and eliminate single points of failure.                                       |
| **Latency**                | Optimize request paths, minimize network hops, cache frequently accessed data, and reduce dependency on slow external services.             |
>	A payment gateway is designed for internet-scale transaction processing. Depending on the business, it may process from thousands to hundreds of thousands of TPS, millions of merchants, and billions of transactions annually. Scale is not limited to request volume—it also includes concurrent users, webhook delivery, event processing, data growth, and global deployments. Architecturally, this requires stateless services, horizontal scaling, durable messaging, partitioned data storage, strong consistency for financial records, and high availability, all while maintaining low latency and exactly-once processing guarantees.

---
9. What are the key non-functional requirements?
	A payment gateway is a financial system where non-functional requirements drive the architecture. The most important requirements are high availability, reliability, consistency, durability, security, low latency, scalability, idempotency, auditability, observability, fault tolerance, and compliance. Together, they ensure payments are processed exactly once, sensitive data is protected, financial records remain accurate, and the platform continues operating despite failures while meeting regulatory obligations. At the Staff Engineer level, I would explain not only what these requirements are, but also how each one shapes the system design.
	
---
10. What happens if the payment gateway is unavailable?
	If a payment gateway is unavailable, new payment requests cannot be processed, leading to failed checkouts and lost revenue. The more challenging problem is handling in-flight transactions, where the bank may have processed the payment but the acknowledgment was lost. A production-grade payment gateway addresses this with idempotency, durable request storage, retries, reconciliation, payment status polling, and immutable transaction records. High availability is achieved through stateless gateway instances, load balancing, multi-AZ and multi-region deployments, multiple acquiring banks, circuit breakers, and automated failover. Even during an outage, read-only operations such as payment status queries and merchant dashboards should continue to function through graceful degradation.

	**What Should Continue to Work?
	
	Even if new payments are temporarily unavailable, other parts of the platform should continue operating where possible.
	Examples:
		Payment status queries
		Refund status checks
		Merchant dashboards
		Historical transaction search
		Monitoring and alerting
		Audit logs
		Internal reconciliation jobs
	
	Separating read-heavy and write-heavy services helps reduce the blast radius.

	**Architecture for High Availability

                Global Load Balancer
                        │
          ┌─────────────┴─────────────┐
          │                           │
     Region A                    Region B
          │                           │
     Load Balancer               Load Balancer
      ┌────┴────┐                ┌────┴────┐
      │         │                │         │
 Gateway 1  Gateway 2        Gateway 3  Gateway 4
      │         │                │         │
      └─────────┴────────────────┴─────────┘
                    │
             Payment Database
                    │
             Message Broker
                    │
             Banks / Networks
---

# 2. End-to-End Payment Flow

11. What happens from the moment a customer clicks "Pay"?
	When the customer clicks **Pay**, the merchant sends a request to the payment gateway. The gateway authenticates the merchant, validates the request, checks idempotency, persists a payment record, performs fraud checks, tokenizes sensitive data, and selects the appropriate acquiring bank or payment rail. It then sends an authorization request through the acquiring bank and payment network to the issuing bank, which approves or declines the transaction. The gateway updates the payment state and immediately returns the result to the merchant. After the synchronous authorization completes, asynchronous processes such as event publishing, webhooks, capture, settlement, reconciliation, notifications, and ledger updates continue the payment lifecycle. This separation keeps the customer-facing path fast while ensuring reliable downstream processing.


	Customer         : John
	Merchant         : Amazon
	Payment Gateway  : Stripe
	Acquiring Bank   : Wells Fargo
	Card Network     : Visa
	Issuing Bank     : Chase Bank


		#Step-by-Step Request & Response Flow

				┌──────────┐  
				│ Customer │  
				└────┬─────┘  
				     │  
				1. Click "Pay"  
				     │  
				     ▼  
				┌──────────┐  
				│ Amazon   │  
				└────┬─────┘  
				     │  
				     │ POST /payments  
				     │  
				     ▼  
				┌──────────────────────────────┐  
				│ Payment Gateway (Stripe)     │  
				├──────────────────────────────┤  
				│ 1. Authenticate Merchant     │  
				│ 2. Validate Request          │  
				│ 3. Idempotency Check         │  
				│ 4. Fraud Check               │  
				│ 5. Tokenize Card             │  
				│ 6. Persist Payment           │  
				│ 7. Select Acquirer           │  
				└────┬─────────────────────────┘  
				     │  
				     │ ISO8583 Authorization Request  
				     ▼  
				┌────────────────────┐  
				│ Wells Fargo        │  
				│ Acquiring Bank     │  
				└────┬───────────────┘  
				     │  
				     ▼  
				┌────────────────────┐  
				│ Visa Network       │  
				└────┬───────────────┘  
				     │  
				     ▼  
				┌────────────────────┐  
				│ Chase Bank         │  
				│ Issuing Bank       │  
				├────────────────────┤  
				│ Verify Card        │  
				│ Check Balance      │  
				│ Fraud Check        │  
				│ Reserve Funds      │  
				└────┬───────────────┘  
				     │  
				     │ APPROVED  
				     ▲  
				┌────────────────────┐  
				│ Visa Network       │  
				└────┬───────────────┘  
				     ▲  
				┌────────────────────┐  
				│ Wells Fargo        │  
				└────┬───────────────┘  
				     ▲  
				┌──────────────────────────────┐  
				│ Payment Gateway              │  
				├──────────────────────────────┤  
				│ Update State = AUTHORIZED    │  
				│ Publish Event                │  
				│ Queue Webhook                │  
				└────┬─────────────────────────┘  
				     ▲  
				     │ 200 OK  
				     │  
				┌────┴─────┐  
				│ Amazon   │  
				└────┬─────┘  
				     ▲  
				     │  
				Payment Successful  
				     │  
				┌────┴─────┐  
				│ Customer │  
				└──────────┘

---
12. How does the gateway authenticate the merchant?
	When a merchant sends a payment request, the gateway first authenticates the merchant before performing any business logic. It verifies the merchant's credentials using mechanisms such as API keys, OAuth tokens, or mutual TLS, validates the HMAC signature to ensure the request hasn't been tampered with, checks timestamps and nonces to prevent replay attacks, and verifies that the merchant is active and authorized to perform the requested operation. Only after these security checks pass does the gateway proceed with payment validation and processing. In production systems, authentication is implemented as a layered security model rather than relying on a single mechanism.
 ---
13. How is the payment request validated?
14. How is the payment routed?
15. How does the gateway communicate with acquiring banks?
16. What role do card networks play?
17. What happens inside the issuing bank?
18. What determines whether a payment is approved or declined?
19. How is the final response returned?
20. What happens after a successful payment?

---

# 3. Core Components

21. What are the major components of a payment gateway?
22. What responsibilities does each component have?
23. Which components are stateless?
24. Which components maintain state?
25. Where is payment data stored?
26. How are transaction logs maintained?
27. How is audit data stored?
28. How are merchant configurations managed?
29. How are webhooks managed?
30. How is settlement handled?

---

# 4. Data Model

31. What entities exist in the system?
32. What information is stored for each payment?
33. How are merchants represented?
34. How are customers represented?
35. How are payment methods modeled?
36. How are refunds stored?
37. How are disputes stored?
38. How are settlement records maintained?
39. How are payment attempts tracked?
40. What indexes are required?

---

# 5. APIs

41. What APIs should the gateway expose?
> A payment gateway should expose APIs that cover the complete payment lifecycle—from merchant onboarding and authentication to payment creation, authorization, capture, refunds, payouts, webhooks, reconciliation, and reporting. APIs should be **RESTful, idempotent where necessary, secure, versioned, and designed around payment resources rather than bank-specific operations.**

## API Design Principles

A production payment gateway API should be:

* Resource-oriented
* Stateless
* Versioned
* Secure
* Idempotent
* Backward compatible
* Auditable

Example:

```text
https://api.gateway.com/v1/payments
```

### High-Level API Categories

```text
                    Payment Gateway APIs
                           │
        ┌──────────────────┼───────────────────┐
        │                  │                   │
 Merchant APIs      Payment APIs        Admin APIs
        │                  │                   │
        └──────────────────┼───────────────────┘
                           │
          Settlement / Reports / Webhooks
```

41. How should payment creation work?
42. How should payment status be queried?
43. How should refunds be initiated?
44. How should cancellations work?
45. How should webhook subscriptions work?
46. How should authentication APIs work?
47. How should merchant onboarding APIs work?
48. Which APIs must be idempotent?
49. Which APIs should be asynchronous?

---

# 6. State Machine

51. What states can a payment have?
52. What transitions are allowed?
53. What happens during authorization?
54. What happens during capture?
55. What happens during settlement?
56. What happens during refunds?
57. What happens during chargebacks?
58. How are failed payments handled?
59. How are pending payments retried?
60. How do state transitions remain consistent?

---

# 7. Database Design

61. Which database should be used?
62. Why not use NoSQL for everything?
63. Which tables require ACID guarantees?
64. How should transactions be modeled?
65. How should indexes be designed?
66. Should payment records ever be updated?
67. Should event sourcing be used?
68. Should soft deletes be allowed?
69. How should historical data be archived?
70. How should backups work?

---

# 8. Distributed Systems

71. Why is idempotency critical?
72. How is an idempotency key implemented?
73. What happens if the client retries?
74. What happens if the bank retries?
75. What happens if webhooks are duplicated?
76. How is duplicate payment prevention implemented?
77. What consistency guarantees are required?
78. Which operations can be eventually consistent?
79. Where should distributed locks be used?
80. Should optimistic locking be preferred?

---

# 9. Reliability

81. How are failures detected?
82. How are retries implemented?
83. What retry strategy should be used?
84. When should retries stop?
85. How are dead-letter queues used?
86. What happens during partial failures?
87. How is timeout handling implemented?
88. How is circuit breaking implemented?
89. How is failover handled?
90. What recovery procedures exist?

---

# 10. Security

91. How are card details protected?
92. What is tokenization?
93. What is encryption at rest?
94. What is encryption in transit?
95. How are API keys secured?
96. How is merchant authentication implemented?
97. How is fraud detected?
98. How are replay attacks prevented?
99. How are webhook signatures verified?
100. What compliance requirements exist?

---

# 11. Scalability

101. How is horizontal scaling achieved?
102. Which services should remain stateless?
103. How is load balancing implemented?
104. How are database bottlenecks handled?
105. How is caching used safely?
106. What should never be cached?
107. How is partitioning implemented?
108. When is sharding required?
109. How are hot merchants handled?
110. How is multi-region deployment designed?

---

# 12. Messaging

111. Why use a message broker?
112. Which events are published?
113. Which services consume them?
114. How is ordering maintained?
115. What happens if messages are duplicated?
116. What delivery guarantees are required?
117. How are failed consumers handled?
118. How are poison messages managed?
119. Should events be immutable?
120. How should schema evolution be handled?

---

# 13. Settlement & Reconciliation

121. What is settlement?
122. What is reconciliation?
123. Why are they separate processes?
124. How is merchant payout calculated?
125. How are fees calculated?
126. How are failed settlements handled?
127. How are discrepancies detected?
128. How are missing transactions identified?
129. How are duplicate settlements prevented?
130. How is reconciliation automated?

---

# 14. Observability

131. Which metrics are critical?
132. What should be logged?
133. Which traces should be collected?
134. How are payment failures monitored?
135. Which alerts should exist?
136. How are slow bank responses detected?
137. How is merchant-specific monitoring implemented?
138. How is fraud monitoring performed?
139. How are retries monitored?
140. What operational dashboards are required?

---

# 15. Failure Scenarios

141. What happens if the client disconnects after payment?
142. What happens if the bank responds after a timeout?
143. What happens if settlement fails?
144. What happens if the webhook cannot be delivered?
145. What happens if Kafka is unavailable?
146. What happens if Redis fails?
147. What happens if the database crashes?
148. What happens if a region goes down?
149. What happens if the payment succeeds but acknowledgment is lost?
150. How is disaster recovery implemented?

---

# 16. Trade-offs

151. Why not make everything synchronous?
152. Why not use two-phase commit?
153. Why use asynchronous events?
154. Why use immutable ledgers?
155. Why separate authorization and capture?
156. Why avoid distributed transactions?
157. When is eventual consistency acceptable?
158. How do latency and consistency trade off?
159. What are the CAP theorem implications?
160. Which trade-offs matter most at scale?

---

# 17. Staff Engineer Discussion

161. How would you redesign the gateway for 100× traffic?
162. What bottlenecks would appear first?
163. How would you reduce p99 latency?
164. How would you improve availability from 99.9% to 99.99%?
165. How would you support multiple acquiring banks?
166. How would you migrate the database with zero downtime?
167. How would you support international payments?
168. How would you detect fraud in real time?
169. How would you design active-active multi-region support?
170. How would you evolve the architecture over the next five years?