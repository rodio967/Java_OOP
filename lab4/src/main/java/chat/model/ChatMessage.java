package chat.model;

import java.io.Serial;

public class ChatMessage extends Message {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String sender;
    private final String message;

    public ChatMessage(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }
}
