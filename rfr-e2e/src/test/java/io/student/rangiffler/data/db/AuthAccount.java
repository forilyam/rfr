package io.student.rangiffler.data.db;

import io.student.rangiffler.data.TestUser;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

public final class AuthAccount {
    private final TestUser user;
    private final JdbcClient auth;

    public AuthAccount(TestUser user) {
        this.user = user;
        this.auth = Jdbc.client(Databases.AUTH);
    }

    public void create() {
        String password = PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(user.password());
        Jdbc.inTransaction(Databases.AUTH, () -> {
            if (auth.sql(Jdbc.sql("auth/create-user.sql"))
                    .param("username", user.username()).param("password", password).update() != 1) {
                throw new IllegalStateException("Expected one inserted auth account for " + user.username());
            }
            if (auth.sql(Jdbc.sql("auth/create-authorities.sql")).param("username", user.username()).update() != 2) {
                throw new IllegalStateException("Expected two inserted authorities for " + user.username());
            }
        });
    }

    public void delete() {
        Jdbc.inTransaction(Databases.AUTH, () -> {
            auth.sql(Jdbc.sql("auth/delete-authorities.sql")).param("username", user.username()).update();
            auth.sql(Jdbc.sql("auth/delete-user.sql")).param("username", user.username()).update();
        });
    }

    public boolean exist() {
        return auth.sql(Jdbc.sql("auth/exist-user.sql"))
                .param("username", user.username()).query(Boolean.class).single();
    }
}
