INSERT INTO friendship (id, requester_id, addressee_id, status, created_at, responded_at)
VALUES (UUID_TO_BIN(UUID()),
        (SELECT id FROM `user` WHERE username = :requester),
        (SELECT id FROM `user` WHERE username = :addressee),
        :status, CURRENT_TIMESTAMP(6),
        CASE WHEN :status = 'ACCEPTED' THEN CURRENT_TIMESTAMP(6) ELSE NULL END)
