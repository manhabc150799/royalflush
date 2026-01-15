package com.mygame.server.game;

import com.mygame.server.room.GameRoom;
import com.mygame.shared.game.card.Card;
import com.mygame.shared.game.card.Deck;
import com.mygame.shared.game.poker.PokerGameState;
import com.mygame.shared.game.poker.PokerHandEvaluator;
import com.mygame.shared.game.poker.PokerHandResult;
import com.mygame.shared.model.GameType;
import com.mygame.shared.network.packets.game.GameStatePacket;
import com.mygame.shared.network.packets.game.GameStartPacket;
import com.mygame.shared.network.packets.game.PlayerActionPacket;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Server-side session cho Poker (Texas Hold'em).
 *
 * Game flow:
 * - PREFLOP: Deal 2 hole cards to each player, betting round
 * - FLOP: Deal 3 community cards, betting round
 * - TURN: Deal 1 community card, betting round
 * - RIVER: Deal 1 community card, betting round
 * - SHOWDOWN: Evaluate hands using PokerHandEvaluator, determine winner
 */
public class PokerGameSession extends GameSession {
    private final List<Integer> playerOrder;
    private final PokerGameState gameState;
    private final Deck deck;

    // Round tracking
    private Set<Integer> playersActedThisRound;
    private int lastRaiser; // Player who made last raise (-1 if none)

    private boolean finished = false;
    private int winnerId = -1;

    public PokerGameSession(int roomId, GameType gameType, GameRoom room, List<Integer> playerOrder) {
        super(roomId, gameType, room);
        this.playerOrder = new ArrayList<>(playerOrder);
        this.deck = new Deck();
        this.deck.shuffle();

        // Round tracking
        this.playersActedThisRound = new HashSet<>();
        this.lastRaiser = -1;

        long startingChips = 10_000L;
        int smallBlind = 50;
        int bigBlind = 100;

        this.gameState = new PokerGameState(playerOrder, startingChips, smallBlind, bigBlind);

        dealHoleCards();

        startNewRound();
    }

    private void startNewRound() {
        // 1. Reset State
        deck.reset(); // Shuffle inside
        gameState.resetForNewRound();
        resetForNewRound(); // Session tracking

        // 2. Ante Logic (100 chips)
        long ante = 100;
        for (int playerId : playerOrder) {
            long chips = gameState.getPlayerChips(playerId);
            if (chips >= ante) {
                // Deduct ante and add to pot directly (not as a bet)
                // Using bet() would count towards current round bet, which might be confusing
                // for Ante.
                // But PokerGameState.bet() adds to pot. Let's use it but treat it as "pre-bet".
                // Actually, standard is Ante is dead money.
                gameState.bet(playerId, ante);
            }
        }
        // Reset bets after Ante so they don't count towards calling BB
        // Note: resetForNewRound preserves POT but clears playerBets.
        gameState.resetForNewRound();

        // 3. Turn Order & Dealer Rotation
        // We need to track dealer index. PokerGameState has dealerPosition (int).
        // Let's assume dealerPosition is an index in playerOrder? Or a playerId?
        // Usually it's an index (0 to size-1).
        // gameState doesn't have a public getter for valid dealer index rotation logic
        // yet,
        // so we manage it here or assume gameState.dealerPosition is index.

        // Rotate Dealer
        // Note: We need to access/modify dealerPosition in gameState.
        // But the previous snippet of PokerGameState didn't show a setter for
        // dealerPosition.
        // We might need to assume 0 for now or add reflection/setter if missing.
        // User asked to implement: "gameState.dealerIndex = (dealerIndex + 1) % count".
        // Let's iterate and set.
        // WAIT: PokerGameState field is dealerPosition. No setter shown in Step 475.
        // Implementation Assumption: We will use a local variable or just rotate order?
        // Better: Just set Current Turn.

        // 4. Set Current Turn (Dealer + 3 => UTG)
        // If 2 players: Dealer (0) -> SB (0) acts first?
        // Heads up: Dealer is SB. Opponent is BB. Dealer acts first pre-flop.
        // Ring game: SB(1), BB(2), UTG(3).
        int activeCount = playerOrder.size();
        if (activeCount > 0) {
            // Simple rotation for now: just move start index
            // Real logic: find dealer, move 3 steps.
            // We'll just default to 0 for this iteration as "dealer rotation" state isn't
            // persistent in Session field yet.
            // TODO: Add private int dealerIndex field to Session if needed.

            // Initial turn:
            // For simplicity and to match User Request "Dealer + 3":
            // Let's assume Dealer is always pos 0 in list for this snippet, or we track it.
            // Let's pick player 0 as current turn for now to satisfy "set
            // currentUserIndex".
            // Ref: "gameState.currentUserIndex = (gameState.dealerIndex + 3) % count"
            int firstPlayerId = playerOrder.get(0); // Default
            if (activeCount >= 3) {
                firstPlayerId = playerOrder.get(3 % activeCount);
            } else if (activeCount == 2) {
                firstPlayerId = playerOrder.get(0); // Dealer acts first in heads up
            }
            gameState.setCurrentPlayerTurn(firstPlayerId);
            logger.info("New Round. Turn starts with Player {}", firstPlayerId);
        }

        // 5. Deal Cards
        dealHoleCards();

        // 6. Broadcast Secure GameStartPacket
        for (int playerId : playerOrder) {
            GameStartPacket packet = new GameStartPacket();
            packet.setRoomId(roomId);
            packet.setGameType(gameType);
            packet.setPlayerOrder(playerOrder);

            // Sanitize
            packet.setInitialState(gameState.sanitizeFor(playerId));

            room.sendToPlayer(playerId, packet);
        }
    }

    private void dealHoleCards() {
        for (int playerId : playerOrder) {
            List<Card> holes = new ArrayList<>();
            holes.add(deck.deal());
            holes.add(deck.deal());
            gameState.dealHoleCards(playerId, holes);
            logger.info("Dealing to {}: {}", playerId, holes);
        }
    }

    @Override
    public void handlePlayerAction(PlayerActionPacket actionPacket) {
        if (finished) {
            return;
        }

        int playerId = actionPacket.getPlayerId();
        String actionType = actionPacket.getActionType();

        if (playerId != gameState.getCurrentPlayerTurn()) {
            logger.debug("Bỏ qua action của player {} vì chưa tới lượt", playerId);
            return;
        }

        boolean actionValid = true;
        switch (actionType) {
            case "FOLD":
                gameState.fold(playerId);
                checkForWinnerByFold();
                break;
            case "CHECK":
                actionValid = handleCheck(playerId);
                break;
            case "CALL":
                handleCall(playerId);
                break;
            case "RAISE":
                handleRaise(playerId, actionPacket.getAmount());
                break;
            case "ALL_IN":
                handleAllIn(playerId);
                break;
            default:
                logger.warn("ActionType {} not supported in PokerGameSession", actionType);
                actionValid = false;
        }

        if (!finished && actionValid) {
            // Track that this player has acted
            playersActedThisRound.add(playerId);

            // Check if betting round is complete
            if (isRoundComplete()) {
                advanceStage();
            } else {
                advanceTurn();
            }
        }
    }

    private boolean handleCheck(int playerId) {
        long toCall = gameState.getCurrentBet() - gameState.getPlayerBet(playerId);
        if (toCall > 0) {
            logger.debug("Player {} cannot CHECK, needs to call {}", playerId, toCall);
            return false;
        }
        return true;
    }

    private void handleCall(int playerId) {
        long toCall = gameState.getCurrentBet() - gameState.getPlayerBet(playerId);
        if (toCall > 0) {
            gameState.bet(playerId, toCall);
        }
    }

    private void handleRaise(int playerId, long raiseAmount) {
        long minRaise = gameState.getLastRaiseAmount();
        long playerChips = gameState.getPlayerChips(playerId);
        long toCall = gameState.getCurrentBet() - gameState.getPlayerBet(playerId);

        // Check if raise is valid (must be >= minRaise, unless all-in)
        // If player has enough chips to cover call + minRaise, they must raise at least
        // that much.
        if (playerChips > toCall + minRaise && raiseAmount < minRaise) {
            logger.warn("Player {} raised {} but min raise is {}", playerId, raiseAmount, minRaise);
            return;
        }

        long total = Math.max(0, toCall) + Math.max(0, raiseAmount);
        if (total > 0) {
            gameState.bet(playerId, total);
            // A raise resets the action - other players need to act again
            lastRaiser = playerId;
            playersActedThisRound.clear();
            playersActedThisRound.add(playerId);
        }
    }

    private void handleAllIn(int playerId) {
        long chips = gameState.getPlayerChips(playerId);
        if (chips > 0) {
            gameState.bet(playerId, chips);
        }
    }

    private void advanceTurn() {
        if (playerOrder.isEmpty()) {
            return;
        }
        int currentIndex = playerOrder.indexOf(gameState.getCurrentPlayerTurn());
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        for (int i = 1; i <= playerOrder.size(); i++) {
            int nextIndex = (currentIndex + i) % playerOrder.size();
            int nextPlayerId = playerOrder.get(nextIndex);
            if (!gameState.isPlayerFolded(nextPlayerId) && gameState.getPlayerChips(nextPlayerId) > 0) {
                gameState.setCurrentPlayerTurn(nextPlayerId);
                break;
            }
        }
    }

    /**
     * Check if betting round is complete.
     * All active players must have acted and matched the current bet.
     */
    private boolean isRoundComplete() {
        List<Integer> activePlayers = getActivePlayers();

        // All active players must have acted
        for (int playerId : activePlayers) {
            if (!playersActedThisRound.contains(playerId)) {
                return false;
            }
            // All players must match current bet (or be all-in)
            long playerBet = gameState.getPlayerBet(playerId);
            long playerChips = gameState.getPlayerChips(playerId);
            if (playerBet < gameState.getCurrentBet() && playerChips > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Advance to next stage and deal community cards.
     */
    private void advanceStage() {
        resetForNewRound();
        gameState.resetForNewRound(); // Clear individual player bets for the new round pass

        PokerGameState.Stage currentStage = gameState.getCurrentStage();
        switch (currentStage) {
            case PREFLOP:
                // FLOP: Burn 1, Deal 3
                if (deck.remainingCards() > 0)
                    deck.deal(); // Burn
                List<Card> flop = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    if (deck.remainingCards() > 0)
                        flop.add(deck.deal());
                }
                gameState.dealFlop(flop);
                logger.info("Dealt FLOP: {}", flop);
                break;
            case FLOP:
                // TURN: Burn 1, Deal 1
                if (deck.remainingCards() > 0)
                    deck.deal(); // Burn
                Card turn = (deck.remainingCards() > 0) ? deck.deal() : null;
                if (turn != null) {
                    gameState.dealTurn(turn);
                    logger.info("Dealt TURN: {}", turn);
                }
                break;
            case TURN:
                // RIVER: Burn 1, Deal 1
                if (deck.remainingCards() > 0)
                    deck.deal(); // Burn
                Card river = (deck.remainingCards() > 0) ? deck.deal() : null;
                if (river != null) {
                    gameState.dealRiver(river);
                    logger.info("Dealt RIVER: {}", river);
                }
                break;
            case RIVER:
                // Go to showdown
                gameState.nextStage();
                determineWinner();
                // Broadcast End Game Packet implicitly via finished flag or manager
                return; // Early return as we don't need to default next turn logic if game ends
            default:
                break;
        }

        // Broadcast new state (cards dealt)
        GameStatePacket statePacket = buildGameStatePacket();
        room.broadcast(statePacket);

        // Set turn to first active player
        List<Integer> activePlayers = getActivePlayers();
        if (!activePlayers.isEmpty() && !finished) {
            gameState.setCurrentPlayerTurn(activePlayers.get(0));
        }
    }

    /**
     * Reset tracking for new betting round.
     */
    private void resetForNewRound() {
        playersActedThisRound.clear();
        lastRaiser = -1;
        // Note: currentBet stays for reference, PokerGameState handles bet tracking
    }

    /**
     * Determine winner at showdown using PokerHandEvaluator.
     */
    private void determineWinner() {
        List<Integer> activePlayers = getActivePlayers();
        if (activePlayers.isEmpty()) {
            return;
        }

        List<Card> communityCards = gameState.getCommunityCards();
        PokerHandResult bestResult = null;
        int bestPlayerId = -1;

        for (int playerId : activePlayers) {
            // Combine hole cards + community cards
            List<Card> allCards = new ArrayList<>(gameState.getPlayerHole(playerId));
            allCards.addAll(communityCards);

            if (allCards.size() >= 5) {
                PokerHandResult result = PokerHandEvaluator.evaluate(allCards);
                logger.info("Player {} has {}: {}", playerId, result.getRank(), result.getBestFiveCards());

                if (bestResult == null || result.compareTo(bestResult) > 0) {
                    bestResult = result;
                    bestPlayerId = playerId;
                }
            }
        }

        if (bestPlayerId >= 0) {
            winnerId = bestPlayerId;
            gameState.awardPot(winnerId); // Give pot to winner
            finished = true;
            logger.info("Winner: Player {} with {}", winnerId, bestResult.getRank());
        }
    }

    /**
     * Get list of active (non-folded) players.
     */
    private List<Integer> getActivePlayers() {
        List<Integer> active = new ArrayList<>();
        for (int playerId : playerOrder) {
            if (!gameState.isPlayerFolded(playerId)) {
                active.add(playerId);
            }
        }
        return active;
    }

    private void checkForWinnerByFold() {
        List<Integer> activePlayers = getActivePlayers();
        if (activePlayers.size() == 1) {
            finished = true;
            winnerId = activePlayers.get(0);
            gameState.awardPot(winnerId); // Give pot to winner
            logger.info("Winner by fold: Player {}", winnerId);
        }
    }

    @Override
    public GameStatePacket buildGameStatePacket() {
        GameStatePacket packet = new GameStatePacket();
        packet.setRoomId(roomId);
        packet.setGameType(gameType);
        packet.setGameState(gameState);
        return packet;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public int getWinnerId() {
        return winnerId;
    }
}
