package chat.model;

import java.io.Serial;
import java.util.Set;

public class UserListMessage extends Message {
    @Serial
    private static final long serialVersionUID = 1L;
    private final Set<String> onlineUsers;

    public UserListMessage(Set<String> onlineUsers) {
        this.onlineUsers = onlineUsers;
    }

    public Set<String> getUsers() {
        return onlineUsers;
    }
}
