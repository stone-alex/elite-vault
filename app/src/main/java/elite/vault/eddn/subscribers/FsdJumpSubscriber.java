package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.eddn.dto.EddnDto;
import elite.vault.eddn.events.EddnMessageEvent;

public class FsdJumpSubscriber {


    @Subscribe
    public void onEvent(EddnMessageEvent event) {

        if (!"FSDJump".equalsIgnoreCase(event.getData().getScanType())) {
            return;
        }
        EddnDto data = event.getData();
        // save star system allegence, economy factions etc.
        //SINGLETONS.getStarSystemManager().save(data);
    }
}