package elite.vault.db.managers;

import elite.vault.db.dao.SystemDao;
import elite.vault.db.util.Database;
import elite.vault.db.util.TimeUtil;
import elite.vault.eddn.dto.ScanDto;

import java.util.ArrayList;
import java.util.List;

public class StarSystemManager {

    private static final StarSystemManager INSTANCE = new StarSystemManager();

    private StarSystemManager() {
    }

    public static StarSystemManager getInstance() {
        return INSTANCE;
    }

    public void save(ScanDto data) {
        Database.withDao(SystemDao.class, dao -> {
            dao.upsert(toEntity(data));
            return Void.TYPE;
        });
    }

    private SystemDao.StarSystem toEntity(ScanDto data) {
        SystemDao.StarSystem entity = new SystemDao.StarSystem();
        entity.setSystemAddress(data.getSystemAddress());
        entity.setStarName(data.getStarSystem());
        Double x = data.getStarPos().get(0);
        Double y = data.getStarPos().get(1);
        Double z = data.getStarPos().get(2);
        entity.setX(x);
        entity.setY(y);
        entity.setZ(z);
        int sx = (int) Math.floor((x + 40000.0) / 1000.0);
        int sy = (int) Math.floor((y + 4000.0) / 1000.0);
        int sz = (int) Math.floor((z + 40000.0) / 1000.0);
        String sector = String.format("%d_%d_%d", sx, sy, sz);
        entity.setSector(sector);
        return entity;
    }

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

    public SystemDao.StarSystem findByName(String starName) {
        return Database.withDao(SystemDao.class, dao -> dao.findByName(starName));
    }

    public List<SystemDao.StarSystem> findNeighbors(double v, double v1, double v2, double v3, double v4, double v5, double x, double y, double z, String currName, String currSector) {
        return Database.withDao(SystemDao.class, dao -> dao.findNeighbors(v, v1, v2, v3, v4, v5, x, y, z, currName, getAdjacentSectors(currSector)));
    }

    public void saveBootStrapData(String sysName, long sysAddr, double x, double y, double z) {
        Database.withDao(SystemDao.class, dao -> {
            dao.upsert(toEntity(sysName, sysAddr, x, y, z));
            return Void.TYPE;
        });
    }

    private SystemDao.StarSystem toEntity(String sysName, long sysAddr, double x, double y, double z) {
        SystemDao.StarSystem entity = new SystemDao.StarSystem();
        entity.setDate(TimeUtil.getCurrentTimestamp());
        entity.setSystemAddress(sysAddr);
        entity.setStarName(sysName);
        entity.setX(x);
        entity.setY(y);
        entity.setZ(z);
        int sx = (int) Math.floor((x + 40000.0) / 1000.0);
        int sy = (int) Math.floor((y + 4000.0) / 1000.0);
        int sz = (int) Math.floor((z + 40000.0) / 1000.0);
        String sector = String.format("%d_%d_%d", sx, sy, sz);
        entity.setSector(sector);
        return entity;
    }
}
