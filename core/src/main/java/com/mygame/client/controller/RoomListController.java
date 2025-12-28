package com.mygame.client.controller;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.github.czyzby.autumn.annotation.Inject;
import com.github.czyzby.autumn.mvc.component.ui.InterfaceService;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewRenderer;
import com.github.czyzby.autumn.mvc.stereotype.View;
import com.github.czyzby.autumn.mvc.stereotype.ViewActionContainer;
import com.github.czyzby.lml.annotation.LmlAction;
import com.github.czyzby.lml.parser.action.ActionContainer;
import com.github.czyzby.lml.annotation.LmlActor;
import com.github.czyzby.lml.annotation.LmlAfter;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.mygame.client.service.NetworkService;
import com.mygame.client.service.SessionManager;
import com.mygame.shared.model.GameType;
import com.mygame.shared.model.RoomInfo;
import com.mygame.shared.network.packets.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Controller cho Room List screen
 */
@View(id = "roomList", value = "ui/templates/room_list.lml")
@ViewActionContainer("roomList")
public class RoomListController implements ViewRenderer, ActionContainer {
    private static final Logger logger = LoggerFactory.getLogger(RoomListController.class);
    
    @Inject private NetworkService networkService;
    @Inject private SessionManager sessionManager;
    @Inject private InterfaceService interfaceService;
    
    @LmlActor("roomListTable") private VisTable roomListTable;
    @LmlActor("statusLabel") private VisLabel statusLabel;
    
    private GameType currentGameType;
    
    @LmlAfter
    public void initialize() {
        logger.info("RoomListController đã khởi tạo");
        
        // Lấy game type từ session manager (nếu có)
        GameType pendingGameType = sessionManager.getAndClearPendingGameType();
        if (pendingGameType != null) {
            currentGameType = pendingGameType;
        } else {
            // Default là POKER nếu không có
            currentGameType = GameType.POKER;
        }
        
        // Setup network listeners
        setupNetworkListeners();
        
        // Load rooms
        refreshRooms();
    }
    
    private void setupNetworkListeners() {
        networkService.addPacketListener(packet -> {
            com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                if (packet instanceof ListRoomsResponse) {
                    handleListRoomsResponse((ListRoomsResponse) packet);
                } else if (packet instanceof CreateRoomResponse) {
                    handleCreateRoomResponse((CreateRoomResponse) packet);
                } else if (packet instanceof JoinRoomResponse) {
                    handleJoinRoomResponse((JoinRoomResponse) packet);
                } else if (packet instanceof RoomUpdatePacket) {
                    // Room update - refresh list
                    refreshRooms();
                }
            });
        });
    }
    
    /**
     * Action: Refresh room list
     */
    @LmlAction("refreshRooms")
    public void refreshRooms() {
        if (!networkService.isConnected()) {
            if (statusLabel != null) {
                statusLabel.setText("Chưa kết nối đến server");
            }
            return;
        }
        
        if (statusLabel != null) {
            statusLabel.setText("Đang tải danh sách phòng...");
        }
        
        ListRoomsRequest request = new ListRoomsRequest(currentGameType);
        networkService.sendPacket(request);
        logger.debug("Đã gửi ListRoomsRequest");
    }
    
    /**
     * Action: Show create room dialog
     */
    @LmlAction("showCreateRoom")
    public void showCreateRoom() {
        logger.info("Hiển thị create room dialog");
        // TODO: Show create room dialog
        // interfaceService.showDialog(CreateRoomDialog.class);
    }
    
    /**
     * Action: Go back to lobby
     */
    @LmlAction("goBack")
    public void goBack() {
        interfaceService.show(LobbyController.class);
    }
    
    /**
     * Join room
     */
    private void joinRoom(int roomId) {
        if (!networkService.isConnected()) {
            if (statusLabel != null) {
                statusLabel.setText("Chưa kết nối đến server");
            }
            return;
        }
        
        JoinRoomRequest request = new JoinRoomRequest(roomId);
        networkService.sendPacket(request);
        logger.info("Đã gửi JoinRoomRequest cho room: {}", roomId);
    }
    
    /**
     * Xử lý ListRoomsResponse
     */
    private void handleListRoomsResponse(ListRoomsResponse response) {
        if (roomListTable == null || response.getRooms() == null) {
            return;
        }
        
        roomListTable.clearChildren();
        
        if (response.getRooms().isEmpty()) {
            VisLabel noRoomsLabel = new VisLabel("Không có phòng nào đang chờ");
            roomListTable.add(noRoomsLabel).colspan(5).row();
            
            if (statusLabel != null) {
                statusLabel.setText("Không có phòng nào");
            }
            return;
        }
        
        // Add rows
        for (RoomInfo room : response.getRooms()) {
            VisTable row = new VisTable();
            
            // Room name
            VisLabel nameLabel = new VisLabel(room.getRoomName() != null ? room.getRoomName() : "Room " + room.getRoomId());
            row.add(nameLabel).width(200).left().pad(5);
            
            // Game type
            VisLabel typeLabel = new VisLabel(room.getGameType().name());
            row.add(typeLabel).width(100).center().pad(5);
            
            // Host
            VisLabel hostLabel = new VisLabel(room.getHostUsername() != null ? room.getHostUsername() : "Unknown");
            row.add(hostLabel).width(120).left().pad(5);
            
            // Players count
            VisLabel playersLabel = new VisLabel(room.getCurrentPlayers() + "/" + room.getMaxPlayers());
            row.add(playersLabel).width(100).center().pad(5);
            
            // Join button
            VisTextButton joinButton = new VisTextButton("Tham gia");
            final int roomId = room.getRoomId();
            joinButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    joinRoom(roomId);
                }
            });
            row.add(joinButton).width(100).pad(5);
            
            roomListTable.add(row).row();
        }
        
        if (statusLabel != null) {
            statusLabel.setText("Tìm thấy " + response.getRooms().size() + " phòng");
        }
        
        logger.info("Đã cập nhật room list với {} rooms", response.getRooms().size());
    }
    
    /**
     * Xử lý CreateRoomResponse
     */
    private void handleCreateRoomResponse(CreateRoomResponse response) {
        if (response.isSuccess() && response.getRoomInfo() != null) {
            // Lưu room info vào session manager
            sessionManager.setPendingRoomInfo(response.getRoomInfo());
            // Chuyển sang room lobby
            logger.info("Đã tạo room thành công, chuyển sang room lobby");
            interfaceService.show(RoomLobbyController.class);
        } else {
            if (statusLabel != null) {
                statusLabel.setText("Lỗi: " + (response.getErrorMessage() != null ? response.getErrorMessage() : "Không thể tạo phòng"));
            }
        }
    }
    
    /**
     * Xử lý JoinRoomResponse
     */
    private void handleJoinRoomResponse(JoinRoomResponse response) {
        if (response.isSuccess() && response.getRoomInfo() != null) {
            // Lưu room info vào session manager
            sessionManager.setPendingRoomInfo(response.getRoomInfo());
            // Chuyển sang room lobby
            logger.info("Đã tham gia room thành công, chuyển sang room lobby");
            interfaceService.show(RoomLobbyController.class);
        } else {
            if (statusLabel != null) {
                statusLabel.setText("Lỗi: " + (response.getErrorMessage() != null ? response.getErrorMessage() : "Không thể tham gia phòng"));
            }
        }
    }
    
    /**
     * Set game type (được gọi từ LobbyController)
     */
    public void setGameType(GameType gameType) {
        this.currentGameType = gameType;
        refreshRooms();
    }
    
    @Override
    public void render(Stage stage, float delta) {
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        stage.act(delta);
        stage.draw();
    }
}

