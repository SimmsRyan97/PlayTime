package com.whiteiverson.minecraft.playtime_plugin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

/**
 * UserHandler is responsible for managing user data and tracking player activity.
 */
public class UserHandler implements Listener {
    private final Main main;
    private final HashMap<UUID, FileConfiguration> userConfigs = new HashMap<>();
    private final File userDataFolder;
    private final File rewardsFile;
    private final HashMap<UUID, Long> lastActive = new HashMap<>(); // Track last active time
    private final long afkThreshold = 300000; // 5 minutes in milliseconds

    /**
     * Initialises the UserHandler and sets up the user data directory.
     */
    public UserHandler() {
        this.main = Main.getInstance();
        this.userDataFolder = new File(main.getDataFolder(), "data");
        this.rewardsFile = new File(main.getDataFolder(), "rewards.yml");
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
        FileConfiguration userConfig;

        if (userFile.exists()) {
            userConfig = YamlConfiguration.loadConfiguration(userFile);
        } else {
            try {
                // Create the file if it doesn't exist
                userFile.createNewFile();

                // Initialise the configuration
                userConfig = YamlConfiguration.loadConfiguration(userFile);
            } catch (IOException e) {
                main.getLogger().severe("Failed to create user data file for " + uuid + ": " + e.getMessage());
                return;
            }
        }

        // Ensure play time is present in the config
        if (!userConfig.contains("playtime")) {
            userConfig.set("playtime", 0.0); // Initialise play time if not present
        }

        // Load and initialise rewards for the user
        loadRewardsForUser(userConfig);

        // Save the updated user config to the file
        saveUserData(uuid);

        // Store the user config in the userConfigs map
        userConfigs.put(uuid, userConfig);
    }

    private void loadRewardsForUser(FileConfiguration userConfig) {
        FileConfiguration rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);

        // Initialise the rewards section if it doesn't exist
        if (!userConfig.contains("rewards")) {
            userConfig.createSection("rewards");
        }

        // Loop through each reward in the rewards.yml file and initialise to false if not set
        for (String reward : rewardsConfig.getConfigurationSection("rewards").getKeys(false)) {
            if (!userConfig.contains("rewards." + reward)) {
                userConfig.set("rewards." + reward, false);
            }
        }
    }

    /**
     * Retrieves the user's join date.
     *
     * @param uuid The UUID of the user.
     * @return The join date as a string or "Date not found" if it doesn't exist.
     */
    public String getUserJoinDate(UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        if (offlinePlayer.hasPlayedBefore()) {
            long firstPlayedMillis = offlinePlayer.getFirstPlayed();
            Date joinDate = new Date(firstPlayedMillis);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
            return dateFormat.format(joinDate); // Return formatted date as String
        }
        return "Date not found"; // Return a message if the player has never played before
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
     * @param uuid         The UUID of the user.
     * @param key          The key for the data.
     * @param defaultValue The default value if the key doesn't exist.
     * @param <T>         The type of the value.
     * @return The value associated with the key, or the default value if not found.
     */
    @SuppressWarnings("unchecked")
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
                config.save(userFile); // Save the updated config to the user's data file
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

    /**
     * Update the player's total play time in their .yml file.
     *
     * @param uuid     The player's UUID.
     * @param playtime The new play time value to set.
     */
    public void updatePlaytime(UUID uuid, double playtime) {
        setUserData(uuid, "playtime", playtime); // Setting playtime in the config
    }
}