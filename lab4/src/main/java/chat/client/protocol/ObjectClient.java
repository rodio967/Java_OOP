package chat.client.protocol;

import chat.client.ChatClient;
import chat.model.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ObjectClient {
    private final ObjectInputStream objectIn;
    private final ObjectOutputStream objectOut;
    private final ChatClient client;

    public ObjectClient(ObjectInputStream objectIn, ObjectOutputStream objectOut, ChatClient client) {
        this.objectIn = objectIn;
        this.objectOut = objectOut;
        this.client = client;
    }

    public void performObjectLogin() throws IOException {
        objectOut.writeObject(new LoginMessage(client.getUsername()));
        objectOut.flush();
    }

    public void sendMessage(String text) throws IOException {
        objectOut.writeObject(new ChatMessage(client.getUsername(), text));
        objectOut.flush();
    }

    public void receiveMessages() throws IOException, ClassNotFoundException {
        Object obj = objectIn.readObject();

        if (obj instanceof ChatMessage) {
            ChatMessage message = (ChatMessage) obj;
            client.addMessage(message.getSender(), message.getMessage());

        } else if (obj instanceof UserEventMessage) {
            UserEventMessage event = (UserEventMessage) obj;
            client.updateUserList(event.getUsername(), event.isLogin());

        } else if (obj instanceof UserListMessage) {
            client.updateUserList(((UserListMessage) obj).getUsers());

        } else if (obj instanceof MessageHistoryMessage) {
            for (ChatMessage message : ((MessageHistoryMessage) obj).getHistory()) {
                client.addMessage(message.getSender(), message.getMessage());
            }
        }
    }


    public void sendLogOutMessage() throws IOException {
        objectOut.writeObject(new LogoutMessage());
        objectOut.flush();
    }

}
