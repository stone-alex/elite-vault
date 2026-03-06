package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.db.managers.StationManager;
import elite.vault.eddn.events.EddnMessageEvent;

import java.util.Arrays;
import java.util.List;

public class StationSubscriber {

    private static final List<String> events = Arrays.asList("Docked", "Location");
    private final StationManager manager = StationManager.getInstance();

    @Subscribe
    public void onEvent(EddnMessageEvent event) {
        if (!events.contains(event.getEventType())) return;
        manager.saveStations(event.getData());
    }
}