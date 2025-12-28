package com.mygame.server;

import com.esotericsoftware.kryonet.Server;
import com.mygame.server.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ServerLauncher {
    private static final Logger logger = LoggerFactory.getLogger(ServerLauncher.class);

    private static final int TCP_PORT = 54555;
    private static final int UDP_PORT = 54777;

    private Server server;
    private DatabaseManager dbManager;

    // --- HÀM MAIN ---
    public static void main(String[] args) {
        System.out.println("\n>>> BẮT ĐẦU KHỞI ĐỘNG SERVER...\n");

        ServerLauncher launcher = new ServerLauncher();

        // Hook tắt server an toàn
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n!!! SERVER ĐANG TẮT !!!");
            launcher.stop();
        }));

        // Thử khởi động
        launcher.start();

        // --- QUAN TRỌNG: Vòng lặp giữ server sống ---
        System.out.println(">>> SERVER ĐANG CHẠY CHẾ ĐỘ KEEP-ALIVE. Đừng tắt cửa sổ này!");
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void start() {
        try {
            // 1. Kết nối Database
            logger.info("Step 1/4: Connecting to database...");
            dbManager = DatabaseManager.getInstance();
            dbManager.connect();
            logger.info("✓ Database connected");

            logger.info("Step 2/4: Initializing schema...");
            dbManager.initDatabase();
            logger.info("✓ Schema initialized");

            // 2. Khởi tạo Mạng
            logger.info("Step 3/4: Starting network server...");
            server = new Server(16384, 8192);
            com.mygame.shared.network.Network.registerPackets(server.getKryo());

            logger.info("Step 4/4: Setting up listeners...");
            ServerListener serverListener = new ServerListener(dbManager);
            server.addListener(serverListener.createListener());

            // 3. Mở cổng
            server.bind(TCP_PORT, UDP_PORT);
            server.start();

            logger.info("\n========================================");
            logger.info("✅ SERVER STARTED SUCCESSFULLY ON PORT " + TCP_PORT);
            logger.info("========================================\n");

        } catch (Exception e) {
            // NẾU CÓ LỖI: In ra màn hình nhưng KHÔNG tắt server ngay
            logger.error("\n\n❌❌❌ LỖI KHỞI ĐỘNG SERVER ❌❌❌");
            logger.error("Nguyên nhân: " + e.getMessage());
            e.printStackTrace();
            logger.error("========================================\n");
        }
    }

    public void stop() {
        if (server != null) server.stop();
        if (dbManager != null) dbManager.close();
    }
}
