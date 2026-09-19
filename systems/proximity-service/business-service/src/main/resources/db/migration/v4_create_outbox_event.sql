CREATE TABLE outbox_event (
    id UUID NOT NULL,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(150) NOT NULL,

    payload JSONB NOT NULL,

    status VARCHAR(20) NOT NULL,

    retry_count INTEGER NOT NULL DEFAULT 0,

    claimed_at TIMESTAMPTZ,
    claimed_by VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,

    last_error VARCHAR(2000),

    CONSTRAINT pk_outbox_event
        PRIMARY KEY (id),

    CONSTRAINT chk_outbox_event_status
        CHECK (status IN (
            'PENDING',
            'PROCESSING',
            'PUBLISHED',
            'FAILED'
        ))
);