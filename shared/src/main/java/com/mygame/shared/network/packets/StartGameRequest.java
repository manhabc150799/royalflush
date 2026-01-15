package com.mygame.shared.network.packets;

/**
 * Request sent by Host to start the game in a room.
 * Server validates that sender is host and min players are met.
 */
public class StartGameRequest {
    private int roomId;

    // Default constructor for Kryo
    public StartGameRequest() {
    }

    public StartGameRequest(int roomId) {
        this.roomId = roomId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }
}
