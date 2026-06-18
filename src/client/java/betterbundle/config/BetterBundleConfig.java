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
    
    // Toggle button position options
    public enum ToggleButtonPosition {
        LEFT,   // Left side, top-aligned
        TOP,    // Top side, right-aligned
        RIGHT,  // Right side (default)
        OFF     // No toggle button
    }
    
    // Config options
    private boolean showStackOverlay = true;
    private boolean stackSameNbt = true; // Merge items with same NBT in GUI
    private ToggleButtonPosition toggleButtonPosition = ToggleButtonPosition.RIGHT;
    private boolean panelVisible = true; // Bundle panel visibility
    
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
            toggleButtonPosition = ToggleButtonPosition.valueOf(
                    props.getProperty("toggleButtonPosition", "RIGHT").toUpperCase());
            panelVisible = Boolean.parseBoolean(props.getProperty("panelVisible", "true"));
        } catch (IOException e) {
            // Ignore errors, use defaults
        } catch (IllegalArgumentException e) {
            // Invalid enum value, use default
            toggleButtonPosition = ToggleButtonPosition.RIGHT;
        }
    }
    
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            
            Properties props = new Properties();
            props.setProperty("showStackOverlay", String.valueOf(showStackOverlay));
            props.setProperty("stackSameNbt", String.valueOf(stackSameNbt));
            props.setProperty("toggleButtonPosition", toggleButtonPosition.name());
            props.setProperty("panelVisible", String.valueOf(panelVisible));
            
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
    
    public ToggleButtonPosition toggleButtonPosition() {
        return toggleButtonPosition;
    }
    
    public void setToggleButtonPosition(ToggleButtonPosition value) {
        toggleButtonPosition = value;
        save();
    }
    
    public boolean panelVisible() {
        return panelVisible;
    }
    
    public void setPanelVisible(boolean value) {
        panelVisible = value;
        save();
        // Also update BundlePanelRenderer.visible
        BundlePanelRendererUpdater.updateVisible(value);
    }
    
    /**
     * Helper class to update BundlePanelRenderer.visible without direct dependency.
     */
    private static class BundlePanelRendererUpdater {
        static void updateVisible(boolean value) {
            betterbundle.gui.BundlePanelRenderer.visible = value;
        }
    }
}