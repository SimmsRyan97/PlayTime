package com.whiteiverson.minecraft.playtime_plugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class Translator {

    private final Main main;
    private final Map<String, FileConfiguration> loadedLanguages = new HashMap<>();
    private File defaultLangFile;

    public Translator() {
        this.main = Main.getInstance();
        loadLanguageFiles();  // Load the language files on initialisation
        loadDefaultLanguage();
    }

    // Updated loadLanguageFiles() as described above
    private void loadLanguageFiles() {
        File langFolder = new File(main.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs(); // Create the directory if it doesn't exist
        }

        try {
            // Get the resource URL for the lang folder
            java.net.URL resource = main.getClass().getClassLoader().getResource("lang");
            if (resource != null) {
                // Use JarURLConnection to access the contents of the jar file
                if (resource.getProtocol().equals("jar")) {
                    java.net.JarURLConnection jarConnection = (java.net.JarURLConnection) resource.openConnection();
                    try (java.util.jar.JarFile jarFile = jarConnection.getJarFile()) {
                        // Iterate through the entries of the JAR file
                        jarFile.stream().filter(entry -> entry.getName().startsWith("lang/") && entry.getName().endsWith(".yml"))
                                .forEach(entry -> {
                                    String fileName = entry.getName().substring(entry.getName().lastIndexOf("/") + 1); // Get the file name
                                    File langFile = new File(langFolder, fileName); // Destination file in plugin's data folder

                                    // Copy the file if it doesn't exist in the plugin's folder
                                    if (!langFile.exists()) {
                                        try (InputStream inputStream = jarFile.getInputStream(entry)) {
                                            if (inputStream != null) {
                                                Files.copy(inputStream, langFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                            }
                                        } catch (IOException e) {
                                        	if (main.getConfig().getBoolean("logging.debug", false)) {
                                        		main.getLogger().log(Level.SEVERE, "Failed to copy language file: " + fileName, e);
                                        	}
                                        }
                                    }

                                    // Load and cache the configuration file
                                    String langKey = fileName.replace(".yml", "").toLowerCase();  // e.g., "messages_en"
                                    loadedLanguages.put(langKey, YamlConfiguration.loadConfiguration(langFile));
                                });
                    }
                }
            }
        } catch (Exception e) {
        	if (main.getConfig().getBoolean("logging.debug", false)) {
        		main.getLogger().log(Level.SEVERE, "Failed to load language files dynamically", e);
        	}
        }
    }

    private void loadDefaultLanguage() {
        // Load the default language file from config
        String defaultLang = main.getConfig().getString("lang", "en");  // Default to English if not set
        File langFolder = new File(main.getDataFolder(), "lang");
        defaultLangFile = new File(langFolder, "messages_" + defaultLang + ".yml");

        if (!defaultLangFile.exists()) {
            main.getLogger().log(Level.WARNING, "Default language file not found, falling back to 'en'.");
            defaultLangFile = new File(langFolder, "messages_en.yml");
        }
    }

    public String getPlayerLocale(Player player) {
        String localeStr = player.getLocale(); // This returns a String, not a Locale object
        if (localeStr != null && localeStr.contains("_")) {
            return localeStr.split("_")[0].toLowerCase();  // Extract the language code (e.g., "en", "fr")
        } else if (localeStr != null) {
            return localeStr.toLowerCase();  // If there's no underscore, return the entire locale string in lowercase
        } else {
            return "en";  // Fallback to English if the locale is null
        }
    }

    public FileConfiguration getLanguageConfig(String lang) {
        lang = lang.toLowerCase();

        // If language is loaded, return it
        if (loadedLanguages.containsKey("messages_" + lang)) {
            return loadedLanguages.get("messages_" + lang);
        } else {
            main.getLogger().warning("Language file for '" + lang + "' not found. Falling back to default language.");
            return YamlConfiguration.loadConfiguration(defaultLangFile);
        }
    }

    // Fetching translated string by key
    public String getTranslation(String key, Player player) {
        String lang = getPlayerLocale(player);  // Get player's language
        FileConfiguration langConfig = getLanguageConfig(lang);  // Load the corresponding language file

        // Return the translated string or fallback to English/default
        return langConfig.getString(key, "Translation not found for key: " + key);
    }

    // Fetching translated string for the console
    public String getConsoleTranslation(String key) {
        FileConfiguration langConfig = YamlConfiguration.loadConfiguration(defaultLangFile);
        return langConfig.getString(key, "Translation not found for key: " + key);
    }
}