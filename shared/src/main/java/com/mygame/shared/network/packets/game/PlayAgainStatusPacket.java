package com.mygame.shared.network.packets.game;

import java.util.List;

/**
 * Packet sent by server to broadcast voting status to all clients after game
 * ends.
 */
public class PlayAgainStatusPacket {
    private int roomId;
    private int currentVotes; // Number of players who voted to play again (0-4)
    private int totalRequired; // Total votes needed (4)
    private String status; // "VOTING", "STARTING_NEW_GAME", "RETURNING_TO_LOBBY"
    private List<Integer> voterIds; // Players who have voted to play again

    public PlayAgainStatusPacket() {
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getCurrentVotes() {
        return currentVotes;
    }

    public void setCurrentVotes(int currentVotes) {
        this.currentVotes = currentVotes;
    }

    public int getTotalRequired() {
        return totalRequired;
    }

    public void setTotalRequired(int totalRequired) {
        this.totalRequired = totalRequired;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Integer> getVoterIds() {
        return voterIds;
    }

    public void setVoterIds(List<Integer> voterIds) {
        this.voterIds = voterIds;
    }
}
