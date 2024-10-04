package com.whiteiverson.minecraft.playtime_plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PlayTimeTopCommand implements CommandExecutor {
    private final Main main;
    private static final int PAGE_SIZE = 10;

    public PlayTimeTopCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if command is enabled
        boolean enabled = main.getConfig().getBoolean("commands.pttop.enabled", true);
        
        // Check if the command is enabled
        if (!enabled) {
            sender.sendMessage(ChatColor.RED + "This command is currently disabled.");
            return true;
        }
    	
        // Retrieve the permission from the config.yml
        String permission = main.getConfig().getString("commands.pttop.permission", "playtime.top");

        // Check if the sender has the necessary permission
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;
        int page = 1;

        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid page number. Showing page 1.");
            }
        }

        List<UUID> sortedPlayers = getSortedPlayers();
        int totalPlayers = sortedPlayers.size();
        int totalPages = (int) Math.ceil((double) totalPlayers / PAGE_SIZE);

        if (page < 1 || page > totalPages) {
            player.sendMessage(ChatColor.RED + "Invalid page number. Please enter a number between 1 and " + totalPages + ".");
            return true;
        }

        if (totalPlayers == 0) {
            player.sendMessage(ChatColor.RED + "No players found.");
            return true;
        }

        // Sending the page title in gold
        player.sendMessage(ChatColor.GOLD + "=== Top Playtime (Page " + page + "/" + totalPages + ") ===");

        int startIndex = (page - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalPlayers);

        // Initialise the rank number based on the starting index.
        for (int i = startIndex; i < endIndex; i++) {
            UUID uuid = sortedPlayers.get(i);
            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
            double playtime = main.getUserHandler().getPlaytime(uuid);

            // The rank is calculated as i + 1 because 'i' is zero-based
            int rank = i + 1;

            // Send the message with the rank, player name, and formatted play time
            player.sendMessage(rank + ". " + playerName + ": " + formatPlaytime(playtime));
        }

        if (page < totalPages) {
            player.sendMessage(ChatColor.GOLD + "========= Go to page " + (page + 1) + " =========");
        } else {
            player.sendMessage(ChatColor.GOLD + "===============================");
        }

        return true;
    }

    private List<UUID> getSortedPlayers() {
        Map<UUID, Double> playerPlaytimeMap = new HashMap<>();

        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            UUID uuid = offlinePlayer.getUniqueId();
            double playtime = main.getUserHandler().getPlaytime(uuid);

            // Only include players with play time greater than 0
            if (playtime > 0) {
                playerPlaytimeMap.put(uuid, playtime);
            }
        }

        return playerPlaytimeMap.entrySet().stream()
            .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    private String formatPlaytime(double playtime) {
        // Fetch play time in seconds
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

        // Formatting play time with green colour and singular/plural
        return ChatColor.GREEN + String.format("%d %s, %d %s, %d %s, %d %s, %d %s",
                months, months == 1 ? "month" : "months",
                days, days == 1 ? "day" : "days",
                hours, hours == 1 ? "hour" : "hours",
                minutes, minutes == 1 ? "minute" : "minutes",
                seconds, seconds == 1 ? "second" : "seconds");
    }
}