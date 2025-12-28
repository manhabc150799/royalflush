package com.mygame.server.game;

import com.mygame.server.room.GameRoom;
import com.mygame.shared.game.card.Card;
import com.mygame.shared.game.card.Deck;
import com.mygame.shared.game.poker.PokerGameState;
import com.mygame.shared.model.GameType;
import com.mygame.shared.network.packets.game.GameStatePacket;
import com.mygame.shared.network.packets.game.PlayerActionPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Server-side session cho Poker.
 *
 * Ở giai đoạn này logic được giữ đơn giản:
 * - Khởi tạo deck, chia 2 lá cho mỗi người chơi.
 * - Quản lý pot, bets, folded state thông qua {@link PokerGameState}.
 * - Xử lý các action cơ bản: FOLD, CHECK, CALL, RAISE, ALL_IN.
 * - Xác định winner tạm thời dựa trên người chơi còn lại cuối cùng.
 *
 * Việc đánh giá hand chi tiết theo luật Poker đã có trong {@link com.mygame.shared.game.poker.PokerHandEvaluator}
 * và có thể được dùng để nâng cấp logic SHOWDOWN trong tương lai.
 */
public class PokerGameSession extends GameSession {
    private final List<Integer> playerOrder;
    private final PokerGameState gameState;
    private final Deck deck;

    private boolean finished = false;
    private int winnerId = -1;

    public PokerGameSession(int roomId, GameType gameType, GameRoom room, List<Integer> playerOrder) {
        super(roomId, gameType, room);
        this.playerOrder = new ArrayList<>(playerOrder);
        this.deck = new Deck();
        this.deck.shuffle();

        long startingChips = 10_000L;
        int smallBlind = 50;
        int bigBlind = 100;

        this.gameState = new PokerGameState(playerOrder, startingChips, smallBlind, bigBlind);

        dealHoleCards();

        if (!playerOrder.isEmpty()) {
            gameState.setCurrentPlayerTurn(playerOrder.get(0));
        }
    }

    private void dealHoleCards() {
        for (int playerId : playerOrder) {
            List<Card> holes = new ArrayList<>();
            holes.add(deck.deal());
            holes.add(deck.deal());
            gameState.dealHoleCards(playerId, holes);
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

        switch (actionType) {
            case "FOLD":
                gameState.fold(playerId);
                checkForWinnerByFold();
                break;
            case "CHECK":
                handleCheck(playerId);
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
                logger.warn("ActionType {} không hỗ trợ trong PokerGameSession", actionType);
        }

        if (!finished) {
            advanceTurn();
        }
    }

    private void handleCheck(int playerId) {
        long toCall = gameState.getCurrentBet() - gameState.getPlayerBet(playerId);
        if (toCall > 0) {
            logger.debug("Player {} không thể CHECK vì còn thiếu bet {}", playerId, toCall);
        }
    }

    private void handleCall(int playerId) {
        long toCall = gameState.getCurrentBet() - gameState.getPlayerBet(playerId);
        if (toCall > 0) {
            gameState.bet(playerId, toCall);
        }
    }

    private void handleRaise(int playerId, long raiseAmount) {
        long toCall = gameState.getCurrentBet() - gameState.getPlayerBet(playerId);
        long total = Math.max(0, toCall) + Math.max(0, raiseAmount);
        if (total > 0) {
            gameState.bet(playerId, total);
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

    private void checkForWinnerByFold() {
        List<Integer> activePlayers = new ArrayList<>();
        for (int playerId : playerOrder) {
            if (!gameState.isPlayerFolded(playerId) && gameState.getPlayerChips(playerId) >= 0) {
                activePlayers.add(playerId);
            }
        }
        if (activePlayers.size() == 1) {
            finished = true;
            winnerId = activePlayers.get(0);
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

