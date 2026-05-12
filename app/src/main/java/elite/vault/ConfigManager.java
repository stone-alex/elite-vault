package elite.vault;

/**
 * Application configuration.
 * <p>
 * Currently a placeholder - configuration will be stored in the SQLite database
 * so that end users never need to edit config files.
 * <p>
 * The old file-based implementation (elite-vault.conf) has been removed along
 * with the MySQL connection keys it served.
 */
public class ConfigManager {

    private static final ConfigManager INSTANCE = new ConfigManager();

    private ConfigManager() {}

    public static ConfigManager getInstance() {
        return INSTANCE;
    }
}
