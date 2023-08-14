package nl.wers.clippy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Config manages the application's configuration settings, stored in a
 * properties file. It initializes from an existing configuration or sets
 * default values if none exists. The class provides an organized approach to
 * manage user preferences and application settings, ensuring a consistent user
 * experience across sessions.
 *
 * @author Walter Stroebel
 */
public class Config {

    // Singleton instance
    private static Config instance;

    // Default constants
    public static final String CLIPPY_PROPERTIES = "clippy.properties";

    // Default properties map
    private static final Map<String, String> DEFAULT_PROPERTIES = new HashMap<>();

    static {
        DEFAULT_PROPERTIES.put(SECTIONS.GUI.name() + ".x", "100");
        DEFAULT_PROPERTIES.put(SECTIONS.GUI.name() + ".y", "100");
        DEFAULT_PROPERTIES.put(SECTIONS.GUI.name() + ".width", "800");
        DEFAULT_PROPERTIES.put(SECTIONS.GUI.name() + ".height", "600");
        // Add other defaults as needed
    }

    // Public method to get the instance
    public static synchronized Config getInstance(Clippy clippy) {
        if (instance == null) {
            instance = new Config(clippy);
        }
        return instance;
    }

    private final Properties properties;
    private final File configFile;
    private final Clippy clippyInstance;

    // Private constructor
    private Config(Clippy clippy) {
        this.clippyInstance = clippy;
        this.properties = new Properties();
        this.configFile = new File(clippy.workDir.get().getParentFile(), CLIPPY_PROPERTIES);

        if (configFile.exists()) {
            loadProperties();
        } else {
            setDefaultProperties();
        }
    }

    private void loadProperties() {
        try ( FileInputStream in = new FileInputStream(configFile)) {
            properties.load(in);
        } catch (IOException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // Set default properties using the map
    private void setDefaultProperties() {
        for (Map.Entry<String, String> entry : DEFAULT_PROPERTIES.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue());
        }
        saveProperties();
    }

    private void saveProperties() {
        try ( FileOutputStream out = new FileOutputStream(configFile)) {
            properties.store(out, "Clippy Configuration");
        } catch (IOException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getProperty(SECTIONS section, String propName) {
        return properties.getProperty(section.name() + "." + propName);
    }

    public void setProperty(SECTIONS section, String propName, String value) {
        properties.setProperty(section.name() + "." + propName, value);
        saveProperties();
    }

    public boolean setCodeBase(File dir) {
        return this.setCodeBase(dir.getAbsolutePath());
    }

    public boolean setCodeBase(String base) {
        if (!new File(base).exists()) {
            return false;
        }
        if (!new File(base, ".git").exists()) {
            return false;
        }
        setProperty(SECTIONS.CODEBASE, CODEBASE, base);
        return true;
    }
    public static final String CODEBASE = "codebase";

    // Enum for property sections
    public enum SECTIONS {
        GUI, PREFS, CODEBASE
    }
}
