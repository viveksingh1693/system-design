INSERT INTO friendship
(
    user_id,
    friend_id,
    status
)
VALUES

-- u1
('u1', 'u2', 'ACTIVE'),
('u1', 'u8', 'ACTIVE'),
('u1', 'u9', 'ACTIVE'),

-- u2
('u2', 'u1', 'ACTIVE'),
('u2', 'u9', 'ACTIVE'),
('u2', 'u10', 'ACTIVE'),

-- u3
('u3', 'u1', 'ACTIVE'),
('u3', 'u2', 'ACTIVE'),
('u3', 'u4', 'ACTIVE'),
('u3', 'u5', 'ACTIVE'),
('u3', 'u6', 'ACTIVE'),
('u3', 'u7', 'ACTIVE'),
('u3', 'u8', 'ACTIVE'),
('u3', 'u9', 'ACTIVE'),
('u3', 'u10', 'ACTIVE');