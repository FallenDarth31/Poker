import java.util.*;

public class HandEvaluator {

    // Hand type constants
    private static final int ROYAL_FLUSH = 1;
    private static final int STRAIGHT_FLUSH = 2;
    private static final int FOUR_OF_A_KIND = 3;
    private static final int FULL_HOUSE = 4;
    private static final int FLUSH = 5;
    private static final int STRAIGHT = 6;
    private static final int THREE_OF_A_KIND = 7;
    private static final int TWO_PAIR = 8;
    private static final int PAIR = 9;
    private static final int HIGH_CARD = 10;

    public static RankHands evaluateHand(Card[] playerHand, Card[] communityCards) {
        // Combine all cards
        Card[] allCards = combineCards(playerHand, communityCards);

        // Check each hand type from best to worst
        RankHands rank;



        if ((rank = checkRoyalFlush(allCards)) != null) return rank;
        if ((rank = checkStraightFlush(allCards)) != null) return rank;
        if ((rank = checkFourOfAKind(allCards)) != null) return rank;
        if ((rank = checkFullHouse(allCards)) != null) return rank;
        if ((rank = checkFlush(allCards)) != null) return rank;
        if ((rank = checkStraight(allCards)) != null) return rank;
        if ((rank = checkThreeOfAKind(allCards)) != null) return rank;
        if ((rank = checkTwoPair(allCards)) != null) return rank;
        if ((rank = checkPair(allCards)) != null) return rank;

        return checkHighCard(allCards);
    }

    private static Card[] combineCards(Card[] hand, Card[] community) {
        Card[] combined = new Card[hand.length + community.length];
        System.arraycopy(hand, 0, combined, 0, hand.length);
        System.arraycopy(community, 0, combined, hand.length, community.length);
        return combined;
    }


    //Kicker -- Used to break ties with cards of the same strength (i.e. A pair of 9's is stronger than a pair of 3's)

    private static RankHands checkRoyalFlush(Card[] cards) {

        RankHands straightFlush = checkStraightFlush(cards);

        // Royal flush is a straight flush with high card of 14 (Ace)
        if (straightFlush != null && straightFlush.values[0] == 14) {
            return new RankHands(ROYAL_FLUSH, straightFlush.values);
        }

        return null; }

    private static RankHands checkStraightFlush(Card[] cards) {
        int[] suitCounts = getSuitCounts(cards);

        // Check each suit that has 5+ cards
        for (int i = 0; i < 4; i++) {
            if (suitCounts[i] >= 5) {
                Suit suit = Suit.values()[i];
                List<Card> suitCards = getCardsOfSuit(cards, suit);

                // Check if these cards form a straight
                Card[] suitArray = suitCards.toArray(new Card[0]);
                RankHands straightCheck = checkStraight(suitArray);

                if (straightCheck != null) {
                    // It's a straight flush!
                    return new RankHands(STRAIGHT_FLUSH, straightCheck.values);
                }
            }
        }

        return null;
    }

    private static RankHands checkFourOfAKind(Card[] cards) {
        int[] counts = getValueCounts(cards);

        // Find four of a kind
        int quadValue = -1;
        for (int i = 13; i >= 1; i--) {
            if (counts[i] == 4) {
                quadValue = i;
                break;
            }
        }

        if (quadValue == -1) return null;

        // Find highest kicker
        int kicker = -1;
        for (int i = 13; i >= 1; i--) {
            if (i != quadValue && counts[i] > 0) {
                kicker = i;
                break;
            }
        }

        int[] values = {quadValue, kicker};
        return new RankHands(FOUR_OF_A_KIND, values);

    }

    private static RankHands checkFullHouse(Card[] cards) {
        int[] counts = getValueCounts(cards);

        // Find three of a kind
        int tripleValue = -1;
        for (int i = 13; i >= 1; i--) {
            if (counts[i] >= 3) {
                tripleValue = i;
                break;
            }
        }

        if (tripleValue == -1) return null;

        // Find a pair (different from triple)
        int pairValue = -1;
        for (int i = 13; i >= 1; i--) {
            if (i != tripleValue && counts[i] >= 2) {
                pairValue = i;
                break;
            }
        }

        if (pairValue == -1) return null;

        int[] values = {tripleValue, pairValue};
        return new RankHands(FULL_HOUSE, values); }

    private static RankHands checkFlush(Card[] cards) {
        int[] suitCounts = getSuitCounts(cards);

        // Find a suit with 5+ cards
        Suit flushSuit = null;
        for (int i = 0; i < 4; i++) {
            if (suitCounts[i] >= 5) {
                flushSuit = Suit.values()[i];
                break;
            }
        }

        if (flushSuit == null) return null;

        // Get all cards of that suit and sort
        List<Card> flushCards = getCardsOfSuit(cards, flushSuit);
        flushCards.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        // Return top 5 cards
        int[] values = new int[5];
        for (int i = 0; i < 5; i++) {
            values[i] = flushCards.get(i).getValue();
        }

        return new RankHands(FLUSH, values);}

    private static RankHands checkStraight(Card[] cards) {
        // Get unique values and sort
        Set<Integer> uniqueValues = new HashSet<>();
        for (Card card : cards) {
            uniqueValues.add(card.getValue());
        }

        List<Integer> sorted = new ArrayList<>(uniqueValues);
        Collections.sort(sorted, Collections.reverseOrder());

        // Check for 5 consecutive cards
        for (int i = 0; i <= sorted.size() - 5; i++) {
            boolean isStraight = true;
            for (int j = 0; j < 4; j++) {
                if (sorted.get(i + j) - sorted.get(i + j + 1) != 1) {
                    isStraight = false;
                    break;
                }
            }

            if (isStraight) {
                int[] values = {sorted.get(i)}; // High card of straight
                return new RankHands(STRAIGHT, values);
            }
        }

        // Check for wheel (A-2-3-4-5)
        if (uniqueValues.contains(14) && uniqueValues.contains(2) &&
                uniqueValues.contains(3) && uniqueValues.contains(4) &&
                uniqueValues.contains(5)) {
            int[] values = {5}; // In wheel, 5 is high card
            return new RankHands(STRAIGHT, values);
        }

        return null;
    }

    private static RankHands checkThreeOfAKind(Card[] cards) {

        int[] counts = getValueCounts(cards);

        // Find three of a kind
        int tripleValue = -1;
        for (int i = 13; i >= 1; i--) {
            if (counts[i] == 3) {
                tripleValue = i;
                break;
            }
        }

        if (tripleValue == -1) return null;

        // Get two highest kickers
        List<Integer> kickers = new ArrayList<>();
        for (int i = 13; i >= 1; i--) {
            if (i != tripleValue && counts[i] > 0) {
                for (int j = 0; j < counts[i]; j++) {
                    kickers.add(i);
                    if (kickers.size() == 2) break;
                }
            }
            if (kickers.size() == 2) break;
        }

        int[] values = {tripleValue, kickers.get(0), kickers.get(1)};
        return new RankHands(THREE_OF_A_KIND, values);

    }

    private static RankHands checkTwoPair(Card[] cards) {
        int[] counts = getValueCounts(cards);

        // Find two pairs (highest to lowest)
        List<Integer> pairs = new ArrayList<>();
        for (int i = 13; i >= 1; i--) {
            if (counts[i] >= 2) {
                pairs.add(i);
            }
        }

        if (pairs.size() < 2) return null; // Need at least 2 pairs

        // Get the two highest pairs
        int pair1 = pairs.get(0);
        int pair2 = pairs.get(1);

        // Find highest kicker
        int kicker = -1;
        for (int i = 13; i >= 1; i--) {
            if (i != pair1 && i != pair2 && counts[i] > 0) {
                kicker = i;
                break;
            }
        }

        int[] values = {pair1, pair2, kicker};
        return new RankHands(TWO_PAIR, values);

    }

    private static RankHands checkPair(Card[] cards) {
        int[] counts = getValueCounts(cards);

        // Find the highest pair
        int pairValue = -1;
        for (int i = 13; i >= 1; i--) {
            if (counts[i] == 2) {
                pairValue = i;
                break;
            }
        }

        if (pairValue == -1) return null; // No pair found

        // Get kickers (remaining high cards)
        List<Integer> kickers = new ArrayList<>();
        for (int i = 13; i >= 1; i--) {
            if (i != pairValue && counts[i] > 0) {
                for (int j = 0; j < counts[i]; j++) {
                    kickers.add(i);
                    if (kickers.size() == 3) break; // Only need 3 kickers
                }
            }
            if (kickers.size() == 3) break;
        }

        // Build values array: [pair, kicker1, kicker2, kicker3]
        int[] values = new int[4];
        values[0] = pairValue;
        for (int i = 0; i < 3; i++) {
            values[i + 1] = kickers.get(i);
        }

        return new RankHands(PAIR, values);

    }

    private static RankHands checkHighCard(Card[] cards) {
        Card[] sorted = cards.clone();
        sortByValue(sorted);

        // Return top 5 cards
        int[] values = new int[5];
        for (int i = 0; i < 5; i++) {
            values[i] = sorted[i].getValue();
        }

        return new RankHands(HIGH_CARD, values);
    }






    //Helper methods

    private static int[] getValueCounts(Card[] cards) {
        int[] counts = new int[14];
        for (Card card : cards) {
            counts[card.getValue()]++;
        }
        return counts;
    }

    private static int[] getSuitCounts(Card[] cards) {
        int[] counts = new int[4];
        for (Card card : cards) {
            counts[card.getSuit().ordinal()]++;
        }
        return counts;
    }

    private static List<Card> getCardsOfSuit(Card[] cards, Suit suit) {
        List<Card> result = new ArrayList<>();
        for (Card card : cards) {
            if (card.getSuit() == suit) {
                result.add(card);
            }
        }
        return result;
    }

    private static void sortByValue(Card[] cards) {
        Arrays.sort(cards, (a, b) -> Integer.compare(b.getValue(), a.getValue()));
    }
}