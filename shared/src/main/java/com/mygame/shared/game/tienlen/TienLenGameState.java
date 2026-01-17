package com.mygame.shared.game.tienlen;

import com.mygame.shared.game.card.Card;
import java.util.*;

/**
 * Class quản lý state của Tiến Lên game.
 * Tracks hands, current trick, turn, skipped players, and finish order.
 */
public class TienLenGameState {
    private Map<Integer, List<Card>> playerHands; // userId -> hand (13 cards)
    private List<Card> currentTrick; // Cards currently on board
    private TienLenCombinationType currentTrickType;
    private int currentPlayerTurn; // Index in playerOrder
    private int lastPlayedPlayer; // PlayerId who played the last valid set
    private List<Integer> playerOrder; // List of playerIds in turn order
    private Map<Integer, Boolean> playerFinished; // userId -> finished?

    private Set<Integer> skippedPlayers; // Players who passed in current round
    private List<Integer> winners; // Order of players who finished (1st, 2nd, 3rd...)
    private Map<Integer, Long> playerCredits; // userId -> credits

    // No-arg constructor for Kryo
    public TienLenGameState() {
        this.playerHands = new HashMap<>();
        this.currentTrick = new ArrayList<>();
        this.playerOrder = new ArrayList<>();
        this.playerFinished = new HashMap<>();
        this.skippedPlayers = new HashSet<>();
        this.winners = new ArrayList<>();
        this.playerCredits = new HashMap<>();
    }

    /**
     * Reset the game state for a new game with the same players.
     */
    public void reset(List<Integer> playerIds) {
        this.playerHands.clear();
        this.currentTrick.clear();
        this.currentTrickType = null;
        this.currentPlayerTurn = 0;
        this.lastPlayedPlayer = -1;
        this.playerOrder = new ArrayList<>(playerIds);
        this.playerFinished.clear();
        this.skippedPlayers.clear();
        this.winners.clear();
        // Keep playerCredits as they are (persist across games)

        for (Integer playerId : playerIds) {
            playerHands.put(playerId, new ArrayList<>());
            playerFinished.put(playerId, false);
        }
    }

    public TienLenGameState(List<Integer> playerIds) {
        this.playerHands = new HashMap<>();
        this.currentTrick = new ArrayList<>();
        this.currentTrickType = null;
        this.currentPlayerTurn = 0;
        this.lastPlayedPlayer = -1;
        this.playerOrder = new ArrayList<>(playerIds);
        this.playerFinished = new HashMap<>();
        this.skippedPlayers = new HashSet<>();
        this.winners = new ArrayList<>();
        this.playerCredits = new HashMap<>();

        for (Integer playerId : playerIds) {
            playerHands.put(playerId, new ArrayList<>());
            playerFinished.put(playerId, false);
            playerCredits.put(playerId, 0L);
        }
    }

    public void setPlayerCredits(int playerId, long credits) {
        playerCredits.put(playerId, credits);
    }

    public long getPlayerCredits(int playerId) {
        return playerCredits.getOrDefault(playerId, 0L);
    }

    /**
     * Deal cards to a player.
     */
    public void dealHand(int playerId, List<Card> cards) {
        playerHands.put(playerId, new ArrayList<>(cards));
        CardCollection.sortHandTienLen(playerHands.get(playerId));
    }

    /**
     * Start a new round (clear board and skipped status).
     * Usually called when everyone skips or a Chop clears the round.
     */
    public void startNewRound() {
        currentTrick.clear();
        currentTrickType = null;
        skippedPlayers.clear();
        // lastPlayedPlayer remains keeping the winner of previous round context until
        // they play
    }

    /**
     * Attempt to play cards. Validates only if cards exist in hand.
     * Logic for 'Can Beat' is in CardCollection, checked by Session.
     * This method assumes move is Validated by Session.
     */
    public void playCards(int playerId, List<Card> cards, TienLenCombinationType type) {
        List<Card> hand = playerHands.get(playerId);
        if (hand == null)
            return;

        hand.removeAll(cards);
        currentTrick.clear();
        currentTrick.addAll(cards);
        currentTrickType = type;
        lastPlayedPlayer = playerId;

        // If hand empty, mark finished
        if (hand.isEmpty()) {
            playerFinished.put(playerId, true);
            if (!winners.contains(playerId)) {
                winners.add(playerId);
            }
        }
    }

    /**
     * Player passes turn.
     */
    public void passTurn(int playerId) {
        skippedPlayers.add(playerId);
    }

    /**
     * Move to next valid player.
     */
    public void nextTurn() {
        if (playerOrder.isEmpty())
            return;

        int startTurn = currentPlayerTurn;
        int size = playerOrder.size();

        do {
            currentPlayerTurn = (currentPlayerTurn + 1) % size;
            int pid = playerOrder.get(currentPlayerTurn);

            // Player valid if: NOT finished AND NOT skipped
            // (Exception: 4-pair chop logic handles 'skipped' override in Session, not
            // here)
            if (!playerFinished.getOrDefault(pid, false) && !skippedPlayers.contains(pid)) {
                return; // Found next player
            }

            // Emergency break if loop full circle (should trigger round end before this)
            if (currentPlayerTurn == startTurn) {
                return;
            }
        } while (currentPlayerTurn != startTurn);
    }

    // --- Getters & Setters ---

    public List<Card> getPlayerHand(int playerId) {
        return playerHands.get(playerId);
    }

    public List<Card> getCurrentTrick() {
        return currentTrick;
    }

    public TienLenCombinationType getCurrentTrickType() {
        return currentTrickType;
    }

    public int getCurrentPlayerId() {
        if (playerOrder == null || playerOrder.isEmpty())
            return -1;
        return playerOrder.get(currentPlayerTurn);
    }

    public int getLastPlayedPlayer() {
        return lastPlayedPlayer;
    }

    public void setLastPlayedPlayer(int id) {
        this.lastPlayedPlayer = id;
    }

    public boolean isSkipped(int playerId) {
        return skippedPlayers.contains(playerId);
    }

    public Set<Integer> getSkippedPlayers() {
        return skippedPlayers;
    }

    public boolean isPlayerFinished(int playerId) {
        return playerFinished.getOrDefault(playerId, false);
    }

    public List<Integer> getWinners() {
        return winners;
    }

    public List<Integer> getPlayerOrder() {
        return playerOrder;
    }

    public void setCurrentPlayerTurn(int index) {
        this.currentPlayerTurn = index;
    }

    public int getCurrentPlayerTurn() {
        return currentPlayerTurn;
    }
}
