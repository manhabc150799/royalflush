package com.mygame.shared.network.packets;

/**
 * Response to StartGameRequest.
 * If success, all clients receive GameStartPacket to start the game.
 */
public class StartGameResponse {
    private boolean success;
    private String errorMessage;

    // Default constructor for Kryo
    public StartGameResponse() {
    }

    public StartGameResponse(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
