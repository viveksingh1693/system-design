-- ============================================================
-- Business Location
-- ============================================================

CREATE TABLE business_location (
    id              UUID PRIMARY KEY,

    business_id     UUID NOT NULL,

    latitude        NUMERIC(9,6) NOT NULL,

    longitude       NUMERIC(9,6) NOT NULL,

    -- --------------------------------------------------------
    -- Generated PostGIS geography point.
    --
    -- IMPORTANT:
    -- ST_MakePoint(longitude, latitude)
    --
    -- X = longitude
    -- Y = latitude
    -- --------------------------------------------------------

    location        GEOGRAPHY(POINT, 4326)
                    GENERATED ALWAYS AS (
                        ST_SetSRID(
                            ST_MakePoint(
                                longitude,
                                latitude
                            ),
                            4326
                        )::geography
                    ) STORED,

    address_line_1  VARCHAR(300),

    address_line_2  VARCHAR(300),

    city            VARCHAR(100),

    state           VARCHAR(100),

    country         VARCHAR(100),

    postal_code     VARCHAR(20),

    status          VARCHAR(20) NOT NULL,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,


    -- --------------------------------------------------------
    -- Foreign key
    -- --------------------------------------------------------

    CONSTRAINT fk_location_business
        FOREIGN KEY (business_id)
        REFERENCES business(id),


    -- --------------------------------------------------------
    -- Coordinate validation
    -- --------------------------------------------------------

    CONSTRAINT chk_business_location_latitude
        CHECK (
            latitude BETWEEN -90 AND 90
        ),

    CONSTRAINT chk_business_location_longitude
        CHECK (
            longitude BETWEEN -180 AND 180
        ),


    -- --------------------------------------------------------
    -- Location status
    -- --------------------------------------------------------

    CONSTRAINT chk_business_location_status
        CHECK (
            status IN (
                'ACTIVE',
                'INACTIVE'
            )
        )
);


-- ============================================================
-- Business lookup
-- ============================================================

CREATE INDEX idx_business_location_business
    ON business_location(business_id);


-- ============================================================
-- Spatial index
--
-- Only active locations participate in proximity searches.
-- ============================================================

CREATE INDEX idx_business_location_active_geo
    ON business_location
    USING GIST (location)
    WHERE status = 'ACTIVE';


-- ============================================================
-- Active locations by business
-- ============================================================

CREATE INDEX idx_business_location_active_business
    ON business_location(business_id)
    WHERE status = 'ACTIVE';