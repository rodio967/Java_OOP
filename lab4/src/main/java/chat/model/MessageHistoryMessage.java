package chat.model;

import java.io.Serial;
import java.util.List;

public class MessageHistoryMessage extends Message {
    @Serial
    private static final long serialVersionUID = 1L;
    private final List<ChatMessage> history;

    public MessageHistoryMessage(List<ChatMessage> history) {
        this.history = history;
    }

    public List<ChatMessage> getHistory() {
        return history;
    }
}
