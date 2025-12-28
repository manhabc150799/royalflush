package com.mygame.server.handlers;

import com.esotericsoftware.kryonet.Connection;
import com.mygame.server.database.DatabaseManager;
import com.mygame.server.database.DailyRewardDAO;
import com.mygame.shared.network.packets.DailyRewardRequest;
import com.mygame.shared.network.packets.DailyRewardResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Handler xử lý DailyRewardRequest
 */
public class DailyRewardHandler {
    private static final Logger logger = LoggerFactory.getLogger(DailyRewardHandler.class);
    private final DailyRewardDAO dailyRewardDAO;
    
    public DailyRewardHandler(DatabaseManager dbManager) {
        this.dailyRewardDAO = new DailyRewardDAO(dbManager);
    }
    
    public void handle(Connection connection, DailyRewardRequest request) {
        logger.info("Nhận daily reward request từ connection {}: userId={}", connection.getID(), request.userId);
        
        try {
            // Kiểm tra có thể nhận không
            if (!dailyRewardDAO.canClaimDailyReward(request.userId)) {
                LocalDateTime nextTime = dailyRewardDAO.getNextRewardTime(request.userId);
                DailyRewardResponse response = new DailyRewardResponse();
                response.success = false;
                response.creditsReceived = 0;
                response.nextRewardTime = nextTime;
                response.errorMessage = "Chưa đến thời gian nhận daily reward";
                connection.sendTCP(response);
                logger.debug("User {} chưa thể nhận daily reward", request.userId);
                return;
            }
            
            // Nhận reward
            long creditsReceived = dailyRewardDAO.claimDailyReward(request.userId);
            LocalDateTime nextTime = dailyRewardDAO.getNextRewardTime(request.userId);
            
            DailyRewardResponse response = new DailyRewardResponse();
            response.success = true;
            response.creditsReceived = creditsReceived;
            response.nextRewardTime = nextTime;
            response.errorMessage = null;
            
            connection.sendTCP(response);
            logger.info("User {} đã nhận daily reward: {} credits", request.userId, creditsReceived);
            
        } catch (SQLException e) {
            logger.error("Lỗi khi xử lý daily reward: {}", e.getMessage(), e);
            DailyRewardResponse response = new DailyRewardResponse();
            response.success = false;
            response.creditsReceived = 0;
            response.nextRewardTime = LocalDateTime.now();
            response.errorMessage = e.getMessage();
            connection.sendTCP(response);
        }
    }
}

