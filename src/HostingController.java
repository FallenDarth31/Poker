
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.List;

// Controller for HostingWindow.fxml
// Lets the host start the server, view connected players, and launch the game
public class HostingController {

    Hosting H = new Hosting();

    @FXML
    private TextArea UsernameList; // Displays the usernames of connected players
    @FXML
    private TextArea ServerIP;     // Shows the host's local IP address for others to connect to
    @FXML
    private Button ConnectButton;

    Boolean hostingstarted = false; // Prevents StartGame() being called before the server is running

    private GameState gameState;

    String IP;

    public List<String> Players = Collections.synchronizedList(new ArrayList<>());

    // Runs on startup — starts a background thread that refreshes the player list every second,
    // and displays the host machine's local IP address
    @FXML
    public void initialize() throws UnknownHostException {

        new Thread(()-> {
            System.out.println("Working");
            while (true) {
                StringBuilder UsernameBox = new StringBuilder();

                // Build a list of connected usernames from the Hosting instance
                synchronized (H.Usernames) {
                    for (int i = 0; i < H.Usernames.size(); i++) {
                        UsernameBox.append(H.Usernames.get(i)).append("\n");
                    }
                }

                String finalText = UsernameBox.toString();
                // Update the UI on the JavaFX Application Thread
                Platform.runLater(() -> {
                    UsernameList.setText(finalText);
                });

                try {
                    Thread.sleep(1000); // Poll once per second
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        InetAddress myIP = InetAddress.getLocalHost();
        IP = myIP.getHostAddress();
        ServerIP.setText(IP);
    }

    // Starts the server when the host clicks the "Host" button
    @FXML
    private void Host() throws IOException {
        System.out.println("Host Game clicked!");
        H.setPlayersList(Players);
        H.Start();
        System.out.println("Working");

        Players = H.Players;
        hostingstarted = true;
    }

    // Starts the game when the host clicks "Start Game"
    // Creates the game state, deals the first hand, and signals all clients to enter the game screen
    @FXML
    private void StartGame() {
        if (hostingstarted) {
            try {
                // Initialise with all connected players, 1000 starting chips and a 50 big blind
                gameState = new GameState(H.Usernames, 1000, 50);
                H.setGameState(gameState);

                gameState.startNewHand();

                H.broadcastMessage("StartingGame"); // Tells clients to switch to the game screen

                H.dealCardsToPlayers();

                H.broadcastGameState();

            } catch (Exception e) {
                System.out.println("Broadcast Error (1)");
            }
        } else {
            System.out.println("Debug: Not Hosting");
        }
    }
}