package io.student.rangiffler.data.db;

import org.springframework.jdbc.core.simple.JdbcClient;

public final class Friendship {
    private final String requester;
    private final String addressee;
    private final JdbcClient api;

    public Friendship(String requester, String addressee) {
        this.requester = requester;
        this.addressee = addressee;
        this.api = Jdbc.client(Databases.API);
    }

    public void create() {
        if (api.sql(Jdbc.sql("api/create-friendship.sql"))
                .param("requester", requester).param("addressee", addressee).update() != 1) {
            throw new IllegalStateException("Expected one inserted friendship between " + requester + " and " + addressee);
        }
    }

    public void delete() {
        api.sql(Jdbc.sql("api/delete-friendship.sql"))
                .param("requester", requester).param("addressee", addressee).update();
    }

    public boolean exist() {
        return api.sql(Jdbc.sql("api/exist-friendship.sql"))
                .param("requester", requester).param("addressee", addressee).query(Boolean.class).single();
    }
}
