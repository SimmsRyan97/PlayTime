package com.whiteiverson.minecraft.playtime_plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class RewardsHandler {
    private Main main;
    private Set<Rewards> rewards;
    private FileConfiguration rewardsConfig;
    private static final int MILLISECONDS_IN_SECOND = 1000;

    public void enable() {
        main = Main.getInstance();
        setupRewards();
    }

    public void disable() {
        // Cleanup tasks if necessary
    }

    private void setupRewards() {
        // Load rewards.yml configuration file
        File rewardsFile = new File(main.getDataFolder(), "rewards.yml");

        if (!rewardsFile.exists()) {
            main.saveResource("rewards.yml", false); // Save default if it doesn't exist
        }

        rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);
        rewards = new TreeSet<>();

        // Ensure rewards section exists
        if (rewardsConfig.contains("rewards")) {
            Set<String> rewardKeys = rewardsConfig.getConfigurationSection("rewards").getKeys(false);
            for (String key : rewardKeys) {
                String name = rewardsConfig.getString("rewards." + key + ".name");
                double time = rewardsConfig.getDouble("rewards." + key + ".time");
                List<String> commands = rewardsConfig.getStringList("rewards." + key + ".commands");
                
                // Handle potential issues with the configuration
                if (name != null && !commands.isEmpty()) {
                    rewards.add(new Rewards(name, time, commands));
                } else {
                    Bukkit.getLogger().warning("Invalid reward configuration for key: " + key);
                }
            }
        } else {
            Bukkit.getLogger().warning("No rewards section found in the rewards config!");
        }
    }

    public void processPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        // Respect reward cooldown from config.yml
        long currentTime = System.currentTimeMillis();
        if (main.getRewardCooldowns().containsKey(uuid)) {
            long lastProcessedTime = main.getRewardCooldowns().get(uuid);
            int cooldown = main.getConfig().getInt("rewards.reward-cooldown") * MILLISECONDS_IN_SECOND; // Convert to milliseconds
            if ((currentTime - lastProcessedTime) < cooldown) {
                return; // Skip processing if within cooldown
            }
        }

        for (Rewards reward : rewards) {
            // Check if the reward is claimed and if the player has sufficient play time
            if (!hasRewardBeenClaimed(uuid, reward) && main.getPlayTimeHandler().hasSufficientPlaytime(uuid, reward.getTime())) {
                executeRewardCommands(player, reward);
                markRewardAsClaimed(uuid, reward, true);

                // Respect broadcast setting from config.yml
                if (main.getConfig().getBoolean("rewards.broadcast")) {
                    Bukkit.broadcastMessage(player.getName() + " has earned a reward: " + reward.getName());
                }

                main.getRewardCooldowns().put(uuid, currentTime); // Update cooldown
            }
        }
    }

    private void executeRewardCommands(Player player, Rewards reward) {
        for (String command : reward.getCommands()) {
            String processedCommand = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
        }
    }

    private boolean hasRewardBeenClaimed(UUID uuid, Rewards reward) {
        // Use the user handler to get the claimed status from user data
        return (boolean) main.getUserHandler().getUserData(uuid, getRewardLabel(reward));
    }

    private void markRewardAsClaimed(UUID uuid, Rewards reward, boolean claimed) {
        // Use the user handler to set the claimed status in user data
        main.getUserHandler().setUserData(uuid, getRewardLabel(reward), claimed);
    }

    private String getRewardLabel(Rewards reward) {
        return "playtime.rewards." + reward.getName();
    }
}