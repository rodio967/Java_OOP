package chat.server;



import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import chat.Config;
import chat.model.Message;
import chat.server.protocol.ServerHandler;
import chat.server.protocol.ObjectHandler;
import chat.server.protocol.XmlHandler;

public class ChatServer {
    private final int port;
    private final int limitMessages = 100;
    private ServerSocket serverSocket;
    private final ExecutorService executorService;
    private final List<ClientHandler> clients;
    private final List<Message> messageHistory;
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

    public void broadcastMessage(Message message, ClientHandler excludeClient) {
        messageHistory.add(message);
        if (messageHistory.size() > limitMessages) {
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
        return new HashSet<>(onlineUsers);
    }

    public List<Message> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }

    private void log(String message) {
        System.out.println("[SERVER LOG] " + new Date() + ": " + message);
    }

    public static void main(String[] args) {
        int port = Config.getServerPort();

        ChatServer server = new ChatServer(port);
        server.start();
    }

    public class ClientHandler implements Runnable {
        private final Socket socket;
        private final ChatServer server;
        private String username;
        private ServerHandler handler;

        public ClientHandler(Socket socket, ChatServer server) {
            this.socket = socket;
            this.server = server;

            try {
                Config.ProtocolType protocolType = Config.getProtocolType();

                switch (protocolType) {
                    case XML:
                        handler = new XmlHandler(socket, server, this);
                        break;
                    case OBJECT:
                        handler = new ObjectHandler(socket, server, this);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown protocol type");
                }
            } catch (IOException e) {
                log("Error creating client handler: " + e.getMessage());
            }

        }

        @Override
        public void run() {
            try {
                String Username = handler.readUsername();

                if (handler.checkUsername(onlineUsers, Username)) {
                    log("Duplicate username: " + Username);
                    return;
                }

                username = Username;
                onlineUsers.add(Username);

                handler.Communication();
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


        public void sendMessage(Message message) {
            try {
                handler.sendMessage(message);
            } catch (IOException e) {
                log("Error sending message to " + username + ": " + e.getMessage());
            }
        }

        public void sendUserEvent(String username, boolean isLogin) {
            try {
                handler.sendUserEvent(username, isLogin);
            } catch (IOException e) {
                log("Error sending user_event to " + this.username + ": " + e.getMessage());
            }
        }


        private void closeResources() {
            try {
                handler.closeResources();
            } catch (IOException e) {
                log("Error closing Server resources: " + e.getMessage());
            }


            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log("Error closing Server socket: " + e.getMessage());
                }
            }
        }
    }
}