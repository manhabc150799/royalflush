package com.mygame.shared.network.packets.game;

import com.mygame.shared.model.GameType;

import java.util.List;

/**
 * Packet gửi từ server tới tất cả clients khi bắt đầu một ván game trong room.
 * Chứa thông tin loại game, roomId và state ban đầu để client sync.
 */
public class GameStartPacket {
    private int roomId;
    private GameType gameType;
    /** Danh sách userId theo thứ tự chơi. */
    private java.util.List<Integer> playerOrder;
    /**
     * Snapshot state ban đầu. Với POKER là {@code PokerGameState},
     * với TIENLEN là {@code TienLenGameState}.
     */
    private Object initialState;

    public GameStartPacket() {
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }

    public java.util.List<Integer> getPlayerOrder() {
        return playerOrder;
    }

    public void setPlayerOrder(java.util.List<Integer> playerOrder) {
        this.playerOrder = playerOrder;
    }

    public Object getInitialState() {
        return initialState;
    }

    public void setInitialState(Object initialState) {
        this.initialState = initialState;
    }
}
