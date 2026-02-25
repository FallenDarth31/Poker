import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.*;
import java.net.Socket;

// Controller for GameWindow.fxml
// Handles all in-game UI updates received from the server, and sends player actions back
public class GameController {

    @FXML private ImageView communityCard1, communityCard2, communityCard3, communityCard4, communityCard5;
    @FXML private ImageView playerCard1, playerCard2;
    @FXML private Label potLabel, currentBetLabel, phaseLabel, turnLabel;
    @FXML private Label playerNameLabel, playerChipsLabel;
    @FXML private TextArea playersListArea;
    @FXML private Button foldButton, checkButton, callButton, raiseButton;
    @FXML private TextField raiseAmountField;
    @FXML private javafx.scene.layout.VBox winnerBanner;
    @FXML private Label winnerLabel, winnerNameLabel;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    // Initialises the controller by creating new I/O streams from the socket
    public void initialize(Socket socket, String username) throws IOException {
        System.out.println("GameController.initialize() called"); // DEBUG
        this.socket = socket;
        this.username = username;
        this.playerNameLabel.setText(username);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        disableAllButtons();
        startMessageListener();
    }

    // Disables all action buttons — called at startup and after the player submits an action
    private void disableAllButtons() {
        foldButton.setDisable(true);
        checkButton.setDisable(true);
        callButton.setDisable(true);
        raiseButton.setDisable(true);
    }

    // Starts a background thread that continuously reads messages from the server
    private void startMessageListener() {
        new Thread(() -> {
            String message;
            try {
                while ((message = in.readLine()) != null) {
                    System.out.println("GameController received: " + message); // DEBUG
                    handleServerMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Parses each pipe-delimited message from the server and updates the UI accordingly
    // All UI updates run on the JavaFX Application Thread via Platform.runLater()
    private void handleServerMessage(String message) {
        System.out.println("handleServerMessage called with: " + message); // DEBUG
        String[] parts = message.split("\\|");
        String messageType = parts[0];

        Platform.runLater(() -> {
            System.out.println("Processing message type: " + messageType); // DEBUG
            switch (messageType) {
                case "DEAL":
                    System.out.println("Processing DEAL message"); // DEBUG
                    playerChipsMap.clear(); // Reset chip map for the new hand
                    winnerBanner.setVisible(false);
                    // Clear community cards from the previous hand
                    communityCard1.setImage(null);
                    communityCard2.setImage(null);
                    communityCard3.setImage(null);
                    communityCard4.setImage(null);
                    communityCard5.setImage(null);
                    // DEAL|Suit|Value|Suit|Value
                    if (parts.length >= 5) {
                        System.out.println("Card 1: " + parts[1] + " " + parts[2]); // DEBUG
                        System.out.println("Card 2: " + parts[3] + " " + parts[4]); // DEBUG

                        Image card1Img = getCardImage(parts[1], Integer.parseInt(parts[2]));
                        Image card2Img = getCardImage(parts[3], Integer.parseInt(parts[4]));

                        if (card1Img != null) {
                            playerCard1.setImage(card1Img);
                            System.out.println("Card 1 image set"); // DEBUG
                        }
                        if (card2Img != null) {
                            playerCard2.setImage(card2Img);
                            System.out.println("Card 2 image set"); // DEBUG
                        }
                    }
                    break;

                case "POT":
                    potLabel.setText("$" + parts[1]);
                    break;

                case "CURRENT_BET":
                    currentBetLabel.setText("Current Bet: $" + parts[1]);
                    callButton.setText("Call $" + parts[1]);
                    break;

                case "TURN":
                    turnLabel.setText("Turn: " + parts[1]);
                    // Enable buttons only if it is this player's turn
                    boolean isMyTurn = parts[1].equals(username);
                    foldButton.setDisable(!isMyTurn);
                    checkButton.setDisable(!isMyTurn);
                    callButton.setDisable(!isMyTurn);
                    raiseButton.setDisable(!isMyTurn);
                    break;

                case "PHASE":
                    phaseLabel.setText("Phase: " + parts[1]);
                    break;

                case "COMMUNITY":
                    // COMMUNITY|Suit,Value|Suit,Value|...
                    updateCommunityCards(parts);
                    break;

                case "PLAYER_CHIPS":
                    // PLAYER_CHIPS|Name|Amount
                    if (parts[1].equals(username)) {
                        playerChipsLabel.setText("Chips: $" + parts[2]);
                    }
                    updatePlayersList(parts[1], parts[2]);
                    break;

                case "ACTION":
                    // ACTION|PlayerName|Action
                    System.out.println(parts[1] + " did " + parts[2]);
                    if (parts[1].equals(username)) {
                        disableAllButtons(); // This player has acted — lock the buttons
                    }
                    break;

                case "WINNER":
                    turnLabel.setText("Winner: " + parts[1]);
                    winnerNameLabel.setText(parts[1] + " WINS!");
                    winnerLabel.setText("🎉 HAND WINNER 🎉");
                    winnerBanner.setVisible(true);
                    disableAllButtons();
                    break;

                case "GAME_OVER":
                    // Tournament is over — one player has all the chips
                    turnLabel.setText("Game Over! Winner: " + parts[1]);
                    phaseLabel.setText("Tournament Complete");
                    winnerNameLabel.setText(parts[1] + " WINS THE GAME!");
                    winnerLabel.setText("🏆 CHAMPION 🏆");
                    winnerBanner.setVisible(true);
                    disableAllButtons();
                    break;

                case "ERROR":
                    // ERROR|Message — lock UI and show the error to the player
                    disableAllButtons();
                    showError(parts[1]);
                    break;
            }
        });
    }

    // Loads the card image for a given suit and value from the resources folder
    // Maps numeric values (e.g. 12) to their name (e.g. "Queen") to build the filename
    private Image getCardImage(String suit, int value) {
        System.out.println("getCardImage called with suit=" + suit + " value=" + value); // DEBUG

        // Normalise the suit string to title case e.g. "SPADES" -> "Spades"
        String suitName = suit.substring(0, 1).toUpperCase() + suit.substring(1).toLowerCase();

        // Map the integer value to the name used in the image filename
        String valueName;
        switch (value) {
            case 14: valueName = "Ace"; break;
            case 2: valueName = "Two"; break;
            case 3: valueName = "Three"; break;
            case 4: valueName = "Four"; break;
            case 5: valueName = "Five"; break;
            case 6: valueName = "Six"; break;
            case 7: valueName = "Seven"; break;
            case 8: valueName = "Eight"; break;
            case 9: valueName = "Nine"; break;
            case 10: valueName = "Ten"; break;
            case 11: valueName = "Jack"; break;
            case 12: valueName = "Queen"; break;
            case 13: valueName = "King"; break;
            default: valueName = "Unknown"; break;
        }

        // File naming convention: "Queen of Spades.png"
        String imagePath = "/resources/cards/" + valueName + " of " + suitName + ".png";

        System.out.println("Trying to load: " + imagePath); // DEBUG

        try {
            Image img = new Image(getClass().getResourceAsStream(imagePath));
            System.out.println("Successfully loaded: " + imagePath); // DEBUG
            return img;
        } catch (Exception e) {
            System.out.println("Failed to load card: " + imagePath);
            e.printStackTrace();
            return null;
        }
    }

    // Clears all community card slots then sets images for each card in the COMMUNITY message
    private void updateCommunityCards(String[] parts) {
        ImageView[] cards = {communityCard1, communityCard2, communityCard3, communityCard4, communityCard5};

        for (ImageView card : cards) {
            card.setImage(null);
        }

        for (int i = 1; i < parts.length && i <= 5; i++) {
            String[] cardData = parts[i].split(",");
            String suit = cardData[0];
            int value = Integer.parseInt(cardData[1]);
            Image img = getCardImage(suit, value);
            if (img != null) {
                cards[i-1].setImage(img);
            }
        }
    }

    // Stores the latest chip count for each player; used to rebuild the players list display
    private final java.util.LinkedHashMap<String, String> playerChipsMap = new java.util.LinkedHashMap<>();

    // Updates the stored chip count for a player and rebuilds the players list text area
    private void updatePlayersList(String name, String chips) {
        playerChipsMap.put(name, chips);

        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, String> entry : playerChipsMap.entrySet()) {
            sb.append(entry.getKey()).append(": $").append(entry.getValue()).append("\n");
        }
        playersListArea.setText(sb.toString());
    }

    // Shows an error alert dialog with the given message
    private void showError(String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(errorMessage);
        alert.showAndWait();
    }

    // Button handlers — each disables all buttons immediately to prevent double-clicking

    @FXML
    private void handleFold() {
        disableAllButtons();
        out.println("FOLD|" + username);
    }

    @FXML
    private void handleCheck() {
        disableAllButtons();
        out.println("CHECK|" + username);
    }

    @FXML
    private void handleCall() {
        disableAllButtons();
        out.println("CALL|" + username);
    }

    // Reads the raise amount from the text field; shows an error if it is not a valid number
    @FXML
    private void handleRaise() {
        try {
            int amount = Integer.parseInt(raiseAmountField.getText());
            if (amount > 0) {
                disableAllButtons();
                out.println("RAISE|" + username + "|" + amount);
                raiseAmountField.clear();
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid raise amount");
        }
    }

    // Alternative initialisation used when transitioning from the lobby
    // Reuses the existing socket streams rather than creating new ones
    public void initializeWithExistingConnection(Socket socket, BufferedReader in, PrintWriter out, String username) {
        System.out.println("GameController.initializeWithExistingConnection() called"); // DEBUG
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.username = username;
        this.playerNameLabel.setText(username);

        disableAllButtons();
        startMessageListener();
    }
}