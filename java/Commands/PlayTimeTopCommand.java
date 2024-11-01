package com.whiteiverson.minecraft.playtime_plugin.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.whiteiverson.minecraft.playtime_plugin.Main;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class PlayTimeTopCommand implements CommandExecutor {
    private final Main main;
    private int PAGE_SIZE;
    Player player = null;

    public PlayTimeTopCommand(Main main) {
        this.main = main;
        PAGE_SIZE = main.getConfig().getInt("page-size", 10);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
        	sender.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) + main.getTranslator().getTranslation("error.player_only", null));
            return true;
        }

        Player player = (Player) sender;
        
        if (!isCommandEnabled(sender)) return true;
        
        int page = parsePageNumber(args, player);
        List<UUID> sortedPlayers = getSortedPlayers();
        int totalPlayers = sortedPlayers.size();
        int totalPages = (int) Math.ceil((double) totalPlayers / PAGE_SIZE);

        if (page < 1 || page > totalPages) {
            player.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) + main.getTranslator().getTranslation("playtime.invalid_page", player) + main.getTranslator().getTranslation("playtime.enter_page", player) + totalPages + ".");
            return true;
        }

        if (totalPlayers == 0) {
            player.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) + main.getTranslator().getTranslation("playtime.no_user", player));
            return true;
        }

        sendPlaytimeList(player, sortedPlayers, page, totalPages);
        return true;
    }

    private boolean isCommandEnabled(CommandSender sender) {
        boolean enabled = main.getConfig().getBoolean("commands.pttop.enabled", true);
        if (!enabled) {
        	sender.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) + main.getTranslator().getTranslation("error.command_disabled", player));
        } else {
            if (!sender.hasPermission("playtime.top")) {
            	sender.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) + main.getTranslator().getTranslation("error.no_permission", player));
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
                player.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) + main.getTranslator().getTranslation("playtime.invalid_page", player) + main.getTranslator().getTranslation("playtime.page_one", player) );
            }
        }
        return page;
    }

    private void sendPlaytimeList(Player player, List<UUID> sortedPlayers, int page, int totalPages) {
        player.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.header")) + "=== " + main.getTranslator().getTranslation("playtime.top", player) + page + "/" + totalPages + ") ===");

        int startIndex = (page - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, sortedPlayers.size());

        for (int i = startIndex; i < endIndex; i++) {
            UUID uuid = sortedPlayers.get(i);
            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
            double playtime = main.getUserHandler().getPlaytime(uuid);
            
            // Format the index properly
            String indexStr = main.getColorUtil().translateColor(main.getConfig().getString("color.list-item")) + (i + 1) + ".";
            
            player.sendMessage(String.format("%s %s",
                indexStr,
                main.getColorUtil().translateColor(main.getConfig().getString("color.user")) + playerName + ": " + ChatColor.RESET + formatPlaytime(playtime, player)));
        }

        if (page < totalPages) {
            player.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.footer")) + "========= " + main.getTranslator().getTranslation("playtime.go_to", player) + (page + 1) + " =========");
        } else {
            player.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.footer")) + "===========================");
        }
    }

    private List<UUID> getSortedPlayers() {
        // Define the path to the data folder
        File dataFolder = new File(main.getDataFolder(), "data");

        // If the data folder doesn't exist, return an empty list
        if (!dataFolder.exists() || !dataFolder.isDirectory()) {
            return Collections.emptyList();
        }

        return Arrays.stream(Bukkit.getOfflinePlayers())
                .map(OfflinePlayer::getUniqueId)
                // Filter to include only players with a data file (uuid.yml) in the data folder
                .filter(uuid -> new File(dataFolder, uuid + ".yml").exists())
                // Only include players with non-zero play time
                .filter(uuid -> main.getUserHandler().getPlaytime(uuid) > 0)
                // Sort the players by their play time in descending order
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
                timeComponents.get("months"), (months),
                timeComponents.get("days"), (days),
                timeComponents.get("hours"), (hours),
                timeComponents.get("minutes"), (minutes),
                timeComponents.get("seconds"), (seconds));
    }
}