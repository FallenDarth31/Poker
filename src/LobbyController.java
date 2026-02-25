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

// Controller for LobbyWindow.fxml
// Displays a waiting screen after the player connects, and listens for the host to start the game
public class LobbyController {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private Stage stage;
    private volatile boolean keepListening = true; // Used to stop the listener thread when the game starts

    // Receives the socket and player details from ClientController and begins listening for messages
    public void PassVariables(Socket socket, String username, Stage stage) throws IOException {
        this.socket = socket;
        this.username = username;
        this.stage = stage;

        initialise();
    }

    // Sets up I/O streams and starts a background thread that waits for the "StartingGame" signal
    // When received, transitions to the game screen using the existing socket connection
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
            while (keepListening) {
                try {
                    if ((message = in.readLine()) == null) break;
                } catch (IOException e) {
                    if (keepListening) {
                        throw new RuntimeException(e);
                    }
                    break;
                }

                System.out.println("LobbyController received: " + message);

                if (message.equals("StartingGame")) {
                    System.out.println("Starting Game!!!");

                    keepListening = false; // Stop this listener — GameController will take over

                    Platform.runLater(() -> {
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameWindow.fxml"));
                            Parent root = loader.load();

                            GameController gameController = loader.getController();
                            // Pass the existing connection so we don't need to reconnect
                            gameController.initializeWithExistingConnection(socket, in, out, username);

                            stage.setScene(new Scene(root, 1000, 800));

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

                    break;
                }
            }
        }).start();
    }

    // Stops the background listener thread gracefully when the lobby is closed
    public void cleanup() {
        keepListening = false;
    }
}