package com.mygame.shared.model;

import java.util.List;

/**
 * Model chứa thông tin một game room
 */
public class RoomInfo {
    private int roomId;
    private String roomName;
    private GameType gameType;
    private int hostUserId;
    private String hostUsername;
    private int maxPlayers;
    private int currentPlayers;
    private String status; // WAITING, PLAYING, FINISHED
    private List<RoomPlayerInfo> players;
    
    // Default constructor cho Kryo
    public RoomInfo() {
    }
    
    // Getters and Setters
    public int getRoomId() {
        return roomId;
    }
    
    public void setRoomId(int roomId) {
        this.roomId = roomId;
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
    
    public int getHostUserId() {
        return hostUserId;
    }
    
    public void setHostUserId(int hostUserId) {
        this.hostUserId = hostUserId;
    }
    
    public String getHostUsername() {
        return hostUsername;
    }
    
    public void setHostUsername(String hostUsername) {
        this.hostUsername = hostUsername;
    }
    
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
    
    public int getCurrentPlayers() {
        return currentPlayers;
    }
    
    public void setCurrentPlayers(int currentPlayers) {
        this.currentPlayers = currentPlayers;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<RoomPlayerInfo> getPlayers() {
        return players;
    }
    
    public void setPlayers(List<RoomPlayerInfo> players) {
        this.players = players;
    }
    
    /**
     * Inner class chứa thông tin player trong room
     */
    public static class RoomPlayerInfo {
        private int userId;
        private String username;
        private int position;
        
        // Default constructor cho Kryo
        public RoomPlayerInfo() {
        }
        
        public RoomPlayerInfo(int userId, String username, int position) {
            this.userId = userId;
            this.username = username;
            this.position = position;
        }
        
        public int getUserId() {
            return userId;
        }
        
        public void setUserId(int userId) {
            this.userId = userId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public int getPosition() {
            return position;
        }
        
        public void setPosition(int position) {
            this.position = position;
        }
    }
}

