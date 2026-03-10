package elite.vault.db.managers;

import elite.vault.db.dao.FactionsDao;
import elite.vault.db.dao.PowerPlayStateDao;
import elite.vault.db.dao.SystemDao;
import elite.vault.db.util.Database;
import elite.vault.eddn.dto.EDDN_FactionDto;
import elite.vault.eddn.dto.EddnDto;

import java.util.ArrayList;
import java.util.List;

public class StarSystemManager {

    private static final StarSystemManager INSTANCE = new StarSystemManager();

    private StarSystemManager() {
    }

    public static StarSystemManager getInstance() {
        return INSTANCE;
    }

    public void save(EddnDto data) {
        Database.withDao(SystemDao.class, dao -> {
            dao.upsert(toEntity(data));
            return Void.TYPE;
        });
    }

    private SystemDao.StarSystem toEntity(EddnDto data) {
        SystemDao.StarSystem entity = new SystemDao.StarSystem();
        entity.setSystemAddress(data.getSystemAddress());
        entity.setStarName(data.getStarSystem());
        Double x = data.getStarPos().get(0);
        Double y = data.getStarPos().get(1);
        Double z = data.getStarPos().get(2);
        entity.setX(x);
        entity.setY(y);
        entity.setZ(z);
        entity.setSector(computeSector(x, y, z));
        return entity;
    }

    public void saveBootStrapData(String sysName, long sysAddr, double x, double y, double z) {
        Database.withDao(SystemDao.class, dao -> {
            dao.upsert(toBootstrapEntity(sysName, sysAddr, x, y, z));
            return Void.TYPE;
        });
    }

    private SystemDao.StarSystem toBootstrapEntity(String sysName, long sysAddr,
                                                   double x, double y, double z) {
        SystemDao.StarSystem entity = new SystemDao.StarSystem();
        // Note: no setDate() — the date field was removed from StarSystem.
        // discovered_at is set by the DB DEFAULT CURRENT_TIMESTAMP on INSERT.
        entity.setSystemAddress(sysAddr);
        entity.setStarName(sysName);
        entity.setX(x);
        entity.setY(y);
        entity.setZ(z);
        entity.setSector(computeSector(x, y, z));
        return entity;
    }

    /**
     * Computes a 1000 ly grid sector key from galactic coordinates.
     * Used as a coarse locality bucket for display / filtering — not for
     * any hot query path (idx_ss_xyz handles spatial lookups directly).
     */
    private static String computeSector(double x, double y, double z) {
        int sx = (int) Math.floor((x + 40000.0) / 1000.0);
        int sy = (int) Math.floor((y + 4000.0) / 1000.0);
        int sz = (int) Math.floor((z + 40000.0) / 1000.0);
        return String.format("%d_%d_%d", sx, sy, sz);
    }

    public SystemDao.StarSystem findByName(String starName) {
        return Database.withDao(SystemDao.class, dao -> dao.findByName(starName));
    }

    public List<SystemDao.StarSystem> findNeighbors(double minX, double maxX,
                                                    double minY, double maxY,
                                                    double minZ, double maxZ,
                                                    double currentX, double currentY, double currentZ,
                                                    double goalX, double goalY, double goalZ,
                                                    double minDistSq, String currName) {
        return Database.withDao(
                SystemDao.class,
                dao -> dao.findNeighbors(
                        minX, maxX, minY, maxY, minZ, maxZ,
                        currentX, currentY, currentZ,
                        goalX, goalY, goalZ,
                        minDistSq, currName
                )
        );
    }

    public List<SystemDao.StarSystem> findSystemsInCorridor(double minX, double maxX,
                                                            double minY, double maxY,
                                                            double minZ, double maxZ) {
        return Database.withDao(
                SystemDao.class,
                dao -> dao.findSystemsInCorridor(minX, maxX, minY, maxY, minZ, maxZ)
        );
    }

    public void saveFsdJump(EddnDto data) {
        if (data.getSystemAllegiance() == null) return;

        Database.withDao(PowerPlayStateDao.class, dao -> {
            dao.upsert(toPowerPlayEntity(data));
            return Void.TYPE;
        });

        List<EDDN_FactionDto> factions = data.getFactions();
        if (factions == null) return;
        for (EDDN_FactionDto faction : factions) {
            if (faction == null) continue;
            if (faction.getAllegiance() == null) continue;
            Database.withDao(FactionsDao.class, dao -> {
                dao.upsert(toFactions(faction, data.getSystemAddress()));
                return Void.TYPE;
            });
        }
    }

    private FactionsDao.Faction toFactions(EDDN_FactionDto faction, Long systemAddress) {
        FactionsDao.Faction entity = new FactionsDao.Faction();
        entity.setAllegiance(faction.getAllegiance());
        entity.setFactionName(faction.getFactionName());
        entity.setFactionState(faction.getFactionState());
        entity.setSystemAddress(systemAddress);
        entity.setGovernment(faction.getGovernment());
        entity.setHappiness(faction.getHappiness());
        entity.setInfluence(faction.getInfluence());
        return entity;
    }

    private PowerPlayStateDao.PowerPlayState toPowerPlayEntity(EddnDto data) {
        PowerPlayStateDao.PowerPlayState entity = new PowerPlayStateDao.PowerPlayState();
        entity.setSystemAddress(data.getSystemAddress());
        entity.setSystemAllegiance(data.getSystemAllegiance());
        entity.setControllingFaction(
                data.getSystemFaction() == null ? null : data.getSystemFaction().getName());
        entity.setControllingPower(data.getControllingPower());
        entity.setSystemEconomy(
                data.getSystemEconomy() == null ? null :
                        data.getSystemEconomy().replace("$economy_", "").replace(";", ""));
        entity.setSystemSecondEconomy(
                data.getSystemSecondEconomy() == null ? null :
                        data.getSystemSecondEconomy().replace("$economy_", "").replace(";", ""));
        entity.setSystemGovernment(
                data.getSystemGovernment() == null ? null :
                        data.getSystemGovernment().replace("$government_", "").replace(";", ""));
        entity.setSystemSecurity(
                data.getSystemSecurity() == null ? null :
                        data.getSystemSecurity()
                                .replace("$GAlAXY_MAP_INFO_state_", "").replace(";", "")
                                .replace("$SYSTEM_SECURITY_", "").replace(";", ""));
        entity.setPowerplayState(data.getPowerplayState());
        entity.setControlProgress(data.getPowerplayStateControlProgress());
        entity.setReinforcement(data.getPowerplayStateReinforcement());
        entity.setUndermining(data.getPowerplayStateUndermining());
        return entity;
    }

    // getAdjacentSectors kept for any future use — not called internally.
    @SuppressWarnings("unused")
    private List<String> getAdjacentSectors(String currentSector) {
        String[] parts = currentSector.split("_");
        int sx = Integer.parseInt(parts[0]);
        int sy = Integer.parseInt(parts[1]);
        int sz = Integer.parseInt(parts[2]);

        List<String> sectors = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    sectors.add(String.format("%d_%d_%d", sx + dx, sy + dy, sz + dz));
                }
            }
        }
        return sectors;
    }
}