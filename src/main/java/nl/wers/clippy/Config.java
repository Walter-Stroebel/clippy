package nl.wers.clippy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Config manages the application's configuration settings, stored in a properties file.
 * It initializes from an existing configuration or sets default values if none exists.
 * The class provides an organized approach to manage user preferences and application settings,
 * ensuring a consistent user experience across sessions.
 * 
 * @author Walter Stroebel
 */
public class Config {

    // Default constants
    public static final String DEFAULT_GUI_X = "100";
    public static final String DEFAULT_GUI_Y = "100";
    public static final String DEFAULT_GUI_WIDTH = "800";
    public static final String DEFAULT_GUI_HEIGHT = "600";
    public static final String CLIPPY_PROPERTIES = "clippy.properties";
    // Add other defaults as needed

    private final Properties properties;
    private final File configFile;
    private final Clippy clippyInstance;

    public Config(Clippy clippy) {
        this.clippyInstance = clippy;
        this.properties = new Properties();
        this.configFile = new File(Clippy.HOME_DIRECTORY, CLIPPY_PROPERTIES);

        // Load or initialize properties
        if (configFile.exists()) {
            loadProperties();
        } else {
            setDefaultProperties();
        }
    }

    private void loadProperties() {
        try (FileInputStream in = new FileInputStream(configFile)) {
            properties.load(in);
        } catch (IOException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setDefaultProperties() {
        properties.setProperty(SECTIONS.GUI.name() + ".x", DEFAULT_GUI_X);
        properties.setProperty(SECTIONS.GUI.name() + ".y", DEFAULT_GUI_Y);
        properties.setProperty(SECTIONS.GUI.name() + ".width", DEFAULT_GUI_WIDTH);
        properties.setProperty(SECTIONS.GUI.name() + ".height", DEFAULT_GUI_HEIGHT);
        // Set other default properties as needed

        saveProperties();
    }

    private void saveProperties() {
        try (FileOutputStream out = new FileOutputStream(configFile)) {
            properties.store(out, "Clippy Configuration");
        } catch (IOException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // Getter and setter methods for properties can be added here
    // Enum for property sections
    public enum SECTIONS {
        GUI, PREFS
    }
}
