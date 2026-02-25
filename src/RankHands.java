// Stores the result of a hand evaluation so two hands can be compared
// handType uses 1-10 where 1 is the strongest (Royal Flush) and 10 is the weakest (High Card)
// values[] stores key card values used to break ties within the same hand type
public class RankHands implements Comparable<RankHands> {
    int handType;
    int[] values;

    // Creates a RankHands result with the given hand type and tiebreaker values
    public RankHands(int handType, int[] values) {
        this.handType = handType;
        this.values = values;
    }

    // Compares this hand to another — returns negative if this hand is stronger,
    // positive if weaker, or 0 if equal. Lower handType = stronger hand.
    @Override
    public int compareTo(RankHands other) {
        // First compare by hand category
        if (this.handType != other.handType) {
            return Integer.compare(this.handType, other.handType);
        }
        // Same category — compare tiebreaker values left to right (higher card wins)
        for (int i = 0; i < Math.min(this.values.length, other.values.length); i++) {
            if (this.values[i] != other.values[i]) {
                return Integer.compare(other.values[i], this.values[i]);
            }
        }

        return 0; // Hands are equal
    }
}