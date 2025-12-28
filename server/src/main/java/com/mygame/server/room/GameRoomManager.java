package com.mygame.server.room;

import com.esotericsoftware.kryonet.Connection;
import com.mygame.server.database.DatabaseManager;
import com.mygame.server.database.RoomDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager quản lý game rooms trong memory
 * Kết hợp với RoomDAO để đồng bộ với database
 */
public class GameRoomManager {
    private static final Logger logger = LoggerFactory.getLogger(GameRoomManager.class);
    
    private final RoomDAO roomDAO;
    private final Map<Integer, GameRoom> activeRooms = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> userToRoom = new ConcurrentHashMap<>(); // userId -> roomId
    
    public GameRoomManager(DatabaseManager dbManager) {
        this.roomDAO = new RoomDAO(dbManager);
    }
    
    /**
     * Tạo room mới
     */
    public GameRoom createRoom(String roomName, String gameType, int hostUserId, int maxPlayers, Connection hostConnection) 
            throws SQLException {
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
        
        // Xóa khỏi database
        roomDAO.removePlayerFromRoom(roomId, userId);
        
        // Xóa khỏi memory
        room.removePlayer(userId);
        userToRoom.remove(userId);
        
        // Nếu room trống hoặc host rời, xóa room
        if (room.getCurrentPlayers() == 0 || room.getHostUserId() == userId) {
            if (room.getCurrentPlayers() > 0) {
                // Chuyển host cho player đầu tiên
                int newHostId = room.getPlayers().keySet().iterator().next();
                room.setHostUserId(newHostId);
                // Update trong database
                // TODO: Update host trong database
            } else {
                // Xóa room
                roomDAO.deleteRoom(roomId);
                activeRooms.remove(roomId);
                logger.info("Đã xóa room trống: {}", roomId);
            }
        }
        
        logger.info("Player {} đã rời room {}", userId, roomId);
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
}

