import java.util.ArrayList;
import java.util.Collections;

// Represents a standard 52-card deck, with methods to shuffle and deal cards
public class Deck {
    private ArrayList<Card> cards;

    // Creates all 52 cards by looping through 4 suits and values 2-14
    public Deck() {
        cards = new ArrayList<>();
        for (int suit = 0; suit < 4; suit++) {
            for (int value = 2; value <= 14; value++) {
                Card card = new Card();
                card.SetSuit(suit);
                card.SetValue(value);
                cards.add(card);
            }
        }
    }

    // Randomly shuffles the deck
    public void shuffle() {
        Collections.shuffle(cards);
    }

    // Removes and returns the top card from the deck, or null if the deck is empty
    public Card dealCard() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.remove(0);
    }
}