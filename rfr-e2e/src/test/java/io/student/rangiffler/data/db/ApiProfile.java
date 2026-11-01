package io.student.rangiffler.data.db;

import org.springframework.jdbc.core.simple.JdbcClient;

public final class ApiProfile {
    private final String username;
    private final JdbcClient api;

    public ApiProfile(String username) {
        this.username = username;
        this.api = Jdbc.client(Databases.API);
    }

    public void create() {
        if (api.sql(Jdbc.sql("api/create-profile.sql")).param("username", username).update() != 1) {
            throw new IllegalStateException("Expected one inserted API profile for " + username);
        }
    }

    public void delete() {
        api.sql(Jdbc.sql("api/delete-profile.sql")).param("username", username).update();
    }

    public boolean exist() {
        return api.sql(Jdbc.sql("api/exist-profile.sql")).param("username", username).query(Boolean.class).single();
    }
}
