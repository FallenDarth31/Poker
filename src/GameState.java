import java.util.ArrayList;
import java.util.List;

public class GameState {
    private List<String> playerNames;
    private int startingChips;
    private int bigBlind;
    private Deck deck;

    private Card[] communityCards;
    private int communityCardsCount;

    private int pot;
    private int currentBet;

    private GamePhase gamePhase;
    private int currentPlayerIndex;
    private int dealerButtonIndex;

    private boolean[] hasActedThisRound; // NEW: Track who has acted

    enum GamePhase {
        WAITING,
        PRE_FLOP,
        FLOP,
        TURN,
        RIVER,
        SHOWDOWN
    }

    private List<Player> players;

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

    //Prepares for a new hand
    public void startNewHand() {
        // Remove players with 0 chips before dealing
        removeEliminatedPlayers();

        // Clear winner from previous hand
        lastWinnerName = null;

        deck = new Deck();
        deck.shuffle();

        communityCards = new Card[5];
        communityCardsCount = 0;

        pot = 0;
        currentBet = bigBlind;

        for (Player player : players) {
            player.hasFolded = false;
            player.isAllIn = false;
            player.currentBet = 0;
        }

        for (Player player : players) {
            player.Hand[0] = deck.dealCard();
            player.Hand[1] = deck.dealCard();
        }

        dealerButtonIndex = (dealerButtonIndex + 1) % players.size();

        int smallBlindIndex;
        int bigBlindIndex;
        int smallBlindAmount = bigBlind / 2;

        if (players.size() == 2) {
            // Heads-up: dealer is small blind, other player is big blind
            smallBlindIndex = dealerButtonIndex;
            bigBlindIndex = (dealerButtonIndex + 1) % players.size();

            //Sets the smallBlindIndex (Who places the smallBlind)
            players.get(smallBlindIndex).currentBet = smallBlindAmount;
            players.get(smallBlindIndex).chips -= smallBlindAmount;
            pot += smallBlindAmount;

            //Sets the bigBlindIndex (Who places the bigBlind)
            players.get(bigBlindIndex).currentBet = bigBlind;
            players.get(bigBlindIndex).chips -= bigBlind;
            pot += bigBlind;

            // Dealer/small blind acts first pre-flop in heads-up
            currentPlayerIndex = dealerButtonIndex;
        } else {
            // 3+ players: normal blind structure
            smallBlindIndex = (dealerButtonIndex + 1) % players.size();
            bigBlindIndex = (dealerButtonIndex + 2) % players.size();

            //Sets the smallBlindIndex (Who places the smallBlind)
            players.get(smallBlindIndex).currentBet = smallBlindAmount;
            players.get(smallBlindIndex).chips -= smallBlindAmount;
            pot += smallBlindAmount;

            //Sets the bigBlindIndex (Who places the bigBlind)
            players.get(bigBlindIndex).currentBet = bigBlind;
            players.get(bigBlindIndex).chips -= bigBlind;
            pot += bigBlind;

            // 3+ players: left of big blind acts first
            currentPlayerIndex = (dealerButtonIndex + 3) % players.size();
        }

        gamePhase = GamePhase.PRE_FLOP;

        // Initialize action tracking
        hasActedThisRound = new boolean[players.size()];
    }

    public void removeEliminatedPlayers() {
        players.removeIf(player -> player.chips <= 0);

        if (dealerButtonIndex >= players.size()) {
            dealerButtonIndex = 0;
        }
    }

    public void playerFold(String playerName) {
        Player player = getPlayerByName(playerName);

        if (player == null) {
            return; // Player not found
        }

        if (player.hasFolded) {
            return; // Already folded
        }

        player.hasFolded = true;

        // Mark as acted
        int playerIndex = players.indexOf(player);
        if (playerIndex >= 0 && playerIndex < hasActedThisRound.length) {
            hasActedThisRound[playerIndex] = true;
        }

        // Check if only 1 player remains BEFORE trying to advance
        // (advanceToNextPlayer loops while hasFolded and would infinite-loop
        // if called when no valid next player exists)
        if (checkIfHandOver()) {
            return; // Hand is over, no need to advance
        }

        // Hand continues — move to next active player
        advanceToNextPlayer();
    }

    public Player getPlayerByName(String name) {
        for (Player player : players) {
            if (player.Name.equals(name)) {
                return player;
            }
        }
        return null;
    }

    private void advanceToNextPlayer() {
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        } while (players.get(currentPlayerIndex).hasFolded);

        System.out.println("DEBUG: Advanced to player index " + currentPlayerIndex + " (" + players.get(currentPlayerIndex).Name + ")");
    }

    // Stores the winner's name once a hand ends, so broadcastGameState can
    // read it without calling the mutating determineWinner() again.
    private String lastWinnerName = null;

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
            // Award pot to last remaining player
            lastPlayer.chips += pot;
            pot = 0;
            lastWinnerName = lastPlayer.Name;
            gamePhase = GamePhase.SHOWDOWN;
            return true; // hand is over
        }
        return false; // hand continues
    }

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

        if (player.currentBet != currentBet) {
            System.out.println("DEBUG CHECK: FAILED - bets don't match");
            return false;
        }

        // Mark as acted
        int playerIndex = players.indexOf(player);
        if (playerIndex >= 0 && playerIndex < hasActedThisRound.length) {
            hasActedThisRound[playerIndex] = true;
        }

        System.out.println("DEBUG CHECK: Before completion check");
        printActionStatus();

        // Check if betting round is complete BEFORE advancing
        if (isBettingRoundComplete()) {
            System.out.println("DEBUG CHECK: Round complete, advancing phase");
            advancePhase();
        } else {
            System.out.println("DEBUG CHECK: Round not complete, advancing player");
            advanceToNextPlayer();
        }

        return true;
    }

    private void printActionStatus() {
        System.out.println("  Action status:");
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            boolean acted = (i < hasActedThisRound.length) ? hasActedThisRound[i] : false;
            System.out.println("    " + p.Name + ": acted=" + acted +
                             ", currentBet=" + p.currentBet +
                             ", folded=" + p.hasFolded);
        }
    }

    private boolean isBettingRoundComplete() {
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);

            if (player.hasFolded || player.isAllIn) {
                continue;
            }

            // Player must have acted AND matched the current bet
            if (i >= hasActedThisRound.length || !hasActedThisRound[i] || player.currentBet != currentBet) {
                return false;
            }
        }

        return true;
    }

    private void advancePhase() {
        switch (gamePhase) {
            case PRE_FLOP:
                // Deal the flop (3 cards)
                communityCards[0] = deck.dealCard();
                communityCards[1] = deck.dealCard();
                communityCards[2] = deck.dealCard();
                communityCardsCount = 3;
                gamePhase = GamePhase.FLOP;
                System.out.println("DEBUG: Advanced to FLOP");
                break;

            case FLOP:
                // Deal the turn (1 card)
                communityCards[3] = deck.dealCard();
                communityCardsCount = 4;
                gamePhase = GamePhase.TURN;
                break;

            case TURN:
                // Deal the river (1 card)
                communityCards[4] = deck.dealCard();
                communityCardsCount = 5;
                gamePhase = GamePhase.RIVER;
                break;

            case RIVER:
                // Betting done, go to showdown
                gamePhase = GamePhase.SHOWDOWN;
                // Award pot and record winner (mutates state once)
                awardPotToWinner();
                break;

            case SHOWDOWN:
                // Hand is over, start new hand
                startNewHand();
                break;

            default:
                break;
        }

        // Reset betting for new round
        if (gamePhase != GamePhase.SHOWDOWN) {
            resetBettingRound();
        }
    }

    private void resetBettingRound() {
        System.out.println("DEBUG: resetBettingRound() called");
        currentBet = 0;

        // Reset each player's current bet for this round
        for (Player player : players) {
            player.currentBet = 0;
        }

        // Reset action tracking
        hasActedThisRound = new boolean[players.size()];

        if (players.size() == 2) {
            // Heads-up post-flop: small blind (dealer) acts first
            currentPlayerIndex = dealerButtonIndex;
        } else {
            // 3+ players: left of dealer acts first
            currentPlayerIndex = (dealerButtonIndex + 1) % players.size();
        }

        // Skip folded players
        while (players.get(currentPlayerIndex).hasFolded) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        }

        System.out.println("DEBUG: After reset, current player is " + players.get(currentPlayerIndex).Name);
    }

    public boolean playerCall(String playerName) {
        Player player = getPlayerByName(playerName);

        if (player == null || player.hasFolded) {
            return false;
        }

        // Calculate how much more they need to put in
        int amountToCall = currentBet - player.currentBet;

        if (amountToCall <= 0) {
            // Already matched the bet - treat as a check
            return playerCheck(playerName);
        }

        // Check if player has enough chips
        if (amountToCall >= player.chips) {
            // Not enough to call - go all-in with what they have
            int allInAmount = player.chips;
            pot += allInAmount;
            player.currentBet += allInAmount;
            player.chips = 0;
            player.isAllIn = true;
        } else {
            // Normal call - player has enough chips
            player.chips -= amountToCall;
            player.currentBet = currentBet;
            pot += amountToCall;
        }

        // Mark as acted
        int playerIndex = players.indexOf(player);
        if (playerIndex >= 0 && playerIndex < hasActedThisRound.length) {
            hasActedThisRound[playerIndex] = true;
        }

        // Check if betting round is complete BEFORE advancing
        if (isBettingRoundComplete()) {
            advancePhase();
        } else {
            advanceToNextPlayer();
        }

        return true;
    }

    public boolean playerRaise(String playerName, int raiseAmount) {
        Player player = getPlayerByName(playerName);

        if (player == null || player.hasFolded) {
            return false; // Invalid
        }

        // Calculate the total amount needed (call current bet + raise)
        int amountToCall = currentBet - player.currentBet;
        int totalAmount = amountToCall + raiseAmount;

        int minimumRaise = bigBlind;
        if (raiseAmount < minimumRaise) {
            return false;
        }

        // Check if player has enough chips
        if (totalAmount > player.chips) {
            // Not enough for full raise - go all-in instead
            int allInAmount = player.chips;
            pot += allInAmount;
            player.currentBet += allInAmount;
            player.chips = 0;
            player.isAllIn = true;

            if (player.currentBet > currentBet) {
                currentBet = player.currentBet;
            }
        } else {
            // Normal raise
            player.chips -= totalAmount;
            player.currentBet += totalAmount;
            pot += totalAmount;
            currentBet = player.currentBet;
        }

        // Mark as acted
        int playerIndex = players.indexOf(player);
        if (playerIndex >= 0 && playerIndex < hasActedThisRound.length) {
            hasActedThisRound[playerIndex] = true;
        }

        // When someone raises, everyone else needs to act again
        // (but skip folded / all-in players — they can't act)
        for (int i = 0; i < hasActedThisRound.length; i++) {
            if (i != playerIndex && !players.get(i).hasFolded && !players.get(i).isAllIn) {
                hasActedThisRound[i] = false;
            }
        }

        advanceToNextPlayer();

        return true;
    }

    // Called exactly once when a hand ends (either from advancePhase on RIVER,
    // or from checkIfHandOver on fold-out). Awards pot and stores winner name.
    private void awardPotToWinner() {
        RankHands bestHand = null;
        Player winner = null;

        for (Player player : players) {
            if (player.hasFolded) {
                continue;
            }

            RankHands playerHand = HandEvaluator.evaluateHand(player.Hand, communityCards);

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

    // Pure getter — safe to call multiple times, does not mutate anything.
    public String getLastWinnerName() {
        return lastWinnerName;
    }

    //Getter methods

    // Getters so Hosting can read and broadcast game state
    public int getPot() {
        return pot;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public GamePhase getCurrentPhase() {
        return gamePhase;
    }

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