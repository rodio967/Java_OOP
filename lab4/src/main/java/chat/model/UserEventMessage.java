package chat.model;

import java.io.Serial;

public class UserEventMessage extends Message {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String username;
    private final boolean isLogin;

    public UserEventMessage(String username, boolean isLogin) {
        this.username = username;
        this.isLogin = isLogin;
    }

    public String getUsername() {
        return username;
    }

    public boolean isLogin() {
        return isLogin;
    }
}
