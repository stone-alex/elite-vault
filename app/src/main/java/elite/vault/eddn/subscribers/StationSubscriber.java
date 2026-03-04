package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.db.managers.StationManager;
import elite.vault.eddn.events.EddnMessageEvent;

public class StationSubscriber {

    private final StationManager manager = StationManager.getInstance();

    @Subscribe
    public void onEvent(EddnMessageEvent event) {
        if ("Docked".equalsIgnoreCase(event.getEventType())) {
            manager.saveStations(event.getData());
        }
    }
}