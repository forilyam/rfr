INSERT INTO `user` (username, password, enabled, account_non_expired,
                    account_non_locked, credentials_non_expired)
VALUES (:username, :password, TRUE, TRUE, TRUE, TRUE)
