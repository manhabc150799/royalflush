package com.mygame.server.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho bảng match_history
 */
public class MatchHistoryDAO {
    private static final Logger logger = LoggerFactory.getLogger(MatchHistoryDAO.class);
    private final DatabaseManager dbManager;
    
    public MatchHistoryDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    /**
     * Lưu lịch sử trận đấu
     */
    public void saveMatch(int userId, String gameType, String matchMode, 
                         String result, long creditsChange, int opponentCount, 
                         int durationSeconds) throws SQLException {
        String sql = "INSERT INTO match_history " +
                     "(user_id, game_type, match_mode, result, credits_change, opponent_count, duration_seconds, timestamp) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, gameType);
            pstmt.setString(3, matchMode);
            pstmt.setString(4, result);
            pstmt.setLong(5, creditsChange);
            pstmt.setInt(6, opponentCount);
            pstmt.setInt(7, durationSeconds);
            
            pstmt.executeUpdate();
            dbManager.getConnection().commit();
            logger.info("Đã lưu lịch sử trận đấu cho user {}: {} - {}", userId, gameType, result);
        } catch (SQLException e) {
            dbManager.getConnection().rollback();
            logger.error("Lỗi khi lưu lịch sử: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Lấy lịch sử trận đấu gần nhất của user
     */
    public List<MatchHistoryEntry> getRecentMatches(int userId, int limit) throws SQLException {
        String sql = "SELECT id, game_type, match_mode, result, credits_change, opponent_count, " +
                     "duration_seconds, timestamp " +
                     "FROM match_history " +
                     "WHERE user_id = ? " +
                     "ORDER BY timestamp DESC " +
                     "LIMIT ?";
        
        List<MatchHistoryEntry> matches = new ArrayList<>();
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                MatchHistoryEntry entry = new MatchHistoryEntry();
                entry.setId(rs.getInt("id"));
                entry.setGameType(rs.getString("game_type"));
                entry.setMatchMode(rs.getString("match_mode"));
                entry.setResult(rs.getString("result"));
                entry.setCreditsChange(rs.getLong("credits_change"));
                entry.setOpponentCount(rs.getInt("opponent_count"));
                entry.setDurationSeconds(rs.getInt("duration_seconds"));
                
                Timestamp timestamp = rs.getTimestamp("timestamp");
                if (timestamp != null) {
                    entry.setTimestamp(LocalDateTime.ofInstant(
                        timestamp.toInstant(), ZoneId.systemDefault()));
                }
                
                matches.add(entry);
            }
        }
        
        return matches;
    }
    
    /**
     * Inner class để chứa match history data
     */
    public static class MatchHistoryEntry {
        private int id;
        private String gameType;
        private String matchMode;
        private String result;
        private long creditsChange;
        private int opponentCount;
        private int durationSeconds;
        private LocalDateTime timestamp;
        
        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getGameType() { return gameType; }
        public void setGameType(String gameType) { this.gameType = gameType; }
        
        public String getMatchMode() { return matchMode; }
        public void setMatchMode(String matchMode) { this.matchMode = matchMode; }
        
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        
        public long getCreditsChange() { return creditsChange; }
        public void setCreditsChange(long creditsChange) { this.creditsChange = creditsChange; }
        
        public int getOpponentCount() { return opponentCount; }
        public void setOpponentCount(int opponentCount) { this.opponentCount = opponentCount; }
        
        public int getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}

