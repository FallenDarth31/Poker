import java.util.ArrayList;
import java.util.List;

// Manages the complete state of an active poker game including players, the deck,
// community cards, the pot, whose turn it is, and the current game phase
public class GameState {
    private List<String> playerNames;
    private int startingChips;
    private int bigBlind;
    private Deck deck;

    private Card[] communityCards;
    private int communityCardsCount;

    private int pot;
    private int currentBet; // The highest bet placed this round; players must match it to stay in

    private GamePhase gamePhase;
    private int currentPlayerIndex;   // Index of the player whose turn it currently is
    private int dealerButtonIndex;    // Index of the current dealer; rotates each hand

    private boolean[] hasActedThisRound; // Tracks which players have acted in the current betting round

    // The phases a poker hand moves through in order
    enum GamePhase {
        WAITING,
        PRE_FLOP,
        FLOP,
        TURN,
        RIVER,
        SHOWDOWN
    }

    private List<Player> players;

    // Initialises the game with the given player names, starting chip count and big blind amount
    public GameState(List<String> playerNames, int startingChips, int bigBlind) {
        this.playerNames = new ArrayList<>(playerNames);
        this.startingChips = startingChips;
        this.bigBlind = bigBlind;

        this.deck = new Deck();
        this.deck.shuffle();

        this.players = new ArrayList<>();
        for (String name : playerNames) {
            players.add(new Player(name, startingChips));
        }

        this.communityCards = new Card[5];
        this.communityCardsCount = 0;
        this.pot = 0;
        this.currentBet = 0;
        this.gamePhase = GamePhase.WAITING;
        this.currentPlayerIndex = 0;
        this.dealerButtonIndex = 0;
    }

    // Prepares for a new hand — removes eliminated players, resets the deck and all hand state,
    // deals two hole cards to each player, and posts the small and big blinds
    public void startNewHand() {
        removeEliminatedPlayers();

        lastWinnerName = null;

        deck = new Deck();
        deck.shuffle();

        communityCards = new Card[5];
        communityCardsCount = 0;

        pot = 0;
        currentBet = bigBlind;

        // Reset each player's hand state for the new round
        for (Player player : players) {
            player.hasFolded = false;
            player.isAllIn = false;
            player.currentBet = 0;
        }

        // Deal two hole cards to each player
        for (Player player : players) {
            player.Hand[0] = deck.dealCard();
            player.Hand[1] = deck.dealCard();
        }

        // Move the dealer button to the next player
        dealerButtonIndex = (dealerButtonIndex + 1) % players.size();

        int smallBlindIndex;
        int bigBlindIndex;
        int smallBlindAmount = bigBlind / 2;

        if (players.size() == 2) {
            // Heads-up rule: the dealer is also the small blind
            smallBlindIndex = dealerButtonIndex;
            bigBlindIndex = (dealerButtonIndex + 1) % players.size();

            players.get(smallBlindIndex).currentBet = smallBlindAmount;
            players.get(smallBlindIndex).chips -= smallBlindAmount;
            pot += smallBlindAmount;

            players.get(bigBlindIndex).currentBet = bigBlind;
            players.get(bigBlindIndex).chips -= bigBlind;
            pot += bigBlind;

            // In heads-up, the dealer/small blind acts first pre-flop
            currentPlayerIndex = dealerButtonIndex;
        } else {
            // Standard structure: small blind is left of dealer, big blind is next
            smallBlindIndex = (dealerButtonIndex + 1) % players.size();
            bigBlindIndex = (dealerButtonIndex + 2) % players.size();

            players.get(smallBlindIndex).currentBet = smallBlindAmount;
            players.get(smallBlindIndex).chips -= smallBlindAmount;
            pot += smallBlindAmount;

            players.get(bigBlindIndex).currentBet = bigBlind;
            players.get(bigBlindIndex).chips -= bigBlind;
            pot += bigBlind;

            // Player left of the big blind acts first pre-flop
            currentPlayerIndex = (dealerButtonIndex + 3) % players.size();
        }

        gamePhase = GamePhase.PRE_FLOP;

        // Initialise action tracking — no players have acted yet
        hasActedThisRound = new boolean[players.size()];
    }

    // Removes any players who have run out of chips before the next hand begins
    public void removeEliminatedPlayers() {
        players.removeIf(player -> player.chips <= 0);

        // Prevent the dealer index going out of bounds after removal
        if (dealerButtonIndex >= players.size()) {
            dealerButtonIndex = 0;
        }
    }

    // Marks the player as folded, then checks if the hand is over or advances to the next player
    public void playerFold(String playerName) {
        Player player = getPlayerByName(playerName);

        if (player == null) {
            return;
        }

        if (player.hasFolded) {
            return;
        }

        player.hasFolded = true;

        // Mark this player as having acted
        int playerIndex = players.indexOf(player);
        if (playerIndex >= 0 && playerIndex < hasActedThisRound.length) {
            hasActedThisRound[playerIndex] = true;
        }

        // If only one player is left, the hand ends immediately
        if (checkIfHandOver()) {
            return;
        }

        advanceToNextPlayer();
    }

    // Finds and returns a player by their username, or null if not found
    public Player getPlayerByName(String name) {
        for (Player player : players) {
            if (player.Name.equals(name)) {
                return player;
            }
        }
        return null;
    }

    // Moves the turn to the next player who has not folded
    private void advanceToNextPlayer() {
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        } while (players.get(currentPlayerIndex).hasFolded);

        System.out.println("DEBUG: Advanced to player index " + currentPlayerIndex + " (" + players.get(currentPlayerIndex).Name + ")");
    }

    // Stores the winner's name once a hand ends so it can be broadcast without re-evaluating hands
    private String lastWinnerName = null;

    // Checks if only one player remains; if so, awards them the pot and ends the hand
    private boolean checkIfHandOver() {
        int activePlayers = 0;
        Player lastPlayer = null;

        for (Player player : players) {
            if (!player.hasFolded) {
                activePlayers++;
                lastPlayer = player;
            }
        }

        if (activePlayers == 1) {
            lastPlayer.chips += pot;
            pot = 0;
            lastWinnerName = lastPlayer.Name;
            gamePhase = GamePhase.SHOWDOWN;
            return true;
        }
        return false;
    }

    // Processes a check action — only valid if the player's bet already matches the current bet
    // If valid, advances the phase or moves to the next player depending on whether the round is complete
    public boolean playerCheck(String playerName) {
        Player player = getPlayerByName(playerName);
        if (player == null || player.hasFolded) {
            System.out.println("DEBUG CHECK: Player null or folded");
            return false;
        }

        System.out.println("DEBUG CHECK: " + playerName + " attempting check");
        System.out.println("  - player.currentBet = " + player.currentBet);
        System.out.println("  - gameState.currentBet = " + currentBet);
        System.out.println("  - gamePhase = " + gamePhase);

        // Player can only check if they have already matched the current bet
        if (player.currentBet != currentBet) {
            System.out.println("DEBUG CHECK: FAILED - bets don't match");
            return false;
        }

        int playerIndex = players.indexOf(player);
        if (playerIndex >= 0 && playerIndex < hasActedThisRound.length) {
            hasActedThisRound[playerIndex] = true;
        }

        System.out.println("DEBUG CHECK: Before completion check");
        printActionStatus();

        if (isBettingRoundComplete()) {
            System.out.println("DEBUG CHECK: Round complete, advancing phase");
            advancePhase();
        } else {
            System.out.println("DEBUG CHECK: Round not complete, advancing player");
            advanceToNextPlayer();
        }

        return true;
    }

    // Debug helper that prints each player's action status to the console
    private void printActionStatus() {
        System.out.println("  Action status:");
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            boolean acted = (i < hasActedThisRound.length) ? hasActedThisRound[i] : false;
            System.out.println("    " + p.Name + ": acted=" + acted +
                    ", currentBet=" + p.currentBet +
                    ", folded=" + p.hasFolded +
                    ", allIn=" + p.isAllIn);
        }
    }

    // Returns true if every active player (not folded, not all-in) has acted
    // and matched the current bet — meaning the betting round is finished
    private boolean isBettingRoundComplete() {
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);

            if (player.hasFolded || player.isAllIn) {
                continue;
            }

            if (i >= hasActedThisRound.length || !hasActedThisRound[i] || player.currentBet != currentBet) {
                return false;
            }
        }

        return true;
    }

    // Moves the game to the next phase and deals the appropriate community cards
    // PRE_FLOP -> FLOP (3 cards), FLOP -> TURN (1 card), TURN -> RIVER (1 card), RIVER -> SHOWDOWN
    private void advancePhase() {
        switch (gamePhase) {
            case PRE_FLOP:
                communityCards[0] = deck.dealCard();
                communityCards[1] = deck.dealCard();
                communityCards[2] = deck.dealCard();
                communityCardsCount = 3;
                gamePhase = GamePhase.FLOP;
                System.out.println("DEBUG: Advanced to FLOP");
                break;

            case FLOP:
                communityCards[3] = deck.dealCard();
                communityCardsCount = 4;
                gamePhase = GamePhase.TURN;
                break;

            case TURN:
                communityCards[4] = deck.dealCard();
                communityCardsCount = 5;
                gamePhase = GamePhase.RIVER;
                break;

            case RIVER:
                // All community cards are out — determine the winner
                gamePhase = GamePhase.SHOWDOWN;
                awardPotToWinner();
                break;

            case SHOWDOWN:
                startNewHand();
                break;

            default:
                break;
        }

        // Reset betting for the new round (not needed at showdown)
        if (gamePhase != GamePhase.SHOWDOWN) {
            resetBettingRound();
        }
    }

    // Resets all betting state at the start of a new round and sets the first player to act
    private void resetBettingRound() {
        System.out.println("DEBUG: resetBettingRound() called");
        currentBet = 0;

        for (Player player : players) {
            player.currentBet = 0;
        }

        // Clear action tracking so all players must act again in the new round
        hasActedThisRound = new boolean[players.size()];

        if (players.size() == 2) {
            currentPlayerIndex = dealerButtonIndex; // Heads-up: dealer acts first post-flop
        } else {
            currentPlayerIndex = (dealerButtonIndex + 1) % players.size(); // Left of dealer acts first
        }

        // Skip any folded players
        while (players.get(currentPlayerIndex).hasFolded) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        }

        System.out.println("DEBUG: After reset, current player is " + players.get(currentPlayerIndex).Name);
    }

    // Processes a call action — the player puts in enough chips to match the current bet
    // If they cannot cover the full amount, they go all-in with their remaining chips
    public boolean playerCall(String playerName) {
        Player player = getPlayerByName(playerName);

        if (player == null || player.hasFolded) {
            return false;
        }

        int amountToCall = currentBet - player.currentBet;

        if (amountToCall <= 0) {
            // Player has already matched the bet, treat as a check
            return playerCheck(playerName);
        }

        if (amountToCall >= player.chips) {
            // Not enough chips to fully call — go all-in
            int allInAmount = player.chips;
            pot += allInAmount;
            player.currentBet += allInAmount;
            player.chips = 0;
            player.isAllIn = true;
        } else {
            player.chips -= amountToCall;
            player.currentBet = currentBet;
            pot += amountToCall;
        }

        int playerIndex = players.indexOf(player);
        if (playerIndex >= 0 && playerIndex < hasActedThisRound.length) {
            hasActedThisRound[playerIndex] = true;
        }

        if (isBettingRoundComplete()) {
            advancePhase();
        } else {
            advanceToNextPlayer();
        }

        return true;
    }

    // Processes a raise action — the player matches the current bet then adds the raise amount on top
    // After a raise, all other active players must act again
    public boolean playerRaise(String playerName, int raiseAmount) {
        Player player = getPlayerByName(playerName);

        if (player == null || player.hasFolded) {
            return false;
        }

        int amountToCall = currentBet - player.currentBet;
        int totalAmount = amountToCall + raiseAmount;

        // Raise must be at least the size of the big blind
        int minimumRaise = bigBlind;
        if (raiseAmount < minimumRaise) {
            return false;
        }

        if (totalAmount > player.chips) {
            // Not enough chips — go all-in instead
            int allInAmount = player.chips;
            pot += allInAmount;
            player.currentBet += allInAmount;
            player.chips = 0;
            player.isAllIn = true;

            if (player.currentBet > currentBet) {
                currentBet = player.currentBet;
            }
        } else {
            player.chips -= totalAmount;
            player.currentBet += totalAmount;
            pot += totalAmount;
            currentBet = player.currentBet;
        }

        int playerIndex = players.indexOf(player);
        if (playerIndex >= 0 && playerIndex < hasActedThisRound.length) {
            hasActedThisRound[playerIndex] = true;
        }

        // Reset action flags for all other active players so they must respond to the raise
        for (int i = 0; i < hasActedThisRound.length; i++) {
            if (i != playerIndex && !players.get(i).hasFolded && !players.get(i).isAllIn) {
                hasActedThisRound[i] = false;
            }
        }

        advanceToNextPlayer();

        return true;
    }

    // Evaluates all remaining players' hands and awards the pot to the winner
    private void awardPotToWinner() {
        RankHands bestHand = null;
        Player winner = null;

        for (Player player : players) {
            if (player.hasFolded) {
                continue;
            }

            RankHands playerHand = HandEvaluator.evaluateHand(player.Hand, communityCards);

            // compareTo() returns negative if playerHand is stronger than bestHand
            if (bestHand == null || playerHand.compareTo(bestHand) < 0) {
                bestHand = playerHand;
                winner = player;
            }
        }

        if (winner != null) {
            winner.chips += pot;
            pot = 0;
            lastWinnerName = winner.Name;
        }
    }

    // Returns the name of the winner of the last completed hand
    public String getLastWinnerName() {
        return lastWinnerName;
    }

    // Getter methods — used by Hosting to read and broadcast the game state

    public int getPot() {
        return pot;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public GamePhase getCurrentPhase() {
        return gamePhase;
    }

    // Returns the name of the player whose turn it currently is
    public String getCurrentPlayerName() {
        if (currentPlayerIndex >= 0 && currentPlayerIndex < players.size()) {
            return players.get(currentPlayerIndex).Name;
        }
        return null;
    }

    public Card[] getCommunityCards() {
        return communityCards;
    }

    public int getCommunityCardCount() {
        return communityCardsCount;
    }

    public List<Player> getPlayers() {
        return players;
    }
}