package com.mygame.shared.network.packets;

import com.mygame.shared.model.RoomInfo;

/**
 * Packet phản hồi tham gia room
 */
public class JoinRoomResponse {
    private boolean success;
    private RoomInfo roomInfo;
    private String errorMessage;
    
    // Default constructor cho Kryo
    public JoinRoomResponse() {
    }
    
    public JoinRoomResponse(boolean success, RoomInfo roomInfo, String errorMessage) {
        this.success = success;
        this.roomInfo = roomInfo;
        this.errorMessage = errorMessage;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public RoomInfo getRoomInfo() {
        return roomInfo;
    }
    
    public void setRoomInfo(RoomInfo roomInfo) {
        this.roomInfo = roomInfo;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

