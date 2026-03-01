package elite.vault.db.managers;

import elite.vault.db.dao.MarketDao;
import elite.vault.db.util.Database;
import elite.vault.db.util.TimeUtil;
import elite.vault.eddn.dto.CommodityMessageDto;

public final class MarketManager {

    private static final MarketManager INSTANCE = new MarketManager();

    private MarketManager() {
    }

    public static MarketManager getInstance() {
        return INSTANCE;
    }

    public void save(CommodityMessageDto data) {
        Database.withDao(MarketDao.class, dao -> {
            dao.upsert(toEntity(data));
            return Void.TYPE;
        });
    }

    private MarketDao.Market toEntity(CommodityMessageDto data) {
        MarketDao.Market entity = new MarketDao.Market();
        entity.setTimestamp(TimeUtil.toEntityDateTime(data.getTimestamp()));
        entity.setData(data.toJson());
        entity.setMarketId(data.getMarketId());
        entity.setStarSystem(data.getSystemName());
        entity.setStationName(data.getStationName());
        return entity;
    }

}
