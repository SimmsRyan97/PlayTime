package com.whiteiverson.minecraft.playtime_plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private static Main instance;
    private PlayTimeHandler playTimeHandler;
    private RewardsHandler rewardsHandler;
    private UserHandler userHandler;

    // To handle reward cooldowns
    private Map<UUID, Long> rewardCooldowns = new HashMap<>();

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("PlayTime plugin is loading...");

        // Load config.yml
        saveDefaultConfig();

        userHandler = new UserHandler();
        playTimeHandler = new PlayTimeHandler(this, userHandler);
        rewardsHandler = new RewardsHandler();

        // Initialise handlers
        playTimeHandler.enable();
        userHandler.enable();
        manageRewards();

        // Register event listeners
        getServer().getPluginManager().registerEvents(userHandler, this);

        // Enable commands based on config
        registerCommands();

        getLogger().info("PlayTime plugin loaded successfully!");
    }

    private void registerCommands() {
        registerCommand("pt", new PlayTimeCommand(this));
        registerCommand("pttop", new PlayTimeTopCommand(this));
        registerCommand("ptreload", this);
    }

    private void registerCommand(String command, Object executor) {
        if (getCommand(command) != null) {
            getCommand(command).setExecutor((CommandExecutor) executor);
        }
    }

    private void manageRewards() {
        if (getConfig().getBoolean("rewards.enabled", true)) {
            rewardsHandler.enable();
        } else {
            rewardsHandler.disable();
        }
    }

    @Override
    public void onDisable() {
        // Ensure to save any necessary data here, if applicable
        playTimeHandler.disable();
        rewardsHandler.disable();
        userHandler.disable();
        instance = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	getLogger().info("Command received: " + cmd.getName());
    	
        if ("ptreload".equalsIgnoreCase(cmd.getName())) {
            if (sender.hasPermission("playtime.reload")) {
                reloadPlugin();
                sender.sendMessage(ChatColor.GREEN + "Plugin configuration reloaded successfully.");
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            }
            return true;
        }
        return false;
    }

    private void reloadPlugin() {
        reloadConfig();
        manageRewards(); // Reload the reward system as well
    }
    
    // Helper method to calculate time components and handle plural/singular formatting
    public static Map<String, String> calculatePlaytime(long totalSeconds) {
        // Time constants
        long secondsInAMinute = 60;
        long secondsInAnHour = 3600;
        long secondsInADay = 86400;
        long secondsInAMonth = 2592000; // Approximation for 30 days

        // Calculate months, days, hours, minutes, and seconds
        long months = totalSeconds / secondsInAMonth;
        long remainingSecondsAfterMonths = totalSeconds % secondsInAMonth;
        long days = remainingSecondsAfterMonths / secondsInADay;
        long remainingSecondsAfterDays = remainingSecondsAfterMonths % secondsInADay;
        long hours = remainingSecondsAfterDays / secondsInAnHour;
        long remainingSecondsAfterHours = remainingSecondsAfterDays % secondsInAnHour;
        long minutes = remainingSecondsAfterHours / secondsInAMinute;
        long seconds = remainingSecondsAfterHours % secondsInAMinute;

        // Green strings with colour coding
        String greenMonths = ChatColor.GREEN + String.valueOf(months) + ChatColor.RESET;
        String greenDays = ChatColor.GREEN + String.valueOf(days) + ChatColor.RESET;
        String greenHours = ChatColor.GREEN + String.valueOf(hours) + ChatColor.RESET;
        String greenMinutes = ChatColor.GREEN + String.valueOf(minutes) + ChatColor.RESET;
        String greenSeconds = ChatColor.GREEN + String.valueOf(seconds) + ChatColor.RESET;

        // Plural/singular formatting
        Map<String, String> timeComponents = new HashMap<>();
        timeComponents.put("greenMonths", greenMonths);
        timeComponents.put("greenDays", greenDays);
        timeComponents.put("greenHours", greenHours);
        timeComponents.put("greenMinutes", greenMinutes);
        timeComponents.put("greenSeconds", greenSeconds);
        timeComponents.put("monthsString", months == 1 ? "month" : "months");
        timeComponents.put("daysString", days == 1 ? "day" : "days");
        timeComponents.put("hoursString", hours == 1 ? "hour" : "hours");
        timeComponents.put("minutesString", minutes == 1 ? "minute" : "minutes");
        timeComponents.put("secondsString", seconds == 1 ? "second" : "seconds");

        return timeComponents;
    }
    
    public PlayTimeHandler getPlayTimeHandler() {
        return playTimeHandler;
    }

    public RewardsHandler getRewardsHandler() {
        return rewardsHandler;
    }

    public UserHandler getUserHandler() {
        return userHandler;
    }

    public Map<UUID, Long> getRewardCooldowns() {
        return rewardCooldowns;
    }
}
