INSERT INTO authority (user_id, authority)
SELECT id, 'read' FROM `user` WHERE username = :username
UNION ALL
SELECT id, 'write' FROM `user` WHERE username = :username
