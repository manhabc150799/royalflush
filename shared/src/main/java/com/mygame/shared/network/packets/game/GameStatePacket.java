package com.mygame.shared.network.packets.game;

import com.mygame.shared.model.GameType;

/**
 * Packet sync full hoặc partial game state từ server tới clients.
 */
public class GameStatePacket {
    private int roomId;
    private GameType gameType;
    private Object gameState; // PokerGameState hoặc TienLenGameState

    public GameStatePacket() {
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

    public Object getGameState() {
        return gameState;
    }

    public void setGameState(Object gameState) {
        this.gameState = gameState;
    }
}
