import java.io.*;
import java.net.*;
import java.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

// Manages the game server — accepts client connections, handles incoming game actions,
// and broadcasts game state updates to all connected players
public class Hosting {
    public List<String> Players = Collections.synchronizedList(new ArrayList<>());
    public List<String> Usernames = Collections.synchronizedList(new ArrayList<>());
    public List<PrintWriter> Writers = Collections.synchronizedList(new ArrayList<>()); // One writer per connected client

    private GameState gameState;

    public void setPlayersList(List<String> players) {
        this.Players = players;
    }

    // Sends a message to every connected client
    public void broadcastMessage(String message) {
        try {
            synchronized (Writers) {
                for (PrintWriter writer : Writers) {
                    writer.println(message);
                }
            }
            System.out.println("Broadcasted: " + message);
        } catch (Exception e) {
            System.out.println("Error in broadcast");
        }
    }

    // Starts the TCP server on port 9000 in a background thread
    // Accepts new client connections continuously and spawns a handler thread for each one
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

    // Handles communication with a single connected client on its own thread
    // The first message received is the player's username; all others are game actions
    class ClientHandler implements Runnable {
        Boolean firstMessage = true;
        private final Socket clientSocket;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                Writers.add(out);

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Client says: " + message);
                    if (firstMessage == true) {
                        Usernames.add(message); // Register the username on first contact
                        firstMessage = false;
                    }
                    else {
                        handleGameAction(message);
                    }
                }

            } catch (IOException e) {
                System.err.println("Client error: " + e.getMessage());
            } finally {
                // Remove the writer when the client disconnects so they no longer receive broadcasts
                if (out != null) {
                    Writers.remove(out);
                }
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setGameState(GameState gs) {
        this.gameState = gs;
    }

    // Sends a message to a specific player by looking up their index in the Usernames list
    public void sendToPlayer(String playerName, String message) {
        synchronized (Usernames) {
            int index = Usernames.indexOf(playerName);
            if (index != -1 && index < Writers.size()) {
                Writers.get(index).println(message);
                System.out.println("Sent to " + playerName + ": " + message);
            }
        }
    }

    // Sends each player their own private hole cards individually (other players cannot see these)
    public void dealCardsToPlayers() {
        for (String username : Usernames) {
            Player player = gameState.getPlayerByName(username);
            if (player != null && player.Hand != null && player.Hand[0] != null && player.Hand[1] != null) {
                Card card1 = player.Hand[0];
                Card card2 = player.Hand[1];

                String cardMessage = "DEAL|" +
                        card1.getSuit() + "|" + card1.getValue() + "|" +
                        card2.getSuit() + "|" + card2.getValue();

                sendToPlayer(username, cardMessage);
            }
        }
    }

    // Broadcasts the current game state to all players — pot, bet, turn, phase,
    // community cards, and chip counts
    // If the hand is over, announces the winner and schedules the next hand after 3 seconds
    public void broadcastGameState() {
        if (gameState == null) return;

        broadcastMessage("POT|" + gameState.getPot());
        broadcastMessage("CURRENT_BET|" + gameState.getCurrentBet());
        broadcastMessage("TURN|" + gameState.getCurrentPlayerName());
        broadcastMessage("PHASE|" + gameState.getCurrentPhase());

        if (gameState.getCommunityCardCount() > 0) {
            StringBuilder cards = new StringBuilder("COMMUNITY");
            for (int i = 0; i < gameState.getCommunityCardCount(); i++) {
                Card card = gameState.getCommunityCards()[i];
                cards.append("|").append(card.getSuit()).append(",").append(card.getValue());
            }
            broadcastMessage(cards.toString());
        }

        for (Player player : gameState.getPlayers()) {
            broadcastMessage("PLAYER_CHIPS|" + player.Name + "|" + player.chips);
        }

        if (gameState.getCurrentPhase() == GameState.GamePhase.SHOWDOWN) {
            String winner = gameState.getLastWinnerName();
            if (winner != null) {
                broadcastMessage("WINNER|" + winner);

                // Wait 3 seconds then start the next hand automatically
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }

                    // End the tournament if fewer than 2 players still have chips
                    long playersWithChips = gameState.getPlayers().stream()
                            .filter(p -> p.chips > 0).count();
                    if (playersWithChips < 2) {
                        broadcastMessage("GAME_OVER|" + winner);
                        return;
                    }

                    gameState.startNewHand();
                    dealCardsToPlayers();
                    broadcastGameState();
                }).start();
            }
        }
    }

    // Processes a game action message from a client (FOLD, CHECK, CALL, or RAISE)
    // Enforces turn order and rejects actions during SHOWDOWN
    private void handleGameAction(String message) {
        if (gameState == null) return;

        String[] parts = message.split("\\|");
        if (parts.length < 2) return;

        String action = parts[0];
        String playerName = parts[1];

        // Reject actions sent while the hand is over
        if (gameState.getCurrentPhase() == GameState.GamePhase.SHOWDOWN) {
            sendToPlayer(playerName, "ERROR|Hand is over, waiting for next hand");
            return;
        }

        // Only the current player is allowed to act
        String currentPlayer = gameState.getCurrentPlayerName();
        if (!playerName.equals(currentPlayer)) {
            sendToPlayer(playerName, "ERROR|It is not your turn");
            return;
        }

        switch (action) {
            case "FOLD":
                gameState.playerFold(playerName);
                broadcastMessage("ACTION|" + playerName + "|FOLD");
                broadcastGameState();
                break;

            case "CHECK":
                boolean checkSuccess = gameState.playerCheck(playerName);
                if (checkSuccess) {
                    broadcastMessage("ACTION|" + playerName + "|CHECK");
                    broadcastGameState();
                } else {
                    sendToPlayer(playerName, "ERROR|Cannot check - you must call $" + gameState.getCurrentBet());
                    sendToPlayer(playerName, "TURN|" + gameState.getCurrentPlayerName()); // Re-enable buttons
                }
                break;

            case "CALL":
                boolean callSuccess = gameState.playerCall(playerName);
                if (callSuccess) {
                    broadcastMessage("ACTION|" + playerName + "|CALL");
                    broadcastGameState();
                } else {
                    sendToPlayer(playerName, "ERROR|Cannot call");
                    sendToPlayer(playerName, "TURN|" + gameState.getCurrentPlayerName());
                }
                break;

            case "RAISE":
                if (parts.length < 3) break;
                int raiseAmount = Integer.parseInt(parts[2]);
                boolean raiseSuccess = gameState.playerRaise(playerName, raiseAmount);
                if (raiseSuccess) {
                    broadcastMessage("ACTION|" + playerName + "|RAISE|" + raiseAmount);
                    broadcastGameState();
                } else {
                    sendToPlayer(playerName, "ERROR|Cannot raise");
                    sendToPlayer(playerName, "TURN|" + gameState.getCurrentPlayerName());
                }
                break;
        }
    }
}