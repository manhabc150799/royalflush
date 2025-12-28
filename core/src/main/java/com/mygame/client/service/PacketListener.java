package com.mygame.client.service;

/**
 * Interface for handling network packets and connection events.
 */
public interface PacketListener {
    /**
     * Called when successfully connected to server
     */
    default void onConnected() {}
    
    /**
     * Called when disconnected from server
     */
    default void onDisconnected() {}
    
    /**
     * Called when connection attempt fails
     */
    default void onConnectionFailed(String error) {}
    
    /**
     * Called when any packet is received
     */
    default void onPacketReceived(Object packet) {}
}
