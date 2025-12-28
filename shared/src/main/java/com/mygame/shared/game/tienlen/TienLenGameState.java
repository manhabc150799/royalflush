package com.mygame.shared.game.tienlen;

import com.mygame.shared.game.card.Card;
import java.util.*;

/**
 * Class quản lý state của Tiến Lên game
 */
public class TienLenGameState {
    private Map<Integer, List<Card>> playerHands; // userId -> hand (13 cards)
    private List<Card> currentTrick; // Cards vừa được đánh
    private TienLenCombinationType currentTrickType;
    private int currentPlayerTurn;
    private int lastPlayedPlayer; // Player vừa đánh
    private List<Integer> playerOrder;
    private Map<Integer, Boolean> playerFinished; // Player đã hết bài
    
    public TienLenGameState(List<Integer> playerIds) {
        this.playerHands = new HashMap<>();
        this.currentTrick = new ArrayList<>();
        this.currentTrickType = null;
        this.currentPlayerTurn = 0;
        this.lastPlayedPlayer = -1;
        this.playerOrder = new ArrayList<>(playerIds);
        this.playerFinished = new HashMap<>();
        
        for (Integer playerId : playerIds) {
            playerHands.put(playerId, new ArrayList<>());
            playerFinished.put(playerId, false);
        }
    }
    
    /**
     * Deal cards cho player
     */
    public void dealHand(int playerId, List<Card> cards) {
        playerHands.put(playerId, new ArrayList<>(cards));
        // Sort hand
        sortHand(playerId);
    }
    
    /**
     * Sort hand của player (theo rank value cho Tiến Lên)
     */
    public void sortHand(int playerId) {
        List<Card> hand = playerHands.get(playerId);
        if (hand != null) {
            hand.sort((a, b) -> Integer.compare(
                a.getRankValueForTienLen(),
                b.getRankValueForTienLen()
            ));
        }
    }
    
    /**
     * Player đánh bài
     */
    public boolean playCards(int playerId, List<Card> cards) {
        List<Card> hand = playerHands.get(playerId);
        if (hand == null || !hand.containsAll(cards)) {
            return false; // Không có cards này
        }
        
        // Validate combination
        TienLenCombinationType type = CardCollection.detectCombination(cards);
        if (type == TienLenCombinationType.INVALID) {
            return false;
        }
        
        // Check có thể chặt được không
        if (currentTrickType != null && !currentTrick.isEmpty()) {
            if (!CardCollection.canBeat(currentTrickType, currentTrick, type, cards)) {
                return false; // Không chặt được
            }
        }
        
        // Remove cards from hand
        hand.removeAll(cards);
        currentTrick = new ArrayList<>(cards);
        currentTrickType = type;
        lastPlayedPlayer = playerId;
        
        // Check win
        if (hand.isEmpty()) {
            playerFinished.put(playerId, true);
        }
        
        return true;
    }
    
    /**
     * Player bỏ lượt
     */
    public void skipTurn(int playerId) {
        // Chỉ bỏ lượt được nếu có người đã đánh
        if (lastPlayedPlayer >= 0 && lastPlayedPlayer != playerId) {
            nextTurn();
        }
    }
    
    /**
     * Next turn
     */
    public void nextTurn() {
        currentPlayerTurn = (currentPlayerTurn + 1) % playerOrder.size();
        
        // Nếu đã hết một vòng và không ai chặt được, clear trick
        if (currentPlayerTurn == lastPlayedPlayer || 
            (lastPlayedPlayer >= 0 && currentPlayerTurn == (lastPlayedPlayer + 1) % playerOrder.size() && 
             currentPlayerTurn == 0)) {
            // Clear trick nếu đã hết vòng
            if (currentPlayerTurn == 0 && lastPlayedPlayer >= 0) {
                currentTrick.clear();
                currentTrickType = null;
                lastPlayedPlayer = -1;
            }
        }
    }
    
    /**
     * Check có player nào thắng chưa
     */
    public Integer getWinner() {
        for (Map.Entry<Integer, Boolean> entry : playerFinished.entrySet()) {
            if (entry.getValue()) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    // Getters
    public List<Card> getPlayerHand(int playerId) {
        return new ArrayList<>(playerHands.getOrDefault(playerId, new ArrayList<>()));
    }
    
    public List<Card> getCurrentTrick() {
        return new ArrayList<>(currentTrick);
    }
    
    public TienLenCombinationType getCurrentTrickType() {
        return currentTrickType;
    }
    
    public int getCurrentPlayerTurn() {
        return currentPlayerTurn;
    }
    
    public int getCurrentPlayerId() {
        if (currentPlayerTurn >= 0 && currentPlayerTurn < playerOrder.size()) {
            return playerOrder.get(currentPlayerTurn);
        }
        return -1;
    }
    
    public boolean isPlayerFinished(int playerId) {
        return playerFinished.getOrDefault(playerId, false);
    }
}
