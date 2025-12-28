package com.mygame.server.handlers;

import com.esotericsoftware.kryonet.Connection;
import com.mygame.server.database.DatabaseManager;
import com.mygame.server.database.UserDAO;
import com.mygame.shared.model.PlayerProfile;
import com.mygame.shared.model.Rank;
import com.mygame.shared.network.packets.RegisterRequest;
import com.mygame.shared.network.packets.RegisterResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Handler xử lý RegisterRequest
 */
public class RegisterHandler {
    private static final Logger logger = LoggerFactory.getLogger(RegisterHandler.class);
    private final UserDAO userDAO;
    
    public RegisterHandler(DatabaseManager dbManager) {
        this.userDAO = new UserDAO(dbManager);
    }
    
    public void handle(Connection connection, RegisterRequest request) {
        logger.info("Nhận register request từ connection {}: username={}", connection.getID(), request.username);
        
        try {
            UserDAO.UserProfile dbProfile = userDAO.register(request.username, request.password);
            
            // Convert sang shared PlayerProfile
            PlayerProfile sharedProfile = convertToSharedProfile(dbProfile);
            
            RegisterResponse response = new RegisterResponse();
            response.success = true;
            response.playerProfile = sharedProfile;
            response.errorMessage = null;
            
            connection.sendTCP(response);
            logger.info("Đăng ký thành công cho user: {} (ID: {})", request.username, dbProfile.getId());
            
        } catch (SQLException e) {
            logger.warn("Đăng ký thất bại: {}", e.getMessage());
            RegisterResponse response = new RegisterResponse();
            response.success = false;
            response.playerProfile = null;
            response.errorMessage = e.getMessage();
            connection.sendTCP(response);
        } catch (Exception e) {
            logger.error("Lỗi không mong đợi khi đăng ký: {}", e.getMessage(), e);
            RegisterResponse response = new RegisterResponse();
            response.success = false;
            response.playerProfile = null;
            response.errorMessage = "Server error: " + e.getMessage();
            connection.sendTCP(response);
        }
    }
    
    private PlayerProfile convertToSharedProfile(UserDAO.UserProfile dbProfile) {
        PlayerProfile shared = new PlayerProfile();
        shared.id = dbProfile.getId();
        shared.username = dbProfile.getUsername();
        shared.credits = dbProfile.getCredits();
        shared.rank = Rank.valueOf(dbProfile.getRank());
        shared.totalWins = dbProfile.getTotalWins();
        shared.totalLosses = dbProfile.getTotalLosses();
        return shared;
    }
}

