package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.eddn.dto.EDDN_FssSignalMessageDto;
import elite.vault.eddn.events.EddnMessageEvent;
import elite.vault.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.vault.Singletons.SINGLETONS;

public class FssSignalSubscriber {

    private static final Logger log = LogManager.getLogger(FssSignalSubscriber.class);

    @Subscribe
    public void onEvent(EddnMessageEvent event) {
        if (!event.matchesSchema("fsssignaldiscovered")) return;
        EDDN_FssSignalMessageDto data = GsonFactory.getGson().fromJson(event.getRawJson(), EDDN_FssSignalMessageDto.class);

        if (!isValid(data)) return;

        if (data.getSystemAddress() == null || data.getStarSystem() == null) {
            log.debug("FSSSignalDiscovered dropped - missing systemAddress or starSystem");
            return;
        }
        if (data.getSignals() == null || data.getSignals().isEmpty()) {
            log.debug("FSSSignalDiscovered dropped - no signals (system={})", data.getStarSystem());
            return;
        }
        EventProcessingExecutor.submit(() -> update(data));
    }

    private boolean isValid(EDDN_FssSignalMessageDto data) {
        boolean isValid = true;
        if (data.getSignals() == null || data.getSignals().isEmpty()) {
            isValid = false;
        }
        log.debug("FSSSignalDiscovered dropped - no signals (system={})", data.getStarSystem());
        return isValid;
    }

    private void update(EDDN_FssSignalMessageDto data) {
        log.info("EDDN FSSSignal {} signals in {}", data.getSignals().size(), data.getStarSystem());
        SINGLETONS.getFssSignalManager().save(data);
    }
}