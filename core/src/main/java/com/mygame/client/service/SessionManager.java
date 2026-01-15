package com.mygame.client.service;

import com.github.czyzby.autumn.annotation.Component;
import com.mygame.shared.model.GameType;
import com.mygame.shared.model.PlayerProfile;
import com.mygame.shared.model.RoomInfo;

/**
 * SessionManager to store current player session data.
 * Managed by Autumn MVC as a singleton component.
 * Use @Inject to get instance instead of getInstance().
 */
@Component
public class SessionManager {
    private PlayerProfile playerProfile;
    private boolean isConnected = false;
    private GameType pendingGameType;
    private RoomInfo pendingRoomInfo;

    // Public default constructor for Autumn MVC
    public SessionManager() {
    }

    // Backward compatibility: getInstance() returns this instance
    // Note: This should only be used if @Inject is not available
    // Prefer using @Inject in components managed by Autumn MVC
    public static SessionManager getInstance() {
        // This will be set by Autumn MVC context
        // For now, return a new instance if needed (fallback)
        // In practice, use @Inject instead
        return new SessionManager();
    }

    public void setPlayerProfile(PlayerProfile profile) {
        this.playerProfile = profile;
    }

    public PlayerProfile getPlayerProfile() {
        return playerProfile;
    }

    public PlayerProfile getCurrentPlayer() {
        return playerProfile;
    }

    public int getCurrentUserId() {
        return playerProfile != null ? playerProfile.id : -1;
    }

    public void setConnected(boolean connected) {
        this.isConnected = connected;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void logout() {
        clear();
    }

    public GameType getAndClearPendingGameType() {
        GameType result = pendingGameType;
        pendingGameType = null;
        return result;
    }

    public void setPendingGameType(GameType gameType) {
        this.pendingGameType = gameType;
    }

    public RoomInfo getAndClearPendingRoomInfo() {
        RoomInfo result = pendingRoomInfo;
        pendingRoomInfo = null;
        return result;
    }

    public void setPendingRoomInfo(RoomInfo roomInfo) {
        this.pendingRoomInfo = roomInfo;
    }

    // Pending room ID for game screen transition
    private int pendingRoomId = -1;

    public void setPendingRoomId(int roomId) {
        this.pendingRoomId = roomId;
    }

    public int getPendingRoomId() {
        return pendingRoomId;
    }

    public void clear() {
        playerProfile = null;
        isConnected = false;
        pendingGameType = null;
        pendingRoomInfo = null;
        pendingRoomId = -1;
    }
}
