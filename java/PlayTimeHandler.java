package com.whiteiverson.minecraft.playtime_plugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PlayTimeHandler {
    private Main main; // Reference to the main plugin instance
    private UserHandler userHandler;
    private boolean trackAfk; // Whether to track AFK time
    private int autoSaveInterval; // Interval for auto-saving play time data

    public PlayTimeHandler(Main main, UserHandler userHandler) {
        this.main = main; // Initialise the main instance
        this.userHandler = userHandler; // Inject the UserHandler
        loadConfigValues();
    }

    public void enable() {
        startTasks();
    }

    public void disable() {
        // Disable-related cleanups if needed
    }

    private void loadConfigValues() {
        // Load values from config.yml
        this.trackAfk = main.getConfig().getBoolean("playtime.track-afk");
        this.autoSaveInterval = main.getConfig().getInt("playtime.auto-save-interval");
    }

    private void startTasks() {
        // Schedule the main tick task
        Bukkit.getScheduler().scheduleSyncRepeatingTask(main, new TickTask(), 0L, 20L);

        // Schedule the auto-save task
        Bukkit.getScheduler().runTaskTimer(main, this::autoSavePlaytimeData, autoSaveInterval * 20L, autoSaveInterval * 20L);
    }
    
    public void processPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            
            // Load user data for the player
            userHandler.loadUserData(uuid); 
            
            // Update the last active time for AFK tracking
            userHandler.setLastActive(uuid, System.currentTimeMillis());

            // Get playtime from UserHandler
            double playtime = userHandler.getUserConfigValue(uuid, "playtime", 0);

            // If the user has no recorded playtime, retrieve it from world files
            if (playtime == 0) {
                playtime = retrievePlaytimeFromWorldFiles(uuid);
                userHandler.setUserData(uuid, "playtime", playtime); // Save the retrieved playtime
            }

            // Increment playtime only if AFK tracking is disabled or the player is not AFK
            if (!trackAfk || !userHandler.isAfk(uuid)) {
                playtime += 1; // Increment playtime by 1 second
                userHandler.setUserData(uuid, "playtime", playtime); // Update playtime in UserHandler
            }

            // Process rewards for the player based on their updated playtime
            main.getRewardsHandler().processPlayer(player);
            
            // Save the updated user data
            userHandler.saveUserData(uuid);
        }
    }

    // Get play time from server files as initial value
    private double retrievePlaytimeFromWorldFiles(UUID uuid) {
        // Get the first world
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);

        // Return 0 if no world is loaded
        if (world == null) {
            Bukkit.getLogger().warning("No worlds found, unable to retrieve playtime for UUID: " + uuid);
            return 0;
        }

        // Define the path to the player's stats file
        File statsDir = new File(world.getWorldFolder(), "stats");
        File playerFile = new File(statsDir, uuid.toString() + ".json");

        // Check if the file exists before reading
        if (playerFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(playerFile))) {
                // Parse the JSON file
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                // Navigate to the play time within "stats.minecraft:custom.minecraft:play_time"
                if (json.has("stats")) {
                    JsonObject stats = json.getAsJsonObject("stats");
                    if (stats.has("minecraft:custom")) {
                        JsonObject customStats = stats.getAsJsonObject("minecraft:custom");
                        if (customStats.has("minecraft:play_time")) {
                            long playTimeTicks = customStats.get("minecraft:play_time").getAsLong();
                            return playTimeTicks / 20.0; // Convert ticks to seconds
                        }
                    }
                }
            } catch (IOException e) {
                Bukkit.getLogger().severe("Failed to read player stats file for UUID " + uuid + ": " + e.getMessage());
            }
        } else {
            Bukkit.getLogger().warning("Stats file not found for UUID: " + uuid);
        }

        return 0; // Return 0 if the file does not exist or there was an issue
    }

    private void autoSavePlaytimeData() {
        // Logic for auto-saving play time data to database or flat-file
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            double playtime = userHandler.getPlaytime(uuid); // Get play time from UserHandler
            userHandler.setUserData(uuid, "playtime", playtime); // Save the play time to persistent storage
        }
    }

    public boolean hasSufficientPlaytime(UUID uuid, double requiredPlaytime) {
        return userHandler.getPlaytime(uuid) >= requiredPlaytime; // Check if the user has sufficient play time
    }
}