-- ============================================================
-- Enable PostGIS
-- ============================================================

CREATE EXTENSION IF NOT EXISTS postgis;


-- ============================================================
-- Business Category
-- ============================================================

CREATE TABLE business_category (
    id              UUID PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_business_category_code
        UNIQUE (code),
    CONSTRAINT uk_business_category_name
        UNIQUE (name),
    CONSTRAINT chk_business_category_status
        CHECK (
            status IN (
                'ACTIVE',
                'INACTIVE'
            )
        )
);