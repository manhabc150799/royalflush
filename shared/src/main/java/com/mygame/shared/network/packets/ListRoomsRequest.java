package com.mygame.shared.network.packets;

import com.mygame.shared.model.GameType;

/**
 * Packet yêu cầu danh sách rooms
 */
public class ListRoomsRequest {
    private GameType gameType;
    
    // Default constructor cho Kryo
    public ListRoomsRequest() {
    }
    
    public ListRoomsRequest(GameType gameType) {
        this.gameType = gameType;
    }
    
    public GameType getGameType() {
        return gameType;
    }
    
    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }
}

