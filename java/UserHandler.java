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
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

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

    private boolean useDatabaseStorage() {
        return main.getDatabaseManager() != null && main.getDatabaseManager().isEnabled();
    }
    
    public void loadConfigValues() {
    	FileConfiguration config = main.getConfig();
    	afkThreshold = config.getLong("track-afk.afk-detection", 300000); // Default to 5 minutes if not set
    }

    public void loadUserData(UUID uuid) {
        if (useDatabaseStorage()) {
            if (main.getUserDataManager() == null) {
                // Database is enabled but UserDataManager is null - connection failed
                main.getLogger().severe("Database is enabled but connection failed. Falling back to file storage for " + uuid);
                // Force disable database for this session
                main.getConfig().set("database.enabled", false);
                loadUserDataFromFile(uuid);
                return;
            }
            loadUserDataFromDatabase(uuid);
        } else {
            loadUserDataFromFile(uuid);
        }
    }

    private void loadUserDataFromDatabase(UUID uuid) {
        try {
            Map<String, Object> userData = main.getUserDataManager().loadUser(uuid.toString());

            if (userData.isEmpty()) {
                // User doesn't exist in database, create new entry
                Player player = Bukkit.getPlayer(uuid);
                String username = player != null ? player.getName() : Bukkit.getOfflinePlayer(uuid).getName();
                String joinDate = setUserJoinDate(uuid);

                main.getUserDataManager().saveUser(uuid.toString(), username, joinDate, 0.0, 0.0);

                // Load rewards
                FileConfiguration rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);
                if (rewardsConfig.contains("rewards")) {
                    for (String reward : Objects.requireNonNull(rewardsConfig.getConfigurationSection("rewards")).getKeys(false)) {
                        String rewardName = rewardsConfig.getString("rewards." + reward + ".name");
                        main.getUserDataManager().updateReward(uuid.toString(), rewardName, false);
                    }
                }
            }
        } catch (SQLException e) {
            if (main.getConfig().getBoolean("logging.debug", false)) {
                main.getLogger().severe("Failed to load user data from database: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void loadUserDataFromFile(UUID uuid) {
        // Your existing file-based loading code
        File userFile = new File(userDataFolder, uuid.toString() + ".yml");
        FileConfiguration userConfig;

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

        if (!userConfig.contains("username")) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                userConfig.set("username", player.getName());
            } else {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                userConfig.set("username", offlinePlayer.getName());
            }
        }

        if (!userConfig.contains("joined")) {
            String joinDate = setUserJoinDate(uuid);
            userConfig.set("joined", joinDate);
        }

        if (!userConfig.contains("playtime")) {
            userConfig.set("playtime", 0.0);
            if (main.getConfig().getBoolean("logging.debug", false)) {
                main.getLogger().info(main.getTranslator().getTranslation("user.initial", null) + uuid);
            }
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

        // Initialise the claimed subsection if it doesn't exist
        if (!userConfig.contains("rewards.claimed")) {
            userConfig.createSection("rewards.claimed");
        }

        // Loop through each reward in the rewards.yml file and add the name to the claimed section if not set
        if (rewardsConfig.contains("rewards")) {
            Objects.requireNonNull(rewardsConfig.getConfigurationSection("rewards")).getKeys(false).forEach(reward -> {
                String rewardName = rewardsConfig.getString("rewards." + reward + ".name");
                if (!userConfig.contains("rewards.claimed." + rewardName)) {
                    userConfig.set("rewards.claimed." + rewardName, false);  // Set initial value to false
                }
            });
        }
    }
    
    /**
     * Determines the join date based on the player's first played date or today's date if never played before.
     *
     * @param uuid The UUID of the player.
     * @return The join date as a string in dd.MM.yyyy format.
     */
    private String setUserJoinDate(UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        if (offlinePlayer.hasPlayedBefore()) {
            Date joinDate = new Date(offlinePlayer.getFirstPlayed());
            return new SimpleDateFormat("dd.MM.yyyy").format(joinDate);
        } else {
            Date currentDate = new Date();
            return new SimpleDateFormat("dd.MM.yyyy").format(currentDate);
        }
    }

    /**
     * Retrieves the user's join date from their config data.
     *
     * @param uuid The UUID of the user.
     * @return The join date as a string.
     */
    public String getUserJoinDate(UUID uuid) {
        if (useDatabaseStorage()) {
            try {
                Map<String, Object> userData = main.getUserDataManager().loadUser(uuid.toString());
                return (String) userData.getOrDefault("joined", "Unknown");
            } catch (SQLException e) {
                if (main.getConfig().getBoolean("logging.debug", false)) {
                    main.getLogger().severe("Failed to get join date from database: " + e.getMessage());
                }
                return "Unknown";
            }
        } else {
            FileConfiguration userConfig = userConfigs.get(uuid);
            if (userConfig == null) {
                loadUserData(uuid);
                userConfig = userConfigs.get(uuid);
            }
            return userConfig.getString("joined", "Unknown");
        }
    }

    public double getPlaytime(UUID uuid) {
        if (useDatabaseStorage()) {
            try {
                return main.getUserDataManager().getPlaytime(uuid.toString());
            } catch (SQLException e) {
                if (main.getConfig().getBoolean("logging.debug", false)) {
                    main.getLogger().severe("Failed to get playtime from database: " + e.getMessage());
                }
                return 0.0;
            }
        } else {
            return getUserConfigValue(uuid, "playtime", 0.0);
        }
    }

    public Object getUserData(UUID uuid, String key) {
        if (useDatabaseStorage()) {
            try {
                Map<String, Object> userData = main.getUserDataManager().loadUser(uuid.toString());
                return userData.get(key);
            } catch (SQLException e) {
                if (main.getConfig().getBoolean("logging.debug", false)) {
                    main.getLogger().severe("Failed to get user data from database: " + e.getMessage());
                }
                return null;
            }
        } else {
            FileConfiguration userConfig = userConfigs.get(uuid);
            if (userConfig == null) {
                loadUserData(uuid);
                userConfig = userConfigs.get(uuid);
            }
            return userConfig != null ? userConfig.get(key) : null;
        }
    }

    public void setUserData(UUID uuid, String key, Object value) {
        if (useDatabaseStorage()) {
            try {
                if (key.equals("playtime")) {
                    Map<String, Object> userData = main.getUserDataManager().loadUser(uuid.toString());
                    String username = (String) userData.getOrDefault("username", Bukkit.getOfflinePlayer(uuid).getName());
                    String joinDate = (String) userData.getOrDefault("joined", setUserJoinDate(uuid));
                    double afkTime = (double) userData.getOrDefault("afk-time", 0.0);

                    main.getUserDataManager().saveUser(uuid.toString(), username, joinDate, (double) value, afkTime);
                } else if (key.equals("afk-time")) {
                    Map<String, Object> userData = main.getUserDataManager().loadUser(uuid.toString());
                    String username = (String) userData.getOrDefault("username", Bukkit.getOfflinePlayer(uuid).getName());
                    String joinDate = (String) userData.getOrDefault("joined", setUserJoinDate(uuid));
                    double playtime = (double) userData.getOrDefault("playtime", 0.0);

                    main.getUserDataManager().saveUser(uuid.toString(), username, joinDate, playtime, (double) value);
                } else if (key.startsWith("rewards.claimed.")) {
                    String rewardName = key.substring("rewards.claimed.".length());
                    main.getUserDataManager().updateReward(uuid.toString(), rewardName, (boolean) value);
                }
            } catch (SQLException e) {
                if (main.getConfig().getBoolean("logging.debug", false)) {
                    main.getLogger().severe("Failed to set user data in database: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            FileConfiguration userConfig = userConfigs.get(uuid);
            if (userConfig == null) {
                loadUserData(uuid);
                userConfig = userConfigs.get(uuid);
            }
            userConfig.set(key, value);
            saveUserData(uuid);
        }
    }

    public void saveUserData(UUID uuid) {
        if (useDatabaseStorage()) {
            saveUserDataToDatabase(uuid);
        } else {
            saveUserDataToFile(uuid);
        }
    }

    private void saveUserDataToDatabase(UUID uuid) {
        try {
            Map<String, Object> userData = main.getUserDataManager().loadUser(uuid.toString());

            Player player = Bukkit.getPlayer(uuid);
            String username = player != null ? player.getName() : Bukkit.getOfflinePlayer(uuid).getName();
            String joinDate = (String) userData.getOrDefault("joined", setUserJoinDate(uuid));
            double playtime = getPlaytime(uuid);
            double afkTime = getUserConfigValue(uuid, "afk-time", 0.0);

            main.getUserDataManager().saveUser(uuid.toString(), username, joinDate, playtime, afkTime);
        } catch (SQLException e) {
            if (main.getConfig().getBoolean("logging.debug", false)) {
                main.getLogger().severe("Failed to save user data to database: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void saveUserDataToFile(UUID uuid) {
        if (useDatabaseStorage()) {
            return;
        }

        // Your existing save code
        File userFile = new File(userDataFolder, uuid.toString() + ".yml");
        FileConfiguration userConfig = userConfigs.get(uuid);

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
        if (useDatabaseStorage()) {
            try {
                Map<String, Object> userData = main.getUserDataManager().loadUser(uuid.toString());
                if (path.equals("playtime")) {
                    return (double) userData.getOrDefault("playtime", def);
                } else if (path.equals("afk-time")) {
                    return (double) userData.getOrDefault("afk-time", def);
                }
                return def;
            } catch (SQLException e) {
                if (main.getConfig().getBoolean("logging.debug", false)) {
                    main.getLogger().severe("Failed to get config value from database: " + e.getMessage());
                }
                return def;
            }
        } else {
            FileConfiguration userConfig = userConfigs.get(uuid);
            if (userConfig == null) {
                loadUserData(uuid);
                userConfig = userConfigs.get(uuid);
            }
            return userConfig != null ? userConfig.getDouble(path, def) : def;
        }
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

        // FIXED: Clear user configs when database is enabled (don't keep in memory)
        if (useDatabaseStorage()) {
            userConfigs.remove(uuid);
        }
    }
}