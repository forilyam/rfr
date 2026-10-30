DELETE
FROM friendship
WHERE status = 'PENDING'
  AND (requester_id = (SELECT id FROM `user` WHERE username = :username)
    OR addressee_id = (SELECT id FROM `user` WHERE username = :username))
