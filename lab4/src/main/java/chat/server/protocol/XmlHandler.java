package chat.server.protocol;

import chat.model.Message;
import chat.model.MessageType;
import chat.server.ChatServer;
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
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class XmlHandler implements ServerHandler {
    private static final int Page_Size = 50;
    private final DataOutputStream dataOut;
    private final DataInputStream dataIn;
    private final ChatServer server;
    private final ChatServer.ClientHandler client;

    public XmlHandler(Socket socket, ChatServer server, ChatServer.ClientHandler client) throws IOException {
        this.dataIn = new DataInputStream(socket.getInputStream());
        this.dataOut = new DataOutputStream(socket.getOutputStream());
        this.server = server;
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
    public void Communication() throws IOException {
        try {
            while (true) {
                String xmlString = acceptXmlResponse();
                Document doc = parseXml(xmlString);

                Element root = doc.getDocumentElement();
                String commandName = root.getAttribute("name");

                if ("login".equals(commandName)) {
                    handleXmlLogin(root);
                } else if ("list".equals(commandName)) {
                    sendXmlUserList();
                } else if ("message".equals(commandName)) {
                    handleXmlMessage(root);
                } else if ("logout".equals(commandName)) {
                    break;
                }
            }
        } catch (ParserConfigurationException | SAXException e) {
            log("XML parsing error: " + e.getMessage());
        }
    }

    @Override
    public void sendMessage(Message message) throws IOException {
        String xml = "<event name=\"message\"><message>" +
                escapeXml(message.getMessage()) +
                "</message><name>" +
                escapeXml(message.getSender()) +
                "</name></event>";

        sendXmlResponse(xml);
    }

    @Override
    public void sendUserEvent(String username, boolean isLogin) throws IOException {
        String eventName = isLogin ? "userlogin" : "userlogout";
        String xml = "<event name=\"" + eventName + "\"><name>" +
                escapeXml(username) + "</name></event>";

        sendXmlResponse(xml);
    }


    @Override
    public boolean checkUsername(Set<String> onlineUsers, String username) throws IOException {
        boolean nameAvailable = onlineUsers.contains(username);

        dataOut.writeBoolean(nameAvailable);
        dataOut.flush();

        return nameAvailable;
    }

    @Override
    public String readUsername() throws IOException {
        return dataIn.readUTF();
    }

    @Override
    public void closeResources() {
        if (dataIn != null) {
            try {
                dataIn.close();
            }catch (IOException e) {
                System.out.println("Error closing Server dataIn: " + e.getMessage());
            }
        }

        if (dataOut != null) {
            try {
                dataOut.close();
            } catch (IOException e) {
                System.out.println("Error closing Server dataOut: " + e.getMessage());
            }
        }
    }


    private void handleXmlLogin(Element root) throws IOException {
        NodeList nameNodes = root.getElementsByTagName("name");
        if (nameNodes.getLength() > 0) {
            String successResponse = "<success><session>" + UUID.randomUUID() + "</session></success>";
            sendXmlResponse(successResponse);

            String username = nameNodes.item(0).getTextContent();
            log("User " + username + " connected (XML)");

            server.broadcastUserEvent(client.getUsername(), true);

            sendXmlUserList();
            sendMessageHistory();
        } else {
            String errorResponse = "<error><message>Invalid login format</message></error>";
            sendXmlResponse(errorResponse);
        }
    }


    public void sendMessageHistory() throws IOException {
        List<Message> history = server.getMessageHistory();
        int size = history.size();
        int start = 0;
        int end = Math.min(size, Page_Size);

        StringBuilder xmlBuilder = new StringBuilder();

        while (start < size) {
            xmlBuilder.setLength(0);
            xmlBuilder.append("<event name=\"history\">");
            for (int i = start; i < end; i++) {
                Message message = history.get(i);
                xmlBuilder.append("<item>")
                        .append("<sender>").append(escapeXml(message.getSender())).append("</sender>")
                        .append("<text>").append(escapeXml(message.getMessage())).append("</text>")
                        .append("</item>");
            }
            xmlBuilder.append("</event>");
            System.out.println("Sending history: " + xmlBuilder.toString());
            sendXmlResponse(xmlBuilder.toString());

            start = end;
            end += Page_Size;
            end = Math.min(size, end);

        }
    }


    public void handleXmlMessage(Element root) throws IOException {
        NodeList messageNodes = root.getElementsByTagName("message");
        if (messageNodes.getLength() > 0) {
            String messageText = messageNodes.item(0).getTextContent();

            Message message = new Message(MessageType.CHAT);
            message.setSender(client.getUsername());
            message.setMessage(messageText);

            server.broadcastMessage(message, client);

            String successResponse = "<success></success>";
            sendXmlResponse(successResponse);
        } else {
            String errorResponse = "<error><message>Invalid message format</message></error>";
            sendXmlResponse(errorResponse);
        }
    }

    public void sendXmlUserList() throws IOException {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<event name=\"userlist\">");

        for (String user : server.getOnlineUsers()) {
            xmlBuilder.append("<user>").append(user).append("</user>");
        }


        xmlBuilder.append("</event>");
        System.out.println("Sending userlist: " + xmlBuilder.toString());
        sendXmlResponse(xmlBuilder.toString());

    }

    private void log(String message) {
        System.out.println("[SERVER LOG] " + new Date() + ": " + message);
    }
}
