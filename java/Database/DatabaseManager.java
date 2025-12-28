// DatabaseManager.java
package com.whiteiverson.minecraft.playtime_plugin.Database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;

public class DatabaseManager {
    private Connection connection;
    private final FileConfiguration config;
    private final Logger logger;
    private final File dataFolder;
    private String dbType;

    public DatabaseManager(FileConfiguration config, Logger logger, File dataFolder) {
        this.config = config;
        this.logger = logger;
        this.dataFolder = dataFolder;
    }

    public void connect() throws SQLException {
        if (!config.getBoolean("database.enabled")) {
            logger.info("Database support is disabled.");
            return;
        }

        dbType = config.getString("database.type", "MySQL").toLowerCase();

        try {
            if ("mysql".equals(dbType)) {
                // Check if MySQL driver is available
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException e) {
                    logger.severe("===========================================");
                    logger.severe("MySQL driver not found!");
                    logger.severe("Download: https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar");
                    logger.severe("Place it in: plugins/ folder");
                    logger.severe("Then restart the server.");
                    logger.severe("===========================================");
                    throw new SQLException("MySQL driver not found", e);
                }

                String host = config.getString("database.mysql.host");
                int port = config.getInt("database.mysql.port", 3306);
                String dbName = config.getString("database.mysql.database_name");
                String user = config.getString("database.mysql.username");
                String pass = config.getString("database.mysql.password");

                String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                        + "?autoReconnect=" + config.getBoolean("database.database_options.auto_reconnect")
                        + "&connectTimeout=" + (config.getInt("database.database_options.connection_timeout") * 1000);
                connection = DriverManager.getConnection(url, user, pass);

            } else if ("sqlite".equals(dbType)) {
                // Check if SQLite driver is available
                try {
                    Class.forName("org.sqlite.JDBC");
                } catch (ClassNotFoundException e) {
                    logger.severe("===========================================");
                    logger.severe("SQLite driver not found!");
                    logger.severe("Download: https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.42.0.0/sqlite-jdbc-3.42.0.0.jar");
                    logger.severe("Place it in: plugins/ folder");
                    logger.severe("Then restart the server.");
                    logger.severe("===========================================");
                    throw new SQLException("SQLite driver not found", e);
                }

                String filePath = config.getString("database.sqlite.file", "playtime_data.db");
                File dbFile = new File(dataFolder, filePath);

                String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
                connection = DriverManager.getConnection(url);

                logger.info("SQLite database location: " + dbFile.getAbsolutePath());
            }

            logger.info("Database connected successfully.");
            createTables();
        } catch (SQLException e) {
            logger.severe("Could not connect to the database: " + e.getMessage());
            throw e;
        }
    }

    private void createTables() throws SQLException {
        if (connection == null || connection.isClosed()) {
            return;
        }

        try (Statement stmt = connection.createStatement()) {
            // Create Users table
            if ("mysql".equals(dbType)) {
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS PlayTime_Users (" +
                                "uuid VARCHAR(36) PRIMARY KEY," +
                                "username VARCHAR(16) NOT NULL," +
                                "joined DATE NOT NULL," +
                                "playtime DOUBLE NOT NULL DEFAULT 0," +
                                "afk_time DOUBLE NOT NULL DEFAULT 0" +
                                ")"
                );

                // Create Rewards table
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS PlayTime_Rewards (" +
                                "id INT AUTO_INCREMENT PRIMARY KEY," +
                                "uuid VARCHAR(36) NOT NULL," +
                                "reward_name VARCHAR(255) NOT NULL," +
                                "claimed BOOLEAN NOT NULL DEFAULT FALSE," +
                                "UNIQUE KEY unique_reward (uuid, reward_name)," +
                                "FOREIGN KEY (uuid) REFERENCES PlayTime_Users(uuid) ON DELETE CASCADE" +
                                ")"
                );
            } else if ("sqlite".equals(dbType)) {
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS PlayTime_Users (" +
                                "uuid TEXT PRIMARY KEY," +
                                "username TEXT NOT NULL," +
                                "joined TEXT NOT NULL," +
                                "playtime REAL NOT NULL DEFAULT 0," +
                                "afk_time REAL NOT NULL DEFAULT 0" +
                                ")"
                );

                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS PlayTime_Rewards (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "uuid TEXT NOT NULL," +
                                "reward_name TEXT NOT NULL," +
                                "claimed INTEGER NOT NULL DEFAULT 0," +
                                "UNIQUE(uuid, reward_name)," +
                                "FOREIGN KEY (uuid) REFERENCES PlayTime_Users(uuid) ON DELETE CASCADE" +
                                ")"
                );
            }

            logger.info("Database tables created successfully.");
        }
    }

    public boolean isEnabled() {
        return config.getBoolean("database.enabled", false);
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.severe("Error closing the database connection: " + e.getMessage());
        }
    }
}