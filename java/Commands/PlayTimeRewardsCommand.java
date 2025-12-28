package com.whiteiverson.minecraft.playtime_plugin.Commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.chat.Chat;

import com.whiteiverson.minecraft.playtime_plugin.Main;
import com.whiteiverson.minecraft.playtime_plugin.UserHandler;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PlayTimeRewardsCommand implements CommandExecutor {

    private final UserHandler userHandler;
    private final File rewardsFile;
    private final Main main;
    private final Chat vaultChat;

    public PlayTimeRewardsCommand(UserHandler userHandler, File rewardsFile) {
        this.userHandler = userHandler;
        this.rewardsFile = rewardsFile;
        this.main = Main.getInstance();
        this.vaultChat = main.getChat();  // Vault chat is retrieved here
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        UUID targetUuid;
        String targetName;

        // Determine the target player
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(main.getTranslator().getTranslation("error.no_permission", sender));
                return true;
            }
            targetUuid = ((Player) sender).getUniqueId();
            targetName = sender.getName();
        } else {
            targetName = args[0];

            if (sender instanceof Player && !sender.hasPermission("playtime.rewards")) {
                sender.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) +
                        main.getTranslator().getTranslation("error.no_permission", sender));
                return true;
            }

            // Attempt to resolve the player using nickname first
            Player onlinePlayer = resolvePlayerByNickname(targetName);
            if (onlinePlayer != null) {
                targetUuid = onlinePlayer.getUniqueId();
                targetName = onlinePlayer.getName();
            } else {
                // Fallback to exact name matching for offline players
            	@SuppressWarnings("deprecation")
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
                if (offlinePlayer.hasPlayedBefore()) {
                    targetUuid = offlinePlayer.getUniqueId();
                    targetName = offlinePlayer.getName();
                } else {
                    sender.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) +
                            main.getTranslator().getTranslation("error.no_user", sender));
                    return true;
                }
            }
        }

        // Load rewards
        FileConfiguration rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);
        if (!rewardsConfig.contains("rewards")) {
            sender.sendMessage(main.getTranslator().getTranslation("rewards.no_rewards", null));
            return true;
        }

        // Load player data
        userHandler.loadUserData(targetUuid);
        double playtime = userHandler.getPlaytime(targetUuid);

        // Display rewards status
        sender.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.user")) + targetName + ":");
        for (String rewardKey : Objects.requireNonNull(rewardsConfig.getConfigurationSection("rewards")).getKeys(false)) {
            String rewardName = rewardKey;
            double requiredTime = rewardsConfig.getDouble("rewards." + rewardKey + ".time");

            if (playtime >= requiredTime) {
                sender.sendMessage(rewardName + " - " + main.getTranslator().getTranslation("rewards.achieved", null));
            } else {
                double timeRemaining = requiredTime - playtime;

                // Use calculatePlaytime method to get the formatted time components
                Map<String, String> timeComponents = Main.calculatePlaytime((long) timeRemaining, main, sender, main.getTranslator());

                // Format the message with translated components
                String message = rewardName + " - " + main.getTranslator().getTranslation("rewards.not_achieved", null) + " - "
                        + timeComponents.get("months") + " " + timeComponents.get("monthsString") + " "
                        + timeComponents.get("days") + " " + timeComponents.get("daysString") + " "
                        + timeComponents.get("hours") + " " + timeComponents.get("hoursString") + " "
                        + timeComponents.get("minutes") + " " + timeComponents.get("minutesString") + " "
                        + timeComponents.get("seconds") + " " + timeComponents.get("secondsString");

                sender.sendMessage(message);
            }
        }
        return true;
    }

    // Method to resolve player by their nickname
    private Player resolvePlayerByNickname(String nickname) {
        if (vaultChat == null) {
            // Vault is not available, so fallback to exact name matching only
            return Bukkit.getPlayer(nickname);
        }

        // Vault is available, try to resolve by nickname with prefix and suffix
        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerDisplayName = vaultChat.getPlayerPrefix(player) + player.getDisplayName() + vaultChat.getPlayerSuffix(player);
            if (ChatColor.stripColor(playerDisplayName).equalsIgnoreCase(nickname)) {
                return player;
            }
        }
        return null;
    }
}