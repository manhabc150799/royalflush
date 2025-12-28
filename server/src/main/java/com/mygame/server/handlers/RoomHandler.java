package com.mygame.server.handlers;

import com.esotericsoftware.kryonet.Connection;
import com.mygame.server.database.DatabaseManager;
import com.mygame.server.room.GameRoom;
import com.mygame.server.room.GameRoomManager;
import com.mygame.shared.model.GameType;
import com.mygame.shared.model.RoomInfo;
import com.mygame.shared.network.packets.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler tổng hợp xử lý tất cả room operations
 */
public class RoomHandler {
    private static final Logger logger = LoggerFactory.getLogger(RoomHandler.class);
    private final GameRoomManager roomManager;
    
    // Map connection -> userId
    private final Map<Connection, Integer> connectionToUser = new ConcurrentHashMap<>();
    
    public RoomHandler(DatabaseManager dbManager) {
        this.roomManager = new GameRoomManager(dbManager);
    }
    
    public void setUserId(Connection connection, int userId) {
        connectionToUser.put(connection, userId);
    }
    
    public Integer getUserId(Connection connection) {
        return connectionToUser.get(connection);
    }
    
    /**
     * Cho phép các component khác (vd. GameSessionManager) truy cập roomManager.
     */
    public GameRoomManager getRoomManager() {
        return roomManager;
    }
    
    public void handleCreateRoom(Connection connection, CreateRoomRequest request) {
        Integer userId = connectionToUser.get(connection);
        if (userId == null) {
            sendError(connection, new CreateRoomResponse(), "Chưa đăng nhập");
            return;
        }
        
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
            
            RoomInfo roomInfo = convertToRoomInfo(room);
            
            CreateRoomResponse response = new CreateRoomResponse();
            response.setSuccess(true);
            response.setRoomInfo(roomInfo);
            response.setErrorMessage(null);
            connection.sendTCP(response);
            
        } catch (SQLException e) {
            sendError(connection, new CreateRoomResponse(), e.getMessage());
        }
    }
    
    public void handleJoinRoom(Connection connection, JoinRoomRequest request) {
        Integer userId = connectionToUser.get(connection);
        if (userId == null) {
            sendError(connection, new JoinRoomResponse(), "Chưa đăng nhập");
            return;
        }
        
        try {
            GameRoom room = roomManager.joinRoom(request.getRoomId(), userId, connection);
            RoomInfo roomInfo = convertToRoomInfo(room);
            
            JoinRoomResponse response = new JoinRoomResponse();
            response.setSuccess(true);
            response.setRoomInfo(roomInfo);
            response.setErrorMessage(null);
            connection.sendTCP(response);
            
            // Broadcast room update đến tất cả players
            RoomUpdatePacket update = new RoomUpdatePacket();
            update.setRoomInfo(roomInfo);
            roomManager.broadcastToRoom(room.getRoomId(), update);
            
        } catch (SQLException e) {
            sendError(connection, new JoinRoomResponse(), e.getMessage());
        }
    }
    
    public void handleLeaveRoom(Connection connection, LeaveRoomRequest request) {
        Integer userId = connectionToUser.get(connection);
        if (userId == null) {
            return;
        }
        
        try {
            roomManager.leaveRoom(request.getRoomId(), userId);
            
            // Broadcast update
            GameRoom room = roomManager.getRoom(request.getRoomId());
            if (room != null) {
                RoomInfo roomInfo = convertToRoomInfo(room);
                RoomUpdatePacket update = new RoomUpdatePacket();
                update.setRoomInfo(roomInfo);
                roomManager.broadcastToRoom(request.getRoomId(), update);
            }
            
        } catch (SQLException e) {
            logger.error("Lỗi khi leave room: {}", e.getMessage(), e);
        }
    }
    
    public void handleListRooms(Connection connection, ListRoomsRequest request) {
        try {
            String gameType = request.getGameType() != null ? request.getGameType().name() : null;
            List<com.mygame.server.database.RoomDAO.RoomData> dbRooms = 
                roomManager.getWaitingRooms(gameType, 50);
            
            List<RoomInfo> roomInfos = new ArrayList<>();
            for (com.mygame.server.database.RoomDAO.RoomData dbRoom : dbRooms) {
                RoomInfo info = new RoomInfo();
                info.setRoomId(dbRoom.getRoomId());
                info.setRoomName(dbRoom.getRoomName());
                info.setGameType(GameType.valueOf(dbRoom.getGameType()));
                info.setHostUserId(dbRoom.getHostUserId());
                info.setHostUsername(dbRoom.getHostUsername());
                info.setMaxPlayers(dbRoom.getMaxPlayers());
                info.setCurrentPlayers(dbRoom.getCurrentPlayers());
                info.setStatus(dbRoom.getStatus());
                roomInfos.add(info);
            }
            
            ListRoomsResponse response = new ListRoomsResponse();
            response.setRooms(roomInfos);
            connection.sendTCP(response);
            
        } catch (SQLException e) {
            logger.error("Lỗi khi list rooms: {}", e.getMessage(), e);
            ListRoomsResponse response = new ListRoomsResponse();
            response.setRooms(new ArrayList<>());
            connection.sendTCP(response);
        }
    }
    
    public void handleDisconnection(Connection connection) {
        Integer userId = connectionToUser.remove(connection);
        if (userId != null) {
            roomManager.handleDisconnection(userId);
        }
    }
    
    private RoomInfo convertToRoomInfo(GameRoom room) {
        RoomInfo info = new RoomInfo();
        info.setRoomId(room.getRoomId());
        info.setRoomName(room.getRoomName());
        info.setGameType(GameType.valueOf(room.getGameType()));
        info.setHostUserId(room.getHostUserId());
        info.setMaxPlayers(room.getMaxPlayers());
        info.setCurrentPlayers(room.getCurrentPlayers());
        info.setStatus(room.getStatus());
        
        // Convert players
        List<RoomInfo.RoomPlayerInfo> playerInfos = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : room.getPlayerPositions().entrySet()) {
            RoomInfo.RoomPlayerInfo playerInfo = new RoomInfo.RoomPlayerInfo();
            playerInfo.setUserId(entry.getKey());
            playerInfo.setPosition(entry.getValue());
            playerInfo.setUsername("Player " + entry.getKey()); // TODO: Load từ database
            playerInfos.add(playerInfo);
        }
        info.setPlayers(playerInfos);
        
        return info;
    }
    
    private void sendError(Connection connection, Object response, String error) {
        try {
            if (response instanceof CreateRoomResponse) {
                ((CreateRoomResponse) response).setSuccess(false);
                ((CreateRoomResponse) response).setErrorMessage(error);
            } else if (response instanceof JoinRoomResponse) {
                ((JoinRoomResponse) response).setSuccess(false);
                ((JoinRoomResponse) response).setErrorMessage(error);
            }
            connection.sendTCP(response);
        } catch (Exception e) {
            logger.error("Lỗi khi gửi error response: {}", e.getMessage());
        }
    }
}

