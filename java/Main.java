package com.whiteiverson.minecraft.playtime_plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.command.Command;
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

        // Load config.yml
        saveDefaultConfig();

        userHandler = new UserHandler();
        playTimeHandler = new PlayTimeHandler(this, userHandler);
        rewardsHandler = new RewardsHandler();

        // Initialise handlers
        playTimeHandler.enable();

        // Enable rewards only if rewards system is enabled in config
        if (getConfig().getBoolean("rewards.enabled")) {
            rewardsHandler.enable();
        }

        userHandler.enable();

        // Register event listeners
        getServer().getPluginManager().registerEvents(userHandler, this);

        // Enable commands based on config
        if (getConfig().getBoolean("commands.pt.enabled")) {
            getCommand("pt").setExecutor(new PlayTimeCommand(this));
        }
        if (getConfig().getBoolean("commands.pttop.enabled")) {
            getCommand("pttop").setExecutor(new PlayTimeTopCommand(this));
        }

        // Register the reload command
        getCommand("playtime_reload").setExecutor(this);  // Register the command
    }

    @Override
    public void onDisable() {
        // Ensure to save any necessary data here, if applicable
        playTimeHandler.disable();
        rewardsHandler.disable();
        userHandler.disable();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("playtime_reload")) {
            if (sender.hasPermission("playtime.reload")) {
                reloadPlugin();
                sender.sendMessage("Plugin configuration reloaded successfully.");
            } else {
                sender.sendMessage("You do not have permission to use this command.");
            }
            return true;
        }
        return false;
    }

    private void reloadPlugin() {
        // Reload the config
        reloadConfig();
        // Logic to reload other components if necessary
        // For example, if you have a method to reload rewards:
        if (getConfig().getBoolean("rewards.enabled")) {
            rewardsHandler.enable();  // Ensure to re-enable if necessary
        } else {
            rewardsHandler.disable();  // Disable if the rewards system is no longer enabled
        }
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