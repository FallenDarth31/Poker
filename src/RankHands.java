public class RankHands implements Comparable<RankHands> {
    int handType;        // 1 = Royal Flush ... 10 = High Card
    int[] values;        // The important card values for tiebreaking

    public RankHands(int handType, int[] values) {
        this.handType = handType;
        this.values = values;
    }

    @Override
    public int compareTo(RankHands other) {
        if (this.handType != other.handType) {
            return Integer.compare(this.handType, other.handType);
        }
                for (int i = 0; i < Math.min(this.values.length, other.values.length); i++) {
            if (this.values[i] != other.values[i]) {
                // Higher card value wins
                return Integer.compare(other.values[i], this.values[i]);
            }
        }

        return 0;
    }
}