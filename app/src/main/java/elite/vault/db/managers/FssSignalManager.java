package elite.vault.db.managers;

import elite.vault.db.dao.FssSignalDao;
import elite.vault.db.util.Database;
import elite.vault.eddn.dto.EDDN_FssSignalDto;
import elite.vault.eddn.dto.EDDN_FssSignalMessageDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public final class FssSignalManager {

    private static final Logger log = LogManager.getLogger(FssSignalManager.class);
    private static final FssSignalManager INSTANCE = new FssSignalManager();

    private FssSignalManager() {
    }

    public static FssSignalManager getInstance() {
        return INSTANCE;
    }

    public void save(EDDN_FssSignalMessageDto data) {
        if (data.getStarSystem() == null) return;
        if (data.getSystemAddress() == null) return;

        List<EDDN_FssSignalDto> signals = data.getSignals();
        if (signals == null || signals.isEmpty()) return;

        for (EDDN_FssSignalDto signal : signals) {
            if (!signal.isResourceExtractionSite()) continue;
            if (signal.getSignalName() == null) {
                log.debug("RES signal dropped - missing signalName (system={})", data.getStarSystem());
                continue;
            }

            FssSignalDao.FssSignal entity = new FssSignalDao.FssSignal();
            entity.setSystemAddress(data.getSystemAddress());
            entity.setStarSystem(data.getStarSystem());
            entity.setSignalName(signal.getSignalName());
            entity.setSignalType(signal.getSignalType());
            entity.setUssType(signal.getUssType());
            entity.setSpawningFaction(signal.getSpawningFaction());
            entity.setSpawningState(signal.getSpawningState());
            entity.setThreatLevel(signal.getThreatLevel());

            Database.withDao(FssSignalDao.class, dao -> {
                        dao.upsert(entity);
                        return Void.TYPE;
                    }
            );


            log.info("FSS RES upserted: {} in {}", signal.getSignalName(), data.getStarSystem());
        }
    }
}