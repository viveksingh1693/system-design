# PostgreSQL L6 Mastery --- Concept-First Learning Path

## Purpose

This track is for **L6 / Staff Engineer-level PostgreSQL mastery**.

The goal is not to memorize PostgreSQL commands or configuration
parameters.

The goal is to build a mental model strong enough that we can:

-   explain PostgreSQL from first principles
-   understand why each major subsystem exists
-   reason about correctness, concurrency, durability, and performance
-   diagnose production failures
-   make architecture and trade-off decisions
-   design a PostgreSQL-like database from scratch
-   defend those decisions in an L6 system-design interview

> **Core principle:** Learn the concept first, then PostgreSQL's
> implementation, then production behavior, and only then build a
> simplified version ourselves.

------------------------------------------------------------------------

# 1. Learning Philosophy

We will follow the same approach used for the other deep technical
topics in this project:

``` text
Problem
   ↓
Naive Solution
   ↓
Why Naive Solution Breaks
   ↓
Conceptual Solution
   ↓
PostgreSQL Design
   ↓
Internal Implementation
   ↓
Production Behavior
   ↓
Failure Modes
   ↓
Trade-offs
   ↓
L6 Interview Questions
   ↓
Build a Simplified Version
```

We will **not start with SQL syntax**.

We will first understand what a database has to solve.

------------------------------------------------------------------------

# 2. The End Goal --- Design a Database From Scratch

By the end of this track, we should be able to start with:

> "We have no PostgreSQL. Design a relational database."

and gradually arrive at:

``` text
                         Clients
                            |
                    Connection Layer
                            |
                            v
                     Query Processing
                            |
             +--------------+--------------+
             |                             |
          Parser                        Planner
             |                             |
             +--------------+--------------+
                            |
                         Executor
                            |
             +--------------+--------------+
             |                             |
          Indexes                       Heap
             |                             |
             +--------------+--------------+
                            |
                       Buffer Manager
                            |
             +--------------+--------------+
             |                             |
            WAL                         Storage
             |                             |
             +--------------+--------------+
                            |
                        Recovery
                            |
                       Replication
                            |
                 +----------+----------+
                 |          |          |
              Replica    Replica    Replica
```

Every box and arrow should eventually be explainable.

------------------------------------------------------------------------

# 3. Learning Layers

We will learn PostgreSQL in six conceptual layers.

## Layer 1 --- Database Fundamentals

Understand the problems databases solve.

Topics:

-   What is a database?
-   What is a DBMS?
-   Relational model
-   Tables, rows, columns
-   Keys and constraints
-   Transactions
-   ACID
-   Consistency
-   Isolation
-   Durability
-   Serializability
-   OLTP vs OLAP
-   Row-oriented vs column-oriented storage
-   Storage engines
-   B-tree vs LSM-tree
-   Memory vs disk
-   Local vs distributed databases

### Target understanding

We should be able to answer:

> Why do we need a database instead of storing objects directly in
> files?

------------------------------------------------------------------------

# 4. Layer 2 --- Database Architecture

Before PostgreSQL internals, understand the generic architecture of a
relational database.

``` text
Client
  |
  v
Connection Manager
  |
  v
SQL
  |
  v
Parser
  |
  v
Analyzer
  |
  v
Planner / Optimizer
  |
  v
Executor
  |
  v
Storage Engine
  |
  +---- Buffer Manager
  |
  +---- Index Manager
  |
  +---- Transaction Manager
  |
  +---- WAL
  |
  v
Disk
```

Questions we must answer:

1.  Why separate parsing from planning?
2.  Why do we need a query optimizer?
3.  Why can't the executor directly read files?
4.  Why do we need a buffer manager?
5.  Why do we need a transaction manager?
6.  Why do we need WAL?
7.  Why can't we simply write every change directly to disk?
8.  Where does concurrency control live?

------------------------------------------------------------------------

# 5. Layer 3 --- PostgreSQL Internals

Once the concepts are clear, map them onto PostgreSQL.

Major areas:

``` text
PostgreSQL
|
+-- Process Architecture
|
+-- Shared Memory
|
+-- Storage
|   +-- Pages
|   +-- Tuples
|   +-- Heap
|   +-- TOAST
|
+-- Buffer Manager
|
+-- WAL
|
+-- MVCC
|
+-- Transaction Manager
|
+-- Lock Manager
|
+-- Indexes
|
+-- Query Parser
|
+-- Query Planner
|
+-- Query Executor
|
+-- Vacuum
|
+-- Statistics
|
+-- Replication
|
+-- Recovery
|
+-- Partitioning
|
+-- Extensions
```

The objective is not just knowing the names.

For every subsystem:

> **What problem does it solve?**

------------------------------------------------------------------------

# 6. Layer 4 --- Correctness

Correctness is one of the most important L6 dimensions.

We will deeply understand:

## Transactions

-   transaction lifecycle
-   BEGIN
-   COMMIT
-   ROLLBACK
-   atomicity

## Isolation

-   Read Committed
-   Repeatable Read
-   Serializable
-   dirty reads
-   non-repeatable reads
-   phantom reads
-   lost updates
-   write skew

## MVCC

-   tuple versions
-   transaction IDs
-   snapshots
-   visibility
-   xmin
-   xmax
-   dead tuples

## Locking

-   row locks
-   table locks
-   predicate concepts
-   advisory locks
-   deadlocks
-   lock contention

### Core question

> How can multiple transactions read and modify the database
> concurrently without corrupting logical state?

------------------------------------------------------------------------

# 7. Layer 5 --- Durability and Failure

A database is not useful merely because it can answer queries.

It must survive failures.

We will understand:

``` text
Transaction
    |
    v
Modify memory
    |
    v
WAL
    |
    v
Commit
    |
    v
Eventually data pages reach disk
```

Topics:

-   WAL
-   LSN
-   WAL buffers
-   WAL writer
-   checkpoints
-   dirty pages
-   fsync
-   crash recovery
-   redo
-   checkpoints
-   full-page writes
-   WAL archiving
-   point-in-time recovery

### Core question

> What happens if the machine loses power one microsecond after COMMIT?

We should be able to trace the answer all the way to disk.

------------------------------------------------------------------------

# 8. Layer 6 --- Performance and Scale

Once correctness is understood, we optimize.

## Query performance

-   sequential scan
-   index scan
-   bitmap scan
-   nested-loop join
-   hash join
-   merge join
-   cardinality estimation
-   selectivity
-   statistics
-   cost model
-   EXPLAIN
-   EXPLAIN ANALYZE

## Storage performance

-   page size
-   locality
-   random vs sequential I/O
-   cache hit ratio
-   buffer pool
-   OS page cache
-   SSD behavior
-   IOPS
-   throughput

## Concurrency performance

-   locks
-   contention
-   connection exhaustion
-   long transactions
-   vacuum interference

### Core question

> Why can a query that is fast with 1 million rows become
> catastrophically slow at 1 billion rows?

------------------------------------------------------------------------

# 9. PostgreSQL Architecture --- Conceptual Map

We will eventually build this mental model:

``` text
                         PostgreSQL Server
                                |
        +-----------------------+-----------------------+
        |                       |                       |
   Connection              Query Engine            Background
   Management                                      Processes
        |                       |                       |
        |                 +-----+-----+          +------+------+
        |                 |           |          |             |
        |               Parser     Planner   Checkpointer   Autovacuum
        |                             |
        |                          Executor
        |                             |
        +-----------------------------+
                                      |
                             Storage Subsystem
                                      |
          +-------------+-------------+-------------+
          |             |             |             |
       Buffer        Heap          Indexes         WAL
       Manager
          |             |             |             |
          +-------------+-------------+-------------+
                                      |
                                    Disk
```

This is a **learning map**, not yet an implementation diagram.

We will refine it as our understanding grows.

------------------------------------------------------------------------

# 10. Concept-First Topic Sequence

## Phase A --- Foundations

### A1. What is a database?

Questions:

1.  Why not use files?
2.  What does a database guarantee?
3.  What makes a DBMS different from a file system?
4.  What problems appear when multiple applications access the same
    data?

------------------------------------------------------------------------

### A2. Relational Model

Questions:

1.  Why relations?
2.  What is a tuple?
3.  What is a relation?
4.  Why primary keys?
5.  Why foreign keys?
6.  What are constraints?
7.  What is normalization?
8.  When is denormalization useful?

------------------------------------------------------------------------

### A3. Transactions

Questions:

1.  What is a transaction?
2.  Why do we need transactions?
3.  What does atomicity actually mean?
4.  What does durability actually mean?
5.  What happens if the application crashes halfway through a
    transaction?

------------------------------------------------------------------------

### A4. ACID

We will not memorize the acronym.

For each property we will ask:

``` text
Problem
  ↓
Failure scenario
  ↓
Required guarantee
  ↓
Database mechanism
```

------------------------------------------------------------------------

# 11. Storage Engine Sequence

After foundations:

``` text
Disk
 ↓
Files
 ↓
Pages
 ↓
Tuples
 ↓
Heap
 ↓
Buffer Pool
 ↓
Indexes
```

Topics:

1.  Disk storage
2.  Database files
3.  Pages
4.  Page layout
5.  Tuple layout
6.  Heap files
7.  Free space
8.  Buffer pool
9.  Buffer replacement
10. Dirty pages
11. Indexes
12. B-tree
13. Other PostgreSQL index types

------------------------------------------------------------------------

# 12. Concurrency Sequence

``` text
Single User
    ↓
Multiple Readers
    ↓
Multiple Writers
    ↓
Concurrent Transactions
    ↓
Isolation Problems
    ↓
Locking
    ↓
MVCC
    ↓
PostgreSQL MVCC
```

We will derive MVCC rather than memorizing it.

------------------------------------------------------------------------

# 13. Durability Sequence

``` text
Write to memory
      ↓
What if process crashes?
      ↓
What if OS crashes?
      ↓
What if machine loses power?
      ↓
What if disk write is partial?
      ↓
WAL
      ↓
Checkpoint
      ↓
Crash Recovery
```

------------------------------------------------------------------------

# 14. Query Engine Sequence

``` text
SQL
 ↓
Parsing
 ↓
Analysis
 ↓
Rewrite
 ↓
Planning
 ↓
Optimization
 ↓
Execution
 ↓
Storage
```

Then:

``` text
SELECT
WHERE
JOIN
GROUP BY
ORDER BY
LIMIT
```

become opportunities to understand:

-   scans
-   indexes
-   joins
-   sorting
-   aggregation
-   parallelism

------------------------------------------------------------------------

# 15. Replication and Distributed PostgreSQL

Only after we understand the single-node database.

Sequence:

``` text
Single PostgreSQL
       ↓
Durable PostgreSQL
       ↓
Primary + Replica
       ↓
WAL Streaming
       ↓
Replication Lag
       ↓
Failover
       ↓
High Availability
       ↓
Read Scaling
       ↓
Partitioning
       ↓
Sharding
```

We will distinguish carefully between:

-   replication
-   partitioning
-   sharding
-   failover
-   high availability
-   distributed transactions

------------------------------------------------------------------------

# 16. Production Engineering

After the internals:

## Observability

-   active sessions
-   locks
-   wait events
-   slow queries
-   query statistics
-   cache behavior
-   replication lag
-   WAL growth
-   vacuum health

## Common incidents

We will reason through:

1.  CPU suddenly reaches 100%
2.  Connections are exhausted
3.  Queries become slow
4.  Disk usage reaches 100%
5.  WAL keeps growing
6.  Replication lag increases
7.  Autovacuum cannot keep up
8.  Deadlocks increase
9.  Long-running transactions block cleanup
10. Primary database fails

For every incident:

``` text
Detect
  ↓
Scope
  ↓
Hypothesis
  ↓
Evidence
  ↓
Mitigation
  ↓
Root Cause
  ↓
Prevention
```

------------------------------------------------------------------------

# 17. L6 Design Questions

At the end of each topic we will create L6-level questions.

Examples:

### Architecture

-   Why is PostgreSQL process-based?
-   Why does PostgreSQL use shared buffers?
-   Why does PostgreSQL use WAL?
-   Why does PostgreSQL use MVCC?

### Storage

-   Why are databases page-oriented?
-   Why not store every row independently?
-   Why does PostgreSQL have heap storage?
-   Why can indexes become larger than the table?

### Concurrency

-   Why MVCC instead of read locks?
-   Why can a database still experience lock contention with MVCC?
-   How does PostgreSQL avoid blocking readers during writes?

### Performance

-   Why did PostgreSQL choose B-tree as the default index?
-   When is an index slower than a sequential scan?
-   Why can an index fail to improve a query?

### Reliability

-   What happens during crash recovery?
-   What happens when WAL fills the disk?
-   What happens if the primary fails during a transaction?

### Scale

-   Why doesn't replication solve write scalability?
-   When should we partition?
-   When should we shard?
-   What consistency problems appear after sharding?

------------------------------------------------------------------------

# 18. Build PostgreSQL From Scratch

Only after the concepts are understood.

We will create a simplified database in stages.

## Version 0 --- File Database

``` text
insert(key, value)
read(key)
```

No SQL.

------------------------------------------------------------------------

## Version 1 --- Pages

``` text
Database
  ├── Page 1
  ├── Page 2
  └── Page 3
```

------------------------------------------------------------------------

## Version 2 --- Heap Storage

Add:

-   tuples
-   tuple IDs
-   page management
-   free space

------------------------------------------------------------------------

## Version 3 --- Buffer Manager

Add:

-   buffer pool
-   page cache
-   dirty pages
-   eviction

------------------------------------------------------------------------

## Version 4 --- B-tree Index

Add:

``` text
lookup(key)
```

without scanning the entire table.

------------------------------------------------------------------------

## Version 5 --- Transactions

Add:

-   BEGIN
-   COMMIT
-   ROLLBACK

------------------------------------------------------------------------

## Version 6 --- MVCC

Add:

-   transaction IDs
-   tuple versions
-   snapshots
-   visibility

------------------------------------------------------------------------

## Version 7 --- WAL

Add:

-   log sequence numbers
-   WAL records
-   durable commits

------------------------------------------------------------------------

## Version 8 --- Crash Recovery

Simulate:

``` text
Write
  ↓
Crash
  ↓
Restart
  ↓
Recover from WAL
```

------------------------------------------------------------------------

## Version 9 --- Query Engine

Add:

``` text
SQL
 ↓
Parser
 ↓
Planner
 ↓
Executor
```

------------------------------------------------------------------------

## Version 10 --- Replication

Add:

``` text
Primary
   |
   +---- Replica
   |
   +---- Replica
```

------------------------------------------------------------------------

# 19. Study Rule

For every concept, we should be able to answer five questions:

### 1. What problem does it solve?

### 2. What happens without it?

### 3. How does PostgreSQL implement it?

### 4. What are the trade-offs?

### 5. How would I design it from scratch?

If we cannot answer these five, the topic is **not mastered yet**.

------------------------------------------------------------------------

# 20. What We Will NOT Do Initially

We will deliberately postpone:

-   memorizing configuration parameters
-   PostgreSQL CLI commands
-   SQL syntax beyond what is needed for examples
-   administration commands
-   extensions
-   cloud-specific PostgreSQL products
-   vendor-specific tuning recipes

These come later.

First:

> **Concept → architecture → internals → implementation → operations**

------------------------------------------------------------------------

# 21. Mastery Standard

A topic is considered mastered when I can explain it at three levels.

## Level 1 --- Simple

Explain it to a junior engineer.

## Level 2 --- Internal

Explain how PostgreSQL implements it.

## Level 3 --- L6

Explain:

-   why the design exists
-   alternatives considered
-   trade-offs
-   failure modes
-   scalability implications
-   operational implications
-   how I would redesign it under different constraints

------------------------------------------------------------------------

# 22. Final Target

At the end of this journey, the question:

> **"Design PostgreSQL from scratch."**

should not feel like a PostgreSQL trivia question.

It should feel like a system-design problem:

``` text
Requirements
    ↓
Correctness
    ↓
Data Model
    ↓
Storage
    ↓
Concurrency
    ↓
Transactions
    ↓
Durability
    ↓
Query Processing
    ↓
Indexes
    ↓
Recovery
    ↓
Replication
    ↓
High Availability
    ↓
Scaling
    ↓
Operations
```

And we should be able to justify every major decision.

------------------------------------------------------------------------

# 23. Starting Point

## Lesson 1

### What is a Database?

We start from absolute zero.

The first question is deliberately simple:

> **If I can store data in files, why do I need a database?**

From that question we will derive:

``` text
Files
 ↓
Concurrent Access
 ↓
Consistency Problems
 ↓
Transactions
 ↓
Durability
 ↓
Indexes
 ↓
Query Processing
 ↓
Database Management System
```

**Do not jump to PostgreSQL yet.**

First understand **why a database needs all the machinery that
PostgreSQL eventually provides.**
