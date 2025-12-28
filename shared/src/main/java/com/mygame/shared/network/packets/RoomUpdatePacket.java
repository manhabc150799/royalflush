package com.mygame.shared.network.packets;

import com.mygame.shared.model.RoomInfo;

/**
 * Packet broadcast từ server khi room có thay đổi (player join/leave, status change)
 */
public class RoomUpdatePacket {
    private RoomInfo roomInfo;
    
    // Default constructor cho Kryo
    public RoomUpdatePacket() {
    }
    
    public RoomUpdatePacket(RoomInfo roomInfo) {
        this.roomInfo = roomInfo;
    }
    
    public RoomInfo getRoomInfo() {
        return roomInfo;
    }
    
    public void setRoomInfo(RoomInfo roomInfo) {
        this.roomInfo = roomInfo;
    }
}

