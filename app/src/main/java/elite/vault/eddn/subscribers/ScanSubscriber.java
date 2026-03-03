package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.eddn.dto.ScanDto;
import elite.vault.eddn.events.EddnMessageEvent;
import elite.vault.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.vault.Singletons.SINGLETONS;

public class ScanSubscriber {

    private static final Logger log = LogManager.getLogger(ScanSubscriber.class);

    @Subscribe
    public void onEvent(EddnMessageEvent event) {

        if (!event.isJournal() || !"Scan".equals(event.getEventType())) {
            return;
        }

        try {
            String json = SINGLETONS.getObjectMapper().writeValueAsString(event.messageNode());
            ScanDto dto = GsonFactory.getGson().fromJson(json, ScanDto.class);
            if (dto.getDistanceFromArrivalLs() == 0) {
                SINGLETONS.getStarSystemManager().save(dto);
                System.out.println("EDDM Star System " + dto.getStarSystem());
            } else {
                SINGLETONS.getStellarObjectManager().save(dto);
                System.out.println("EDDM Stellar Object " + dto.getStarSystem());
            }
        } catch (Exception e) {
            log.error("Unable to process EDEN event " + event.getEventType(), e);
        }
    }
}
