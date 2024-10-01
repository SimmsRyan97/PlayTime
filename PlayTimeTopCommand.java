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
        }

        return true;
    }

    private List<UUID> getSortedPlayers() {
        Map<UUID, Double> playerPlaytimeMap = new HashMap<>();

        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            UUID uuid = offlinePlayer.getUniqueId();
            double playtime = main.getUserHandler().getPlaytime(uuid);
            playerPlaytimeMap.put(uuid, playtime);
        }

        return playerPlaytimeMap.entrySet().stream()
            .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    private String formatPlaytime(double playtime) {
        long totalSeconds = (long) playtime;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        // Formatting play time with green colour
        return ChatColor.GREEN + String.format("%d days, %d hours, %d minutes, %d seconds", days, hours, minutes, seconds);
    }
}