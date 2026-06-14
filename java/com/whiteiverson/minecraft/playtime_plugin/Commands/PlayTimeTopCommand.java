package com.whiteiverson.minecraft.playtime_plugin.Commands;

import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.whiteiverson.minecraft.playtime_plugin.Main;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class PlayTimeTopCommand implements CommandExecutor {
    private final Main main;
    private final Chat vaultChat; // Vault's Chat object
    private int PAGE_SIZE;

    public PlayTimeTopCommand(Main main) {
        this.main = main;
        this.vaultChat = main.getChat(); // Retrieve Vault's Chat object from the main class
        PAGE_SIZE = main.getConfig().getInt("page-size", 10);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) 
                + main.getTranslator().getTranslation("error.player_only", null));
            return true;
        }

        Player player = (Player) sender;

        if (!isCommandEnabled(sender)) return true;

        int page = parsePageNumber(args, player);
        List<UUID> sortedPlayers = getSortedPlayers();
        int totalPlayers = sortedPlayers.size();
        int totalPages = (int) Math.ceil((double) totalPlayers / PAGE_SIZE);

        if (page < 1 || page > totalPages) {
            player.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) 
                + main.getTranslator().getTranslation("playtime.invalid_page", player) 
                + main.getTranslator().getTranslation("playtime.enter_page", player) + totalPages + ".");
            return true;
        }

        if (totalPlayers == 0) {
            player.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) 
                + main.getTranslator().getTranslation("playtime.no_user", player));
            return true;
        }

        sendPlaytimeList(player, sortedPlayers, page, totalPages);
        return true;
    }

    private boolean isCommandEnabled(CommandSender sender) {
        boolean enabled = main.getConfig().getBoolean("commands.pttop.enabled", true);
        if (!enabled) {
            sender.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) 
                + main.getTranslator().getTranslation("error.command_disabled", null));
        } else if (!sender.hasPermission("playtime.top")) {
            sender.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) 
                + main.getTranslator().getTranslation("error.no_permission", null));
            return false;
        }
        return enabled;
    }

    private int parsePageNumber(String[] args, Player player) {
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) 
                    + main.getTranslator().getTranslation("playtime.invalid_page", player) 
                    + main.getTranslator().getTranslation("playtime.page_one", player));
            }
        }
        return page;
    }

    private void sendPlaytimeList(Player player, List<UUID> sortedPlayers, int page, int totalPages) {
        player.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.header")) 
            + "=== " + main.getTranslator().getTranslation("playtime.top", player) + page + "/" + totalPages + ") ===");

        int startIndex = (page - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, sortedPlayers.size());

        for (int i = startIndex; i < endIndex; i++) {
            UUID uuid = sortedPlayers.get(i);
            String displayName = resolveDisplayName(uuid);

            double playtime = main.getUserHandler().getPlaytime(uuid);

            String indexStr = main.getColorUtil().translateColor(main.getConfig().getString("color.list-item")) + (i + 1) + ".";

            player.sendMessage(String.format("%s %s",
                    indexStr,
                    main.getColorUtil().translateColor(main.getConfig().getString("color.user")) + displayName + ": " + ChatColor.RESET + formatPlaytime(playtime, player)));
        }

        if (page < totalPages) {
            player.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.footer")) 
                + "======== " + main.getTranslator().getTranslation("playtime.go_to", player) + (page + 1) + " ========");
        } else {
            player.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.footer")) + "===========================");
        }
    }

    private List<UUID> getSortedPlayers() {
        if (main.getDatabaseManager() != null && main.getDatabaseManager().isEnabled()) {
            try {
                return main.getUserDataManager().getAllUserUUIDs().stream()
                        .map(UUID::fromString)
                        .collect(Collectors.toList());
            } catch (SQLException e) {
                main.getLogger().severe("Failed to get player list from database: " + e.getMessage());
                return Collections.emptyList();
            }
        } else {
            // Your existing file-based code
            File dataFolder = new File(main.getDataFolder(), "data");

            if (!dataFolder.exists() || !dataFolder.isDirectory()) {
                return Collections.emptyList();
            }

            return Arrays.stream(Bukkit.getOfflinePlayers())
                    .map(OfflinePlayer::getUniqueId)
                    .filter(uuid -> new File(dataFolder, uuid + ".yml").exists())
                    .filter(uuid -> main.getUserHandler().getPlaytime(uuid) > 0)
                    .sorted(Comparator.comparingDouble(main.getUserHandler()::getPlaytime).reversed())
                    .collect(Collectors.toList());
        }
    }

    private String resolveDisplayName(UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        Player onlinePlayer = offlinePlayer.getPlayer();

        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            if (vaultChat != null) {
                String prefix = vaultChat.getPlayerPrefix(onlinePlayer);
                String suffix = vaultChat.getPlayerSuffix(onlinePlayer);
                String displayName = onlinePlayer.getName(); // Use actual name, not display name

                // FIXED: Translate color codes instead of stripping them
                String coloredPrefix = prefix != null ? main.getColorUtil().translateColor(prefix) : "";
                String coloredSuffix = suffix != null ? main.getColorUtil().translateColor(suffix) : "";

                return coloredPrefix + displayName + coloredSuffix;
            }
            return onlinePlayer.getName();
        }

        return offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
    }

    private String formatPlaytime(double playtime, Player player) {
        long totalSeconds = (long) playtime;
        Map<String, String> timeComponents = Main.calculatePlaytime(totalSeconds, main, player, main.getTranslator());

        return String.format("%s %s, %s %s, %s %s, %s %s, %s %s",
                timeComponents.get("months"), timeComponents.get("monthsString"),
                timeComponents.get("days"), timeComponents.get("daysString"),
                timeComponents.get("hours"), timeComponents.get("hoursString"),
                timeComponents.get("minutes"), timeComponents.get("minutesString"),
                timeComponents.get("seconds"), timeComponents.get("secondsString"));
    }
}
