package com.whiteiverson.minecraft.playtime_plugin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

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
    private final HashMap<UUID, Long> lastActive = new HashMap<>();
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
    	//TODO Add enabling of tasks if needed
    }
    
    public void disable() {
    	//TODO Add disabling of tasks if needed
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
                userFile.createNewFile();
                userConfig = YamlConfiguration.loadConfiguration(userFile);
            } catch (IOException e) {
                main.getLogger().severe("Failed to create user data file for " + uuid + ": " + e.getMessage());
                return;
            }
        }

        // Ensure playtime is present in the config
        if (!userConfig.contains("playtime")) {
            userConfig.set("playtime", 0.0); // Initialise playtime if not present
        }

        loadRewardsForUser(userConfig);

        saveUserData(uuid);
        userConfigs.put(uuid, userConfig);
    }

    private void loadRewardsForUser(FileConfiguration userConfig) {
        FileConfiguration rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);

        // Initialise the rewards section if it doesn't exist
        if (!userConfig.contains("rewards")) {
            userConfig.createSection("rewards");
        }

        // Loop through each reward in the rewards.yml file and initialise to false if not set
        rewardsConfig.getConfigurationSection("rewards").getKeys(false).forEach(reward -> {
            if (!userConfig.contains("rewards." + reward)) {
                userConfig.set("rewards." + reward, false);
            }
        });
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
            Date joinDate = new Date(offlinePlayer.getFirstPlayed());
            return new SimpleDateFormat("dd.MM.yyyy").format(joinDate);
        }
        return "Date not found"; // Return if player never played
    }

    /**
     * Retrieves the user's playtime.
     *
     * @param uuid The UUID of the user.
     * @return The playtime as a double.
     */
    public double getPlaytime(UUID uuid) {
        return getUserConfigValue(uuid, "playtime", 0.0);
    }

    /**
     * Retrieves user data based on a UUID and a specific key.
     *
     * @param uuid The UUID of the user.
     * @param key  The key for the data.
     * @return The value associated with the key, or null if not found.
     */
    public Object getUserData(UUID uuid, String key) {
        return userConfigs.get(uuid).get(key);
    }

    /**
     * Sets user data for a specific key and UUID.
     *
     * @param uuid  The UUID of the user.
     * @param key   The key to set the data.
     * @param value The value to be set.
     */
    public void setUserData(UUID uuid, String key, Object value) {
        userConfigs.get(uuid).set(key, value);
        saveUserData(uuid);
    }

    /**
     * Saves the user's data to the corresponding user data file.
     *
     * @param uuid The UUID of the user.
     */
    public void saveUserData(UUID uuid) {
        File userFile = new File(userDataFolder, uuid.toString() + ".yml");
        FileConfiguration userConfig = userConfigs.get(uuid);

        try {
            userConfig.save(userFile);
        } catch (IOException e) {
            main.getLogger().severe("Failed to save user data for " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Retrieves a configuration value from a user's data.
     *
     * @param uuid The UUID of the user.
     * @param path The key path in the config.
     * @param def  The default value if the key does not exist.
     * @return The value associated with the path, or the default value if not found.
     */
    public double getUserConfigValue(UUID uuid, String path, double def) {
        return userConfigs.get(uuid).getDouble(path, def);
    }

    /**
     * Tracks the last active time for the player.
     *
     * @param uuid       The UUID of the player.
     * @param activeTime The time to be tracked as the last active time.
     */
    public void setLastActive(UUID uuid, long activeTime) {
        lastActive.put(uuid, activeTime);
    }

    /**
     * Checks whether the player is considered AFK based on their last activity.
     *
     * @param uuid The UUID of the player.
     * @return True if the player is AFK, false otherwise.
     */
    public boolean isAfk(UUID uuid) {
        return (System.currentTimeMillis() - lastActive.getOrDefault(uuid, 0L)) > afkThreshold;
    }
}