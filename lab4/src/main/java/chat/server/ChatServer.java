package chat.server;



import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import chat.model.ChatMessage;
import chat.server.protocol.ObjectProtocolHandler;
import chat.server.protocol.XmlProtocolHandler;

public class ChatServer {
    private static final int DEFAULT_PORT = 5555;
    private final int port;
    private ServerSocket serverSocket;
    private final ExecutorService executorService;
    private final List<ClientHandler> clients;
    private final List<ChatMessage> messageHistory;
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    private boolean running;


    public ChatServer(int port) {
        this.port = port;
        this.executorService = Executors.newCachedThreadPool();
        this.clients = new CopyOnWriteArrayList<>();
        this.messageHistory = new CopyOnWriteArrayList<>();
    }

    public void start() {
        running = true;

        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (running) {
                if (scanner.nextLine().equalsIgnoreCase("exit")) {
                    stop();
                    break;
                }
            }
            scanner.close();
        }).start();


        try {
            serverSocket = new ServerSocket(port);
            log("Server started on port " + port);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);


                clients.add(clientHandler);
                executorService.execute(clientHandler);
            }
        } catch (IOException e) {
            if (running) {
                log("Server error: " + e.getMessage());
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log("Error closing server: " + e.getMessage());
        }
        executorService.shutdownNow();
        log("Server stopped");
    }

    public void broadcastMessage(ChatMessage message, ClientHandler excludeClient) {
        messageHistory.add(message);
        if (messageHistory.size() > 100) {
            messageHistory.remove(0);
        }

        for (ClientHandler client : clients) {
            if (client != excludeClient) {
                client.sendMessage(message);
            }
        }
    }

    public void broadcastUserEvent(String username, boolean isLogin) {
        for (ClientHandler client : clients) {
            client.sendUserEvent(username, isLogin);
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);

        if (client.getUsername() != null) {
            broadcastUserEvent(client.getUsername(), false);
            log("User " + client.getUsername() + " disconnected");
        }
    }


    public Set<String> getOnlineUsers() {
        return onlineUsers;
    }

    public List<ChatMessage> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }

    private void log(String message) {
        System.out.println("[SERVER LOG] " + new Date() + ": " + message);
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        ChatServer server = new ChatServer(port);
        server.start();
    }

    public class ClientHandler implements Runnable {
        private final Socket socket;
        private final ChatServer server;
        private ObjectInputStream objectIn;
        private ObjectOutputStream objectOut;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;
        private String username;
        private boolean useXml;

        public XmlProtocolHandler xmlHandler;
        public ObjectProtocolHandler objectHandler;

        public ClientHandler(Socket socket, ChatServer server) {

            this.socket = socket;
            this.server = server;
            try {
                this.objectOut = new ObjectOutputStream(socket.getOutputStream());
                this.objectIn = new ObjectInputStream(socket.getInputStream());
                this.dataOut = new DataOutputStream(socket.getOutputStream());
                this.dataIn = new DataInputStream(socket.getInputStream());

                this.xmlHandler = new XmlProtocolHandler(dataIn, dataOut, server, this);
                this.objectHandler = new ObjectProtocolHandler(objectIn, objectOut, server, this);

            } catch (IOException e) {
                log("Error creating client handler: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                String UserName = objectIn.readUTF();

                if (checkUsername(UserName)) {
                    log("Duplicate username: " + UserName);
                    return;
                }

                username = UserName;
                onlineUsers.add(UserName);

                defineProtocol();
            } catch (IOException e) {
                log("Client connection error: " + e.getMessage());
            } finally {
                if (username != null) {
                    onlineUsers.remove(username);
                }
                server.removeClient(this);
                closeResources();
            }
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getUsername() {
            return username;
        }

        public boolean checkUsername(String UserName) throws IOException {
            boolean nameAvailable = onlineUsers.contains(UserName);

            objectOut.writeBoolean(nameAvailable);
            objectOut.flush();

            return nameAvailable;
        }

        public void defineProtocol() throws IOException {
            String protocol = objectIn.readUTF();
            useXml = "XML".equals(protocol);
            log("Client connected using protocol: " + protocol);

            if (useXml) {
                xmlHandler.handleXmlCommunication();
            } else {
                objectHandler.HandleObjectCommunication();
            }
        }


        public void sendMessage(ChatMessage message) {
            try {
                if (useXml) {
                    xmlHandler.sendMessage(message);
                } else {
                    objectHandler.sendMessage(message);
                }
            } catch (IOException e) {
                log("Error sending message to " + username + ": " + e.getMessage());
            }
        }

        public void sendUserEvent(String username, boolean isLogin) {
            try {
                if (useXml) {
                    xmlHandler.sendUserEvent(username, isLogin);
                } else {
                    objectHandler.sendUserEvent(username, isLogin);
                }
            } catch (IOException e) {
                log("Error sending user event to " + this.username + ": " + e.getMessage());
            }
        }


        private void closeResources() {
            try {
                if (objectIn != null) objectIn.close();
                if (objectOut != null) objectOut.close();
                if (dataIn != null) dataIn.close();
                if (dataOut != null) dataOut.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                log("Error closing resources: " + e.getMessage());
            }
        }
    }
}