// Enum to represent the four possible suits a card can have
enum Suit {
    Diamonds,
    Hearts,
    Spades,
    Clubs
}

// Represents a single playing card with a suit and a value
public class Card {
    private Suit suit;
    private int value; // 2-10 are face value, 11=Jack, 12=Queen, 13=King, 14=Ace

    public Card() {

    }

    // Sets the suit using an index that maps to the Suit enum (0=Diamonds, 1=Hearts, 2=Spades, 3=Clubs)
    public void SetSuit(int SuitSet) {
        this.suit = Suit.values()[SuitSet];
    }

    // Sets the numeric value of the card
    public void SetValue(int Value) {
        this.value = Value;
    }

    // Returns the suit of the card
    public Suit getSuit() {
        return this.suit;
    }

    // Returns the numeric value of the card
    public int getValue() {
        return this.value;
    }
}