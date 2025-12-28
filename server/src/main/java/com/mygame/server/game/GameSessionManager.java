package com.mygame.server.game;

import com.mygame.server.database.DatabaseManager;
import com.mygame.server.database.MatchHistoryDAO;
import com.mygame.server.database.UserDAO;
import com.mygame.server.room.GameRoom;
import com.mygame.server.room.GameRoomManager;
import com.mygame.shared.model.GameType;
import com.mygame.shared.model.MatchMode;
import com.mygame.shared.network.packets.game.GameEndPacket;
import com.mygame.shared.network.packets.game.GameStartPacket;
import com.mygame.shared.network.packets.game.GameStatePacket;
import com.mygame.shared.network.packets.game.PlayerActionPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý vòng đời các {@link GameSession} gắn với {@link GameRoom}.
 *
 * Trách nhiệm:
 * - Tạo session phù hợp với loại game (Poker, Tiến Lên) khi ván bắt đầu.
 * - Nhận {@link PlayerActionPacket} từ ServerListener và forward vào session tương ứng.
 * - Broadcast {@link GameStartPacket}, {@link GameStatePacket}, {@link com.mygame.shared.network.packets.game.PlayerTurnPacket}
 *   và {@link GameEndPacket} tới toàn bộ room.
 * - Kết thúc ván: cập nhật credits, rank, thống kê win/loss và lưu match history.
 */
public class GameSessionManager {
    private static final Logger logger = LoggerFactory.getLogger(GameSessionManager.class);

    private final GameRoomManager roomManager;
    private final MatchHistoryDAO matchHistoryDAO;
    private final UserDAO userDAO;

    private final Map<Integer, GameSession> sessions = new ConcurrentHashMap<>(); // roomId -> session
    private final Map<Integer, Instant> sessionStartTimes = new ConcurrentHashMap<>();

    public GameSessionManager(DatabaseManager dbManager, GameRoomManager roomManager) {
        this.roomManager = roomManager;
        this.matchHistoryDAO = new MatchHistoryDAO(dbManager);
        this.userDAO = new UserDAO(dbManager);
    }

    /**
     * Tạo session mới cho room dựa trên GameType.
     * Nếu đã tồn tại session cho roomId thì trả về session cũ.
     */
    public synchronized GameSession startSessionIfAbsent(int roomId, GameType gameType) {
        GameSession existing = sessions.get(roomId);
        if (existing != null) {
            return existing;
        }

        GameRoom room = roomManager.getRoom(roomId);
        if (room == null) {
            logger.warn("Không thể start session: room {} không tồn tại trong memory", roomId);
            return null;
        }

        // Lấy danh sách playerIds theo thứ tự chỗ ngồi (position tăng dần)
        List<Map.Entry<Integer, Integer>> sortedEntries = new ArrayList<>(room.getPlayerPositions().entrySet());
        sortedEntries.sort(Comparator.comparingInt(Map.Entry::getValue));
        List<Integer> playerIds = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : sortedEntries) {
            playerIds.add(entry.getKey());
        }

        GameSession session;
        switch (gameType) {
            case POKER:
                session = new PokerGameSession(roomId, gameType, room, playerIds);
                break;
            case TIENLEN:
                session = new TienLenGameSession(roomId, gameType, room, playerIds);
                break;
            default:
                logger.warn("GameType {} chưa được hỗ trợ cho room {}", gameType, roomId);
                return null;
        }

        sessions.put(roomId, session);
        sessionStartTimes.put(roomId, Instant.now());
        room.setStatus("PLAYING");

        // Broadcast GameStartPacket + snapshot ban đầu
        GameStartPacket startPacket = new GameStartPacket();
        startPacket.setRoomId(roomId);
        startPacket.setGameType(gameType);
        startPacket.setPlayerOrder(playerIds);
        startPacket.setInitialState(session.buildGameStatePacket().getGameState());
        room.broadcast(startPacket);

        logger.info("Đã start session {} cho room {} ({})", session.getClass().getSimpleName(), roomId, gameType);
        return session;
    }

    /**
     * Xử lý PlayerActionPacket gửi từ client.
     * Tự động tạo session nếu chưa có (lazy start ván chơi).
     */
    public void handlePlayerAction(PlayerActionPacket packet) {
        int roomId = packet.getRoomId();
        GameType gameType = packet.getGameType();

        GameSession session = sessions.get(roomId);
        if (session == null) {
            session = startSessionIfAbsent(roomId, gameType);
            if (session == null) {
                logger.warn("Không thể xử lý PlayerActionPacket: không có session cho room {}", roomId);
                return;
            }
        }

        session.handlePlayerAction(packet);

        GameRoom room = roomManager.getRoom(roomId);
        if (room == null) {
            logger.warn("Room {} không tồn tại khi broadcast game state", roomId);
            return;
        }

        // Broadcast snapshot state sau action
        GameStatePacket statePacket = session.buildGameStatePacket();
        room.broadcast(statePacket);

        // Nếu ván đã kết thúc, finalize
        if (session.isFinished()) {
            finalizeSession(room, session);
        }
    }

    /**
     * Kết thúc ván: tính toán kết quả đơn giản, cập nhật DB và gửi GameEndPacket.
     */
    private void finalizeSession(GameRoom room, GameSession session) {
        int roomId = room.getRoomId();
        GameType gameType = session.getGameType();
        int winnerId = session.getWinnerId();
        if (winnerId <= 0) {
            logger.warn("Session cho room {} đánh dấu finished nhưng không có winnerId hợp lệ", roomId);
        }

        List<Integer> playerIds = new ArrayList<>(room.getPlayerPositions().keySet());
        List<Long> creditChanges = new ArrayList<>();

        // Chính sách thưởng/phạt rất đơn giản: winner +1000, mỗi người thua -500.
        for (int playerId : playerIds) {
            long delta = (playerId == winnerId) ? 1_000L : -500L;
            creditChanges.add(delta);
            try {
                userDAO.updateCredits(playerId, delta);
                // Note: Stats (wins/losses) can be calculated from match_history if needed

                String result = (playerId == winnerId) ? "WIN" : "LOSE";
                int opponentCount = playerIds.size() - 1;
                int durationSeconds = calculateDurationSeconds(roomId);

                matchHistoryDAO.saveMatch(
                        playerId,
                        gameType.name(),
                        MatchMode.MULTIPLAYER.name(),
                        result,
                        delta,
                        opponentCount,
                        durationSeconds
                );
            } catch (SQLException e) {
                logger.error("Lỗi khi cập nhật kết quả ván cho user {}: {}", playerId, e.getMessage(), e);
            }
        }

        GameEndPacket endPacket = new GameEndPacket();
        endPacket.setRoomId(roomId);
        endPacket.setGameType(gameType);
        endPacket.setWinnerId(winnerId);
        endPacket.setPlayerIds(playerIds);
        endPacket.setCreditChanges(creditChanges);

        room.setStatus("FINISHED");
        room.broadcast(endPacket);

        sessions.remove(roomId);
        sessionStartTimes.remove(roomId);

        logger.info("Đã kết thúc ván {} ở room {}, winner: {}", gameType, roomId, winnerId);
    }

    private int calculateDurationSeconds(int roomId) {
        Instant start = sessionStartTimes.get(roomId);
        if (start == null) {
            return 0;
        }
        return (int) Duration.between(start, Instant.now()).getSeconds();
    }
}

