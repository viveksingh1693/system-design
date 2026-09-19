-- Seed data for business-service local development/testing.
-- Intended for PostgreSQL + PostGIS.
-- Prefer a Flyway seed migration only for controlled environments;
-- otherwise execute manually against a local/dev database.

INSERT INTO business_category
    (id, name, code, description, status, created_at, updated_at)
VALUES
    ('11111111-1111-1111-1111-111111111111',
     'Restaurant', 'RESTAURANT',
     'Restaurants and dining businesses', 'ACTIVE', NOW(), NOW()),
    ('22222222-2222-2222-2222-222222222222',
     'Cafe', 'CAFE',
     'Cafes and coffee shops', 'ACTIVE', NOW(), NOW()),
    ('33333333-3333-3333-3333-333333333333',
     'Grocery Store', 'GROCERY_STORE',
     'Grocery and daily-needs stores', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO business
    (id, name, description, category_id, status, version, created_at, updated_at)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     'The Food Corner',
     'Multi-cuisine restaurant',
     '11111111-1111-1111-1111-111111111111',
     'ACTIVE', 0, NOW(), NOW()),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
     'Brew & Bean',
     'Specialty coffee and bakery',
     '22222222-2222-2222-2222-222222222222',
     'ACTIVE', 0, NOW(), NOW()),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc',
     'Daily Needs Market',
     'Neighborhood grocery store',
     '33333333-3333-3333-3333-333333333333',
     'ACTIVE', 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO business_location
    (id, business_id, latitude, longitude,
     address_line_1, address_line_2, city, state, country, postal_code,
     status, created_at, updated_at)
VALUES
    ('aaaa1111-1111-1111-1111-111111111111',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     28.4595, 77.0266,
     'Golf Course Road', 'Sector 54',
     'Gurugram', 'Haryana', 'India', '122011',
     'ACTIVE', NOW(), NOW()),

    ('aaaa2222-2222-2222-2222-222222222222',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     28.4592, 77.0272,
     'Golf Course Road', 'Sector 54',
     'Gurugram', 'Haryana', 'India', '122011',
     'ACTIVE', NOW(), NOW()),

    ('bbbb1111-1111-1111-1111-111111111111',
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
     28.4601, 77.0278,
     'Golf Course Road', 'Sector 53',
     'Gurugram', 'Haryana', 'India', '122002',
     'ACTIVE', NOW(), NOW()),

    ('cccc1111-1111-1111-1111-111111111111',
     'cccccccc-cccc-cccc-cccc-cccccccccccc',
     28.4610, 77.0290,
     'Golf Course Road', 'Sector 52',
     'Gurugram', 'Haryana', 'India', '122003',
     'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Verification:
-- SELECT id, name, code, status FROM business_category ORDER BY name;
-- SELECT id, name, category_id, status FROM business ORDER BY name;
-- SELECT id, business_id, latitude, longitude,
--        ST_AsText(location) AS location
-- FROM business_location
-- ORDER BY business_id, id;
