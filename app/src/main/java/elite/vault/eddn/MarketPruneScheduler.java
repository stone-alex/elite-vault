package elite.vault.eddn;

import elite.vault.db.dao.CommodityDao;
import elite.vault.db.util.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MarketPruneScheduler {

    private static final Logger log = LogManager.getLogger(MarketPruneScheduler.class);
    private static final MarketPruneScheduler INSTANCE = new MarketPruneScheduler();

    // 3-hour TTL enforced by DELETE; runs every 15 minutes.
    private static final long INITIAL_DELAY_SEC = 60;
    private static final long PERIOD_SEC = 900;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "market-prune-scheduler");
        t.setDaemon(true);
        return t;
    });

    private MarketPruneScheduler() {
    }

    public static MarketPruneScheduler getInstance() {
        return INSTANCE;
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                int deleted = Database.withDao(CommodityDao.class, CommodityDao::pruneExpired);
                if (deleted > 0) {
                    log.debug("Market TTL prune: removed {} expired commodity rows", deleted);
                }
            } catch (Exception e) {
                log.error("Market TTL prune failed: {}", e.getMessage(), e);
            }
        }, INITIAL_DELAY_SEC, PERIOD_SEC, TimeUnit.SECONDS);

        log.info("Market TTL scheduler started - prune interval {}s, TTL 3h", PERIOD_SEC);
    }

    public void stop() {
        scheduler.shutdown();
    }
}