# ADR-007: Distributed File-Processing Platform Architecture

- **Status:** Accepted
- **Date:** 2026-08-24
- **Scope:** Global, multi-tenant distributed file ingestion and processing platform

## Context

The platform must support large-file ingestion and asynchronous processing for document processing, OCR, image/video transformation, metadata extraction, compression, conversion, malware scanning, and batch processing.

Files can range from KB to TB and processing may take milliseconds to hours. The platform must provide:

- High throughput and horizontal scalability
- Large-file and resumable-upload support
- Fault tolerance and recovery
- Idempotent processing
- Multi-tenant isolation
- Security and malware protection
- Multi-region processing and data locality
- Cost efficiency
- Strong observability

## Decision

We will use the following architecture:

```text
                           CLIENT
                              |
                              v
                       API GATEWAY / WAF
                              |
                              v
                        FILE API SERVICE
                              |
                    +---------+---------+
                    |                   |
                    v                   v
                Metadata DB       Pre-Signed URL
                                        |
                                        v
                                 OBJECT STORAGE
                                        |
                                        v
                                  FileUploaded
                                        |
                                        v
                                      Kafka
                                        |
                                        v
                                 WORKFLOW ENGINE
                                        |
              +-------------------------+-------------------------+
              |                         |                         |
              v                         v                         v
         Virus Workers            OCR Workers              Transform Workers
              |                         |                         |
              +-------------------------+-------------------------+
                                        |
                                        v
                                  OUTPUT STORAGE
                                        |
                                        v
                                    VALIDATION
                                        |
                                        v
                                    COMPLETED
                                        |
                                        v
                                  NOTIFICATION
```

### Core decisions

1. Object storage is the system of record for file bytes.
2. Metadata DB stores file/job metadata, not large binaries.
3. Clients upload directly using short-lived pre-signed URLs.
4. Large uploads use multipart/resumable upload.
5. Control plane is separated from data plane.
6. Kafka handles durable event transport.
7. Temporal or equivalent durable workflow engine handles workflow state, retries, timers and recovery.
8. Processing uses specialized horizontally scalable worker pools.
9. Workers stream or chunk files instead of loading entire files into memory.
10. Every processing step is idempotent.
11. Worker leases/heartbeats recover abandoned jobs.
12. Bounded retries and DLQ handle poison jobs.
13. Files remain untrusted until malware scanning succeeds.
14. Tenant identity is enforced through metadata, jobs, events, object paths and authorization.
15. Processing occurs close to storage where possible.
16. Downloads use short-lived signed URLs.
17. Audit events and distributed tracing provide accountability.

## Control Plane vs Data Plane

### Control Plane

```text
File metadata
Processing jobs
Workflow state
Tenant configuration
Authorization
Policies
Processing status
Retries
Audit
```

### Data Plane

```text
File bytes
Scanning
OCR
Transformation
Compression
Encoding
Chunk processing
Output generation
```

## Object Storage

Use S3, Azure Blob Storage, or GCS for binary content.

The database stores:

```text
fileId
tenantId
objectKey
region
size
checksum
contentType
status
version
```

The object store stores the actual bytes.

### Rejected

Storing large files directly in PostgreSQL/MySQL because it increases database size, replication/backup cost, and contention with transactional workloads.

## Direct Upload

Application servers do not proxy large files:

```text
Client -> File API -> Signed URL
Client -> Object Storage
```

This reduces API bandwidth and improves scalability.

## Multipart Upload

Large files are split into parts:

```text
File
 |
 +-- Part 1
 +-- Part 2
 +-- Part 3
 ...
 +-- Part N
```

Parts can be uploaded concurrently and retried independently.

## File Lifecycle

```text
UPLOADING
    |
    v
UPLOADED
    |
    v
SCANNING
    |
 +--+---------+
 |            |
 v            v
CLEAN      QUARANTINED
 |
 v
READY
 |
 v
PROCESSING
 |
 +---------+
 |         |
 v         v
COMPLETED FAILED
```

## Workflow

Example:

```text
Validate
   |
VirusScan
   |
ExtractMetadata
   |
OCR
   |
Transform
   |
ValidateOutput
   |
PublishOutput
   |
Notify
```

Kafka provides event transport, buffering, fan-out and replay. The workflow engine provides durable state, dependencies, retries, timers, timeouts and recovery.

## Worker Architecture

```text
                     JOB QUEUE
                         |
          +--------------+--------------+
          |              |              |
          v              v              v
      Scan Workers    OCR Workers   Transform Workers
          ×N              ×N             ×N
```

Use separate CPU, memory-optimized, GPU and batch worker pools where appropriate.

## Idempotency

Processing assumes at-least-once delivery.

Logical step identity:

```text
jobId + stepId + inputVersion
```

Use a uniqueness constraint to prevent duplicate business effects.

## Worker Lease

Workers maintain:

```text
workerId
leaseUntil
heartbeatAt
```

If a lease expires, another worker may recover the step using optimistic concurrency.

## Retry and DLQ

Transient failures use exponential backoff and jitter.

Permanent failures such as invalid formats or malware are not retried.

After maximum attempts:

```text
Job -> DLQ
```

The DLQ must support inspection, replay, cancellation and repair.

## Security

Uploaded files are untrusted until scanning succeeds:

```text
UPLOADED -> SCANNING -> READY
                    |
                    +-> QUARANTINED
```

Use encryption, IAM, bucket policies, short-lived signed URLs, tenant isolation and audit logging.

## Multi-Region

Prefer:

```text
Region A Storage -> Region A Workers
Region B Storage -> Region B Workers
Region C Storage -> Region C Workers
```

This reduces latency and egress while supporting data-residency requirements.

## Alternatives Considered

### Database Blob Storage

Rejected because object storage is better suited to massive binary data.

### Synchronous Processing

Rejected because long-running jobs cause timeouts, connection exhaustion and poor failure recovery.

### Kafka as Workflow Engine

Rejected because Kafka is an event transport system, not a complete durable workflow engine.

### One Generic Worker Pool

Rejected because CPU, memory and GPU workloads require different resource profiles.

### Application-Server File Proxying

Rejected because it creates a bandwidth and scalability bottleneck.

## Consequences

### Positive

- Large-file friendly
- Horizontally scalable
- Resumable
- Fault tolerant
- Strong tenant isolation
- Independent worker scaling
- Durable workflows
- Good cost profile
- Strong observability

### Negative

- More infrastructure
- Workflow-engine complexity
- Eventual consistency
- More complex debugging
- Requires idempotent workers
- Requires DLQ/replay tooling
- Multi-region data locality adds operational complexity

## Architectural Principle

> Treat file bytes as durable data, processing as a recoverable workflow, and every processing step as an idempotent distributed operation.
