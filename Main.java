package com.whiteiverson.minecraft.playtime_plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    }

    @Override
    public void onDisable() {
        // Ensure to save any necessary data here, if applicable
        playTimeHandler.disable();
        rewardsHandler.disable();
        userHandler.disable();
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