package elite.vault.eddn;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public final class MarketPruneScheduler {

    private static final Logger log = LogManager.getLogger(MarketPruneScheduler.class);
    private static final MarketPruneScheduler INSTANCE = new MarketPruneScheduler();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "market-prune-scheduler");
            t.setDaemon(true);
            return t;
        }
    });

    private MarketPruneScheduler() {
    }

    public static MarketPruneScheduler getInstance() {
        return INSTANCE;
    }

    public void start() {
/*        scheduler.scheduleWithFixedDelay(() -> {
            try {
                SINGLETONS.getMarketManager().prune();
            } catch (Exception e) {
                log.error("Market prune failed: {}", e.getMessage(), e);
            }
        }, 30, 60, TimeUnit.SECONDS);*/
    }

    public void stop() {
        scheduler.shutdown();
    }
}