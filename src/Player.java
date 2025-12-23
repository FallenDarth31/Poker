public class Player {
    String Name;
    Card[] Hand = new Card[2];
    int chips;

    int currentBet;
    boolean hasFolded;
    boolean isAllIn;

    public Player(String name, int startingChips) {
        this.Name = name;
        this.chips = startingChips;
        this.currentBet = 0;
        this.hasFolded = false;
        this.isAllIn = false;
    }

}