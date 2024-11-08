// Updated ColorUtil.java with improved version parsing

package com.whiteiverson.minecraft.playtime_plugin.Utilities;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class ColorUtil {

    public String translateColor(String input) {
        if (input == null) {
            return ""; // Return empty string if null
        }

        // Check if the input starts with a '#' and is a hex code
        if (input.startsWith("#") && input.length() == 7) {
            // Only use hex colors for 1.16+ (where ChatColor.of() is available)
            if (isHexColorSupported()) {
                try {
                    // Try to parse as a hex color for 1.16+ servers
                    return net.md_5.bungee.api.ChatColor.of(input).toString();
                } catch (IllegalArgumentException e) {
                    // Invalid hex code, handle if necessary (e.g., log it, use default color, etc.)
                    return ChatColor.RESET.toString(); // Default to reset if color is invalid
                }
            } else {
                // For older versions (pre-1.16), default to reset as hex is not supported
                return ChatColor.RESET.toString();
            }
        }

        // Otherwise, assume it's using Minecraft's & color codes and translate them
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    // Improved version parsing to handle unexpected formats
    private boolean isHexColorSupported() {
        String version = Bukkit.getServer().getBukkitVersion();
        
        // Regex to match version numbers like "1.16", "1.16.5", "21-R0", etc.
        String[] versionParts = version.split("\\.");
        
        try {
            // Parse major version
            int majorVersion = Integer.parseInt(versionParts[0].replaceAll("[^\\d]", ""));
            
            // Check if we have a minor version and parse it if available
            int minorVersion = versionParts.length > 1 ? Integer.parseInt(versionParts[1].replaceAll("[^\\d]", "")) : 0;

            // Hex colors are supported from version 1.16 and onwards
            return majorVersion > 1 || (majorVersion == 1 && minorVersion >= 16);

        } catch (NumberFormatException e) {
            // Log an error message to warn of unexpected version format and default to false
            e.printStackTrace();
        }

        return false;
    }
}
