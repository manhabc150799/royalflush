package com.mygame.shared.game.poker;

import com.mygame.shared.game.card.Card;
import com.mygame.shared.game.card.Suit;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class đánh giá poker hand từ 5-7 lá bài
 */
public class PokerHandEvaluator {
    
    /**
     * Đánh giá best hand từ danh sách cards (5-7 lá)
     */
    public static PokerHandResult evaluate(List<Card> cards) {
        if (cards.size() < 5) {
            throw new IllegalArgumentException("Need at least 5 cards");
        }
        
        // Tìm best 5-card combination
        List<List<Card>> combinations = generateCombinations(cards, 5);
        PokerHandResult bestResult = null;
        
        for (List<Card> combo : combinations) {
            PokerHandResult result = evaluateFiveCards(combo);
            if (bestResult == null || result.compareTo(bestResult) > 0) {
                bestResult = result;
            }
        }
        
        return bestResult;
    }
    
    /**
     * Đánh giá 5 lá bài
     */
    private static PokerHandResult evaluateFiveCards(List<Card> cards) {
        if (cards.size() != 5) {
            throw new IllegalArgumentException("Need exactly 5 cards");
        }
        
        // Sort by rank descending
        List<Card> sorted = new ArrayList<>(cards);
        sorted.sort((a, b) -> Integer.compare(b.getRank(), a.getRank()));
        
        // Count ranks and suits
        Map<Integer, Integer> rankCounts = new HashMap<>();
        Map<Suit, Integer> suitCounts = new HashMap<>();
        
        for (Card card : sorted) {
            rankCounts.put(card.getRank(), rankCounts.getOrDefault(card.getRank(), 0) + 1);
            suitCounts.put(card.getSuit(), suitCounts.getOrDefault(card.getSuit(), 0) + 1);
        }
        
        List<Integer> ranks = sorted.stream().map(Card::getRank).collect(Collectors.toList());
        boolean isFlush = suitCounts.size() == 1;
        boolean isStraight = isStraight(ranks);
        
        // Check for straight flush / royal flush
        if (isStraight && isFlush) {
            if (ranks.get(0) == 14 && ranks.get(1) == 13) { // A, K, Q, J, 10
                return new PokerHandResult(PokerHandRank.ROYAL_FLUSH, sorted, ranks);
            }
            return new PokerHandResult(PokerHandRank.STRAIGHT_FLUSH, sorted, ranks);
        }
        
        // Check for four of a kind
        if (rankCounts.containsValue(4)) {
            int fourRank = rankCounts.entrySet().stream()
                .filter(e -> e.getValue() == 4)
                .map(Map.Entry::getKey)
                .findFirst().orElse(0);
            List<Integer> kickers = new ArrayList<>(Arrays.asList(fourRank));
            kickers.addAll(rankCounts.entrySet().stream()
                .filter(e -> e.getValue() == 1)
                .map(Map.Entry::getKey)
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList()));
            return new PokerHandResult(PokerHandRank.FOUR_OF_A_KIND, sorted, kickers);
        }
        
        // Check for full house
        boolean hasThree = rankCounts.containsValue(3);
        boolean hasPair = rankCounts.containsValue(2);
        if (hasThree && hasPair) {
            int threeRank = rankCounts.entrySet().stream()
                .filter(e -> e.getValue() == 3)
                .map(Map.Entry::getKey)
                .findFirst().orElse(0);
            int pairRank = rankCounts.entrySet().stream()
                .filter(e -> e.getValue() == 2)
                .map(Map.Entry::getKey)
                .findFirst().orElse(0);
            return new PokerHandResult(PokerHandRank.FULL_HOUSE, sorted, Arrays.asList(threeRank, pairRank));
        }
        
        // Check for flush
        if (isFlush) {
            return new PokerHandResult(PokerHandRank.FLUSH, sorted, ranks);
        }
        
        // Check for straight
        if (isStraight) {
            return new PokerHandResult(PokerHandRank.STRAIGHT, sorted, ranks);
        }
        
        // Check for three of a kind
        if (hasThree) {
            int threeRank = rankCounts.entrySet().stream()
                .filter(e -> e.getValue() == 3)
                .map(Map.Entry::getKey)
                .findFirst().orElse(0);
            List<Integer> kickers = new ArrayList<>(Arrays.asList(threeRank));
            kickers.addAll(rankCounts.entrySet().stream()
                .filter(e -> e.getValue() == 1)
                .map(Map.Entry::getKey)
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList()));
            return new PokerHandResult(PokerHandRank.THREE_OF_A_KIND, sorted, kickers);
        }
        
        // Check for two pair
        List<Integer> pairRanks = rankCounts.entrySet().stream()
            .filter(e -> e.getValue() == 2)
            .map(Map.Entry::getKey)
            .sorted(Collections.reverseOrder())
            .collect(Collectors.toList());
        
        if (pairRanks.size() >= 2) {
            List<Integer> kickers = new ArrayList<>(pairRanks);
            kickers.addAll(rankCounts.entrySet().stream()
                .filter(e -> e.getValue() == 1)
                .map(Map.Entry::getKey)
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList()));
            return new PokerHandResult(PokerHandRank.TWO_PAIR, sorted, kickers);
        }
        
        // Check for one pair
        if (pairRanks.size() == 1) {
            List<Integer> kickers = new ArrayList<>(pairRanks);
            kickers.addAll(rankCounts.entrySet().stream()
                .filter(e -> e.getValue() == 1)
                .map(Map.Entry::getKey)
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList()));
            return new PokerHandResult(PokerHandRank.ONE_PAIR, sorted, kickers);
        }
        
        // High card
        return new PokerHandResult(PokerHandRank.HIGH_CARD, sorted, ranks);
    }
    
    /**
     * Kiểm tra có phải straight không (A có thể là 1 hoặc 14)
     */
    private static boolean isStraight(List<Integer> ranks) {
        Collections.sort(ranks, Collections.reverseOrder());
        
        // Check normal straight
        boolean normalStraight = true;
        for (int i = 0; i < ranks.size() - 1; i++) {
            if (ranks.get(i) - ranks.get(i + 1) != 1) {
                normalStraight = false;
                break;
            }
        }
        if (normalStraight) return true;
        
        // Check A-2-3-4-5 straight (wheel)
        if (ranks.contains(14) && ranks.contains(2) && ranks.contains(3) && 
            ranks.contains(4) && ranks.contains(5)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Generate all combinations of size k from list
     */
    private static List<List<Card>> generateCombinations(List<Card> list, int k) {
        List<List<Card>> result = new ArrayList<>();
        generateCombinationsHelper(list, k, 0, new ArrayList<>(), result);
        return result;
    }
    
    private static void generateCombinationsHelper(List<Card> list, int k, int start, 
                                                   List<Card> current, List<List<Card>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        for (int i = start; i < list.size(); i++) {
            current.add(list.get(i));
            generateCombinationsHelper(list, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}
