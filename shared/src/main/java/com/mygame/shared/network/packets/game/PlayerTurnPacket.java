package com.mygame.shared.network.packets.game;

import com.mygame.shared.model.GameType;

/**
 * Packet thông báo lượt chơi hiện tại cho clients.
 */
public class PlayerTurnPacket {
    private int roomId;
    private GameType gameType;
    private int currentPlayerId;

    public PlayerTurnPacket() {
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }

    public int getCurrentPlayerId() {
        return currentPlayerId;
    }

    public void setCurrentPlayerId(int currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
    }
}
