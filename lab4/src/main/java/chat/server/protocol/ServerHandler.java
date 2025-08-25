package chat.server.protocol;

import chat.model.Message;

import java.io.IOException;


public interface ServerHandler {
    void Communication() throws IOException;
    void sendMessage(Message message) throws IOException;
    void sendUserEvent(String username, boolean isLogin) throws IOException;
    void closeResources() throws IOException;
}
