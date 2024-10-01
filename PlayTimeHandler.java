package com.whiteiverson.minecraft.playtime_plugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

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
            userHandler.loadUserData(uuid); // Load user data for the player
            userHandler.updateLastActive(uuid); // Update last active time

            double playtime = userHandler.getPlaytime(uuid); // Get play time from UserHandler

            // Check if the user file exists
            if (playtime == 0) {
                // If the file doesn't exist, retrieve playtime from world files
                playtime = retrievePlaytimeFromWorldFiles(uuid);
                userHandler.setUserData(uuid, "playtime", playtime); // Save the retrieved playtime
            }

            if (!trackAfk || !userHandler.isAfk(uuid)) { // Check if tracking AFK time is enabled
                playtime += 1; // Increment play time
                userHandler.setUserData(uuid, "playtime", playtime); // Update play time in UserHandler
            }

            main.getRewardsHandler().processPlayer(player); // Process rewards for the player
        }
    }

    private double retrievePlaytimeFromWorldFiles(UUID uuid) {
        // Define the path to the player data file
        File worldDir = new File(main.getServer().getWorlds().get(0).getWorldFolder(), "playerdata");
        File playerFile = new File(worldDir, uuid.toString() + ".dat");

        if (playerFile.exists()) {
            try (FileInputStream fis = new FileInputStream(playerFile)) {
                CompoundTag nbtData = NbtIo.readCompressed(fis);

                // Retrieve playtime from the appropriate tag
                if (nbtData.contains("PlayTime")) {
                    long playTime = nbtData.getLong("PlayTime"); // Playtime is usually in ticks
                    return playTime / 20.0; // Convert ticks to seconds (1 tick = 1/20 seconds)
                }
            } catch (IOException e) {
                main.getLogger().severe("Failed to read player data file for UUID " + uuid + ": " + e.getMessage());
            }
        }

        return 0; // Return 0 if file does not exist or if unable to retrieve playtime
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