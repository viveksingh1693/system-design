# ADR-009: Media Processing Platform — Low-Level Design

**Status:** Accepted  
**Date:** 2026-08-29  
**Related ADR:** ADR-008  
**Scope:** API contracts, data model, events, workers, idempotency, retries, state machines and processing execution

---

# 1. Component Model

```text
                    +----------------+
                    | API Gateway    |
                    +-------+--------+
                            |
              +-------------+-------------+
              |                           |
              v                           v
      Upload Service              Media Service
              |                           |
              v                           v
       Metadata DB <------------- Job DB
              |
              v
       Object Storage
              |
              v
         Event Publisher
              |
              v
            Kafka
              |
              v
       Workflow Engine
              |
       +------+------+
       |             |
       v             v
   Job Queue      Job Queue
       |             |
       v             v
 CPU Workers      GPU Workers
       |             |
       +------+------+
              |
              v
       Processed Storage
              |
              v
             CDN
```

---

# 2. Core Entities

## Asset

```text
Asset
-----
assetId
tenantId
ownerId
createdAt
status
currentVersion
```

## AssetVersion

```text
AssetVersion
------------
assetId
version
sourceUri
checksum
size
contentType
createdAt
status
```

## ProcessingJob

```text
ProcessingJob
-------------
jobId
assetId
assetVersion
workflowId
status
priority
createdAt
updatedAt
```

## ProcessingStep

```text
ProcessingStep
--------------
jobId
stepId
operation
status
attempt
workerId
inputVersion
outputUri
errorCode
startedAt
completedAt
```

## IdempotencyRecord

```text
IdempotencyRecord
-----------------
tenantId
idempotencyKey
requestHash
status
resourceId
response
createdAt
expiresAt
```

---

# 3. Database Schema

```sql
CREATE TABLE assets (
    asset_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    current_version INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

```sql
CREATE TABLE asset_versions (
    asset_id UUID NOT NULL,
    version INTEGER NOT NULL,
    source_uri TEXT NOT NULL,
    checksum VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    content_type VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    PRIMARY KEY(asset_id, version)
);
```

```sql
CREATE TABLE processing_jobs (
    job_id UUID PRIMARY KEY,
    asset_id UUID NOT NULL,
    asset_version INTEGER NOT NULL,
    workflow_id VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL,
    priority INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

```sql
CREATE TABLE processing_steps (
    job_id UUID NOT NULL,
    step_id VARCHAR(100) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt INTEGER NOT NULL,
    worker_id VARCHAR(200),
    output_uri TEXT,
    error_code VARCHAR(100),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,

    PRIMARY KEY(job_id, step_id)
);
```

```sql
CREATE TABLE idempotency_records (
    tenant_id UUID NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL,
    resource_id UUID,
    response JSONB,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,

    PRIMARY KEY(tenant_id, idempotency_key)
);
```

---

# 4. Upload API

```http
POST /v1/assets
Authorization: Bearer <token>
Idempotency-Key: UPLOAD-123
Content-Type: application/json
```

Request:

```json
{
  "fileName": "video.mp4",
  "contentType": "video/mp4",
  "size": 5368709120
}
```

Response:

```http
201 Created
```

```json
{
  "assetId": "A123",
  "uploadId": "U456",
  "uploadUrl": "...",
  "expiresAt": "..."
}
```

The returned URL is used for direct multipart upload.

---

# 5. Complete Upload API

```http
POST /v1/assets/{assetId}/upload/complete
Idempotency-Key: COMPLETE-A123
```

The service verifies:

```text
Upload exists
+
Expected parts exist
+
Checksum
+
Authorization
```

Then:

```text
UPLOADING
    |
    v
UPLOADED
```

---

# 6. Processing API

```http
POST /v1/assets/{assetId}/process
Idempotency-Key: PROCESS-A123-V1
```

Request:

```json
{
  "profiles": [
    "360P",
    "720P",
    "1080P"
  ],
  "generateThumbnail": true,
  "extractMetadata": true,
  "moderate": true
}
```

Response:

```http
202 Accepted
```

```json
{
  "jobId": "J123",
  "status": "QUEUED"
}
```

---

# 7. Job Status API

```http
GET /v1/jobs/{jobId}
```

Response:

```json
{
  "jobId": "J123",
  "status": "PROCESSING",
  "progress": 62,
  "steps": [
    {
      "name": "METADATA",
      "status": "COMPLETED"
    },
    {
      "name": "THUMBNAIL",
      "status": "COMPLETED"
    },
    {
      "name": "TRANSCODE_720P",
      "status": "PROCESSING"
    }
  ]
}
```

---

# 8. Job State Machine

```text
                 +-------------+
                 |             |
                 v             |
QUEUED → RUNNING → COMPLETED   |
   |        |                   |
   |        v                   |
   |      FAILED                |
   |        |                   |
   |        v                   |
   +------ RETRY --------------+
```

More detailed:

```text
QUEUED
  |
  v
VALIDATING
  |
  v
PROCESSING
  |
  +------> FAILED_RETRYABLE
  |              |
  |              v
  |            QUEUED
  |
  +------> FAILED_PERMANENT
  |
  v
COMPLETED
```

---

# 9. Processing Step State Machine

```text
PENDING
   |
   v
RUNNING
   |
   +------> RETRY
   |          |
   |          v
   |       RUNNING
   |
   +------> FAILED
   |
   v
COMPLETED
```

---

# 10. Media Uploaded Event

```json
{
  "eventId": "EVT-123",
  "eventType": "MEDIA_UPLOADED",
  "version": 1,
  "timestamp": "2026-08-29T10:00:00Z",
  "tenantId": "T1",
  "assetId": "A123",
  "assetVersion": 1,
  "sourceUri": "s3://raw/T1/A123/v1/source.mp4",
  "checksum": "SHA256..."
}
```

---

# 11. Processing Command

```json
{
  "jobId": "J123",
  "assetId": "A123",
  "assetVersion": 1,
  "operation": "TRANSCODE",
  "profile": "720P",
  "attempt": 1
}
```

---

# 12. Worker Processing

```text
Queue
  |
  v
Worker
  |
  +--> Acquire lease
  |
  +--> Validate job
  |
  +--> Check idempotency
  |
  +--> Download/stream input
  |
  +--> Execute FFmpeg
  |
  +--> Validate output
  |
  +--> Upload output
  |
  +--> Update DB
  |
  +--> Complete job
```

---

# 13. Transcoding Command

Conceptually:

```text
ffmpeg
  -i input.mp4
  -vf scale=1280:720
  -c:v h264
  -c:a aac
  output.mp4
```

The actual worker should construct commands from validated profiles rather than accepting arbitrary client-provided command-line arguments.

---

# 14. Deterministic Output

For:

```text
job = J123
operation = TRANSCODE
version = 1
profile = 720P
```

output:

```text
/assets/A123/v1/transcoded/720p/output.mp4
```

Idempotency key:

```text
J123:TRANSCODE:V1:720P
```

If the output already exists and passes validation:

```text
SKIP PROCESSING
```

and mark the step:

```text
COMPLETED
```

---

# 15. Worker Lease

Worker obtains:

```text
jobId = J123
workerId = W456
leaseUntil = T + 5 minutes
fencingToken = 1024
```

Worker periodically renews the lease.

If it stops:

```text
lease expires
```

another worker can claim the job.

---

# 16. Fencing

Suppose:

```text
Worker A → token 1024
Worker B → token 1025
```

Worker A becomes stale.

When it tries to update:

```text
UPDATE processing_steps
...
WHERE job_id = ?
AND fencing_token >= 1024
```

the newer worker's token prevents stale updates.

The exact implementation can use a monotonically increasing version/token maintained by the job/lease store.

---

# 17. Retry Policy

Retryable:

```text
NETWORK_TIMEOUT
STORAGE_TIMEOUT
TEMPORARY_DB_FAILURE
WORKER_CRASH
TRANSIENT_SERVICE_ERROR
```

Non-retryable:

```text
CORRUPT_MEDIA
UNSUPPORTED_CODEC
INVALID_FORMAT
POLICY_VIOLATION
INVALID_CONFIGURATION
```

Retry:

```text
attempt 1 → 1 sec
attempt 2 → 2 sec
attempt 3 → 4 sec
attempt 4 → 8 sec
```

Use jitter to avoid synchronized retries.

---

# 18. Dead Letter Queue

After maximum retries:

```text
Processing Queue
       |
       v
Retry
       |
       v
Retry
       |
       v
Retry limit
       |
       v
DLQ
```

DLQ message:

```json
{
  "jobId": "J123",
  "assetId": "A123",
  "stepId": "TRANSCODE_720P",
  "attempts": 5,
  "errorCode": "ENCODER_FAILURE",
  "lastError": "...",
  "timestamp": "..."
}
```

---

# 19. Outbox Pattern

When job state and event publication must be coordinated:

```text
BEGIN

UPDATE processing_job
SET status = 'COMPLETED';

INSERT INTO outbox_events (...);

COMMIT;
```

Then:

```text
Outbox
   |
   v
Publisher
   |
   v
Kafka
```

This prevents:

```text
DB updated
+
Event lost
```

---

# 20. Idempotent Consumer

Every consumer checks:

```text
eventId
```

or a business-level operation identity.

```text
Receive event
      |
      v
Already processed?
   /          \
 YES           NO
  |             |
Ignore       Process
                |
                v
        Record processed event
```

---

# 21. Metadata Worker

Input:

```text
sourceUri
```

Output:

```json
{
  "durationMs": 632000,
  "width": 1920,
  "height": 1080,
  "codec": "h264",
  "fps": 30,
  "bitrate": 5000000,
  "audioChannels": 2
}
```

Metadata is stored in the transactional database.

---

# 22. Thumbnail Worker

Generate:

```text
thumbnail.jpg
preview.jpg
sprite.jpg
```

Example:

```text
/assets/A123/v1/thumbnails/thumbnail.jpg
/assets/A123/v1/thumbnails/preview.jpg
/assets/A123/v1/thumbnails/sprite.jpg
```

---

# 23. Streaming Package

For streaming:

```text
Original
   |
   v
Transcoding
   |
   v
Segments
   |
   v
Packaging
   |
   +--> HLS
   |
   +--> DASH
```

Output:

```text
master.m3u8
720p/index.m3u8
720p/segment001.ts
720p/segment002.ts
...
```

---

# 24. Progress Calculation

Overall progress can be calculated from weighted steps.

Example:

```text
Validation      5%
Metadata       10%
Thumbnail       5%
Moderation     15%
Transcoding    50%
Packaging      15%
```

If:

```text
Validation       = 100%
Metadata         = 100%
Thumbnail        = 100%
Moderation       = 100%
Transcoding      = 40%
Packaging        = 0%
```

overall progress:

```text
5 + 10 + 5 + 15 + 20 = 55%
```

Weights should be configurable because processing time differs significantly by media type and profile.

---

# 25. Priority

Jobs can have:

```text
P0 → Premium / interactive
P1 → Normal
P2 → Batch
P3 → Reprocessing
```

Queues:

```text
P0 Queue
P1 Queue
P2 Queue
P3 Queue
```

Workers prioritize according to business policy while maintaining fairness so low-priority workloads cannot starve indefinitely.

---

# 26. Multi-Tenant Isolation

Every entity contains:

```text
tenantId
```

Storage:

```text
tenant/T1/...
tenant/T2/...
```

Authorization must verify:

```text
Authenticated User
        |
        v
Tenant Membership
        |
        v
Resource Ownership
```

Tenant quotas:

```text
maxStorage
maxConcurrentJobs
maxUploadSize
maxGPUHours
```

---

# 27. Security Pipeline

Before processing:

```text
Upload
  |
  v
File Validation
  |
  v
Malware Scan
  |
  v
Media Parser
  |
  v
Processing
```

Validation should inspect:

```text
Actual container
Codec
File structure
Size
Duration
Resolution
Declared MIME type
```

Do not execute arbitrary user-provided media-processing commands.

---

# 28. Observability

Every request/job carries:

```text
traceId
requestId
tenantId
assetId
jobId
stepId
workerId
```

Example log:

```text
INFO
traceId=abc
jobId=J123
assetId=A123
step=TRANSCODE
profile=720P
worker=W456
status=COMPLETED
durationMs=82340
```

Metrics:

```text
media.upload.rate
media.processing.rate
media.processing.duration
media.processing.failure
media.queue.depth
media.queue.age
media.retry.rate
media.dlq.count
media.worker.cpu
media.worker.gpu
```

---

# 29. Failure Scenarios

## Worker crashes

```text
Worker A
   |
   X
Crash
   |
   v
Lease expires
   |
   v
Worker B
   |
   v
Idempotency check
   |
   v
Resume/reprocess
```

## Storage temporarily unavailable

```text
Worker
 |
 v
Storage
 |
 X
 |
 v
Retry with backoff
```

## Kafka unavailable

The source media remains durable in object storage and processing state remains durable in the database/workflow engine. Events can be republished/reconciled.

## Database unavailable

Processing pauses or retries depending on the operation. Workers should not mark work successfully completed without durable state.

---

# 30. Reprocessing

Suppose a new codec/profile is introduced.

Do not mutate the original source:

```text
A123
 |
 +-- v1 source
 +-- v1 old outputs
 +-- v1 new outputs
```

or create an explicit processing-version identifier:

```text
processingVersion = 2
```

This allows controlled reprocessing and rollback.

---

# 31. Content Deduplication

Calculate:

```text
SHA-256(source)
```

If identical content already exists:

```text
same checksum
```

the platform may reuse the underlying immutable content or processing results subject to tenant/security/retention policy.

Logical assets remain separate.

---

# 32. API Error Model

Standardized response:

```json
{
  "error": {
    "code": "MEDIA_UNSUPPORTED_CODEC",
    "message": "The uploaded media uses an unsupported codec.",
    "requestId": "REQ-123"
  }
}
```

Examples:

```text
MEDIA_TOO_LARGE
MEDIA_INVALID_FORMAT
MEDIA_UNSUPPORTED_CODEC
MEDIA_PROCESSING_FAILED
MEDIA_NOT_FOUND
MEDIA_UNAUTHORIZED
IDEMPOTENCY_CONFLICT
```

---

# 33. Rate Limiting

Apply limits at:

```text
Tenant
User
API
IP
```

Example:

```text
Upload API
Process API
Status API
```

Processing quotas should be separate from API rate limits.

---

# 34. HLD → LLD Mapping

```text
HLD Component          LLD Implementation
------------------------------------------------
API Gateway            REST APIs
Object Storage         Multipart Upload
Event Bus              Kafka Events
Workflow               Workflow State Machine
Worker Pool            CPU/GPU Workers
Database               PostgreSQL Tables
Idempotency            Unique Constraints
Job Ownership          Lease + Fencing
Reliability            Retry + DLQ
Events                 Outbox
Delivery               CDN + Signed URLs
Observability          Metrics + Logs + Traces
```

---

# 35. Final LLD Flow

```text
                    CLIENT
                       |
                       v
                 POST /assets
                       |
                       v
               Upload Service
                       |
                       v
                Create Asset
                       |
                       v
              Generate Upload URL
                       |
                       v
                 Object Storage
                       |
                       v
              Multipart Upload
                       |
                       v
                Upload Complete
                       |
                       v
             MEDIA_UPLOADED EVENT
                       |
                       v
                 Workflow Engine
                       |
       +---------------+---------------+
       |               |               |
       v               v               v
   Validation       Metadata       Moderation
       |               |               |
       +---------------+---------------+
                       |
                       v
                  Transcoding
                       |
             +---------+---------+
             |                   |
             v                   v
         CPU Workers         GPU Workers
             |                   |
             +---------+---------+
                       |
                       v
                 Output Storage
                       |
                       v
                   Packaging
                       |
                       v
                      CDN
                       |
                       v
                    CLIENT
```

---

# 36. Final Architectural Principles

The implementation should follow these principles:

1. **Never route large media through API servers.**
2. **Use object storage for media bytes.**
3. **Use asynchronous processing for long-running workloads.**
4. **Use a workflow engine for complex orchestration.**
5. **Scale CPU/GPU workers independently.**
6. **Assume at-least-once delivery.**
7. **Make every processing step idempotent.**
8. **Use leases for worker ownership.**
9. **Use fencing/versioning against stale workers.**
10. **Use an outbox for reliable event publication.**
11. **Use retries with exponential backoff and jitter.**
12. **Use DLQs for unrecoverable failures.**
13. **Keep source media immutable/versioned.**
14. **Use CDN for global delivery.**
15. **Design observability into every asynchronous boundary.**

---

## Final Interview Summary

> **"My media platform separates five concerns: storage, orchestration, compute, transactional metadata, and delivery. Media goes directly from clients into object storage using multipart pre-signed uploads. Storage events initiate a durable workflow, which fans out to independently scalable metadata, thumbnail, moderation and CPU/GPU transcoding workers. Processing is asynchronous and idempotent, with leases, fencing, retries and DLQs handling failures. Outputs are immutable and versioned in object storage and delivered globally through a CDN. PostgreSQL manages transactional metadata and workflow/job state, while Kafka provides event distribution. This gives us independent scaling, fault isolation, global delivery and predictable recovery from distributed failures."**