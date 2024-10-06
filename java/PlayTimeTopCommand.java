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
        if (!isCommandEnabled(sender)) return true;

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;
        int page = parsePageNumber(args, player);
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

        sendPlaytimeList(player, sortedPlayers, page, totalPages);
        return true;
    }

    private boolean isCommandEnabled(CommandSender sender) {
        boolean enabled = main.getConfig().getBoolean("commands.pttop.enabled", true);
        if (!enabled) {
            sender.sendMessage(ChatColor.RED + "This command is currently disabled.");
        } else {
            String permission = main.getConfig().getString("commands.pttop.permission", "playtime.top");
            if (!sender.hasPermission(permission)) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return false;
            }
        }
        return enabled;
    }

    private int parsePageNumber(String[] args, Player player) {
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid page number. Showing page 1.");
            }
        }
        return page;
    }

    private void sendPlaytimeList(Player player, List<UUID> sortedPlayers, int page, int totalPages) {
        player.sendMessage(ChatColor.GOLD + "=== Top Playtime (Page " + page + "/" + totalPages + ") ===");

        int startIndex = (page - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, sortedPlayers.size());

        for (int i = startIndex; i < endIndex; i++) {
            UUID uuid = sortedPlayers.get(i);
            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
            double playtime = main.getUserHandler().getPlaytime(uuid);
            player.sendMessage((i + 1) + ". " + playerName + ": " + formatPlaytime(playtime));
        }

        if (page < totalPages) {
            player.sendMessage(ChatColor.GOLD + "========= Go to page " + (page + 1) + " =========");
        } else {
            player.sendMessage(ChatColor.GOLD + "===========================");
        }
    }

    private List<UUID> getSortedPlayers() {
        return Arrays.stream(Bukkit.getOfflinePlayers())
                .map(OfflinePlayer::getUniqueId)
                .filter(uuid -> main.getUserHandler().getPlaytime(uuid) > 0)
                .sorted(Comparator.comparingDouble(main.getUserHandler()::getPlaytime).reversed())
                .collect(Collectors.toList());
    }

    private String formatPlaytime(double playtime) {
        long totalSeconds = (long) playtime;
        Map<String, String> timeComponents = Main.calculatePlaytime(totalSeconds);
        return String.format("%s %s, %s %s, %s %s, %s %s, %s %s",
                timeComponents.get("greenMonths"), timeComponents.get("monthsString"),
                timeComponents.get("greenDays"), timeComponents.get("daysString"),
                timeComponents.get("greenHours"), timeComponents.get("hoursString"),
                timeComponents.get("greenMinutes"), timeComponents.get("minutesString"),
                timeComponents.get("greenSeconds"), timeComponents.get("secondsString"));
    }
}