package elite.vault.eddn.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.vault.eddn.dto.ScanDto;
import elite.vault.eddn.events.EddnMessageEvent;
import elite.vault.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

import static elite.vault.Singletons.INSTANCE;

public class ScanSubscriber {

    private static final Logger log = LogManager.getLogger(ScanSubscriber.class);

    List<String> temp = Arrays.asList("FSDJump", "Scan");

    @Subscribe
    public void onEvent(EddnMessageEvent event) {

        if (temp.contains(event.eventType())) {
            System.out.println(event.eventType());
            //System.out.println(event.messageNode().toPrettyString());
        }

        if (!event.isJournal() || !"Scan".equals(event.eventType())) {
            return;
        }

        try {
            String json = INSTANCE.getObjectMapper().writeValueAsString(event.messageNode());
            ScanDto dto = GsonFactory.getGson().fromJson(json, ScanDto.class);
            // save repo.saveBodyScan(dto);
            //System.out.println(dto.toJson());

        } catch (Exception e) {
            log.error("Unable to process EDEN event " + event.eventType(), e);
        }
    }
}
