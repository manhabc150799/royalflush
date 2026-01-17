package com.mygame.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.mygame.server.database.DatabaseManager;
import com.mygame.server.handlers.*;
import com.mygame.server.game.GameSessionManager;
import com.mygame.shared.network.packets.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class để tạo Listener xử lý các kết nối và packets từ client
 */
public class ServerListener {
    private static final Logger logger = LoggerFactory.getLogger(ServerListener.class);

    private final DatabaseManager dbManager;
    private final LoginHandler loginHandler;
    private final RegisterHandler registerHandler;
    private final LeaderboardHandler leaderboardHandler;
    private final MatchHistoryHandler matchHistoryHandler;
    private final DailyRewardHandler dailyRewardHandler;
    private final QuestHandler questHandler;
    private final com.mygame.server.handlers.RoomHandler roomHandler;
    private final GameSessionManager gameSessionManager;

    // Map connection -> userId để track logged in users
    private final java.util.Map<com.esotericsoftware.kryonet.Connection, Integer> connectionToUser = new java.util.concurrent.ConcurrentHashMap<>();

    public ServerListener(DatabaseManager dbManager) {
        this.dbManager = dbManager;

        // Khởi tạo các handlers
        this.loginHandler = new LoginHandler(dbManager);
        this.registerHandler = new RegisterHandler(dbManager);
        this.leaderboardHandler = new LeaderboardHandler(dbManager);
        this.matchHistoryHandler = new MatchHistoryHandler(dbManager);
        this.dailyRewardHandler = new DailyRewardHandler(dbManager);
        this.questHandler = new QuestHandler(dbManager, connectionToUser);
        this.roomHandler = new com.mygame.server.handlers.RoomHandler(dbManager);
        this.gameSessionManager = new GameSessionManager(dbManager, roomHandler.getRoomManager());
        // Wire GameSessionManager to RoomHandler for starting sessions
        this.roomHandler.setGameSessionManager(this.gameSessionManager);

        // Setup login callback để lưu userId
        loginHandler.setLoginCallback((connection, userId) -> {
            connectionToUser.put(connection, userId);
            roomHandler.setUserId(connection, userId);
            logger.info("Đã lưu userId {} cho connection {}", userId, connection.getID());
        });
    }

    /**
     * Tạo và trả về Listener instance để add vào server
     */
    public Listener createListener() {
        return new Listener() {
            @Override
            public void connected(Connection connection) {
                logger.info("Client kết nối: {} (ID: {})", connection.getRemoteAddressTCP(), connection.getID());
            }

            @Override
            public void disconnected(Connection connection) {
                logger.info("Client ngắt kết nối: {} (ID: {})", connection.getRemoteAddressTCP(), connection.getID());
                // Cleanup room data
                roomHandler.handleDisconnection(connection);
                connectionToUser.remove(connection);
            }

            @Override
            public void received(Connection connection, Object object) {
                // Ignore internal KryoNet framework messages (KeepAlive, etc.)
                if (object instanceof com.esotericsoftware.kryonet.FrameworkMessage) {
                    return;
                }

                // Route packets đến các handlers tương ứng
                logger.debug("Nhận packet từ client {}: {}", connection.getID(), object.getClass().getSimpleName());

                if (object instanceof LoginRequest) {
                    loginHandler.handle(connection, (LoginRequest) object);
                } else if (object instanceof RegisterRequest) {
                    registerHandler.handle(connection, (RegisterRequest) object);
                } else if (object instanceof LeaderboardRequest) {
                    leaderboardHandler.handle(connection, (LeaderboardRequest) object);
                } else if (object instanceof MatchHistoryRequest) {
                    matchHistoryHandler.handle(connection, (MatchHistoryRequest) object);
                } else if (object instanceof DailyRewardRequest) {
                    dailyRewardHandler.handle(connection, (DailyRewardRequest) object);
                } else if (object instanceof GetQuestsRequest) {
                    questHandler.handleGetQuests(connection, (GetQuestsRequest) object);
                } else if (object instanceof ClaimQuestRequest) {
                    questHandler.handleClaimQuest(connection, (ClaimQuestRequest) object);
                } else if (object instanceof CreateRoomRequest) {
                    roomHandler.handleCreateRoom(connection, (CreateRoomRequest) object);
                } else if (object instanceof JoinRoomRequest) {
                    roomHandler.handleJoinRoom(connection, (JoinRoomRequest) object);
                } else if (object instanceof LeaveRoomRequest) {
                    roomHandler.handleLeaveRoom(connection, (LeaveRoomRequest) object);
                } else if (object instanceof ListRoomsRequest) {
                    roomHandler.handleListRooms(connection, (ListRoomsRequest) object);
                } else if (object instanceof StartGameRequest) {
                    roomHandler.handleStartGame(connection, (StartGameRequest) object);
                } else if (object instanceof com.mygame.shared.network.packets.game.PlayerActionPacket) {
                    gameSessionManager
                            .handlePlayerAction((com.mygame.shared.network.packets.game.PlayerActionPacket) object);
                } else if (object instanceof com.mygame.shared.network.packets.game.PlayAgainVotePacket) {
                    gameSessionManager
                            .handlePlayAgainVote((com.mygame.shared.network.packets.game.PlayAgainVotePacket) object);
                } else {
                    logger.warn("Unknown packet type: {}", object.getClass().getName());
                }
            }

            @Override
            public void idle(Connection connection) {
                // Có thể dùng để ping/pong nếu cần
            }
        };
    }
}
