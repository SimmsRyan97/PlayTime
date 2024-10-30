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

    public String onPlaceholderRequest(OfflinePlayer offlinePlayer, String identifier) {
        if (offlinePlayer == null || offlinePlayer.getUniqueId() == null) {
            return "";
        }
        
        UUID playerUUID = offlinePlayer.getUniqueId();
        long playtimeSeconds = (long) main.getUserHandler().getPlaytime(playerUUID);
        String joinDate = main.getUserHandler().getUserJoinDate(playerUUID);

        Map<String, String> timeComponents = Main.calculatePlaytime(playtimeSeconds, main, offlinePlayer, main.getTranslator());

        switch (identifier) {
            case "total_playtime":
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
                long hours = playtimeSeconds / 3600;
                return hours + " " + (hours == 1 ? "hour" : "hours");
            case "playtime_in_minutes":
                long minutes = playtimeSeconds / 60;
                return minutes + " " + (minutes == 1 ? "minute" : "minutes");
            case "playtime_in_seconds":
                return playtimeSeconds + " " + (playtimeSeconds == 1 ? "second" : "seconds");
            case "playtime_join_date":
                return joinDate;

            default:
                return null;
        }
    }
}
