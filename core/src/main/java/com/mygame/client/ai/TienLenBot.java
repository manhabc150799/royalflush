package com.mygame.client.ai;

import com.mygame.shared.game.card.Card;
import com.mygame.shared.game.tienlen.CardCollection;
import com.mygame.shared.game.tienlen.TienLenCombinationType;
import com.mygame.shared.game.tienlen.TienLenGameState;

import java.util.*;

/**
 * AI Bot cho Tiến Lên với strategy: đánh lá nhỏ nhất có thể chặt được
 */
public class TienLenBot {
    private final int botId;
    private final TienLenBotStrategy strategy = new TienLenBotStrategy();
    
    public TienLenBot(int botId) {
        this.botId = botId;
    }
    
    /**
     * Quyết định cards để đánh
     */
    public List<Card> decidePlay(TienLenGameState gameState) {
        List<Card> hand = gameState.getPlayerHand(botId);
        if (hand.isEmpty()) {
            return null; // Hết bài
        }
        
        List<Card> currentTrick = gameState.getCurrentTrick();
        TienLenCombinationType currentTrickType = gameState.getCurrentTrickType();
        
        // Nếu không có bài trên bàn, đánh lá nhỏ nhất
        if (currentTrick.isEmpty() || currentTrickType == null) {
            return Arrays.asList(hand.get(0)); // Đánh lá nhỏ nhất
        }
        
        // Tìm combination nhỏ nhất có thể chặt được
        List<Card> bestPlay = strategy.findSmallestBeatingCombination(hand, currentTrickType, currentTrick);
        
        if (bestPlay != null) {
            return bestPlay;
        }
        
        // Không chặt được -> bỏ lượt
        return null;
    }
    
}

