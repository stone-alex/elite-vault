package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.eddn.dto.EDDN_SignalDto;
import elite.vault.eddn.dto.EddnDto;
import elite.vault.eddn.events.EddnMessageEvent;

import java.util.Arrays;
import java.util.List;

public class SignalsSubscriber {

    private static final List<String> events = Arrays.asList("SAASignalsFound");

    @Subscribe
    public void onEvent(EddnMessageEvent event) {
        if (!event.isJournal()) return;
        if (!events.contains(event.getEventType())) return;

        EddnDto data = event.getData();
        if (data == null) return;

        Long systemAddress = data.getSystemAddress();
        List<EDDN_SignalDto> signals = data.getSignals();
        data.getBodyId();
        data.getBodyName();
    }

}
