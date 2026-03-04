package elite.vault.db.managers;

import elite.vault.bootstrap.EntryDto;
import elite.vault.db.dao.MaterialsDao;
import elite.vault.db.dao.RingsDao;
import elite.vault.db.dao.StationsDao;
import elite.vault.db.dao.StellarObjectDao;
import elite.vault.db.util.Database;
import elite.vault.db.util.TimeUtil;
import elite.vault.eddn.dto.ScanDto;

import java.util.List;

public class StellarObjectManager {

    private static final StellarObjectManager INSTANCE = new StellarObjectManager();

    private StellarObjectManager() {
    }

    public static StellarObjectManager getInstance() {
        return INSTANCE;
    }

    public void save(ScanDto data) {
        saveMaterials(data.getMaterials(), data.getSystemAddress(), data.getBodyId(), data.getBodyName());
        Database.withDao(StellarObjectDao.class, dao -> {
            dao.upsert(toEntity(data));
            return Void.TYPE;
        });

    }

    private void saveMaterials(List<ScanDto.Material> materials, Long systemAddress, Long bodyId, String bodyName) {
        if (materials == null || materials.isEmpty()) return;
        for (ScanDto.Material m : materials) {
            Database.withDao(MaterialsDao.class, dao -> {
                MaterialsDao.Material entity = new MaterialsDao.Material();
                entity.setBodyId(bodyId);
                entity.setMaterialName(m.getName());
                entity.setPercent(m.getPercent());
                entity.setSystemAddress(systemAddress);
                entity.setBodyId(bodyId);
                entity.setBodyName(bodyName);
                dao.upsert(entity);
                return Void.TYPE;
            });
        }
    }

    private StellarObjectDao.StellarObject toEntity(ScanDto dto) {
        StellarObjectDao.StellarObject data = new StellarObjectDao.StellarObject();
        data.setTimestamp(TimeUtil.toEntityDateTime(dto.getTimestamp()));
        data.setBodyId(dto.getBodyId());
        data.setStarSystem(dto.getStarSystem());
        data.setSystemAddress(dto.getSystemAddress());
        data.setX(dto.getStarPos().get(0));
        data.setY(dto.getStarPos().get(1));
        data.setZ(dto.getStarPos().get(2));
        return data;
    }

    private StellarObjectDao.StellarObject toEntity(EntryDto dto, double x, double y, double z) {
        StellarObjectDao.StellarObject data = new StellarObjectDao.StellarObject();
        data.setTimestamp(TimeUtil.toEntityDateTime(dto.getTimestamp()));
        data.setBodyId(dto.getBodyId());
        data.setStarSystem(dto.getBodyName());
        data.setSystemAddress(dto.getSystemAddress());
        data.setX(x);
        data.setY(y);
        data.setZ(z);
        return data;
    }

    public void saveBootStrapData(EntryDto entry, String sysName, long sysAddr, double x, double y, double z) {

        Database.withDao(StellarObjectDao.class, dao -> {
            dao.upsert(toEntity(entry, x, y, z));
            return Void.TYPE;
        });

        ///
        List<EntryDto.Ring> rings = entry.getRings();
        if (rings != null && !rings.isEmpty()) {
            saveRings(rings, entry.getSystemAddress(), entry.getBodyId());
        }

        ///
        List<EntryDto.Station> stations = entry.getStations();
        if (stations != null && !stations.isEmpty()) {
            saveStations(stations, entry.getSystemAddress());
        }

    }

    private void saveStations(List<EntryDto.Station> stations, Long systemAddress) {
        for (EntryDto.Station station : stations) {
            Database.withDao(StationsDao.class, dao -> {
                StationsDao.Station entity = new StationsDao.Station();
                entity.setStationId(station.getId());
                entity.setSystemAddress(systemAddress);
                entity.setControllingFaction(station.getControllingFaction());
                entity.setControllingFactionState(station.getControllingFactionState());
                entity.setDistanceToArrival(station.getDistanceToArrival());
                entity.setEconomies(station.getEconomies());
                entity.setRealName(station.getRealName());
                entity.setGovernment(station.getGovernment());
                if (station.getLandingPads() != null) {
                    entity.setHasLargePad(station.getLandingPads().getLarge() > 0);
                    entity.setHasMediumPad(station.getLandingPads().getMedium() > 0);
                    entity.setHasSmallPad(station.getLandingPads().getSmall() > 0);
                }
                entity.setPrimaryEconomy(station.getPrimaryEconomy());
                entity.setServices(station.getServices());
                dao.upsert(entity);
                return Void.TYPE;
            });
        }
    }

    private void saveRings(List<EntryDto.Ring> rings, long sysAddr, long bodyId) {
        for (EntryDto.Ring r : rings) {
            Database.withDao(RingsDao.class, dao -> {
                RingsDao.Ring entity = new RingsDao.Ring();
                entity.setSystemAddress(sysAddr);
                entity.setBodyId(bodyId);
                entity.setInnerRadius(r.getInnerRadius());
                entity.setOuterRadius(r.getOuterRadius());
                entity.setMass(r.getMass());
                entity.setRingType(r.getType());
                entity.setSignals(r.getSignals().toString());
                dao.upsert(entity);
                return Void.TYPE;
            });
        }
    }
}
