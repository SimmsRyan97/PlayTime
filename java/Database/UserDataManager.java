package com.whiteiverson.minecraft.playtime_plugin.Database;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("SqlNoDataSourceInspection")
public class UserDataManager {
    private final DatabaseManager databaseManager;

    public UserDataManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    // Adds or updates a user by UUID
    public void saveUser(String uuid, String username, String joinedDate, double playtime, double afkTime) throws SQLException {
        String query = "INSERT INTO PlayTime_Users (uuid, username, joined, playtime, afk_time) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE username = ?, playtime = ?, afk_time = ?";

        // For SQLite, use different syntax
        if (isSQLite()) {
            query = "INSERT INTO PlayTime_Users (uuid, username, joined, playtime, afk_time) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET username = ?, playtime = ?, afk_time = ?";
        }

        try (PreparedStatement stmt = databaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, uuid);
            stmt.setString(2, username);
            stmt.setString(3, joinedDate);
            stmt.setDouble(4, playtime);
            stmt.setDouble(5, afkTime);

            // Update values
            stmt.setString(6, username);
            stmt.setDouble(7, playtime);
            stmt.setDouble(8, afkTime);

            stmt.executeUpdate();
        }
    }

    // Load user data
    public Map<String, Object> loadUser(String uuid) throws SQLException {
        String query = "SELECT username, joined, playtime, afk_time FROM PlayTime_Users WHERE uuid = ?";
        Map<String, Object> userData = new HashMap<>();

        try (PreparedStatement stmt = databaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                userData.put("username", rs.getString("username"));
                userData.put("joined", rs.getString("joined"));
                userData.put("playtime", rs.getDouble("playtime"));
                userData.put("afk-time", rs.getDouble("afk_time"));
            }
        }

        return userData;
    }

    // Retrieves playtime using UUID
    public double getPlaytime(String uuid) throws SQLException {
        String query = "SELECT playtime FROM PlayTime_Users WHERE uuid = ?";
        try (PreparedStatement stmt = databaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getDouble("playtime") : 0.0;
        }
    }

    // Retrieves AFK time using UUID
    public double getAfkTime(String uuid) throws SQLException {
        String query = "SELECT afk_time FROM PlayTime_Users WHERE uuid = ?";
        try (PreparedStatement stmt = databaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getDouble("afk_time") : 0.0;
        }
    }

    // Retrieves rewards based on UUID
    public Map<String, Boolean> getRewards(String uuid) throws SQLException {
        String query = "SELECT reward_name, claimed FROM PlayTime_Rewards WHERE uuid = ?";
        Map<String, Boolean> rewards = new HashMap<>();

        try (PreparedStatement stmt = databaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                boolean claimed = isSQLite() ? rs.getInt("claimed") == 1 : rs.getBoolean("claimed");
                rewards.put(rs.getString("reward_name"), claimed);
            }
        }

        return rewards;
    }

    // Updates a reward for a specific user
    public void updateReward(String uuid, String rewardName, boolean claimed) throws SQLException {
        String query = "INSERT INTO PlayTime_Rewards (uuid, reward_name, claimed) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE claimed = ?";

        if (isSQLite()) {
            query = "INSERT INTO PlayTime_Rewards (uuid, reward_name, claimed) " +
                    "VALUES (?, ?, ?) " +
                    "ON CONFLICT(uuid, reward_name) DO UPDATE SET claimed = ?";
        }

        try (PreparedStatement stmt = databaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, uuid);
            stmt.setString(2, rewardName);

            if (isSQLite()) {
                stmt.setInt(3, claimed ? 1 : 0);
                stmt.setInt(4, claimed ? 1 : 0);
            } else {
                stmt.setBoolean(3, claimed);
                stmt.setBoolean(4, claimed);
            }

            stmt.executeUpdate();
        }
    }

    // Get all user UUIDs (for /pttop command)
    public java.util.List<String> getAllUserUUIDs() throws SQLException {
        String query = "SELECT uuid FROM PlayTime_Users WHERE playtime > 0 ORDER BY playtime DESC";
        java.util.List<String> uuids = new java.util.ArrayList<>();

        try (PreparedStatement stmt = databaseManager.getConnection().prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                uuids.add(rs.getString("uuid"));
            }
        }

        return uuids;
    }

    private boolean isSQLite() {
        try {
            return databaseManager.getConnection().getMetaData().getDatabaseProductName().equals("SQLite");
        } catch (SQLException e) {
            return false;
        }
    }
}