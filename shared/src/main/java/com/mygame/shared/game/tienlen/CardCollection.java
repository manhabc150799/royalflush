package com.mygame.shared.game.tienlen;

import com.mygame.shared.game.card.Card;
import java.util.*;

/**
 * Class quản lý và validate combinations trong Tiến Lên
 */
public class CardCollection {
    
    /**
     * Detect combination type từ danh sách cards
     */
    public static TienLenCombinationType detectCombination(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            return TienLenCombinationType.INVALID;
        }
        
        int size = cards.size();
        
        if (size == 1) {
            return TienLenCombinationType.SINGLE;
        }
        
        if (size == 2) {
            if (isSameRank(cards)) {
                return TienLenCombinationType.PAIR;
            }
            return TienLenCombinationType.INVALID;
        }
        
        if (size == 3) {
            if (isSameRank(cards)) {
                return TienLenCombinationType.TRIPLE;
            }
            if (isStraight(cards)) {
                return TienLenCombinationType.STRAIGHT;
            }
            return TienLenCombinationType.INVALID;
        }
        
        if (size == 4) {
            if (isSameRank(cards)) {
                return TienLenCombinationType.FOUR_OF_A_KIND; // Bomb
            }
            if (isStraight(cards)) {
                return TienLenCombinationType.STRAIGHT;
            }
            return TienLenCombinationType.INVALID;
        }
        
        // >= 5 cards
        if (isStraight(cards)) {
            return TienLenCombinationType.STRAIGHT;
        }
        
        if (isPairSequence(cards)) {
            return TienLenCombinationType.PAIR_SEQUENCE;
        }
        
        if (isTripleSequence(cards)) {
            return TienLenCombinationType.TRIPLE_SEQUENCE;
        }
        
        return TienLenCombinationType.INVALID;
    }
    
    /**
     * Kiểm tra có thể chặt được combination trước đó không
     */
    public static boolean canBeat(TienLenCombinationType prevType, List<Card> prevCards,
                                  TienLenCombinationType currentType, List<Card> currentCards) {
        if (prevCards == null || prevCards.isEmpty() || currentCards == null || currentCards.isEmpty()) {
            return false;
        }
        
        // Bomb (FOUR_OF_A_KIND) chặt được mọi thứ trừ bomb cao hơn
        if (currentType == TienLenCombinationType.FOUR_OF_A_KIND) {
            if (prevType == TienLenCombinationType.FOUR_OF_A_KIND) {
                return compareBombs(prevCards, currentCards) < 0;
            }
            return true; // Bomb chặt mọi thứ khác
        }
        
        // Đôi thông chặt heo:
        // - 3 đôi thông (6 lá) chặt được 1 heo
        // - 4 đôi thông (8 lá) chặt được đôi heo
        if (currentType == TienLenCombinationType.PAIR_SEQUENCE) {
            int size = currentCards.size();
            boolean isValidPairSequence = detectCombination(currentCards) == TienLenCombinationType.PAIR_SEQUENCE;
            if (isValidPairSequence) {
                if (prevType == TienLenCombinationType.SINGLE && isSingleTwo(prevCards) && size >= 6) {
                    return true;
                }
                if (prevType == TienLenCombinationType.PAIR && isPairOfTwos(prevCards) && size >= 8) {
                    return true;
                }
            }
        }
        
        // Chỉ cùng loại mới chặt được
        if (prevType != currentType) {
            return false;
        }
        
        // Cùng size
        if (prevCards.size() != currentCards.size()) {
            return false;
        }
        
        // So sánh theo loại
        switch (currentType) {
            case SINGLE:
            case STRAIGHT:
                return compareHighestCard(prevCards, currentCards) < 0;
            
            case PAIR:
            case TRIPLE:
                return compareSameRank(prevCards, currentCards) < 0;
            
            case PAIR_SEQUENCE:
            case TRIPLE_SEQUENCE:
                return compareSequence(prevCards, currentCards) < 0;
            
            default:
                return false;
        }
    }
    
    /**
     * Kiểm tra cùng rank
     */
    private static boolean isSameRank(List<Card> cards) {
        if (cards.isEmpty()) return false;
        int rank = cards.get(0).getRank();
        return cards.stream().allMatch(c -> c.getRank() == rank);
    }
    
    /**
     * Kiểm tra có phải straight không (sảnh)
     */
    private static boolean isStraight(List<Card> cards) {
        if (cards.size() < 3) return false;
        
        List<Integer> tienLenRanks = new ArrayList<>();
        for (Card card : cards) {
            tienLenRanks.add(card.getRankValueForTienLen());
        }
        Collections.sort(tienLenRanks);
        
        for (int i = 0; i < tienLenRanks.size() - 1; i++) {
            if (tienLenRanks.get(i + 1) - tienLenRanks.get(i) != 1) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Kiểm tra có phải đôi thông không
     */
    private static boolean isPairSequence(List<Card> cards) {
        if (cards.size() < 6 || cards.size() % 2 != 0) return false;
        
        // Group thành pairs
        Map<Integer, Integer> rankCounts = new HashMap<>();
        for (Card card : cards) {
            rankCounts.put(card.getRank(), rankCounts.getOrDefault(card.getRank(), 0) + 1);
        }
        
        // Mỗi rank phải có đúng 2 lá
        for (int count : rankCounts.values()) {
            if (count != 2) return false;
        }
        
        // Các ranks phải tạo thành straight
        List<Integer> ranks = new ArrayList<>(rankCounts.keySet());
        ranks.sort((a, b) -> Integer.compare(
            new Card(a, cards.get(0).getSuit()).getRankValueForTienLen(),
            new Card(b, cards.get(0).getSuit()).getRankValueForTienLen()
        ));
        
        return isStraightFromRanks(ranks, cards.get(0).getSuit());
    }
    
    /**
     * Kiểm tra có phải tam thông không
     */
    private static boolean isTripleSequence(List<Card> cards) {
        if (cards.size() < 6 || cards.size() % 3 != 0) return false;
        
        Map<Integer, Integer> rankCounts = new HashMap<>();
        for (Card card : cards) {
            rankCounts.put(card.getRank(), rankCounts.getOrDefault(card.getRank(), 0) + 1);
        }
        
        for (int count : rankCounts.values()) {
            if (count != 3) return false;
        }
        
        List<Integer> ranks = new ArrayList<>(rankCounts.keySet());
        ranks.sort((a, b) -> Integer.compare(
            new Card(a, cards.get(0).getSuit()).getRankValueForTienLen(),
            new Card(b, cards.get(0).getSuit()).getRankValueForTienLen()
        ));
        
        return isStraightFromRanks(ranks, cards.get(0).getSuit());
    }
    
    private static boolean isStraightFromRanks(List<Integer> ranks, com.mygame.shared.game.card.Suit suit) {
        if (ranks.size() < 2) return false;
        
        for (int i = 0; i < ranks.size() - 1; i++) {
            int rank1 = new Card(ranks.get(i), suit).getRankValueForTienLen();
            int rank2 = new Card(ranks.get(i + 1), suit).getRankValueForTienLen();
            if (rank2 - rank1 != 1) return false;
        }
        return true;
    }
    
    /**
     * So sánh highest card
     */
    private static int compareHighestCard(List<Card> cards1, List<Card> cards2) {
        int max1 = cards1.stream().mapToInt(c -> c.getRankValueForTienLen()).max().orElse(0);
        int max2 = cards2.stream().mapToInt(c -> c.getRankValueForTienLen()).max().orElse(0);
        return Integer.compare(max1, max2);
    }
    
    /**
     * So sánh same rank combinations
     */
    private static int compareSameRank(List<Card> cards1, List<Card> cards2) {
        int rank1 = cards1.get(0).getRankValueForTienLen();
        int rank2 = cards2.get(0).getRankValueForTienLen();
        return Integer.compare(rank1, rank2);
    }
    
    /**
     * So sánh sequences
     */
    private static int compareSequence(List<Card> cards1, List<Card> cards2) {
        return compareHighestCard(cards1, cards2);
    }
    
    /**
     * So sánh bombs
     */
    private static int compareBombs(List<Card> cards1, List<Card> cards2) {
        return compareSameRank(cards1, cards2);
    }
    
    /**
     * Helper: có phải 1 lá heo không
     */
    private static boolean isSingleTwo(List<Card> cards) {
        if (cards.size() != 1) return false;
        Card c = cards.get(0);
        // Sử dụng rank value theo Tiến Lên để tránh phụ thuộc mapping rank nội bộ
        int twoValue = new Card(2, com.mygame.shared.game.card.Suit.CLUBS).getRankValueForTienLen();
        return c.getRankValueForTienLen() == twoValue;
    }
    
    /**
     * Helper: có phải đôi heo không
     */
    private static boolean isPairOfTwos(List<Card> cards) {
        if (cards.size() != 2) return false;
        int twoValue = new Card(2, com.mygame.shared.game.card.Suit.CLUBS).getRankValueForTienLen();
        return cards.get(0).getRankValueForTienLen() == twoValue
            && cards.get(1).getRankValueForTienLen() == twoValue;
    }
}
