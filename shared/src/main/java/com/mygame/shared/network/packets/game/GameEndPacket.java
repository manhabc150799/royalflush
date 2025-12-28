package com.mygame.shared.network.packets.game;

import com.mygame.shared.model.GameType;

import java.util.List;

/**
 * Packet gửi kết quả cuối ván từ server tới clients.
 */
public class GameEndPacket {
    private int roomId;
    private GameType gameType;
    private int winnerId;
    private java.util.List<Integer> playerIds;
    private java.util.List<Long> creditChanges;

    public GameEndPacket() {
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

    public int getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(int winnerId) {
        this.winnerId = winnerId;
    }

    public java.util.List<Integer> getPlayerIds() {
        return playerIds;
    }

    public void setPlayerIds(java.util.List<Integer> playerIds) {
        this.playerIds = playerIds;
    }

    public java.util.List<Long> getCreditChanges() {
        return creditChanges;
    }

    public void setCreditChanges(java.util.List<Long> creditChanges) {
        this.creditChanges = creditChanges;
    }
}
