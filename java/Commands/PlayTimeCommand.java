package com.whiteiverson.minecraft.playtime_plugin.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.whiteiverson.minecraft.playtime_plugin.Main;

import java.util.Map;
import java.util.UUID;

public class PlayTimeCommand implements CommandExecutor {
    private final Main main;
    Player player = null;

    public PlayTimeCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Check command label
        if (!(label.equalsIgnoreCase("playtime") || label.equalsIgnoreCase("pt"))) {
            return false; // Not the correct command
        }

        if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            sender.sendMessage(ChatColor.RED + main.getTranslator().getConsoleTranslation("error.player_only"));
            return true;
        }

        // Check if the command is enabled
        if (!main.getConfig().getBoolean("commands.pt.enabled", true)) {
            sender.sendMessage(ChatColor.RED + main.getTranslator().getTranslation("error.command_disabled", player));
            return true;
        }

        // Initialise UUID and playerName variables
        UUID targetUUID;
        String playerName;
        String joinDate;

        // Handle self-query or other player queries
        if (args.length == 0) {
            // No username provided, querying self
            targetUUID = player.getUniqueId();
            playerName = "You";  // Set to "You" explicitly for self-query
            joinDate = main.getUserHandler().getUserJoinDate(targetUUID);
        } else {
            // Handle other player queries
            playerName = args[0];

            // Check permission to view other players' play time
            if (!sender.hasPermission("playtime.check")) {
                sender.sendMessage(ChatColor.RED + main.getTranslator().getTranslation("error.no_permission", player));
                return true;
            }

            // Find the target player (online or offline)
            Player onlinePlayer = Bukkit.getPlayerExact(playerName);
            if (onlinePlayer != null) {
                targetUUID = onlinePlayer.getUniqueId();
                playerName = onlinePlayer.getName();
            } else {
                @SuppressWarnings("deprecation")
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                if (offlinePlayer.hasPlayedBefore()) {
                    targetUUID = offlinePlayer.getUniqueId();
                    playerName = offlinePlayer.getName();
                } else {
                    sender.sendMessage(ChatColor.RED + main.getTranslator().getTranslation("error.no_user", player));
                    return true;
                }
            }

            joinDate = main.getUserHandler().getUserJoinDate(targetUUID);

            // Check if the username is the same as the sender's username
            if (targetUUID.equals(player.getUniqueId())) {
                playerName = "You";  // Set to "You" if the player is querying their own play time
            }
        }

        // Fetch play time in seconds and convert it into time components
        double playtime = main.getUserHandler().getPlaytime(targetUUID);
        long totalSeconds = (long) playtime;

        // Pass the translator instance to calculatePlaytime method
        Map<String, String> timeComponents = Main.calculatePlaytime(totalSeconds, main, sender, main.getTranslator());
        String greenDate = ChatColor.GREEN + joinDate + ChatColor.RESET;

        // Prepare the message based on self or other player query
        String message = formatPlaytimeMessage(playerName, timeComponents, greenDate);

        // Send the message to the sender
        sender.sendMessage(message);
        return true;
    }

    private String formatPlaytimeMessage(String playerName, Map<String, String> timeComponents, String greenDate) {
        String translationKey = playerName.equals("You") ? "playtime.self" : "playtime.other";
        
        // Prepare the message with correct arguments for String.format
        String messageTemplate = main.getTranslator().getTranslation(translationKey, player);
        
        main.getLogger().info("Message Template: " + messageTemplate);
        
        return String.format(messageTemplate,
                timeComponents.get("greenMonths"), timeComponents.get("monthsString"),
                timeComponents.get("greenDays"), timeComponents.get("daysString"),
                timeComponents.get("greenHours"), timeComponents.get("hoursString"),
                timeComponents.get("greenMinutes"), timeComponents.get("minutesString"),
                timeComponents.get("greenSeconds"), timeComponents.get("secondsString"),
                greenDate // Join date
        );
    }
}
