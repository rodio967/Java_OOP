package chat.server.protocol;

import chat.model.Message;

import java.io.IOException;
import java.util.Set;

public interface ServerHandler {
    void Communication() throws IOException;
    void sendMessage(Message message) throws IOException;
    void sendUserEvent(String username, boolean isLogin) throws IOException;
    boolean checkUsername(Set<String> onlineUsers, String username) throws IOException;
    String readUsername() throws IOException;
    void closeResources() throws IOException;
}
