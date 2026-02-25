import java.util.*;

// Evaluates the best 5-card poker hand from a player's hole cards and the community cards
public class HandEvaluator {

    // Constants representing each hand type — lower number means stronger hand
    private static final int ROYAL_FLUSH     = 1;
    private static final int STRAIGHT_FLUSH  = 2;
    private static final int FOUR_OF_A_KIND  = 3;
    private static final int FULL_HOUSE      = 4;
    private static final int FLUSH           = 5;
    private static final int STRAIGHT        = 6;
    private static final int THREE_OF_A_KIND = 7;
    private static final int TWO_PAIR        = 8;
    private static final int PAIR            = 9;
    private static final int HIGH_CARD       = 10;

    // Combines the player's hole cards with the community cards, then checks each hand
    // type from strongest to weakest and returns the first match found
    public static RankHands evaluateHand(Card[] playerHand, Card[] communityCards) {
        Card[] allCards = combineCards(playerHand, communityCards);

        RankHands rank;

        if ((rank = checkRoyalFlush(allCards))    != null) return rank;
        if ((rank = checkStraightFlush(allCards)) != null) return rank;
        if ((rank = checkFourOfAKind(allCards))   != null) return rank;
        if ((rank = checkFullHouse(allCards))     != null) return rank;
        if ((rank = checkFlush(allCards))         != null) return rank;
        if ((rank = checkStraight(allCards))      != null) return rank;
        if ((rank = checkThreeOfAKind(allCards))  != null) return rank;
        if ((rank = checkTwoPair(allCards))       != null) return rank;
        if ((rank = checkPair(allCards))          != null) return rank;

        return checkHighCard(allCards);
    }

    // Merges the player's hole cards and community cards into one array
    private static Card[] combineCards(Card[] hand, Card[] community) {
        Card[] combined = new Card[hand.length + community.length];
        System.arraycopy(hand, 0, combined, 0, hand.length);
        System.arraycopy(community, 0, combined, hand.length, community.length);
        return combined;
    }

    // A Royal Flush is a Straight Flush where the highest card is an Ace (14)
    private static RankHands checkRoyalFlush(Card[] cards) {
        RankHands straightFlush = checkStraightFlush(cards);
        if (straightFlush != null && straightFlush.values[0] == 14) {
            return new RankHands(ROYAL_FLUSH, straightFlush.values);
        }
        return null;
    }

    // Checks for five consecutive cards all of the same suit
    private static RankHands checkStraightFlush(Card[] cards) {
        int[] suitCounts = getSuitCounts(cards);

        for (int i = 0; i < 4; i++) {
            if (suitCounts[i] >= 5) {
                Suit suit = Suit.values()[i];
                List<Card> suitCards = getCardsOfSuit(cards, suit);

                // Check if the suited cards also form a straight
                Card[] suitArray = suitCards.toArray(new Card[0]);
                RankHands straightCheck = checkStraight(suitArray);

                if (straightCheck != null) {
                    return new RankHands(STRAIGHT_FLUSH, straightCheck.values);
                }
            }
        }
        return null;
    }

    // Checks for four cards of the same value; stores the value and the highest remaining kicker
    private static RankHands checkFourOfAKind(Card[] cards) {
        int[] counts = getValueCounts(cards);

        int quadValue = -1;
        for (int i = 14; i >= 2; i--) {
            if (counts[i] == 4) {
                quadValue = i;
                break;
            }
        }

        if (quadValue == -1) return null;

        // Find the highest remaining card as a kicker for tiebreaking
        int kicker = -1;
        for (int i = 14; i >= 2; i--) {
            if (i != quadValue && counts[i] > 0) {
                kicker = i;
                break;
            }
        }

        int[] values = {quadValue, kicker};
        return new RankHands(FOUR_OF_A_KIND, values);
    }

    // Checks for three cards of one value and two of another (a triple and a pair)
    private static RankHands checkFullHouse(Card[] cards) {
        int[] counts = getValueCounts(cards);

        int tripleValue = -1;
        for (int i = 14; i >= 2; i--) {
            if (counts[i] >= 3) {
                tripleValue = i;
                break;
            }
        }

        if (tripleValue == -1) return null;

        int pairValue = -1;
        for (int i = 14; i >= 2; i--) {
            if (i != tripleValue && counts[i] >= 2) {
                pairValue = i;
                break;
            }
        }

        if (pairValue == -1) return null;

        int[] values = {tripleValue, pairValue};
        return new RankHands(FULL_HOUSE, values);
    }

    // Checks for five or more cards of the same suit; returns the top 5 values for tiebreaking
    private static RankHands checkFlush(Card[] cards) {
        int[] suitCounts = getSuitCounts(cards);

        Suit flushSuit = null;
        for (int i = 0; i < 4; i++) {
            if (suitCounts[i] >= 5) {
                flushSuit = Suit.values()[i];
                break;
            }
        }

        if (flushSuit == null) return null;

        List<Card> flushCards = getCardsOfSuit(cards, flushSuit);
        flushCards.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int[] values = new int[5];
        for (int i = 0; i < 5; i++) {
            values[i] = flushCards.get(i).getValue();
        }

        return new RankHands(FLUSH, values);
    }

    // Checks for five consecutive card values (suit does not matter)
    // Also handles the special A-2-3-4-5 "wheel" straight where the Ace acts as low
    private static RankHands checkStraight(Card[] cards) {
        // Use a set to remove duplicate values before checking for consecutive cards
        Set<Integer> uniqueValues = new HashSet<>();
        for (Card card : cards) {
            uniqueValues.add(card.getValue());
        }

        List<Integer> sorted = new ArrayList<>(uniqueValues);
        Collections.sort(sorted, Collections.reverseOrder());

        // Slide a window of 5 and check if all values are consecutive
        for (int i = 0; i <= sorted.size() - 5; i++) {
            boolean isStraight = true;
            for (int j = 0; j < 4; j++) {
                if (sorted.get(i + j) - sorted.get(i + j + 1) != 1) {
                    isStraight = false;
                    break;
                }
            }

            if (isStraight) {
                int[] values = {sorted.get(i)}; // Store the high card of the straight
                return new RankHands(STRAIGHT, values);
            }
        }

        // Check for the wheel straight: A-2-3-4-5 (Ace used as low card)
        if (uniqueValues.contains(14) && uniqueValues.contains(2) &&
                uniqueValues.contains(3) && uniqueValues.contains(4) &&
                uniqueValues.contains(5)) {
            int[] values = {5}; // 5 is the effective high card in a wheel
            return new RankHands(STRAIGHT, values);
        }

        return null;
    }

    // Checks for exactly three cards of the same value; also stores the two highest kickers
    private static RankHands checkThreeOfAKind(Card[] cards) {
        int[] counts = getValueCounts(cards);

        int tripleValue = -1;
        for (int i = 14; i >= 2; i--) {
            if (counts[i] == 3) {
                tripleValue = i;
                break;
            }
        }

        if (tripleValue == -1) return null;

        // Collect the two highest remaining cards as kickers
        List<Integer> kickers = new ArrayList<>();
        for (int i = 14; i >= 2; i--) {
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

    // Checks for two separate pairs; also stores the highest remaining kicker
    private static RankHands checkTwoPair(Card[] cards) {
        int[] counts = getValueCounts(cards);

        List<Integer> pairs = new ArrayList<>();
        for (int i = 14; i >= 2; i--) {
            if (counts[i] >= 2) {
                pairs.add(i);
            }
        }

        if (pairs.size() < 2) return null;

        int pair1 = pairs.get(0);
        int pair2 = pairs.get(1);

        // Find the highest remaining card as the kicker
        int kicker = -1;
        for (int i = 14; i >= 2; i--) {
            if (i != pair1 && i != pair2 && counts[i] > 0) {
                kicker = i;
                break;
            }
        }

        int[] values = {pair1, pair2, kicker};
        return new RankHands(TWO_PAIR, values);
    }

    // Checks for a single pair; also stores the three highest remaining kickers for tiebreaking
    private static RankHands checkPair(Card[] cards) {
        int[] counts = getValueCounts(cards);

        int pairValue = -1;
        for (int i = 14; i >= 2; i--) {
            if (counts[i] == 2) {
                pairValue = i;
                break;
            }
        }

        if (pairValue == -1) return null;

        // Collect the three highest remaining cards as kickers
        List<Integer> kickers = new ArrayList<>();
        for (int i = 14; i >= 2; i--) {
            if (i != pairValue && counts[i] > 0) {
                for (int j = 0; j < counts[i]; j++) {
                    kickers.add(i);
                    if (kickers.size() == 3) break;
                }
            }
            if (kickers.size() == 3) break;
        }

        int[] values = new int[4];
        values[0] = pairValue;
        for (int i = 0; i < 3; i++) {
            values[i + 1] = kickers.get(i);
        }

        return new RankHands(PAIR, values);
    }

    // Returns the five highest cards when no other hand type is found
    private static RankHands checkHighCard(Card[] cards) {
        Card[] sorted = cards.clone();
        sortByValue(sorted);

        int[] values = new int[5];
        for (int i = 0; i < 5; i++) {
            values[i] = sorted[i].getValue();
        }

        return new RankHands(HIGH_CARD, values);
    }


    // Helper methods

    // Returns an array where index i holds the count of cards with value i
    private static int[] getValueCounts(Card[] cards) {
        int[] counts = new int[15];
        for (Card card : cards) {
            counts[card.getValue()]++;
        }
        return counts;
    }

    // Returns an array where index i holds the count of cards with suit i
    private static int[] getSuitCounts(Card[] cards) {
        int[] counts = new int[4];
        for (Card card : cards) {
            counts[card.getSuit().ordinal()]++;
        }
        return counts;
    }

    // Returns a list of all cards matching the given suit
    private static List<Card> getCardsOfSuit(Card[] cards, Suit suit) {
        List<Card> result = new ArrayList<>();
        for (Card card : cards) {
            if (card.getSuit() == suit) {
                result.add(card);
            }
        }
        return result;
    }

    // Sorts an array of cards in descending order of value (highest first)
    private static void sortByValue(Card[] cards) {
        Arrays.sort(cards, (a, b) -> Integer.compare(b.getValue(), a.getValue()));
    }
}