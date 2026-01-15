package com.mygame.server.game;

import com.mygame.server.room.GameRoom;
import com.mygame.shared.model.GameType;
import com.mygame.shared.network.packets.game.GameStatePacket;
import com.mygame.shared.network.packets.game.PlayerActionPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base server-side game session gắn với một {@link GameRoom}.
 *
 * Nhiệm vụ chính:
 * - Nắm giữ state game trong room.
 * - Nhận PlayerActionPacket từ ServerListener/RoomHandler và cập nhật state.
 * - Broadcast GameStatePacket / PlayerTurnPacket / GameEndPacket về cho toàn bộ
 * room.
 */
public abstract class GameSession {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final int roomId;
    protected final GameType gameType;
    protected final GameRoom room;

    protected GameSession(int roomId, GameType gameType, GameRoom room) {
        this.roomId = roomId;
        this.gameType = gameType;
        this.room = room;
    }

    public int getRoomId() {
        return roomId;
    }

    public GameType getGameType() {
        return gameType;
    }

    public GameRoom getRoom() {
        return room;
    }

    /**
     * Xử lý một action đến từ client.
     */
    public abstract void handlePlayerAction(PlayerActionPacket actionPacket);

    /**
     * Lấy snapshot state hiện tại để sync cho client mới join hoặc resync.
     */
    public abstract GameStatePacket buildGameStatePacket();

    /**
     * Cho biết ván đã kết thúc hay chưa.
     */
    public abstract boolean isFinished();

    /**
     * Trả về userId thắng ván (nếu đã có), -1 nếu chưa xác định.
     */
    public abstract int getWinnerId();
}
