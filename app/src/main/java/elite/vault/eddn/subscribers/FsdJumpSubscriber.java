package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.eddn.dto.EddnDto;
import elite.vault.eddn.events.EddnMessageEvent;

import static elite.vault.Singletons.SINGLETONS;

public class FsdJumpSubscriber {


    @Subscribe
    public void onEvent(EddnMessageEvent event) {

        if ("FSDJump".equalsIgnoreCase(event.getEventType())) {
            EddnDto data = event.getData();
            SINGLETONS.getStarSystemManager().saveFsdJump(data);
        }
    }
}