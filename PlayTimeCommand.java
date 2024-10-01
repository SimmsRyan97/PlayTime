package com.whiteiverson.minecraft.playtime_plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayTimeCommand implements CommandExecutor {
    private Main main;

    public PlayTimeCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        UUID targetUUID;
        String playerName;
        String joinDate;

        if (args.length == 0) {
            // No arguments, show the sender's play time
            if (sender instanceof Player) {
                Player player = (Player) sender;
                targetUUID = player.getUniqueId();
                playerName = "You"; // Set to "You" for self-query
                joinDate = main.getUserHandler().getUserJoinDate(targetUUID); // Fetch join date for the player
            } else {
                sender.sendMessage("You must be a player to use this command.");
                return true;
            }
        } else {
            playerName = args[0];
            List<OfflinePlayer> matchedPlayers = findMatchingPlayers(playerName);

            // Handle matching logic
            if (matchedPlayers.isEmpty()) {
                sender.sendMessage("User doesn't exist.");
                return true;
            } else if (matchedPlayers.size() > 1) {
                // If more than one match, suggest options
                StringBuilder suggestions = new StringBuilder("Did you mean: ");
                for (OfflinePlayer matched : matchedPlayers) {
                    suggestions.append(matched.getName()).append(", ");
                }
                // Remove last comma and space
                suggestions.setLength(suggestions.length() - 2);
                sender.sendMessage(suggestions.toString());
                return true;
            }

            // We have exactly one match
            targetUUID = matchedPlayers.get(0).getUniqueId();
            joinDate = main.getUserHandler().getUserJoinDate(targetUUID);
        }

        // Fetch play time
        double playtimeInMinutes = main.getUserHandler().getPlaytime(targetUUID); // Updated method name

        // Calculate days, hours, minutes, and seconds from play time
        long totalSeconds = (long) (playtimeInMinutes * 60);
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        // Check AFK status
        boolean isAFK = main.getUserHandler().isAfk(targetUUID);
        String afkMessage = isAFK ? " (AFK time not included)" : "";

        // Construct the message with green numbers
        String message;
        String greenDays = ChatColor.GREEN + String.valueOf(days) + ChatColor.RESET;
        String greenHours = ChatColor.GREEN + String.valueOf(hours) + ChatColor.RESET;
        String greenMinutes = ChatColor.GREEN + String.valueOf(minutes) + ChatColor.RESET;
        String greenSeconds = ChatColor.GREEN + String.valueOf(seconds) + ChatColor.RESET;

        if (playerName.equals("You")) {
            message = String.format("Your playtime is %s days, %s hours, %s minutes, and %s seconds.%s You joined on %s.",
                    greenDays, greenHours, greenMinutes, greenSeconds, afkMessage, joinDate);
        } else {
            message = String.format("%s's playtime is %s days, %s hours, %s minutes, and %s seconds.%s They joined on %s.",
                    playerName, greenDays, greenHours, greenMinutes, greenSeconds, afkMessage, joinDate);
        }

        // Send the message to the sender
        sender.sendMessage(message);
        return true;
    }

    private List<OfflinePlayer> findMatchingPlayers(String partialName) {
        List<OfflinePlayer> matchedPlayers = new ArrayList<>();

        // Check online players
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getName().toLowerCase().startsWith(partialName.toLowerCase())) {
                matchedPlayers.add(onlinePlayer);
            }
        }

        // Check offline players
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getName() != null && offlinePlayer.getName().toLowerCase().startsWith(partialName.toLowerCase())) {
                matchedPlayers.add(offlinePlayer);
            }
        }

        return matchedPlayers;
    }
}