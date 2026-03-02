package elite.vault.db.managers;

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
        Database.withDao(StellarObjectDao.class, dao -> {
            dao.upsert(toEntity(data));
            return Void.TYPE;
        });

    }

    private StellarObjectDao.StellarObject toEntity(ScanDto dto) {
        StellarObjectDao.StellarObject data = new StellarObjectDao.StellarObject();
        data.setTimestamp(TimeUtil.toEntityDateTime(dto.getTimestamp()));
        data.setData(dto.toJson());
        data.setStarSystem(dto.getStarSystem());
        data.setSystemAddress(dto.getSystemAddress());
        data.setX(dto.getStarPos().get(0));
        data.setY(dto.getStarPos().get(1));
        data.setZ(dto.getStarPos().get(2));
        return data;
    }

    public StellarObjectDao.StellarObject findByName(String fromSystem) {
        return Database.withDao(StellarObjectDao.class, dao -> dao.findByName(fromSystem));
    }

    public List<StellarObjectDao.StellarObject> findNeighbors(double v, double v1, double v2, double v3, double v4, double v5, double x, double y, double z, String currName) {
        return Database.withDao(StellarObjectDao.class, dao -> dao.findNeighbors(v, v1, v2, v3, v4, v5, x, y, z, currName));
    }
}
