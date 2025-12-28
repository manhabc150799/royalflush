package com.mygame.shared.network.packets;

import com.mygame.shared.model.RoomInfo;
import java.util.List;

/**
 * Packet phản hồi danh sách rooms
 */
public class ListRoomsResponse {
    private List<RoomInfo> rooms;
    
    // Default constructor cho Kryo
    public ListRoomsResponse() {
    }
    
    public ListRoomsResponse(List<RoomInfo> rooms) {
        this.rooms = rooms;
    }
    
    public List<RoomInfo> getRooms() {
        return rooms;
    }
    
    public void setRooms(List<RoomInfo> rooms) {
        this.rooms = rooms;
    }
}

