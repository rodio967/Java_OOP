package chat.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

public class Message implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final MessageType type;

    private String sender;
    private String message;
    private String username;
    private List<Message> history;
    private boolean isLogin;
    private Set<String> onlineUsers;

    public Message(MessageType type) {
        this.type = type;
    }

    public MessageType getType() {
        return type;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setHistory(List<Message> history) {
        this.history = history;
    }

    public void setLogin(boolean login) {
        this.isLogin = login;
    }

    public void setOnlineUsers(Set<String> onlineUsers) {
        this.onlineUsers = onlineUsers;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public String getUsername() {
        return username;
    }

    public List<Message> getHistory() {
        return history;
    }

    public boolean isLogin() {
        return isLogin;
    }

    public Set<String> getUsers() {
        return onlineUsers;
    }
}




