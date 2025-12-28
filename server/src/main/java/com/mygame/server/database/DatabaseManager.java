package com.mygame.server.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Quản lý kết nối database PostgreSQL và khởi tạo schema
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "5432";
    private static final String DB_NAME = "postgres";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "12212332";

    private static final String POSTGRES_URL = "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/postgres";
    private static final String DB_URL = "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        // Private constructor for singleton
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Tạo database nếu chưa tồn tại
     */
    private void ensureDatabaseExists() throws SQLException {
        try (Connection postgresConn = DriverManager.getConnection(POSTGRES_URL, DB_USER, DB_PASSWORD)) {
            postgresConn.setAutoCommit(true);

            // Kiểm tra database đã tồn tại chưa
            String checkDbSql = "SELECT 1 FROM pg_database WHERE datname = ?";
            try (java.sql.PreparedStatement checkStmt = postgresConn.prepareStatement(checkDbSql)) {
                checkStmt.setString(1, DB_NAME);
                try (java.sql.ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        // Database chưa tồn tại, tạo mới
                        logger.info("Database '{}' chưa tồn tại, đang tạo...", DB_NAME);
                        String createDbSql = "CREATE DATABASE " + DB_NAME;
                        try (Statement createStmt = postgresConn.createStatement()) {
                            createStmt.executeUpdate(createDbSql);
                            logger.info("Đã tạo database '{}' thành công", DB_NAME);
                        } catch (SQLException e) {
                            logger.error("Lỗi khi tạo database '{}': {}", DB_NAME, e.getMessage());
                            throw e;
                        }
                    } else {
                        logger.info("Database '{}' đã tồn tại", DB_NAME);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi khi kiểm tra/tạo database: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Kết nối đến database
     */
    public void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            // Verify we're connected to the correct database
            try {
                String currentDb = connection.getCatalog();
                if (DB_NAME.equals(currentDb)) {
                    logger.info("Đã kết nối đến database: {}", currentDb);
                    return;
                } else {
                    logger.warn("Đang kết nối đến database sai: {} (mong đợi: {}). Đóng kết nối cũ...",
                               currentDb, DB_NAME);
                    connection.close();
                    connection = null;
                }
            } catch (SQLException e) {
                logger.warn("Lỗi khi verify database, đóng kết nối: {}", e.getMessage());
                try {
                    connection.close();
                } catch (SQLException ignored) {}
                connection = null;
            }
        }

        // Đảm bảo database tồn tại trước khi kết nối
        ensureDatabaseExists();

        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            connection.setAutoCommit(false); // Sử dụng transactions

            // Verify đúng database
            String currentDb = connection.getCatalog();
            if (!DB_NAME.equals(currentDb)) {
                connection.close();
                connection = null;
                throw new SQLException("Kết nối đến database sai: " + currentDb + " (mong đợi: " + DB_NAME + ")");
            }

            logger.info("Đã kết nối đến PostgreSQL database: {} (catalog: {})", DB_NAME, currentDb);
        } catch (SQLException e) {
            logger.error("Lỗi kết nối database '{}': {}", DB_NAME, e.getMessage(), e);
            throw new SQLException("Không thể kết nối đến database '" + DB_NAME + "'. " +
                                 "Hãy đảm bảo database đã được tạo. Lỗi: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy connection hiện tại
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        return connection;
    }

    /**
     * Khởi tạo database schema từ file SQL
     */
    public void initDatabase() {
        try {
            logger.info("Đang khởi tạo database schema...");

            // Đọc file schema.sql
            String sql;
            try (InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream("database/schema.sql")) {

                if (inputStream == null) {
                    logger.error("Không tìm thấy file schema.sql");
                    return;
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    sql = reader.lines().collect(Collectors.joining("\n"));
                }
            }

            // Tách các câu lệnh SQL (phân cách bởi ;)
            String[] statements = sql.split(";");

            try (Statement stmt = getConnection().createStatement()) {
                for (String statement : statements) {
                    statement = statement.trim();
                    if (!statement.isEmpty() && !statement.startsWith("--")) {
                        try {
                            stmt.execute(statement);
                            logger.debug("Đã thực thi: {}", statement.substring(0, Math.min(50, statement.length())));
                        } catch (SQLException e) {
                            // Bỏ qua lỗi nếu table đã tồn tại
                            if (!e.getMessage().contains("already exists")) {
                                logger.warn("Lỗi khi thực thi SQL: {}", e.getMessage());
                            }
                        }
                    }
                }
                getConnection().commit();
                logger.info("Đã khởi tạo database schema thành công");
            }
        } catch (Exception e) {
            logger.error("Lỗi khi khởi tạo database: {}", e.getMessage(), e);
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                logger.error("Lỗi rollback: {}", rollbackEx.getMessage());
            }
        }
    }

    /**
     * Đóng kết nối database
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Đã đóng kết nối database");
            }
        } catch (SQLException e) {
            logger.error("Lỗi khi đóng kết nối: {}", e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra kết nối có hoạt động không
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}

