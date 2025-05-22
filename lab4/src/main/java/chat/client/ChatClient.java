package chat.client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;

import chat.client.protocol.ObjectClient;
import chat.client.protocol.XmlClient;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class ChatClient extends JFrame {
    private String username;
    private String serverAddress;
    private int serverPort;
    private boolean useXml;

    private Socket socket;
    private ObjectInputStream objectIn;
    private ObjectOutputStream objectOut;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;

    private XmlClient xmlClient;
    private ObjectClient objectClient;

    private final DefaultListModel<String> userListModel;
    private final DefaultListModel<String> messageListModel;

    private final JTextArea messageArea;
    private final JTextField inputField;
    private final JList<String> userList;
    private final JComboBox<String> protocolCombo;

    public String getUsername() {
        return username;
    }

    public ChatClient() {
        super("Chat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());


        JPanel connectPanel = new JPanel(new FlowLayout());
        JTextField usernameField = new JTextField(10);
        JTextField serverField = new JTextField("localhost", 10);
        JTextField portField = new JTextField("5555", 5);
        protocolCombo = new JComboBox<>(new String[]{"Java Serialization", "XML"});
        JButton connectButton = new JButton("Connect");

        connectPanel.add(new JLabel("Username:"));
        connectPanel.add(usernameField);
        connectPanel.add(new JLabel("Server:"));
        connectPanel.add(serverField);
        connectPanel.add(new JLabel("Port:"));
        connectPanel.add(portField);
        connectPanel.add(protocolCombo);
        connectPanel.add(connectButton);

        add(connectPanel, BorderLayout.NORTH);


        JPanel chatPanel = new JPanel(new GridLayout(1, 2));


        JPanel messagePanel = new JPanel(new BorderLayout());
        messageListModel = new DefaultListModel<>();
        JList<String> messageList = new JList<>(messageListModel);

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane messageScroll = new JScrollPane(messageList);
        messagePanel.add(messageScroll, BorderLayout.CENTER);

        inputField = new JTextField();
        inputField.setEnabled(false);
        inputField.addActionListener(e -> sendMessage());
        JButton sendButton = new JButton("Send");
        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> sendMessage());

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        messagePanel.add(inputPanel, BorderLayout.SOUTH);


        JPanel userPanel = new JPanel(new BorderLayout());
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userScroll = new JScrollPane(userList);
        userPanel.add(new JLabel("Online Users"), BorderLayout.NORTH);
        userPanel.add(userScroll, BorderLayout.CENTER);

        chatPanel.add(messagePanel);
        chatPanel.add(userPanel);
        add(chatPanel, BorderLayout.CENTER);


        connectButton.addActionListener(e -> {
            username = usernameField.getText().trim();
            serverAddress = serverField.getText().trim();
            serverPort = Integer.parseInt(portField.getText().trim());
            useXml = protocolCombo.getSelectedIndex() == 1;

            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a username", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }


            try {
                if (connectToServer()) {
                    usernameField.setEnabled(false);
                    serverField.setEnabled(false);
                    portField.setEnabled(false);
                    protocolCombo.setEnabled(false);
                    connectButton.setEnabled(false);
                    inputField.setEnabled(true);
                    sendButton.setEnabled(true);
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Connection error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });
    }


    private boolean connectToServer() throws IOException, InterruptedException {
        socket = new Socket(serverAddress, serverPort);
        objectOut = new ObjectOutputStream(socket.getOutputStream());
        objectIn = new ObjectInputStream(socket.getInputStream());
        dataOut = new DataOutputStream(socket.getOutputStream());
        dataIn = new DataInputStream(socket.getInputStream());

        xmlClient = new XmlClient(dataIn, dataOut, this);
        objectClient = new ObjectClient(objectIn, objectOut, this);


        if (checkUsername()) {
            JOptionPane.showMessageDialog(this, "Имя " + username + " занято, выберите другое",
                    "Error", JOptionPane.ERROR_MESSAGE);
            closeResources();
            return false;
        }

        defineProtocol();

        startReceiveThread();

        return true;
    }

    private boolean checkUsername() throws IOException {
        objectOut.writeUTF(username);
        objectOut.flush();

        return objectIn.readBoolean();
    }

    private void defineProtocol() throws IOException {
        if (useXml) {
            objectOut.writeUTF("XML");
            objectOut.flush();

            xmlClient.performXmlLogin();
        } else {
            objectOut.writeUTF("OBJECT");
            objectOut.flush();

            objectClient.performObjectLogin();
        }
    }

    private void startReceiveThread() {
        new Thread(this::receiveMessages).start();
    }


    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        try {
            if (useXml) {
                xmlClient.sendMessage(text);
            } else {
                objectClient.sendMessage(text);
            }

            inputField.setText("");

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка отправки: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void receiveMessages() {
        try {
            while (true) {
                if (useXml) {
                    xmlClient.receiveMessages();

                } else {
                    objectClient.receiveMessages();
                }
            }
        } catch (IOException | ClassNotFoundException | ParserConfigurationException | SAXException ex) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Connection lost: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            });
        }
    }

    public void addMessage(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            String formattedMessage = "[" + new Date() + "] " + sender + ": " + message;
            messageListModel.addElement(formattedMessage);
        });
    }

    public void updateUserListObject(Set<String> users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String user : users) {
                userListModel.addElement(user);
            }
        });
    }

    public void updateUserList(String user, boolean add) {
        SwingUtilities.invokeLater(() -> {
            if (add) {
                userListModel.addElement(user);
            } else {
                userListModel.removeElement(user);
            }
        });
    }

    public void updateUserListXml(Element root) {
        NodeList userNodes = root.getElementsByTagName("user");
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (int i = 0; i < userNodes.getLength(); i++) {
                String user = userNodes.item(i).getTextContent();
                if (!user.isEmpty()) {
                    userListModel.addElement(user);
                }
            }
        });
    }


    private void closeResources() {
        try {
            if (objectIn != null) objectIn.close();
            if (objectOut != null) objectOut.close();
            if (dataIn != null) dataIn.close();
            if (dataOut != null) dataOut.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("Error closing resources: " + e.getMessage());
        }
    }

    private void sendLogOutMessage() throws IOException {
        if (useXml) {
            xmlClient.sendLogoutMessage();

        } else {
            objectClient.sendLogOutMessage();
        }
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatClient client = new ChatClient();
            client.setVisible(true);


            client.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    try {
                        if (client.isConnected()) {
                            client.sendLogOutMessage();
                        }

                        client.closeResources();
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(client,
                                "Ошибка при завершении работы: " + e.getMessage());
                    }
                }
            });

        });
    }
}
