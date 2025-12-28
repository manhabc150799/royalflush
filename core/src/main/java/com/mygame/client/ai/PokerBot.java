package com.mygame.client.ai;

import com.mygame.shared.game.card.Card;
import com.mygame.shared.game.poker.PokerHandEvaluator;
import com.mygame.shared.game.poker.PokerHandRank;
import com.mygame.shared.game.poker.PokerGameState;

import java.util.List;
import java.util.Random;

/**
 * AI Bot cho Poker với strategy đơn giản
 */
public class PokerBot {
    private static final Random random = new Random();
    private final int botId;
    
    public PokerBot(int botId) {
        this.botId = botId;
    }
    
    /**
     * Quyết định action của bot
     */
    public BotAction decideAction(PokerGameState gameState, List<Card> holeCards) {
        // Tính xác suất thắng dựa trên hole cards và community cards
        double winProbability = calculateWinProbability(holeCards, gameState.getCommunityCards());
        
        long chips = gameState.getPlayerChips(botId);
        long currentBet = gameState.getCurrentBet();
        long playerBet = gameState.getPlayerBet(botId);
        long toCall = currentBet - playerBet;
        
        // Strategy dựa trên xác suất
        if (winProbability < 0.3) {
            // Xác suất thấp -> Fold
            return BotAction.FOLD;
        } else if (winProbability < 0.6) {
            // Xác suất trung bình -> Check/Call
            if (toCall == 0) {
                return BotAction.CHECK;
            } else if (toCall <= chips * 0.1) {
                return BotAction.CALL;
            } else {
                return BotAction.FOLD;
            }
        } else if (winProbability < 0.8) {
            // Xác suất tốt -> Raise nhỏ
            if (toCall == 0) {
                return BotAction.RAISE;
            } else {
                return BotAction.CALL;
            }
        } else {
            // Xác suất rất tốt -> Raise lớn hoặc All-in
            if (chips < currentBet * 2) {
                return BotAction.ALL_IN;
            } else {
                return BotAction.RAISE;
            }
        }
    }
    
    /**
     * Tính xác suất thắng (đơn giản)
     */
    private double calculateWinProbability(List<Card> holeCards, List<Card> communityCards) {
        if (holeCards.size() < 2) return 0.0;
        
        // Đánh giá hand hiện tại
        List<Card> allCards = new java.util.ArrayList<>(holeCards);
        allCards.addAll(communityCards);
        
        if (allCards.size() >= 5) {
            com.mygame.shared.game.poker.PokerHandResult result = PokerHandEvaluator.evaluate(allCards);
            PokerHandRank rank = result.getRank();
            
            // Map rank to probability
            switch (rank) {
                case ROYAL_FLUSH:
                case STRAIGHT_FLUSH:
                    return 0.95;
                case FOUR_OF_A_KIND:
                    return 0.85;
                case FULL_HOUSE:
                    return 0.75;
                case FLUSH:
                    return 0.65;
                case STRAIGHT:
                    return 0.55;
                case THREE_OF_A_KIND:
                    return 0.45;
                case TWO_PAIR:
                    return 0.35;
                case ONE_PAIR:
                    return 0.25;
                default:
                    return 0.15;
            }
        } else {
            // Pre-flop: đánh giá dựa trên hole cards
            return evaluateHoleCards(holeCards);
        }
    }
    
    /**
     * Đánh giá hole cards (pre-flop)
     */
    private double evaluateHoleCards(List<Card> holeCards) {
        if (holeCards.size() < 2) return 0.0;
        
        Card card1 = holeCards.get(0);
        Card card2 = holeCards.get(1);
        
        // Pair
        if (card1.getRank() == card2.getRank()) {
            if (card1.getRank() >= 10) return 0.5; // High pair
            return 0.3; // Low pair
        }
        
        // High cards
        boolean highCard1 = card1.getRank() >= 10;
        boolean highCard2 = card2.getRank() >= 10;
        if (highCard1 && highCard2) {
            // Suited
            if (card1.getSuit() == card2.getSuit()) {
                return 0.4;
            }
            return 0.3;
        }
        
        // Suited connectors
        if (card1.getSuit() == card2.getSuit()) {
            int rankDiff = Math.abs(card1.getRank() - card2.getRank());
            if (rankDiff <= 2) {
                return 0.25;
            }
        }
        
        return 0.15; // Weak hand
    }
    
    /**
     * Tính bet amount cho raise
     */
    public long calculateRaiseAmount(PokerGameState gameState) {
        long chips = gameState.getPlayerChips(botId);
        long currentBet = gameState.getCurrentBet();
        
        // Raise 20-50% của chips còn lại
        long raiseAmount = (long) (chips * (0.2 + random.nextDouble() * 0.3));
        return Math.min(raiseAmount, chips);
    }
    
    /**
     * Enum cho bot actions
     */
    public enum BotAction {
        FOLD,
        CHECK,
        CALL,
        RAISE,
        ALL_IN
    }
}

