package com.whiteiverson.minecraft.playtime_plugin.Database;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class UserDataManager {
    private final DatabaseManager databaseManager;

    public UserDataManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    // Adds or updates a user by UUID, setting playtime and updating username if it changes
    public void addUser(String uuid, String username, Date joined, double playtime) throws SQLException {
        String query = "INSERT INTO Users (uuid, username, joined, playtime) " +
                       "VALUES (?, ?, ?, ?) " +
                       "ON DUPLICATE KEY UPDATE username = ?, playtime = ?";
        
        try (PreparedStatement stmt = databaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, uuid);
            stmt.setString(2, username);
            stmt.setDate(3, joined);
            stmt.setDouble(4, playtime);

            // Update values if a record with the same UUID exists
            stmt.setString(5, username);  // Update username if it has changed
            stmt.setDouble(6, playtime);  // Update playtime
            
            stmt.executeUpdate();
        }
    }

    // Retrieves playtime using UUID instead of username for accuracy
    public double getPlaytime(String uuid) throws SQLException {
        String query = "SELECT playtime FROM Users WHERE uuid = ?";
        try (PreparedStatement stmt = databaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getDouble("playtime") : 0.0;
        }
    }

    // Retrieves rewards based on UUID instead of username, returning a map of rewards
    public Map<String, Boolean> getRewards(String uuid) throws SQLException {
        String query = "SELECT reward_name, claimed FROM Rewards WHERE uuid = ?";
        Map<String, Boolean> rewards = new HashMap<>();

        try (PreparedStatement stmt = databaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                rewards.put(rs.getString("reward_name"), rs.getBoolean("claimed"));
            }
        }

        return rewards;
    }

    // Updates a reward for a specific user identified by UUID
    public void updateReward(String uuid, String rewardName, boolean claimed) throws SQLException {
        String query = "INSERT INTO Rewards (uuid, reward_name, claimed) " +
                       "VALUES (?, ?, ?) " +
                       "ON DUPLICATE KEY UPDATE claimed = ?";
        
        try (PreparedStatement stmt = databaseManager.getConnection().prepareStatement(query)) {
            stmt.setString(1, uuid);
            stmt.setString(2, rewardName);
            stmt.setBoolean(3, claimed);
            stmt.setBoolean(4, claimed);  // Update claimed status if reward already exists
            stmt.executeUpdate();
        }
    }
}