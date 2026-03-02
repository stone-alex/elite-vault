package elite.vault.db.managers;

import elite.vault.db.dao.SystemDao;
import elite.vault.db.util.Database;
import elite.vault.eddn.dto.ScanDto;

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
        entity.setX(data.getStarPos().get(0));
        entity.setY(data.getStarPos().get(1));
        entity.setZ(data.getStarPos().get(2));
        return entity;
    }

    public SystemDao.StarSystem findByName(String starName) {
        return Database.withDao(SystemDao.class, dao -> dao.findByName(starName));
    }

    public List<SystemDao.StarSystem> findNeighbors(double v, double v1, double v2, double v3, double v4, double v5, double x, double y, double z, String currName) {
        return Database.withDao(SystemDao.class, dao -> dao.findNeighbors(v, v1, v2, v3, v4, v5, x, y, z, currName));
    }
}
