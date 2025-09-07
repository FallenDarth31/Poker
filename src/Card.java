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

    void SetSuit(int SuitSet) {
        this.suit = Suit.values()[SuitSet];
    }
    void SetValue(int Value) {
        this.value = Value;
    }
}
