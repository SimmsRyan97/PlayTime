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
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public class PlayTimeCommand implements CommandExecutor {
    private final Main main;
    private final Chat vaultChat; // Vault Chat API for nickname handling

    public PlayTimeCommand(Main main) {
        this.main = main;
        this.vaultChat = main.getChat();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, String label, String[] args) {
        // Command label check
        if (!(label.equalsIgnoreCase("playtime") || label.equalsIgnoreCase("pt"))) {
            return false;
        }

        UUID targetUUID;
        String playerName;
        String joinDate;
        boolean isSelf = false;

        // Determine target player
        if (args.length == 0) {
            // Self-playtime check, ensure sender is a player
            if (!(sender instanceof Player)) {
                return true;
            }
            Player player = (Player) sender;
            targetUUID = player.getUniqueId();
            playerName = "You";
            isSelf = true;
        } else {
            playerName = args[0];

            if (sender instanceof Player && !sender.hasPermission("playtime.check")) {
                sender.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) +
                        main.getTranslator().getTranslation("error.no_permission", sender));
                return true;
            }

            // Attempt to resolve the player using nickname first
            Player onlinePlayer = resolvePlayerByNickname(playerName);
            if (onlinePlayer != null) {
                targetUUID = onlinePlayer.getUniqueId();
                playerName = getFormattedPlayerName(onlinePlayer); // FIXED: Get formatted name with prefix/suffix
            } else {
                // Fallback to exact name matching for offline players
                @SuppressWarnings("deprecation")
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                if (offlinePlayer.hasPlayedBefore()) {
                    targetUUID = offlinePlayer.getUniqueId();
                    playerName = getFormattedPlayerName(offlinePlayer); // FIXED: Get formatted name
                } else {
                    sender.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) +
                            main.getTranslator().getTranslation("error.no_user", sender));
                    return true;
                }
            }
        }

        joinDate = main.getUserHandler().getUserJoinDate(targetUUID);

        double playtime = main.getUserHandler().getPlaytime(targetUUID);
        long totalSeconds = (long) playtime;

        Map<String, String> timeComponents = Main.calculatePlaytime(totalSeconds, main, sender, main.getTranslator());
        String date = main.getColorUtil().translateColor(main.getConfig().getString("color.interval")) + joinDate + ChatColor.RESET;

        String message = formatPlaytimeMessage(playerName, timeComponents, date, sender, isSelf);
        sender.sendMessage(message);
        return true;
    }

    private String getFormattedPlayerName(OfflinePlayer offlinePlayer) {
        Player onlinePlayer = offlinePlayer.getPlayer();

        if (onlinePlayer != null && onlinePlayer.isOnline() && vaultChat != null) {
            String prefix = vaultChat.getPlayerPrefix(onlinePlayer);
            String suffix = vaultChat.getPlayerSuffix(onlinePlayer);
            String name = onlinePlayer.getName();

            // Translate color codes
            String coloredPrefix = prefix != null ? main.getColorUtil().translateColor(prefix) : "";
            String coloredSuffix = suffix != null ? main.getColorUtil().translateColor(suffix) : "";

            return coloredPrefix + name + coloredSuffix;
        }

        // For offline players, just return their name
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
    }

    private Player resolvePlayerByNickname(String nickname) {
        if (vaultChat == null) {
            return null;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerDisplayName = vaultChat.getPlayerPrefix(player) + player.getDisplayName() + vaultChat.getPlayerSuffix(player);
            if (ChatColor.stripColor(playerDisplayName).equalsIgnoreCase(nickname)) {
                return player;
            }
        }
        return null;
    }

    private String formatPlaytimeMessage(String playerName, Map<String, String> timeComponents, String date, CommandSender sender, boolean isSelf) {
        String translationKey = isSelf ? "playtime.self" : "playtime.other";

        // Prepare the message with correct arguments for String.format
        String messageTemplate = main.getTranslator().getTranslation(translationKey, sender);

        if (isSelf) {
            // Format for the current player
            return String.format(messageTemplate,
                    timeComponents.get("months"), timeComponents.get("monthsString"),
                    timeComponents.get("days"), timeComponents.get("daysString"),
                    timeComponents.get("hours"), timeComponents.get("hoursString"),
                    timeComponents.get("minutes"), timeComponents.get("minutesString"),
                    timeComponents.get("seconds"), timeComponents.get("secondsString"),
                    date // Join date
            );
        } else {
            // Format for other players (or console target)
            return String.format(messageTemplate,
                    playerName, // Player name with colored prefix/suffix
                    timeComponents.get("months"), timeComponents.get("monthsString"),
                    timeComponents.get("days"), timeComponents.get("daysString"),
                    timeComponents.get("hours"), timeComponents.get("hoursString"),
                    timeComponents.get("minutes"), timeComponents.get("minutesString"),
                    timeComponents.get("seconds"), timeComponents.get("secondsString"),
                    date // Join date
            );
        }
    }
}