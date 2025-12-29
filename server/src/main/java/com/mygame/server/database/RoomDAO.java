package com.mygame.server.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho bảng game_rooms và room_players
 */
public class RoomDAO {
    private static final Logger logger = LoggerFactory.getLogger(RoomDAO.class);
    private final DatabaseManager dbManager;
    
    public RoomDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    /**
     * Tạo room mới
     */
    public RoomData createRoom(String roomName, String gameType, int hostUserId, int maxPlayers) throws SQLException {
        String sql = "INSERT INTO game_rooms (room_name, game_type, host_user_id, max_players, current_players, status) " +
                     "VALUES (?, ?, ?, ?, 1, 'WAITING') RETURNING id";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, roomName);
            pstmt.setString(2, gameType);
            pstmt.setInt(3, hostUserId);
            pstmt.setInt(4, maxPlayers);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int roomId = rs.getInt("id");
                
                // Thêm host vào room_players
                addPlayerToRoom(roomId, hostUserId, 0);
                
                dbManager.getConnection().commit();
                logger.info("Đã tạo room: {} (ID: {})", roomName, roomId);
                return getRoomData(roomId);
            }
        } catch (SQLException e) {
            dbManager.getConnection().rollback();
            logger.error("Lỗi khi tạo room: {}", e.getMessage(), e);
            throw e;
        }
        
        throw new SQLException("Không thể tạo room");
    }
    
    /**
     * Thêm player vào room
     */
    public void addPlayerToRoom(int roomId, int userId, int position) throws SQLException {
        String sql = "INSERT INTO room_players (room_id, user_id, position) VALUES (?, ?, ?) " +
                     "ON CONFLICT (room_id, user_id) DO NOTHING";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, position);
            pstmt.executeUpdate();
            
            // Update current_players count
            updatePlayerCount(roomId);
            
            dbManager.getConnection().commit();
            logger.debug("Đã thêm player {} vào room {}", userId, roomId);
        } catch (SQLException e) {
            dbManager.getConnection().rollback();
            throw e;
        }
    }
    
    /**
     * Xóa player khỏi room
     */
    public void removePlayerFromRoom(int roomId, int userId) throws SQLException {
        String sql = "DELETE FROM room_players WHERE room_id = ? AND user_id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
            
            // Update current_players count
            updatePlayerCount(roomId);
            
            dbManager.getConnection().commit();
            logger.debug("Đã xóa player {} khỏi room {}", userId, roomId);
        } catch (SQLException e) {
            dbManager.getConnection().rollback();
            throw e;
        }
    }
    
    /**
     * Cập nhật số lượng players trong room
     */
    private void updatePlayerCount(int roomId) throws SQLException {
        String sql = "UPDATE game_rooms SET current_players = " +
                     "(SELECT COUNT(*) FROM room_players WHERE room_id = ?) " +
                     "WHERE id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            pstmt.setInt(2, roomId);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Lấy thông tin room
     */
    public RoomData getRoomData(int roomId) throws SQLException {
        String sql = "SELECT id, room_name, game_type, host_user_id, max_players, current_players, status, created_at " +
                     "FROM game_rooms WHERE id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                RoomData room = new RoomData();
                room.setRoomId(rs.getInt("id"));
                room.setRoomName(rs.getString("room_name"));
                room.setGameType(rs.getString("game_type"));
                room.setHostUserId(rs.getInt("host_user_id"));
                room.setMaxPlayers(rs.getInt("max_players"));
                room.setCurrentPlayers(rs.getInt("current_players"));
                room.setStatus(rs.getString("status"));
                
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    room.setCreatedAt(LocalDateTime.ofInstant(createdAt.toInstant(), ZoneId.systemDefault()));
                }
                
                // Lấy danh sách players
                room.setPlayers(getRoomPlayers(roomId));
                
                return room;
            }
        } catch (SQLException e) {
            try {
                dbManager.getConnection().rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Lỗi rollback: {}", rollbackEx.getMessage());
            }
            logger.error("Lỗi khi lấy room data: {}", e.getMessage(), e);
            throw e;
        }
        
        throw new SQLException("Room không tồn tại");
    }
    
    /**
     * Lấy danh sách players trong room
     */
    public List<RoomPlayerData> getRoomPlayers(int roomId) throws SQLException {
        String sql = "SELECT rp.user_id, rp.position, u.username " +
                     "FROM room_players rp " +
                     "JOIN users u ON rp.user_id = u.user_id " +
                     "WHERE rp.room_id = ? " +
                     "ORDER BY rp.position";
        
        List<RoomPlayerData> players = new ArrayList<>();
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                RoomPlayerData player = new RoomPlayerData();
                player.setUserId(rs.getInt("user_id"));
                player.setUsername(rs.getString("username"));
                player.setPosition(rs.getInt("position"));
                players.add(player);
            }
        } catch (SQLException e) {
            try {
                dbManager.getConnection().rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Lỗi rollback: {}", rollbackEx.getMessage());
            }
            logger.error("Lỗi khi lấy room players: {}", e.getMessage(), e);
            throw e;
        }
        
        return players;
    }
    
    /**
     * Lấy danh sách rooms đang chờ (WAITING)
     */
    public List<RoomData> getWaitingRooms(String gameType, int limit) throws SQLException {
        String sql = "SELECT id, room_name, game_type, host_user_id, max_players, current_players, status, created_at " +
                     "FROM game_rooms " +
                     "WHERE status = 'WAITING' " +
                     (gameType != null ? "AND game_type = ? " : "") +
                     "AND current_players < max_players " +
                     "ORDER BY created_at DESC " +
                     "LIMIT ?";
        
        List<RoomData> rooms = new ArrayList<>();
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            int paramIndex = 1;
            if (gameType != null) {
                pstmt.setString(paramIndex++, gameType);
            }
            pstmt.setInt(paramIndex, limit);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                RoomData room = new RoomData();
                room.setRoomId(rs.getInt("id"));
                room.setRoomName(rs.getString("room_name"));
                room.setGameType(rs.getString("game_type"));
                room.setHostUserId(rs.getInt("host_user_id"));
                room.setMaxPlayers(rs.getInt("max_players"));
                room.setCurrentPlayers(rs.getInt("current_players"));
                room.setStatus(rs.getString("status"));
                
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    room.setCreatedAt(LocalDateTime.ofInstant(createdAt.toInstant(), ZoneId.systemDefault()));
                }
                
                // Lấy host username
                UserDAO userDAO = new UserDAO(dbManager);
                try {
                    UserDAO.UserProfile host = userDAO.getUserProfile(room.getHostUserId());
                    room.setHostUsername(host.getUsername());
                } catch (SQLException e) {
                    try {
                        dbManager.getConnection().rollback();
                    } catch (SQLException rollbackEx) {
                        logger.error("Lỗi rollback: {}", rollbackEx.getMessage());
                    }
                    room.setHostUsername("Unknown");
                }
                
                rooms.add(room);
            }
        } catch (SQLException e) {
            try {
                dbManager.getConnection().rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Lỗi rollback: {}", rollbackEx.getMessage());
            }
            logger.error("Lỗi khi lấy waiting rooms: {}", e.getMessage(), e);
            throw e;
        }
        
        return rooms;
    }
    
    /**
     * Cập nhật host của room
     */
    public void updateHost(int roomId, int newHostId) throws SQLException {
        String sql = "UPDATE game_rooms SET host_user_id = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, newHostId);
            pstmt.setInt(2, roomId);
            pstmt.executeUpdate();
            dbManager.getConnection().commit();
            logger.info("Đã cập nhật host của room {} thành user {}", roomId, newHostId);
        } catch (SQLException e) {
            try {
                dbManager.getConnection().rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Lỗi rollback: {}", rollbackEx.getMessage());
            }
            logger.error("Lỗi khi cập nhật host: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Cập nhật status của room
     */
    public void updateRoomStatus(int roomId, String status) throws SQLException {
        String sql = "UPDATE game_rooms SET status = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, roomId);
            pstmt.executeUpdate();
            
            if ("PLAYING".equals(status)) {
                // Update started_at
                String updateStarted = "UPDATE game_rooms SET started_at = CURRENT_TIMESTAMP WHERE id = ?";
                try (PreparedStatement pstmt2 = dbManager.getConnection().prepareStatement(updateStarted)) {
                    pstmt2.setInt(1, roomId);
                    pstmt2.executeUpdate();
                }
            }
            
            dbManager.getConnection().commit();
        } catch (SQLException e) {
            dbManager.getConnection().rollback();
            throw e;
        }
    }
    
    /**
     * Xóa room (khi empty hoặc finished)
     */
    public void deleteRoom(int roomId) throws SQLException {
        // Xóa room_players trước (CASCADE sẽ tự động xóa)
        String deletePlayers = "DELETE FROM room_players WHERE room_id = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(deletePlayers)) {
            pstmt.setInt(1, roomId);
            pstmt.executeUpdate();
        }
        
        // Xóa room
        String deleteRoom = "DELETE FROM game_rooms WHERE id = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(deleteRoom)) {
            pstmt.setInt(1, roomId);
            pstmt.executeUpdate();
            dbManager.getConnection().commit();
            logger.info("Đã xóa room: {}", roomId);
        } catch (SQLException e) {
            dbManager.getConnection().rollback();
            throw e;
        }
    }
    
    /**
     * Inner class chứa room data
     */
    public static class RoomData {
        private int roomId;
        private String roomName;
        private String gameType;
        private int hostUserId;
        private String hostUsername;
        private int maxPlayers;
        private int currentPlayers;
        private String status;
        private LocalDateTime createdAt;
        private List<RoomPlayerData> players;
        
        // Getters and Setters
        public int getRoomId() { return roomId; }
        public void setRoomId(int roomId) { this.roomId = roomId; }
        
        public String getRoomName() { return roomName; }
        public void setRoomName(String roomName) { this.roomName = roomName; }
        
        public String getGameType() { return gameType; }
        public void setGameType(String gameType) { this.gameType = gameType; }
        
        public int getHostUserId() { return hostUserId; }
        public void setHostUserId(int hostUserId) { this.hostUserId = hostUserId; }
        
        public String getHostUsername() { return hostUsername; }
        public void setHostUsername(String hostUsername) { this.hostUsername = hostUsername; }
        
        public int getMaxPlayers() { return maxPlayers; }
        public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
        
        public int getCurrentPlayers() { return currentPlayers; }
        public void setCurrentPlayers(int currentPlayers) { this.currentPlayers = currentPlayers; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public List<RoomPlayerData> getPlayers() { return players; }
        public void setPlayers(List<RoomPlayerData> players) { this.players = players; }
    }
    
    /**
     * Inner class chứa room player data
     */
    public static class RoomPlayerData {
        private int userId;
        private String username;
        private int position;
        
        // Getters and Setters
        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public int getPosition() { return position; }
        public void setPosition(int position) { this.position = position; }
    }
}

