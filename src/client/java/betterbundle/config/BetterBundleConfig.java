package betterbundle.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Simple configuration manager for BetterBundleHUD.
 * Stores config in a properties file.
 */
public class BetterBundleConfig {
    private static final Path CONFIG_PATH = Path.of("config/better-bundle.properties");
    
    // Config options
    private boolean showStackOverlay = true;
    private boolean stackSameNbt = true; // Merge items with same NBT in GUI
    
    private static BetterBundleConfig instance;
    
    public static BetterBundleConfig getInstance() {
        if (instance == null) {
            instance = new BetterBundleConfig();
            instance.load();
        }
        return instance;
    }
    
    public void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        
        try (InputStream is = Files.newInputStream(CONFIG_PATH)) {
            Properties props = new Properties();
            props.load(is);
            
            showStackOverlay = Boolean.parseBoolean(props.getProperty("showStackOverlay", "true"));
            stackSameNbt = Boolean.parseBoolean(props.getProperty("stackSameNbt", "true"));
        } catch (IOException e) {
            // Ignore errors, use defaults
        }
    }
    
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            
            Properties props = new Properties();
            props.setProperty("showStackOverlay", String.valueOf(showStackOverlay));
            props.setProperty("stackSameNbt", String.valueOf(stackSameNbt));
            
            try (OutputStream os = Files.newOutputStream(CONFIG_PATH)) {
                props.store(os, "BetterBundleHUD Configuration");
            }
        } catch (IOException e) {
            // Ignore errors
        }
    }
    
    public boolean showStackOverlay() {
        return showStackOverlay;
    }
    
    public void setShowStackOverlay(boolean value) {
        showStackOverlay = value;
        save();
    }
    
    public boolean stackSameNbt() {
        return stackSameNbt;
    }
    
    public void setStackSameNbt(boolean value) {
        stackSameNbt = value;
        save();
    }
}