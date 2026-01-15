package com.mygame.server.room;

import com.esotericsoftware.kryonet.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class đại diện cho một game room trong memory
 */
public class GameRoom {
    private static final Logger logger = LoggerFactory.getLogger(GameRoom.class);

    private int roomId;
    private String roomName;
    private String gameType;
    private int hostUserId;
    private int maxPlayers;
    private String status; // WAITING, PLAYING, FINISHED

    // Map: userId -> Connection
    private final Map<Integer, Connection> players = new LinkedHashMap<>();
    // Map: userId -> position
    private final Map<Integer, Integer> playerPositions = new LinkedHashMap<>();

    public GameRoom(int roomId, String roomName, String gameType, int hostUserId, int maxPlayers) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.gameType = gameType;
        this.hostUserId = hostUserId;
        this.maxPlayers = maxPlayers;
        this.status = "WAITING";
    }

    public GameRoom(com.mygame.server.database.RoomDAO.RoomData roomData) {
        this.roomId = roomData.getRoomId();
        this.roomName = roomData.getRoomName();
        this.gameType = roomData.getGameType();
        this.hostUserId = roomData.getHostUserId();
        this.maxPlayers = roomData.getMaxPlayers();
        this.status = roomData.getStatus();

        // Load players từ roomData
        if (roomData.getPlayers() != null) {
            for (com.mygame.server.database.RoomDAO.RoomPlayerData playerData : roomData.getPlayers()) {
                playerPositions.put(playerData.getUserId(), playerData.getPosition());
                // Connection sẽ được set khi player join
            }
        }
    }

    public void addPlayer(int userId, Connection connection, int position) {
        players.put(userId, connection);
        playerPositions.put(userId, position);
        logger.debug("Đã thêm player {} vào room {} (position: {})", userId, roomId, position);
    }

    public void removePlayer(int userId) {
        players.remove(userId);
        playerPositions.remove(userId);
        logger.debug("Đã xóa player {} khỏi room {}", userId, roomId);
    }

    public void broadcast(Object message) {
        // Copy values to avoid ConcurrentModificationException if player disconnects
        // during broadcast
        java.util.List<Connection> connections = new java.util.ArrayList<>(players.values());
        for (Connection connection : connections) {
            if (connection != null) {
                try {
                    connection.sendTCP(message);
                } catch (Exception e) {
                    logger.error("Lỗi khi broadcast đến connection: {}", e.getMessage());
                }
            }
        }
    }

    public void sendToPlayer(int userId, Object message) {
        Connection connection = players.get(userId);
        if (connection != null) {
            try {
                connection.sendTCP(message);
            } catch (Exception e) {
                logger.error("Lỗi khi gửi message đến player {}: {}", userId, e.getMessage());
            }
        }
    }

    // Getters and Setters
    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public int getHostUserId() {
        return hostUserId;
    }

    public void setHostUserId(int hostUserId) {
        this.hostUserId = hostUserId;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCurrentPlayers() {
        return players.size();
    }

    public Map<Integer, Connection> getPlayers() {
        return players;
    }

    public Map<Integer, Integer> getPlayerPositions() {
        return playerPositions;
    }

    public boolean isHost(int userId) {
        return hostUserId == userId;
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    /**
     * Get minimum players required to start the game.
     * 
     * @return 2 for POKER (testing), 4 for TIENLEN
     */
    public int getMinPlayers() {
        if ("TIENLEN".equals(gameType)) {
            return 4;
        }
        return 2;
    }

    /**
     * Check all players' status and remove bankrupt players.
     * Called after each round by game logic.
     * 
     * @param playerBalances Map of userId -> current balance
     * @return List of kicked user IDs
     */
    public java.util.List<Integer> checkPlayerStatus(java.util.Map<Integer, Long> playerBalances) {
        java.util.List<Integer> kicked = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Integer, Long> entry : playerBalances.entrySet()) {
            if (entry.getValue() <= 0) {
                kicked.add(entry.getKey());
            }
        }
        // Remove kicked players
        for (Integer userId : kicked) {
            removePlayer(userId);
            logger.info("Player {} removed from room {} due to bankruptcy (balance <= 0)", userId, roomId);
        }
        return kicked;
    }
}
