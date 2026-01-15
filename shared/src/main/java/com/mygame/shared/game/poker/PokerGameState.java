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
    private long lastRaiseAmount; // For min-raise tracking
    private Map<Integer, Long> totalContributions = new HashMap<>(); // Track total bet per player in entire hand
    private List<SidePot> sidePots = new ArrayList<>(); // Side pots for all-in scenarios

    /**
     * Represents a pot (main or side) with amount and eligible players.
     */
    public static class SidePot {
        private long amount;
        private Set<Integer> eligiblePlayers;

        public SidePot() {
            this.eligiblePlayers = new HashSet<>();
        }

        public SidePot(long amount, Set<Integer> eligiblePlayers) {
            this.amount = amount;
            this.eligiblePlayers = new HashSet<>(eligiblePlayers);
        }

        public long getAmount() {
            return amount;
        }

        public void setAmount(long amount) {
            this.amount = amount;
        }

        public Set<Integer> getEligiblePlayers() {
            return eligiblePlayers;
        }

        public void setEligiblePlayers(Set<Integer> players) {
            this.eligiblePlayers = players;
        }
    }

    /**
     * No-arg constructor for Kryo serialization.
     */
    public PokerGameState() {
        this.currentStage = Stage.PREFLOP;
        this.communityCards = new ArrayList<>();
        this.playerHoles = new HashMap<>();
        this.playerChips = new HashMap<>();
        this.playerBets = new HashMap<>();
        this.playerFolded = new HashMap<>();
    }

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
        this.lastRaiseAmount = bigBlind; // Initial min-raise is big blind

        for (Integer playerId : playerIds) {
            playerChips.put(playerId, startingChips);
            playerBets.put(playerId, 0L);
            playerFolded.put(playerId, false);
            playerHoles.put(playerId, new ArrayList<>());
        }
    }

    /**
     * Constructor with individual credits per player (from database).
     */
    public PokerGameState(Map<Integer, Long> playerCredits, int smallBlind, int bigBlind) {
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
        this.lastRaiseAmount = bigBlind;

        for (Integer playerId : playerCredits.keySet()) {
            playerChips.put(playerId, playerCredits.get(playerId));
            playerBets.put(playerId, 0L);
            playerFolded.put(playerId, false);
            playerHoles.put(playerId, new ArrayList<>());
        }
    }

    /**
     * Copy constructor for deep copy.
     */
    public PokerGameState(PokerGameState other) {
        this.currentStage = other.currentStage;
        this.communityCards = new ArrayList<>(other.communityCards);
        this.playerHoles = new HashMap<>();
        for (Map.Entry<Integer, List<Card>> entry : other.playerHoles.entrySet()) {
            this.playerHoles.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        this.playerChips = new HashMap<>(other.playerChips);
        this.playerBets = new HashMap<>(other.playerBets);
        this.playerFolded = new HashMap<>(other.playerFolded);
        this.pot = other.pot;
        this.currentBet = other.currentBet;
        this.currentPlayerTurn = other.currentPlayerTurn;
        this.dealerPosition = other.dealerPosition;
        this.smallBlind = other.smallBlind;
        this.bigBlind = other.bigBlind;
        this.lastRaiseAmount = other.lastRaiseAmount;
    }

    /**
     * Create a sanitized copy for a specific player (hides opponent holes).
     */
    public PokerGameState sanitizeFor(int targetPlayerId) {
        PokerGameState copy = new PokerGameState(this);
        // Hide cards if not Showdown
        if (copy.currentStage != Stage.SHOWDOWN) {
            for (Integer playerId : copy.playerHoles.keySet()) {
                if (playerId != targetPlayerId) {
                    copy.playerHoles.put(playerId, new ArrayList<>()); // Hide
                }
            }
        }
        return copy;
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

        // Track total contributions for side pot calculation
        long prevTotal = totalContributions.getOrDefault(playerId, 0L);
        totalContributions.put(playerId, prevTotal + actualBet);

        if (totalBet > currentBet) {
            // Track raise amount for min-raise rule
            long raiseBy = totalBet - currentBet;
            if (raiseBy > 0) {
                lastRaiseAmount = raiseBy;
            }
            currentBet = totalBet;
        }
    }

    public void fold(int playerId) {
        playerFolded.put(playerId, true);
    }

    /**
     * Reset bets for new betting round (called after FLOP/TURN/RIVER deal).
     */
    public void resetForNewRound() {
        for (Integer playerId : playerBets.keySet()) {
            playerBets.put(playerId, 0L);
        }
        currentBet = 0;
        lastRaiseAmount = bigBlind; // Reset min-raise to big blind
    }

    /**
     * Award pot to winner (legacy single pot).
     */
    public void awardPot(int winnerId) {
        long currentChips = playerChips.getOrDefault(winnerId, 0L);
        playerChips.put(winnerId, currentChips + pot);
        pot = 0;
    }

    /**
     * Award a specific amount to a player (used for side pots).
     */
    public void awardAmount(int winnerId, long amount) {
        long currentChips = playerChips.getOrDefault(winnerId, 0L);
        playerChips.put(winnerId, currentChips + amount);
    }

    /**
     * Calculate side pots based on player contributions.
     * Each pot contains an amount and eligible players who contributed at least
     * that level.
     * 
     * @param activePlayers List of players still in the hand (not folded)
     * @return List of pots from smallest contribution level to largest
     */
    public List<SidePot> calculateSidePots(List<Integer> activePlayers) {
        sidePots.clear();

        // Get contributions only for active (non-folded) players
        List<Long> contributionLevels = new ArrayList<>();
        for (int playerId : activePlayers) {
            long contribution = totalContributions.getOrDefault(playerId, 0L);
            if (contribution > 0 && !contributionLevels.contains(contribution)) {
                contributionLevels.add(contribution);
            }
        }

        // Sort contribution levels ascending
        Collections.sort(contributionLevels);

        long previousLevel = 0;
        for (long level : contributionLevels) {
            long potAmount = 0;
            Set<Integer> eligible = new HashSet<>();

            // For each contribution level, collect from all players (including folded)
            for (Integer playerId : totalContributions.keySet()) {
                long playerContribution = totalContributions.getOrDefault(playerId, 0L);
                // How much this player contributes to this pot level
                long contributionToThisPot = Math.min(playerContribution, level)
                        - Math.min(playerContribution, previousLevel);
                potAmount += contributionToThisPot;

                // Only active players are eligible to win
                if (activePlayers.contains(playerId) && playerContribution >= level) {
                    eligible.add(playerId);
                }
            }

            if (potAmount > 0 && !eligible.isEmpty()) {
                sidePots.add(new SidePot(potAmount, eligible));
            }

            previousLevel = level;
        }

        return new ArrayList<>(sidePots);
    }

    /**
     * Get total contribution for a player in this hand.
     */
    public long getTotalContribution(int playerId) {
        return totalContributions.getOrDefault(playerId, 0L);
    }

    /**
     * Get all side pots.
     */
    public List<SidePot> getSidePots() {
        return new ArrayList<>(sidePots);
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
    public Stage getCurrentStage() {
        return currentStage;
    }

    public List<Card> getCommunityCards() {
        return new ArrayList<>(communityCards);
    }

    public Map<Integer, List<Card>> getPlayerHoles() {
        return new HashMap<>(playerHoles);
    }

    public List<Card> getPlayerHole(int playerId) {
        return new ArrayList<>(playerHoles.getOrDefault(playerId, new ArrayList<>()));
    }

    public long getPot() {
        return pot;
    }

    public long getCurrentBet() {
        return currentBet;
    }

    public int getCurrentPlayerTurn() {
        return currentPlayerTurn;
    }

    public void setCurrentPlayerTurn(int playerId) {
        this.currentPlayerTurn = playerId;
    }

    public long getPlayerChips(int playerId) {
        return playerChips.getOrDefault(playerId, 0L);
    }

    public long getPlayerBet(int playerId) {
        return playerBets.getOrDefault(playerId, 0L);
    }

    public boolean isPlayerFolded(int playerId) {
        return playerFolded.getOrDefault(playerId, false);
    }

    public int getSmallBlind() {
        return smallBlind;
    }

    public int getBigBlind() {
        return bigBlind;
    }

    public long getLastRaiseAmount() {
        return lastRaiseAmount;
    }
}
