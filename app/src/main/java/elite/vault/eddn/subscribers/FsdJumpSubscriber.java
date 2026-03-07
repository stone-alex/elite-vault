package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.eddn.dto.EddnDto;
import elite.vault.eddn.events.EddnMessageEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.vault.Singletons.SINGLETONS;

public class FsdJumpSubscriber {

    private static final Logger log = LogManager.getLogger(FsdJumpSubscriber.class);

    @Subscribe
    public void onEvent(EddnMessageEvent event) {

        if ("FSDJump".equalsIgnoreCase(event.getEventType())) {
            EventProcessingExecutor.submit(() -> update(event));
        }
    }

    private static void update(EddnMessageEvent event) {
        log.info("EDDN FsdJump " + event.getEventType());
        EddnDto data = event.getData();
        SINGLETONS.getStarSystemManager().saveFsdJump(data);
    }
}