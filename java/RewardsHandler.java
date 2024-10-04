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

    /**
     * Enables the reward system by loading the rewards from configuration.
     */
    public void enable() {
        main = Main.getInstance();
        setupRewards();
    }

    /**
     * Disables the reward system, providing a placeholder for future cleanup tasks.
     */
    public void disable() {
        // Cleanup tasks if necessary
    }

    /**
     * Loads and sets up rewards from the rewards.yml configuration file.
     */
    private void setupRewards() {
        // Load rewards.yml configuration file
        File rewardsFile = new File(main.getDataFolder(), "rewards.yml");

        // Create the file if it doesn't exist
        if (!rewardsFile.exists()) {
            main.saveResource("rewards.yml", false); // Save default rewards.yml if it doesn't exist
        }

        rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);
        rewards = new TreeSet<>();

        // Ensure rewards section exists in the configuration
        if (rewardsConfig.contains("rewards")) {
            Set<String> rewardKeys = rewardsConfig.getConfigurationSection("rewards").getKeys(false);

            // Loop through each reward entry in the rewards.yml
            for (String key : rewardKeys) {
                String name = rewardsConfig.getString("rewards." + key + ".name");
                double time = rewardsConfig.getDouble("rewards." + key + ".time");
                List<String> commands = rewardsConfig.getStringList("rewards." + key + ".commands");

                // Validate reward configuration before adding it
                if (name != null && !commands.isEmpty()) {
                    rewards.add(new Rewards(name, time, commands));
                } else {
                    Bukkit.getLogger().warning("Invalid reward configuration for key: " + key);
                }
            }
        } else {
            Bukkit.getLogger().warning("No rewards section found in the rewards.yml config!");
        }
    }

    /**
     * Processes rewards for a player based on their play time and reward claims.
     *
     * @param player The player being processed for rewards.
     */
    public void processPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Check cooldown to avoid frequent processing
        if (main.getRewardCooldowns().containsKey(uuid)) {
            long lastProcessedTime = main.getRewardCooldowns().get(uuid);
            int cooldown = main.getConfig().getInt("rewards.reward-cooldown", 0) * MILLISECONDS_IN_SECOND; // Convert to milliseconds
            if ((currentTime - lastProcessedTime) < cooldown) {
                return; // Skip processing if still within cooldown period
            }
        }

        // Iterate over each reward
        for (Rewards reward : rewards) {
            // Check if the player has sufficient play time and hasn't claimed the reward
            if (!hasRewardBeenClaimed(uuid, reward) && main.getPlayTimeHandler().hasSufficientPlaytime(uuid, reward.getTime())) {
                executeRewardCommands(player, reward);
                markRewardAsClaimed(uuid, reward, true); // Mark reward as claimed

                // Broadcast reward to the server if broadcasting is enabled
                if (main.getConfig().getBoolean("rewards.broadcast")) {
                    Bukkit.broadcastMessage(player.getName() + " has earned a reward: " + reward.getName());
                }

                // Update last reward processing time
                main.getRewardCooldowns().put(uuid, currentTime);
            }
        }
    }

    /**
     * Executes commands associated with a reward.
     *
     * @param player The player receiving the reward.
     * @param reward The reward being processed.
     */
    private void executeRewardCommands(Player player, Rewards reward) {
        for (String command : reward.getCommands()) {
            // Replace placeholder %player% with the player's actual name
            String processedCommand = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand); // Execute command as console
        }
    }

    /**
     * Checks whether a reward has already been claimed by the player.
     *
     * @param uuid   The player's UUID.
     * @param reward The reward being checked.
     * @return True if the reward has been claimed, false otherwise.
     */
    private boolean hasRewardBeenClaimed(UUID uuid, Rewards reward) {
        // Update: Check if the reward is claimed in the rewards.claimed section
        return main.getUserHandler().getUserData(uuid, "rewards.claimed." + reward.getName()) instanceof Boolean
                && (Boolean) main.getUserHandler().getUserData(uuid, "rewards.claimed." + reward.getName());
    }

    /**
     * Marks a reward as claimed or unclaimed for a player.
     *
     * @param uuid    The player's UUID.
     * @param reward  The reward being marked.
     * @param claimed Whether the reward has been claimed.
     */
    private void markRewardAsClaimed(UUID uuid, Rewards reward, boolean claimed) {
        // Update: Store reward claim status under rewards.claimed in the user file
        main.getUserHandler().setUserData(uuid, "rewards.claimed." + reward.getName(), claimed);
    }

    /**
     * Retrieves the set of rewards.
     *
     * @return A set containing all rewards.
     */
    public Set<Rewards> getRewards() {
        return rewards;
    }
}