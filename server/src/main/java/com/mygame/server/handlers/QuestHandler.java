package com.mygame.server.handlers;

import com.esotericsoftware.kryonet.Connection;
import com.mygame.server.database.DatabaseManager;
import com.mygame.server.database.QuestDAO;
import com.mygame.server.database.UserDAO;
import com.mygame.shared.model.Quest;
import com.mygame.shared.network.packets.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handler for quest-related requests
 */
public class QuestHandler {
    private static final Logger logger = LoggerFactory.getLogger(QuestHandler.class);
    private final QuestDAO questDAO;
    private final UserDAO userDAO;
    private final Map<Connection, Integer> connectionToUser;
    
    public QuestHandler(DatabaseManager dbManager, Map<Connection, Integer> connectionToUser) {
        this.questDAO = new QuestDAO(dbManager);
        this.userDAO = new UserDAO(dbManager);
        this.connectionToUser = connectionToUser;
    }
    
    /**
     * Handle GetQuestsRequest
     */
    public void handleGetQuests(Connection connection, GetQuestsRequest request) {
        Integer userId = connectionToUser.get(connection);
        
        if (userId == null) {
            logger.warn("GetQuestsRequest from unauthenticated connection {}", connection.getID());
            GetQuestsResponse response = new GetQuestsResponse();
            response.success = false;
            response.errorMessage = "Not authenticated";
            connection.sendTCP(response);
            return;
        }
        
        try {
            logger.info("Getting daily quests for user {}", userId);
            List<QuestDAO.QuestProgress> dbQuests = questDAO.getUserDailyQuests(userId);
            
            if (dbQuests == null || dbQuests.isEmpty()) {
                logger.warn("No quests returned for user {}", userId);
                GetQuestsResponse response = new GetQuestsResponse();
                response.success = false;
                response.errorMessage = "No quests available. Please check database configuration.";
                connection.sendTCP(response);
                return;
            }
            
            // Convert to shared model
            List<Quest> quests = new ArrayList<>();
            for (QuestDAO.QuestProgress dbQuest : dbQuests) {
                Quest quest = new Quest(
                    dbQuest.getQuestId(),
                    dbQuest.getDescription(),
                    dbQuest.getGameType(),
                    dbQuest.getTargetCount(),
                    dbQuest.getRewardCredits(),
                    dbQuest.getCurrentProgress(),
                    dbQuest.isClaimed()
                );
                quests.add(quest);
            }
            
            GetQuestsResponse response = new GetQuestsResponse();
            response.success = true;
            response.quests = quests;
            connection.sendTCP(response);
            
            logger.info("Sent {} quests to user {}", quests.size(), userId);
            
        } catch (SQLException e) {
            logger.error("Failed to get quests for user {}: {}", userId, e.getMessage(), e);
            GetQuestsResponse response = new GetQuestsResponse();
            response.success = false;
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("daily_quest_config")) {
                errorMsg = "Database configuration error. Please contact administrator.";
            }
            response.errorMessage = errorMsg != null ? errorMsg : "Database error";
            connection.sendTCP(response);
        } catch (Exception e) {
            logger.error("Unexpected error getting quests for user {}: {}", userId, e.getMessage(), e);
            GetQuestsResponse response = new GetQuestsResponse();
            response.success = false;
            response.errorMessage = "Unexpected error: " + e.getMessage();
            connection.sendTCP(response);
        }
    }
    
    /**
     * Handle ClaimQuestRequest
     */
    public void handleClaimQuest(Connection connection, ClaimQuestRequest request) {
        Integer userId = connectionToUser.get(connection);
        
        if (userId == null) {
            logger.warn("ClaimQuestRequest from unauthenticated connection {}", connection.getID());
            ClaimQuestResponse response = new ClaimQuestResponse();
            response.success = false;
            response.errorMessage = "Not authenticated";
            connection.sendTCP(response);
            return;
        }
        
        try {
            boolean claimed = questDAO.claimQuestReward(userId, request.questId);
            
            ClaimQuestResponse response = new ClaimQuestResponse();
            response.success = claimed;
            response.questId = request.questId;
            
            if (claimed) {
                // Get updated credits
                UserDAO.UserProfile profile = userDAO.getUserProfile(userId);
                response.creditsAwarded = 20000; // Quest reward amount
                response.newTotalCredits = profile.getCredits();
                logger.info("User {} claimed quest {} reward: 20000 credits", userId, request.questId);
            } else {
                response.errorMessage = "Quest not completed or already claimed";
            }
            
            connection.sendTCP(response);
            
        } catch (SQLException e) {
            logger.error("Failed to claim quest for user {}: {}", userId, e.getMessage(), e);
            ClaimQuestResponse response = new ClaimQuestResponse();
            response.success = false;
            response.errorMessage = "Database error";
            connection.sendTCP(response);
        }
    }
}

