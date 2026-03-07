package elite.vault.db.managers;

import com.google.common.eventbus.Subscribe;
import elite.vault.db.dao.StationsDao;
import elite.vault.db.util.Database;
import elite.vault.eddn.dto.EddnDto;

public class StationManager {

    private static final StationManager INSTANCE = new StationManager();

    private StationManager() {
    }

    public static StationManager getInstance() {
        return INSTANCE;
    }


    @Subscribe
    public void saveStations(EddnDto data) {
        StringBuilder economies = new StringBuilder();
        StringBuilder services = new StringBuilder();

        if (data.getEconomies() != null) data.getEconomies().forEach(economy -> economies.append(economy).append(", "));
        if (data.getStationServices() != null) data.getStationServices().forEach(service -> services.append(service).append(", "));

        Database.withDao(StationsDao.class, dao -> {
            if (data.getMarketId() == null) return Void.TYPE;
            StationsDao.Station entity = new StationsDao.Station();
            entity.setDistanceToArrival(data.getDistFromStarLs());
            entity.setStationId(data.getMarketId());
            entity.setStationType(data.getStationType());
            entity.setSystemAddress(data.getSystemAddress());
            entity.setControllingFaction(data.getStationFaction() == null ? null : data.getStationFaction().getName());
            entity.setControllingFactionState(data.getStationFaction() == null ? null : data.getStationFaction().getName());
            entity.setEconomies(economies.toString());
            entity.setRealName(data.getStationName());
            entity.setGovernment(data.getStationGovernment());
            if (data.getLandingPads() != null) {
                entity.setHasLargePad(data.getLandingPads().getLarge() != null && data.getLandingPads().getLarge() > 0);
                entity.setHasMediumPad(data.getLandingPads().getMedium() != null && data.getLandingPads().getMedium() > 0);
                entity.setHasSmallPad(data.getLandingPads().getSmall() != null && data.getLandingPads().getSmall() > 0);
            }
            entity.setPrimaryEconomy(data.getStationEconomy());
            entity.setServices(services.toString());
            dao.upsert(entity);
            return Void.TYPE;
        });
    }
}
