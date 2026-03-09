package elite.vault.db.managers;

import elite.vault.db.dao.StationsDao;
import elite.vault.db.dao.SystemDao;
import elite.vault.db.util.Database;
import elite.vault.eddn.dto.EddnDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StationManager {

    private static final Logger log = LogManager.getLogger(StationManager.class);
    private static final StationManager INSTANCE = new StationManager();

    private StationManager() {
    }

    public static StationManager getInstance() {
        return INSTANCE;
    }

    public void saveStations(EddnDto data) {
        if (data.getMarketId() == null) return;
        if (data.getLandingPads() == null) return;
        if (data.getDistFromStarLs() == null) return;
        if (data.getSystemAddress() == null) return;

        // Resolve coordinates — must happen before the station upsert.
        // If the system hasn't arrived via EDDN yet we drop this station;
        // it will arrive again the next time a pilot visits and by then the
        // system will likely be known.
        SystemDao.StarSystem system = Database.withDao(SystemDao.class,
                dao -> dao.findByAddress(data.getSystemAddress()));

        if (system == null) {
            log.debug("Station dropped — system {} not yet in DB (station: {})",
                    data.getSystemAddress(), data.getStationName());
            return;
        }

        StringBuilder economies = new StringBuilder();
        StringBuilder services = new StringBuilder();

        if (data.getEconomies() != null) data.getEconomies().forEach(e -> economies.append(e).append(", "));
        if (data.getStationServices() != null) data.getStationServices().forEach(s -> services.append(s).append(", "));

        StationsDao.Station entity = new StationsDao.Station();
        entity.setMarketId(data.getMarketId());
        entity.setSystemAddress(data.getSystemAddress());
        entity.setRealName(data.getStationName());
        entity.setStationType(data.getStationType());
        entity.setDistanceToArrival(data.getDistFromStarLs());
        entity.setPrimaryEconomy(data.getStationEconomy());
        entity.setEconomies(economies.toString());
        entity.setGovernment(data.getStationGovernment());
        entity.setServices(services.toString());
        entity.setControllingFaction(
                data.getStationFaction() == null ? null : data.getStationFaction().getName());
//        entity.setControllingFactionState(
//                data.getStationFaction() == null ? null : data.getStationFaction().);
        entity.setControllingFactionState(null); //TODO: need data here
        entity.setHasLargePad(
                data.getLandingPads().getLarge() != null && data.getLandingPads().getLarge() > 0);
        entity.setHasMediumPad(
                data.getLandingPads().getMedium() != null && data.getLandingPads().getMedium() > 0);
        entity.setHasSmallPad(
                data.getLandingPads().getSmall() != null && data.getLandingPads().getSmall() > 0);

        // Coordinates copied from star_system — stored on stations to avoid
        // the join in hot route queries.
        entity.setX(system.getX());
        entity.setY(system.getY());
        entity.setZ(system.getZ());

        Database.withDao(StationsDao.class, dao -> {
            dao.upsert(entity);
            return Void.TYPE;
        });
    }
}