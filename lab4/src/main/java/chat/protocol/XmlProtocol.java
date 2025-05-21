package chat.protocol;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

public abstract class XmlProtocol {
    protected DataInputStream dataIn;
    protected DataOutputStream dataOut;

    public XmlProtocol(DataInputStream dataIn, DataOutputStream dataOut) {
        this.dataIn = dataIn;
        this.dataOut = dataOut;
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

}
