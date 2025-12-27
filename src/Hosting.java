import java.io.*;
import java.net.*;
import java.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;


public class Hosting {
    public List<String> Players = Collections.synchronizedList(new ArrayList<>());
    public List<String> Usernames = Collections.synchronizedList(new ArrayList<>());
    public List<PrintWriter> Writers = Collections.synchronizedList(new ArrayList<>());

    private GameState gameState;


    public void setPlayersList(List<String> players) {
        this.Players = players;
    }

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
                        Usernames.add(message);
                        firstMessage = false;
                    }
                    else {
                    // Handle game actions
                    handleGameAction(message);
                    }
                }

            } catch (IOException e) {
                System.err.println("Client error: " + e.getMessage());
            } finally {
                // Remove writer when client disconnects
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

    public void sendToPlayer(String playerName, String message) {
        synchronized (Usernames) {
            int index = Usernames.indexOf(playerName);
            if (index != -1 && index < Writers.size()) {
                Writers.get(index).println(message);
                System.out.println("Sent to " + playerName + ": " + message);
            }
        }
    }

    // Deal private cards to all players
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

    // Broadcast current game state to all players
    public void broadcastGameState() {
        if (gameState == null) return;

        broadcastMessage("POT|" + gameState.getPot());
        broadcastMessage("CURRENT_BET|" + gameState.getCurrentBet());
        broadcastMessage("TURN|" + gameState.getCurrentPlayerName());
        broadcastMessage("PHASE|" + gameState.getCurrentPhase());

        // Send community cards if any are revealed
        if (gameState.getCommunityCardCount() > 0) {
            StringBuilder cards = new StringBuilder("COMMUNITY");
            for (int i = 0; i < gameState.getCommunityCardCount(); i++) {
                Card card = gameState.getCommunityCards()[i];
                cards.append("|").append(card.getSuit()).append(",").append(card.getValue());
            }
            broadcastMessage(cards.toString());
        }

        // Broadcast all player chip counts
        for (Player player : gameState.getPlayers()) {
            broadcastMessage("PLAYER_CHIPS|" + player.Name + "|" + player.chips);
        }
    }

    // Handle game action messages from clients
    private void handleGameAction(String message) {
        if (gameState == null) return;

        String[] parts = message.split("\\|");
        if (parts.length < 2) return;

        String action = parts[0];
        String playerName = parts[1];

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
                    sendToPlayer(playerName, "ERROR|Cannot check");
                }
                break;

            case "CALL":
                boolean callSuccess = gameState.playerCall(playerName);
                if (callSuccess) {
                    broadcastMessage("ACTION|" + playerName + "|CALL");
                    broadcastGameState();
                } else {
                    sendToPlayer(playerName, "ERROR|Cannot call");
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
                }
                break;
        }
    }


}
