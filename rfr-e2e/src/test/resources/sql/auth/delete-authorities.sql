DELETE FROM authority WHERE user_id IN
(SELECT id FROM `user` WHERE username = :username)
