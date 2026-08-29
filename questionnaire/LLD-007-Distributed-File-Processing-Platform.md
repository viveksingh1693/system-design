# LLD-007: Distributed File-Processing Platform

- **Status:** Proposed
- **Date:** 2026-08-24
- **Scope:** Low-level design for a global, multi-tenant distributed file-processing platform

## 1. Component Architecture

```text
                         CLIENT
                            |
                            v
                    API Gateway / WAF
                            |
                            v
                      File API Service
                     /        |        \
                    /         |         \
                   v          v          v
             Metadata DB  Redis Cache  AuthZ
                   |
                   v
             Object Storage
                   |
                   v
             Upload Event
                   |
                   v
                 Kafka
                   |
                   v
            Workflow Engine
                   |
        +----------+----------+
        |          |          |
        v          v          v
      Scan       OCR       Transform
     Worker     Worker       Worker
        |          |          |
        +----------+----------+
                   |
                   v
              Output Storage
                   |
                   v
              Notification
```

## 2. Package Structure

```text
file-platform/
|
+-- api/
|   +-- FileController
|   +-- UploadController
|   +-- JobController
|   +-- DownloadController
|
+-- application/
|   +-- FileApplicationService
|   +-- UploadApplicationService
|   +-- ProcessingApplicationService
|   +-- JobApplicationService
|
+-- domain/
|   +-- File
|   +-- FileVersion
|   +-- ProcessingJob
|   +-- ProcessingStep
|   +-- UploadSession
|   +-- FileStatus
|   +-- JobStatus
|   +-- StepStatus
|
+-- workflow/
|   +-- FileProcessingWorkflow
|   +-- ScanActivity
|   +-- MetadataActivity
|   +-- OCRActivity
|   +-- TransformActivity
|   +-- ValidationActivity
|   +-- OutputActivity
|
+-- storage/
|   +-- ObjectStorage
|   +-- S3ObjectStorage
|   +-- AzureBlobStorage
|   +-- GcsObjectStorage
|
+-- worker/
|   +-- ProcessingWorker
|   +-- WorkerLeaseService
|   +-- WorkerHeartbeat
|
+-- messaging/
|   +-- FileEventPublisher
|   +-- JobEventConsumer
|   +-- OutboxPublisher
|
+-- repository/
|   +-- FileRepository
|   +-- JobRepository
|   +-- StepRepository
|   +-- UploadRepository
|   +-- OutboxRepository
|
+-- security/
|   +-- TenantAuthorizationService
|   +-- ObjectAccessPolicy
|
+-- idempotency/
|   +-- IdempotencyService
|
+-- retry/
|   +-- RetryPolicy
|   +-- DeadLetterService
|
+-- audit/
|   +-- AuditService
|
+-- observability/
|   +-- Metrics
|   +-- Tracing
|   +-- Logging
|
+-- notification/
|   +-- NotificationService
|
+-- reconciliation/
|   +-- StorageReconciliationJob
```

## 3. Domain Model

### File

```java
class File {
    UUID fileId;
    UUID tenantId;
    String fileName;
    String contentType;
    long sizeBytes;
    String checksum;
    String objectKey;
    String region;
    FileStatus status;
    long version;
    Instant createdAt;
    Instant updatedAt;
}
```

### File Status

```java
enum FileStatus {
    UPLOADING,
    UPLOADED,
    SCANNING,
    QUARANTINED,
    READY,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    DELETED
}
```

### UploadSession

```java
class UploadSession {
    UUID uploadId;
    UUID fileId;
    UUID tenantId;
    String objectKey;
    UploadStatus status;
    long totalSize;
    int expectedParts;
    int completedParts;
    Instant expiresAt;
}
```

### ProcessingJob

```java
class ProcessingJob {
    UUID jobId;
    UUID fileId;
    UUID tenantId;
    String workflowType;
    String workflowId;
    JobPriority priority;
    JobStatus status;
    int attempt;
    String errorCode;
    String errorMessage;
    Instant createdAt;
    Instant startedAt;
    Instant completedAt;
}
```

### ProcessingStep

```java
class ProcessingStep {
    UUID jobId;
    String stepId;
    long inputVersion;
    StepStatus status;
    int attempt;
    String workerId;
    Instant leaseUntil;
    Instant heartbeatAt;
    String outputObjectKey;
    String checksum;
    Instant startedAt;
    Instant completedAt;
}
```

## 4. Database Schema

### files

```sql
CREATE TABLE files (
    file_id        UUID PRIMARY KEY,
    tenant_id      UUID NOT NULL,
    file_name      VARCHAR(500) NOT NULL,
    content_type   VARCHAR(200),
    size_bytes     BIGINT NOT NULL,
    checksum       VARCHAR(128),
    object_key     VARCHAR(1000) NOT NULL,
    region         VARCHAR(50) NOT NULL,
    status         VARCHAR(40) NOT NULL,
    version        BIGINT NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL
);

CREATE INDEX idx_files_tenant_created
ON files(tenant_id, created_at DESC);

CREATE INDEX idx_files_tenant_status
ON files(tenant_id, status);
```

### upload_sessions

```sql
CREATE TABLE upload_sessions (
    upload_id       UUID PRIMARY KEY,
    file_id         UUID NOT NULL,
    tenant_id       UUID NOT NULL,
    object_key      VARCHAR(1000) NOT NULL,
    status          VARCHAR(40) NOT NULL,
    total_size      BIGINT NOT NULL,
    expected_parts  INTEGER NOT NULL,
    completed_parts INTEGER NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);
```

### processing_jobs

```sql
CREATE TABLE processing_jobs (
    job_id          UUID PRIMARY KEY,
    file_id         UUID NOT NULL,
    tenant_id       UUID NOT NULL,
    workflow_id     VARCHAR(200),
    workflow_type   VARCHAR(100) NOT NULL,
    priority        INTEGER NOT NULL,
    status          VARCHAR(40) NOT NULL,
    attempt         INTEGER NOT NULL,
    error_code      VARCHAR(100),
    error_message   VARCHAR(2000),
    created_at      TIMESTAMP NOT NULL,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP
);

CREATE INDEX idx_jobs_status_priority
ON processing_jobs(status, priority, created_at);

CREATE INDEX idx_jobs_tenant
ON processing_jobs(tenant_id, created_at DESC);
```

### processing_steps

```sql
CREATE TABLE processing_steps (
    job_id           UUID NOT NULL,
    step_id          VARCHAR(100) NOT NULL,
    input_version    BIGINT NOT NULL,
    status           VARCHAR(40) NOT NULL,
    attempt          INTEGER NOT NULL,
    worker_id        VARCHAR(200),
    lease_until      TIMESTAMP,
    heartbeat_at     TIMESTAMP,
    output_object    VARCHAR(1000),
    checksum         VARCHAR(128),
    started_at       TIMESTAMP,
    completed_at     TIMESTAMP,

    PRIMARY KEY(job_id, step_id, input_version)
);

CREATE INDEX idx_steps_lease
ON processing_steps(status, lease_until);
```

### outbox_events

```sql
CREATE TABLE outbox_events (
    event_id        UUID PRIMARY KEY,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(200) NOT NULL,
    event_version   INTEGER NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    published_at    TIMESTAMP
);

CREATE INDEX idx_outbox_unpublished
ON outbox_events(created_at)
WHERE published_at IS NULL;
```

### file_audit

```sql
CREATE TABLE file_audit (
    event_id       UUID PRIMARY KEY,
    file_id        UUID NOT NULL,
    tenant_id      UUID NOT NULL,
    actor_id       VARCHAR(200),
    action         VARCHAR(100) NOT NULL,
    metadata       JSONB,
    created_at     TIMESTAMP NOT NULL
);
```

## 5. API Design

```text
POST   /v1/files/uploads
POST   /v1/files/uploads/{uploadId}/complete
GET    /v1/files/{fileId}
GET    /v1/files/{fileId}/status
GET    /v1/files/{fileId}/download

POST   /v1/files/{fileId}/process
GET    /v1/jobs/{jobId}
POST   /v1/jobs/{jobId}/retry
POST   /v1/jobs/{jobId}/cancel

DELETE /v1/files/{fileId}
```

### Create Upload

```http
POST /v1/files/uploads
```

```json
{
  "fileName": "invoice.pdf",
  "contentType": "application/pdf",
  "size": 10485760,
  "checksum": "sha256..."
}
```

Flow:

```text
Validate -> Authenticate -> Authorize -> Create File
-> Create UploadSession -> Generate Signed URL(s)
-> Return Upload Information
```

### Processing Request

```http
POST /v1/files/{fileId}/process
```

```json
{
  "workflow": "DOCUMENT_OCR",
  "priority": "HIGH",
  "outputFormat": "JSON"
}
```

Response:

```json
{
  "jobId": "JOB-123",
  "workflowId": "WF-123",
  "status": "QUEUED"
}
```

Return `202 Accepted` because processing is asynchronous.

## 6. Multipart Upload Flow

```text
Create Multipart Upload
        |
        v
Generate Part URLs
        |
        v
Client Uploads Parts
        |
        v
Track Completed Parts
        |
        v
Complete Multipart Upload
        |
        v
Verify Size / Checksum
        |
        v
File = UPLOADED
```

The server does not receive the file bytes.

## 7. Upload Completion Transaction

```text
BEGIN
  Validate upload session
  Verify object-store completion
  Update file: UPLOADING -> UPLOADED
  Insert FileUploaded outbox event
  Insert audit event
COMMIT
```

Because object storage is external, a reconciliation job handles crashes between object completion and database updates.

## 8. Workflow

Example:

```text
DocumentOcrWorkflow

Validate
   |
VirusScan
   |
MetadataExtraction
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

A durable workflow engine owns the state.

## 9. Activity Contract

```java
interface ProcessingActivity {
    ActivityResult execute(ProcessingContext context);
}

class ProcessingContext {
    UUID tenantId;
    UUID fileId;
    UUID jobId;
    String stepId;
    long inputVersion;
    String inputObjectKey;
    String traceId;
}
```

Each activity has:

```text
Timeout
Retry Policy
Idempotency
Tracing
Metrics
```

## 10. Worker Lease Algorithm

Conceptually:

```sql
UPDATE processing_steps
SET worker_id = :worker,
    status = 'RUNNING',
    lease_until = :leaseUntil,
    heartbeat_at = CURRENT_TIMESTAMP
WHERE job_id = :jobId
  AND step_id = :stepId
  AND input_version = :version
  AND status IN ('PENDING', 'RETRYABLE')
  AND (lease_until IS NULL OR lease_until < CURRENT_TIMESTAMP);
```

If one row is updated, the worker owns the lease.

## 11. Heartbeat

```sql
UPDATE processing_steps
SET heartbeat_at = CURRENT_TIMESTAMP,
    lease_until = :newLease
WHERE job_id = :jobId
  AND step_id = :stepId
  AND worker_id = :workerId
  AND status = 'RUNNING';
```

If the worker dies, the lease eventually expires.

## 12. Idempotent Step Execution

Logical identity:

```text
(jobId, stepId, inputVersion)
```

Flow:

```text
Lookup Step
   |
   +-- COMPLETED -> Return stored output
   |
   +-- Otherwise -> Claim -> Process -> Write Output -> Commit
```

Use deterministic output keys:

```text
tenant/{tenantId}/jobs/{jobId}/steps/{stepId}/v{inputVersion}/output
```

## 13. Output Commit Ordering

Correct:

```text
Process
   |
   v
Write Output
   |
   v
Verify Checksum
   |
   v
DB: Step = COMPLETED
```

Incorrect:

```text
DB = COMPLETED
   |
   v
Write Output
   |
   X
```

The database must never claim completion before durable output exists.

## 14. Chunk Processing

For chunkable workloads:

```text
File
 |
 +-- Chunk 1
 +-- Chunk 2
 +-- Chunk 3
 ...
 +-- Chunk N
```

Each chunk has:

```text
jobId
stepId
chunkId
offset
length
checksum
status
```

Workers process chunks independently.

Merge validates:

```text
All chunks exist
Checksums are valid
Versions match
No chunk is duplicated
```

## 15. Retry

Use:

```text
Exponential Backoff
+
Jitter
+
Maximum Attempts
```

Example:

```text
Attempt 1 -> short delay
Attempt 2 -> 1s + jitter
Attempt 3 -> 5s + jitter
Attempt 4 -> 30s + jitter
Attempt 5 -> 5m + jitter
```

Permanent errors skip retry:

```text
INVALID_FILE
UNSUPPORTED_FORMAT
VIRUS_DETECTED
AUTHORIZATION_FAILURE
```

## 16. DLQ

```text
Worker -> Failure -> Retry Policy
                    |
                    +-- retryable -> Retry
                    |
                    +-- max attempts -> DLQ
```

DLQ records should include:

```text
jobId
fileId
stepId
attempt
errorCode
lastWorker
failedAt
```

Operators can inspect, replay or cancel jobs.

## 17. Cancellation

```text
Client
 |
POST /jobs/{id}/cancel
 |
 v
Workflow
 |
 v
Cancellation Signal
 |
 v
Worker
 |
 v
Stop at safe checkpoint
```

Cancellation should be cooperative.

## 18. Priority and Backpressure

Priority classes:

```text
P0 Critical
P1 High
P2 Normal
P3 Batch
```

Possible queues:

```text
file-processing-critical
file-processing-high
file-processing-normal
file-processing-batch
```

When queue depth grows:

```text
Queue Depth ↑ -> Autoscaler -> Worker Replicas ↑
```

If capacity is exhausted:

```text
Admission Control
Tenant Quotas
Rate Limiting
Priority Scheduling
```

## 19. Autoscaling Signals

Use:

```text
Queue depth
Kafka lag
Oldest job age
Processing latency
CPU
Memory
GPU utilization
Jobs/sec
```

Kubernetes implementation can use KEDA + HPA.

## 20. Tenant Quotas

Example:

```text
Tenant A:
  Concurrent jobs = 100
  Storage = 10 TB
  Upload bandwidth = 1 Gbps

Tenant B:
  Concurrent jobs = 10
  Storage = 1 TB
  Upload bandwidth = 100 Mbps
```

Quota enforcement prevents noisy-neighbor problems.

## 21. Object Key Design

```text
tenant/{tenantId}/
  files/{fileId}/
    versions/{version}/
      original
      metadata.json
      scan/result.json
      ocr/result.json
      transform/output.pdf
```

## 22. Event Topics

```text
file.events
file.uploads
file.processing
file.processing.retry
file.processing.dlq
file.notifications
file.audit
```

Events:

```text
FileUploaded
FileScanCompleted
FileQuarantined
FileReady
ProcessingStarted
ProcessingCompleted
ProcessingFailed
FileDeleted
```

Kafka key:

```text
fileId
```

when per-file ordering is required.

## 23. Event Consumer Idempotency

```sql
CREATE TABLE consumed_events (
    consumer_name VARCHAR(200) NOT NULL,
    event_id      UUID NOT NULL,
    processed_at  TIMESTAMP NOT NULL,
    PRIMARY KEY(consumer_name, event_id)
);
```

Duplicate events are ignored after successful processing.

## 24. Notification

Processing completion should not block the workflow:

```text
ProcessingCompleted
       |
       v
Kafka
       |
       v
Notification Service
       |
 +-----+------+------+
 |            |      |
Email       Webhook  Push
```

Notifications have independent retries.

## 25. Caching

Redis may cache:

```text
File metadata
Tenant quotas
Workflow configuration
Authorization policy
Job status
```

Redis is never the source of truth.

## 26. Storage Reconciliation

Periodic reconciliation checks:

```text
Object Storage
      |
      v
Compare with Metadata DB
      |
      +-- Object exists, DB missing -> Recover/Quarantine
      +-- DB says UPLOADING, object complete -> Investigate/Recover
      +-- DB object missing -> Alert/Repair
```

## 27. Failure Handling

| Failure | Handling |
|---|---|
| Upload interruption | Resume multipart upload |
| Corrupt upload | Checksum validation |
| Virus detected | Quarantine |
| Worker crash | Lease timeout + retry |
| Temporary storage failure | Retry |
| Invalid file | Permanent failure |
| Processing timeout | Retry/terminate |
| Kafka unavailable | Outbox retry |
| Duplicate event | Idempotent consumer |
| Duplicate job | Step idempotency |
| Workflow failure | Durable recovery |
| Output write failure | Retry |
| Region failure | Regional failover/replay where permitted |
| Poison job | DLQ |

## 28. Failure: Worker Crash

```text
Job RUNNING
     |
     v
Worker A
     |
     X crash
     |
     v
Lease expires
     |
     v
Worker B claims
     |
     v
Check idempotency
     |
     v
Resume/reuse output
```

## 29. Failure: Output Exists but DB Commit Fails

```text
Output written
     |
     X DB commit fails
     |
     v
Retry
     |
     v
Check deterministic output key
     |
     v
Verify checksum
     |
     v
Commit metadata
```

## 30. Failure: Kafka Outage

The application transaction still commits:

```text
DB state update
+
Outbox event
```

The outbox publisher retries when Kafka becomes available.

## 31. Failure: Region Outage

If policy permits:

```text
Region A failure
      |
      v
Region B workers
      |
      v
Process replicated objects
```

Otherwise the regional backlog waits for recovery.

## 32. Security Flow

```text
Request
   |
Authentication
   |
Tenant Authorization
   |
Resource Authorization
   |
Operation
```

Every object access checks tenant ownership.

Virus scanner workers should have restricted network access, limited permissions, resource limits and isolated execution.

## 33. Observability

### Upload

```text
Upload rate
Upload error rate
Upload duration
Multipart failure rate
```

### Processing

```text
Jobs/sec
P50/P95/P99 latency
Success rate
Failure rate
Retry rate
DLQ count
```

### Queue

```text
Queue depth
Oldest job age
Kafka lag
```

### Infrastructure

```text
CPU
Memory
GPU
Pod restarts
Storage I/O
Network
```

### Tenant

```text
Quota usage
Concurrent jobs
Storage usage
SLA compliance
```

## 34. Distributed Tracing

Trace:

```text
Upload Request
    |
    v
File API
    |
    v
Object Storage
    |
    v
Kafka
    |
    v
Workflow
    |
    +-- Scan
    +-- OCR
    +-- Transform
    |
    v
Output Storage
```

Propagate:

```text
traceId
correlationId
fileId
jobId
workflowId
tenantId
```

Never put file contents or secrets into logs.

## 35. Audit

Audit actions:

```text
FILE_CREATED
UPLOAD_STARTED
UPLOAD_COMPLETED
FILE_SCANNED
FILE_QUARANTINED
PROCESSING_STARTED
PROCESSING_COMPLETED
PROCESSING_FAILED
FILE_DOWNLOADED
FILE_DELETED
```

Audit records are append-only.

## 36. Retention

```text
Original Files      -> business retention
Temporary Chunks    -> short TTL
Intermediate Output -> workflow-specific TTL
Final Output        -> business retention
Audit               -> compliance retention
DLQ                 -> operational retention
```

Use object-storage lifecycle policies.

## 37. Consistency Model

Strong consistency:

```text
File ownership
Tenant authorization
Job identity
Step idempotency
State transitions
```

Eventual consistency:

```text
Search
Analytics
Notifications
Dashboard aggregates
Non-critical caches
```

## 38. Core Invariants

```text
1. A file belongs to exactly one tenant.

2. A processing step has one logical identity:
   (jobId, stepId, inputVersion).

3. A completed step must have durable output.

4. A quarantined file cannot be normally downloaded.

5. A worker must hold a valid lease before mutating a running step.

6. A job cannot transition through an invalid state.

7. Duplicate events cannot create duplicate business effects.

8. Metadata must never claim COMPLETED before durable output exists.

9. Large file bytes must not flow through the API service.

10. Redis/cache is never the sole source of truth.
```

## 39. Final LLD

```text
                      +----------------------+
                      |      API Gateway     |
                      +----------+-----------+
                                 |
                                 v
                      +----------------------+
                      |    File API Service  |
                      +----------+-----------+
                                 |
                +----------------+----------------+
                |                                 |
                v                                 v
        +---------------+                 +---------------+
        |  Metadata DB  |                 | Object Store  |
        +---------------+                 +---------------+
                |                                 |
                +----------------+----------------+
                                 |
                                 v
                              Kafka
                                 |
                                 v
                        +----------------+
                        | Workflow Engine|
                        +-------+--------+
                                |
          +---------------------+---------------------+
          |                     |                     |
          v                     v                     v
   +-------------+       +-------------+       +-------------+
   | Scan Worker |       | OCR Worker  |       | Transform   |
   |     ×N      |       |     ×N      |       | Worker ×N   |
   +------+------+       +------+------+       +------+------+
          |                     |                     |
          +---------------------+---------------------+
                                |
                                v
                         Output Storage
                                |
                                v
                          Validation
                                |
                                v
                         Processing DB
                                |
                                v
                           Kafka Events
                                |
                   +------------+------------+
                   |                         |
                   v                         v
             Notification                 Analytics
```

## 40. Interview-Ready Answer

> **I would separate the control plane from the data plane. File metadata, jobs and workflow state belong to the control plane, while object storage and processing workers form the data plane. Large files should never flow through application servers; clients upload directly to object storage using pre-signed multipart URLs.**
>
> **Once the upload completes, an event starts a durable workflow. I would use Kafka for event transport and a workflow engine such as Temporal for long-running orchestration, retries, timers and recovery. Processing workers would be specialized by workload and scale independently based on queue depth, lag and job age.**
>
> **Every processing step would be idempotent because at-least-once delivery and worker crashes can cause duplicate execution. I would use `(jobId, stepId, inputVersion)` as the logical step identity, and worker leases/heartbeats would recover abandoned jobs. Large files would be streamed or chunked instead of loaded into memory.**
>
> **Uploaded files remain untrusted until malware scanning succeeds. Tenant isolation would be enforced at the API, database, object-storage and worker layers. Downloads would use short-lived signed URLs.**
>
> **Finally, I would use retries with exponential backoff, a DLQ for poison jobs, transactional outbox for reliable events, output versioning, storage reconciliation and distributed tracing. For global deployment, I would process files close to their storage region to reduce latency, cross-region bandwidth and egress costs while respecting data-residency requirements.**
