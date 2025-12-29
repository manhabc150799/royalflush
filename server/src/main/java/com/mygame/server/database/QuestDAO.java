package com.mygame.server.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Daily Quest System
 */
public class QuestDAO {
    private static final Logger logger = LoggerFactory.getLogger(QuestDAO.class);
    private final DatabaseManager dbManager;
    private static final long QUEST_REWARD = 20000; // Each quest gives 20,000 credits
    
    public QuestDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    /**
     * Get user's daily quests for today
     * Auto-assigns quests if user doesn't have any for today
     * Auto-completes login quest if user logged in today
     */
    public List<QuestProgress> getUserDailyQuests(int userId) throws SQLException {
        LocalDate today = LocalDate.now();
        
        // Check if user has quests for today
        List<QuestProgress> quests = getQuestsForDate(userId, today);
        
        if (quests.isEmpty()) {
            // Assign today's quests
            assignDailyQuests(userId, today);
            quests = getQuestsForDate(userId, today);
            
            // If still empty after assignment, check if config table has quests
            if (quests.isEmpty()) {
                logger.error("No quests assigned to user {} even after assignment attempt. Check daily_quest_config table.", userId);
                throw new SQLException("No quests available in daily_quest_config");
            }
        }
        
        // Auto-complete login quest if user logged in today
        try {
            autoCompleteLoginQuest(userId, today, quests);
        } catch (SQLException e) {
            try {
                dbManager.getConnection().rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Lỗi rollback: {}", rollbackEx.getMessage());
            }
            logger.warn("Failed to auto-complete login quest, continuing anyway: {}", e.getMessage());
        }
        
        return quests;
    }
    
    /**
     * Auto-complete login quest if user logged in today
     */
    private void autoCompleteLoginQuest(int userId, LocalDate today, List<QuestProgress> quests) throws SQLException {
        if (quests.isEmpty()) {
            return;
        }
        
        // Check if user logged in today
        String checkLoginSql = "SELECT last_login FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(checkLoginSql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Timestamp lastLogin = rs.getTimestamp("last_login");
                if (lastLogin != null) {
                    LocalDate loginDate = lastLogin.toLocalDateTime().toLocalDate();
                    if (loginDate.equals(today)) {
                        // User logged in today, find and complete login quest
                        for (QuestProgress quest : quests) {
                            if (quest.getDescription() != null && 
                                quest.getDescription().toLowerCase().contains("login") &&
                                !quest.isClaimed() && 
                                quest.getCurrentProgress() < quest.getTargetCount()) {
                                
                                // Update progress to target count
                                String updateSql = "UPDATE user_quest_progress " +
                                    "SET current_progress = target_count " +
                                    "FROM daily_quest_config " +
                                    "WHERE user_quest_progress.quest_id = daily_quest_config.quest_id " +
                                    "AND user_quest_progress.user_id = ? " +
                                    "AND user_quest_progress.quest_id = ? " +
                                    "AND user_quest_progress.date_assigned = ?";
                                
                                try (PreparedStatement updateStmt = dbManager.getConnection().prepareStatement(updateSql)) {
                                    updateStmt.setInt(1, userId);
                                    updateStmt.setInt(2, quest.getQuestId());
                                    updateStmt.setDate(3, Date.valueOf(today));
                                    int updated = updateStmt.executeUpdate();
                                    dbManager.getConnection().commit();
                                    
                                    if (updated > 0) {
                                        // Update in-memory quest
                                        quest.setCurrentProgress(quest.getTargetCount());
                                        logger.info("Auto-completed login quest for user {}", userId);
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            try {
                dbManager.getConnection().rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Lỗi rollback: {}", rollbackEx.getMessage());
            }
            // Don't throw - just log and continue
            logger.warn("Failed to auto-complete login quest for user {}: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Get quests for specific date
     */
    private List<QuestProgress> getQuestsForDate(int userId, LocalDate date) throws SQLException {
        String sql = "SELECT uqp.quest_id, dqc.description, dqc.game_type, " +
                     "dqc.target_count, dqc.reward_credits, uqp.current_progress, uqp.is_claimed " +
                     "FROM user_quest_progress uqp " +
                     "JOIN daily_quest_config dqc ON uqp.quest_id = dqc.quest_id " +
                     "WHERE uqp.user_id = ? AND uqp.date_assigned = ? " +
                     "ORDER BY uqp.quest_id";
        
        List<QuestProgress> quests = new ArrayList<>();
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setDate(2, Date.valueOf(date));
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                QuestProgress quest = new QuestProgress();
                quest.setQuestId(rs.getInt("quest_id"));
                quest.setDescription(rs.getString("description"));
                quest.setGameType(rs.getString("game_type"));
                quest.setTargetCount(rs.getInt("target_count"));
                quest.setRewardCredits(rs.getLong("reward_credits"));
                quest.setCurrentProgress(rs.getInt("current_progress"));
                quest.setClaimed(rs.getBoolean("is_claimed"));
                
                quests.add(quest);
            }
            
            logger.debug("Retrieved {} quests for user {} on date {}", quests.size(), userId, date);
        } catch (SQLException e) {
            try {
                dbManager.getConnection().rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Lỗi rollback: {}", rollbackEx.getMessage());
            }
            logger.error("SQL error getting quests for date: {}", e.getMessage(), e);
            throw e;
        }
        
        return quests;
    }
    
    /**
     * Assign daily quests to user for specific date
     * Assigns all 5 quests from config
     */
    private void assignDailyQuests(int userId, LocalDate date) throws SQLException {
        // First check if config has any quests
        String checkConfigSql = "SELECT COUNT(*) FROM daily_quest_config";
        int questCount = 0;
        try (PreparedStatement checkStmt = dbManager.getConnection().prepareStatement(checkConfigSql)) {
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                questCount = rs.getInt(1);
                if (questCount == 0) {
                    logger.error("daily_quest_config table is empty! Cannot assign quests.");
                    throw new SQLException("No quests configured in daily_quest_config table. Please run fix_quests.sql");
                }
                logger.info("Found {} quests in daily_quest_config", questCount);
            }
        } catch (SQLException e) {
            try {
                dbManager.getConnection().rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Lỗi rollback: {}", rollbackEx.getMessage());
            }
            logger.error("Error checking daily_quest_config: {}", e.getMessage(), e);
            throw e;
        }
        
        // Delete any existing quests for today (in case of retry)
        String deleteSql = "DELETE FROM user_quest_progress WHERE user_id = ? AND date_assigned = ?";
        try (PreparedStatement deleteStmt = dbManager.getConnection().prepareStatement(deleteSql)) {
            deleteStmt.setInt(1, userId);
            deleteStmt.setDate(2, Date.valueOf(date));
            deleteStmt.executeUpdate();
        }
        
        String sql = "INSERT INTO user_quest_progress (user_id, quest_id, current_progress, is_claimed, date_assigned) " +
                     "SELECT ?, quest_id, 0, FALSE, ? " +
                     "FROM daily_quest_config " +
                     "ORDER BY quest_id";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setDate(2, Date.valueOf(date));
            
            int count = pstmt.executeUpdate();
            dbManager.getConnection().commit();
            
            logger.info("Assigned {} daily quests to user {} (expected: {})", count, userId, questCount);
            
            if (count == 0) {
                throw new SQLException("No quests were assigned. Check daily_quest_config table has data.");
            }
            
            if (count != questCount) {
                logger.warn("Assigned {} quests but expected {}. Some quests may be missing.", count, questCount);
            }
        } catch (SQLException e) {
            dbManager.getConnection().rollback();
            logger.error("Failed to assign daily quests to user {}: {}", userId, e.getMessage(), e);
            logger.error("This usually means daily_quest_config table is empty. Run fix_quests.sql script.");
            throw e;
        }
    }
    
    /**
     * Update quest progress
     */
    public void updateQuestProgress(int userId, String gameType, String eventType, int amount) throws SQLException {
        LocalDate today = LocalDate.now();
        
        // Update matching quests
        String sql = "UPDATE user_quest_progress " +
                     "SET current_progress = LEAST(current_progress + ?, " +
                     "    (SELECT target_count FROM daily_quest_config WHERE quest_id = user_quest_progress.quest_id)) " +
                     "WHERE user_id = ? AND date_assigned = ? AND is_claimed = FALSE " +
                     "AND quest_id IN (" +
                     "    SELECT quest_id FROM daily_quest_config " +
                     "    WHERE game_type = ? OR game_type = 'ANY'" +
                     ")";
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, amount);
            pstmt.setInt(2, userId);
            pstmt.setDate(3, Date.valueOf(today));
            pstmt.setString(4, gameType);
            
            int updated = pstmt.executeUpdate();
            dbManager.getConnection().commit();
            
            if (updated > 0) {
                logger.debug("Updated {} quests for user {} ({} event)", updated, userId, eventType);
            }
        } catch (SQLException e) {
            dbManager.getConnection().rollback();
            logger.error("Failed to update quest progress: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Claim quest reward
     */
    public boolean claimQuestReward(int userId, int questId) throws SQLException {
        LocalDate today = LocalDate.now();
        
        // Check if quest is completed and not claimed
        String checkSql = "SELECT uqp.current_progress, dqc.target_count, uqp.is_claimed " +
                          "FROM user_quest_progress uqp " +
                          "JOIN daily_quest_config dqc ON uqp.quest_id = dqc.quest_id " +
                          "WHERE uqp.user_id = ? AND uqp.quest_id = ? AND uqp.date_assigned = ?";
        
        try (PreparedStatement checkStmt = dbManager.getConnection().prepareStatement(checkSql)) {
            checkStmt.setInt(1, userId);
            checkStmt.setInt(2, questId);
            checkStmt.setDate(3, Date.valueOf(today));
            
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                int progress = rs.getInt("current_progress");
                int target = rs.getInt("target_count");
                boolean claimed = rs.getBoolean("is_claimed");
                
                if (claimed) {
                    logger.warn("Quest {} already claimed by user {}", questId, userId);
                    return false;
                }
                
                if (progress < target) {
                    logger.warn("Quest {} not completed by user {} ({}/{})", questId, userId, progress, target);
                    return false;
                }
                
                // Mark as claimed
                String claimSql = "UPDATE user_quest_progress SET is_claimed = TRUE " +
                                  "WHERE user_id = ? AND quest_id = ? AND date_assigned = ?";
                
                try (PreparedStatement claimStmt = dbManager.getConnection().prepareStatement(claimSql)) {
                    claimStmt.setInt(1, userId);
                    claimStmt.setInt(2, questId);
                    claimStmt.setDate(3, Date.valueOf(today));
                    claimStmt.executeUpdate();
                }
                
                // Add credits (always 20,000)
                String creditSql = "UPDATE users SET credits = credits + ? WHERE user_id = ?";
                try (PreparedStatement creditStmt = dbManager.getConnection().prepareStatement(creditSql)) {
                    creditStmt.setLong(1, QUEST_REWARD);
                    creditStmt.setInt(2, userId);
                    creditStmt.executeUpdate();
                }
                
                dbManager.getConnection().commit();
                logger.info("User {} claimed quest {} reward: {} credits", userId, questId, QUEST_REWARD);
                return true;
            }
        } catch (SQLException e) {
            dbManager.getConnection().rollback();
            logger.error("Failed to claim quest reward: {}", e.getMessage(), e);
            throw e;
        }
        
        return false;
    }
    
    /**
     * Quest progress data class
     */
    public static class QuestProgress {
        private int questId;
        private String description;
        private String gameType;
        private int targetCount;
        private long rewardCredits;
        private int currentProgress;
        private boolean isClaimed;
        
        // Getters and Setters
        public int getQuestId() { return questId; }
        public void setQuestId(int questId) { this.questId = questId; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getGameType() { return gameType; }
        public void setGameType(String gameType) { this.gameType = gameType; }
        
        public int getTargetCount() { return targetCount; }
        public void setTargetCount(int targetCount) { this.targetCount = targetCount; }
        
        public long getRewardCredits() { return rewardCredits; }
        public void setRewardCredits(long rewardCredits) { this.rewardCredits = rewardCredits; }
        
        public int getCurrentProgress() { return currentProgress; }
        public void setCurrentProgress(int currentProgress) { this.currentProgress = currentProgress; }
        
        public boolean isClaimed() { return isClaimed; }
        public void setClaimed(boolean claimed) { isClaimed = claimed; }
        
        public boolean isCompleted() {
            return currentProgress >= targetCount;
        }
    }
}

