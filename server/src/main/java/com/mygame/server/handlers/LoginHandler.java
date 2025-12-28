package com.mygame.server.handlers;

import com.esotericsoftware.kryonet.Connection;
import com.mygame.server.database.DatabaseManager;
import com.mygame.server.database.UserDAO;
import com.mygame.shared.model.PlayerProfile;
import com.mygame.shared.model.Rank;
import com.mygame.shared.network.packets.LoginRequest;
import com.mygame.shared.network.packets.LoginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Handler xử lý LoginRequest
 */
public class LoginHandler {
    private static final Logger logger = LoggerFactory.getLogger(LoginHandler.class);
    private final UserDAO userDAO;
    
    public LoginHandler(DatabaseManager dbManager) {
        this.userDAO = new UserDAO(dbManager);
    }
    
    public interface LoginCallback {
        void onLoginSuccess(Connection connection, int userId);
    }
    
    private LoginCallback loginCallback;
    
    public void setLoginCallback(LoginCallback callback) {
        this.loginCallback = callback;
    }
    
    public void handle(Connection connection, LoginRequest request) {
        logger.info("Nhận login request từ connection {}: username={}", connection.getID(), request.username);
        
        try {
            UserDAO.UserProfile dbProfile = userDAO.login(request.username, request.password);
            
            // Convert sang shared PlayerProfile
            PlayerProfile sharedProfile = convertToSharedProfile(dbProfile);
            
            LoginResponse response = new LoginResponse();
            response.success = true;
            response.playerProfile = sharedProfile;
            response.errorMessage = null;
            
            connection.sendTCP(response);
            
            // Callback để lưu userId
            if (loginCallback != null) {
                loginCallback.onLoginSuccess(connection, dbProfile.getId());
            }
            
            logger.info("Login thành công cho user: {} (ID: {})", request.username, dbProfile.getId());
            
        } catch (SQLException e) {
            logger.warn("Login thất bại: {}", e.getMessage());
            LoginResponse response = new LoginResponse();
            response.success = false;
            response.playerProfile = null;
            response.errorMessage = e.getMessage();
            connection.sendTCP(response);
        } catch (Exception e) {
            logger.error("Lỗi không mong đợi khi login: {}", e.getMessage(), e);
            LoginResponse response = new LoginResponse();
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

