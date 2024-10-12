package com.whiteiverson.minecraft.playtime_plugin.Rewards;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.whiteiverson.minecraft.playtime_plugin.Main;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class RewardsHandler {
    private Main main;
    private Set<Rewards> rewards;
    private FileConfiguration rewardsConfig;
    private static final int MILLISECONDS_IN_SECOND = 1000;

    // Constructor to accept Main instance
    public RewardsHandler(Main main) {
        this.main = main; // Initialise main through the constructor
        this.rewards = new HashSet<>(); // Initialise rewards Set
    }
    
    public void enable() {
    	setupRewards(); // Setup rewards in the constructor
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
                    Bukkit.getLogger().warning(main.getTranslator().getConsoleTranslation("rewards.invalid_key") + key);
                }
            }
        } else {
            Bukkit.getLogger().warning(main.getTranslator().getConsoleTranslation("rewards.no_rewards"));
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

        // Check cool down to avoid frequent processing
        long lastProcessedTime = main.getRewardCooldowns().getOrDefault(uuid, 0L);
        int cooldown = main.getConfig().getInt("rewards.reward-cooldown", 0) * MILLISECONDS_IN_SECOND;
        if ((currentTime - lastProcessedTime) < cooldown || cooldown <= 0) {
            return; // Skip processing if still within cool down period
        }

        // Iterate over each reward
        for (Rewards reward : rewards) {
            // Check if the player has sufficient play time and hasn't claimed the reward
            if (!hasRewardBeenClaimed(uuid, reward) && main.getPlayTimeHandler().hasSufficientPlaytime(uuid, reward.getTime())) {
                executeRewardCommands(player, reward);
                markRewardAsClaimed(uuid, reward, true); // Mark reward as claimed

                // Broadcast reward to the server if broadcasting is enabled
                if (main.getConfig().getBoolean("rewards.broadcast")) {
                    Bukkit.broadcastMessage(player.getName() + main.getTranslator().getTranslation("rewards.earned", player) + reward.getName());
                }

                if (main.getConfig().getBoolean("logging.reward-claims")) {
                    Bukkit.getLogger().info(player.getName() + main.getTranslator().getConsoleTranslation("rewards.earned") + reward.getName());
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
        return Optional.ofNullable(main.getUserHandler().getUserData(uuid, "rewards.claimed." + reward.getName()))
                .filter(data -> data instanceof Boolean)
                .map(data -> (Boolean) data)
                .orElse(false);
    }

    /**
     * Marks a reward as claimed or unclaimed for a player.
     *
     * @param uuid    The player's UUID.
     * @param reward  The reward being marked.
     * @param claimed Whether the reward has been claimed.
     */
    private void markRewardAsClaimed(UUID uuid, Rewards reward, boolean claimed) {
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