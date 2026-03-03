package elite.vault;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigManager {

    public static final String SYSTEM_CONFIG_FILENAME = "elite-vault.conf";
    // Config keys
    public static final String SPANSH_DATA = "spansh-data";
    public static final String DB_SERVER = "db-server";
    public static final String DB_PORT = "db-port";
    public static final String DB_USER = "db-user";
    public static final String DB_PASS = "db-pass";
    public static final String DB_NAME = "db-name";
    public static final String DB_SQLITE_PATH = "sqlite-path";


    private static final Logger log = LogManager.getLogger(ConfigManager.class);
    private static final ConfigManager INSTANCE = new ConfigManager();
    private final Map<String, String> DEFAULT_SYSTEM_CONFIG = new LinkedHashMap<>();
    private final File configFile;

    private ConfigManager() {
        // Initialize default configs
        DEFAULT_SYSTEM_CONFIG.put(SPANSH_DATA, "");
        String configHome = System.getenv("XDG_CONFIG_HOME");
        if (configHome == null || configHome.isEmpty()) {
            configHome = System.getProperty("user.home") + "/.config";
        }
        configFile = Path.of(configHome, SYSTEM_CONFIG_FILENAME).toFile();
    }

    public static ConfigManager getInstance() {
        return INSTANCE;
    }


    public String getSystemKey(String key) {
        if (!configFile.exists()) {
            log.warn("Config file does not exist: {}", configFile.getAbsolutePath());
            return DEFAULT_SYSTEM_CONFIG.get(key);
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(configFile));
            log.info("Config file opened successfully, size: {} bytes", configFile.length());
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int separatorIndex = line.indexOf('=');
                if (separatorIndex > 0) {
                    String configKey = line.substring(0, separatorIndex).replaceAll("\"", "").trim();
                    String configValue = line.substring(separatorIndex + 1).replaceAll("\"", "").trim();
                    if (configKey.equals(key)) {
                        return configValue;
                    }
                }
                log.info("Finished reading, total lines: {}", lineCount);
            }
            return DEFAULT_SYSTEM_CONFIG.get(key);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}