package com.mygame.server.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Data Access Object cho daily reward system
 */
public class DailyRewardDAO {
    private static final Logger logger = LoggerFactory.getLogger(DailyRewardDAO.class);
    private final DatabaseManager dbManager;
    
    // Số credits nhận được mỗi ngày (random từ 1000-5000)
    private static final long MIN_DAILY_REWARD = 1000;
    private static final long MAX_DAILY_REWARD = 5000;
    
    public DailyRewardDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    /**
     * Kiểm tra user có thể nhận daily reward không (24h từ lần cuối)
     */
    public boolean canClaimDailyReward(int userId) throws SQLException {
        String sql = "SELECT last_daily_reward FROM users WHERE user_id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Timestamp lastReward = rs.getTimestamp("last_daily_reward");
                
                if (lastReward == null) {
                    // Chưa nhận lần nào
                    return true;
                }
                
                // Kiểm tra đã qua 24h chưa
                LocalDateTime lastRewardTime = LocalDateTime.ofInstant(
                    lastReward.toInstant(), ZoneId.systemDefault());
                LocalDateTime now = LocalDateTime.now();
                
                // Nếu lastRewardTime + 24h < now thì có thể nhận
                return lastRewardTime.plusHours(24).isBefore(now);
            }
        }
        
        return false;
    }
    
    /**
     * Nhận daily reward và cập nhật credits
     */
    public long claimDailyReward(int userId) throws SQLException {
        if (!canClaimDailyReward(userId)) {
            throw new SQLException("Chưa đến thời gian nhận daily reward");
        }
        
        // Random credits từ MIN đến MAX
        long creditsReward = MIN_DAILY_REWARD + 
            (long)(Math.random() * (MAX_DAILY_REWARD - MIN_DAILY_REWARD + 1));
        
        String sql = "UPDATE users SET credits = credits + ?, last_daily_reward = CURRENT_TIMESTAMP WHERE user_id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, creditsReward);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
            
            // Tính lại rank nếu cần
            UserDAO userDAO = new UserDAO(dbManager);
            userDAO.updateCredits(userId, 0); // Chỉ để trigger update rank
            
            dbManager.getConnection().commit();
            logger.info("User {} đã nhận daily reward: {} credits", userId, creditsReward);
            return creditsReward;
        } catch (SQLException e) {
            dbManager.getConnection().rollback();
            logger.error("Lỗi khi nhận daily reward: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Lấy thời gian có thể nhận reward tiếp theo
     */
    public LocalDateTime getNextRewardTime(int userId) throws SQLException {
        String sql = "SELECT last_daily_reward FROM users WHERE user_id = ?";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Timestamp lastReward = rs.getTimestamp("last_daily_reward");
                
                if (lastReward == null) {
                    // Có thể nhận ngay
                    return LocalDateTime.now();
                }
                
                LocalDateTime lastRewardTime = LocalDateTime.ofInstant(
                    lastReward.toInstant(), ZoneId.systemDefault());
                
                // Thời gian nhận tiếp theo = lastRewardTime + 24h
                return lastRewardTime.plusHours(24);
            }
        }
        
        return LocalDateTime.now();
    }
}

