package com.mygame.client.service;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.github.czyzby.autumn.annotation.Component;
import com.github.czyzby.autumn.annotation.Initiate;
import com.mygame.shared.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optimized NetworkService with auto-reconnect, packet queuing, and connection management.
 * Senior-level implementation with proper error handling and thread safety.
 * 
 * Auto-connects to localhost on initialization for development/testing.
 */
@Component
public class NetworkService {
    private static final Logger logger = LoggerFactory.getLogger(NetworkService.class);
    
    // Server configuration (localhost for development)
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_TCP_PORT = 54555;
    private static final int DEFAULT_UDP_PORT = 54777;
    
    private static final int CONNECT_TIMEOUT = 5000; // 5 seconds
    private static final int RECONNECT_DELAY = 3000; // 3 seconds
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int PACKET_QUEUE_SIZE = 100;
    
    private Client client;
    private final List<PacketListener> listeners = new ArrayList<>();
    private final BlockingQueue<Object> packetQueue = new LinkedBlockingQueue<>(PACKET_QUEUE_SIZE);
    
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    
    private String host;
    private int tcpPort;
    private int udpPort;
    
    private Thread reconnectThread;
    private Thread packetSenderThread;
    
    public NetworkService() {
        initializeClient();
    }
    
    /**
     * Auto-connect to server on initialization
     * This is called automatically by Autumn MVC after all components are initialized
     */
    @Initiate
    private void autoConnect() {
        logger.info("NetworkService initialized, attempting auto-connect to {}:{}", DEFAULT_HOST, DEFAULT_TCP_PORT);
        // Small delay to ensure client thread is fully started
        new Thread(() -> {
            try {
                Thread.sleep(500); // Give client thread time to start
                connect(DEFAULT_HOST, DEFAULT_TCP_PORT, DEFAULT_UDP_PORT);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Auto-connect interrupted");
            }
        }, "AutoConnect").start();
    }
    
    private void initializeClient() {
        client = new Client(16384, 8192); // Write buffer: 16KB, Object buffer: 8KB
        Network.registerPackets(client.getKryo());
        
        client.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                logger.info("Connected to server: {}", connection.getRemoteAddressTCP());
                isConnected.set(true);
                isConnecting.set(false);
                reconnectAttempts.set(0);
                
                // Process queued packets
                startPacketSender();
                
                // Notify listeners
                for (PacketListener listener : listeners) {
                    try {
                        listener.onConnected();
                    } catch (Exception e) {
                        logger.error("Error in connection listener", e);
                    }
                }
            }
            
            @Override
            public void disconnected(Connection connection) {
                logger.warn("Disconnected from server");
                isConnected.set(false);
                stopPacketSender();
                
                // Notify listeners
                for (PacketListener listener : listeners) {
                    try {
                        listener.onDisconnected();
                    } catch (Exception e) {
                        logger.error("Error in disconnection listener", e);
                    }
                }
                
                // Attempt auto-reconnect if not manually disconnected
                if (host != null && reconnectAttempts.get() < MAX_RECONNECT_ATTEMPTS) {
                    scheduleReconnect();
                }
            }
            
            @Override
            public void received(Connection connection, Object object) {
                handleReceivedPacket(object);
            }
        });
        
        // Start client update thread
        client.start();
    }
    
    /**
     * Connect to server with optimized connection handling
     */
    public void connect(String host, int tcpPort, int udpPort) {
        if (isConnecting.get() || isConnected.get()) {
            logger.warn("Already connected or connecting");
            return;
        }
        
        this.host = host;
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        reconnectAttempts.set(0);
        
        connectInternal();
    }
    
    private void connectInternal() {
        if (isConnecting.getAndSet(true)) {
            logger.debug("Already connecting, skipping duplicate connection attempt");
            return;
        }
        
        new Thread(() -> {
            try {
                logger.info("Connecting to {}:{} (TCP), {} (UDP)", host, tcpPort, udpPort);
                client.connect(CONNECT_TIMEOUT, host, tcpPort, udpPort);
                // Connection successful - the connected() callback will be called
                logger.info("Connection established successfully");
            } catch (IOException e) {
                logger.error("Failed to connect to server: {}", e.getMessage());
                isConnecting.set(false);
                isConnected.set(false);
                
                // Notify listeners of connection failure
                for (PacketListener listener : listeners) {
                    try {
                        listener.onConnectionFailed(e.getMessage());
                    } catch (Exception ex) {
                        logger.error("Error in connection failure listener", ex);
                    }
                }
                
                // Schedule reconnect
                if (reconnectAttempts.incrementAndGet() < MAX_RECONNECT_ATTEMPTS) {
                    logger.info("Scheduling reconnect attempt {}", reconnectAttempts.get());
                    scheduleReconnect();
                } else {
                    logger.error("Max reconnect attempts ({}) reached. Please check if server is running.", MAX_RECONNECT_ATTEMPTS);
                }
            } catch (Exception e) {
                logger.error("Unexpected error during connection", e);
                isConnecting.set(false);
                isConnected.set(false);
            }
        }, "NetworkConnect").start();
    }
    
    private void scheduleReconnect() {
        if (reconnectThread != null && reconnectThread.isAlive()) {
            return;
        }
        
        reconnectThread = new Thread(() -> {
            try {
                Thread.sleep(RECONNECT_DELAY);
                logger.info("Attempting reconnect (attempt {})", reconnectAttempts.get());
                connectInternal();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "NetworkReconnect");
        reconnectThread.start();
    }
    
    /**
     * Send packet with queuing support for offline mode
     */
    public void sendPacket(Object packet) {
        if (packet == null) {
            logger.warn("Attempted to send null packet");
            return;
        }
        
        if (isConnected.get()) {
            try {
                client.sendTCP(packet);
                logger.debug("Sent packet: {}", packet.getClass().getSimpleName());
            } catch (Exception e) {
                logger.error("Failed to send packet", e);
                // Queue packet for retry
                queuePacket(packet);
            }
        } else {
            // Queue packet if not connected
            queuePacket(packet);
        }
    }
    
    private void queuePacket(Object packet) {
        if (!packetQueue.offer(packet)) {
            logger.warn("Packet queue full, dropping packet: {}", packet.getClass().getSimpleName());
        }
    }
    
    private void startPacketSender() {
        if (packetSenderThread != null && packetSenderThread.isAlive()) {
            return;
        }
        
        packetSenderThread = new Thread(() -> {
            while (isConnected.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    Object packet = packetQueue.take();
                    if (isConnected.get()) {
                        client.sendTCP(packet);
                        logger.debug("Sent queued packet: {}", packet.getClass().getSimpleName());
                    } else {
                        // Re-queue if disconnected
                        packetQueue.offer(packet);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error sending queued packet", e);
                }
            }
        }, "PacketSender");
        packetSenderThread.setDaemon(true);
        packetSenderThread.start();
    }
    
    private void stopPacketSender() {
        if (packetSenderThread != null) {
            packetSenderThread.interrupt();
        }
    }
    
    private void handleReceivedPacket(Object packet) {
        logger.debug("Received packet: {}", packet.getClass().getSimpleName());
        
        for (PacketListener listener : listeners) {
            try {
                listener.onPacketReceived(packet);
            } catch (Exception e) {
                logger.error("Error handling packet in listener", e);
            }
        }
    }
    
    public void addListener(PacketListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    public void addPacketListener(java.util.function.Consumer<Object> packetConsumer) {
        addListener(new PacketListener() {
            @Override
            public void onPacketReceived(Object packet) {
                packetConsumer.accept(packet);
            }
        });
    }
    
    public void removeListener(PacketListener listener) {
        listeners.remove(listener);
    }
    
    public void disconnect() {
        logger.info("Disconnecting from server");
        host = null; // Prevent auto-reconnect
        reconnectAttempts.set(MAX_RECONNECT_ATTEMPTS);
        stopPacketSender();
        
        if (client != null) {
            client.close();
        }
        
        isConnected.set(false);
        isConnecting.set(false);
        packetQueue.clear();
    }
    
    public boolean isConnected() {
        return isConnected.get() && client != null && client.isConnected();
    }
    
    public boolean isConnecting() {
        return isConnecting.get();
    }
    
    public void dispose() {
        disconnect();
        if (client != null) {
            client.stop();
        }
        listeners.clear();
    }
}
