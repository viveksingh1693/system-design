# Kafka — L6 / Staff Engineer Master Questionnaire

> **Goal:** Master Kafka for Staff Engineer / L6 System Design interviews through progressive depth: fundamentals → internals → distributed systems → failure → trade-offs → production → scale → interview scenarios.

This questionnaire is based on the Staff Engineer Master Thinking Framework:
**Problem → Internals → Complexity → Failure → Tradeoffs → Production → Observability → Scale → Ownership → Communication.**

---

# Mastery Strategy

We will master Kafka in this order:

1. Topic
2. Partition
3. Offset
4. Producer
5. Partitioner
6. Broker
7. Leader / Follower
8. ISR
9. Replication
10. Consumer
11. Consumer Group
12. Consumer Lag
13. Rebalancing
14. Delivery Semantics
15. KRaft / Metadata Management

For each concept, study it through the same Staff Engineer lens rather than memorizing isolated facts.

---

# Level 1 — Kafka Fundamentals

## A. Problem Space

1. What problem does Kafka solve?
Kafka solves the distributed systems problem of reliably transporting and retaining high-volume event streams while decoupling producers from consumers. It provides durable storage, partition-based parallelism, consumer-controlled progress, replayability, and replication, allowing independent systems to process the same stream at different rates without requiring synchronous communication between them.
> "Kafka is a message broker."
> The core abstraction is: Distributed, durable, append-only event log.
---

2. Why was Kafka created?
Kafka was originally created at LinkedIn because the existing systems for moving data between applications and data systems couldn't efficiently handle the scale, throughput, and fan-out they needed. LinkedIn needed a durable, scalable event pipeline where many independent consumers could consume the same stream at their own pace and replay historical data when necessary. Kafka addressed this by introducing a distributed, partitioned, replicated append-only log rather than a traditional transient message queue.
---

3. What problems existed with traditional message queues?
4. Why is Kafka called a distributed log?
5. How is Kafka different from a traditional queue?
6. Why does Kafka retain messages after consumption?
7. Who should use Kafka?
8. Who should not use Kafka?
9. What assumptions does Kafka make?
10. What are Kafka's fundamental design goals?

## B. Architecture

11. What are Kafka's major components?

                         Kafka Cluster
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
      Producers            Brokers           Consumers
          │                   │                   │
          │              ┌────┴────┐              │
          │              │ Topics  │              │
          │              │    │     │              │
          │              │Partitions│             │
          │              │    │     │              │
          │              │ Replicas │              │
          │              └─────────┘              │
          │                   │                   │
          └───────────────────┼───────────────────┘
                              │
                       Metadata / KRaft
                       
---
12. What is a topic?
	A Kafka topic is a logical, named stream of events. It is divided into one or more partitions, where each partition is an ordered append-only log. Producers publish records to topics, and consumers read them through consumer groups. The topic itself provides logical organization, while partitions provide ordering, parallelism, and scalability. Unlike a traditional queue, records aren't deleted when consumed; they're retained according to the topic's retention policy, which enables independent consumers and replay.
---
	
13. What is a partition?
	A partition is Kafka's fundamental unit of storage, ordering, and parallelism. It's an ordered, append-only log where each record receives an offset. A topic is divided into multiple partitions so that partitions can be distributed across brokers and processed concurrently by consumers. Kafka guarantees ordering within a partition, not across partitions. Partitions can also be replicated across brokers for fault tolerance. The key architectural trade-off is that more partitions increase parallelism and throughput but also increase metadata, replication, and operational overhead.
---

14. What is a broker? 
		                Kafka Cluster
		       ┌──────────┼──────────┐
		       ▼          ▼          ▼
		   Broker 1    Broker 2    Broker 3
		       │          │          │
		      P0         P1         P2
		      P3         P4         P5

              Kafka
                │
       ┌────────┴────────┐
       │                 │
   Control Plane      Data Plane
       │                 │
     KRaft             Brokers
                         │
              ┌──────────┼──────────┐
              ▼          ▼          ▼
          Producers   Partitions  Consumers
      
	Broker provides the machine capacity; partition provides the unit of data, ordering, and parallelism.
	The Kafka control plane manages the leadership transition. Producers and consumers refresh their metadata and start communicating with the new leader. This is why replication and leader election are critical to Kafka availability.

A Kafka broker is a server in a Kafka cluster that stores partition replicas and serves producer and consumer requests. Brokers form the data plane: they handle writes, reads, storage, and replication. Partitions are distributed across brokers, and each partition has a leader replica that handles normal traffic while follower replicas provide fault tolerance. If a broker fails, leadership for affected partitions can move to eligible replicas. At scale, broker capacity is constrained by disk, network, CPU, memory, and partition count, so broker sizing and partition placement are important architectural concerns.

---
15. What is a producer?

	A Kafka producer is a client responsible for publishing records to Kafka topics. It serializes records, determines the target partition using the partitioning strategy, batches and optionally compresses records, discovers the partition leader through metadata, and sends produce requests to that broker. It handles acknowledgements, retries, timeouts, and can use idempotent production to prevent duplicate appends caused by retries. At scale, producer design is primarily about balancing throughput, latency, durability, memory usage, and backpressure.

	batch.size: Approximate target size for a producer batch.
	linger.ms: How long the producer can wait for additional records before sending the batch.
	acks=0 Producer doesn't wait for an acknowledgement.
	acks=1 The leader acknowledges the write.
	acks=all The leader waits for the required in-sync replicas according to Kafka's replication configuration.
	
	Idempotent producer protects Kafka's append operation against producer retries; it does not magically make an entire business workflow exactly-once.
---


16. What is a consumer?
	- Why is pull useful? 
			Because the consumer controls its processing rate. If the downstream database is slow: The consumer doesn't have to accept records faster than it can handle them.
	 - Kafka internally stores consumer-group offset information in:  **__consumer_offsets**
	- Within a consumer group, a partition is actively assigned to at most one consumer at a time. Consumer parallelism ≤ Partition count
	- **Consumer Lag**  Lag tells us whether consumers are keeping up with producers.
		Suppose Kafka has: Latest offset = 1,000,000
		the consumer has processed: Committed offset = 900,000
		 Then approximately: Lag ≈ 100,000 records
						Kafka
			─────────────────────────────────────────────>
			                         ↑              ↑
			                    Consumer         Latest
			                    progress         record
			
			                         ←── Lag ──→
	 - Kafka's consumer offset and your business transaction are two different state machines unless you deliberately make them atomic.
	A Kafka consumer is a client application that pulls records from Kafka partitions and processes them. Consumers typically operate within consumer groups, where partitions are distributed among consumers for parallelism and fault tolerance. A consumer tracks its position using offsets and commits progress so it can recover after failures. The important design considerations are processing rate, consumer lag, rebalancing, offset semantics, backpressure, and idempotency because a crash between business processing and offset commit can cause duplicate processing.
---

17. What is a consumer group?
                         Kafka Topic
                              │
                ┌─────────────┼─────────────┐
                ▼             ▼             ▼
               P0            P1            P2
                │             │             │
                └─────────────┼─────────────┘
                              │
                              ▼
                     Consumer Group
                              │
                 ┌────────────┼────────────┐
                 ▼            ▼            ▼
              Consumer 1   Consumer 2   Consumer 3
                 │            │            │
              Assigned       Assigned     Assigned
                 │            │            │
                 ▼            ▼            ▼
                P0           P1           P2
                 │            │            │
                 ▼            ▼            ▼
              Process       Process      Process
                 │            │            │
                 └────────────┼────────────┘
                              ▼
                       Commit Offsets
                              │
                              ▼
                    __consumer_offsets


                         orders
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
       Payment Group   Fraud Group   Analytics Group
              │             │             │
          ┌───┴───┐     ┌───┴───┐     ┌───┴───┐
          ▼       ▼     ▼       ▼     ▼       ▼
         C1      C2    C3      C4    C5      C6

	The groups don't share their consumption progress.
	**Consumer = worker**
	Consumer group = team of workers sharing the workload
	Consumer groups provide parallelism, but partitioning determines ordering.
	Scale based on the bottleneck, not merely on Kafka partition count.

	A consumer group is a logical set of consumers that cooperate to process a Kafka topic. Kafka assigns each partition to at most one consumer within the group at a time, which provides parallel processing and horizontal scalability. If there are more consumers than partitions, some consumers remain idle. Each consumer group maintains independent offsets, so multiple groups can consume the same topic independently and at different speeds. Group membership changes trigger rebalancing, which provides fault tolerance but can introduce processing disruption if it happens frequently.

	Committed offsets are maintained per consumer group and partition, not as permanent state on an individual consumer. A consumer is simply the current worker assigned to a partition. If that consumer fails, another consumer in the group can take ownership and resume from the group's committed offset. If a partition is hot, however, adding consumers doesn't help because only one consumer in the group can actively consume that partition. We first determine whether the problem is slow processing or uneven partitioning. For a hot key, we may increase partitions, change the partitioning strategy, shard the hot key, or isolate heavy tenants. But sharding a key can sacrifice per-key ordering, so at L6 level I'd explicitly establish the required ordering scope before choosing the solution.
---
18. What is an offset?
		(topic, partition, offset)
		Consumer Group + Topic + Partition
		The consumer's current processing position and its committed offset are not necessarily the same.
		This is why Kafka consumers commonly operate with **at-least-once processing** and require idempotency when duplicate business processing is possible.
		"Exactly once" is not simply a property you get by committing offsets differently. You need to reason about the entire processing pipeline.
		
		
		Kafka Partition P0
		
		100      101      102      103      104      105
		 |        |        |        |        |        |
		 A        B        C        D        E        F
		                   ↑
		                   │
		            Committed Offset
		                   
		                              ↑
		                              │
		                       Current processing
		                       
		                                                ↑
		                                                │
		                                         Latest record
			An offset is the monotonically increasing position of a record within a Kafka partition. It provides ordering and allows consumers to track their position in the log. Offsets are scoped to a partition, so a record is identified by topic, partition, and offset. A consumer group's committed offset represents its persisted progress for a particular partition and is stored in Kafka's internal offset topic. This allows another consumer to resume after a rebalance or failure. The key design issue is that business processing and offset commitment are separate operations, so committing after processing gives at-least-once semantics and can result in duplicate processing unless the business operation is idempotent.
---

19. What is a replica?
	Kafka has RF=3. One broker fails. Is the data still safe?
	Potentially yes, assuming the partition has at least one healthy in-sync replica available and the relevant durability configuration was satisfied. The failed broker's partition leaders can be moved to eligible replicas. However, I would check ISR state, under-replicated partitions, acknowledgement configuration, and whether we have lost an entire failure domain. RF=3 by itself doesn't guarantee safety if all replicas are placed in the same failure domain or if replicas were not sufficiently in sync.
		Replication factor and failure-domain-aware placement must be considered together.
	                      Producer
                        │
                        ▼
                  Partition Leader
                        │
                 Append Record
                        │
             ┌──────────┴──────────┐
             ▼                     ▼
        Follower 1            Follower 2
             │                     │
             └──────────┬──────────┘
                        ▼
                       ISR
                        │
                        ▼
                  Required ACK
                        │
                        ▼
                     Producer
                                 KAFKA CLUSTER
                              │
             ┌────────────────┼────────────────┐
             ▼                ▼                ▼
          Broker 1         Broker 2         Broker 3
             │                │                │
             └────────────────┼────────────────┘
                              │
                            Topic
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
             P0              P1              P2
              │               │               │
        ┌─────┼─────┐   ┌─────┼─────┐   ┌─────┼─────┐
        ▼     ▼     ▼   ▼     ▼     ▼   ▼     ▼     ▼
       B1    B2    B3  B2    B1    B3  B3    B2    B1
       L     F     F   L     F     F   L     F     F


		Partition
		    │
		    ├── Leader
		    ├── Followers
		    ├── ISR
		    └── Offsets
		             │
		             ▼
		       Consumer Group
		             │
		       ┌─────┼─────┐
		       ▼     ▼     ▼
		      C1    C2    C3


	A Kafka replica is a copy of a partition stored on another broker. The replication factor determines how many total copies exist, including the leader. The leader handles normal partition traffic while followers replicate its log. Kafka tracks in-sync replicas, or ISR, and if the leader fails, an eligible ISR replica can become the new leader. Replication improves durability and availability but increases storage and network cost, so at scale we also need to consider replica placement across failure domains and recovery traffic.
---

20. What is a leader?
	Leader is not a separate type of server. It is a role played by one replica of a partition.
	|Role|Responsibility|
	|---|---|
	|**Leader**|Handles normal partition traffic and establishes log order|
	|**Follower**|Replicates the leader's log|
	|**Replica**|Generic term for either leader or follower|
	|**ISR**|Replicas sufficiently caught up with the leader|

	Producer
	    │
	    ▼
	Leader
	    │
	    ├──→ Follower 1 ✓
	    └──→ Follower 2 ✓
	             │
	             ▼
	            ACK
"The leader broker for 1,000 partitions dies. What happens?"

Broker failure
      │
      ▼
1,000 affected partition leaders
      │
      ▼
Eligible ISR replicas identified
      │
      ▼
New leaders elected
      │
      ▼
Clients refresh metadata
      │
      ▼
Traffic resumes


Leader election storm
        +
Metadata propagation
        +
Producer retries
        +
Consumer fetch retries
        +
Under-replicated partitions
        +
Recovery traffic

#### Complete Leader Diagram
                           KAFKA CLUSTER
                                │
                 ┌──────────────┼──────────────┐
                 ▼              ▼              ▼
              Broker 1       Broker 2       Broker 3
                 │              │              │
                 ▼              ▼              ▼
                P0             P0             P0
              LEADER         FOLLOWER       FOLLOWER
                 │              │              │
                 │──────────────►│              │
                 │───────────────┼─────────────►│
                 │              Replication     │
                 │
                 ▼
             Producer
                 │
                 ▼
              Consumer
  
  
  #### Complete Failure Diagram
                    BEFORE FAILURE

				P0
				│
				├── Broker 1 → LEADER
				├── Broker 2 → FOLLOWER
				└── Broker 3 → FOLLOWER
				          │
				          │
				          ▼
				      Broker 1 ❌
				          │
				          ▼
				   Kafka Controller
				          │
				          ▼
				   Select eligible ISR
				          │
				          ▼
				   Broker 2 → NEW LEADER
				          │
				          ├── Broker 3 → FOLLOWER
				          │
				          ▼
				   Producer / Consumer
				   refresh metadata
				          │
				          ▼
				      Traffic resumes


A Kafka partition leader is the replica responsible for serving normal traffic for a partition and establishing the order of writes to that partition. Producers send writes to the leader, and consumers normally fetch from the leader, while follower replicas replicate its log. The leader is a role of a partition replica, not a separate server, so the same broker can lead some partitions and follow others. If the leader's broker fails, Kafka can elect an eligible in-sync replica as the new leader. At scale, leader distribution matters because a hot partition creates a hot leader, and simply moving the leader doesn't solve the underlying hot-partition problem.

---
## C. End-to-End

21. What happens when an application publishes an event?
22. How does the producer find the correct broker?
23. How is a partition selected?
24. How is the message stored?
25. How is the message replicated?
26. How does a consumer discover its partition?
27. How does the consumer read the event?
28. How does Kafka know where the consumer stopped?
29. What happens if the consumer crashes?
30. What happens if the broker crashes?

---

# 1. TOPIC

## Fundamentals

1. What is a Kafka topic?
2. Why does Kafka need topics?
3. Is a topic physically stored as one file?
4. Can a topic have multiple partitions?
5. Can different topics have different configurations?
6. Can multiple consumer groups consume the same topic?
7. Can multiple consumers within one group consume the same partition?
8. What happens if a topic has one partition?
9. What happens if a topic has 1,000 partitions?
10. How should topics be named?

## Design

11. How do you decide whether two events belong in the same topic?
12. Should `OrderCreated` and `OrderCancelled` be separate topics?
13. When would you create separate topics for different consumers?
14. Should every microservice have its own topic?
15. How should topic ownership work?

## Retention

16. How does Kafka retention work?
17. What is time-based retention?
18. What is size-based retention?
19. What happens when retention expires?
20. Does deleting a message require deleting an individual record?

## Staff-Level Questions

21. How would you design topic governance for 10,000 teams?
22. How do you prevent topic proliferation?
23. How would you classify critical vs non-critical topics?
24. How would you migrate a topic without downtime?
25. How would you design topic-level isolation?

---

# 2. PARTITION

> **Key idea:** Partitioning is one of the most important Kafka concepts for system design because partitions provide ordered logs and the primary unit of parallelism.

## Fundamentals

1. What is a partition?
2. Why does Kafka partition topics?
3. Why can't Kafka simply have one global log?
4. What ordering guarantee does a partition provide?
5. Is ordering guaranteed across partitions?
6. Why is partition the unit of parallelism?
7. Can a partition exist on multiple brokers?
8. What is a partition leader?
9. What are partition replicas?
10. What happens when a partition leader fails?

## Partitioning

11. How is a message assigned to a partition?
12. What happens if the producer specifies a key?
13. What happens if no key is provided?
14. How does hashing affect partition selection?
15. What happens when the number of partitions changes?
16. Can increasing partitions affect ordering?
17. Can partitions be decreased?
18. How do you choose the initial partition count?

## Hot Partition

19. What is a hot partition?
20. What causes hot partitions?
21. How would you detect one?
22. How would you fix one?
23. What happens if one customer generates 50% of all traffic?
24. Can adding more partitions solve a hot key?
25. When would key salting be appropriate?

## Staff-Level Scenarios

26. You need strict ordering per customer and 10M events/sec. Design the partitioning strategy.
27. One customer becomes 100× larger than everyone else. What changes?
28. You started with 20 partitions and now need 2,000. How do you migrate?
29. How do partitions affect broker capacity?
30. How do partitions affect consumer scalability?

---

# 3. OFFSET

## Fundamentals

1. What is an offset?
2. Why does Kafka need offsets?
3. Is an offset globally unique?
4. Is an offset unique across partitions?
5. Does offset represent time?
6. Can offsets change?
7. Why is offset ordering important?
8. What is the latest offset?
9. What is the committed offset?
10. What is consumer position?

## Consumer Recovery

11. What happens when a consumer crashes?
12. How does it resume processing?
13. What happens if the consumer commits too early?
14. What happens if the consumer commits too late?
15. What is duplicate processing?
16. What is message loss caused by incorrect offset handling?

## Staff-Level

17. Explain the difference between latest offset, consumer position, committed offset, and consumer lag.
18. Design offset management for a payment processor.
19. How would you recover from a corrupted consumer offset?
20. How would you replay events from 24 hours ago?

---

# 4. PRODUCER

## Fundamentals

1. What is a Kafka producer?
2. How does a producer discover brokers?
3. Does the producer send every message to any broker?
4. How does the producer know the partition leader?
5. What is batching?
6. What is compression?
7. Why is batching important?
8. What is `acks=0`?
9. What is `acks=1`?
10. What is `acks=all`?

## Reliability

11. What happens when a produce request fails?
12. How does the producer retry?
13. What causes duplicate messages?
14. What is producer idempotence?
15. How does idempotent production help?
16. What is a producer sequence number?
17. What happens when the broker becomes unavailable?
18. How should retry backoff work?

## Performance

19. How do you increase producer throughput?
20. What is `batch.size`?
21. What is `linger.ms`?
22. How does compression affect performance?
23. What is the trade-off between batching and latency?
24. How do you handle a 10 GB/sec producer workload?

## Staff-Level

25. Design a producer for 5M events/sec.
26. How would you protect Kafka from a producer traffic spike?
27. What happens if producers retry faster than Kafka can recover?
28. How do you design producer backpressure?
29. How do you guarantee that a payment event isn't silently lost?

---

# 5. PARTITIONER

## Fundamentals

1. What is a partitioner?
2. Why does the producer need one?
3. How does key-based partitioning work?
4. Why does the key matter for ordering?
5. What happens when the key is null?
6. What is round-robin distribution?
7. What is sticky partitioning?
8. When should you use a custom partitioner?

## Design Questions

9. Why would `customerId` be a good key?
10. Why could `country` be a bad key?
11. Why could `merchantId` create a hot partition?
12. How would you partition payment events?
13. How would you partition IoT events?
14. How would you partition log events?

## Staff-Level

15. Design a partitioning strategy for 10M events/sec, 100M customers, and strict ordering per customer.
16. What happens when one customer becomes extremely hot?
17. Can you preserve ordering while distributing a hot key?
18. What business decisions should influence partition-key selection?

---

# 6. BROKER

## Fundamentals

1. What is a Kafka broker?
2. What does a broker store?
3. Can one broker store many topics?
4. Can one broker contain multiple partitions?
5. What happens when a broker crashes?
6. How does a broker serve producers?
7. How does a broker serve consumers?
8. What is broker metadata?
9. What is broker capacity?
10. What limits broker throughput?

## Storage

11. How does Kafka store records on disk?
12. What is an append-only log?
13. What is a log segment?
14. Why does Kafka use segments?
15. What indexes exist?
16. Why is sequential I/O important?
17. How does the OS page cache help Kafka?

## Staff-Level

18. How would you size a broker?
19. How many partitions can one broker safely handle?
20. What happens when disk reaches 90%?
21. What happens when network bandwidth is saturated?
22. How do you rebalance partitions across brokers?
23. How do you replace a failed broker?

---

# 7. LEADER / FOLLOWER

## Fundamentals

1. What is a partition leader?
2. Why does every partition need a leader?
3. What does the leader do?
4. What do followers do?
5. Can producers write directly to followers?
6. Can consumers read from followers?
7. What happens when the leader fails?
8. How is a new leader selected?

## Failure

9. What is leader election?
10. What is an unclean leader election?
11. Why can unclean leader election cause data loss?
12. What happens during a network partition?
13. What happens when the old leader returns?

## Staff-Level

14. Explain leader election during a broker failure.
15. How do you minimize leader-election impact?
16. How do you distribute leadership evenly?
17. What happens if one broker becomes leader for too many partitions?
18. How would you design failure-domain-aware replica placement?

---

# 8. ISR — IN-SYNC REPLICAS

## Fundamentals

1. What is ISR?
2. Why does Kafka need ISR?
3. When does a replica enter ISR?
4. When does a replica leave ISR?
5. What happens when ISR shrinks?
6. What happens if only one replica remains in ISR?
7. How does ISR relate to durability?
8. How does `min.insync.replicas` work?

## Deep Dive

9. Explain `Replication Factor = 3` and `ISR = {B1, B2, B3}`.
10. B2 fails. What happens?
11. B3 becomes slow. What happens?
12. Only B1 remains in ISR. What happens with `acks=all`?
13. Can Kafka continue accepting writes?
14. What happens when B2 catches up?

## Staff-Level

15. Design durability guarantees for financial transactions.
16. What is your preferred RF and why?
17. How would you balance durability against availability?
18. What would you do if ISR repeatedly shrinks?
19. How would you detect replication degradation?

---

# 9. REPLICATION

## Fundamentals

1. Why does Kafka replicate data?
2. What is replication factor?
3. What does RF=3 mean?
4. Does replication mean three independent copies?
5. Is replication synchronous or asynchronous?
6. What does `acks=all` mean?
7. What is replica lag?
8. What is under-replication?

## Failure Scenarios

9. One broker fails.
10. Two brokers fail.
11. Leader fails.
12. Follower fails.
13. Network partition occurs.
14. Disk corruption occurs.
15. Entire rack fails.
16. Entire availability zone fails.

## Staff-Level

17. Design Kafka across three availability zones.
18. Where should replicas be placed?
19. How do you avoid correlated failures?
20. What RPO does your design provide?
21. How would you recover from a multi-broker failure?
22. How would you design cross-region replication?

---

# 10. CONSUMER

## Fundamentals

1. What is a Kafka consumer?
2. How does it find partitions?
3. How does it fetch messages?
4. Does the broker push messages to consumers?
5. Why does Kafka use pull-based consumption?
6. What is `poll()`?
7. How does a consumer maintain position?
8. How does a consumer commit offsets?

## Processing

9. What happens if processing takes 5 minutes?
10. What happens if the consumer crashes during processing?
11. What happens if the database write succeeds but offset commit fails?
12. What happens if offset commit succeeds but database write fails?

This leads directly into at-least-once processing and idempotency.

## Staff-Level

13. Design a consumer processing 1M events/sec.
14. How would you handle downstream database slowness?
15. How would you implement backpressure?
16. How would you prevent consumer OOM?
17. How would you replay historical events safely?

---

# 11. CONSUMER GROUP

## Fundamentals

1. What is a consumer group?
2. Why do consumer groups exist?
3. How are partitions assigned?
4. Can two consumers in one group consume the same partition?
5. Can two different groups consume the same partition?
6. What happens if consumers > partitions?
7. What happens if partitions > consumers?
8. Why is consumer group scalability limited by partition count?

## Example

```text
6 Partitions
3 Consumers

C1 → P0 P1
C2 → P2 P3
C3 → P4 P5
```

Then:

```text
6 Partitions
10 Consumers
```

Only six consumers can actively own partitions at a time.

## Staff-Level

9. How do you choose consumer-group size?
10. How do you scale consumers?
11. How do you handle a slow consumer?
12. How do you isolate one consumer group's failure from another?
13. Can one topic support thousands of consumer groups?
14. What are the scalability implications?

---

# 12. CONSUMER LAG

## Fundamentals

1. What is consumer lag?
2. How is it calculated?
3. Why does lag matter?
4. What causes lag?
5. Is lag always bad?
6. How do you distinguish temporary lag from permanent lag?

## Diagnosis

```text
Producer rate ↑
Consumer rate →
        ↓
Lag ↑
```

Possible causes:

- Producer spike
- Slow consumer
- Slow DB
- Hot partition
- Consumer rebalance
- Network problems
- CPU exhaustion

## Staff-Level

7. Design an alerting strategy for lag.
8. How do you automatically scale consumers based on lag?
9. Why can simply adding consumers fail to solve lag?
10. How do you identify the bottleneck?
11. What happens when downstream processing is slower than Kafka ingestion?
12. How would you recover from 1 billion accumulated events?

---

# 13. REBALANCING

> **High-value L6 topic.**

## Fundamentals

1. What is consumer rebalancing?
2. Why does rebalancing happen?
3. What happens when a consumer joins?
4. What happens when a consumer leaves?
5. What happens when a consumer crashes?
6. What happens when partitions increase?
7. What happens during a rebalance?

## Failure

8. What is a rebalance storm?
9. How can frequent rebalances affect throughput?
10. What happens to consumer processing during rebalance?
11. What happens to uncommitted offsets?

## Staff-Level

12. Design a consumer group with 1,000 consumers.
13. How would you minimize rebalance impact?
14. How do cooperative rebalancing strategies help?
15. How would you diagnose rebalance storms?
16. How do long-running processing tasks interact with consumer liveness?
17. What happens if a consumer is alive but stops polling?

---

# 14. DELIVERY SEMANTICS

> **Must-master L6 topic.**

## At-Most-Once

1. What does at-most-once mean?
2. How can messages be lost?
3. When is it acceptable?

## At-Least-Once

4. What does at-least-once mean?
5. Why do duplicates occur?
6. Why is idempotency important?
7. How would you make a payment consumer idempotent?

## Exactly-Once

8. What does exactly-once actually mean?
9. Exactly once between which components?
10. Does Kafka guarantee exactly-once end-to-end?
11. What are Kafka transactions?
12. What is idempotent producer behavior?
13. How are offsets and transactions related?
14. What happens when a transaction aborts?
15. What happens when a consumer crashes during a transaction?

## Staff-Level Scenarios

16. Design exactly-once processing for:

```text
Kafka → Payment Service → Database
```

17. What happens if:

```text
DB commit succeeds
Kafka offset commit fails
```

18. What happens if:

```text
Kafka offset commit succeeds
DB commit fails
```

19. How would you implement an idempotency key?
20. When is at-least-once + idempotent consumer better than exactly-once?

---

# 15. KRAFT / METADATA MANAGEMENT

## Fundamentals

1. Why does Kafka need metadata?
2. What metadata does Kafka maintain?
3. What is the controller?
4. What is KRaft?
5. Why was ZooKeeper historically used?
6. What problem does KRaft solve?
7. What is the controller quorum?
8. What happens if the controller fails?
9. How is metadata replicated?
10. How does a broker discover cluster metadata?

## Deep Dive

11. Who decides partition leadership?
12. Who maintains partition assignments?
13. Who handles broker registration?
14. How does a new broker join?
15. What happens when a broker disappears?
16. How does the cluster recover metadata?

## Staff-Level

17. How does metadata scale with millions of partitions?
18. What happens if the controller quorum loses majority?
19. How does metadata availability affect producers?
20. How would you design metadata management for 1,000 brokers?
21. What are the failure modes of the metadata plane?

---

# Cross-Concept Scenarios

## Scenario 1 — Broker Failure

> Broker 3 suddenly dies while handling 500K writes/sec.

Explain:

1. What fails?
2. Which partitions are affected?
3. What happens to leaders?
4. How is a new leader selected?
5. What happens to producers?
6. What happens to consumers?
7. What happens to ISR?
8. What happens to consumer lag?
9. How do we recover Broker 3?
10. How do we verify correctness?

---

## Scenario 2 — Consumer Lag

> Consumer lag suddenly increases from 10K to 50M.

Walk through:

```text
Detection
   ↓
Diagnosis
   ↓
Root cause
   ↓
Mitigation
   ↓
Scaling
   ↓
Recovery
   ↓
Verification
```

Questions:

1. Is producer throughput increasing?
2. Is consumer throughput decreasing?
3. Is there a hot partition?
4. Is the database slow?
5. Is there a rebalance storm?
6. Can we add consumers?
7. Do we have enough partitions?
8. What happens after scaling?
9. How do we prevent recurrence?

---

## Scenario 3 — Payment System

> Design Kafka for a global payment platform.

Requirements:

```text
10M transactions/sec
99.999% durability
Per-account ordering
Multi-region
Replay
Fraud detection
Audit
```

You must decide:

1. Partition key
2. Number of partitions
3. Replication factor
4. AZ strategy
5. Producer configuration
6. Consumer groups
7. Retry strategy
8. DLQ
9. Exactly-once / idempotency
10. Disaster recovery
11. Multi-region architecture
12. Observability

---

## Scenario 4 — Hot Customer

> One customer generates 30% of all Kafka traffic.

Questions:

1. Why is this a problem?
2. Will adding partitions solve it?
3. Can we change the partition key?
4. What happens to ordering?
5. Can we shard the customer?
6. How do we preserve ordering?
7. What changes are required downstream?

---

## Scenario 5 — Multi-Region Kafka

> Design Kafka across US, Europe and Asia.

Questions:

1. Active-active or active-passive?
2. Where are events produced?
3. Where are events consumed?
4. How do you replicate events?
5. How do you avoid duplicates?
6. What happens during a regional outage?
7. What happens to ordering?
8. What is the RPO?
9. What is the RTO?
10. How do you prevent replication loops?

---

## Scenario 6 — 10× Growth

Current:

```text
1M events/sec
100 brokers
```

Future:

```text
10M events/sec
1000 brokers
```

Explain how you evolve:

```text
Partitions
     ↓
Brokers
     ↓
Replication
     ↓
Network
     ↓
Storage
     ↓
Consumers
     ↓
Metadata
     ↓
Operations
```

---

# Final L6 Kafka Interview Checklist

## Core

- [ ] What is Kafka?
- [ ] Why Kafka?
- [ ] Kafka vs RabbitMQ
- [ ] Topic
- [ ] Partition
- [ ] Offset
- [ ] Broker

## Distribution

- [ ] Partitioning
- [ ] Leader/follower
- [ ] ISR
- [ ] Replication
- [ ] Leader election
- [ ] Failure domains

## Producers

- [ ] Producer flow
- [ ] Batching
- [ ] Compression
- [ ] `acks`
- [ ] Retries
- [ ] Idempotence
- [ ] Producer failure

## Consumers

- [ ] Consumer flow
- [ ] Consumer group
- [ ] Offset commit
- [ ] Consumer lag
- [ ] Rebalancing
- [ ] Backpressure

## Correctness

- [ ] Ordering
- [ ] At-most-once
- [ ] At-least-once
- [ ] Exactly-once
- [ ] Idempotency
- [ ] Transactions

## Scale

- [ ] Partition sizing
- [ ] Broker sizing
- [ ] Storage calculation
- [ ] Network calculation
- [ ] Consumer scaling
- [ ] Hot partitions
- [ ] Hot keys

## Production

- [ ] Monitoring
- [ ] Consumer lag alerts
- [ ] Under-replicated partitions
- [ ] Disk exhaustion
- [ ] Failure recovery
- [ ] DR
- [ ] Multi-region
- [ ] Capacity planning

## Architecture

- [ ] KRaft
- [ ] Metadata management
- [ ] Controller quorum
- [ ] Cluster scaling
- [ ] Partition movement

## Staff Engineer

- [ ] Explain trade-offs
- [ ] Challenge assumptions
- [ ] Compare alternatives
- [ ] Design under changing constraints
- [ ] Explain failure modes
- [ ] Explain operational ownership
- [ ] Calculate capacity
- [ ] Defend architecture decisions

---

# Mastery Method

For every concept, use seven rounds:

### Round 1 — Fundamentals
Can I define it and explain why it exists?

### Round 2 — Internals
Can I explain exactly how it works?

### Round 3 — Distributed Systems
Can I explain how it behaves across multiple nodes?

### Round 4 — Failure
Can I explain what happens when components fail?

### Round 5 — Trade-offs
Can I defend why this design was chosen?

### Round 6 — Production
Can I operate and troubleshoot it?

### Round 7 — L6 Interview
Can I redesign it when the interviewer changes the constraints?

---

# Recommended Starting Point

Start with:

> **1. What is a Kafka Topic, and why does Kafka need the concept of a Topic?**

Do not move to Partition until you can explain Topic clearly from:

**Fundamentals → Internals → Failure → Trade-offs → Production → L6 scenario.**
