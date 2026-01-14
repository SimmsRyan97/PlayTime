package com.whiteiverson.minecraft.playtime_plugin.Rewards;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.whiteiverson.minecraft.playtime_plugin.Main;
import com.whiteiverson.minecraft.playtime_plugin.Utilities.Translator;

import com.earth2me.essentials.Essentials;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.*;

public class RewardsHandler {
    private final Main main;
    private Set<Rewards> rewards;
    private static final int MILLISECONDS_IN_SECOND = 1000;

    // Constructor to accept Main instance
    public RewardsHandler(Main main) {
        this.main = main;
        this.rewards = new HashSet<>();
    }

    public void enable() {
        setupRewards();
    }

    /**
     * Loads and sets up rewards from the rewards.yml configuration file.
     */
    private void setupRewards() {
        // Load rewards.yml configuration file
        File rewardsFile = new File(main.getDataFolder(), "rewards.yml");

        // Create the file if it doesn't exist
        if (!rewardsFile.exists()) {
            main.saveResource("rewards.yml", false);
        }

        FileConfiguration rewardsConfig = Translator.loadYamlWithBomHandlingStatic(rewardsFile);
        rewards = new TreeSet<>();

        // Ensure rewards section exists in the configuration
        if (rewardsConfig.contains("rewards")) {
            Set<String> rewardKeys = Objects.requireNonNull(rewardsConfig.getConfigurationSection("rewards")).getKeys(false);

            // Loop through each reward entry in the rewards.yml
            for (String key : rewardKeys) {
                String name = rewardsConfig.getString("rewards." + key + ".name");
                double time = rewardsConfig.getDouble("rewards." + key + ".time");
                List<String> commands = rewardsConfig.getStringList("rewards." + key + ".commands");

                // Validate reward configuration before adding it
                if (name != null && !commands.isEmpty()) {
                    rewards.add(new Rewards(name, time, commands));
                } else {
                    Bukkit.getLogger().warning(main.getTranslator().getTranslation("rewards.invalid_key", null) + key);
                }
            }
        } else {
            Bukkit.getLogger().warning(main.getTranslator().getTranslation("rewards.no_rewards", null));
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

        // FIXED: Only skip if cooldown is positive AND not enough time has passed
        if (cooldown > 0 && (currentTime - lastProcessedTime) < cooldown) {
            return; // Skip processing if still within cool down period
        }

        // Iterate over each reward
        for (Rewards reward : rewards) {

            // Check if the player has sufficient play time and hasn't claimed the reward
            if (!hasRewardBeenClaimed(uuid, reward) && main.getPlayTimeHandler().hasSufficientPlaytime(uuid, reward.getTime())) {

                // Mark as claimed FIRST, before executing commands
                markRewardAsClaimed(uuid, reward);

                // Then execute commands
                executeRewardCommands(player, reward);

                // Broadcast reward to the server if broadcasting is enabled
                if (main.getConfig().getBoolean("rewards.broadcast.chat")) {
                    String playerNameColor, earnedColor, rewardsColor;
                    playerNameColor = main.getColorUtil().translateColor(main.getConfig().getString("color.user"));
                    earnedColor = main.getColorUtil().translateColor(main.getConfig().getString("color.earned"));
                    rewardsColor = main.getColorUtil().translateColor(main.getConfig().getString("color.reward"));

                    // Broadcast to Minecraft server
                    Bukkit.broadcastMessage(playerNameColor + player.getName() + earnedColor + main.getTranslator().getTranslation("rewards.earned", player) + rewardsColor + reward.getName());
                }

                if(main.getConfig().getBoolean("rewards.broadcast.discord")) {
                    // Check if EssentialsX is installed and use its API to send a Discord message
                    if (Bukkit.getPluginManager().isPluginEnabled("EssentialsDiscord")) {
                        sendToDiscordEssentials(player, reward);
                    }

                    // Check if DiscordSRV is installed and use its API to send a Discord message
                    if (Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) {
                        sendToDiscordDiscordSRV(player, reward);
                    }
                }

                if (main.getConfig().getBoolean("logging.reward-claims")) {
                    Bukkit.getLogger().info(player.getName() + main.getTranslator().getTranslation("rewards.earned", null) + reward.getName());
                }

                // Update last reward processing time
                main.getRewardCooldowns().put(uuid, currentTime);
            }
        }
    }

    /**
     * Sends a reward message to Discord via EssentialsX.
     *
     * @param player The player receiving the reward.
     * @param reward The reward being processed.
     */
    private void sendToDiscordEssentials(Player player, Rewards reward) {
        Essentials essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");

        String channelName = Objects.requireNonNull(essentials).getConfig().getString("channels.primary");

        String message = player.getName() + " has earned the reward: " + reward.getName();
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "discordbroadcast " + channelName + message);
    }

    /**
     * Sends a reward message to Discord via DiscordSRV.
     *
     * @param player The player receiving the reward.
     * @param reward The reward being processed.
     */
    private void sendToDiscordDiscordSRV(Player player, Rewards reward) {
        String channelName = Objects.requireNonNull(DiscordSRV.getPlugin().getConfig().getConfigurationSection("Channels")).getString("global");

        String message = "**" + player.getName() + "** has earned the reward: **" + reward.getName() + "**";

        TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelName);
        if( textChannel != null ) {
            textChannel.sendMessage(message).queue();
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
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
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
        return main.getUserHandler().isRewardClaimed(uuid, reward.getName());
    }

    /**
     * Marks a reward as claimed for a player.
     *
     * @param uuid   The player's UUID.
     * @param reward The reward being marked.
     */
    private void markRewardAsClaimed(UUID uuid, Rewards reward) {
        main.getUserHandler().setUserData(uuid, "rewards.claimed." + reward.getName(), true);
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