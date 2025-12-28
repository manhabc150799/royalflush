package com.mygame.client.controller;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
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
import com.kotcrab.vis.ui.widget.VisTextField;
import com.mygame.client.service.NetworkService;
import com.mygame.client.service.SessionManager;
import com.mygame.shared.model.RoomInfo;
import com.mygame.shared.network.packets.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller cho Room Lobby (trong phòng chờ)
 */
@View(id = "roomLobby", value = "ui/templates/room_lobby.lml")
@ViewActionContainer("roomLobby")
public class RoomLobbyController implements ViewRenderer, ActionContainer {
    private static final Logger logger = LoggerFactory.getLogger(RoomLobbyController.class);
    
    @Inject private NetworkService networkService;
    @Inject private SessionManager sessionManager;
    @Inject private InterfaceService interfaceService;
    
    @LmlActor("roomNameLabel") private VisLabel roomNameLabel;
    @LmlActor("playerSlotsTable") private VisTable playerSlotsTable;
    @LmlActor("startButton") private VisTextButton startButton;
    @LmlActor("leaveButton") private VisTextButton leaveButton;
    @LmlActor("chatTable") private VisTable chatTable;
    @LmlActor("chatInput") private VisTextField chatInput;
    
    private RoomInfo currentRoom;
    private int currentUserId;
    
    @LmlAfter
    public void initialize() {
        logger.info("RoomLobbyController đã khởi tạo");
        
        currentUserId = sessionManager.getCurrentUserId();
        
        // Lấy room info từ session manager (nếu có)
        RoomInfo pendingRoom = sessionManager.getAndClearPendingRoomInfo();
        if (pendingRoom != null) {
            setRoomInfo(pendingRoom);
        }
        
        // Setup network listeners
        setupNetworkListeners();
    }
    
    /**
     * Set room info (được gọi khi join/create room thành công)
     */
    public void setRoomInfo(RoomInfo roomInfo) {
        this.currentRoom = roomInfo;
        updateUI();
    }
    
    private void setupNetworkListeners() {
        networkService.addPacketListener(packet -> {
            com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                if (packet instanceof RoomUpdatePacket) {
                    handleRoomUpdate((RoomUpdatePacket) packet);
                }
            });
        });
    }
    
    /**
     * Cập nhật UI với room info hiện tại
     */
    private void updateUI() {
        if (currentRoom == null) {
            return;
        }
        
        // Update room name
        if (roomNameLabel != null) {
            roomNameLabel.setText(currentRoom.getRoomName() != null ? 
                currentRoom.getRoomName() : "Room " + currentRoom.getRoomId());
        }
        
        // Update player slots
        updatePlayerSlots();
        
        // Update start button visibility (chỉ host mới thấy)
        if (startButton != null) {
            startButton.setVisible(currentRoom.getHostUserId() == currentUserId && 
                                 currentRoom.getCurrentPlayers() >= 2);
        }
    }
    
    /**
     * Cập nhật player slots
     */
    private void updatePlayerSlots() {
        if (playerSlotsTable == null || currentRoom == null) {
            return;
        }
        
        playerSlotsTable.clearChildren();
        
        int maxPlayers = currentRoom.getMaxPlayers();
        java.util.List<RoomInfo.RoomPlayerInfo> players = currentRoom.getPlayers();
        
        // Tạo map position -> player
        java.util.Map<Integer, RoomInfo.RoomPlayerInfo> playerMap = new java.util.HashMap<>();
        if (players != null) {
            for (RoomInfo.RoomPlayerInfo player : players) {
                playerMap.put(player.getPosition(), player);
            }
        }
        
        // Tạo slots
        String[] hubImages = {"images/CatUI/hub1.png", "images/CatUI/hub2.png", 
                             "images/CatUI/hub3.png", "images/CatUI/hub4.png"};
        
        for (int i = 0; i < maxPlayers; i++) {
            VisTable slot = new VisTable();
            slot.setBackground(VisUI.getSkin().getDrawable("dialogDim"));
            slot.pad(10);
            
            RoomInfo.RoomPlayerInfo player = playerMap.get(i);
            
            if (player != null) {
                // Có player
                String hubImage = hubImages[i % hubImages.length];
                
                // Hub image (placeholder)
                VisLabel hubLabel = new VisLabel("Player " + (i + 1));
                slot.add(hubLabel).row();
                
                // Player name
                VisLabel nameLabel = new VisLabel(player.getUsername() != null ? 
                    player.getUsername() : "Player " + player.getUserId());
                if (player.getUserId() == currentUserId) {
                    nameLabel.setColor(1f, 1f, 0f, 1f); // Highlight mình
                }
                slot.add(nameLabel).row();
                
                // Host indicator
                if (player.getUserId() == currentRoom.getHostUserId()) {
                    VisLabel hostLabel = new VisLabel("[HOST]");
                    hostLabel.setColor(1f, 0.84f, 0f, 1f); // Vàng
                    slot.add(hostLabel).row();
                }
            } else {
                // Slot trống
                VisLabel emptyLabel = new VisLabel("Trống");
                emptyLabel.setColor(0.5f, 0.5f, 0.5f, 1f);
                slot.add(emptyLabel);
            }
            
            playerSlotsTable.add(slot).width(150).height(120).pad(5).row();
        }
    }
    
    /**
     * Xử lý RoomUpdatePacket
     */
    private void handleRoomUpdate(RoomUpdatePacket packet) {
        if (packet.getRoomInfo() != null && 
            packet.getRoomInfo().getRoomId() == (currentRoom != null ? currentRoom.getRoomId() : -1)) {
            currentRoom = packet.getRoomInfo();
            updateUI();
            logger.debug("Đã cập nhật room info");
        }
    }
    
    /**
     * Action: Leave room
     */
    @LmlAction("leaveRoom")
    public void leaveRoom() {
        if (currentRoom == null) {
            return;
        }
        
        LeaveRoomRequest request = new LeaveRoomRequest(currentRoom.getRoomId());
        networkService.sendPacket(request);
        
        // Chuyển về room list
        interfaceService.show(RoomListController.class);
        logger.info("Đã rời room");
    }
    
    /**
     * Action: Start game
     */
    @LmlAction("startGame")
    public void startGame() {
        if (currentRoom == null || currentRoom.getHostUserId() != currentUserId) {
            logger.warn("Không phải host hoặc room không hợp lệ");
            return;
        }
        
        if (currentRoom.getCurrentPlayers() < 2) {
            logger.warn("Cần ít nhất 2 người chơi");
            return;
        }
        
        // TODO: Gửi StartGameRequest
        logger.info("Bắt đầu game cho room: {}", currentRoom.getRoomId());
    }
    
    /**
     * Action: Send chat message
     */
    @LmlAction("sendChat")
    public void sendChat() {
        if (chatInput == null || currentRoom == null) {
            return;
        }
        
        String message = chatInput.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        
        // TODO: Gửi ChatMessage packet
        addChatMessage(sessionManager.getCurrentPlayer().username, message);
        
        chatInput.setText("");
        logger.debug("Chat: {}", message);
    }
    
    /**
     * Thêm chat message vào UI
     */
    private void addChatMessage(String username, String message) {
        if (chatTable == null) {
            return;
        }
        
        VisLabel messageLabel = new VisLabel(username + ": " + message);
        chatTable.add(messageLabel).left().row();
        
        // Scroll to bottom
        // TODO: Auto scroll
    }
    
    @Override
    public void render(Stage stage, float delta) {
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        stage.act(delta);
        stage.draw();
    }
}

