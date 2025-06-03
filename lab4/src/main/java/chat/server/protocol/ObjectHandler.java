package chat.server.protocol;


import chat.model.*;
import chat.server.ChatServer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Set;
import java.util.logging.Logger;

public class ObjectHandler implements ServerHandler {
    private final Logger logger = Logger.getLogger(ObjectHandler.class.getName());
    private final ObjectOutputStream objectOut;
    private final ObjectInputStream objectIn;
    private final ChatServer server;
    private final ChatServer.ClientHandler clientHandler;

    public ObjectHandler(Socket socket, ChatServer server, ChatServer.ClientHandler clientHandler) throws IOException {
        this.objectIn = new ObjectInputStream(socket.getInputStream());
        this.objectOut = new ObjectOutputStream(socket.getOutputStream());
        this.server = server;
        this.clientHandler = clientHandler;
    }

    public void sendLoginMessage(Message login) throws IOException {
        String username = login.getUsername();

        logger.info("User " + username + " connected (OBJECT)");

        server.broadcastUserEvent(clientHandler.getUsername(), true);

        Message userList = new Message(MessageType.USER_LIST);
        userList.setOnlineUsers(server.getOnlineUsers());

        Message messageHistory = new Message(MessageType.MESSAGE_HISTORY);
        messageHistory.setHistory(server.getMessageHistory());

        objectOut.writeObject(userList);
        objectOut.writeObject(messageHistory);
        objectOut.flush();
    }



    @Override
    public void Communication() throws IOException {
        cycle:
        while (true) {
            try {
                Message message = (Message)objectIn.readObject();

                switch(message.getType()) {
                    case LOGIN -> sendLoginMessage(message);
                    case CHAT -> server.broadcastMessage(message, clientHandler);
                    case LOGOUT -> {
                        break cycle;
                    }
                }
            } catch (ClassNotFoundException e) {
                logger.severe("Serialize parsing error: " + e.getMessage());
            }

        }
    }

    @Override
    public void sendMessage(Message message) throws IOException {
        objectOut.writeObject(message);
        objectOut.flush();
    }

    @Override
    public void sendUserEvent(String username, boolean isLogin) throws IOException {
        Message userEventMessage = new Message(MessageType.USER_EVENT);
        userEventMessage.setUsername(username);
        userEventMessage.setLogin(isLogin);

        objectOut.writeObject(userEventMessage);
        objectOut.flush();
    }

    @Override
    public boolean checkUsername(Set<String> onlineUsers, String username) throws IOException {
        boolean nameAvailable = onlineUsers.contains(username);

        objectOut.writeBoolean(nameAvailable);
        objectOut.flush();

        return nameAvailable;

    }

    @Override
    public String readUsername() throws IOException {
        return objectIn.readUTF();
    }

    @Override
    public void closeResources() {
        if (objectIn != null) {
            try {
                objectIn.close();
            } catch (IOException e) {
                System.out.println("Error closing Server objectIn: " + e.getMessage());
            }
        }

        if (objectOut != null) {
            try {
                objectOut.close();
            }catch (IOException e) {
                System.out.println("Error closing Server objectOut: " + e.getMessage());
            }
        }
    }
}
