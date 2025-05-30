package chat.client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;

import chat.Config;
import chat.client.protocol.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class ChatClient extends JFrame {
    private final int limitMessages = 100;
    private String username;
    private String serverAddress;
    private int serverPort;
    private Socket socket;
    private ClientHandler handler;

    private final DefaultListModel<String> userListModel;
    private final DefaultListModel<String> messageListModel;

    private final JTextArea messageArea;
    private final JTextField inputField;
    private final JList<String> userList;

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
        JTextField serverField = new JTextField(Config.getServerIp(), 10);
        JTextField portField = new JTextField(Integer.toString(Config.getServerPort()), 5);
        JButton connectButton = new JButton("Connect");

        connectPanel.add(new JLabel("Username:"));
        connectPanel.add(usernameField);
        connectPanel.add(new JLabel("Server:"));
        connectPanel.add(serverField);
        connectPanel.add(new JLabel("Port:"));
        connectPanel.add(portField);
        connectPanel.add(connectButton);

        portField.setEnabled(false);
        serverField.setEnabled(false);

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
        userPanel.add(userScroll, BorderLayout.CENTER);

        chatPanel.add(messagePanel);
        chatPanel.add(userPanel);
        add(chatPanel, BorderLayout.CENTER);


        connectButton.addActionListener(e -> {
            username = usernameField.getText().trim();
            serverAddress = Config.getServerIp();
            serverPort = Config.getServerPort();

            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a username", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }


            try {
                if (connectToServer()) {
                    usernameField.setEnabled(false);
                    serverField.setEnabled(false);
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
        Config.ProtocolType protocolType = Config.getProtocolType();

        switch (protocolType) {
            case XML:
                handler = new XmlHandler(socket, this);
                break;
            case OBJECT:
                handler = new ObjectHandler(socket, this);
                break;
            default:
                throw new IllegalArgumentException("Unknown protocol type");
        }

        if (handler.checkUsername(username)) {
            JOptionPane.showMessageDialog(this, "Имя " + username + " занято, выберите другое",
                    "Error", JOptionPane.ERROR_MESSAGE);
            closeResources();
            return false;
        }

        handler.performLogin();

        startReceiveThread();
        return true;
    }


    private void startReceiveThread() {
        new Thread(this::receiveMessages).start();
    }


    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        try {
            handler.sendMessage(text);

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
                handler.receiveMessages();
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

            if (messageListModel.getSize() >= limitMessages) {
                messageListModel.remove(0);
            }

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
        System.out.println("Close Client");

        try {
            handler.closeResources();
        } catch (IOException e) {
            System.out.println("Error closing Client resources: " + e.getMessage());
        }


        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing Client socket: " + e.getMessage());
            }
        }
    }

    private void sendLogOutMessage() throws IOException {
        handler.sendLogoutMessage();
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
