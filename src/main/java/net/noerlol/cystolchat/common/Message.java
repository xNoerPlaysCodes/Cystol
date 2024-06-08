package net.noerlol.cystolchat.common;

public class Message {
    private final String message;
    private final User user;
    private final MessageType type;
    public Message(String message, User user, MessageType type) {
        this.message = message;
        this.user = user;
        this.type = type;
    }

    public String getMessage() {
        return this.message;
    }

    public User getUser() {
        return this.user;
    }

    public MessageType getType() {
        return this.type;
    }
}