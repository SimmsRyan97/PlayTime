package com.whiteiverson.minecraft.playtime_plugin.Commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatColor;

import com.whiteiverson.minecraft.playtime_plugin.Main;

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
            return false;
        }

        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            sender.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) + 
                main.getTranslator().getConsoleTranslation("error.player_only"));
            return true;
        }

        if (!main.getConfig().getBoolean("commands.pt.enabled", true)) {
            sender.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) + 
                main.getTranslator().getTranslation("error.command_disabled", player));
            return true;
        }

        UUID targetUUID;
        String playerName;
        String joinDate;

        if (args.length == 0) {
            targetUUID = player.getUniqueId();
            playerName = "You";
            joinDate = main.getUserHandler().getUserJoinDate(targetUUID);
        } else {
            playerName = args[0];

            if (!sender.hasPermission("playtime.check")) {
                sender.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) + 
                    main.getTranslator().getTranslation("error.no_permission", player));
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
                    sender.sendMessage(main.getColorUtil().translateColor(main.getConfig().getString("color.error")) + 
                        main.getTranslator().getTranslation("error.no_user", player));
                    return true;
                }
            }

            joinDate = main.getUserHandler().getUserJoinDate(targetUUID);

            if (targetUUID.equals(player.getUniqueId())) {
                playerName = "You";
            }
        }

        double playtime = main.getUserHandler().getPlaytime(targetUUID);
        long totalSeconds = (long) playtime;

        Map<String, String> timeComponents = Main.calculatePlaytime(totalSeconds, main, sender, main.getTranslator());
        String date = main.getColorUtil().translateColor(main.getConfig().getString("color.interval")) + joinDate + ChatColor.RESET;

        String message = formatPlaytimeMessage(playerName, timeComponents, date, player);

        sender.sendMessage(message);
        return true;
    }

    private String formatPlaytimeMessage(String playerName, Map<String, String> timeComponents, String date, Player player) {
        String translationKey = playerName.equals("You") ? "playtime.self" : "playtime.other";
        
        // Prepare the message with correct arguments for String.format
        String messageTemplate = player != null 
                ? main.getTranslator().getTranslation(translationKey, player) 
                : main.getTranslator().getTranslation(translationKey, null); // Handle console case

        return String.format(messageTemplate,
                timeComponents.get("months"), timeComponents.get("monthsString"),
                timeComponents.get("days"), timeComponents.get("daysString"),
                timeComponents.get("hours"), timeComponents.get("hoursString"),
                timeComponents.get("minutes"), timeComponents.get("minutesString"),
                timeComponents.get("seconds"), timeComponents.get("secondsString"),
                date // Join date
        );
    }
}