package com.mygame.server.game;

import com.mygame.server.database.UserDAO;
import com.mygame.server.room.GameRoom;
import com.mygame.shared.game.card.Card;
import com.mygame.shared.game.card.Deck;
import com.mygame.shared.game.card.Suit;
import com.mygame.shared.game.tienlen.CardCollection;
import com.mygame.shared.game.tienlen.TienLenCombinationType;
import com.mygame.shared.game.tienlen.TienLenGameState;
import com.mygame.shared.model.GameType;
import com.mygame.shared.network.packets.game.GameStartPacket;
import com.mygame.shared.network.packets.game.GameStatePacket;
import com.mygame.shared.network.packets.game.PlayerActionPacket;
import com.mygame.shared.network.packets.game.PlayAgainVotePacket;
import com.mygame.shared.network.packets.game.PlayAgainStatusPacket;
import com.mygame.shared.network.packets.game.GameEndPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;

/**
 * Server-side session for Tien Len Mien Nam.
 * Implements strict rules: 3 < 2, Combinations, Round Logic (Skip/Pass),
 * Instant Win.
 * Scoring: Fixed Buy-In 10,000. Winner takes Pot.
 */
public class TienLenGameSession extends GameSession {
    private static final Logger logger = LoggerFactory.getLogger(TienLenGameSession.class);
    private final List<Integer> playerOrder;
    private final TienLenGameState gameState;

    private boolean finished = false;
    private int winnerId = -1;

    // Voting state for play again system
    private boolean inVotingPhase = false;
    private Set<Integer> playAgainVotes = new HashSet<>();

    // Scoring
    private static final long BUY_IN_AMOUNT = 10_000L;
    private Map<Integer, Long> initialCredits;
    private long pot = 0;

    public TienLenGameSession(int roomId, GameType gameType, GameRoom room, List<Integer> playerOrder) {
        super(roomId, gameType, room);
        this.playerOrder = new ArrayList<>(playerOrder);
        this.initialCredits = new HashMap<>();

        // Load initial credits and deduct Buy-In
        try {
            UserDAO userDAO = new UserDAO(com.mygame.server.database.DatabaseManager.getInstance());
            for (Integer userId : playerOrder) {
                try {
                    UserDAO.UserProfile profile = userDAO.getUserProfile(userId);
                    long credits = (profile != null) ? profile.getCredits() : 0;
                    initialCredits.put(userId, credits);
                    pot += BUY_IN_AMOUNT; // Collect buy-in
                } catch (SQLException e) {
                    logger.error("Failed to load credits for user {}", userId, e);
                    initialCredits.put(userId, 0L);
                }
            }
        } catch (Exception e) {
            logger.error("Error creating UserDAO", e);
        }

        this.gameState = new TienLenGameState(playerOrder);
        // Sync initial credits to game state
        for (Map.Entry<Integer, Long> entry : initialCredits.entrySet()) {
            this.gameState.setPlayerCredits(entry.getKey(), entry.getValue());
        }

        dealHands();

        // INTEGRATION FIX: Broadcast GameStart logic so clients navigate to GameScreen
        broadcastGameStart();
        broadcastState();

        // Check instant win (Toi Trang) immediately
        int instantWinner = checkInstantWin();
        if (instantWinner != -1) {
            logger.info("Instant Winner (Toi Trang) detected: {}", instantWinner);
            endGame(instantWinner);
        } else {
            // Determine starting player (Must have 3 Spades for first game logic)
            // For simplicity, we enforce 3-Spades start rule every game for now, or Random
            // if not found
            determineFirstPlayer();
        }
    }

    private void broadcastGameStart() {
        GameStartPacket packet = new GameStartPacket();
        packet.setRoomId(roomId);
        packet.setGameType(gameType);
        packet.setPlayerOrder(new ArrayList<>(playerOrder)); // Copy to avoid Kryo shared reference issues if any
        packet.setInitialState(gameState);
        if (room != null) {
            room.broadcast(packet);
        }
    }

    private void dealHands() {
        Deck deck = new Deck();
        deck.shuffle();
        List<Card> allCards = new ArrayList<>();
        for (int i = 0; i < 52; i++)
            allCards.add(deck.deal());

        int playerCount = playerOrder.size();
        int cardsPerPlayer = 13;

        for (int i = 0; i < playerCount; i++) {
            int playerId = playerOrder.get(i);
            List<Card> hand = new ArrayList<>();
            for (int j = 0; j < cardsPerPlayer; j++) {
                hand.add(allCards.get(i * cardsPerPlayer + j));
            }
            gameState.dealHand(playerId, hand);
        }
    }

    private void determineFirstPlayer() {
        // Find player with 3 of Spades
        int starter = -1;
        Card threeSpades = new Card(3, Suit.SPADES);

        for (Integer pid : playerOrder) {
            if (gameState.getPlayerHand(pid).contains(threeSpades)) {
                starter = pid;
                logger.info("Player {} has the 3 of Spades", pid);
                break;
            }
        }

        if (starter != -1) {
            // Set turn to this player
            int index = playerOrder.indexOf(starter);
            gameState.setCurrentPlayerTurn(index);
            logger.info("First player determined: {} (Index {})", starter, index);
        } else {
            // Fallback (should rare happen with 4 players, common with 2)
            // If < 4 players, 3 Spades might not be dealt. Random start.
            gameState.setCurrentPlayerTurn(0);
            logger.info("3 of Spades not dealt. Defaulting to Player {} (Index 0)", playerOrder.get(0));
        }

        // Start new round state
        gameState.startNewRound();
    }

    /**
     * Check Instant Win conditions (Toi Trang)
     */
    private int checkInstantWin() {
        for (Integer pid : playerOrder) {
            List<Card> hand = gameState.getPlayerHand(pid);
            // 1. 4 of a Kind (2s) - Tứ quý heo
            if (countRank(hand, 2) == 4)
                return pid;
            // 2. 6 Pairs
            if (countPairs(hand) >= 6)
                return pid;
            // 3. Dragon Straight (3 to A) - Sảnh rồng? (Usually 3-A, no 2)
            if (isDragonStraight(hand))
                return pid;
            // 4. Quad 3 (First game only - optional, implementing simplified)
        }
        return -1;
    }

    private int countRank(List<Card> hand, int rank) {
        return (int) hand.stream().filter(c -> c.getRank() == rank).count();
    }

    private int countPairs(List<Card> hand) {
        // Simplified pair counting
        Map<Integer, Integer> counts = new HashMap<>();
        for (Card c : hand)
            counts.put(c.getRank(), counts.getOrDefault(c.getRank(), 0) + 1);
        int pairs = 0;
        for (int count : counts.values())
            if (count >= 2)
                pairs++;
        return pairs;
    }

    private boolean isDragonStraight(List<Card> hand) {
        // Needs 3,4,5,6,7,8,9,10,J,Q,K,A (12 cards) at least? Usually requires 12
        // specific ranks
        // Tien Len Sảnh Rồng is 3 to A (12 cards).
        Set<Integer> ranks = new HashSet<>();
        for (Card c : hand)
            ranks.add(c.getRank());
        // Check 3 to 14 present
        for (int r = 3; r <= 14; r++)
            if (!ranks.contains(r))
                return false;
        return true;
    }

    @Override
    public void handlePlayerAction(PlayerActionPacket actionPacket) {
        if (finished)
            return;

        int playerId = actionPacket.getPlayerId();
        String actionType = actionPacket.getActionType();
        int currentPlayerId = gameState.getCurrentPlayerId();

        logger.info("Action received: {} from Player {} (Current Turn: {})", actionType, playerId, currentPlayerId);

        // Validate turn
        if (playerId != currentPlayerId) {
            logger.warn("Ignore action from {} (not turn, expected {})", playerId, currentPlayerId);
            return;
        }

        switch (actionType) {
            case "PLAY":
                handlePlay(playerId, actionPacket.getCards());
                break;
            case "SKIP":
                handleSkip(playerId);
                break;
            default:
                logger.warn("Unknown action: {}", actionType);
        }
    }

    private void handlePlay(int playerId, List<Card> cards) {
        // 1. Check card ownership
        if (!gameState.getPlayerHand(playerId).containsAll(cards)) {
            logger.warn("Player {} tried to play cards they don't have: {}", playerId, cards);
            return;
        }

        // 2. Detect Type
        TienLenCombinationType type = CardCollection.detectCombination(cards);
        if (type == TienLenCombinationType.INVALID) {
            logger.warn("Invalid combination played by {}", playerId);
            return;
        }

        // 3. Check Move Validity
        List<Card> boardCards = gameState.getCurrentTrick();
        TienLenCombinationType boardType = gameState.getCurrentTrickType();

        // Validate 3 Spades rule for the FIRST turn of the FIRST game
        // (assuming determiningFirstPlayer set turn correctly, but we ensure the CARD
        // is played)
        if (isFirstTurnOfGame() && !containsThreeSpades(cards)) {
            // Check if player actually HAS 3 spades (sanity check)
            if (gameState.getPlayerHand(playerId).contains(new Card(3, Suit.SPADES))) {
                logger.warn("REJECTED: First turn must contain 3 Spades. Player {} played {}", playerId, cards);
                return;
            } else {
                logger.warn("Player {} started but doesn't have 3 Spades? Logic error.", playerId);
            }
        }

        if (boardCards != null && !boardCards.isEmpty()) {
            // Must beat current board
            if (!CardCollection.canBeat(boardType, boardCards, type, cards)) {
                logger.warn("REJECTED: Cannot beat current cards. Board: {}, Played: {}", boardCards, cards);
                return;
            }
        }

        logger.info("Player {} played valid move: {} ({})", playerId, cards, type);

        // 4. Update State
        gameState.playCards(playerId, cards, type);

        // 5. Check Finish
        if (gameState.isPlayerFinished(playerId)) {
            logger.info("Player {} finished!", playerId);
            // Don't end game immediately. Check how many left.
            checkGameEnd();
        }

        // 6. Next Turn
        if (!finished) {
            gameState.nextTurn();
        }

        broadcastState();
    }

    private boolean isFirstTurnOfGame() {
        // Check if this is the very first move of the session (Pot check + hand size)
        // And ensuring 3 Spade check is only if someone actually holds it
        return pot == BUY_IN_AMOUNT * playerOrder.size()
                && gameState.getPlayerHand(playerOrder.get(0)).size() == 13
                && gameState.getWinners().isEmpty()
                && gameState.getCurrentTrick().isEmpty();
    }

    private boolean containsThreeSpades(List<Card> cards) {
        Card target = new Card(3, Suit.SPADES);
        for (Card c : cards) {
            if (c.getRank() == 3 && c.getSuit() == Suit.SPADES)
                return true;
        }
        return false;
    }

    private void handleSkip(int playerId) {
        if (gameState.getCurrentTrick().isEmpty()) {
            return;
        }

        gameState.passTurn(playerId);
        gameState.nextTurn();

        checkRoundEnd();
        broadcastState();
    }

    private void checkRoundEnd() {
        int lastPlayer = gameState.getLastPlayedPlayer();
        if (lastPlayer == -1)
            return;

        // Count active players (not finished, not skipped)
        int active = 0;
        for (Integer pid : playerOrder) {
            if (!gameState.isPlayerFinished(pid) && !gameState.isSkipped(pid)) {
                active++;
            }
        }

        // If everyone else passed (active <= 1), the Last Player wins the round.
        // Note: active includes the current turn player if they haven't skipped yet.
        // If strict logic: we cycle through turns. If we come back to 'lastPlayer' (or
        // next if last is finished)
        // and everyone else skipped, round ends.

        // Simpler check: If all other players are skipped or finished?
        boolean allOthersSkipped = true;
        for (Integer pid : playerOrder) {
            if (pid != lastPlayer && !gameState.isPlayerFinished(pid) && !gameState.isSkipped(pid)) {
                allOthersSkipped = false;
                break;
            }
        }

        if (allOthersSkipped) {
            // Round finished.
            gameState.startNewRound();

            // Determine who leads next
            if (gameState.isPlayerFinished(lastPlayer)) {
                // If round winner is finished, pass lead to next active player in order
                int lastIdx = playerOrder.indexOf(lastPlayer);
                int size = playerOrder.size();
                for (int i = 1; i < size; i++) {
                    int nextIdx = (lastIdx + i) % size;
                    int nextPid = playerOrder.get(nextIdx);
                    if (!gameState.isPlayerFinished(nextPid)) {
                        gameState.setCurrentPlayerTurn(nextIdx);
                        break;
                    }
                }
            } else {
                // Winner leads
                gameState.setCurrentPlayerTurn(playerOrder.indexOf(lastPlayer));
            }
        }
    }

    private void checkGameEnd() {
        int totalPlayers = playerOrder.size();
        int finishedCount = gameState.getWinners().size();

        // End if only 1 player remaining (or 0 if 1-player test)
        if (totalPlayers - finishedCount <= 1) {
            // Add the last remaining player to winners list (as last place)
            for (Integer pid : playerOrder) {
                if (!gameState.isPlayerFinished(pid)) {
                    // Mark as finished for tracking (optional)
                    // gameState.getWinners().add(pid); // Don't add to winners if they didn't
                    // finish 'out'
                    break;
                }
            }

            // Determine session winner (Rank 1)
            int sessionWinner = gameState.getWinners().isEmpty() ? -1 : gameState.getWinners().get(0);
            endGame(sessionWinner);
        }
    }

    private void endGame(int winner) {
        finished = true;
        winnerId = winner;
        inVotingPhase = true;
        playAgainVotes.clear();

        broadcastState();
        broadcastGameEndPacket();
    }

    /**
     * Broadcast GameEndPacket to all clients when game finishes.
     */
    private void broadcastGameEndPacket() {
        if (room == null)
            return;

        GameEndPacket packet = new GameEndPacket();
        packet.setRoomId(roomId);
        packet.setGameType(gameType);
        packet.setWinnerId(winnerId);
        packet.setPlayerIds(new ArrayList<>(playerOrder));

        // Build credit changes list
        Map<Integer, Long> changes = getCreditChanges();
        List<Long> creditChangesList = new ArrayList<>();
        for (Integer pid : playerOrder) {
            creditChangesList.add(changes.getOrDefault(pid, 0L));
        }
        packet.setCreditChanges(creditChangesList);

        room.broadcast(packet);
        logger.info("Broadcast GameEndPacket. Winner: {}", winnerId);
    }

    /**
     * Handle play again vote from a player.
     * 
     * @param packet The vote packet from client
     */
    public void handlePlayAgainVote(PlayAgainVotePacket packet) {
        if (!inVotingPhase) {
            logger.warn("Received vote but not in voting phase");
            return;
        }

        int playerId = packet.getPlayerId();
        String voteType = packet.getVoteType();

        logger.info("Player {} voted: {}", playerId, voteType);

        if ("RETURN_TO_LOBBY".equals(voteType)) {
            // One player wants to return - all return to lobby
            logger.info("Player {} triggered return to lobby for all players", playerId);
            inVotingPhase = false;
            broadcastVotingStatus("RETURNING_TO_LOBBY");
            return;
        }

        if ("PLAY_AGAIN".equals(voteType)) {
            playAgainVotes.add(playerId);

            // Check if all players voted
            int totalPlayers = playerOrder.size();
            if (playAgainVotes.size() >= totalPlayers) {
                // All voted to play again - start new game
                logger.info("All {} players voted to play again. Starting new game.", totalPlayers);
                inVotingPhase = false;
                broadcastVotingStatus("STARTING_NEW_GAME");
                restartGame();
            } else {
                // Still waiting for more votes
                broadcastVotingStatus("VOTING");
            }
        }
    }

    /**
     * Broadcast voting status to all clients.
     */
    private void broadcastVotingStatus(String status) {
        if (room == null)
            return;

        PlayAgainStatusPacket packet = new PlayAgainStatusPacket();
        packet.setRoomId(roomId);
        packet.setCurrentVotes(playAgainVotes.size());
        packet.setTotalRequired(playerOrder.size());
        packet.setStatus(status);
        packet.setVoterIds(new ArrayList<>(playAgainVotes));

        room.broadcast(packet);
        logger.info("Broadcast voting status: {} ({}/{})", status, playAgainVotes.size(), playerOrder.size());
    }

    /**
     * Restart the game with the same players.
     */
    private void restartGame() {
        // Reset game state
        finished = false;
        winnerId = -1;
        pot = 0;
        playAgainVotes.clear();

        // Collect buy-ins again
        for (Integer userId : playerOrder) {
            pot += BUY_IN_AMOUNT;
        }

        // Create new game state
        gameState.reset(playerOrder);

        // Deal new hands
        dealHands();

        // Broadcast game start
        broadcastGameStart();
        broadcastState();

        // Check instant win
        int instantWinner = checkInstantWin();
        if (instantWinner != -1) {
            logger.info("Instant Winner (Toi Trang) detected: {}", instantWinner);
            endGame(instantWinner);
        } else {
            determineFirstPlayer();
        }
    }

    /**
     * Check if in voting phase.
     */
    public boolean isInVotingPhase() {
        return inVotingPhase;
    }

    private void broadcastState() {
        if (room != null) {
            room.broadcast(buildGameStatePacket());
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
        if (!finished)
            return changes;

        // Payout: Winner takes Pot.
        // Others lose their buy-in.
        if (winnerId != -1) {
            changes.put(winnerId, pot - BUY_IN_AMOUNT);
            for (Integer pid : playerOrder) {
                if (pid != winnerId) {
                    changes.put(pid, -BUY_IN_AMOUNT);
                }
            }
        }
        return changes;
    }
}
