// Represents a player in the poker game, storing their name, cards, chips and hand status
public class Player {
    String Name;
    Card[] Hand = new Card[2]; // The player's two private hole cards
    int chips;

    int currentBet;   // How much the player has bet in the current round
    boolean hasFolded; // True if the player has folded this hand
    boolean isAllIn;   // True if the player has committed all their chips

    // Creates a new player with a given name and starting chip count
    public Player(String name, int startingChips) {
        this.Name = name;
        this.chips = startingChips;
        this.currentBet = 0;
        this.hasFolded = false;
        this.isAllIn = false;
    }
}