package com.whiteiverson.minecraft.playtime_plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.whiteiverson.minecraft.playtime_plugin.Commands.PlayTimeCommand;
import com.whiteiverson.minecraft.playtime_plugin.Commands.PlayTimeTopCommand;
import com.whiteiverson.minecraft.playtime_plugin.Rewards.RewardsHandler;

public class Main extends JavaPlugin {
    private static Main instance;
    private PlayTimeHandler playTimeHandler;
    private RewardsHandler rewardsHandler;
    private UserHandler userHandler;
    private Translator translator;

    // To handle reward cooldowns
    private Map<UUID, Long> rewardCooldowns = new HashMap<>();

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Instantiate the Translator
        translator = new Translator();

        // Load config.yml
        saveDefaultConfig();

        // Output loading message in console using console-specific translation method
        getLogger().info(translator.getConsoleTranslation("plugin.loading")); // For console

        userHandler = new UserHandler();
        playTimeHandler = new PlayTimeHandler(this, userHandler);
        rewardsHandler = new RewardsHandler(this); // Pass instance to RewardsHandler
        
        // Initialise handlers
        userHandler.enable();
        playTimeHandler.enable();
        
        manageRewards();

        // Register event listeners
        getServer().getPluginManager().registerEvents(userHandler, this);

        // Enable commands based on config
        registerCommands();

        // Output success message in console using console-specific translation method
        getLogger().info(translator.getConsoleTranslation("plugin.load_success")); // For console
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
            rewardsHandler.enable(); // Initialise rewards if enabled
        }
    }

    @Override
    public void onDisable() {
        // Ensure to save any necessary data here, if applicable
        instance = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if ("ptreload".equalsIgnoreCase(cmd.getName())) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (sender.hasPermission("playtime.reload")) {
                    reloadPlugin();
                    sender.sendMessage(ChatColor.GREEN + translator.getTranslation("plugin.reload", player)); // Player translation
                } else {
                    sender.sendMessage(ChatColor.RED + translator.getTranslation("error.no_permission", player)); // Player translation
                }
            } else {
                sender.sendMessage(ChatColor.GREEN + translator.getConsoleTranslation("plugin.reload")); // Console translation
            }
            return true;
        }
        return false;
    }

    private void reloadPlugin() {
        reloadConfig();
        userHandler.loadConfigValues();
        playTimeHandler.loadConfigValues();
        manageRewards();
    }

    // Helper method to calculate time components and handle plural/singular formatting
    public static Map<String, String> calculatePlaytime(long totalSeconds, Main main, CommandSender sender, Translator translator) {
        // Ensure sender is a player
        Player player = null;

        if (sender instanceof Player) {
            player = (Player) sender;
        }

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

        // Plural/singular formatting using translations
        Map<String, String> timeComponents = new HashMap<>();
        timeComponents.put("greenMonths", greenMonths);
        timeComponents.put("greenDays", greenDays);
        timeComponents.put("greenHours", greenHours);
        timeComponents.put("greenMinutes", greenMinutes);
        timeComponents.put("greenSeconds", greenSeconds);

        // Plural/singular translation
        timeComponents.put("monthsString", months == 1 ? translator.getTranslation("playtime.time.months.singular", player) : translator.getTranslation("playtime.time.months.plural", player));
        timeComponents.put("daysString", days == 1 ? translator.getTranslation("playtime.time.days.singular", player) : translator.getTranslation("playtime.time.days.plural", player));
        timeComponents.put("hoursString", hours == 1 ? translator.getTranslation("playtime.time.hours.singular", player) : translator.getTranslation("playtime.time.hours.plural", player));
        timeComponents.put("minutesString", minutes == 1 ? translator.getTranslation("playtime.time.minutes.singular", player) : translator.getTranslation("playtime.time.minutes.plural", player));
        timeComponents.put("secondsString", seconds == 1 ? translator.getTranslation("playtime.time.seconds.singular", player) : translator.getTranslation("playtime.time.seconds.plural", player));

        return timeComponents;
    }

    public RewardsHandler getRewardsHandler() {
        return rewardsHandler;
    }

    public PlayTimeHandler getPlayTimeHandler() {
        return playTimeHandler;
    }

    public UserHandler getUserHandler() {
        return userHandler;
    }

    public Map<UUID, Long> getRewardCooldowns() {
        return rewardCooldowns;
    }

    public Translator getTranslator() {
        return translator;
    }
}
