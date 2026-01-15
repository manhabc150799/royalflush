package com.mygame.shared.game.tienlen;

import com.mygame.shared.game.card.Card;
import java.util.*;

/**
 * Class quản lý và validate combinations trong Tiến Lên Miền Nam.
 * Rules:
 * - Sorting: 3 < ... < A < 2. Suits: Spade(1) < Club(2) < Diamond(3) <
 * Heart(4).
 * - Combinations: Single, Pair, Triple, Quad, Straight (No 2), PairSeq (3+
 * pairs).
 * - Comparisons: Highest card (Rank + Suit).
 */
public class CardCollection {

    /**
     * Sort hand according to Tien Len rules.
     */
    public static void sortHandTienLen(List<Card> cards) {
        if (cards == null)
            return;
        cards.sort((c1, c2) -> {
            int rank1 = c1.getRankValueForTienLen();
            int rank2 = c2.getRankValueForTienLen();
            if (rank1 != rank2) {
                return Integer.compare(rank1, rank2);
            }
            return Integer.compare(c1.getSuitValueForTienLen(), c2.getSuitValueForTienLen());
        });
    }

    /**
     * Detect combination type from a list of cards.
     */
    public static TienLenCombinationType detectCombination(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            return TienLenCombinationType.INVALID;
        }

        // Must sort first to ensure logic works
        List<Card> sorted = new ArrayList<>(cards);
        sortHandTienLen(sorted);

        int size = sorted.size();

        if (size == 1) {
            return TienLenCombinationType.SINGLE;
        }

        if (size == 2) {
            if (isSameRank(sorted))
                return TienLenCombinationType.PAIR;
            return TienLenCombinationType.INVALID;
        }

        if (size == 3) {
            if (isSameRank(sorted))
                return TienLenCombinationType.TRIPLE;
            if (isStraight(sorted))
                return TienLenCombinationType.STRAIGHT;
            return TienLenCombinationType.INVALID;
        }

        if (size == 4) {
            if (isSameRank(sorted))
                return TienLenCombinationType.FOUR_OF_A_KIND;
            if (isStraight(sorted))
                return TienLenCombinationType.STRAIGHT;
            return TienLenCombinationType.INVALID;
        }

        // 5+ cards
        if (isStraight(sorted))
            return TienLenCombinationType.STRAIGHT;
        if (isPairSequence(sorted))
            return TienLenCombinationType.PAIR_SEQUENCE;

        return TienLenCombinationType.INVALID;
    }

    /**
     * Check if 'currentCards' can beat 'prevCards'.
     */
    public static boolean canBeat(TienLenCombinationType prevType, List<Card> prevCards,
            TienLenCombinationType currentType, List<Card> currentCards) {
        if (prevType == null || currentType == null)
            return false;

        // Sort both for consistent comparison
        List<Card> prev = new ArrayList<>(prevCards);
        List<Card> curr = new ArrayList<>(currentCards);
        sortHandTienLen(prev);
        sortHandTienLen(curr);

        // --- CHOP RULES (Hàng chặt Hàng / Heo) ---

        // 1. 3 PAIRS SEQUENCE (3 Đôi Thông)
        if (currentType == TienLenCombinationType.PAIR_SEQUENCE && curr.size() == 6) {
            // Cuts Single Pig (any 2)
            if (prevType == TienLenCombinationType.SINGLE && isPig(prev.get(0)))
                return true;
            // Cuts smaller 3 Pairs Sequence
            if (prevType == TienLenCombinationType.PAIR_SEQUENCE && prev.size() == 6) {
                return compareHightestCard(prev, curr) < 0;
            }
        }

        // 2. QUAD (Tứ Quý)
        if (currentType == TienLenCombinationType.FOUR_OF_A_KIND) {
            // Cuts Single Pig OR Pair of Pigs
            if (prevType == TienLenCombinationType.SINGLE && isPig(prev.get(0)))
                return true;
            if (prevType == TienLenCombinationType.PAIR && isPairOfPigs(prev))
                return true;
            // Cuts 3 Pairs Sequence
            if (prevType == TienLenCombinationType.PAIR_SEQUENCE && prev.size() == 6)
                return true;
            // Cuts smaller Quad
            if (prevType == TienLenCombinationType.FOUR_OF_A_KIND) {
                return compareHightestCard(prev, curr) < 0;
            }
        }

        // 3. 4 PAIRS SEQUENCE (4 Đôi Thông - Bất tử cắt)
        if (currentType == TienLenCombinationType.PAIR_SEQUENCE && curr.size() == 8) {
            // Cuts Single Pig, Pair Pigs
            if (prevType == TienLenCombinationType.SINGLE && isPig(prev.get(0)))
                return true;
            if (prevType == TienLenCombinationType.PAIR && isPairOfPigs(prev))
                return true;
            // Cuts 3 Pairs Sequence, Quad
            if (prevType == TienLenCombinationType.PAIR_SEQUENCE && prev.size() == 6)
                return true;
            if (prevType == TienLenCombinationType.FOUR_OF_A_KIND)
                return true;
            // Cuts smaller 4 Pairs Sequence
            if (prevType == TienLenCombinationType.PAIR_SEQUENCE && prev.size() == 8) {
                return compareHightestCard(prev, curr) < 0;
            }
        }

        // --- NORMAL RULES ---

        // Must be same type and same size
        if (prevType != currentType)
            return false;
        if (prev.size() != curr.size())
            return false;

        // Compare highest card (Rank + Suit)
        return compareHightestCard(prev, curr) < 0;
    }

    // --- HELPER METHODS ---

    /**
     * Compare highest card of two sets.
     * Returns < 0 if cards1 < cards2 (cards2 wins).
     */
    private static int compareHightestCard(List<Card> cards1, List<Card> cards2) {
        Card max1 = cards1.get(cards1.size() - 1); // Assumes sorted
        Card max2 = cards2.get(cards2.size() - 1);

        int rank1 = max1.getRankValueForTienLen();
        int rank2 = max2.getRankValueForTienLen();

        if (rank1 != rank2) {
            return Integer.compare(rank1, rank2);
        }
        return Integer.compare(max1.getSuitValueForTienLen(), max2.getSuitValueForTienLen());
    }

    private static boolean isSameRank(List<Card> cards) {
        if (cards.isEmpty())
            return false;
        int rank = cards.get(0).getRank();
        for (Card c : cards) {
            if (c.getRank() != rank)
                return false;
        }
        return true;
    }

    private static boolean isStraight(List<Card> sortedCards) {
        if (sortedCards.size() < 3)
            return false;

        // Rule: 2 cannot be in a straight (Sequence 3..A only)
        for (Card c : sortedCards) {
            if (c.getRank() == 2)
                return false;
        }

        for (int i = 0; i < sortedCards.size() - 1; i++) {
            int rank1 = sortedCards.get(i).getRankValueForTienLen();
            int rank2 = sortedCards.get(i + 1).getRankValueForTienLen();
            if (rank2 - rank1 != 1)
                return false;
        }
        return true;
    }

    private static boolean isPairSequence(List<Card> sortedCards) {
        int size = sortedCards.size();
        // Must be at least 3 pairs (6 cards) and even number
        if (size < 6 || size % 2 != 0)
            return false;

        // Check if each adjacent pair is a Pair
        for (int i = 0; i < size; i += 2) {
            Card c1 = sortedCards.get(i);
            Card c2 = sortedCards.get(i + 1);
            if (c1.getRank() != c2.getRank())
                return false;
        }

        // Check if pairs are consecutive (e.g. 33, 44, 55)
        // Rule: No 2s in Pair Sequence
        if (sortedCards.get(size - 1).getRank() == 2)
            return false;

        for (int i = 0; i < size - 2; i += 2) {
            int rank1 = sortedCards.get(i).getRankValueForTienLen();
            int rank2 = sortedCards.get(i + 2).getRankValueForTienLen(); // Next pair
            if (rank2 - rank1 != 1)
                return false;
        }

        return true;
    }

    private static boolean isPig(Card c) {
        return c.getRank() == 2;
    }

    private static boolean isPairOfPigs(List<Card> cards) {
        return cards.size() == 2 && isPig(cards.get(0)) && isPig(cards.get(1));
    }
}
