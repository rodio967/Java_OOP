package chat.server.protocol;

import chat.model.ChatMessage;
import chat.protocol.XmlProtocol;
import chat.server.ChatServer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import javax.xml.parsers.ParserConfigurationException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

public class XmlProtocolHandler extends XmlProtocol {
    private final ChatServer server;
    private final ChatServer.ClientHandler client;

    public XmlProtocolHandler(DataInputStream dataIn, DataOutputStream dataOut, ChatServer server, ChatServer.ClientHandler client) {
        super(dataIn, dataOut);
        this.server = server;
        this.client = client;
    }


    public void handleXmlCommunication() throws IOException {
        try {
            while (true) {

                String xmlString = acceptXmlResponse();
                Document doc = parseXml(xmlString);

                Element root = doc.getDocumentElement();
                String commandName = root.getAttribute("name");

                if ("login".equals(commandName)) {
                    handleXmlLogin(root);
                } else if ("list".equals(commandName)) {
                    handleXmlListUsers(root);
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
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<event name=\"history\">");

        for (ChatMessage msg : server.getMessageHistory()) {
            xmlBuilder.append("<item>")
                    .append("<sender>").append(escapeXml(msg.getSender())).append("</sender>")
                    .append("<text>").append(escapeXml(msg.getMessage())).append("</text>")
                    .append("</item>");
        }

        xmlBuilder.append("</event>");
        System.out.println("Sending history: " + xmlBuilder.toString());
        sendXmlResponse(xmlBuilder.toString());
    }


    public void handleXmlListUsers(Element root) throws IOException {
        sendXmlUserList();
    }

    public void handleXmlMessage(Element root) throws IOException {
        NodeList messageNodes = root.getElementsByTagName("message");
        if (messageNodes.getLength() > 0) {
            String messageText = messageNodes.item(0).getTextContent();
            ChatMessage message = new ChatMessage(client.getUsername(), messageText);
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


    public void sendMessage(ChatMessage message) throws IOException {
        String xml = "<event name=\"message\"><message>" +
                escapeXml(message.getMessage()) +
                "</message><name>" +
                escapeXml(message.getSender()) +
                "</name></event>";

        sendXmlResponse(xml);
    }



    public void sendUserEvent(String username, boolean isLogin) throws IOException {
        String eventName = isLogin ? "userlogin" : "userlogout";
        String xml = "<event name=\"" + eventName + "\"><name>" +
                escapeXml(username) + "</name></event>";

        sendXmlResponse(xml);
    }


    private void log(String message) {
        System.out.println("[SERVER LOG] " + new Date() + ": " + message);
    }

}
