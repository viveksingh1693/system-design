-- ============================================================
-- Transactional Outbox
-- ============================================================

CREATE TABLE outbox_event (

    id              UUID PRIMARY KEY,


    -- --------------------------------------------------------
    -- Aggregate information
    -- --------------------------------------------------------

    aggregate_type  VARCHAR(100) NOT NULL,

    aggregate_id    UUID NOT NULL,


    -- --------------------------------------------------------
    -- Event information
    -- --------------------------------------------------------

    event_type      VARCHAR(150) NOT NULL,

    payload         JSONB NOT NULL,


    -- --------------------------------------------------------
    -- Publishing lifecycle
    --
    -- PENDING
    -- PROCESSING
    -- PUBLISHED
    -- FAILED
    -- --------------------------------------------------------

    status          VARCHAR(20) NOT NULL,


    -- --------------------------------------------------------
    -- Retry information
    -- --------------------------------------------------------

    retry_count     INTEGER NOT NULL DEFAULT 0,


    -- --------------------------------------------------------
    -- Timestamps
    -- --------------------------------------------------------

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    published_at    TIMESTAMP WITH TIME ZONE,


    -- --------------------------------------------------------
    -- Publisher claim / lease
    --
    -- Used to prevent multiple publisher instances from
    -- processing the same event concurrently.
    -- --------------------------------------------------------

    claimed_at      TIMESTAMP WITH TIME ZONE,

    claimed_by      VARCHAR(100),


    -- --------------------------------------------------------
    -- Last failure
    -- --------------------------------------------------------

    last_error      VARCHAR(2000),


    -- --------------------------------------------------------
    -- Constraints
    -- --------------------------------------------------------

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
        CHECK (
            retry_count >= 0
        )
);


-- ============================================================
-- Pending event index
--
-- Publisher normally looks for:
--
-- status = PENDING
-- ORDER BY created_at
-- ============================================================

CREATE INDEX idx_outbox_event_pending
    ON outbox_event(created_at)
    WHERE status = 'PENDING';


-- ============================================================
-- Processing event index
--
-- Used for recovering events whose publisher instance died.
-- ============================================================

CREATE INDEX idx_outbox_event_processing
    ON outbox_event(claimed_at)
    WHERE status = 'PROCESSING';


-- ============================================================
-- Aggregate lookup
--
-- Useful for debugging/auditing event history for a business.
-- ============================================================

CREATE INDEX idx_outbox_event_aggregate
    ON outbox_event(
        aggregate_type,
        aggregate_id
    );