import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;
import java.io.*;
import java.net.*;
import java.util.*;

public class Hosting {

    public List<String> Players = Collections.synchronizedList(new ArrayList<>());

    private JTextArea playerListTextArea;


    public Hosting (){
        Card[] Deck = new Card[52];


    }
    public void Start() throws IOException {

        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(9000);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    new Thread(clientHandler).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();




    }

    public void UpdatePlayers(JTextArea Text)
    {
        Text.setText("");

        for (int i = 0; i < Players.size(); i++) {
            Text.append(Players.get(i).toString() + "\n");
        }

    }

    // ClientHandler handles communication with a single client
    class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                // Setup input and output streams for communication
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                // Communicate with the client
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Client says: " + message);

                    // Example server response
                    out.println("Message received: " + message);

                    // Optional: break if client says "exit"
                    if (message.equalsIgnoreCase("exit")) {
                        out.println("Goodbye!");
                        break;
                    }
                }

                // Clean up
                clientSocket.close();
                System.out.println("Client disconnected.");

            } catch (IOException e) {
                System.err.println("Error in client communication: " + e.getMessage());
            }
        }
    }

}
