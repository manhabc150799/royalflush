package com.mygame.shared.network.packets;

/**
 * Packet yêu cầu rời room
 */
public class LeaveRoomRequest {
    private int roomId;
    
    // Default constructor cho Kryo
    public LeaveRoomRequest() {
    }
    
    public LeaveRoomRequest(int roomId) {
        this.roomId = roomId;
    }
    
    public int getRoomId() {
        return roomId;
    }
    
    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }
}

