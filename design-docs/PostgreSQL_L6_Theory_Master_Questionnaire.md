# PostgreSQL L6 Theory Master Questionnaire

## Purpose

This is the **theory-first PostgreSQL questionnaire** for L6 / Staff Engineer preparation.

We will use the same thinking framework used across the project: **Problem → Internals → Complexity → Failure → Tradeoffs → Production → Observability → Scale → Ownership → Interview Readiness**. The source framework explicitly organizes study around these ten dimensions. 

The goal is to master PostgreSQL concepts before commands, tuning, administration, or implementation.

---

# 1. Problem Space — 10

1. What problem does a database solve?
2. Why do we need a DBMS if we can store data in files?
3. Who experiences the problems a database solves?
4. What was used before relational databases?
5. Why was the old approach insufficient?
6. What assumptions does a relational database make?
7. What constraints does a database operate under?
8. What business metrics can a database improve?
9. What non-functional requirements matter for a database?
10. What scale was PostgreSQL designed to support?

### Deep Questions

11. Why PostgreSQL instead of a file-based system?
12. Why PostgreSQL instead of a key-value store?
13. Why PostgreSQL instead of a document database?
14. What workloads is PostgreSQL particularly good at?
15. What workloads is PostgreSQL a poor fit for?

---

# 2. Internal Working — 10

16. What are the major components of PostgreSQL?
                         PostgreSQL
                              |
          +-------------------+-------------------+
          |                   |                   |
     Connection &         Query Engine        Background
     Process Layer                             Processes
          |                   |                   |
          |            +------+-------+       +---+---+
          |            |              |       |       |
          |          Parser        Planner  WAL     Vacuum
          |            |              |
          |            +------+-------+
          |                   |
          |                Executor
          |                   |
          +-------------------+-------------------+
                              |
                       Storage Engine
                              |
          +-------------------+-------------------+
          |                   |                   |
      Buffer Manager       Heap/Tables         Indexes
          |                   |                   |
          +-------------------+-------------------+
                              |
                             WAL
                              |
                            Disk
                              |
                       Recovery/Replay
                              |
                         Replication


17. What is the end-to-end flow of a SQL query?
18. What are the major states of a PostgreSQL transaction?
19. How is relational data represented internally?
20. How are PostgreSQL components connected?
21. What important data structures does PostgreSQL use?
22. What important algorithms does PostgreSQL use?
23. What protocols are involved between client and server?
24. What synchronization happens internally?
25. What lifecycle events occur from connection creation to query completion?

### Architecture Deep Dive

26. Why does PostgreSQL have a server-process architecture?
27. What is a backend process?
28. What is shared memory?
29. What are background processes?
30. What is stored in process-local memory versus shared memory?
31. What happens after a client sends SQL?
32. Where does parsing happen?
33. Where does planning happen?
34. Where does execution happen?
35. Where does storage management happen?

---

# 3. Relational Model & Data Representation — 10

36. What is the relational model?
37. What is a relation?
38. What is a tuple?
39. What is an attribute?
40. What is a primary key?
41. What is a foreign key?
42. What are constraints?
43. Why is normalization useful?
44. When is denormalization useful?
45. How does a logical table map to physical storage?

### Storage Questions

46. What is a database page?
47. Why do databases use pages?
48. What is a heap?
49. How is a tuple represented inside a page?
50. What is a tuple identifier?
51. What is free space?
52. What happens when a page becomes full?
53. What is TOAST?
54. Why isn't every row stored as an independent file?
55. How does PostgreSQL organize table data on disk?

---

# 4. Query Processing — 10

56. What happens when PostgreSQL receives SQL?
57. What is parsing?
58. What is semantic analysis?
59. What is query rewriting?
60. What is query planning?
61. What is query optimization?
62. What is query execution?
63. What is the difference between a logical query and a physical plan?
64. Why can't PostgreSQL execute SQL directly?
65. Why does PostgreSQL need statistics?

### Planner

66. What is cardinality estimation?
67. What is selectivity?
68. What is the cost model?
69. What is a sequential scan?
70. What is an index scan?
71. What is a bitmap scan?
72. What is a nested-loop join?
73. What is a hash join?
74. What is a merge join?
75. How does PostgreSQL choose between plans?

---

# 5. Complexity — 10

76. What is the read complexity of a heap table?
77. What is the write complexity?
78. What is the update complexity?
79. What is the delete complexity?
80. What is B-tree search complexity?
81. What is index lookup complexity?
82. What is the memory complexity of major components?
83. What is the network cost of a query?
84. What is the disk I/O cost?
85. What becomes the scalability bottleneck first?

### L6 Complexity

86. When is a sequential scan cheaper than an index scan?
87. How does table size affect query behavior?
88. How does data distribution affect planning?
89. How does cardinality estimation affect performance?
90. What happens when planner estimates are wrong?

---

# 6. Transactions & Concurrency — 10

91. What is a transaction?
92. Why do databases need transactions?
93. What does atomicity mean internally?
94. What does consistency mean internally?
95. What does isolation mean internally?
96. What does durability mean internally?
97. What are isolation levels?
98. What anomalies can occur under weak isolation?
99. What is locking?
100. What is concurrency control?

### MVCC

101. Why can't every transaction simply lock every row it reads?
102. What is MVCC?
103. Why does PostgreSQL use MVCC?
104. How does MVCC allow readers and writers to run concurrently?
105. What is a transaction ID?
106. What are xmin and xmax?
107. What is a snapshot?
108. How does PostgreSQL determine tuple visibility?
109. What is a dead tuple?
110. Why does MVCC create a cleanup problem?

---

# 7. Failure Modes — 10

111. What happens if a PostgreSQL server crashes?
112. What happens if a backend process crashes?
113. What happens if the OS crashes?
114. What happens if the machine loses power?
115. What happens if the disk fails?
116. What happens if the network fails during a transaction?
117. What happens if two transactions deadlock?
118. What happens if disk space is exhausted?
119. What happens if WAL grows without bound?
120. What is PostgreSQL's recovery strategy?

### Failure Scenarios

121. What happens if the server crashes immediately after COMMIT?
122. What happens if it crashes immediately before COMMIT?
123. What happens to uncommitted changes?
124. What happens to dirty pages?
125. How does PostgreSQL know what must be recovered?
126. What happens if required WAL is missing?
127. What happens if a transaction remains open for hours?
128. What happens if autovacuum falls behind?
129. What happens when transaction IDs approach wraparound?
130. What corruption scenarios must PostgreSQL defend against?

---

# 8. WAL & Durability — 10

131. Why can't PostgreSQL simply write every change directly to table pages?
132. What is Write-Ahead Logging?
133. What problem does WAL solve?
134. What is a WAL record?
135. What is an LSN?
136. What is WAL ordering?
137. What is WAL buffering?
138. What is a checkpoint?
139. What is crash recovery?
140. How does PostgreSQL replay WAL?

### Deep WAL

141. Why must WAL become durable before corresponding data pages?
142. What is the relationship between WAL and dirty pages?
143. Why can COMMIT be acknowledged before all data pages are written?
144. What is full-page writing?
145. What happens during crash recovery?
146. What is redo?
147. What is WAL archiving?
148. What is point-in-time recovery?
149. What is the durability versus performance trade-off?
150. What becomes the bottleneck under very high WAL volume?

---

# 9. Buffer & Storage Management — 10

151. Why does PostgreSQL need a buffer manager?
152. What is shared_buffers?
153. What is a buffer page?
154. What is a dirty page?
155. When is a page loaded into memory?
156. When is a page evicted?
157. How does PostgreSQL choose pages for eviction?
158. What is the relationship between PostgreSQL's buffer cache and the OS page cache?
159. Why can double caching happen?
160. What happens when the working set is larger than memory?

### L6 Questions

161. How does cache locality affect PostgreSQL?
162. Why is random I/O different from sequential I/O?
163. How does buffer pressure affect latency?
164. Why can increasing memory fail to improve performance?
165. How do you distinguish CPU-bound, memory-bound, and I/O-bound database workloads?

---

# 10. Indexes — 10

166. Why do databases need indexes?
167. What problem does a B-tree solve?
168. Why is B-tree a strong default for relational workloads?
169. How is a B-tree structured?
170. What happens during an index lookup?
171. What happens during a page split?
172. What is index height?
173. What is index selectivity?
174. What is a composite index?
175. Why does column order matter?

### PostgreSQL Index Theory

176. What is an index-only scan?
177. What is a covering index?
178. What is a partial index?
179. What is an expression index?
180. When is B-tree a bad choice?
181. What is a Hash index?
182. What is a GIN index?
183. What is a GiST index?
184. What is a BRIN index?
185. Why does PostgreSQL support multiple index types?

---

# 11. Vacuum & MVCC Maintenance — 10

186. Why does PostgreSQL need VACUUM?
187. Why aren't old tuple versions immediately deleted?
188. What are dead tuples?
189. What is autovacuum?
190. What does autovacuum actually do?
191. What is transaction ID freezing?
192. What is transaction ID wraparound?
193. What is table bloat?
194. Why can a table grow while logical row count stays stable?
195. What happens when vacuum cannot keep up?

### L6 Questions

196. How do long-running transactions interfere with vacuum?
197. Why do high-update workloads create bloat?
198. How does vacuum affect performance?
199. What happens if autovacuum is disabled?
200. How would you diagnose a vacuum-related production problem?

---

# 12. Trade-offs — 10

201. Why use PostgreSQL instead of a simpler solution?
202. What alternatives exist?
203. Why choose PostgreSQL for OLTP?
204. What is the latency versus consistency trade-off?
205. What is the cost versus performance trade-off?
206. What is the availability versus consistency trade-off?
207. What is the memory versus speed trade-off?
208. What is the operational simplicity versus flexibility trade-off?
209. What is the developer productivity versus control trade-off?
210. What future-scaling trade-off is created by today's schema decisions?

### Compare

211. PostgreSQL vs MySQL?
212. PostgreSQL vs MongoDB?
213. PostgreSQL vs Redis?
214. PostgreSQL vs Cassandra?
215. PostgreSQL vs an embedded database?
216. PostgreSQL vs distributed SQL?
217. When should PostgreSQL NOT be used?

---

# 13. Production Thinking — 10

218. What should be separated in a production PostgreSQL architecture?
219. What supporting services are needed?
220. How is PostgreSQL health monitored?
221. How is configuration managed?
222. How are failures isolated?
223. What retry behavior should applications use?
224. What timeout behavior should applications use?
225. What fallback mechanisms exist?
226. What SLAs should a database support?
227. What SLOs should a production PostgreSQL deployment have?

### Production Architecture

228. Why is connection pooling necessary?
229. What happens when too many connections reach PostgreSQL?
230. What is PgBouncer?
231. What is session pooling?
232. What is transaction pooling?
233. Where should read/write routing happen?
234. How should database migrations be handled safely?
235. How should schema changes be performed on large tables?
236. How should backups be designed?
237. How should disaster recovery be designed?

---

# 14. Observability — 10

238. What PostgreSQL metrics matter?
239. What logs matter?
240. What query-level information matters?
241. How will debugging happen?
242. What alerts should exist?
243. What dashboards should exist?
244. What anomalies matter?
245. How do we detect hotspots?
246. How do we detect degradation?
247. How do we verify correctness?

### Production Signals

248. How do we detect connection saturation?
249. How do we detect lock contention?
250. How do we detect slow queries?
251. How do we detect cache problems?
252. How do we detect replication lag?
253. How do we detect WAL pressure?
254. How do we detect vacuum problems?
255. How do we detect disk exhaustion?
256. How do we distinguish CPU, memory, disk, and lock bottlenecks?
257. How do we prove that a performance change actually helped?

---

# 15. Replication & High Availability — 10

258. Why do we need replication?
259. Replication vs backup — what is the difference?
260. What is physical replication?
261. What is logical replication?
262. How does streaming replication work?
263. What is a WAL sender?
264. What is a WAL receiver?
265. What is replication lag?
266. What is synchronous replication?
267. What is asynchronous replication?

### HA

268. What happens when the primary fails?
269. How does failover work?
270. How does a replica become primary?
271. What is split brain?
272. What is a replication slot?
273. What happens if a replica falls far behind?
274. How do we prevent data loss during failover?
275. What is RPO?
276. What is RTO?
277. How would you design PostgreSQL HA for a payment system?

---

# 16. Scale Evolution — 10

278. How would you design PostgreSQL for one server?
279. How would you design it for 10 servers?
280. How would you design it for 100 servers?
281. What changes at very large scale?
282. How would PostgreSQL be deployed globally?
283. How would you design multi-region PostgreSQL?
284. How would disaster recovery work across regions?
285. How would you optimize database infrastructure cost?
286. How would you perform capacity planning?
287. What future growth concerns affect today's architecture?

### Scaling

288. Why doesn't replication solve write scalability?
289. When should we scale vertically?
290. When should we add read replicas?
291. When should we partition?
292. When should we shard?
293. Partitioning vs sharding — what is the difference?
294. What problems appear when data is distributed?
295. How does cross-shard querying work?
296. How do distributed transactions change the architecture?
297. When should we consider distributed SQL?

---

# 17. Ownership Thinking — 10

298. Who owns PostgreSQL in an organization?
299. Who supports it?
300. How is onboarding done?
301. What documentation should exist?
302. What runbooks should exist?
303. What technical debt accumulates?
304. What would break first under increasing load?
305. What would keep you awake at night?
306. What database work can be automated?
307. What is the long-term maintenance burden?

### Staff Engineer Ownership

308. Who owns schema design?
309. Who owns query performance?
310. Who approves indexes?
311. Who owns database reliability?
312. Who decides when to shard?
313. Who defines database SLOs?
314. Who owns backup and disaster recovery?
315. How should application teams interact with database/platform teams?
316. How do we prevent database knowledge from becoming tribal knowledge?
317. What decisions should be captured as ADRs?

---

# 18. Interview Readiness — 10

318. Can I explain PostgreSQL architecture in 2 minutes?
319. Can I draw PostgreSQL architecture from memory?
320. Can I explain the trade-offs behind its major design choices?
321. Can I explain PostgreSQL failure modes?
322. Can I explain how PostgreSQL scales?
323. Can I compare PostgreSQL with alternatives?
324. Can I discuss production concerns?
325. Can I discuss ownership concerns?
326. Can I discuss database infrastructure cost?
327. Can I redesign the database under new constraints?

---

# 19. Master Theory Questions

328. Why is PostgreSQL architected the way it is?
329. What are the major subsystems?
330. How do those subsystems interact?
331. What are the most important architectural boundaries?
332. What would break if one subsystem disappeared?
333. How does PostgreSQL turn logical rows into durable bytes?
334. How does a SELECT travel from SQL to disk?
335. How does an INSERT travel from SQL to durable storage?
336. How does an UPDATE change physical storage?
337. How does DELETE affect physical storage?
338. How can two users update the same row?
339. How does PostgreSQL decide which row version a transaction sees?
340. Why does PostgreSQL need locks if it has MVCC?
341. How are deadlocks detected and handled?
342. Why does WAL exist?
343. Why does checkpointing exist?
344. How does crash recovery reconstruct correct state?
345. What exactly does COMMIT mean?
346. Why is a query slow?
347. How does PostgreSQL choose an execution plan?
348. Why can an index make a query slower?
349. Why can the same query have different plans?
350. Why can performance degrade without application code changing?

---

# 20. Theory Mastery Rule

For every major concept, we must be able to answer:

1. What problem does it solve?
2. What happened before it?
3. Why was the previous approach insufficient?
4. What is the conceptual solution?
5. How does PostgreSQL implement it?
6. What data structures are involved?
7. What algorithms are involved?
8. What is the complexity?
9. What can fail?
10. What are the trade-offs?
11. How does it behave in production?
12. How does it scale?
13. How would I explain it to an interviewer?
14. How would I design it from scratch?

If we cannot answer these, the concept is **not mastered**.

---

# 21. Learning Order

We will answer the questionnaire in this order:

```text
1. What is a Database?
        ↓
2. Database Architecture
        ↓
3. Relational Model
        ↓
4. Storage Fundamentals
        ↓
5. Pages and Tuples
        ↓
6. Transactions
        ↓
7. Concurrency
        ↓
8. MVCC
        ↓
9. WAL
        ↓
10. Buffer Manager
        ↓
11. Indexes
        ↓
12. Query Processing
        ↓
13. Query Planner
        ↓
14. Vacuum
        ↓
15. Failure & Recovery
        ↓
16. Replication
        ↓
17. High Availability
        ↓
18. Partitioning
        ↓
19. Scaling
        ↓
20. Production Architecture
```

Only after theory is solid:

```text
Theory
  ↓
PostgreSQL Internals
  ↓
Hands-on
  ↓
Implementation
  ↓
Production Scenarios
  ↓
L6 Mock Interviews
```

---

# 22. Final Mental Model

```text
                  DATABASE
                     |
        +------------+------------+
        |                         |
     Correctness              Performance
        |                         |
   +----+----+              +-----+-----+
   |         |              |           |
Transactions MVCC        Storage      Query Engine
   |         |              |           |
Isolation   Visibility     Pages      Planner
Locks       Versions       Buffers    Executor
   |         |              |           |
   +---------+--------------+-----------+
                     |
                   WAL
                     |
                Durability
                     |
                 Recovery
                     |
               Replication
                     |
                    HA
                     |
                  Scale
```

## Staff Engineer Mental Model

**Problem → Internals → Complexity → Failure → Tradeoffs → Production → Observability → Scale → Ownership → Communication**

We will master the theory first, then move to PostgreSQL implementation details, hands-on work, and finally the "design a database from scratch" exercise.
