package net.noerlol.cystolchat.common;

public class User {
    private final String username;
    public User(String username) {
        this.username = username;
    }

    public String getUsername() {
        return this.username;
    }

    public enum Type {
        NONE;
    }
}
