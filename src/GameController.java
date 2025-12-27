import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.*;
import java.net.Socket;

public class GameController {

    @FXML private ImageView communityCard1, communityCard2, communityCard3, communityCard4, communityCard5;
    @FXML private ImageView playerCard1, playerCard2;
    @FXML private Label potLabel, currentBetLabel, phaseLabel, turnLabel;
    @FXML private Label playerNameLabel, playerChipsLabel;
    @FXML private TextArea playersListArea;
    @FXML private Button foldButton, checkButton, callButton, raiseButton;
    @FXML private TextField raiseAmountField;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public void initialize(Socket socket, String username) throws IOException {
        System.out.println("GameController.initialize() called"); // DEBUG
        this.socket = socket;
        this.username = username;
        this.playerNameLabel.setText(username);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Disable buttons initially
        disableAllButtons();

        // Start listening for server messages
        startMessageListener();
    }

    private void disableAllButtons() {
        foldButton.setDisable(true);
        checkButton.setDisable(true);
        callButton.setDisable(true);
        raiseButton.setDisable(true);
    }

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

    private void handleServerMessage(String message) {
        System.out.println("handleServerMessage called with: " + message); // DEBUG
        String[] parts = message.split("\\|");
        String messageType = parts[0];

        Platform.runLater(() -> {
            System.out.println("Processing message type: " + messageType); // DEBUG
            switch (messageType) {
                case "DEAL":
                    System.out.println("Processing DEAL message"); // DEBUG
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
                    // Enable/disable buttons based on if it's your turn
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
                    break;

                case "ERROR":
                    // ERROR|Message
                    showError(parts[1]);
                    break;
            }
        });
    }

    // Helper method to load card image
    private Image getCardImage(String suit, int value) {
        System.out.println("getCardImage called with suit=" + suit + " value=" + value); // DEBUG

        // Convert suit enum to proper case (e.g., "SPADES" -> "Spades")
        String suitName = suit.substring(0, 1).toUpperCase() + suit.substring(1).toLowerCase();

        // Convert value to card name
        String valueName;
        switch (value) {
            case 1:
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

        // Match your actual file names: "Queen of Spades.png"
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

    private void updateCommunityCards(String[] parts) {
        ImageView[] cards = {communityCard1, communityCard2, communityCard3, communityCard4, communityCard5};

        // Clear all cards first
        for (ImageView card : cards) {
            card.setImage(null);
        }

        // Set visible cards
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

    private void updatePlayersList(String name, String chips) {
        String entry = name + ": $" + chips + "\n";
        String currentText = playersListArea.getText();

        if (!currentText.contains(name + ":")) {
            playersListArea.appendText(entry);
        }
    }

    private void showError(String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(errorMessage);
        alert.showAndWait();
    }

    // Button handlers
    @FXML
    private void handleFold() {
        out.println("FOLD|" + username);
        disableAllButtons();
    }

    @FXML
    private void handleCheck() {
        out.println("CHECK|" + username);
        disableAllButtons();
    }

    @FXML
    private void handleCall() {
        out.println("CALL|" + username);
        disableAllButtons();
    }

    @FXML
    private void handleRaise() {
        try {
            int amount = Integer.parseInt(raiseAmountField.getText());
            if (amount > 0) {
                out.println("RAISE|" + username + "|" + amount);
                raiseAmountField.clear();
                disableAllButtons();
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid raise amount");
        }
    }
    public void initializeWithExistingConnection(Socket socket, BufferedReader in, PrintWriter out, String username) {
        System.out.println("GameController.initializeWithExistingConnection() called"); // DEBUG
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.username = username;
        this.playerNameLabel.setText(username);

        // Disable buttons initially
        disableAllButtons();

        // Start listening for server messages
        startMessageListener();
    }
}