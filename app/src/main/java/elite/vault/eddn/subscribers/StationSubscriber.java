package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.db.managers.StationManager;
import elite.vault.eddn.events.EddnMessageEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class StationSubscriber {

    private static final Logger log = LogManager.getLogger(StationSubscriber.class);
    private static final List<String> events = Arrays.asList("Docked", "Location");
    private final StationManager manager = StationManager.getInstance();

    @Subscribe
    public void onEvent(EddnMessageEvent event) {
        if (!events.contains(event.getEventType())) return;
        EventProcessingExecutor.submit(() -> update(event));
    }

    private void update(EddnMessageEvent event) {
        log.info("EDDN Station " + event.getEventType());
        manager.saveStations(event.getData());
    }
}