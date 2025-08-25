package chat.client.protocol;

import chat.client.ChatClient;
import chat.model.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Logger;

public class ObjectHandler implements ClientHandler {
    private final Logger logger = Logger.getLogger(ObjectHandler.class.getName());
    private final ObjectOutputStream objectOut;
    private final ObjectInputStream objectIn;
    private final ChatClient client;

    public ObjectHandler(Socket socket, ChatClient client) throws IOException {
        this.objectOut = new ObjectOutputStream(socket.getOutputStream());
        this.objectIn = new ObjectInputStream(socket.getInputStream());
        this.client = client;
    }

    @Override
    public void sendMessage(String text) throws IOException {
        Message ChatMessage = new Message(MessageType.CHAT);
        ChatMessage.setSender(client.getUsername());
        ChatMessage.setMessage(text);
        objectOut.writeObject(ChatMessage);
        objectOut.flush();
    }

    @Override
    public void receiveMessages() throws IOException, ClassNotFoundException {
        Message message = (Message)objectIn.readObject();

        switch(message.getType()) {
            case CHAT -> client.addMessage(message.getSender(), message.getMessage());
            case USER_EVENT -> client.updateUserList(message.getUsername(), message.isLogin());
            case USER_LIST -> client.updateUserListObject(message.getUsers());
            case MESSAGE_HISTORY -> {
                for (Message msg : message.getHistory()) {
                    client.addMessage(msg.getSender(), msg.getMessage());
                }
            }
        }
    }

    @Override
    public void sendLogoutMessage() throws IOException {
        Message LogoutMessage = new Message(MessageType.LOGOUT);
        objectOut.writeObject(LogoutMessage);
        objectOut.flush();
    }

    @Override
    public boolean performLogin() throws IOException {
        Message LoginMessage = new Message(MessageType.LOGIN);
        LoginMessage.setUsername(client.getUsername());

        objectOut.writeObject(LoginMessage);
        objectOut.flush();

        return checkUsername();
    }


    public boolean checkUsername() throws IOException {
        return objectIn.readBoolean();
    }

    @Override
    public void closeResources() {
        if (objectIn != null) {
            try {
                objectIn.close();
            } catch (IOException e) {
                logger.severe("Error closing Client objectIn: " + e.getMessage());
            }
        }


        if (objectOut != null) {
            try {
                objectOut.close();
            }catch (IOException e) {
                logger.severe("Error closing Client objectOut: " + e.getMessage());
            }
        }
    }
}
