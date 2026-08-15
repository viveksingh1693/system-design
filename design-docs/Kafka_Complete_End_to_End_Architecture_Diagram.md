# Kafka — Complete End-to-End Architecture Diagram

## 1. Complete Kafka Architecture

```text
Application / Orders Service
        |
        | Event
        v
Kafka Producer
        |
        +-- Serialize (Key, Value, Headers)
        |
        +-- Select Partition (Partitioner)
        |
        +-- Batch Records
        |
        +-- Compress
        |
        +-- Find Partition Leader (Metadata)
        |
        v
+--------------------------------------------------------------------------------+
|                                KAFKA CLUSTER                                  |
|                                                                                |
|  +------------------+    +------------------+    +------------------+         |
|  |     BROKER 1     |    |     BROKER 2     |    |     BROKER 3     |         |
|  |                  |    |                  |    |                  |         |
|  | Topic: orders    |    | Topic: orders    |    | Topic: orders    |         |
|  |                  |    |                  |    |                  |         |
|  | P0 - LEADER      |<-->| P0 - FOLLOWER   |<-->| P0 - FOLLOWER   |         |
|  | 0 1 2 3 4 5 ...  |    | 0 1 2 3 4 5 ...  |    | 0 1 2 3 4 5 ...  |         |
|  |                  |    |                  |    |                  |         |
|  | P1 - FOLLOWER    |<-->| P1 - LEADER     |<-->| P1 - FOLLOWER    |         |
|  | 0 1 2 3 4 5 ...  |    | 0 1 2 3 4 5 ...  |    | 0 1 2 3 4 5 ...  |         |
|  |                  |    |                  |    |                  |         |
|  | P2 - FOLLOWER    |<-->| P2 - FOLLOWER   |<-->| P2 - LEADER      |         |
|  | 0 1 2 3 4 5 ...  |    | 0 1 2 3 4 5 ...  |    | 0 1 2 3 4 5 ...  |         |
|  |                  |    |                  |    |                  |         |
|  |    Local Disk     |    |    Local Disk     |    |    Local Disk     |         |
|  +------------------+    +------------------+    +------------------+         |
|                                                                                |
|              Replication: Leader -> Followers / ISR                           |
+--------------------------------------------------------------------------------+
        |
        | Fetch Request
        v
+--------------------------------------------------------------------------------+
|                         CONSUMER GROUP: orders-group                          |
|                                                                                |
|  +----------------+       +----------------+       +----------------+          |
|  |   Consumer 1   |       |   Consumer 2   |       |   Consumer 3   |          |
|  | Assigned: P0   |       | Assigned: P1   |       | Assigned: P2   |          |
|  |       |        |       |       |        |       |       |        |          |
|  |    Fetch       |       |    Fetch       |       |    Fetch       |          |
|  |       |        |       |       |        |       |       |        |          |
|  |    Process     |       |    Process     |       |    Process     |          |
|  |       |        |       |       |        |       |       |        |          |
|  | Commit Offset  |       | Commit Offset  |       | Commit Offset  |          |
|  +----------------+       +----------------+       +----------------+          |
+--------------------------------------------------------------------------------+
        |
        | Offset Commit
        v
+-----------------------------+
|      __consumer_offsets     |
|                             |
| Group + Topic + Partition   |
|            ->               |
|     Committed Offset        |
+-----------------------------+
```

---

## 2. Producer -> Kafka -> Consumer Flow

```text
Application
     |
     | Event
     v
Producer
     |
     +-- Serialize
     |     |
     |     +-- Key + Value + Headers
     |
     +-- Select Partition
     |     |
     |     +-- Partitioner
     |           |
     |           +-- Key -> Hash -> Partition
     |
     +-- Batch
     |     |
     |     +-- batch.size / linger.ms
     |
     +-- Compress
     |
     +-- Find Partition Leader
     |     |
     |     +-- Metadata
     |
     v
Partition Leader
     |
     +-- Append
     |     |
     |     +-- Assign Offset
     |
     +-- Replicate
     |     |
     |     +-- Follower 1
     |     +-- Follower 2
     |
     v
Required Replicas / ISR
     |
     v
ACK
     |
     +-- acks=0
     +-- acks=1
     +-- acks=all
     |
     v
Producer
     |
     v
Application
```

---

## 3. Consumer Flow

```text
Kafka Partition
      |
      | Fetch Request
      v
Consumer
      |
      +-- Fetch Records
      |
      +-- Deserialize
      |
      +-- Process Records
      |
      +-- Business Operation
      |
      +-- Commit Offset
              |
              v
       __consumer_offsets
              |
              v
       Group + Topic + Partition
              |
              v
       Committed Offset
```

---

## 4. All Major Concepts

```text
                         KAFKA
                           |
                           v
                    Kafka Cluster
                           |
              +------------+------------+
              |            |            |
              v            v            v
           Broker 1     Broker 2     Broker 3
              |            |            |
              +------------+------------+
                           |
                           v
                         Topic
                           |
              +------------+------------+
              v            v            v
         Partition 0  Partition 1  Partition 2
              |            |            |
              v            v            v
          Offset 0      Offset 0      Offset 0
          Offset 1      Offset 1      Offset 1
          Offset 2      Offset 2      Offset 2
              |            |            |
              +------------+------------+
                           |
                           v
                    Consumer Group
                           |
              +------------+------------+
              v            v            v
          Consumer 1   Consumer 2   Consumer 3
              |            |            |
              v            v            v
           Process      Process      Process
              |            |            |
              +------------+------------+
                           |
                           v
                     Commit Offset
                           |
                           v
                 __consumer_offsets
```

---

## 5. Partition + Replica Model

```text
Topic: orders

Partition 0
--------------------------------------
Broker 1 -> LEADER
Broker 2 -> FOLLOWER
Broker 3 -> FOLLOWER

Partition 1
--------------------------------------
Broker 1 -> FOLLOWER
Broker 2 -> LEADER
Broker 3 -> FOLLOWER

Partition 2
--------------------------------------
Broker 1 -> FOLLOWER
Broker 2 -> FOLLOWER
Broker 3 -> LEADER
```

For each partition:

```text
             Partition
                  |
       +----------+----------+
       v          v          v
    Leader     Follower   Follower
       |          |          |
       +----------+----------+
                  |
                  v
                 ISR
```

### Key rule

A partition has one active leader and multiple replicas. The ISR is the set of replicas sufficiently caught up with the leader.

---

## 6. Consumer Group Scaling

### 4 Partitions + 4 Consumers

```text
P0 -> Consumer 1
P1 -> Consumer 2
P2 -> Consumer 3
P3 -> Consumer 4
```

### 4 Partitions + 2 Consumers

```text
P0 + P1 -> Consumer 1
P2 + P3 -> Consumer 2
```

### 4 Partitions + 8 Consumers

```text
P0 -> Consumer 1
P1 -> Consumer 2
P2 -> Consumer 3
P3 -> Consumer 4

Consumer 5 -> IDLE
Consumer 6 -> IDLE
Consumer 7 -> IDLE
Consumer 8 -> IDLE
```

### Important rule

> Within a consumer group, one partition can be actively assigned to only one consumer at a time.

Therefore:

```text
Consumer parallelism <= Partition count
```

---

## 7. Multiple Consumer Groups

```text
                         orders
                            |
             +--------------+--------------+
             |              |              |
             v              v              v
       Payment Group   Fraud Group   Analytics Group
             |              |              |
          +--+--+         +--+--+         +--+--+
          v     v         v     v         v     v
         C1    C2        C3    C4        C5    C6
```

Each consumer group maintains independent offsets.

```text
Payment Group   -> P0 -> offset 500
Fraud Group     -> P0 -> offset 350
Analytics Group -> P0 -> offset 100
```

---

## 8. Offset Model

```text
Partition 0

Offset 0 -> Event A
Offset 1 -> Event B
Offset 2 -> Event C
Offset 3 -> Event D
Offset 4 -> Event E
Offset 5 -> Event F
```

The committed offset represents the consumer group's persisted progress.

```text
Consumer Group
      +
Topic
      +
Partition
      +
Committed Offset
```

---

## 9. KRaft / Control Plane

```text
              KRaft Controller Quorum

           +-------------------------+
           |                         |
           v                         v
     Controller 1              Controller 2
        Leader                   Follower
           |                         |
           +------------+------------+
                        |
                        v
                  Controller 3
                    Follower
                        |
                        v
                 Cluster Metadata
                        |
          +-------------+-------------+
          v             v             v
       Brokers      Partition      Leaders
                    Assignment
```

The control plane manages:

- Broker membership
- Partition metadata
- Leadership
- Replica assignments
- Cluster configuration

---

## 10. Data Plane vs Control Plane

```text
                         KAFKA CLUSTER
                              |
                +-------------+-------------+
                |                           |
                v                           v
          CONTROL PLANE                DATA PLANE
                |                           |
              KRaft                       Brokers
                |                           |
       +--------+--------+          +-------+-------+
       v        v        v          v       v       v
   Metadata  Leader    Config    Topics Partitions Replicas
             Election
                                          |
                                          v
                                      Producers
                                          |
                                          v
                                      Consumers
```

---

## 11. Complete End-to-End Sequence

```text
1.  Application creates event
          |
          v
2.  Producer serializes event
          |
          v
3.  Producer determines partition
          |
          v
4.  Producer gets partition leader from metadata
          |
          v
5.  Producer batches records
          |
          v
6.  Producer optionally compresses batch
          |
          v
7.  Producer sends ProduceRequest
          |
          v
8.  Partition Leader receives request
          |
          v
9.  Leader appends records to log
          |
          v
10. Records receive offsets
          |
          v
11. Followers replicate records
          |
          v
12. Required ISR condition is satisfied
          |
          v
13. Leader sends ACK
          |
          v
14. Producer receives ACK
          |
          v
15. Consumer sends FetchRequest
          |
          v
16. Broker returns records
          |
          v
17. Consumer processes records
          |
          v
18. Consumer commits offset
          |
          v
19. Offset stored in __consumer_offsets
          |
          v
20. Consumer can resume from committed position
```

---

## 12. Broker Failure Flow

```text
Broker 1
   |
   +-- P0 Leader
   +-- P1 Follower
   +-- P2 Leader
        |
        X
     FAILURE
        |
        v
Kafka detects broker failure
        |
        v
Affected partitions identified
        |
        v
Eligible ISR replicas selected
        |
        v
New partition leaders
        |
        v
Producer refreshes metadata
        |
        v
Consumer refreshes metadata
        |
        v
Traffic resumes
        |
        v
Failed broker recovers
        |
        v
Replica catches up
        |
        v
Replica returns to ISR
```

---

## 13. Consumer Failure Flow

```text
Consumer 1
    |
    | Processing P0
    X
  CRASH
    |
    v
Consumer Group detects failure
    |
    v
Rebalance
    |
    v
P0 reassigned to Consumer 2
    |
    v
Consumer 2 reads from committed offset
    |
    v
Processing resumes
```

If processing occurred after the last committed offset, that record may be processed again. This is why at-least-once processing and idempotency matter.

---

## 14. Producer Retry Flow

```text
Producer
   |
   | ProduceRequest
   v
Broker
   |
   X
Request / ACK failure
   |
   v
Producer Retry
   |
   +-- Backoff
   +-- Refresh metadata if needed
   +-- Retry
        |
        v
Partition Leader
        |
        v
Idempotent sequence handling
        |
        v
ACK
```

---

## 15. One-Line Definition of Every Concept

| Concept | Mental Model |
|---|---|
| Kafka Cluster | Collection of Kafka brokers working together |
| Broker | Kafka server that stores and serves partition data |
| Topic | Logical named stream of events |
| Partition | Ordered append-only log and unit of parallelism |
| Offset | Position of a record within a partition |
| Leader | Replica responsible for normal partition traffic |
| Follower | Replica that copies the leader's data |
| ISR | Replicas sufficiently caught up with the leader |
| Producer | Client that publishes records |
| Partitioner | Determines which partition receives a record |
| Consumer | Client that reads records |
| Consumer Group | Consumers cooperating to process partitions |
| Consumer Lag | Difference between available records and consumer progress |
| KRaft | Kafka's metadata/control-plane consensus architecture |
| __consumer_offsets | Internal Kafka topic used to store committed consumer offsets |

---

# 16. L6 Interview Whiteboard Version

If you have only 2–3 minutes to draw Kafka in an interview:

```text
                         APPLICATION
                              |
                              v
                         PRODUCER
                              |
                    +---------+---------+
                    |                   |
                Partitioner          Metadata
                    |                   |
                    +---------+---------+
                              v
                 +-------------------------+
                 |      KAFKA CLUSTER      |
                 |                         |
                 |  Broker 1  Broker 2  Broker 3
                 |     |         |         |
                 |    P0        P1        P2
                 |     |         |         |
                 |  Leader    Leader    Leader
                 |     |         |         |
                 |  Replica   Replica   Replica
                 |                         |
                 |      Ordered Logs       |
                 |      + Offsets          |
                 +------------+------------+
                              |
                         Fetch Records
                              |
                              v
                    +-------------------+
                    |  CONSUMER GROUP   |
                    |                   |
                    | C1 -> P0          |
                    | C2 -> P1          |
                    | C3 -> P2          |
                    +---------+---------+
                              |
                         Commit Offset
                              |
                              v
                    __consumer_offsets
```

---

# 17. Final Mental Model

Memorize this chain:

```text
Application
     |
     v
Producer
     |
     +-- Serialize
     +-- Partition
     +-- Batch
     +-- Compress
     +-- Metadata
     |
     v
Kafka Cluster
     |
     +-- Broker
     |     |
     |     +-- Topic
     |     |     |
     |     |     +-- Partition
     |     |           |
     |     |           +-- Offset
     |     |           +-- Leader
     |     |           +-- Followers
     |     |
     |     +-- Replicas
     |     +-- Local Disk
     |
     +-- KRaft
           |
           +-- Cluster Metadata
     |
     v
Consumer Group
     |
     +-- Consumer 1
     +-- Consumer 2
     +-- Consumer 3
     |
     v
Fetch
     |
     v
Process
     |
     v
Commit Offset
     |
     v
__consumer_offsets
```

## The Single Most Important Flow

```text
Producer
   |
   v
Topic
   |
   v
Partition
   |
   v
Partition Leader
   |
   v
Replicas / ISR
   |
   v
ACK
   |
   v
Consumer Group
   |
   v
Consumer
   |
   v
Process
   |
   v
Commit Offset
```

This is the diagram to be able to draw from memory during an L6 Kafka system design interview.
