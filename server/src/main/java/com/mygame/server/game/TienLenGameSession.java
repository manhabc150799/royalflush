package com.mygame.server.game;

import com.mygame.server.room.GameRoom;
import com.mygame.shared.game.card.Card;
import com.mygame.shared.game.card.Deck;
import com.mygame.shared.game.tienlen.TienLenGameState;
import com.mygame.shared.model.GameType;
import com.mygame.shared.network.packets.game.GameStatePacket;
import com.mygame.shared.network.packets.game.PlayerActionPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side session cho Tiến Lên.
 *
 * Logic giai đoạn đầu:
 * - Chia 13 lá cho mỗi người chơi.
 * - Sử dụng {@link TienLenGameState} để validate lượt đánh/bỏ lượt.
 * - Xử lý actionType: PLAY, SKIP.
 * - Sync state tới clients thông qua {@link GameStatePacket}.
 */
public class TienLenGameSession extends GameSession {
    private final List<Integer> playerOrder;
    private final TienLenGameState gameState;

    private boolean finished = false;
    private int winnerId = -1;

    public TienLenGameSession(int roomId, GameType gameType, GameRoom room, List<Integer> playerOrder) {
        super(roomId, gameType, room);
        this.playerOrder = new ArrayList<>(playerOrder);

        this.gameState = new TienLenGameState(playerOrder);
        dealHands();

        if (!playerOrder.isEmpty()) {
            // Tạm thời: cho player đầu tiên trong danh sách đi trước
            // (có thể nâng cấp để chọn người giữ 3♠ sau này).
            gameState.nextTurn(); // đảm bảo currentPlayerTurn index 0
        }
    }

    private void dealHands() {
        Deck deck = new Deck();
        deck.shuffle();

        List<Card> allCards = new ArrayList<>();
        for (int i = 0; i < 52; i++) {
            allCards.add(deck.deal());
        }

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

    @Override
    public void handlePlayerAction(PlayerActionPacket actionPacket) {
        if (finished) {
            return;
        }

        int playerId = actionPacket.getPlayerId();
        String actionType = actionPacket.getActionType();

        int currentPlayerId = gameState.getCurrentPlayerId();
        if (playerId != currentPlayerId) {
            logger.debug("Bỏ qua action của player {} vì chưa tới lượt (current: {})", playerId, currentPlayerId);
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
                logger.warn("ActionType {} không hỗ trợ trong TienLenGameSession", actionType);
        }
    }

    private void handlePlay(int playerId, java.util.List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            logger.debug("Player {} gửi PLAY nhưng không có cards", playerId);
            return;
        }

        boolean ok = gameState.playCards(playerId, cards);
        if (!ok) {
            logger.debug("Lượt đánh của player {} không hợp lệ, bị từ chối", playerId);
            return;
        }

        Integer winner = gameState.getWinner();
        if (winner != null) {
            finished = true;
            winnerId = winner;
            return;
        }

        gameState.nextTurn();
    }

    private void handleSkip(int playerId) {
        gameState.skipTurn(playerId);
        gameState.nextTurn();
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

