# Consistent Hashing

> Cleaned version of the original notes. Manual review is still recommended because some sections contain duplicate content intentionally written for explanation.

## Problem Space (10)

### 1. What problem does this solve?
    Consistent hashing solves     **resharding problem**. Consistent hashing solves the problem of efficient data distribution in a dynamic distributed system where servers can be added or removed frequently.
    95% cache miss
        Database overload
        Latency spikes
        Connection pool exhaustion
        Cascading failures

    More specifically, it minimizes the amount of data that must move when the cluster membership changes.
     Consistent hashing solves the data redistribution problem in distributed systems with changing cluster membership. Traditional approaches like `hash(key) % N` cause almost all keys to move when servers are added or removed, leading to cache misses, database overload, and instability. Consistent hashing minimizes remapping so that only a small fraction of keys move, enabling efficient scaling and resilience.
    
### 2. Who experiences this problem?
    Any system that distributes data, requests, or workload across multiple machines and whose cluster membership changes over time.
     **Distributed Cache Systems
     Distributed Databases
     Load Balancers
     CDNs
     Object Storage Systems
     Message Queue Partitioning
     Service Discovery Systems
     
     Systems that distribute data or requests across multiple machines and whose membership changes frequently experience this problem. Examples include distributed caches, databases, CDNs, storage systems, and load balancers. They need a stable way to map keys to nodes without causing massive data movement whenever servers are added or removed.
### 3. What happens if we do nothing?
    Let's say we ignore the problem and continue using: 
        server = hash(key) % N
        Your system may still work initially, but it becomes increasingly expensive, unstable, and operationally painful as the cluster changes.
     The consequences depend on what the system is doing.
     #Scenario 1: Distributed Cache
         Application -> 100 Redis nodes -> 200M cached objects
             Traffic increases.
         Application -> 101 Redis nodes -> 200M cached objects (Almost 90% Invalidate)
        Database CPU spikes
        Connection pool exhaustion
        Latency increases
        Timeouts
        Retries increase traffic further
        Potential cascading failures
    #Scenario 2: Distributed Database
        20 TB data
        100 shards
        You add 1 machine. Without proper redistribution:
            20 TB data migration
        Problems:
        - Huge network traffic
        - Long rebalance windows
        - Increased disk I/O
        - Higher operational risk
        Adding one machine becomes a significant event instead of a routine operation.

    #Scenario 3: Load Balancer
        Users -> Backend Servers
        You use sticky sessions.
            user123 → S4
        Add one server.
            Suddenly
            user123 → S9
        Effects:
    - User sessions disappear (if sessions are stored locally)
    - Authentication state may be lost
    - Connection warm-up restarts
        User experience degrades.
        
    If we do nothing and continue using naive hashing, every membership change causes large-scale key redistribution. In cache systems, that leads to cache misses and database overload. In storage systems, it causes expensive data migration. In load balancers, it breaks session affinity. Over time, scaling operations themselves become a source of instability.

### 4. What was used before this?
    Simple modulo-based partitioning (`hash(key) % N`) was used before consistent hashing.
    
    Approach 1: Manual partition tables
        Problems:
        - Manual maintenance
        - Human errors
        - Difficult rebalancing
        - Doesn't scale
    
    Approach 2: Central lookup table
    - Huge metadata
    - Extra network hop
    - Central bottleneck
    - High availability concerns
    
    Approach 3: Fixed partitions (still very popular)
    Instead of mapping directly:
        Key -> Server
        Do
        Key -> Partition -> Server
    1024 partitions -> 100 servers
    Many systems don't use classic consistent hashing anymore. They use fixed partitions.

    Approach 4: Consistent Hashing
        A decentralized way to achieve minimal movement without central coordination.
        
        Single Server
        ↓
        Modulo Hashing
        ↓
        Manual Partitioning
        ↓
        Lookup Tables
        ↓
        Consistent Hashing
        ↓
        Virtual Nodes
        ↓
        Rendezvous Hashing
        ↓
        Jump Hash
        ↓
        Maglev

| Solution           | Why it eventually struggled    |
| ------------------ | ------------------------------ |
| Single server      | Capacity limit                 |
| Modulo hashing     | Massive redistribution         |
| Manual partitions  | Operational burden             |
| Lookup tables      | Central bottleneck             |
| Consistent hashing | Ring complexity and imbalance  |
| Rendezvous/Jump    | Simpler or faster alternatives |
Before consistent hashing, systems primarily used modulo-based hashing (`hash(key) % N`). It works well for static clusters because it's simple and fast. However, as systems became dynamic and servers were frequently added or removed, modulo hashing caused massive redistribution. Engineers then explored partition tables, lookup tables, and eventually consistent hashing to minimize movement during membership changes.

### 5. Why was the old approach insufficient?

   There were two common approaches before consistent hashing became popular.
    1. Modulo hashing
        A) Why was modulo hashing insufficient?
            server = hash(key) % N
            It has a hidden assumption: The number of servers (N) is stable. That assumption breaks in modern systems. Servers are not permanent. 
            Crash
            Maintenance
            Autoscaling
            Deployments
            Regional expansion
            Every time N changes: Almost all keys move.This creates a chain reaction.
            Membership change -> Key redistribution -> Cache miss -> Database overload -> Latency spike -> Retries ->Even more load
    2. Central lookup/partition tables
          Problem 1: Central bottleneck
          Problem 2: Single point of failure
          Problem 3: Huge metadata
          Problem 4: Coordination overhead
        Both eventually became insufficient at scale.
        # This is the engineering requirement matrix

| Requirement           | Modulo | Lookup Table | Consistent Hash |
| --------------------- | ------ | ------------ | --------------- |
| Decentralized         | ✅      | ❌            | ✅               |
| Minimal movement      | ❌      | ✅            | ✅               |
| No central bottleneck | ✅      | ❌            | ✅               |
| Scales easily         | ⚠️     | ⚠️           | ✅               |
> The old approaches were insufficient because they either assumed static infrastructure or introduced central bottlenecks. Modulo hashing redistributed almost all keys whenever the number of servers changed, while lookup tables required centralized coordination and additional infrastructure. Consistent hashing was designed to provide decentralized, scalable, and stable key distribution with minimal redistribution during membership changes.

### 6. What assumptions does this make?
### 7. What constraints exist?
### 8. What business metrics improve?
    Consistent hashing improves system stability during scaling events, which indirectly improves availability, latency, cost efficiency, and customer experience.
    # Business KPI mapping
    Consistent hashing improves business metrics indirectly by making infrastructure changes safe. It reduces latency spikes, prevents outages during scaling, lowers infrastructure costs by protecting downstream systems, improves availability, and reduces operational burden. The real business value is that growth and scaling stop being risky events and become routine operations.

| Technical Improvement | Business Metric                 |
| --------------------- | ------------------------------- |
| Fewer cache misses    | Lower infrastructure cost       |
| Stable scaling        | Higher availability             |
| Lower DB load         | Better customer experience      |
| Less redistribution   | Faster deployments              |
| Fewer incidents       | Higher engineering productivity |
| Better load balancing | Better resource utilization     |
### 9. What non-functional requirements matter?
### 10. What scale was this designed for?
     Consistent hashing was designed for large, dynamic distributed systems where nodes frequently join and leave. It was invented because operational instability becomes expensive at scale.
     Consistent hashing was designed for large, dynamic distributed systems where data movement is expensive and cluster membership changes frequently. The problem isn't the number of servers alone; it's the combination of many keys, many nodes, and frequent scaling events. The goal is to make infrastructure changes proportional to the size of the change rather than proportional to the entire dataset.
     Don't ask, "At what scale does this algorithm work?" Ask, "At what scale does the old solution become operationally unacceptable?"
## Internal Working (10)

### 13. What are the main components?
    The **algorithm** has only six core pieces:
    
    1. Hash Function: Converts both **keys** and **nodes** into positions in the same hash space.
    2. Hash Space (Ring): The hash space is a circular range, The "ring" is simply a way to make the hash space wrap around instead of ending.
    3. Physical Nodes: These are the actual machines. Each physical node owns one or more ranges of the hash space.
    4. Virtual Nodes: Instead of placing a physical server once: Place it many times. They improve load balancing and make rebalancing smoother.
    5. Keys: The objects being distributed. Each key is hashed into the same hash space.
    6. Lookup Algorithm: 
        Given a key:
        1. Hash the key.
        2. Find its position on the ring.
        3. Move clockwise.
        4. Choose the first node encountered.

```
Key -> Hash -> Ring -> Clockwise -> Owner
```

        Time complexity is typically **O(log N)** using an ordered data structure.
    
    The **production system** adds:
    
    7. Membership Service
    8. Ring Builder
    9. Ring Distribution: Once the ring changes, every client must receive the updated version.A key requirement is that **all clients eventually converge on the same ring**.
    10. Client SDK / Router: Instead of asking a central service, the client computes it locally. This eliminates an extra network hop.
    11. Failure Detector
    12. Replication
    13. Rebalancer
    
>     **The algorithm solves "Which node owns this key?" The production system solves "How do thousands of clients agree on that answer while nodes are continuously joining, leaving, and failing?"**
### 14. What is the end-to-end flow?
### 15. What are the states involved?
### 16. What is data represented?
### 17. How are components connected?
### 18. What data structures are used?
| Component     | Data Structure              | Purpose                              |
| ------------- | --------------------------- | ------------------------------------ |
| Ring          | Sorted Array / Balanced BST | Maintain ordered node positions      |
| Node Lookup   | Hash Map                    | Hash → Node mapping                  |
| Membership    | Set / Hash Map              | Track active nodes                   |
| Virtual Nodes | Array/List                  | Store vnode hashes per physical node |
| Rebalancer    | Queue                       | Background migration tasks           |
The core data structure is an ordered representation of the ring, typically a sorted array or a balanced tree, because we need efficient successor lookups. A hash map maps hash values to node metadata for O(1) access after the successor is found. Additional structures include a membership map for healthy nodes, mappings from physical nodes to their virtual nodes for efficient removal, and work queues for asynchronous rebalancing. In practice, many systems prefer a sorted array because lookups happen orders of magnitude more frequently than membership changes, making read performance more important than insertion cost.
### 19. What algorithms are used?

|Algorithm|Purpose|Complexity|
|---|---|---|
|Hashing|Map keys/nodes to hash space|O(1)|
|Sorting|Build the ring|O(N log N)|
|Binary Search|Find successor node|O(log N)|
|Wrap-around|Handle end of ring|O(1)|
|Virtual Node Generation|Improve load distribution|O(V) per node|
|Rebalancing|Move affected data|Depends on keys moved|
|Replication Placement|Choose replica nodes|O(R)|
Common hash functions include MD5, SHA-1 (for placement, not security), xxHash, MurmurHash, or CRC variants depending on performance and compatibility needs.

A production implementation uses multiple algorithms. A hash function maps both keys and nodes into the same hash space. The ring is built by sorting node hashes. During lookups, binary search finds the first node clockwise from the key's hash, with a wrap-around step if the search reaches the end of the ring. Virtual node generation improves load balancing by hashing multiple logical instances per physical node. When membership changes, a rebalancing algorithm identifies and migrates only the affected key ranges. Finally, replica selection walks clockwise to choose additional replica nodes while ensuring they reside on distinct physical machines.

Consistent hashing is an architecture composed of several algorithms—hashing, sorting, successor search, wrap-around, replication, and rebalancing—that together provide stable key placement in a dynamic distributed system.
### 20. What protocols are involved?
### 21. What synchronization happens?
### 22. What lifecycle events exist?

## Complexity (10)

### 24. Read complexity?
### 25. Write complexity?
### 26. Update complexity?
### 27. Delete complexity?
### 28. Search complexity?
### 29. Rebalancing complexity?
### 30. Memory complexity?
### 31. Network complexity?
### 32. Disk complexity?
### 33. Scalability bottleneck?

## Failure Modes (10)

### 35. Single node failure?
### 36. Dependency failure?
### 37. Network partition?
### 38. Hot partition?
### 39. Hot key?
### 40. Resource exhaustion?
### 41. Cascading failures?
### 42. Data corruption?
### 43. Retry storms?
### 44. Recovery strategy?

## Tradeoffs (10)

### 46. Why not use a simpler solution?
### 47. What alternatives exist?
### 48. Why was this chosen?
### 49. Latency vs consistency?
### 50. Cost vs performance?
### 51. Availability vs consistency?
### 52. Memory vs speed?
### 53. Operational simplicity vs flexibility?
### 54. Developer productivity vs control?
### 55. Future-proofing tradeoff?

## Production Thinking (10)

### 57. What should be separated?
### 58. What services are needed?
### 59. How is health monitored?
### 60. How is configuration managed?
### 61. How are failures isolated?
### 62. What retries are needed?
### 63. What timeouts are needed?
### 64. What fallback mechanisms exist?
### 65. What SLAs exist?
### 66. What SLOs exist?

## Observability (10)

### 68. What metrics matter?
### 69. What logs matter?
### 70. What traces matter?
### 71. How will debugging happen?
### 72. What alerts should exist?
### 73. What dashboards should exist?
### 74. What anomalies matter?
### 75. How do we detect hotspots?
### 76. How do we detect degradation?
### 77. How do we verify correctness?

## Scale Evolution (10)

### 79. 1 server design?
### 80. 10 server design?
### 81. 100 server design?
### 82. 1000 server design?
### 83. Global deployment?
### 84. Multi-region?
### 85. Disaster recovery?
### 86. Cost optimization?
### 87. Capacity planning?
### 88. Future growth concerns?

## Ownership Thinking (10)

### 90. Who owns this?
### 91. Who supports this?
### 92. How is onboarding done?
### 93. What documentation exists?
### 94. What runbooks exist?
### 95. What technical debt exists?
### 96. What would break first?
### 97. What would keep you awake at night?
### 98. What manual work can be automated?
### 99. What is the maintenance burden?

## Interview Readiness (10)

### 101. Can I explain it in 2 minutes?
### 102. Can I draw it?
### 103. Can I explain tradeoffs?
### 104. Can I explain failures?
### 105. Can I explain scaling?
### 106. Can I compare alternatives?
### 107. Can I discuss production concerns?
### 108. Can I discuss ownership concerns?
### 109. Can I discuss costs?
### 110. Can I redesign it under new constraints?

Staff Engineer Mental Model

Problem → Internals → Complexity → Failure → Tradeoffs → Production → Observability → Scale → Ownership → Communication