INSERT INTO `user` (username, country_id)
VALUES (:username, (SELECT id FROM country WHERE code = 'ru'))
