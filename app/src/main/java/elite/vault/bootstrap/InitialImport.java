package elite.vault.bootstrap;

import elite.vault.ConfigManager;

import java.io.IOException;
import java.nio.file.Path;

public class InitialImport {

    public static void main(String[] args) throws IOException {
        BootstrapImporter importer = BootstrapImporter.getInstance();
        importer.importFromFile(
                Path.of(
                        ConfigManager.getInstance().getSystemKey(ConfigManager.SPANSH_DATA)
                )
        );
    }
}