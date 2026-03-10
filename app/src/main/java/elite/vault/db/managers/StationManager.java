package elite.vault.db.managers;

import elite.vault.db.dao.StationsDao;
import elite.vault.db.dao.SystemDao;
import elite.vault.db.util.Database;
import elite.vault.eddn.dto.EDDN_EconomyDto;
import elite.vault.eddn.dto.EddnDto;
import elite.vault.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

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

        // Serialize economies as a JSON array of EDDN_EconomyDto objects.
        // The schema column is JSON type; storing a proper array lets
        // JSON_OVERLAPS and the functional index on economies work correctly.
        // Example output: [{"Name":"$economy_Industrial;","Proportion":0.7}, ...]
        String economiesJson = toJson(data.getEconomies());

        // Services is a plain list of strings from EDDN — serialize as JSON array.
        // Example output: ["Commodities","Refuel","Repair"]
        String servicesJson = toJson(data.getStationServices());

        StationsDao.Station entity = new StationsDao.Station();
        entity.setMarketId(data.getMarketId());
        entity.setSystemAddress(data.getSystemAddress());
        entity.setRealName(data.getStationName());
        entity.setStationType(data.getStationType());
        entity.setDistanceToArrival(data.getDistFromStarLs());
        entity.setPrimaryEconomy(data.getStationEconomy());
        entity.setEconomies(economiesJson);
        entity.setGovernment(data.getStationGovernment());
        entity.setServices(servicesJson);
        entity.setControllingFaction(
                data.getStationFaction() == null ? null : data.getStationFaction().getName());
        entity.setControllingFactionState(null); // TODO: EDDN does not currently supply this
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

    /**
     * Serializes any object to a JSON string using the project Gson instance.
     * Returns null if the input is null or empty (preserves DB NULL rather than
     * storing an empty array, which would be a non-null JSON value).
     */
    private static String toJson(Object value) {
        if (value == null) return null;
        if (value instanceof List<?> list && list.isEmpty()) return null;
        return GsonFactory.getGson().toJson(value);
    }

    /**
     * Builds a station entity for the bootstrap importer path.
     * economies and services come from Spansh dump data — already typed as
     * List<EDDN_EconomyDto> and List<String> respectively in BootstrapEntryDto.
     * Serialized to JSON here for consistency with the EDDN ingest path.
     */
    public static String toEconomiesJson(List<EDDN_EconomyDto> economies) {
        return toJson(economies);
    }

    public static String toServicesJson(List<String> services) {
        return toJson(services);
    }
}