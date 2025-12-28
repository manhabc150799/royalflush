package com.mygame.server.handlers;

import com.esotericsoftware.kryonet.Connection;
import com.mygame.server.database.DatabaseManager;
import com.mygame.server.database.LeaderboardDAO;
import com.mygame.shared.model.LeaderboardEntry;
import com.mygame.shared.model.Rank;
import com.mygame.shared.network.packets.LeaderboardRequest;
import com.mygame.shared.network.packets.LeaderboardResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler xử lý LeaderboardRequest
 */
public class LeaderboardHandler {
    private static final Logger logger = LoggerFactory.getLogger(LeaderboardHandler.class);
    private final LeaderboardDAO leaderboardDAO;
    
    public LeaderboardHandler(DatabaseManager dbManager) {
        this.leaderboardDAO = new LeaderboardDAO(dbManager);
    }
    
    public void handle(Connection connection, LeaderboardRequest request) {
        logger.debug("Nhận leaderboard request từ connection {}: limit={}", connection.getID(), request.limit);
        
        try {
            List<LeaderboardDAO.LeaderboardEntry> dbEntries = leaderboardDAO.getTopPlayers(request.limit);
            
            // Convert sang shared LeaderboardEntry
            List<LeaderboardEntry> sharedEntries = new ArrayList<>();
            for (LeaderboardDAO.LeaderboardEntry dbEntry : dbEntries) {
                LeaderboardEntry shared = new LeaderboardEntry();
                shared.rank = dbEntry.getRank();
                shared.username = dbEntry.getUsername();
                shared.credits = dbEntry.getCredits();
                shared.rankEnum = Rank.valueOf(dbEntry.getRankEnum());
                sharedEntries.add(shared);
            }
            
            LeaderboardResponse response = new LeaderboardResponse();
            response.entries = sharedEntries;
            
            connection.sendTCP(response);
            logger.debug("Đã gửi {} entries trong leaderboard", sharedEntries.size());
            
        } catch (SQLException e) {
            logger.error("Lỗi khi lấy leaderboard: {}", e.getMessage(), e);
            // Gửi response rỗng nếu có lỗi
            LeaderboardResponse response = new LeaderboardResponse();
            response.entries = new ArrayList<>();
            connection.sendTCP(response);
        }
    }
}

