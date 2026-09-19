CREATE TABLE business (
    id              UUID PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(2000),

    category_id     UUID NOT NULL,

    status          VARCHAR(20) NOT NULL,

    version         BIGINT NOT NULL DEFAULT 0,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_business_category
        FOREIGN KEY (category_id)
        REFERENCES business_category(id)
);

CREATE INDEX idx_business_category
    ON business(category_id);

CREATE INDEX idx_business_active_category
    ON business(category_id)
    WHERE status = 'ACTIVE';