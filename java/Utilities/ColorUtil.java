package com.whiteiverson.minecraft.playtime_plugin.Utilities;

import net.md_5.bungee.api.ChatColor;

public class ColorUtil {

    public String translateColor(String input) {
        if (input == null) {
            return ""; // Return empty string if null
        }
        
        // Check if the input starts with a '#' and is a hex code
        if (input.startsWith("#") && input.length() == 7) {
            try {
                // Try to parse as a hex color
                return ChatColor.of(input).toString();
            } catch (IllegalArgumentException e) {
                // Invalid hex code, handle if necessary (e.g. log it, use default color, etc.)
                return ChatColor.RESET.toString(); // Default to reset if color is invalid
            }
        }

        // Otherwise, assume it's using Minecraft's & color codes, and translate them
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}