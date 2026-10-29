DELETE
friendship FROM friendship
JOIN `user` requester ON requester.id = friendship.requester_id
JOIN `user` addressee ON addressee.id = friendship.addressee_id
WHERE friendship.status = 'ACCEPTED'
  AND ((requester.username = :requester AND addressee.username = :addressee)
    OR (requester.username = :addressee AND addressee.username = :requester))
