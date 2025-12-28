package com.mygame.server.handlers;

import com.esotericsoftware.kryonet.Connection;
import com.mygame.server.database.DatabaseManager;
import com.mygame.server.room.GameRoom;
import com.mygame.server.room.GameRoomManager;
import com.mygame.shared.model.GameType;
import com.mygame.shared.model.RoomInfo;
import com.mygame.shared.network.packets.CreateRoomRequest;
import com.mygame.shared.network.packets.CreateRoomResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Handler xử lý CreateRoomRequest
 */
public class CreateRoomHandler {
    private static final Logger logger = LoggerFactory.getLogger(CreateRoomHandler.class);
    private final GameRoomManager roomManager;
    
    // Map connection -> userId (tạm thời, nên lưu trong connection object)
    private final java.util.Map<Connection, Integer> connectionToUser = new java.util.concurrent.ConcurrentHashMap<>();
    
    public CreateRoomHandler(DatabaseManager dbManager) {
        this.roomManager = new GameRoomManager(dbManager);
    }
    
    public void setUserId(Connection connection, int userId) {
        connectionToUser.put(connection, userId);
    }
    
    public void handle(Connection connection, CreateRoomRequest request) {
        Integer userId = connectionToUser.get(connection);
        if (userId == null) {
            logger.warn("Connection không có userId, không thể tạo room");
            CreateRoomResponse response = new CreateRoomResponse();
            response.setSuccess(false);
            response.setRoomInfo(null);
            response.setErrorMessage("Chưa đăng nhập");
            connection.sendTCP(response);
            return;
        }
        
        logger.info("Nhận CreateRoomRequest từ user {}: {} ({})", userId, request.getRoomName(), request.getGameType());
        
        try {
            String roomName = request.getRoomName() != null && !request.getRoomName().isEmpty() 
                ? request.getRoomName() 
                : "Room " + userId;
            
            GameRoom room = roomManager.createRoom(
                roomName,
                request.getGameType().name(),
                userId,
                request.getMaxPlayers(),
                connection
            );
            
            // Convert sang RoomInfo
            RoomInfo roomInfo = convertToRoomInfo(room, userId);
            
            CreateRoomResponse response = new CreateRoomResponse();
            response.setSuccess(true);
            response.setRoomInfo(roomInfo);
            response.setErrorMessage(null);
            
            connection.sendTCP(response);
            logger.info("Đã tạo room thành công: {} (ID: {})", roomName, room.getRoomId());
            
        } catch (SQLException e) {
            logger.error("Lỗi khi tạo room: {}", e.getMessage(), e);
            CreateRoomResponse response = new CreateRoomResponse();
            response.setSuccess(false);
            response.setRoomInfo(null);
            response.setErrorMessage(e.getMessage());
            connection.sendTCP(response);
        }
    }
    
    private RoomInfo convertToRoomInfo(GameRoom room, int currentUserId) {
        RoomInfo info = new RoomInfo();
        info.setRoomId(room.getRoomId());
        info.setRoomName(room.getRoomName());
        info.setGameType(GameType.valueOf(room.getGameType()));
        info.setHostUserId(room.getHostUserId());
        info.setMaxPlayers(room.getMaxPlayers());
        info.setCurrentPlayers(room.getCurrentPlayers());
        info.setStatus(room.getStatus());
        
        // Convert players
        java.util.List<RoomInfo.RoomPlayerInfo> playerInfos = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Integer, Integer> entry : room.getPlayerPositions().entrySet()) {
            RoomInfo.RoomPlayerInfo playerInfo = new RoomInfo.RoomPlayerInfo();
            playerInfo.setUserId(entry.getKey());
            playerInfo.setPosition(entry.getValue());
            // Username sẽ được load sau
            playerInfo.setUsername("Player " + entry.getKey());
            playerInfos.add(playerInfo);
        }
        info.setPlayers(playerInfos);
        
        return info;
    }
}

