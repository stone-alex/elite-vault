package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.eddn.dto.EDDN_JournalDto;
import elite.vault.eddn.events.EddnMessageEvent;
import elite.vault.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

import static elite.vault.Singletons.SINGLETONS;

public class StellarObjectSubscriber {

    private static final Logger log = LogManager.getLogger(StellarObjectSubscriber.class);
    private static final Set<String> EVENTS = Set.of("Scan", "ScanBaryCentre", "FSDJump", "Docked", "SAASignalsFound");

    @Subscribe
    public void onEvent(EddnMessageEvent event) {
        if (!event.isJournal()) return;
        EDDN_JournalDto data = GsonFactory.getGson().fromJson(event.getRawJson(), EDDN_JournalDto.class);
        if (!EVENTS.contains(data.getEvent())) return;
        EventProcessingExecutor.submit(() -> update(data));
    }

    private static void update(EDDN_JournalDto data) {
        if (data.getStarPos() != null && data.getStarSystem() != null) {
            SINGLETONS.getStarSystemManager().save(data);
            log.info("EDDN Star System {}", data.getStarSystem());
        }

        if (data.getDistanceFromArrivalLs() != null && data.getDistanceFromArrivalLs() > 0) {
            if ("Detailed".equalsIgnoreCase(data.getScanType())) {
                SINGLETONS.getStellarObjectManager().save(data);
                log.info("EDDN Stellar Object {}", data.getStarSystem());
            } else {
                SINGLETONS.getStellarObjectManager().savePartial(data);
            }
        }
    }
}