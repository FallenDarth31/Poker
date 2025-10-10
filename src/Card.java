enum Suit {
    Diamonds,
    Hearts,
    Spades,
    Clubs
}

public class Card{
    private Suit suit;
    private int value;

    public Card (){

    }

    public void SetSuit(int SuitSet) {
        this.suit = Suit.values()[SuitSet];
    }
    public void SetValue(int Value) {
        this.value = Value;
    }

    public Suit getSuit() {
        return this.suit;
    }

    public int getValue() {
        return this.value;
    }
}
