package com.whiteiverson.minecraft.playtime_plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
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
            	sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
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
        long totalSeconds = (long) playtime;
        
        // Fetch the time components using the helper method
        Map<String, String> timeComponents = Main.calculatePlaytime(totalSeconds);
        
        String greenDate = ChatColor.GREEN + joinDate + ChatColor.RESET;
        String message;

        if (playerName.equals("You")) {
            message = String.format("Your playtime is %s %s, %s %s, %s %s, %s %s, and %s %s. You joined on %s.",
	                    timeComponents.get("greenMonths"), timeComponents.get("monthsString"),
	                    timeComponents.get("greenDays"), timeComponents.get("daysString"),
	                    timeComponents.get("greenHours"), timeComponents.get("hoursString"),
	                    timeComponents.get("greenMinutes"), timeComponents.get("minutesString"),
	                    timeComponents.get("greenSeconds"), timeComponents.get("secondsString"),
	                    greenDate
                    );
        } else {
            message = String.format("%s's playtime is %s %s, %s %s, %s %s, %s %s, and %s %s. They joined on %s.",
	                    playerName, timeComponents.get("greenMonths"), timeComponents.get("monthsString"),
	                    timeComponents.get("greenDays"), timeComponents.get("daysString"),
	                    timeComponents.get("greenHours"), timeComponents.get("hoursString"),
	                    timeComponents.get("greenMinutes"), timeComponents.get("minutesString"),
	                    timeComponents.get("greenSeconds"), timeComponents.get("secondsString"),
	                    greenDate
                    );
        }

        // Send the message to the sender
        sender.sendMessage(message);
        return true;
    }
}