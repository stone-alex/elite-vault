package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.eddn.dto.EddnDto;
import elite.vault.eddn.events.EddnMessageEvent;
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
        if ("AutoScan".equalsIgnoreCase(event.getData().getScanType())) {
            return;
        }

        EddnDto data = event.getData();
        if (data.getDistanceFromArrivalLs() == 0) {
            SINGLETONS.getStarSystemManager().save(data);
            log.info("EDDM Star System " + data.getStarSystem());
        } else {
            SINGLETONS.getStellarObjectManager().save(data);
            log.info("EDDM Stellar Object " + data.getStarSystem());
        }
    }
}
