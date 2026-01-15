package com.mygame.server.game;

import com.mygame.server.database.DatabaseManager;
import com.mygame.server.database.UserDAO;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side session for Poker (Texas Hold'em).
 * 
 * Blinds:
 * - Small Blind (SB): 500
 * - Big Blind (BB): 1000
 * 
 * Position Assignment:
 * - Host (first player / room creator): Big Blind
 * - Player 2: Small Blind
 * - Player 3+: UTG (first to act pre-flop)
 * 
 * Game Flow:
 * - PREFLOP: Post blinds, deal 2 hole cards, UTG acts first
 * - FLOP: Deal 3 community cards, SB acts first
 * - TURN: Deal 1 community card, SB acts first
 * - RIVER: Deal 1 community card, SB acts first
 * - SHOWDOWN: Evaluate hands, award pot
 */
public class PokerGameSession extends GameSession {

    private static final Logger logger = LoggerFactory.getLogger(PokerGameSession.class);

    // Fixed blinds
    private static final long SMALL_BLIND = 500L;
    private static final long BIG_BLIND = 1000L;

    private final List<Integer> playerOrder; // Join order (index 0 = host)
    private final PokerGameState gameState;
    private final Deck deck;

    // Position indices in playerOrder
    private int bbIndex; // Big Blind position (host = 0)
    private int sbIndex; // Small Blind position

    // Round tracking
    private Set<Integer> playersActedThisRound;
    private int lastRaiser = -1;

    private boolean finished = false;
    private int winnerId = -1;
    private Map<Integer, Long> initialChips = new HashMap<>(); // Track starting chips for credit calculation

    public PokerGameSession(int roomId, GameType gameType, GameRoom room, List<Integer> playerOrder) {
        super(roomId, gameType, room);
        this.playerOrder = new ArrayList<>(playerOrder);
        this.deck = new Deck();
        this.playersActedThisRound = new HashSet<>();

        // Position assignment based on join order
        // Host (index 0) = BB, Player 2 (index 1) = SB
        int playerCount = playerOrder.size();
        this.bbIndex = 0; // Host is always BB
        this.sbIndex = (playerCount >= 2) ? 1 : 0; // Player 2 is SB (or host if only 1 player)

        // Load credits from database for each player
        Map<Integer, Long> playerCredits = new HashMap<>();
        try {
            UserDAO userDAO = new UserDAO(DatabaseManager.getInstance());
            for (Integer playerId : playerOrder) {
                UserDAO.UserProfile profile = userDAO.getUserProfile(playerId);
                if (profile != null) {
                    playerCredits.put(playerId, profile.getCredits());
                    logger.info("Loaded credits for player {}: {}", playerId, profile.getCredits());
                } else {
                    playerCredits.put(playerId, 10_000L); // Fallback
                    logger.warn("Could not load profile for player {}, using fallback 10000", playerId);
                }
            }
        } catch (Exception e) {
            logger.error("Error loading player credits from database, using fallback", e);
            for (Integer playerId : playerOrder) {
                playerCredits.put(playerId, 10_000L);
            }
        }

        // Store initial chips for credit change calculation
        this.initialChips = new HashMap<>(playerCredits);

        // Initialize game state with actual credits from database
        this.gameState = new PokerGameState(playerCredits, (int) SMALL_BLIND, (int) BIG_BLIND);

        startNewHand();
    }

    /**
     * Start a new hand: post blinds, deal cards, set first actor.
     */
    private void startNewHand() {
        // 1. Reset deck and state
        deck.reset();
        gameState.resetForNewRound();
        playersActedThisRound.clear();
        lastRaiser = -1;

        int playerCount = playerOrder.size();
        if (playerCount < 2) {
            logger.warn("Not enough players to start poker hand");
            return;
        }

        // 2. Post Blinds
        int sbPlayerId = playerOrder.get(sbIndex);
        int bbPlayerId = playerOrder.get(bbIndex);

        // Small Blind
        long sbAmount = Math.min(SMALL_BLIND, gameState.getPlayerChips(sbPlayerId));
        if (sbAmount > 0) {
            gameState.bet(sbPlayerId, sbAmount);
            logger.info("Player {} posts SB: {}", sbPlayerId, sbAmount);
        }

        // Big Blind
        long bbAmount = Math.min(BIG_BLIND, gameState.getPlayerChips(bbPlayerId));
        if (bbAmount > 0) {
            gameState.bet(bbPlayerId, bbAmount);
            logger.info("Player {} posts BB: {}", bbPlayerId, bbAmount);
        }

        // 3. Deal hole cards
        dealHoleCards();

        // 4. Set first actor (UTG = player after BB)
        // Pre-flop: UTG is the player after BB
        int utgIndex = (bbIndex + 1) % playerCount;
        if (playerCount == 2) {
            // Heads up: SB acts first pre-flop
            utgIndex = sbIndex;
        }
        int firstActor = findNextActivePlayer(utgIndex);
        gameState.setCurrentPlayerTurn(firstActor);
        logger.info("Pre-flop starts. First actor: Player {}", firstActor);

        // 5. Broadcast GameStartPacket to each player (with sanitized state)
        broadcastGameStart();
    }

    private void dealHoleCards() {
        for (int playerId : playerOrder) {
            if (!gameState.isPlayerFolded(playerId)) {
                List<Card> holes = new ArrayList<>();
                holes.add(deck.deal());
                holes.add(deck.deal());
                gameState.dealHoleCards(playerId, holes);
                logger.info("Dealt to {}: {}", playerId, holes);
            }
        }
    }

    private void broadcastGameStart() {
        for (int playerId : playerOrder) {
            GameStartPacket packet = new GameStartPacket();
            packet.setRoomId(roomId);
            packet.setGameType(gameType);
            packet.setPlayerOrder(playerOrder);
            packet.setInitialState(gameState.sanitizeFor(playerId));
            room.sendToPlayer(playerId, packet);
        }
    }

    @Override
    public void handlePlayerAction(PlayerActionPacket actionPacket) {
        if (finished) {
            logger.debug("Game finished, ignoring action");
            return;
        }

        int playerId = actionPacket.getPlayerId();
        String actionType = actionPacket.getActionType();

        if (playerId != gameState.getCurrentPlayerTurn()) {
            logger.debug("Not player {}'s turn (current: {})", playerId, gameState.getCurrentPlayerTurn());
            return;
        }

        boolean actionValid = true;
        switch (actionType) {
            case "FOLD":
                handleFold(playerId);
                break;
            case "CHECK":
                actionValid = handleCheck(playerId);
                break;
            case "CALL":
                handleCall(playerId);
                break;
            case "RAISE":
                actionValid = handleRaise(playerId, actionPacket.getAmount());
                break;
            default:
                logger.warn("Unknown action type: {}", actionType);
                actionValid = false;
        }

        if (!finished && actionValid) {
            playersActedThisRound.add(playerId);

            // Broadcast updated state
            broadcastGameState();

            // Check if betting round is complete
            if (isRoundComplete()) {
                advanceStage();
            } else {
                advanceTurn();
            }
        }
    }

    private void handleFold(int playerId) {
        gameState.fold(playerId);
        logger.info("Player {} folds", playerId);
        checkForWinnerByFold();
    }

    private boolean handleCheck(int playerId) {
        long toCall = gameState.getCurrentBet() - gameState.getPlayerBet(playerId);
        if (toCall > 0) {
            logger.warn("Player {} cannot CHECK, must call {}", playerId, toCall);
            return false;
        }
        logger.info("Player {} checks", playerId);
        return true;
    }

    private void handleCall(int playerId) {
        long toCall = gameState.getCurrentBet() - gameState.getPlayerBet(playerId);
        long playerChips = gameState.getPlayerChips(playerId);

        if (toCall <= 0) {
            // No bet to call, treat as check
            logger.info("Player {} calls (no bet, treated as check)", playerId);
            return;
        }

        // All-in if not enough chips
        long actualCall = Math.min(toCall, playerChips);
        gameState.bet(playerId, actualCall);

        if (actualCall < toCall) {
            logger.info("Player {} calls all-in with {}", playerId, actualCall);
        } else {
            logger.info("Player {} calls {}", playerId, actualCall);
        }
    }

    private boolean handleRaise(int playerId, long raiseAmount) {
        long currentBet = gameState.getCurrentBet();
        long playerBet = gameState.getPlayerBet(playerId);
        long playerChips = gameState.getPlayerChips(playerId);
        long toCall = currentBet - playerBet;

        // raiseAmount is the TOTAL new bet amount the player wants to make
        // Validate: must be >= currentBet + minRaise
        long minRaise = BIG_BLIND;
        long minTotalBet = currentBet + minRaise;

        // Check if this is an all-in (allow any amount if going all-in)
        boolean isAllIn = (raiseAmount >= playerChips);

        if (!isAllIn && raiseAmount < minTotalBet) {
            logger.warn("Player {} raise {} is less than min {}", playerId, raiseAmount, minTotalBet);
            return false;
        }

        // Cap at player's chips
        long actualRaise = Math.min(raiseAmount, playerChips);

        // Calculate how much more they need to put in
        long additionalBet = actualRaise - playerBet;
        if (additionalBet > 0) {
            gameState.bet(playerId, additionalBet);

            // Reset action tracking - all other players must act again
            lastRaiser = playerId;
            playersActedThisRound.clear();
            playersActedThisRound.add(playerId);

            if (isAllIn) {
                logger.info("Player {} raises all-in to {}", playerId, actualRaise);
            } else {
                logger.info("Player {} raises to {}", playerId, actualRaise);
            }
        }

        return true;
    }

    private void advanceTurn() {
        int currentIndex = playerOrder.indexOf(gameState.getCurrentPlayerTurn());
        int nextActor = findNextActivePlayer((currentIndex + 1) % playerOrder.size());
        gameState.setCurrentPlayerTurn(nextActor);

        // Broadcast updated state with new turn
        broadcastGameState();
    }

    private int findNextActivePlayer(int startIndex) {
        int count = playerOrder.size();
        for (int i = 0; i < count; i++) {
            int idx = (startIndex + i) % count;
            int playerId = playerOrder.get(idx);
            if (!gameState.isPlayerFolded(playerId) && gameState.getPlayerChips(playerId) > 0) {
                return playerId;
            }
        }
        // Fallback: return first non-folded player
        for (int playerId : playerOrder) {
            if (!gameState.isPlayerFolded(playerId)) {
                return playerId;
            }
        }
        return playerOrder.get(0);
    }

    private boolean isRoundComplete() {
        List<Integer> activePlayers = getActivePlayers();

        // Count players who can still act (have chips and not folded)
        int playersWhoCanAct = 0;
        for (int playerId : activePlayers) {
            if (gameState.getPlayerChips(playerId) > 0) {
                playersWhoCanAct++;
            }
        }

        // If only one player can act, round is complete
        if (playersWhoCanAct <= 1) {
            return true;
        }

        // Check if all active players have acted and matched the bet
        for (int playerId : activePlayers) {
            if (!playersActedThisRound.contains(playerId)) {
                return false;
            }
            // Must match current bet (or be all-in)
            long playerBet = gameState.getPlayerBet(playerId);
            long playerChips = gameState.getPlayerChips(playerId);
            if (playerBet < gameState.getCurrentBet() && playerChips > 0) {
                return false;
            }
        }
        return true;
    }

    private void advanceStage() {
        playersActedThisRound.clear();
        lastRaiser = -1;
        gameState.resetForNewRound();

        PokerGameState.Stage currentStage = gameState.getCurrentStage();

        switch (currentStage) {
            case PREFLOP:
                dealFlop();
                break;
            case FLOP:
                dealTurn();
                break;
            case TURN:
                dealRiver();
                break;
            case RIVER:
                goToShowdown();
                return;
            default:
                break;
        }

        // Set first actor for post-flop (SB or first active player left of dealer)
        int firstActorIndex = sbIndex;
        int firstActor = findNextActivePlayer(firstActorIndex);
        gameState.setCurrentPlayerTurn(firstActor);

        // Broadcast state
        broadcastGameState();
    }

    private void dealFlop() {
        if (deck.remainingCards() > 0)
            deck.deal(); // Burn
        List<Card> flop = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (deck.remainingCards() > 0) {
                flop.add(deck.deal());
            }
        }
        gameState.dealFlop(flop);
        logger.info("Dealt FLOP: {}", flop);
    }

    private void dealTurn() {
        if (deck.remainingCards() > 0)
            deck.deal(); // Burn
        if (deck.remainingCards() > 0) {
            Card turn = deck.deal();
            gameState.dealTurn(turn);
            logger.info("Dealt TURN: {}", turn);
        }
    }

    private void dealRiver() {
        if (deck.remainingCards() > 0)
            deck.deal(); // Burn
        if (deck.remainingCards() > 0) {
            Card river = deck.deal();
            gameState.dealRiver(river);
            logger.info("Dealt RIVER: {}", river);
        }
    }

    private void goToShowdown() {
        gameState.nextStage();
        determineWinner();
    }

    private void determineWinner() {
        List<Integer> activePlayers = getActivePlayers();
        if (activePlayers.isEmpty()) {
            return;
        }

        List<Card> communityCards = gameState.getCommunityCards();

        // Pre-calculate hand results for all active players
        Map<Integer, PokerHandResult> playerResults = new HashMap<>();
        for (int playerId : activePlayers) {
            List<Card> allCards = new ArrayList<>(gameState.getPlayerHole(playerId));
            allCards.addAll(communityCards);

            if (allCards.size() >= 5) {
                PokerHandResult result = PokerHandEvaluator.evaluate(allCards);
                playerResults.put(playerId, result);
                logger.info("Player {} has {}: {}", playerId, result.getRank(), result.getBestFiveCards());
            }
        }

        // Calculate side pots
        List<PokerGameState.SidePot> pots = gameState.calculateSidePots(activePlayers);
        logger.info("Total pots: {} (main pot value: {})", pots.size(), gameState.getPot());

        // Track overall winner for GameEndPacket
        int overallWinnerId = -1;
        long maxWon = 0;

        // Award each pot to its winner
        for (int i = 0; i < pots.size(); i++) {
            PokerGameState.SidePot sidePot = pots.get(i);
            Set<Integer> eligible = sidePot.getEligiblePlayers();
            long potAmount = sidePot.getAmount();

            if (eligible.isEmpty() || potAmount == 0)
                continue;

            // Find best hand among eligible players
            PokerHandResult bestResult = null;
            int bestPlayerId = -1;

            for (int playerId : eligible) {
                PokerHandResult result = playerResults.get(playerId);
                if (result != null) {
                    if (bestResult == null || result.compareTo(bestResult) > 0) {
                        bestResult = result;
                        bestPlayerId = playerId;
                    }
                }
            }

            if (bestPlayerId >= 0) {
                gameState.awardAmount(bestPlayerId, potAmount);
                String potName = (i == 0) ? "Main pot" : "Side pot " + i;
                logger.info("{} ({}) won by Player {} with {}",
                        potName, potAmount, bestPlayerId, bestResult.getRank());

                // Track who won the most for GameEndPacket
                if (potAmount > maxWon) {
                    maxWon = potAmount;
                    overallWinnerId = bestPlayerId;
                }
            }
        }

        // Set the winner (player who won the most)
        if (overallWinnerId >= 0) {
            winnerId = overallWinnerId;
        } else if (!activePlayers.isEmpty()) {
            winnerId = activePlayers.get(0);
        }

        finished = true;
        logger.info("Game finished. Overall winner: Player {}", winnerId);

        // Clear the pot since we've distributed it via side pots
        gameState.awardPot(-1); // This effectively zeros the pot

        // Broadcast final state
        broadcastGameState();
    }

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
            gameState.awardPot(winnerId);
            logger.info("Winner by fold: Player {}", winnerId);
            broadcastGameState();
        }
    }

    private void broadcastGameState() {
        for (int playerId : playerOrder) {
            GameStatePacket packet = new GameStatePacket();
            packet.setRoomId(roomId);
            packet.setGameType(gameType);
            packet.setGameState(gameState.sanitizeFor(playerId));
            room.sendToPlayer(playerId, packet);
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

    @Override
    public Map<Integer, Long> getCreditChanges() {
        Map<Integer, Long> changes = new HashMap<>();
        for (int playerId : playerOrder) {
            long initial = initialChips.getOrDefault(playerId, 0L);
            long current = gameState.getPlayerChips(playerId);
            long delta = current - initial;
            changes.put(playerId, delta);
            logger.debug("Player {} credit change: {} - {} = {}", playerId, current, initial, delta);
        }
        return changes;
    }
}
