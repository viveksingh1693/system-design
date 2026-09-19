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

    last_error      VARCHAR(2000)
);

CREATE INDEX idx_outbox_event_status_created
ON outbox_event(status, created_at);

CREATE INDEX idx_outbox_event_aggregate
ON outbox_event(aggregate_type, aggregate_id);