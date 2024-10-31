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
                long days = playtimeSeconds / 86400;
                String daysString = days == 1 
                    ? main.getTranslator().getTranslation("playtime.time.days.singular", offlinePlayer)
                    : main.getTranslator().getTranslation("playtime.time.days.plural", offlinePlayer);
                return  main.getColorUtil().translateColor(main.getConfig().getString("color.integer"))  + days + " " + daysString;

            case "playtime_in_hours":
                long hours = playtimeSeconds / 3600;
                String hoursString = hours == 1 
                    ? main.getTranslator().getTranslation("playtime.time.hours.singular", offlinePlayer)
                    : main.getTranslator().getTranslation("playtime.time.hours.plural", offlinePlayer);
                return  main.getColorUtil().translateColor(main.getConfig().getString("color.integer"))  + hours + " " + hoursString;

            case "playtime_in_minutes":
                long minutes = playtimeSeconds / 60;
                String minutesString = minutes == 1 
                    ? main.getTranslator().getTranslation("playtime.time.minutes.singular", offlinePlayer)
                    : main.getTranslator().getTranslation("playtime.time.minutes.plural", offlinePlayer);
                return main.getColorUtil().translateColor(main.getConfig().getString("color.integer"))  + minutes + " " + minutesString;

            case "playtime_in_seconds":
                String secondsString = playtimeSeconds == 1 
                    ? main.getTranslator().getTranslation("playtime.time.seconds.singular", offlinePlayer)
                    : main.getTranslator().getTranslation("playtime.time.seconds.plural", offlinePlayer);
                return main.getColorUtil().translateColor(main.getConfig().getString("color.integer"))  + playtimeSeconds + " " + secondsString;

            case "join_date":
                return joinDate;

            default:
                return null;
        }
    }
}