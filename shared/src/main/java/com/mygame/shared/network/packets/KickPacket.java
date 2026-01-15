package com.mygame.shared.network.packets;

/**
 * Packet sent to a player when they are kicked from a room.
 * Reasons include: BANKRUPT (balance <= 0), DISCONNECTED, HOST_KICK, etc.
 */
public class KickPacket {
    private int roomId;
    private String reason;

    // Default constructor for Kryo
    public KickPacket() {
    }

    public KickPacket(int roomId, String reason) {
        this.roomId = roomId;
        this.reason = reason;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    // Common kick reasons
    public static final String REASON_BANKRUPT = "BANKRUPT";
    public static final String REASON_DISCONNECTED = "DISCONNECTED";
    public static final String REASON_HOST_KICK = "HOST_KICK";
    public static final String REASON_GAME_OVER = "GAME_OVER";
}
