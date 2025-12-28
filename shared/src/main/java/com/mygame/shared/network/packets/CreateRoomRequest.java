package com.mygame.shared.network.packets;

import com.mygame.shared.model.GameType;

/**
 * Packet yêu cầu tạo room mới
 */
public class CreateRoomRequest {
    private String roomName;
    private GameType gameType;
    private int maxPlayers;
    
    // Default constructor cho Kryo
    public CreateRoomRequest() {
        this.maxPlayers = 5; // Default
    }
    
    public CreateRoomRequest(String roomName, GameType gameType, int maxPlayers) {
        this.roomName = roomName;
        this.gameType = gameType;
        this.maxPlayers = maxPlayers;
    }
    
    public String getRoomName() {
        return roomName;
    }
    
    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
    
    public GameType getGameType() {
        return gameType;
    }
    
    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }
    
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
}

