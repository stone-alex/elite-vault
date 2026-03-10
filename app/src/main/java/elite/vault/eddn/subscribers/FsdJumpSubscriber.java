package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.eddn.dto.EDDN_JournalDto;
import elite.vault.eddn.events.EddnMessageEvent;
import elite.vault.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.vault.Singletons.SINGLETONS;

public class FsdJumpSubscriber {

    private static final Logger log = LogManager.getLogger(FsdJumpSubscriber.class);

    @Subscribe
    public void onEvent(EddnMessageEvent event) {
        if (!event.isJournal()) return;
        EDDN_JournalDto data = GsonFactory.getGson().fromJson(event.getRawJson(), EDDN_JournalDto.class);
        if (!"FSDJump".equalsIgnoreCase(data.getEvent())) return;
        if (data.getStarSystem() == null || data.getSystemAddress() == null) {
            log.debug("FSDJump dropped - missing required fields");
            return;
        }
        EventProcessingExecutor.submit(() -> update(data));
    }

    private static void update(EDDN_JournalDto data) {
        log.info("EDDN FsdJump {}", data.getStarSystem());
        SINGLETONS.getStarSystemManager().saveFsdJump(data);
    }
}