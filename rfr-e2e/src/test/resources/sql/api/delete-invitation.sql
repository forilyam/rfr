DELETE
friendship FROM friendship
JOIN `user` requester ON requester.id = friendship.requester_id
JOIN `user` addressee ON addressee.id = friendship.addressee_id
WHERE friendship.status = 'PENDING'
  AND requester.username = :requester AND addressee.username = :addressee
