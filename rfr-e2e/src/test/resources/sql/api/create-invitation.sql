INSERT INTO friendship (id, requester_id, addressee_id, status, created_at)
VALUES (UUID_TO_BIN(UUID()),
        (SELECT id FROM `user` WHERE username = :requester),
        (SELECT id FROM `user` WHERE username = :addressee),
        'PENDING', CURRENT_TIMESTAMP(6))
