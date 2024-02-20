// We need two threads, the one who receives all
// the messages from the server and the other one in the
// one that receives out console input

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client implements Runnable {

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean done;

    @Override
    public void run() {
        try {
            client = new Socket("127.0.0.1", 9999);
            out = new PrintWriter(client.getOutputStream() ,true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            InputHandler inHandler = new InputHandler();
            var t = new Thread(inHandler);
            t.start();

            String inMessage;
            while ((inMessage = in.readLine()) != null) {
                System.out.println(inMessage);
            }
        } catch (IOException e) {
            // TODO: handle
        }
    }

    public void shutdown() {
        done = true;
        try {
            in.close();
            out.close();
            if(!client.isClosed()) {
                client.close();
            }
        } catch (IOException e) {
            // TODO: Handle it
        }
    }

    class InputHandler implements Runnable {
        // we need to constantly ask for a new line input!

        @Override
        public void run() {
            try {
                var inReader = new BufferedReader(new InputStreamReader(System.in));
                while (!done) {
                    String message = inReader.readLine();
                    out.println(message);
                }
            } catch (Exception e) {
                shutdown();
            }
        }
    }

    public static void main(String[] args) {
        var client = new Client();
        client.run();
    }
}
