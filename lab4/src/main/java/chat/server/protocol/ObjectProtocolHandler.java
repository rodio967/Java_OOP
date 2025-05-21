package chat.server.protocol;

import chat.model.*;
import chat.server.ChatServer;

import java.io.*;
import java.util.Date;

public class ObjectProtocolHandler {
    private final ObjectInputStream objectIn;
    private final ObjectOutputStream objectOut;
    private final ChatServer server;
    private final ChatServer.ClientHandler clientHandler;

    public ObjectProtocolHandler(ObjectInputStream objectIn, ObjectOutputStream objectOut,
                                 ChatServer server, ChatServer.ClientHandler clientHandler) {
        this.objectIn = objectIn;
        this.objectOut = objectOut;
        this.server = server;
        this.clientHandler = clientHandler;
    }

    public void HandleObjectCommunication() throws IOException {
        while (true) {
            try {
                Object obj = objectIn.readObject();
                if (obj instanceof LoginMessage) {
                    LoginMessage login = (LoginMessage) obj;
                    sendLoginMessage(login);

                } else if (obj instanceof ChatMessage) {
                    ChatMessage message = (ChatMessage) obj;
                    server.broadcastMessage(message, clientHandler);

                } else if (obj instanceof LogoutMessage) {
                    break;
                }
            } catch (ClassNotFoundException e) {
                log("Serialize parsing error: " + e.getMessage());
            }

        }
    }

    public void sendLoginMessage(LoginMessage login) throws IOException {
        String username = login.getUsername();
        clientHandler.setUsername(username);

        server.broadcastUserEvent(clientHandler.getUsername(), true);


        objectOut.writeObject(new UserListMessage(server.getUserList()));
        objectOut.writeObject(new MessageHistoryMessage(server.getMessageHistory()));
        objectOut.flush();
    }

    public void sendMessage(ChatMessage message) throws IOException {
        objectOut.writeObject(message);
        objectOut.flush();
    }

    public void sendUserEvent(String username, boolean isLogin) throws IOException {
        objectOut.writeObject(new UserEventMessage(username, isLogin));
        objectOut.flush();
    }

    private void log(String message) {
        System.out.println("[SERVER LOG] " + new Date() + ": " + message);
    }
}