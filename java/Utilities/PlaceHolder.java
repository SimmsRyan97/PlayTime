package com.whiteiverson.minecraft.playtime_plugin.Utilities;

import java.util.Map;
import java.util.UUID;

import org.bukkit.OfflinePlayer;

import com.whiteiverson.minecraft.playtime_plugin.Main;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PlaceHolder extends PlaceholderExpansion {
    private final Main main;
    
    public PlaceHolder() {
        this.main = Main.getInstance();
    }

    @Override
    public String getIdentifier() {
        return "playtime"; // The identifier used in PlaceholderAPI
    }

    @Override
    public String getAuthor() {
        return main.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return main.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // This ensures the placeholder stays registered
    }
    
    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String identifier) {
        if (offlinePlayer == null) {
            return "";  // Safely return an empty string if called without a player
        }
        
        UUID playerUUID = offlinePlayer.getUniqueId();
        long playtimeSeconds = (long) main.getUserHandler().getPlaytime(playerUUID);
        String joinDate = main.getUserHandler().getUserJoinDate(playerUUID);

        Map<String, String> timeComponents = Main.calculatePlaytime(playtimeSeconds, main, offlinePlayer, main.getTranslator());

        switch (identifier) {
            case "total":
                // Show time with intervals in total playtime
                return String.format("%s %s %s %s %s",
                    timeComponents.get("months") + " " + timeComponents.get("monthsString"),
                    timeComponents.get("days") + " " + timeComponents.get("daysString"),
                    timeComponents.get("hours") + " " + timeComponents.get("hoursString"),
                    timeComponents.get("minutes") + " " + timeComponents.get("minutesString"),
                    timeComponents.get("seconds") + " " + timeComponents.get("secondsString")
                ).trim();

            case "in_months":
            	// Calculate and display only numeric months
            	long months = playtimeSeconds / 2628000; // Approximate amount of seconds in an average month
            	return main.getColorUtil().translateColor(main.getConfig().getString("color.integer")) + months;

            case "in_days":
                // Calculate and display only numeric days
                long days = playtimeSeconds / 86400;
                return main.getColorUtil().translateColor(main.getConfig().getString("color.integer")) + days;

            case "in_hours":
                // Calculate and display only numeric hours
                long hours = playtimeSeconds / 3600;
                return main.getColorUtil().translateColor(main.getConfig().getString("color.integer")) + hours;

            case "in_minutes":
                // Calculate and display only numeric minutes
                long minutes = playtimeSeconds / 60;
                return main.getColorUtil().translateColor(main.getConfig().getString("color.integer")) + minutes;

            case "in_seconds":
                // Display only numeric seconds
                return main.getColorUtil().translateColor(main.getConfig().getString("color.integer")) + playtimeSeconds;

            case "join_date":
            	return main.getColorUtil().translateColor(main.getConfig().getString("color.integer")) + joinDate;

            default:
                return null;
        }
    }
}