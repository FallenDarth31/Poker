import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LobbyController {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private Stage stage;
    private volatile boolean keepListening = true; // Add this flag

    public void PassVariables(Socket socket, String username, Stage stage) throws IOException {
        this.socket = socket;
        this.username = username;
        this.stage = stage;

        initialise();
    }

    @FXML
    public void initialise() throws IOException {

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        new Thread(() -> {
            String message;
            while (keepListening) { // Changed from while(true)
                try {
                    if ((message = in.readLine()) == null) break;
                } catch (IOException e) {
                    if (keepListening) { // Only throw if we're still supposed to be listening
                        throw new RuntimeException(e);
                    }
                    break;
                }

                System.out.println("LobbyController received: " + message); // DEBUG

                if (message.equals("StartingGame")) {
                    System.out.println("Starting Game!!!");

                    // Stop this listener
                    keepListening = false;

                    // Transition to game window
                    Platform.runLater(() -> {
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameWindow.fxml"));
                            Parent root = loader.load();

                            GameController gameController = loader.getController();
                            // Pass the existing socket, in, out, and username
                            gameController.initializeWithExistingConnection(socket, in, out, username);

                            // Use the stored stage reference
                            stage.setScene(new Scene(root, 1000, 700));

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

                    break; // Exit the loop
                }
            }
        }).start();
    }

    public void cleanup() {
        keepListening = false;
    }
}