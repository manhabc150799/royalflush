package com.mygame.server.room;

import com.esotericsoftware.kryonet.Connection;
import com.mygame.server.database.DatabaseManager;
import com.mygame.server.database.RoomDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manager quản lý game rooms trong memory
 * Kết hợp với RoomDAO để đồng bộ với database
 * Includes periodic cleanup for empty rooms
 */
public class GameRoomManager {
    private static final Logger logger = LoggerFactory.getLogger(GameRoomManager.class);

    private final RoomDAO roomDAO;
    private final Map<Integer, GameRoom> activeRooms = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> userToRoom = new ConcurrentHashMap<>(); // userId -> roomId
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();

    public GameRoomManager(DatabaseManager dbManager) {
        this.roomDAO = new RoomDAO(dbManager);

        // Start periodic cleanup for empty rooms (every 30 seconds)
        cleanupScheduler.scheduleAtFixedRate(this::cleanupEmptyRooms, 30, 30, TimeUnit.SECONDS);
        logger.info("Started empty room cleanup scheduler (every 30 seconds)");
    }

    /**
     * Tạo room mới
     */
    public GameRoom createRoom(String roomName, String gameType, int hostUserId, int maxPlayers,
            Connection hostConnection)
            throws SQLException {
        // IMPORTANT: Leave any existing room first to prevent duplicate players
        Integer existingRoomId = userToRoom.get(hostUserId);
        if (existingRoomId != null) {
            logger.info("User {} is already in room {}, leaving before creating new room", hostUserId, existingRoomId);
            leaveRoom(existingRoomId, hostUserId);
        }

        // Tạo trong database
        RoomDAO.RoomData roomData = roomDAO.createRoom(roomName, gameType, hostUserId, maxPlayers);

        // Tạo trong memory
        GameRoom room = new GameRoom(roomData.getRoomId(), roomName, gameType, hostUserId, maxPlayers);
        room.addPlayer(hostUserId, hostConnection, 0);

        activeRooms.put(room.getRoomId(), room);
        userToRoom.put(hostUserId, room.getRoomId());

        logger.info("Đã tạo room: {} (ID: {})", roomName, room.getRoomId());
        return room;
    }

    /**
     * Tham gia room
     */
    public GameRoom joinRoom(int roomId, int userId, Connection connection) throws SQLException {
        // IMPORTANT: Leave any existing room first to prevent duplicate players
        Integer existingRoomId = userToRoom.get(userId);
        if (existingRoomId != null && existingRoomId != roomId) {
            logger.info("User {} is already in room {}, leaving before joining room {}", userId, existingRoomId,
                    roomId);
            leaveRoom(existingRoomId, userId);
        }

        GameRoom room = activeRooms.get(roomId);

        if (room == null) {
            // Load từ database nếu chưa có trong memory
            RoomDAO.RoomData roomData = roomDAO.getRoomData(roomId);
            room = new GameRoom(roomData);
            activeRooms.put(roomId, room);
        }

        // Kiểm tra room có đầy không
        if (room.getCurrentPlayers() >= room.getMaxPlayers()) {
            throw new SQLException("Room đã đầy");
        }

        // Kiểm tra status
        if (!"WAITING".equals(room.getStatus())) {
            throw new SQLException("Room không còn chờ players");
        }

        // Thêm vào database
        int position = room.getCurrentPlayers();
        roomDAO.addPlayerToRoom(roomId, userId, position);

        // Thêm vào memory
        room.addPlayer(userId, connection, position);
        userToRoom.put(userId, roomId);

        logger.info("Player {} đã tham gia room {}", userId, roomId);
        return room;
    }

    /**
     * Rời room
     */
    public void leaveRoom(int roomId, int userId) throws SQLException {
        GameRoom room = activeRooms.get(roomId);

        if (room == null) {
            logger.warn("Room {} không tồn tại trong memory", roomId);
            return;
        }

        boolean wasHost = (room.getHostUserId() == userId);

        // Xóa khỏi database
        roomDAO.removePlayerFromRoom(roomId, userId);

        // Xóa khỏi memory
        room.removePlayer(userId);
        userToRoom.remove(userId);

        int playersAfterLeave = room.getCurrentPlayers();

        // Nếu room trống (không còn ai), xóa room
        if (playersAfterLeave == 0) {
            roomDAO.deleteRoom(roomId);
            activeRooms.remove(roomId);
            logger.info("Đã xóa room trống: {}", roomId);
        } else if (wasHost && playersAfterLeave > 0) {
            // Host rời nhưng còn players, chuyển host cho player đầu tiên
            int newHostId = room.getPlayers().keySet().iterator().next();
            room.setHostUserId(newHostId);
            // Update host trong database
            roomDAO.updateHost(roomId, newHostId);
            logger.info("Đã chuyển host của room {} từ {} sang {}", roomId, userId, newHostId);
        }

        logger.info("Player {} đã rời room {} (còn lại {} players)", userId, roomId, playersAfterLeave);
    }

    /**
     * Cập nhật status của room (ví dụ: khi game bắt đầu -> PLAYING)
     */
    public void updateRoomStatus(int roomId, String status) throws SQLException {
        GameRoom room = activeRooms.get(roomId);
        if (room != null) {
            room.setStatus(status);
        }
        roomDAO.updateRoomStatus(roomId, status);
        logger.info("Đã cập nhật status của room {} thành {}", roomId, status);
    }

    /**
     * Lấy room theo ID
     */
    public GameRoom getRoom(int roomId) {
        return activeRooms.get(roomId);
    }

    /**
     * Lấy room mà user đang tham gia
     */
    public GameRoom getRoomByUser(int userId) {
        Integer roomId = userToRoom.get(userId);
        if (roomId != null) {
            return activeRooms.get(roomId);
        }
        return null;
    }

    /**
     * Lấy danh sách rooms đang chờ
     */
    public java.util.List<RoomDAO.RoomData> getWaitingRooms(String gameType, int limit) throws SQLException {
        return roomDAO.getWaitingRooms(gameType, limit);
    }

    /**
     * Broadcast message đến tất cả players trong room
     */
    public void broadcastToRoom(int roomId, Object message) {
        GameRoom room = activeRooms.get(roomId);
        if (room != null) {
            room.broadcast(message);
        }
    }

    /**
     * Cleanup khi connection bị ngắt
     */
    public void handleDisconnection(int userId) {
        Integer roomId = userToRoom.get(userId);
        if (roomId != null) {
            try {
                leaveRoom(roomId, userId);
            } catch (SQLException e) {
                logger.error("Lỗi khi xử lý disconnect: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Periodically cleanup empty rooms from memory and database
     */
    private void cleanupEmptyRooms() {
        try {
            List<Integer> emptyRoomIds = new ArrayList<>();

            for (Map.Entry<Integer, GameRoom> entry : activeRooms.entrySet()) {
                if (entry.getValue().getCurrentPlayers() == 0) {
                    emptyRoomIds.add(entry.getKey());
                }
            }

            for (Integer roomId : emptyRoomIds) {
                try {
                    roomDAO.deleteRoom(roomId);
                    activeRooms.remove(roomId);
                    logger.info("Cleanup: Deleted empty room {}", roomId);
                } catch (SQLException e) {
                    logger.error("Failed to cleanup room {}: {}", roomId, e.getMessage());
                }
            }

            if (!emptyRoomIds.isEmpty()) {
                logger.info("Cleaned up {} empty rooms", emptyRoomIds.size());
            }
        } catch (Exception e) {
            logger.error("Error during empty room cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Shutdown cleanup scheduler
     */
    public void shutdown() {
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
        }
        logger.info("GameRoomManager shutdown complete");
    }
}
