package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.db.dao.SystemDao;
import elite.vault.db.managers.StarSystemManager;
import elite.vault.eddn.dto.EddnDto;
import elite.vault.eddn.events.EddnMessageEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.vault.Singletons.SINGLETONS;

public class CommoditySubscriber {

    private static final Logger log = LogManager.getLogger(CommoditySubscriber.class);
    private final StarSystemManager starSystemManager = StarSystemManager.getInstance();

    @Subscribe
    public void onEvent(EddnMessageEvent event) {
        if (!event.schemaRef().contains("commodity/3")) {
            return;
        }

        // No need to check eventType() — it's always null here
        try {

            EddnDto data = event.getData();
            SystemDao.StarSystem star = starSystemManager.findByName(data.getStarSystem());
            if (star == null) return;
            log.info("Market update " + data.getMarketId() + " " + data.getStarSystem() + " " + data.getStationName());
            SINGLETONS.getMarketManager().save(data, star.getSystemAddress(), star.getX(), star.getY(), star.getZ());
        } catch (Exception e) {
            log.warn("Failed to process commodity/3 message", e);
        }
    }
}
