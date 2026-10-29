SELECT EXISTS (SELECT 1
               FROM friendship
               WHERE requester_id = (SELECT id FROM `user` WHERE username = :username)
                  OR addressee_id = (SELECT id FROM `user` WHERE username = :username))
