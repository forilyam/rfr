package io.student.rangiffler.data.db;

import org.springframework.jdbc.core.simple.JdbcClient;

public final class Friendships {
    private final String username;
    private final JdbcClient api;

    public Friendships(String username) {
        this.username = username;
        this.api = Jdbc.client(Databases.API);
    }

    public void delete() {
        Jdbc.inTransaction(Databases.API, () -> {
            api.sql(Jdbc.sql("api/delete-friends.sql")).param("username", username).update();
            api.sql(Jdbc.sql("api/delete-invitations.sql")).param("username", username).update();
        });
    }

    public boolean exist() {
        return api.sql(Jdbc.sql("api/exist-friendships.sql")).param("username", username).query(Boolean.class).single();
    }
}
