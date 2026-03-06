package elite.vault.db.managers;

import elite.vault.bootstrap.BootstrapEntryDto;
import elite.vault.db.dao.MaterialsDao;
import elite.vault.db.dao.RingsDao;
import elite.vault.db.dao.StationsDao;
import elite.vault.db.dao.StellarObjectDao;
import elite.vault.db.util.Database;
import elite.vault.db.util.TimeUtil;
import elite.vault.eddn.dto.EDDN_MaterialDto;
import elite.vault.eddn.dto.EddnDto;

import java.util.List;

public class StellarObjectManager {

    private static final StellarObjectManager INSTANCE = new StellarObjectManager();

    private StellarObjectManager() {
    }

    public static StellarObjectManager getInstance() {
        return INSTANCE;
    }

    public void save(EddnDto data) {
        Database.withDao(StellarObjectDao.class, dao -> {
            dao.upsert(toEntity(data));
            return Void.TYPE;
        });

        saveMaterials(data.getMaterials(), data.getSystemAddress(), data.getBodyId(), data.getBodyName());
    }


    public void savePartial(EddnDto data) {
        Database.withDao(StellarObjectDao.class, dao -> {
            StellarObjectDao.StellarObject entity = dao.findBy(data.getSystemAddress(), data.getBodyId());
            if (entity == null) {
                dao.upsert(toEntity(data));
            }
            return Void.TYPE;
        });
    }

    private void saveMaterials(List<EDDN_MaterialDto> materials, Long systemAddress, Long bodyId, String bodyName) {
        if (materials == null || materials.isEmpty()) return;
        for (EDDN_MaterialDto m : materials) {
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

    private StellarObjectDao.StellarObject toEntity(EddnDto dto) {
        StellarObjectDao.StellarObject data = new StellarObjectDao.StellarObject();
        data.setTimestamp(TimeUtil.toEntityDateTime(dto.getTimestamp()));
        data.setBodyId(dto.getBodyId());
        data.setBodyName(dto.getBodyName());
        data.setSystemAddress(dto.getSystemAddress());
        data.setX(dto.getStarPos().get(0));
        data.setY(dto.getStarPos().get(1));
        data.setZ(dto.getStarPos().get(2));
        data.setAtmosphereType(dto.getAtmosphereType());
        data.setDistanceFromArrivalLs(dto.getDistanceFromArrivalLs());
        data.setEccentricity(dto.getEccentricity());
        data.setLandable(dto.getLandable());
        data.setMassEm(dto.getMassEm());
        data.setMeanAnomaly(dto.getMeanAnomaly());
        data.setOrbitalInclination(dto.getOrbitalInclination());
        data.setOrbitalPeriod(dto.getOrbitalPeriod());
        data.setPeriapsis(dto.getPeriapsis());
        data.setPlanetClass(dto.getPlanetClass());
        data.setRadius(dto.getRadius());
        data.setRotationPeriod(dto.getRotationPeriod());
        data.setSemiMajorAxis(dto.getSemiMajorAxis());
        data.setSurfaceGravity(dto.getSurfaceGravity());
        data.setSurfacePressure(dto.getSurfacePressure());
        data.setSurfaceTemperature(dto.getSurfaceTemperature());
        data.setTerraformState(dto.getTerraformState());
        data.setTidalLock(dto.getTidalLock() != null && dto.getTidalLock());
        data.setVolcanism(dto.getVolcanism());
        return data;
    }

    private StellarObjectDao.StellarObject toEntity(BootstrapEntryDto dto, double x, double y, double z) {
        StellarObjectDao.StellarObject data = new StellarObjectDao.StellarObject();
        data.setTimestamp(TimeUtil.toEntityDateTime(dto.getTimestamp()));
        data.setBodyId(dto.getBodyId());
        data.setBodyName(dto.getBodyName());
        data.setSystemAddress(dto.getSystemAddress());
        data.setX(x);
        data.setY(y);
        data.setZ(z);
        data.setAtmosphereType(dto.getAtmosphereType());
        data.setDistanceFromArrivalLs(dto.getDistanceToArrival());
        //data.setEccentricity
        data.setLandable(dto.getLandable());
        //data.setMassEm
        data.setMeanAnomaly(dto.getMeanAnomaly());
        data.setOrbitalInclination(dto.getOrbitalInclination());
        data.setOrbitalPeriod(dto.getOrbitalPeriod());
        //data.setPeriapsis
        //data.setPlanetClass
        data.setRadius(dto.getRadius());
        data.setRotationPeriod(dto.getRotationalPeriod());
        data.setSemiMajorAxis(dto.getSemiMajorAxis());
        data.setSurfaceGravity(dto.getGravity());
        data.setSurfacePressure(dto.getSurfacePressure());
        data.setSurfaceTemperature(dto.getSurfaceTemperature());
        data.setTerraformState(dto.getTerraformingState());
        //data.setTidalLock
        data.setVolcanism(dto.getVolcanismType());
        return data;
    }

    public void saveBootStrapData(BootstrapEntryDto entry, String sysName, long sysAddr, double x, double y, double z) {

        Database.withDao(StellarObjectDao.class, dao -> {
            dao.upsert(toEntity(entry, x, y, z));
            return Void.TYPE;
        });

        ///
        List<BootstrapEntryDto.Ring> rings = entry.getRings();
        if (rings != null && !rings.isEmpty()) {
            saveBootstrapRings(rings, entry.getSystemAddress(), entry.getBodyId());
        }

        ///
        List<BootstrapEntryDto.Station> stations = entry.getStations();
        if (stations != null && !stations.isEmpty()) {
            saveBootstrpStations(stations, entry.getSystemAddress());
        }

    }

    private void saveBootstrpStations(List<BootstrapEntryDto.Station> stations, Long systemAddress) {
        for (BootstrapEntryDto.Station station : stations) {
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
                entity.setStationType(station.getType());
                dao.upsert(entity);
                return Void.TYPE;
            });
        }
    }

    private void saveBootstrapRings(List<BootstrapEntryDto.Ring> rings, long sysAddr, long bodyId) {
        for (BootstrapEntryDto.Ring r : rings) {
            Database.withDao(RingsDao.class, dao -> {
                RingsDao.Ring entity = new RingsDao.Ring();
                entity.setSystemAddress(sysAddr);
                entity.setBodyId(bodyId);
                entity.setInnerRadius(r.getInnerRadius());
                entity.setOuterRadius(r.getOuterRadius());
                entity.setMass(r.getMass());
                entity.setRingType(r.getType());
                entity.setSignals(r.getSignals() == null ? null : r.getSignals().toString());
                dao.upsert(entity);
                return Void.TYPE;
            });
        }
    }
}
