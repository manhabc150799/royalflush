package com.mygame.shared.game.poker;

import com.mygame.shared.game.card.Card;
import java.util.*;

/**
 * Class quản lý state của poker game
 */
public class PokerGameState {
    public enum Stage {
        PREFLOP,
        FLOP,
        TURN,
        RIVER,
        SHOWDOWN,
        FINISHED
    }
    
    private Stage currentStage;
    private List<Card> communityCards;
    private Map<Integer, List<Card>> playerHoles; // userId -> hole cards
    private Map<Integer, Long> playerChips; // userId -> chips
    private Map<Integer, Long> playerBets; // userId -> current bet amount
    private Map<Integer, Boolean> playerFolded; // userId -> folded
    private long pot;
    private long currentBet;
    private int currentPlayerTurn;
    private int dealerPosition;
    private int smallBlind;
    private int bigBlind;
    
    public PokerGameState(List<Integer> playerIds, long startingChips, int smallBlind, int bigBlind) {
        this.currentStage = Stage.PREFLOP;
        this.communityCards = new ArrayList<>();
        this.playerHoles = new HashMap<>();
        this.playerChips = new HashMap<>();
        this.playerBets = new HashMap<>();
        this.playerFolded = new HashMap<>();
        this.pot = 0;
        this.currentBet = 0;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.dealerPosition = 0;
        
        for (Integer playerId : playerIds) {
            playerChips.put(playerId, startingChips);
            playerBets.put(playerId, 0L);
            playerFolded.put(playerId, false);
            playerHoles.put(playerId, new ArrayList<>());
        }
    }
    
    public void dealHoleCards(int playerId, List<Card> cards) {
        playerHoles.put(playerId, new ArrayList<>(cards));
    }
    
    public void addCommunityCard(Card card) {
        communityCards.add(card);
    }
    
    public void dealFlop(List<Card> cards) {
        communityCards.addAll(cards);
        currentStage = Stage.FLOP;
    }
    
    public void dealTurn(Card card) {
        communityCards.add(card);
        currentStage = Stage.TURN;
    }
    
    public void dealRiver(Card card) {
        communityCards.add(card);
        currentStage = Stage.RIVER;
    }
    
    public void bet(int playerId, long amount) {
        long currentPlayerBet = playerBets.getOrDefault(playerId, 0L);
        long totalBet = currentPlayerBet + amount;
        long chips = playerChips.getOrDefault(playerId, 0L);
        
        if (totalBet > chips) {
            totalBet = chips; // All-in
        }
        
        long actualBet = totalBet - currentPlayerBet;
        playerChips.put(playerId, chips - actualBet);
        playerBets.put(playerId, totalBet);
        pot += actualBet;
        
        if (totalBet > currentBet) {
            currentBet = totalBet;
        }
    }
    
    public void fold(int playerId) {
        playerFolded.put(playerId, true);
    }
    
    public void nextStage() {
        switch (currentStage) {
            case PREFLOP:
                currentStage = Stage.FLOP;
                break;
            case FLOP:
                currentStage = Stage.TURN;
                break;
            case TURN:
                currentStage = Stage.RIVER;
                break;
            case RIVER:
                currentStage = Stage.SHOWDOWN;
                break;
        }
    }
    
    // Getters
    public Stage getCurrentStage() { return currentStage; }
    public List<Card> getCommunityCards() { return new ArrayList<>(communityCards); }
    public Map<Integer, List<Card>> getPlayerHoles() { return new HashMap<>(playerHoles); }
    public List<Card> getPlayerHole(int playerId) { return new ArrayList<>(playerHoles.getOrDefault(playerId, new ArrayList<>())); }
    public long getPot() { return pot; }
    public long getCurrentBet() { return currentBet; }
    public int getCurrentPlayerTurn() { return currentPlayerTurn; }
    public void setCurrentPlayerTurn(int playerId) { this.currentPlayerTurn = playerId; }
    public long getPlayerChips(int playerId) { return playerChips.getOrDefault(playerId, 0L); }
    public long getPlayerBet(int playerId) { return playerBets.getOrDefault(playerId, 0L); }
    public boolean isPlayerFolded(int playerId) { return playerFolded.getOrDefault(playerId, false); }
    public int getSmallBlind() { return smallBlind; }
    public int getBigBlind() { return bigBlind; }
}
