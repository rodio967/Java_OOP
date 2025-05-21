package chat.model;

import java.io.Serial;

public class LoginMessage extends Message {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String username;

    public LoginMessage(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
