# MoneyGram — Senior Java Engineer Master Questionnaire

**Target:** Senior Java Engineer / 10+ years  
**Focus:** Java 17/21, Spring, distributed systems, payments, Kafka, databases, cloud, DevOps, AI, system design, and technical leadership.

## Preparation Priority

### Tier 1 — Must Master
Java 17/21 · Spring Boot · Microservices · Distributed Systems · Kafka · REST/API Design · PostgreSQL · Oracle · System Design · Payment Systems · Idempotency · Saga / Distributed Transactions · High Availability · Production Troubleshooting · Kubernetes

### Tier 2 — Strong Preparation
CQRS · DDD · Spring Cloud · Docker · CI/CD · AWS · Security · Testing / Testcontainers · Database Performance · Enterprise Integration

### Tier 3 — Differentiator
GenAI · AI Agents · RAG · Conversational AI · AI + Enterprise API Integration

---

## Java 17 / 21 — Core & Advanced

1. What are the major changes from Java 8 to Java 17?
2. What are the important changes introduced in Java 21?
3. What are records and when should you use them?
4. What are sealed classes?
5. What are pattern matching and pattern matching for switch?
6. What is the difference between final, immutable, and effectively immutable?
7. Explain equals() and hashCode() contracts.
8. Why is immutability important in distributed systems?
9. How does HashMap work internally?
10. How does ConcurrentHashMap work internally?
11. What happens when multiple threads update the same HashMap?
12. Explain Java generics and type erasure.
13. When would you use CopyOnWriteArrayList?

## Java Concurrency

14. What is the Java Memory Model?
15. What does volatile guarantee, and what does it not guarantee?
16. Explain atomicity, visibility, and ordering.
17. volatile vs AtomicInteger?
18. How does CAS work?
19. What is a race condition?
20. What is deadlock and how do you detect/prevent it?
21. synchronized vs ReentrantLock?
22. How do you choose thread-pool size?
23. What is CompletableFuture and how do you handle exceptions?
24. How would you implement backpressure?

## Spring Boot / Spring Framework

25. Explain Spring IoC and Dependency Injection.
26. How does Spring create and manage beans?
27. Explain the Spring Bean lifecycle.
28. What happens when two beans implement the same interface?
29. How does Spring AOP work?
30. JDK proxy vs CGLIB proxy?
31. Why does @Transactional sometimes not work?
32. What happens internally when a transactional method is called?
33. Explain transaction propagation and isolation levels.
34. What is Spring Boot auto-configuration?
35. How would you implement centralized exception handling?
36. How would you implement idempotency in a Spring Boot API?

## Spring Cloud

37. What problems does Spring Cloud solve?
38. What is service discovery?
39. Client-side vs server-side service discovery?
40. How would you manage configuration across environments?
41. How do you handle service-to-service communication?
42. What is a circuit breaker?
43. Why can retries make an outage worse?
44. How do you combine timeout, retry, circuit breaker, and bulkhead patterns?
45. How would you implement distributed tracing?

## Microservices Architecture

46. What makes a good microservice boundary?
47. When should you not use microservices?
48. Monolith vs modular monolith vs microservices?
49. How do you decompose a monolith?
50. What is bounded context?
51. How do you identify service boundaries?
52. Synchronous vs asynchronous communication?
53. How do you prevent cascading failures?
54. How do you handle distributed transactions?
55. What is eventual consistency?
56. How do you handle schema evolution and API backward compatibility?
57. How do you perform zero-downtime deployments?

## Distributed Systems

58. What makes a system distributed?
59. What are the biggest challenges in distributed systems?
60. Explain CAP theorem.
61. Strong consistency vs eventual consistency?
62. What is idempotency and why is it important in payment systems?
63. What happens if a request succeeds but the response is lost?
64. How do you safely retry such a request?
65. At-most-once vs at-least-once vs effectively-once processing?
66. How do you handle duplicate and out-of-order messages?
67. How do you design for partial failures?
68. How would you design a highly available service across multiple regions?

## CQRS / Saga / DDD

69. What is CQRS and when is it useful?
70. How do you maintain consistency between CQRS read and write models?
71. Why can't a normal database transaction span multiple microservices?
72. What is the Saga pattern?
73. Choreography vs orchestration?
74. What are compensating transactions?
75. How do you make Saga steps idempotent?
76. How do you recover an interrupted Saga?
77. What is Domain-Driven Design?
78. What is a bounded context?
79. Entity vs Value Object?
80. Aggregate vs Entity and what is an Aggregate Root?
81. How would you model a payment domain using DDD?

## REST APIs & Enterprise Integration

82. What makes a good REST API?
83. PUT vs POST vs PATCH?
84. What makes an HTTP operation idempotent?
85. How should HTTP status codes be used?
86. Offset vs cursor pagination?
87. How would you design API versioning?
88. How would you implement rate limiting?
89. How would you secure APIs?
90. OAuth2 vs JWT?
91. How would you design an API Gateway?
92. What responsibilities should and should not belong in an API Gateway?
93. How would you handle third-party API failures?
94. What are enterprise integration patterns?
95. How would you migrate SOAP to REST?
96. How do you guarantee that a transaction is not accidentally processed twice?

## Kafka — Deep Dive

97. What problem does Kafka solve?
98. Explain Kafka architecture.
99. What are topics, partitions, offsets, brokers, replicas, leaders, and ISR?
100. What is a consumer group?
101. How does Kafka maintain ordering?
102. What happens when a consumer crashes?
103. How does consumer rebalancing work?
104. What is consumer lag?
105. How would you handle a hot partition?
106. How would you increase Kafka throughput?
107. What is the difference between acks=0, acks=1, and acks=all?
108. What is an idempotent Kafka producer?
109. What are Kafka transactions?
110. How do you handle duplicate or poison messages?
111. What is a Dead Letter Topic?
112. How do you evolve Kafka schemas?
113. How would you guarantee ordering for payment transactions?

## Databases — PostgreSQL / Oracle / NoSQL

114. How does PostgreSQL MVCC work?
115. Explain ACID and transaction isolation levels.
116. What causes database deadlocks?
117. How do you diagnose slow SQL?
118. What is an execution plan?
119. How do indexes work?
120. B-tree vs Hash index?
121. How do you design composite indexes?
122. When can an index hurt performance?
123. How would you optimize a slow query?
124. What is connection pooling and how does HikariCP work?
125. How do you size a connection pool?
126. How do you handle database failures?
127. Read replicas vs primary database?
128. When would you choose Couchbase over PostgreSQL?
129. How do you model data in a document database?

## Payment Systems — Critical

130. What happens when a user initiates an international money transfer?
131. Design a money-transfer system.
132. What are the major components of a payment platform?
133. How do you ensure payment idempotency?
134. What happens if the payment provider times out?
135. What happens if the debit succeeds but credit fails?
136. How do you reconcile transactions?
137. What is a payment ledger?
138. Why should a payment ledger be immutable?
139. How would you model transaction states?
140. What happens if a transaction gets stuck in PROCESSING?
141. How do you recover stuck transactions?
142. How do you prevent double debit?
143. How do you handle duplicate payment requests?
144. How would you design transaction reconciliation?
145. What is settlement?
146. What is clearing?
147. How would you design a transaction audit trail?
148. How would you guarantee financial correctness when distributed services fail?

## Security

149. Authentication vs authorization?
150. OAuth2 and JWT?
151. How would you securely store secrets?
152. How do you encrypt data at rest and in transit?
153. How do you protect sensitive financial information?
154. How would you implement service-to-service authentication?
155. What is mTLS?
156. How would you prevent replay attacks?
157. How would you protect against SQL injection?
158. How would you protect APIs against abuse?
159. How would you audit financial transactions?

## AWS / Cloud

160. Explain AWS core services relevant to backend systems.
161. EC2 vs ECS vs EKS?
162. What are API Gateway, ALB, Route 53, S3, RDS, DynamoDB, SQS, SNS, and MSK used for?
163. How would you deploy a Spring Boot application on AWS?
164. How would you design a highly available application on AWS?
165. How would you secure an AWS application?
166. How would you implement CI/CD on AWS?
167. Azure AKS to AWS EKS: what changes and what remains the same?

## Docker / Kubernetes / DevOps

168. What problem does Docker solve?
169. Container vs VM?
170. How do you optimize a Docker image?
171. What are Pod, Deployment, and Service in Kubernetes?
172. ConfigMap vs Secret?
173. Liveness vs readiness probe?
174. What happens when a pod crashes?
175. How does Kubernetes perform service discovery?
176. What is horizontal pod autoscaling?
177. How do you troubleshoot a crashing pod?
178. How do you perform zero-downtime deployment?
179. Rolling deployment vs blue-green deployment?
180. What makes a good CI/CD pipeline?
181. How do you rollback a deployment?

## Testing

182. Unit test vs integration test?
183. JUnit 5 architecture?
184. Mockito: Mock vs Spy?
185. What should you mock and what should you not mock?
186. What is Testcontainers?
187. How would you test Kafka consumers?
188. How would you test database interactions?
189. How would you test distributed workflows?
190. How would you test retry and failure scenarios?
191. How would you test idempotency?
192. What is contract testing?

## AI / GenAI

193. What is an LLM?
194. What is Generative AI?
195. How does an LLM-based application work?
196. What is RAG?
197. Why use RAG instead of fine-tuning?
198. What is an embedding?
199. How does semantic search work?
200. What is an AI Agent?
201. Agent vs chatbot?
202. What is tool calling?
203. How would you integrate an LLM into a Spring Boot application?
204. How would you expose enterprise APIs to an AI agent?
205. How do you secure AI agents?
206. How do you prevent prompt injection?
207. How do you handle hallucinations?
208. How would you build a customer-support chatbot for MoneyGram?
209. How would you build a Voicebot backed by Java microservices?
210. How would you ensure an AI agent cannot accidentally initiate an unauthorized financial transaction?

## System Design — MoneyGram

211. Design a money transfer system end-to-end.
212. Design an international payment platform supporting multiple countries and currencies.
213. Design an immutable payment ledger.
214. Design a transaction reconciliation system.
215. Design a notification platform for transaction status changes.
216. Design a real-time fraud detection system.
217. Design a conversational money-transfer assistant.
218. Design a global money-transfer platform for 50M+ customers/year across 200+ countries.

## Production Troubleshooting

219. API latency increases from 100 ms to 3 seconds. How do you debug it?
220. Kafka consumer lag suddenly increases. What do you investigate?
221. Database CPU reaches 100%. What do you do?
222. Connection pool is exhausted. How do you diagnose it?
223. Kubernetes pods continuously restart. How do you investigate?
224. One microservice starts returning 5xx errors. What do you investigate?
225. A downstream payment provider becomes unavailable. What should happen?
226. Transactions are being duplicated. How do you diagnose and fix it?
227. Transactions are stuck in processing. How do you recover them?
228. Kafka messages are arriving out of order. How do you handle it?
229. How would you distinguish an application problem from an infrastructure problem?

## Leadership / Behavioral

230. Tell me about yourself.
231. Tell me about the most complex system you have designed.
232. Tell me about a legacy system you modernized.
233. Tell me about a major production incident you handled.
234. Tell me about a time you made a decision with incomplete information.
235. Tell me about a technical decision you disagreed with.
236. Tell me about a time you influenced an architect or senior engineer.
237. Tell me about a time you mentored another engineer.
238. Tell me about a time you made a mistake.
239. Tell me about a project that failed.
240. Tell me about a time you improved system reliability.
241. Tell me about a time you improved engineering productivity.
242. Tell me about a time you balanced technical debt against business priorities.
243. Why MoneyGram?
244. Why payments/fintech?
245. Why are you looking for a change?
246. Why should we hire you?
247. What does technical ownership mean to you?
248. How do you operate when requirements are ambiguous?

## Interview Strategy

For each question, prepare at four levels:

1. **Concept** — define it clearly.
2. **Internals** — explain how it works.
3. **Production** — explain failure modes, observability, and operational trade-offs.
4. **L6/Senior depth** — explain design decisions, alternatives, trade-offs, and business impact.

For system-design questions, always cover:

- Requirements and assumptions
- Scale / QPS / data volume
- APIs
- Data model
- Service boundaries
- Synchronous vs asynchronous communication
- Kafka / messaging
- Consistency and transactions
- Idempotency
- Failure handling
- Scalability
- Availability
- Security
- Observability
- Deployment / rollback
- Trade-offs
