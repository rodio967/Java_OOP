package chat.client.protocol;

import chat.client.ChatClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class XmlHandler implements ClientHandler {
    private final DataOutputStream dataOut;
    private final DataInputStream dataIn;
    private final ChatClient client;

    public XmlHandler(Socket socket, ChatClient client) throws IOException {
        this.dataOut = new DataOutputStream(socket.getOutputStream());
        this.dataIn = new DataInputStream(socket.getInputStream());
        this.client = client;
    }

    public String acceptXmlResponse() throws IOException {
        int length = dataIn.readInt();
        byte[] xmlBytes = new byte[length];
        dataIn.readFully(xmlBytes);

        return new String(xmlBytes, StandardCharsets.UTF_8);
    }

    public void sendXmlResponse(String xml) throws IOException {
        byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
        dataOut.writeInt(bytes.length);
        dataOut.write(bytes);
        dataOut.flush();
    }


    public Document parseXml(String xml) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }


    public String escapeXml(String input) {
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    @Override
    public void sendMessage(String text) throws IOException {
        String xml = String.format(
                "<command name=\"message\">" +
                        "<message>%s</message>" +
                        "<name>%s</name>" +
                        "</command>",
                escapeXml(text),
                client.getUsername()
        );

        System.out.println("Sending XML message: " + xml);


        sendXmlResponse(xml);
    }

    @Override
    public void receiveMessages() throws IOException, ParserConfigurationException, SAXException{
        String xmlString = acceptXmlResponse();
        System.out.println("Server receive: " + xmlString);

        Document doc = parseXml(xmlString);

        Element root = doc.getDocumentElement();
        String eventName = root.getAttribute("name");

        if ("userlogin".equals(eventName) || "userlogout".equals(eventName)) {
            handleUserLoginOrLogout(root, eventName);
        } else if ("message".equals(eventName)) {
            handleMessage(root);
        }else if ("history".equals(eventName)) {
            handleHistory(root);
        } else if ("userlist".equals(eventName)) {
            handleUserList(root);
        }
    }

    private void handleUserLoginOrLogout(Element root, String eventName) {
        String user = root.getElementsByTagName("name").item(0).getTextContent();
        client.updateUserList(user, "userlogin".equals(eventName));
    }

    private void handleMessage(Element root) {
        String sender = root.getElementsByTagName("name").item(0).getTextContent();
        String message = root.getElementsByTagName("message").item(0).getTextContent();
        client.addMessage(sender, message);
    }

    private void handleHistory(Element root) {
        NodeList items = root.getElementsByTagName("item");

        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            String sender = item.getElementsByTagName("sender").item(0).getTextContent();
            String text = item.getElementsByTagName("text").item(0).getTextContent();
            client.addMessage(sender, text);
        }
    }

    private void handleUserList(Element root) {
        client.updateUserListXml(root);
    }

    @Override
    public void sendLogoutMessage() throws IOException {
        String xml = "<command name=\"logout\">" +
                "<name>" + client.getUsername() + "</name>" +
                "</command>";

        System.out.println("Sending XML logout: " + xml);
        sendXmlResponse(xml);
    }

    @Override
    public void performLogin() throws IOException {
        String xml = String.format(
                "<command name=\"login\">" +
                        "<name>%s</name>" +
                        "</command>",
                escapeXml(client.getUsername())
        );

        System.out.println("Sending XML login: " + xml);

        sendXmlResponse(xml);

        String response = acceptXmlResponse();
        System.out.println("Server response: " + response);
    }

    @Override
    public boolean checkUsername(String username) throws IOException {
        dataOut.writeUTF(username);
        dataOut.flush();

        return dataIn.readBoolean();
    }

    @Override
    public void closeResources() {
        if (dataIn != null) {
            try {
                dataIn.close();
            }catch (IOException e) {
                System.out.println("Error closing Client dataIn: " + e.getMessage());
            }
        }

        if (dataOut != null) {
            try {
                dataOut.close();
            } catch (IOException e) {
                System.out.println("Error closing Client dataOut: " + e.getMessage());
            }
        }
    }
}
