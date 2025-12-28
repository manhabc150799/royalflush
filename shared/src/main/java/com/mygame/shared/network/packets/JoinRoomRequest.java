package com.mygame.shared.network.packets;

/**
 * Packet yêu cầu tham gia room
 */
public class JoinRoomRequest {
    private int roomId;
    
    // Default constructor cho Kryo
    public JoinRoomRequest() {
    }
    
    public JoinRoomRequest(int roomId) {
        this.roomId = roomId;
    }
    
    public int getRoomId() {
        return roomId;
    }
    
    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }
}

