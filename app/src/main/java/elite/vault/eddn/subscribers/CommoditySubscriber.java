package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.db.dao.SystemDao;
import elite.vault.db.managers.StarSystemManager;
import elite.vault.eddn.dto.EDDN_CommodityMessageDto;
import elite.vault.eddn.events.EddnMessageEvent;
import elite.vault.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.vault.Singletons.SINGLETONS;

public class CommoditySubscriber {

    private static final Logger log = LogManager.getLogger(CommoditySubscriber.class);
    private final StarSystemManager starSystemManager = StarSystemManager.getInstance();

    @Subscribe
    public void onEvent(EddnMessageEvent event) {
        if (!event.matchesSchema("commodity/3")) return;
        EDDN_CommodityMessageDto data = GsonFactory.getGson().fromJson(event.getRawJson(), EDDN_CommodityMessageDto.class);
        if (data.getSystemName() == null || data.getMarketId() == null) {
            log.debug("Commodity update dropped - missing required fields");
            return;
        }
        EventProcessingExecutor.submit(() -> update(data));
    }

    private void update(EDDN_CommodityMessageDto data) {
        SystemDao.StarSystem star = starSystemManager.findByName(data.getSystemName());
        if (star == null) {
            log.debug("Commodity update dropped - unknown system: {}", data.getSystemName());
            return;
        }
        log.info("Market update {} {} {}", data.getMarketId(), star.getStarName(), data.getStationName());
        SINGLETONS.getMarketManager().save(data, star.getSystemAddress());
    }
}