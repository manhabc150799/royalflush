package com.mygame.shared.network.packets.game;

import com.mygame.shared.game.card.Card;
import com.mygame.shared.model.GameType;

import java.util.List;

/**
 * Packet gửi từ client lên server mô tả một action trong game.
 *
 * Thiết kế nhẹ, đủ cho cả Poker và Tiến Lên:
 * - POKER: dùng actionType (FOLD/CHECK/CALL/RAISE/ALL_IN) + amount.
 * - TIENLEN: dùng actionType (PLAY/SKIP) + cards.
 */
public class PlayerActionPacket {
    private int roomId;
    private int playerId;
    private GameType gameType;
    private String actionType;
    private long amount; // dùng cho bet/raise trong Poker
    private java.util.List<Card> cards; // dùng cho Tiến Lên (các lá đánh ra)

    public PlayerActionPacket() {
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public java.util.List<Card> getCards() {
        return cards;
    }

    public void setCards(java.util.List<Card> cards) {
        this.cards = cards;
    }
}
