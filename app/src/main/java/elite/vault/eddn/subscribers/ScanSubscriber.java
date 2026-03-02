package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.eddn.dto.ScanDto;
import elite.vault.eddn.events.EddnMessageEvent;
import elite.vault.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.vault.Singletons.INSTANCE;

public class ScanSubscriber {

    private static final Logger log = LogManager.getLogger(ScanSubscriber.class);

    @Subscribe
    public void onEvent(EddnMessageEvent event) {

        if (!event.isJournal() || !"Scan".equals(event.getEventType())) {
            return;
        }

        try {
            String json = INSTANCE.getObjectMapper().writeValueAsString(event.messageNode());
            ScanDto dto = GsonFactory.getGson().fromJson(json, ScanDto.class);
            if (dto.getDistanceFromArrivalLs() == 0) {
                INSTANCE.getStarSystemManager().save(dto);
            } else {
                INSTANCE.getStellarObjectManager().save(dto);
            }
        } catch (Exception e) {
            log.error("Unable to process EDEN event " + event.getEventType(), e);
        }
    }
}
