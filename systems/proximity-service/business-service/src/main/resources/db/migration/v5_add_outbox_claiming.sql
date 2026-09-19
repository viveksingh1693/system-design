CREATE TABLE outbox_event (
    id              UUID PRIMARY KEY,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(150) NOT NULL,
    payload         JSONB NOT NULL,
    status          VARCHAR(20) NOT NULL,
    retry_count     INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at    TIMESTAMP WITH TIME ZONE,
    claimed_at      TIMESTAMP WITH TIME ZONE,
    claimed_by      VARCHAR(100),
    last_error      VARCHAR(2000),

    CONSTRAINT chk_outbox_event_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'PUBLISHED',
                'FAILED'
            )
        ),

    CONSTRAINT chk_outbox_event_retry_count
        CHECK (retry_count >= 0)
);

CREATE INDEX idx_outbox_event_pending
    ON outbox_event(status, created_at)
    WHERE status = 'PENDING';

CREATE INDEX idx_outbox_event_processing
    ON outbox_event(status, claimed_at)
    WHERE status = 'PROCESSING';