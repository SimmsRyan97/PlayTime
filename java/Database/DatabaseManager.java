// DatabaseManager.java
package com.whiteiverson.minecraft.playtime_plugin.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;

public class DatabaseManager {
    private Connection connection;
    private final FileConfiguration config;
    private final Logger logger;

    public DatabaseManager(FileConfiguration config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public void connect() throws SQLException {
        if (!config.getBoolean("database.enabled")) {
            logger.info("Database support is disabled.");
            return;
        }

        String dbType = config.getString("database.type", "MySQL").toLowerCase();

        try {
            if ("mysql".equals(dbType)) {
                String host = config.getString("database.mysql.host");
                int port = config.getInt("database.mysql.port", 3306);
                String dbName = config.getString("database.mysql.database_name");
                String user = config.getString("database.mysql.username");
                String pass = config.getString("database.mysql.password");

                String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?autoReconnect=" + config.getBoolean("database.database_options.auto_reconnect") + "&connectTimeout=" + config.getInt("database.database_options.connection_timeout") * 1000;
                connection = DriverManager.getConnection(url, user, pass);

            } else if ("sqlite".equals(dbType)) {
                String filePath = config.getString("database.sqlite.file", "playtime_data.db");
                String url = "jdbc:sqlite:" + filePath;
                connection = DriverManager.getConnection(url);
            }

            logger.info("Database connected successfully.");
        } catch (SQLException e) {
            logger.severe("Could not connect to the database: " + e.getMessage());
            throw e;
        }
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