package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.eddn.dto.CommodityMessageDto;
import elite.vault.eddn.events.EddnMessageEvent;
import elite.vault.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.vault.Singletons.INSTANCE;

public class CommoditySubscriber {

    private static final Logger log = LogManager.getLogger(CommoditySubscriber.class);

    @Subscribe
    public void onEvent(EddnMessageEvent event) {
        if (!event.schemaRef().contains("commodity/3")) {
            return;
        }

        // No need to check eventType() — it's always null here
        try {
            String json = INSTANCE.getObjectMapper().writeValueAsString(event.messageNode());
            CommodityMessageDto dto = GsonFactory.getGson().fromJson(json, CommodityMessageDto.class);
            INSTANCE.getMarketManager().save(dto);
        } catch (Exception e) {
            log.warn("Failed to process commodity/3 message", e);
        }
    }
}
