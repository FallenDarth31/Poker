import java.io.*;
import java.net.*;
import java.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;


public class Hosting {
    public List<String> Players = Collections.synchronizedList(new ArrayList<>());
    public List<String> Usernames = Collections.synchronizedList(new ArrayList<>());


    public void setPlayersList(List<String> players) {
        this.Players = players;
    }


    public void Start() throws IOException {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(9000)) {
                System.out.println("Server started on port 9000...");

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    new Thread(clientHandler).start();
                    Players.add(clientSocket.getInetAddress().getHostAddress());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    class ClientHandler implements Runnable {
        Boolean firstMessage = true;
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Client says: " + message);
                    if (firstMessage == true) {
                        Usernames.add(message);
                        firstMessage = false;
                    }
                    //out.println("Message received: " + message);
                }

            } catch (IOException e) {
                System.err.println("Client error: " + e.getMessage());
            }
        }
    }

}
