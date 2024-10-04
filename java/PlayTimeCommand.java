package com.whiteiverson.minecraft.playtime_plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayTimeCommand implements CommandExecutor {
    private final Main main;

    public PlayTimeCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(label.equalsIgnoreCase("playtime") || label.equalsIgnoreCase("pt"))) {
            return false; // Not the correct command
        }

        // Check if command is enabled
        boolean enabled = main.getConfig().getBoolean("commands.pt.enabled", true);

        // Check if the command is enabled
        if (!enabled) {
            sender.sendMessage(ChatColor.RED + "This command is currently disabled.");
            return true;
        }

        UUID targetUUID;
        String playerName;
        String joinDate;

        if (args.length == 0) {
            // No arguments, show the sender's play time
            if (sender instanceof Player) {
                Player player = (Player) sender;
                targetUUID = player.getUniqueId();
                playerName = "You"; // Set to "You" for self-query
                joinDate = main.getUserHandler().getUserJoinDate(targetUUID);
            } else {
                sender.sendMessage("You must be a player to use this command.");
                return true;
            }
        } else {
            playerName = args[0];

            // Check if the sender has permission to view other players' play time
            String permission = main.getConfig().getString("commands.pt.permission", "playtime.check");
            if (!(sender.hasPermission(permission))) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to view other players' playtime.");
                return true;
            }

            // First try to find the player exactly (online players only)
            Player onlinePlayer = Bukkit.getPlayerExact(playerName);

            if (onlinePlayer != null) {
                targetUUID = onlinePlayer.getUniqueId();
                playerName = onlinePlayer.getName();
            } else {
                // Use deprecated method to find offline player by name
                @SuppressWarnings("deprecation")
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

                if (offlinePlayer.hasPlayedBefore()) {
                    targetUUID = offlinePlayer.getUniqueId();
                    playerName = offlinePlayer.getName();
                } else {
                    // If no matches, send a message in red
                    sender.sendMessage(ChatColor.RED + "User doesn't exist.");
                    return true;
                }
            }

            joinDate = main.getUserHandler().getUserJoinDate(targetUUID);
        }

        // Fetch play time in seconds
        double playtime = main.getUserHandler().getPlaytime(targetUUID);

        // Calculate months, days, hours, minutes, and seconds from play time
        long totalSeconds = (long) playtime;  // Play time is already in seconds

        // Define time constants
        long secondsInAMinute = 60;
        long secondsInAnHour = 3600; // 60 * 60
        long secondsInADay = 86400;  // 60 * 60 * 24
        long secondsInAMonth = 2592000; // Approximation: 30 days * 24 hours * 60 minutes * 60 seconds

        long months = totalSeconds / secondsInAMonth;
        long remainingSecondsAfterMonths = totalSeconds % secondsInAMonth;
        long days = remainingSecondsAfterMonths / secondsInADay;
        long remainingSecondsAfterDays = remainingSecondsAfterMonths % secondsInADay;
        long hours = remainingSecondsAfterDays / secondsInAnHour;
        long remainingSecondsAfterHours = remainingSecondsAfterDays % secondsInAnHour;
        long minutes = remainingSecondsAfterHours / secondsInAMinute;
        long seconds = remainingSecondsAfterHours % secondsInAMinute;

        // Construct the message with green numbers
        String message;
        String greenMonths = ChatColor.GREEN + String.valueOf(months) + ChatColor.RESET;
        String greenDays = ChatColor.GREEN + String.valueOf(days) + ChatColor.RESET;
        String greenHours = ChatColor.GREEN + String.valueOf(hours) + ChatColor.RESET;
        String greenMinutes = ChatColor.GREEN + String.valueOf(minutes) + ChatColor.RESET;
        String greenSeconds = ChatColor.GREEN + String.valueOf(seconds) + ChatColor.RESET;
        String greenDate = ChatColor.GREEN + joinDate + ChatColor.RESET;

        // Create time strings with singular/plural handling
        String monthsString = months + " " + (months == 1 ? "month" : "months");
        String daysString = days + " " + (days == 1 ? "day" : "days");
        String hoursString = hours + " " + (hours == 1 ? "hour" : "hours");
        String minutesString = minutes + " " + (minutes == 1 ? "minute" : "minutes");
        String secondsString = seconds + " " + (seconds == 1 ? "second" : "seconds");

        if (playerName.equals("You")) {
            message = String.format("Your playtime is %s, %s, %s, %s, and %s. You joined on %s.",
                    greenMonths + " " + monthsString, greenDays + " " + daysString,
                    greenHours + " " + hoursString, greenMinutes + " " + minutesString,
                    greenSeconds + " " + secondsString, greenDate);
        } else {
            message = String.format("%s's playtime is %s, %s, %s, %s, and %s. They joined on %s.",
                    playerName, greenMonths + " " + monthsString, greenDays + " " + daysString,
                    greenHours + " " + hoursString, greenMinutes + " " + minutesString,
                    greenSeconds + " " + secondsString, greenDate);
        }

        // Send the message to the sender
        sender.sendMessage(message);
        return true;
    }
}