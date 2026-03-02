package elite.vault.db.managers;

import elite.vault.db.dao.StellarObjectDao;
import elite.vault.db.util.Database;
import elite.vault.db.util.TimeUtil;
import elite.vault.eddn.dto.ScanDto;

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
        data.setBodyId(dto.getBodyId());
        data.setData(dto.toJson());
        data.setStarSystem(dto.getStarSystem());
        data.setSystemAddress(dto.getSystemAddress());
        data.setX(dto.getStarPos().get(0));
        data.setY(dto.getStarPos().get(1));
        data.setZ(dto.getStarPos().get(2));
        return data;
    }
}
