CREATE TABLE friendship
(
    user_id    VARCHAR(64) NOT NULL,
    friend_id  VARCHAR(64) NOT NULL,

    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_friendship
        PRIMARY KEY (user_id, friend_id),

    CONSTRAINT chk_friendship_status
        CHECK (status IN ('ACTIVE', 'BLOCKED', 'REMOVED')),

    CONSTRAINT chk_friendship_not_self
        CHECK (user_id <> friend_id)
);

CREATE INDEX idx_friendship_user_status
    ON friendship (user_id, status);