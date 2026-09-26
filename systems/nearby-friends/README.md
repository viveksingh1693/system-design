docker compose up -d

docker exec nearby-kafka   /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092

docker exec nearby-kafka /opt/kafka/bin/kafka-topics.sh --create --topic location-updates --bootstrap-server localhost:9092 --partitions 6 --replication-factor 1


CREATE TABLE friendship (
    user_id       VARCHAR(64) NOT NULL,
    friend_id     VARCHAR(64) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, friend_id)
);

CREATE INDEX idx_friendship_user
    ON friendship(user_id);

INSERT INTO friendship(user_id, friend_id)
VALUES
('u1','u2'),
('u1','u8'),
('u1','u9'),

('u2','u1'),
('u2','u9'),
('u2','u10'),

('u3','u1'),
('u3','u2'),
('u3','u4'),
('u3','u5'),
('u3','u6'),
('u3','u7'),
('u3','u8'),
('u3','u9'),
('u3','u10');


{
  "userId": "u1",
  "latitude": 28.4595,
  "longitude": 77.0266,
  "accuracyMeters": 8.5,
  "timestamp": "2026-09-25T06:30:00Z"
}


nearby:users
GEOADD nearby:users 77.0266 28.4595 u1
GEOADD nearby:users 77.0300 28.4610 u2
GEOADD nearby:users 77.0200 28.4580 u3


             u1 Mobile
                │
                │ location update
                ▼
       ┌──────────────────┐
       │ Location Service │
       └────────┬─────────┘
                │
                │ Kafka
                ▼
       ┌──────────────────┐
       │ location-updates │
       └────────┬─────────┘
                │
                ▼
       ┌──────────────────┐
       │ Location Worker  │
       └────────┬─────────┘
                │
                ▼
          Redis GEO
       nearby:users