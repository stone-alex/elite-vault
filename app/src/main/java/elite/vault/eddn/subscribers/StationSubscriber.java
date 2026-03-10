package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.db.managers.StationManager;
import elite.vault.eddn.dto.EDDN_JournalDto;
import elite.vault.eddn.events.EddnMessageEvent;
import elite.vault.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

public class StationSubscriber {

    private static final Logger log = LogManager.getLogger(StationSubscriber.class);
    private static final Set<String> EVENTS = Set.of("Docked", "Location");
    private final StationManager manager = StationManager.getInstance();

    @Subscribe
    public void onEvent(EddnMessageEvent event) {
        if (!event.isJournal()) return;
        EDDN_JournalDto data = GsonFactory.getGson().fromJson(event.getRawJson(), EDDN_JournalDto.class);
        if (!EVENTS.contains(data.getEvent())) return;
        if (data.getStationName() == null || data.getMarketId() == null) {
            log.debug("Station event dropped - missing required fields (event={})", data.getEvent());
            return;
        }
        EventProcessingExecutor.submit(() -> update(data));
    }

    private void update(EDDN_JournalDto data) {
        log.info("EDDN Station {}", data.getStationName());
        manager.saveStations(data);
    }
}