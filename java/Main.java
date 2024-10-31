package com.whiteiverson.minecraft.playtime_plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;

import com.whiteiverson.minecraft.playtime_plugin.Commands.PlayTimeCommand;
import com.whiteiverson.minecraft.playtime_plugin.Commands.PlayTimeTopCommand;
import com.whiteiverson.minecraft.playtime_plugin.Rewards.RewardsHandler;
import com.whiteiverson.minecraft.playtime_plugin.Utilities.ColorUtil;
import com.whiteiverson.minecraft.playtime_plugin.Utilities.PlaceHolder;
import com.whiteiverson.minecraft.playtime_plugin.Utilities.Translator;

public class Main extends JavaPlugin {
    private static Main instance;
    private PlayTimeHandler playTimeHandler;
    private RewardsHandler rewardsHandler;
    private UserHandler userHandler;
    private Translator translator;
    private ColorUtil colorUtil;

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
        
        // Initiate the Colors
        colorUtil = new ColorUtil();

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
        
        // PlaceholderAPI integration
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
        	new PlaceHolder().register();
        }

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
                    sender.sendMessage(colorUtil.translateColor(getConfig().getString("color.success")) + translator.getTranslation("plugin.reload", player)); // Player translation
                } else {
                    sender.sendMessage(colorUtil.translateColor(getConfig().getString("color.error")) + translator.getTranslation("error.no_permission", player)); // Player translation
                }
            } else {
                sender.sendMessage(colorUtil.translateColor(getConfig().getString("color.success")) + translator.getConsoleTranslation("plugin.reload")); // Console translation
            }
            return true;
        }
        return false;
    }

    private void reloadPlugin() {
        reloadConfig(); // Reloads config.yml file
        userHandler.loadConfigValues(); // Reloads values in userHandler
        playTimeHandler.loadConfigValues(); // Reloads values in playTimeHandler
        manageRewards();
    }

    public static Map<String, String> calculatePlaytime(long totalSeconds, Main main, Object sender, Translator translator) {
        // Determine if the sender is a Player
        Player player = null;

        if (sender instanceof Player) {
            player = (Player) sender;
        }

        // Continue with time calculation as before
        long secondsInAMinute = 60;
        long secondsInAnHour = 3600;
        long secondsInADay = 86400;
        long secondsInAMonth = 2592000; // Approximation for 30 days

        long months = totalSeconds / secondsInAMonth;
        long remainingSecondsAfterMonths = totalSeconds % secondsInAMonth;
        long days = remainingSecondsAfterMonths / secondsInADay;
        long remainingSecondsAfterDays = remainingSecondsAfterMonths % secondsInADay;
        long hours = remainingSecondsAfterDays / secondsInAnHour;
        long remainingSecondsAfterHours = remainingSecondsAfterDays % secondsInAnHour;
        long minutes = remainingSecondsAfterHours / secondsInAMinute;
        long seconds = remainingSecondsAfterHours % secondsInAMinute;

        String integerColor = main.colorUtil.translateColor(main.getConfig().getString("color.integer"));
        String monthsText = integerColor + months + ChatColor.RESET;
        String daysText = integerColor + days + ChatColor.RESET;
        String hoursText = integerColor + hours + ChatColor.RESET;
        String minutesText = integerColor + minutes + ChatColor.RESET;
        String secondsText = integerColor + seconds + ChatColor.RESET;

        Map<String, String> timeComponents = new HashMap<>();
        timeComponents.put("months", monthsText);
        timeComponents.put("days", daysText);
        timeComponents.put("hours", hoursText);
        timeComponents.put("minutes", minutesText);
        timeComponents.put("seconds", secondsText);

        String intervalColor = main.colorUtil.translateColor(main.getConfig().getString("color.interval"));
        
        // Use a default translation if the player is null
        String monthsString = months == 1
            ? (player != null ? translator.getTranslation("playtime.time.months.singular", player) : translator.getConsoleTranslation("playtime.time.months.singular"))
            : (player != null ? translator.getTranslation("playtime.time.months.plural", player) : translator.getConsoleTranslation("playtime.time.months.plural"));
            
        String daysString = days == 1
            ? (player != null ? translator.getTranslation("playtime.time.days.singular", player) : translator.getConsoleTranslation("playtime.time.days.singular"))
            : (player != null ? translator.getTranslation("playtime.time.days.plural", player) : translator.getConsoleTranslation("playtime.time.days.plural"));
            
        String hoursString = hours == 1
            ? (player != null ? translator.getTranslation("playtime.time.hours.singular", player) : translator.getConsoleTranslation("playtime.time.hours.singular"))
            : (player != null ? translator.getTranslation("playtime.time.hours.plural", player) : translator.getConsoleTranslation("playtime.time.hours.plural"));
            
        String minutesString = minutes == 1
            ? (player != null ? translator.getTranslation("playtime.time.minutes.singular", player) : translator.getConsoleTranslation("playtime.time.minutes.singular"))
            : (player != null ? translator.getTranslation("playtime.time.minutes.plural", player) : translator.getConsoleTranslation("playtime.time.minutes.plural"));
            
        String secondsString = seconds == 1
            ? (player != null ? translator.getTranslation("playtime.time.seconds.singular", player) : translator.getConsoleTranslation("playtime.time.seconds.singular"))
            : (player != null ? translator.getTranslation("playtime.time.seconds.plural", player) : translator.getConsoleTranslation("playtime.time.seconds.plural"));
            
        // Add the translated strings to timeComponents
        timeComponents.put("monthsString", intervalColor + monthsString + ChatColor.RESET);
        timeComponents.put("daysString", intervalColor + daysString + ChatColor.RESET);
        timeComponents.put("hoursString", intervalColor + hoursString + ChatColor.RESET);
        timeComponents.put("minutesString", intervalColor + minutesString + ChatColor.RESET);
        timeComponents.put("secondsString", intervalColor + secondsString + ChatColor.RESET);

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
    
    public ColorUtil getColorUtil() {
    	return colorUtil;
    }
}
