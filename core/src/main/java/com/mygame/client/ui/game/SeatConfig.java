package com.mygame.client.ui.game;

/**
 * SeatConfig - Hardcoded seat positions for 5 players.
 * 
 * Base resolution: 1280x720
 * Positions scale proportionally with screen size.
 * 
 * Seat Layout:
 * [2] [3]
 * Top-Left Top-Right
 * 
 * [1] [4]
 * Bot-Left Bot-Right
 * 
 * [0] - USER (hidden, uses HUD)
 */
public class SeatConfig {

    public static final int MAX_SEATS = 5;

    // Base resolution for coordinate calculation
    public static final float BASE_WIDTH = 1280f;
    public static final float BASE_HEIGHT = 720f;

    /**
     * Absolute coordinates for each seat (at base resolution 1280x720).
     * Seat 0 is the user (rendered separately in HUD).
     * Format: [seatIndex] = {x, y}
     */
    public static final float[][] SEAT_COORDS = {
            { 640, 80 }, // Seat 0: User (bottom center) - hidden, uses HUD
            { 100, 300 }, // Seat 1: Bottom-Left
            { 300, 550 }, // Seat 2: Top-Left
            { 980, 550 }, // Seat 3: Top-Right
            { 1180, 300 } // Seat 4: Bottom-Right
    };

    /**
     * Get visual seat index for a player.
     * Local player is always index 0 (uses HUD instead of seat).
     */
    public static int getVisualIndex(int serverIndex, int localIndex, int playerCount) {
        if (playerCount <= 0)
            return 0;
        return (serverIndex - localIndex + playerCount) % playerCount;
    }

    /**
     * Get X coordinate scaled to current screen width.
     */
    public static float getX(int seatIndex, float screenWidth) {
        if (seatIndex < 0 || seatIndex >= MAX_SEATS)
            seatIndex = 1;
        float scale = screenWidth / BASE_WIDTH;
        return SEAT_COORDS[seatIndex][0] * scale;
    }

    /**
     * Get Y coordinate scaled to current screen height.
     */
    public static float getY(int seatIndex, float screenHeight) {
        if (seatIndex < 0 || seatIndex >= MAX_SEATS)
            seatIndex = 1;
        float scale = screenHeight / BASE_HEIGHT;
        return SEAT_COORDS[seatIndex][1] * scale;
    }

    /**
     * Check if seat is for local player (uses HUD instead).
     */
    public static boolean isUserSeat(int visualIndex) {
        return visualIndex == 0;
    }
}
