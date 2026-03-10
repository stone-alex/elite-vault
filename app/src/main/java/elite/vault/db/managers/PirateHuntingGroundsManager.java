package elite.vault.db.managers;

import elite.vault.api.dto.API_HuntingGroundDto;
import elite.vault.api.dto.API_MissionProviderDto;
import elite.vault.db.dao.MissionProviderDao;
import elite.vault.db.dao.PirateHuntingGroundsDao;
import elite.vault.db.util.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public final class PirateHuntingGroundsManager {

    private static final Logger log = LogManager.getLogger(PirateHuntingGroundsManager.class);
    private static final PirateHuntingGroundsManager INSTANCE = new PirateHuntingGroundsManager();

    private PirateHuntingGroundsManager() {
    }

    public static PirateHuntingGroundsManager getInstance() {
        return INSTANCE;
    }

    public List<API_HuntingGroundDto> findHuntingGrounds(double x, double y, double z, double rangeLy) {
        log.info("Hunting grounds search: ({}, {}, {}) range={}ly", x, y, z, rangeLy);

        List<PirateHuntingGroundsDao.HuntingGround> results = Database.withDao(
                PirateHuntingGroundsDao.class,
                dao -> dao.findHuntingGrounds(x, y, z, rangeLy)
        );

        return results.stream().map(r -> {
            API_HuntingGroundDto dto = new API_HuntingGroundDto();
            dto.setSystemName(r.getStarName());
            dto.setX(r.getX());
            dto.setY(r.getY());
            dto.setZ(r.getZ());
            dto.setDistanceLy(r.getDistanceLy());
            dto.setResGrades(r.getResGrades());
            dto.setPirateFactions(r.getPirateFactions());
            dto.setConfirmedCount(r.getMaxConfirmed());
            dto.setLastSeen(r.getLastSeen() != null ? r.getLastSeen().toString() : null);
            dto.setMissionProviders(findProviders(r));
            return dto;
        }).collect(Collectors.toList());
    }

    private List<API_MissionProviderDto> findProviders(PirateHuntingGroundsDao.HuntingGround huntingGround) {
        List<MissionProviderDao.MissionProvider> providers = Database.withDao(
                MissionProviderDao.class,
                dao -> dao.findProviders(
                        huntingGround.getX(),
                        huntingGround.getY(),
                        huntingGround.getZ(),
                        huntingGround.getSystemAddress()
                )
        );

        return providers.stream().map(p -> {
            API_MissionProviderDto dto = new API_MissionProviderDto();
            dto.setStationName(p.getStationName());
            dto.setStationType(p.getStationType());
            dto.setSystemName(p.getSystemName());
            dto.setDistanceLy(p.getDistanceLy());
            dto.setDistanceToArrival(p.getDistanceToArrival());
            dto.setHasLargePad(p.isHasLargePad());
            dto.setControllingFaction(p.getControllingFaction());
            dto.setFactionState(p.getFactionState());
            dto.setInfluence(p.getInfluence());
            return dto;
        }).collect(Collectors.toList());
    }
}