package com.whiteiverson.minecraft.playtime_plugin.Utilities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

public class LibraryLoader {
    private final File libFolder;
    private final Logger logger;

    public LibraryLoader(File dataFolder, Logger logger) {
        this.libFolder = new File(dataFolder, "lib");
        this.logger = logger;
        if (!libFolder.exists()) {
            libFolder.mkdirs();
        }
    }

    public void loadDependencies() {
        logger.info("Loading database dependencies...");

        // MySQL Connector
        boolean mysqlLoaded = loadLibrary(
                "mysql-connector-j",
                "8.0.33",
                "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar"
        );

        // SQLite JDBC
        boolean sqliteLoaded = loadLibrary(
                "sqlite-jdbc",
                "3.42.0.0",
                "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.42.0.0/sqlite-jdbc-3.42.0.0.jar"
        );

        if (mysqlLoaded && sqliteLoaded) {
            logger.info("All database dependencies loaded successfully!");
        } else {
            logger.warning("Some database dependencies failed to load. Check logs above.");
        }
    }

    private boolean loadLibrary(String name, String version, String downloadUrl) {
        File libFile = new File(libFolder, name + "-" + version + ".jar");

        // Download if not exists
        if (!libFile.exists()) {
            logger.info("Downloading " + name + " " + version + "...");
            try {
                downloadFile(downloadUrl, libFile);
                logger.info("Successfully downloaded " + name);
            } catch (IOException e) {
                logger.severe("Failed to download " + name + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            logger.info(name + " " + version + " already exists, skipping download.");
        }

        // Verify file exists and has content
        if (!libFile.exists() || libFile.length() == 0) {
            logger.severe("Library file is missing or empty: " + libFile.getAbsolutePath());
            return false;
        }

        // Load into classpath
        try {
            addToClasspath(libFile);
            logger.info("Loaded " + name + " " + version + " into classpath");
            return true;
        } catch (Exception e) {
            logger.severe("Failed to load " + name + " into classpath: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void downloadFile(String urlString, File destination) throws IOException {
        logger.info("Downloading from: " + urlString);
        logger.info("Saving to: " + destination.getAbsolutePath());

        URL url = new URL(urlString);
        try (InputStream in = url.openStream()) {
            Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        logger.info("Download complete. File size: " + destination.length() + " bytes");
    }

    private void addToClasspath(File file) throws Exception {
        // Get the system class loader
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();

        // For newer Java versions, we need to use a different approach
        if (classLoader instanceof URLClassLoader) {
            // Java 8 style
            URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(urlClassLoader, file.toURI().toURL());
        } else {
            // Java 9+ style - use the plugin classloader instead
            ClassLoader pluginClassLoader = this.getClass().getClassLoader();

            if (pluginClassLoader instanceof URLClassLoader) {
                URLClassLoader urlClassLoader = (URLClassLoader) pluginClassLoader;
                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(urlClassLoader, file.toURI().toURL());
            } else {
                throw new IllegalStateException("ClassLoader is not a URLClassLoader, cannot dynamically load libraries");
            }
        }
    }
}