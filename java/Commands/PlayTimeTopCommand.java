package com.whiteiverson.minecraft.playtime_plugin.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.whiteiverson.minecraft.playtime_plugin.Main;

import java.util.*;
import java.util.stream.Collectors;

public class PlayTimeTopCommand implements CommandExecutor {
    private final Main main;
    private static final int PAGE_SIZE = 10;
    Player player = null;

    public PlayTimeTopCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
        	sender.sendMessage(ChatColor.RED + main.getTranslator().getConsoleTranslation("error.player_only"));
            return true;
        }

        Player player = (Player) sender;
        
        if (!isCommandEnabled(sender)) return true;
        
        int page = parsePageNumber(args, player);
        List<UUID> sortedPlayers = getSortedPlayers();
        int totalPlayers = sortedPlayers.size();
        int totalPages = (int) Math.ceil((double) totalPlayers / PAGE_SIZE);

        if (page < 1 || page > totalPages) {
            player.sendMessage(ChatColor.RED + main.getTranslator().getTranslation("playtime.invalid_page", player) + main.getTranslator().getTranslation("playtime.enter_page", player) + totalPages + ".");
            return true;
        }

        if (totalPlayers == 0) {
            player.sendMessage(ChatColor.RED + main.getTranslator().getTranslation("playtime.no_user", player));
            return true;
        }

        sendPlaytimeList(player, sortedPlayers, page, totalPages);
        return true;
    }

    private boolean isCommandEnabled(CommandSender sender) {
        boolean enabled = main.getConfig().getBoolean("commands.pttop.enabled", true);
        if (!enabled) {
        	sender.sendMessage(ChatColor.RED + main.getTranslator().getTranslation("error.command_disabled", player));
        } else {
            String permission = main.getConfig().getString("commands.pttop.permission", "playtime.top");
            if (!sender.hasPermission(permission)) {
            	sender.sendMessage(ChatColor.RED + main.getTranslator().getTranslation("error.no_permission", player));
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
                player.sendMessage(ChatColor.RED + main.getTranslator().getTranslation("playtime.invalid_page", player) + main.getTranslator().getTranslation("playtime.page_one", player) );
            }
        }
        return page;
    }

    private void sendPlaytimeList(Player player, List<UUID> sortedPlayers, int page, int totalPages) {
        player.sendMessage(ChatColor.GOLD + "=== " + main.getTranslator().getTranslation("playtime.top", player) + page + "/" + totalPages + ") ===");

        int startIndex = (page - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, sortedPlayers.size());

        for (int i = startIndex; i < endIndex; i++) {
            UUID uuid = sortedPlayers.get(i);
            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
            double playtime = main.getUserHandler().getPlaytime(uuid);
            player.sendMessage(String.format("%d. %s: %s",
                    i + 1, playerName, formatPlaytime(playtime, player)));
        }

        if (page < totalPages) {
            player.sendMessage(ChatColor.GOLD + "========= " + main.getTranslator().getTranslation("playtime.go_to", player) + (page + 1) + " =========");
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

    private String formatPlaytime(double playtime, Player player) {
        long totalSeconds = (long) playtime;
        Map<String, String> timeComponents = Main.calculatePlaytime(totalSeconds, main, player, main.getTranslator());
        
        // Using translations for each time component
        String months = timeComponents.get("monthsString");
        String days = timeComponents.get("daysString");
        String hours = timeComponents.get("hoursString");
        String minutes = timeComponents.get("minutesString");
        String seconds = timeComponents.get("secondsString");
        
        return String.format("%s %s, %s %s, %s %s, %s %s, %s %s",
                timeComponents.get("greenMonths"), (months),
                timeComponents.get("greenDays"), (days),
                timeComponents.get("greenHours"), (hours),
                timeComponents.get("greenMinutes"), (minutes),
                timeComponents.get("greenSeconds"), (seconds));
    }
}