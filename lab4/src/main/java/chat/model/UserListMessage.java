package chat.model;

import java.io.Serial;
import java.util.List;

public class UserListMessage extends Message {
    @Serial
    private static final long serialVersionUID = 1L;
    private final List<String> users;

    public UserListMessage(List<String> users) {
        this.users = users;
    }

    public List<String> getUsers() {
        return users;
    }
}
