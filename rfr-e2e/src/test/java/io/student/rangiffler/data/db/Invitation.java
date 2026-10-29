package io.student.rangiffler.data.db;

import org.springframework.jdbc.core.simple.JdbcClient;

public final class Invitation {
    private final String requester;
    private final String addressee;
    private final JdbcClient api;

    public Invitation(String requester, String addressee) {
        this.requester = requester;
        this.addressee = addressee;
        this.api = Jdbc.client(Databases.API);
    }

    public void create() {
        if (api.sql(Jdbc.sql("api/create-invitation.sql"))
                .param("requester", requester).param("addressee", addressee).update() != 1) {
            throw new IllegalStateException("Expected one inserted invitation from " + requester + " to " + addressee);
        }
    }

    public void delete() {
        api.sql(Jdbc.sql("api/delete-invitation.sql"))
                .param("requester", requester).param("addressee", addressee).update();
    }

    public boolean exist() {
        return api.sql(Jdbc.sql("api/exist-invitation.sql"))
                .param("requester", requester).param("addressee", addressee).query(Boolean.class).single();
    }
}
