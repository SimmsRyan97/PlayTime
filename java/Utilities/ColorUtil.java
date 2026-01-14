// Updated ColorUtil.java with improved version parsing

package com.whiteiverson.minecraft.playtime_plugin.Utilities;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class ColorUtil {

    public String translateColor(String input) {
        if (input == null) {
            return "";
        }

        // FIXED: Handle LuckPerms hex color format (&#RRGGBB)
        if (input.contains("&#")) {
            input = translateLuckPermsHex(input);
        }

        // Check if the input starts with a '#' and is a hex code
        if (input.startsWith("#") && input.length() == 7) {
            if (isHexColorSupported()) {
                try {
                    return net.md_5.bungee.api.ChatColor.of(input).toString();
                } catch (IllegalArgumentException e) {
                    return ChatColor.RESET.toString();
                }
            } else {
                return ChatColor.RESET.toString();
            }
        }

        // Otherwise, assume it's using Minecraft's & color codes and translate them
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private String translateLuckPermsHex(String input) {
        // LuckPerms uses &#RRGGBB, we need to convert to proper format
        if (isHexColorSupported()) {
            // For 1.16+, convert &#RRGGBB to actual hex color
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})");
            java.util.regex.Matcher matcher = pattern.matcher(input);

            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String hex = matcher.group(1);
                try {
                    String replacement = net.md_5.bungee.api.ChatColor.of("#" + hex).toString();
                    matcher.appendReplacement(sb, replacement);
                } catch (IllegalArgumentException e) {
                    matcher.appendReplacement(sb, "");
                }
            }
            matcher.appendTail(sb);
            return sb.toString();
        } else {
            // For pre-1.16, just remove the hex codes
            return input.replaceAll("&#[A-Fa-f0-9]{6}", "");
        }
    }

    // Improved version parsing to handle unexpected formats
    private boolean isHexColorSupported() {
        String version = Bukkit.getServer().getBukkitVersion();
        String[] versionParts = version.split("\\.");

        try {
            int majorVersion = Integer.parseInt(versionParts[0].replaceAll("\\D", ""));
            int minorVersion = versionParts.length > 1 ? Integer.parseInt(versionParts[1].replaceAll("\\D", "")) : 0;
            return majorVersion > 1 || (majorVersion == 1 && minorVersion >= 16);
        } catch (NumberFormatException e) {
            // Failed to parse version, assume hex colors not supported
        }

        return false;
    }
}
