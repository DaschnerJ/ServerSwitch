import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ClientRegister {

    // The server socket.
    private static ServerSocket serverSocket = null;
    // The client socket.
    private static Socket clientSocket = null;

    public static Handler handler = new Handler();

    static Thread commandReader = null;


    public static void main(String args[]) {
        commandReader = new Thread() {
            public void run() {
                Scanner reader = new Scanner(System.in);
                while (Handler.running) {
                    // Reading from System.in
                    String line = reader.nextLine();
                    handler.command(line);// Scans the next token of the input as an int.
                    //once finished

                }
                reader.close();
            }
        };
        commandReader.start();

        // The default port number.
        int portNumber = 2222;
        if (args.length < 1) {
            System.out.println("Usage: java MultiThreadChatServerSync <portNumber>\n"
                    + "Now using port number=" + portNumber);
        } else {
            portNumber = Integer.valueOf(args[0]).intValue();
        }

        /*
         * Open a server socket on the portNumber (default 2222). Note that we can
         * not choose a port less than 1023 if we are not privileged users (root).
         */
        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            System.out.println(e);
        }

        /*
         * Create a client socket for each connection and pass it to a new client
         * thread.
         */
        while (true) {
            try {
                clientSocket = serverSocket.accept();
                int i = 0;
                for (i = 0; i < Handler.maxClientsCount; i++) {
                    if (Handler.threads[i] == null) {
                        (Handler.threads[i] = new client(clientSocket, Handler.threads)).start();
                        break;
                    }
                }
                if (i == Handler.maxClientsCount) {
                    PrintStream os = new PrintStream(clientSocket.getOutputStream());
                    os.println("Server too busy. Try later.");
                    os.close();
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}

/*
 * The chat client thread. This client thread opens the input and the output
 * streams for a particular client, ask the client's name, informs all the
 * clients connected to the server about the fact that a new client has joined
 * the chat room, and as long as it receive data, echos that data back to all
 * other clients. The thread broadcast the incoming messages to all clients and
 * routes the private message to the particular client. When a client leaves the
 * chat room this thread informs also all the clients about that and terminates.
 */
class client extends Thread {

    private String clientName = null;
    private DataInputStream is = null;
    private PrintStream os = null;
    protected Socket clientSocket = null;
    protected String serial = null;
    private final client[] threads;
    private int maxClientsCount;

    private MessageHandler mh = new MessageHandler();

    public client(Socket clientSocket, client[] threads) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        maxClientsCount = threads.length;
    }

    public void run() {
        int maxClientsCount = this.maxClientsCount;
        client[] threads = this.threads;

        try {
            /*
             * Create input and output streams for this client.
             */
            is = new DataInputStream(clientSocket.getInputStream());
            os = new PrintStream(clientSocket.getOutputStream());
            String name;
            while (true) {
                os.println("Requesting identification...");
                name = clientSocket.getRemoteSocketAddress().toString();
                System.out.println("New registered client from IP:Port of: " + name);
                name = is.readLine().trim();
                serial = name;
                ClientRegister.handler.register(this, serial);
                if (name.indexOf('@') == -1) {
                    break;
                } else {
                    os.println("The name should not contain '@' character.");
                }

            }

            /* Welcome the new the client. */
            os.println("Client has been registered.");
            synchronized (this) {
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] != null && threads[i] == this) {
                        clientName = "@" + name;
                        break;
                    }
                }
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] != null && threads[i] != this) {
                        System.out.println("Message all clients of new client.");
                        threads[i].os.println("A new client has connected to the network.");
                    }
                }
            }
            /* Start the conversation. */
            while (true) {
                //Read the lines...
                String line = is.readLine();
                //Default command to quit.
                if (line.equals("/quit")) {
                    break;
                }
                else if(line.startsWith("error"))
                {
                    ClientRegister.handler.message(line);
                }
                else {
                    //Handle commands here
                    mh.command(line, this);
                }
            }
            synchronized (this) {
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] != null && threads[i] != this
                            && threads[i].clientName != null) {
                        threads[i].os.println("A client has left the network.");
                    }
                }
            }
            os.println("Thank you for using Ez client.");

            /*
             * Clean up. Set the current thread variable to null so that a new client
             * could be accepted by the server.
             */
            synchronized (this) {
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] == this) {
                        threads[i] = null;
                    }
                }
            }
            /*
             * Close the output stream, close the input stream, close the socket.
             */
            is.close();
            os.close();
            clientSocket.close();
        } catch (IOException e) {
        }
    }

    protected void sendMessage(String msg) {
        os.println(msg);
    }
}

