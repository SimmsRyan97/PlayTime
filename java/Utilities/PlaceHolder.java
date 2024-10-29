package com.whiteiverson.minecraft.playtime_plugin.Utilities;

import java.util.Map;

import org.bukkit.entity.Player;

import com.whiteiverson.minecraft.playtime_plugin.Main;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PlaceHolder extends PlaceholderExpansion {
	private final Main main;
	
	public PlaceHolder() {
		this.main = Main.getInstance();
    }

    @Override
    public String getIdentifier() {
        return "playtimeplugin"; // The identifier used in PlaceholderAPI
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
    public String onPlaceholderRequest(Player player, String identifier) {
        // Check if player is null (could happen if placeholder is used in console)
        if (player == null) {
            return "";
        }
        
        long playtimeSeconds = (long) main.getUserHandler().getPlaytime(player.getUniqueId());
        String joinDate = main.getUserHandler().getUserJoinDate(player.getUniqueId());
        Map<String, String> timeComponents = Main.calculatePlaytime(playtimeSeconds, main, player, main.getTranslator());

        // Add specific placeholders
        switch (identifier) {
            case "playtime":
                // Concatenate formatted playtime components
                return String.format("%s %s %s %s %s",
                    timeComponents.get("months") + " " + timeComponents.get("monthsString"),
                    timeComponents.get("days") + " " + timeComponents.get("daysString"),
                    timeComponents.get("hours") + " " + timeComponents.get("hoursString"),
                    timeComponents.get("minutes") + " " + timeComponents.get("minutesString"),
                    timeComponents.get("seconds") + " " + timeComponents.get("secondsString")
                ).trim();

            case "playtime_in_months":
                return timeComponents.get("months") + " " + timeComponents.get("monthsString");
            case "playtime_in_days":
                return timeComponents.get("days") + " " + timeComponents.get("daysString");
            case "playtime_in_hours":
                long hours = playtimeSeconds / 3600; // Total hours
                return String.valueOf(hours) + " " + (hours == 1 ? "hour" : "hours");
            case "playtime_in_minutes":
                long minutes = playtimeSeconds / 60; // Total minutes
                return String.valueOf(minutes) + " " + (minutes == 1 ? "minute" : "minutes");
            case "playtime_in_seconds":
                return String.valueOf(playtimeSeconds) + " " + (playtimeSeconds == 1 ? "second" : "seconds");
            case "playtime_join_date":
                return joinDate;

            // Add more placeholders as needed
            default:
                return null; // Placeholder wasn't found
        }
    }
}
