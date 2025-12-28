package com.mygame.shared.game.poker;

import com.mygame.shared.game.card.Card;
import java.util.List;

/**
 * Kết quả đánh giá poker hand
 */
public class PokerHandResult {
    private PokerHandRank rank;
    private List<Card> bestFiveCards;
    private List<Integer> kickers; // Các lá bài để so sánh khi rank bằng nhau
    
    public PokerHandResult(PokerHandRank rank, List<Card> bestFiveCards, List<Integer> kickers) {
        this.rank = rank;
        this.bestFiveCards = bestFiveCards;
        this.kickers = kickers;
    }
    
    public PokerHandRank getRank() {
        return rank;
    }
    
    public List<Card> getBestFiveCards() {
        return bestFiveCards;
    }
    
    public List<Integer> getKickers() {
        return kickers;
    }
    
    /**
     * So sánh với hand khác
     * @return >0 nếu this tốt hơn, <0 nếu other tốt hơn, 0 nếu bằng
     */
    public int compareTo(PokerHandResult other) {
        int rankCompare = Integer.compare(this.rank.getValue(), other.rank.getValue());
        if (rankCompare != 0) return rankCompare;
        
        // So sánh kickers
        int minSize = Math.min(this.kickers.size(), other.kickers.size());
        for (int i = 0; i < minSize; i++) {
            int kickerCompare = Integer.compare(this.kickers.get(i), other.kickers.get(i));
            if (kickerCompare != 0) return kickerCompare;
        }
        
        return 0;
    }
}
