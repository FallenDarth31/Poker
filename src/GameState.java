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

        int smallBlindIndex = (dealerButtonIndex + 1) % players.size();
        int bigBlindIndex = (dealerButtonIndex + 2) % players.size();

        int smallBlindAmount = bigBlind / 2;

        //Sets the smallBlindIndex (Who places the smallBlind)
        players.get(smallBlindIndex).currentBet = smallBlindAmount;
        players.get(smallBlindIndex).chips -= smallBlindAmount;
        pot += smallBlindAmount;

        //Sets the bigBlindIndex (Who places the bigBlind)
        players.get(bigBlindIndex).currentBet = bigBlind;
        players.get(bigBlindIndex).chips -= bigBlind;
        pot += bigBlind;

        currentPlayerIndex = (dealerButtonIndex + 3) % players.size();

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

        // Move to next player
        advanceToNextPlayer();

        //Check if only 1 player remains
        checkIfHandOver();
    }

    private Player getPlayerByName(String name) {
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
    }

    private void checkIfHandOver() {
        int activePlayers = 0;
        for (Player player : players) {
            if (!player.hasFolded) {
                activePlayers++;
            }
        }

        if (activePlayers == 1) {
            // Hand is over, award pot to last player

        }
    }

    public boolean playerCheck(String playerName) {
        Player player = getPlayerByName(playerName);
        if (player == null || player.hasFolded) {
            return false;
        }

        if (player.currentBet != currentBet) {
            return false;

        }

        advanceToNextPlayer();

        if (isBettingRoundComplete()) {
            advancePhase();
        }

        return true;

    }

    private boolean isBettingRoundComplete() {
        for (Player player : players) {
            // Skip folded players
            if (player.hasFolded) {
                continue;
            }

            // Skip all-in players (they can't act)
            if (player.isAllIn) {
                continue;
            }

            // If any active player hasn't matched the current bet, round isn't done
            if (player.currentBet != currentBet) {
                return false;
            }
        }

        return true; // Everyone has matched
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
        currentBet = 0;

        // Reset each player's current bet for this round
        for (Player player : players) {
            player.currentBet = 0;
        }

        // First to act is left of dealer (different from pre-flop)
        currentPlayerIndex = (dealerButtonIndex + 1) % players.size();

        // Skip folded players
        while (players.get(currentPlayerIndex).hasFolded) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        }
    }

}
