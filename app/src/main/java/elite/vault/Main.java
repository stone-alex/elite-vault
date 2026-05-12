package elite.vault;

import elite.vault.api.server.ApiServer;
import elite.vault.eddn.EdDnClient;
import elite.vault.eddn.MarketPruneScheduler;
import elite.vault.eddn.SubscriberRegistration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.vault.Singletons.SINGLETONS;

/**
 * Single entry point - starts EDDN ingest and the REST API in the same process.
 * <p>
 * Port resolution order:
 * 1. --port <n>          command-line argument
 * 2. ELITE_VAULT_PORT    environment variable
 * 3. 8085                default
 */
public class Main {

    private static final Logger log = LogManager.getLogger(Main.class);
    private static final int DEFAULT_PORT = 8085;

    public static void main(String[] args) {
        log.info("Elite Vault starting");

        // Accessing SINGLETONS triggers Database static init (connection + schema migration)
        // and warms all manager caches (commodity types, system name cache, etc.)
        SINGLETONS.initialize();

        // EDDN ingest pipeline
        SubscriberRegistration.registerSubscribers();
        EdDnClient.getInstance().start();
        MarketPruneScheduler.getInstance().start();

        // REST API
        int port = resolvePort(args);
        ApiServer apiServer = new ApiServer();
        apiServer.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Elite Vault shutting down");
            apiServer.stop();
            MarketPruneScheduler.getInstance().stop();
        }, "shutdown-hook"));
    }

    private static int resolvePort(String[] args) {
        // --port <n>
        for (int i = 0; i < args.length - 1; i++) {
            if ("--port".equals(args[i])) {
                try {
                    return Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException ignored) {
                    break;
                }
            }
        }
        // ELITE_VAULT_PORT env var
        String env = System.getenv("ELITE_VAULT_PORT");
        if (env != null && !env.isBlank()) {
            try {
                return Integer.parseInt(env.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_PORT;
    }
}
