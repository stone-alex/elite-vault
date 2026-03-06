package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.eddn.dto.EddnDto;
import elite.vault.eddn.events.EddnMessageEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

import static elite.vault.Singletons.SINGLETONS;

public class StellarObjectSubscriber {

    private static final Logger log = LogManager.getLogger(StellarObjectSubscriber.class);
    private static final List<String> events = Arrays.asList("Scan", "ScanBaryCentre", "FSDJump", "Docked", "SAASignalsFound");

    @Subscribe
    public void onEvent(EddnMessageEvent event) {
        if (!event.isJournal()) return;
        if (!events.contains(event.getEventType())) return;

        EddnDto data = event.getData();
        if (data == null) return;

        /// cherry-pick data
        ///  save star
        if (data.getStarPos() != null && data.getStarSystem() != null) {
            SINGLETONS.getStarSystemManager().save(data);
            log.info("EDDM Star System " + data.getStarSystem());
        }

        /// save stellar object
        if (data.getDistanceFromArrivalLs() != null && data.getDistanceFromArrivalLs() > 0) {
            if (data.getScanType().equalsIgnoreCase("Detailed")) {
                /// save detailed
                SINGLETONS.getStellarObjectManager().save(data);
                log.info("EDDM Stellar Object " + data.getStarSystem());
            } else {
                SINGLETONS.getStellarObjectManager().savePartial(data);
            }
        } else if ("Location".equalsIgnoreCase(event.getEventType())) {

        }
    }
}
