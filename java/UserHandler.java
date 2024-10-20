package com.whiteiverson.minecraft.playtime_plugin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
    private long afkThreshold;

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
    	loadConfigValues();
    }
    
    public void loadConfigValues() {
    	FileConfiguration config = main.getConfig();
    	afkThreshold = config.getLong("track-afk.afk-detection", 300000); // Default to 5 minutes if not set
    }

    /**
     * Loads user data for a specific UUID from the user data file.
     *
     * @param uuid The UUID of the user.
     */
    public void loadUserData(UUID uuid) {
        File userFile = new File(userDataFolder, uuid.toString() + ".yml");
        FileConfiguration userConfig;

        // Load existing config or create a new file
        if (userFile.exists()) {
            userConfig = YamlConfiguration.loadConfiguration(userFile);
        } else {
            try {
                userFile.createNewFile();
                userConfig = YamlConfiguration.loadConfiguration(userFile);
            } catch (IOException e) {
                if (main.getConfig().getBoolean("logging.debug", false)) {
                    main.getLogger().severe(e.getMessage());
                }
                return;
            }
        }

        // Add the username to the config if it's not already set
        if (!userConfig.contains("username")) {
            Player player = Bukkit.getPlayer(uuid);  // Try to get online player

            if (player != null) {
                userConfig.set("username", player.getName());  // Set the online player's name
            } else {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);  // Handle offline player
                userConfig.set("username", offlinePlayer.getName());  // Set the offline player's name
            }
        }

        // Initialise play time if it's not already set
        if (!userConfig.contains("playtime")) {
            userConfig.set("playtime", 0.0);  // Initialise play time
            if (main.getConfig().getBoolean("logging.debug", false)) {
                main.getLogger().info(main.getTranslator().getConsoleTranslation("user.initial") + uuid);
            }
        }

        loadRewardsForUser(userConfig);
        saveUserData(uuid);  // Save the updated user data
        userConfigs.put(uuid, userConfig);  // Cache the config
    }

    private void loadRewardsForUser(FileConfiguration userConfig) {
        FileConfiguration rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);

        // Initialise the rewards section if it doesn't exist
        if (!userConfig.contains("rewards")) {
            userConfig.createSection("rewards");
        }

        // Loop through each reward in the rewards.yml file and initialise to false if not set
        if (rewardsConfig.contains("rewards")) {
            rewardsConfig.getConfigurationSection("rewards").getKeys(false).forEach(reward -> {
                if (!userConfig.contains("rewards." + reward)) {
                    userConfig.set("rewards." + reward, false);
                }
            });
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
            // If the player has played before, return their first played date
            Date joinDate = new Date(offlinePlayer.getFirstPlayed());
            return new SimpleDateFormat("dd.MM.yyyy").format(joinDate);
        } else {
            // If the player hasn't played before, return today's date
            Date currentDate = new Date();
            return new SimpleDateFormat("dd.MM.yyyy").format(currentDate);
        }
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
     * Retrieves user data based on a UUID and a specific key.
     *
     * @param uuid The UUID of the user.
     * @param key  The key for the data.
     * @return The value associated with the key, or null if not found.
     */
    public Object getUserData(UUID uuid, String key) {
        FileConfiguration userConfig = userConfigs.get(uuid);
        if (userConfig == null) {
            loadUserData(uuid);  // Load the user's data if it wasn't loaded
            userConfig = userConfigs.get(uuid);
        }
        return userConfig != null ? userConfig.get(key) : null;
    }

    /**
     * Sets user data for a specific key and UUID.
     *
     * @param uuid  The UUID of the user.
     * @param key   The key to set the data.
     * @param value The value to be set.
     */
    public void setUserData(UUID uuid, String key, Object value) {
        FileConfiguration userConfig = userConfigs.get(uuid);
        if (userConfig == null) {
            loadUserData(uuid);  // Load the user's data if it wasn't loaded
            userConfig = userConfigs.get(uuid);
        }
        userConfig.set(key, value);
        saveUserData(uuid); // Save the user data regardless of whether they're online
    }

    /**
     * Saves the user's data to the corresponding user data file.
     *
     * @param uuid The UUID of the user.
     */
    public void saveUserData(UUID uuid) {
        File userFile = new File(userDataFolder, uuid.toString() + ".yml");
        FileConfiguration userConfig = userConfigs.get(uuid);

        // Check if the userConfig is null
        if (userConfig == null) {
            return;
        }

        try {
            userConfig.save(userFile);
        } catch (IOException e) {
        	if (main.getConfig().getBoolean("logging.debug", false)) {
	            main.getLogger().severe(e.getMessage());
	            e.printStackTrace();
        	}
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
        FileConfiguration userConfig = userConfigs.get(uuid);
        if (userConfig == null) {
            loadUserData(uuid);  // Load the user's data if it wasn't loaded
            userConfig = userConfigs.get(uuid);
        }
        return userConfig != null ? userConfig.getDouble(path, def) : def;
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
        long currentTime = System.currentTimeMillis();
        long lastActiveTime = lastActive.getOrDefault(uuid, 0L);
        
        boolean afkStatus = (currentTime - lastActiveTime) > afkThreshold;
        
        return afkStatus;
    }

    /**
     * Handles player join events, moves, interacts or chats
     *
     * @param event.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        loadUserData(uuid);  // Ensure data is loaded on join
        setLastActive(uuid, System.currentTimeMillis());
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        setLastActive(uuid, System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        setLastActive(uuid, System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        setLastActive(uuid, System.currentTimeMillis());
    }
    
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        saveUserData(uuid);
        lastActive.remove(uuid);
    }
}