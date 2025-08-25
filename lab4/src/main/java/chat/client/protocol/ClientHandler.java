package chat.client.protocol;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public interface ClientHandler {
    void sendMessage(String text) throws IOException;
    void receiveMessages() throws IOException, ParserConfigurationException, SAXException, ClassNotFoundException;
    void sendLogoutMessage() throws IOException;
    boolean performLogin() throws IOException;
    void closeResources() throws IOException;
}
