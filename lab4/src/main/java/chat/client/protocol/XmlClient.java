package chat.client.protocol;

import chat.client.ChatClient;
import chat.protocol.XmlProtocol;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class XmlClient extends XmlProtocol {
    private final ChatClient client;

    public XmlClient(DataInputStream dataIn, DataOutputStream dataOut, ChatClient client) {
        super(dataIn, dataOut);
        this.client = client;
    }

    public void performXmlLogin() throws IOException {
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

    public void receiveMessages() throws IOException, ParserConfigurationException, SAXException {
        String xmlString = acceptXmlResponse();

        System.out.println("Server receive: " + xmlString);

        Document doc = parseXml(xmlString);

        Element root = doc.getDocumentElement();
        String eventName = root.getAttribute("name");

        if ("userlogin".equals(eventName) || "userlogout".equals(eventName)) {
            String user = root.getElementsByTagName("name").item(0).getTextContent();
            client.updateUserList(user, "userlogin".equals(eventName));

        } else if ("message".equals(eventName)) {
            String sender = root.getElementsByTagName("name").item(0).getTextContent();
            String message = root.getElementsByTagName("message").item(0).getTextContent();
            client.addMessage(sender, message);

        }else if ("history".equals(eventName)) {
            NodeList items = root.getElementsByTagName("item");

            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String sender = item.getElementsByTagName("sender").item(0).getTextContent();
                String text = item.getElementsByTagName("text").item(0).getTextContent();
                client.addMessage(sender, text);
            }

        } else if ("userlist".equals(eventName)) {
            client.updateUserListXml(root);
        }
    }

    public void sendLogoutMessage() throws IOException {
        String xml = "<command name=\"logout\">" +
                "<name>" + client.getUsername() + "</name>" +
                "</command>";

        System.out.println("Sending XML logout: " + xml);
        sendXmlResponse(xml);
    }

}

