package com.whiteiverson.minecraft.playtime_plugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

/**
 * UserHandler is responsible for managing user data and tracking player activity.
 */
public class UserHandler implements Listener {
    private final Main main;
    private final HashMap<UUID, FileConfiguration> userConfigs = new HashMap<>();
    private final File userDataFolder;
    private final HashMap<UUID, Long> lastActive = new HashMap<>(); // Track last active time
    private final long afkThreshold = 300000; // 5 minutes in milliseconds

    /**
     * Initializes the UserHandler and sets up the user data directory.
     */
    public UserHandler() {
        this.main = Main.getInstance();
        this.userDataFolder = new File(main.getDataFolder(), "data");
        if (!userDataFolder.exists()) {
            userDataFolder.mkdirs(); // Create data directory if it doesn't exist
        }
    }

    public void enable() {
        // Initialisation logic if needed
    }

    public void disable() {
        // Clean up resources if needed
    }

    /**
     * Loads user data for a specific UUID from the user data file.
     *
     * @param uuid The UUID of the user.
     */
    public void loadUserData(UUID uuid) {
        File userFile = new File(userDataFolder, uuid.toString() + ".yml");
        if (userFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(userFile);
            userConfigs.put(uuid, config);
            main.getLogger().info("Loaded user data for " + uuid);
        } else {
            main.getLogger().warning("User data file not found for " + uuid);
        }
    }

    /**
     * Retrieves the user's join date.
     *
     * @param uuid The UUID of the user.
     * @return The join date as a string or "Date not found" if it doesn't exist.
     */
    public String getUserJoinDate(UUID uuid) {
        return getUserConfigValue(uuid, "joinDate", "Date not found");
    }

    /**
     * Retrieves the user's play time.
     *
     * @param uuid The UUID of the user.
     * @return The play time as a double.
     */
    public double getPlaytime(UUID uuid) {
        return getUserConfigValue(uuid, "playtime", 0.0);
    }

    /**
     * A generic method to retrieve user configuration values with a safe cast.
     *
     * @param uuid        The UUID of the user.
     * @param key         The key for the data.
     * @param defaultValue The default value if the key doesn't exist.
     * @param <T>        The type of the value.
     * @return The value associated with the key, or the default value if not found.
     */
    private <T> T getUserConfigValue(UUID uuid, String key, T defaultValue) {
        FileConfiguration config = userConfigs.get(uuid);
        if (config != null) {
            Object value = config.get(key, defaultValue);
            if (value != null && value.getClass().isAssignableFrom(defaultValue.getClass())) {
                return (T) value; // Safe cast
            }
        }
        return defaultValue; // Return the default value if not found or if cast fails
    }

    /**
     * Retrieves user data based on a UUID and a specific key.
     *
     * @param uuid The UUID of the user.
     * @param key  The key for the data.
     * @return The value associated with the key, or null if not found.
     */
    public Object getUserData(UUID uuid, String key) {
        FileConfiguration config = userConfigs.get(uuid);
        return (config != null) ? config.get(key) : null;
    }

    /**
     * Sets user data for a specific key and saves it to the user file.
     *
     * @param uuid  The UUID of the user.
     * @param key   The key for the data.
     * @param value The value to set.
     */
    public void setUserData(UUID uuid, String key, Object value) {
        FileConfiguration config = userConfigs.get(uuid);
        if (config != null) {
            config.set(key, value);
            saveUserData(uuid); // Save after setting the value
        }
    }

    /**
     * Saves user data to the file.
     *
     * @param uuid The UUID of the user.
     */
    private void saveUserData(UUID uuid) {
        File userFile = new File(userDataFolder, uuid.toString() + ".yml");
        FileConfiguration config = userConfigs.get(uuid);
        if (config != null) {
            try {
                config.save(userFile);
                main.getLogger().info("Saved user data for " + uuid);
            } catch (IOException e) {
                main.getLogger().severe("Failed to save user data for " + uuid + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Processes a player by loading their user data and updating their last active time.
     *
     * @param player The player to process.
     */
    public void processPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        loadUserData(uuid); // Load user data when the player is processed
        updateLastActive(uuid); // Update last active time
    }

    /**
     * Updates the last active time for a player.
     *
     * @param uuid The UUID of the user.
     */
    public void updateLastActive(UUID uuid) {
        lastActive.put(uuid, System.currentTimeMillis()); // Update last active timestamp
    }

    /**
     * Checks if the player is AFK based on their last active time.
     *
     * @param uuid The UUID of the user.
     * @return True if the player is AFK, false otherwise.
     */
    public boolean isAfk(UUID uuid) {
        Long lastActiveTime = lastActive.get(uuid);
        return (lastActiveTime != null) && ((System.currentTimeMillis() - lastActiveTime) > afkThreshold);
    }

    /**
     * Listens for player movement events to update their activity.
     *
     * @param event The PlayerMoveEvent.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        updateLastActive(event.getPlayer().getUniqueId()); // Update last active time on movement
    }
}