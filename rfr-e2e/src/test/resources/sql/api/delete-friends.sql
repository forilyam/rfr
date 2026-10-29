DELETE
FROM friendship
WHERE status = 'ACCEPTED'
  AND (requester_id = (SELECT id FROM `user` WHERE username = :username)
    OR addressee_id = (SELECT id FROM `user` WHERE username = :username))
