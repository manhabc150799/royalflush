package com.mygame.server.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho leaderboard
 */
public class LeaderboardDAO {
    private static final Logger logger = LoggerFactory.getLogger(LeaderboardDAO.class);
    private final DatabaseManager dbManager;
    
    public LeaderboardDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    /**
     * Lấy top players theo credits
     */
    public List<LeaderboardEntry> getTopPlayers(int limit) throws SQLException {
        String sql = "SELECT user_id, username, credits, current_rank, total_wins, total_losses " +
                     "FROM users " +
                     "ORDER BY credits DESC " +
                     "LIMIT ?";
        
        List<LeaderboardEntry> entries = new ArrayList<>();
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            int rank = 1;
            while (rs.next()) {
                LeaderboardEntry entry = new LeaderboardEntry();
                entry.setRank(rank++);
                entry.setUserId(rs.getInt("user_id"));
                entry.setUsername(rs.getString("username"));
                entry.setCredits(rs.getLong("credits"));
                entry.setRankEnum(rs.getString("current_rank"));
                entry.setTotalWins(rs.getInt("total_wins"));
                entry.setTotalLosses(rs.getInt("total_losses"));
                
                entries.add(entry);
            }
        } catch (SQLException e) {
            logger.error("Lỗi khi lấy leaderboard: {}", e.getMessage(), e);
            throw e;
        }
        
        logger.debug("Đã lấy {} players từ leaderboard", entries.size());
        return entries;
    }
    
    /**
     * Lấy rank của player trong leaderboard (vị trí xếp hạng)
     */
    public int getPlayerRank(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) + 1 as rank " +
                     "FROM users " +
                     "WHERE credits > (SELECT credits FROM users WHERE user_id = ?)";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("rank");
            }
        }
        
        return -1; // Không tìm thấy
    }
    
    /**
     * Inner class để chứa leaderboard entry data
     */
    public static class LeaderboardEntry {
        private int rank;
        private int userId;
        private String username;
        private long credits;
        private String rankEnum;
        private int totalWins;
        private int totalLosses;
        
        // Getters and Setters
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        
        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public long getCredits() { return credits; }
        public void setCredits(long credits) { this.credits = credits; }
        
        public String getRankEnum() { return rankEnum; }
        public void setRankEnum(String rankEnum) { this.rankEnum = rankEnum; }
        
        public int getTotalWins() { return totalWins; }
        public void setTotalWins(int totalWins) { this.totalWins = totalWins; }
        
        public int getTotalLosses() { return totalLosses; }
        public void setTotalLosses(int totalLosses) { this.totalLosses = totalLosses; }
    }
}

