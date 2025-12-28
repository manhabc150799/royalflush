package com.mygame.server.database;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Data Access Object cho bảng users
 */
public class UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);
    private final DatabaseManager dbManager;
    
    public UserDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    /**
     * Đăng ký user mới
     */
    public UserProfile register(String username, String password) throws SQLException {
        // Kiểm tra username đã tồn tại chưa
        if (usernameExists(username)) {
            throw new SQLException("Account already exists");
        }
        
        // Hash password
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        
        String sql = "INSERT INTO users (username, password_hash, credits, created_at) " +
                     "VALUES (?, ?, 1000, CURRENT_TIMESTAMP) RETURNING user_id";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt("user_id");
                dbManager.getConnection().commit();
                logger.info("Đã đăng ký user mới: {} (ID: {})", username, userId);
                return getUserProfile(userId);
            }
        } catch (SQLException e) {
            dbManager.getConnection().rollback();
            logger.error("Lỗi khi đăng ký user: {}", e.getMessage(), e);
            throw e;
        }
        
        throw new SQLException("Không thể tạo user");
    }
    
    /**
     * Đăng nhập user
     */
    public UserProfile login(String username, String password) throws SQLException {
        String sql = "SELECT user_id, username, password_hash, credits " +
                     "FROM users WHERE username = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                
                // Verify password
                if (BCrypt.checkpw(password, storedHash)) {
                    int userId = rs.getInt("user_id");
                    
                    // Update last_login
                    updateLastLogin(userId);
                    
                    dbManager.getConnection().commit();
                    logger.info("User đăng nhập thành công: {}", username);
                    return getUserProfile(userId);
                } else {
                    logger.warn("Incorrect password for user: {}", username);
                    throw new SQLException("Incorrect username or password");
                }
            } else {
                logger.warn("User not found: {}", username);
                throw new SQLException("Incorrect username or password");
            }
        } catch (SQLException e) {
            dbManager.getConnection().rollback();
            throw e;
        }
    }
    
    /**
     * Lấy thông tin user profile
     */
    public UserProfile getUserProfile(int userId) throws SQLException {
        String sql = "SELECT user_id, username, credits, created_at, last_login " +
                     "FROM users WHERE user_id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                UserProfile profile = new UserProfile();
                profile.setId(rs.getInt("user_id"));
                profile.setUsername(rs.getString("username"));
                profile.setCredits(rs.getLong("credits"));
                // Calculate rank from credits
                profile.setRank(calculateRank(rs.getLong("credits")));
                // Set defaults for wins/losses (can be calculated from match_history if needed)
                profile.setTotalWins(0);
                profile.setTotalLosses(0);
                
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    profile.setCreatedAt(LocalDateTime.ofInstant(
                        createdAt.toInstant(), ZoneId.systemDefault()));
                }
                
                Timestamp lastLogin = rs.getTimestamp("last_login");
                if (lastLogin != null) {
                    profile.setLastLogin(LocalDateTime.ofInstant(
                        lastLogin.toInstant(), ZoneId.systemDefault()));
                }
                
                return profile;
            }
        }
        
        throw new SQLException("User not found");
    }
    
    /**
     * Cập nhật credits
     */
    public void updateCredits(int userId, long amount) throws SQLException {
        String sql = "UPDATE users SET credits = credits + ? WHERE user_id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, amount);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
            
            dbManager.getConnection().commit();
            logger.debug("Đã cập nhật credits cho user {}: {}", userId, amount);
        } catch (SQLException e) {
            dbManager.getConnection().rollback();
            logger.error("Lỗi khi cập nhật credits: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Tính rank dựa trên credits (logic-based, no database column)
     */
    public String calculateRank(long credits) {
        if (credits >= 8_000_001) return "MASTER";
        if (credits >= 5_000_001) return "PLATINUM";
        if (credits >= 2_000_001) return "GOLD";
        if (credits >= 500_001) return "SILVER";
        if (credits >= 100_001) return "BRONZE";
        return "IRON";
    }
    
    /**
     * Kiểm tra username đã tồn tại chưa
     */
    private boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }
    
    /**
     * Cập nhật last_login
     */
    private void updateLastLogin(int userId) throws SQLException {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Inner class để chứa user profile data
     */
    public static class UserProfile {
        private int id;
        private String username;
        private long credits;
        private String rank;
        private int totalWins;
        private int totalLosses;
        private LocalDateTime createdAt;
        private LocalDateTime lastLogin;
        
        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public long getCredits() { return credits; }
        public void setCredits(long credits) { this.credits = credits; }
        
        public String getRank() { return rank; }
        public void setRank(String rank) { this.rank = rank; }
        
        public int getTotalWins() { return totalWins; }
        public void setTotalWins(int totalWins) { this.totalWins = totalWins; }
        
        public int getTotalLosses() { return totalLosses; }
        public void setTotalLosses(int totalLosses) { this.totalLosses = totalLosses; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public LocalDateTime getLastLogin() { return lastLogin; }
        public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    }
}

