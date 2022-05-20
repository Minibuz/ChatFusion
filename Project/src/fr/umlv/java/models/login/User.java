package fr.umlv.java.models.login;

import java.util.Objects;

public record User(String login, String password) {

    public User {
        Objects.requireNonNull(login);
    }

    public User(String login) {
        this(login, null);
    }
}
