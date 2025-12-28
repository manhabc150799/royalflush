package com.mygame.server.handlers;

import com.esotericsoftware.kryonet.Connection;
import com.mygame.server.database.DatabaseManager;
import com.mygame.server.database.MatchHistoryDAO;
import com.mygame.shared.model.MatchHistoryEntry;
import com.mygame.shared.network.packets.MatchHistoryRequest;
import com.mygame.shared.network.packets.MatchHistoryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler xử lý MatchHistoryRequest
 */
public class MatchHistoryHandler {
    private static final Logger logger = LoggerFactory.getLogger(MatchHistoryHandler.class);
    private final MatchHistoryDAO matchHistoryDAO;
    
    public MatchHistoryHandler(DatabaseManager dbManager) {
        this.matchHistoryDAO = new MatchHistoryDAO(dbManager);
    }
    
    public void handle(Connection connection, MatchHistoryRequest request) {
        logger.debug("Nhận match history request từ connection {}: userId={}, limit={}", 
                     connection.getID(), request.userId, request.limit);
        
        try {
            List<MatchHistoryDAO.MatchHistoryEntry> dbEntries = 
                matchHistoryDAO.getRecentMatches(request.userId, request.limit);
            
            // Convert sang shared MatchHistoryEntry
            List<MatchHistoryEntry> sharedEntries = new ArrayList<>();
            for (MatchHistoryDAO.MatchHistoryEntry dbEntry : dbEntries) {
                MatchHistoryEntry shared = new MatchHistoryEntry();
                shared.gameType = dbEntry.getGameType();
                shared.matchMode = dbEntry.getMatchMode();
                shared.result = dbEntry.getResult();
                shared.creditsChange = dbEntry.getCreditsChange();
                // Convert timestamp from LocalDateTime to Date
                if (dbEntry.getTimestamp() != null) {
                    shared.timestamp = java.util.Date.from(
                        dbEntry.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant()
                    );
                }
                sharedEntries.add(shared);
            }
            
            MatchHistoryResponse response = new MatchHistoryResponse();
            response.entries = sharedEntries;
            
            connection.sendTCP(response);
            logger.debug("Đã gửi {} entries trong match history", sharedEntries.size());
            
        } catch (SQLException e) {
            logger.error("Lỗi khi lấy match history: {}", e.getMessage(), e);
            // Gửi response rỗng nếu có lỗi
            MatchHistoryResponse response = new MatchHistoryResponse();
            response.entries = new ArrayList<>();
            connection.sendTCP(response);
        }
    }
}

