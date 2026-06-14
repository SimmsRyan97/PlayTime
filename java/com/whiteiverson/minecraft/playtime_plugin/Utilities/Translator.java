package com.whiteiverson.minecraft.playtime_plugin.Utilities;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.whiteiverson.minecraft.playtime_plugin.Main;

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

    /**
     * Static utility method to load YAML files
     * Can be used by other classes
     */
    public static FileConfiguration loadYamlWithBomHandlingStatic(File file) {
        // YamlConfiguration.loadConfiguration handles BOM automatically
        return YamlConfiguration.loadConfiguration(file);
    }

    // Updated loadLanguageFiles() as described above
    private void loadLanguageFiles() {
        File langFolder = new File(main.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            if (!langFolder.mkdirs()) {
                main.getLogger().warning("Failed to create lang folder: " + langFolder.getAbsolutePath());
            }
        }

        // First, copy language files from JAR (existing code)
        try {
            java.net.URL resource = main.getClass().getClassLoader().getResource("lang");
            if (resource != null) {
                if (resource.getProtocol().equals("jar")) {
                    java.net.JarURLConnection jarConnection = (java.net.JarURLConnection) resource.openConnection();
                    try (java.util.jar.JarFile jarFile = jarConnection.getJarFile()) {
                        jarFile.stream().filter(entry -> entry.getName().startsWith("lang/") && entry.getName().endsWith(".yml"))
                                .forEach(entry -> {
                                    String fileName = entry.getName().substring(entry.getName().lastIndexOf("/") + 1);
                                    File langFile = new File(langFolder, fileName);

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
                                });
                    }
                }
            }
        } catch (Exception e) {
            if (main.getConfig().getBoolean("logging.debug", false)) {
                main.getLogger().log(Level.SEVERE, "Failed to load language files dynamically", e);
            }
        }

        // NOW load all language files from the lang folder (both from JAR and manually added)
        File[] langFiles = langFolder.listFiles((dir, name) -> name.startsWith("messages_") && name.endsWith(".yml"));
        if (langFiles != null) {
            for (File langFile : langFiles) {
                String langKey = langFile.getName().replace(".yml", "").toLowerCase();
                FileConfiguration config = loadYamlWithBomHandling(langFile);
                loadedLanguages.put(langKey, config);
                main.getLogger().info("Loaded language file: " + langKey);
            }
        }
    }

    /**
     * Load YAML configuration
     */
    private FileConfiguration loadYamlWithBomHandling(File file) {
        // YamlConfiguration.loadConfiguration handles BOM automatically
        return YamlConfiguration.loadConfiguration(file);
    }

    public void loadDefaultLanguage() {
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
        if (localeStr.contains("_")) {
            return localeStr.split("_")[0].toLowerCase();  // Extract the language code (e.g., "en", "fr")
        } else {
            return localeStr.toLowerCase();  // If there's no underscore, return the entire locale string in lowercase
        }
    }

    public FileConfiguration getLanguageConfig(String lang) {
        lang = lang.toLowerCase();

        // If language is loaded, return it
        if (loadedLanguages.containsKey("messages_" + lang)) {
            return loadedLanguages.get("messages_" + lang);
        } else {
            main.getLogger().warning("Language file for '" + lang + "' not found. Falling back to default language.");
            return loadYamlWithBomHandling(defaultLangFile);
        }
    }

    // Fetching translated string by key
    public String getTranslation(String key, Object sender) {
    	
        // Determine if the sender is a Player
        if (sender instanceof Player) {
            Player player = (Player) sender;
            
            String lang = getPlayerLocale(player);  // Get player's language
            FileConfiguration langConfig = getLanguageConfig(lang);  // Load the corresponding language file

            // Return the translated string or fallback to English/default
            return langConfig.getString(key, "Translation not found for key: " + key);
        } else {
        	return getConsoleTranslation(key); // Fallback if sender is not a player
        }
    }

    // Fetching translated string for the console
    public String getConsoleTranslation(String key) {
        FileConfiguration langConfig = loadYamlWithBomHandling(defaultLangFile);
        return langConfig.getString(key, "Translation not found for key: " + key);
    }
}