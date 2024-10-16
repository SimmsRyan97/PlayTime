package com.whiteiverson.minecraft.playtime_plugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
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
    }

    public void enable() {
    	loadConfigValues();
        startTasks();
    }

    public void loadConfigValues() {
        // Load values from config.yml
        trackAfk = main.getConfig().getBoolean("track-afk.enabled", false);
        autoSaveInterval = main.getConfig().getInt("playtime.auto-save-interval", 300);
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
            
            // Check if user data was successfully loaded
            if (userHandler.getUserData(uuid, "playtime") == null) {
                continue; // Skip processing for this player
            }

            // Get play time from UserHandler
            double playtime = userHandler.getUserConfigValue(uuid, "playtime", 0);
            double afkTime = userHandler.getUserConfigValue(uuid, "afk-time", 0); // Retrieve afk-time

            // If the user has no recorded play time, retrieve it from world files
            if (playtime == 0) {
                playtime = retrievePlaytimeFromWorldFiles(uuid);
                userHandler.setUserData(uuid, "playtime", playtime); // Save the retrieved play time
            }

            // Increment play time and afk-time based on AFK status
            if (trackAfk && userHandler.isAfk(uuid)) {
                // Player is AFK, increment the afk-time
                afkTime += 1; // Increment AFK time by 1 second
                userHandler.setUserData(uuid, "afk-time", afkTime); // Save afk-time in UserHandler
            } else {
                // Player is active, increment the play time
                playtime += 1; // Increment play time by 1 second
                userHandler.setUserData(uuid, "playtime", playtime); // Update play time in UserHandler
            }

            // Process rewards for the player based on their updated play time
            main.getRewardsHandler().processPlayer(player);
            
            // Save the updated user data
            userHandler.saveUserData(uuid);
        }
    }
    
    @SuppressWarnings("deprecation") // The deprecated method is for older versions of Minecraft
	private JsonObject parseJson(Reader reader) throws IOException {
        try {
            // Attempt to use the newer parseReader(Reader) method (available in GSON 2.8.6+)
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (NoSuchMethodError e) {
            // If the newer method isn't available, fall back to the older parse(String) method
            StringBuilder jsonContent = new StringBuilder();
            String line;
            BufferedReader bufferedReader = new BufferedReader(reader);
            while ((line = bufferedReader.readLine()) != null) {
                jsonContent.append(line);
            }
            return new JsonParser().parse(jsonContent.toString()).getAsJsonObject();
        }
    }

    // Get play time from server files as initial value
    private double retrievePlaytimeFromWorldFiles(UUID uuid) {
        // Get the first world
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);

        // Return 0 if no world is loaded
        if (world == null) {
            return 0;
        }

        // Define the path to the player's stats file
        File statsDir = new File(world.getWorldFolder(), "stats");
        File playerFile = new File(statsDir, uuid.toString() + ".json");

        // Check if the file exists before reading
        if (playerFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(playerFile))) {
                // Use the utility method to parse the JSON file
                JsonObject json = parseJson(reader);

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
                if (main.getConfig().getBoolean("logging.debug", false)) {
                    Bukkit.getLogger().severe(e.getMessage());
                }
            }
        } else {
            Bukkit.getLogger().warning(("user.not_found") + uuid);
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