package com.mygame.shared.network.packets.game;

/**
 * Packet sent by client to vote for play again or return to lobby after game
 * ends.
 */
public class PlayAgainVotePacket {
    private int roomId;
    private int playerId;
    private String voteType; // "PLAY_AGAIN" or "RETURN_TO_LOBBY"

    public PlayAgainVotePacket() {
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public String getVoteType() {
        return voteType;
    }

    public void setVoteType(String voteType) {
        this.voteType = voteType;
    }
}
