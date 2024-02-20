import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private final ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                // server.accept returns a new socket
                Socket client = server.accept();
                var handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
            shutdown();
        }
    }

    public void broadcast(String message) {
        for (var c : connections) {
            if (c != null) {
                c.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        try {
            done = true;
            pool.shutdown();
            if (!server.isClosed()) {
                server.close();
            }
            for (var c : connections) {
                c.shutdown();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    class ConnectionHandler implements Runnable {

        private final Socket client;
        private BufferedReader in; // used to get the in stream from the socket
        private PrintWriter out; // used to get the out stream into the socket

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                // Printing the message into the socket's out stream
                out.println("Please enter your nickname: ");
                String nickname = in.readLine();
                System.out.println(nickname + " Connected");
                broadcast(nickname + " Joined the chat");
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/nick ")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            broadcast(nickname + "renamed themselves to " + messageSplit[1]);
                            System.out.println(nickname + "renamed themselves to " + messageSplit[1]);
                            nickname = messageSplit[1];
                            out.println("Successfully changed the nickname to " + nickname);
                        } else {
                            out.println("no nickname provided");
                        }
                    } else if (message.startsWith("/quit")) {
                        broadcast(nickname + " left the server");
                        System.out.println(nickname + " left the server");
                        shutdown();
                    } else {
                        broadcast(nickname + ": " + message);
                    }
                }

            } catch (IOException e) {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static void main(String[] args) {
        var server = new Server();
        server.run();
    }
}
