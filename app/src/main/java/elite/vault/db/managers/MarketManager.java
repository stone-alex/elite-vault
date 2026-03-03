package elite.vault.db.managers;

import elite.vault.db.dao.CommodityDao;
import elite.vault.db.dao.MarketDao;
import elite.vault.db.util.Database;
import elite.vault.db.util.TimeUtil;
import elite.vault.eddn.dto.CommodityItemDto;
import elite.vault.eddn.dto.CommodityMessageDto;

import java.util.LinkedList;
import java.util.List;

public final class MarketManager {

    private static final MarketManager INSTANCE = new MarketManager();

    private MarketManager() {
    }

    public static MarketManager getInstance() {
        return INSTANCE;
    }

    public void save(CommodityMessageDto data, Long systemAddress, double x, double y, double z) {
        Database.withDao(MarketDao.class, dao -> {
            dao.prune();
            return Void.TYPE;
        });

        Database.withDao(MarketDao.class, dao -> {
            dao.upsert(toEntity(data), systemAddress);
            List<CommodityItemDto> commodities = data.getCommodities();
            saveCommodities(commodities, data.getMarketId(), systemAddress, x, y, z);
            return Void.TYPE;
        });
    }

    private void saveCommodities(List<CommodityItemDto> data, long marketId, long systemAddress, double x, double y, double z) {
        for (CommodityItemDto d : data) {
            Database.withDao(CommodityDao.class, dao -> {
                dao.upsert(toEntity(d, marketId, systemAddress, x, y, z));
                return Void.TYPE;
            });
        }
    }

    private CommodityDao.Commodity toEntity(CommodityItemDto data, long marketId, long systemAddress, double x, double y, double z) {
        CommodityDao.Commodity entity = new CommodityDao.Commodity();
        entity.setTimestamp(TimeUtil.getCurrentTimestamp());
        entity.setBuyPrice(data.getBuyPrice());
        entity.setSellPrice(data.getSellPrice());
        entity.setCommodity(data.getName());
        entity.setDemand(data.getDemand());
        entity.setMarketId(marketId);
        entity.setStock(data.getStock());
        entity.setSystemAddress(systemAddress);
        entity.setX(x);
        entity.setY(y);
        entity.setZ(z);
        return entity;
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

    public List<CommodityItemDto> findCommodities(String commodity, boolean hasDemand) {
        LinkedList<CommodityItemDto> result = new LinkedList<>();
//        Database.withDao(MarketDao.class, dao ->{
//            List<MarketDao.Market> entity = dao.find(commodity, hasDemand);
//            for (MarketDao.Market e : entity) {
//                result.add(GsonFactory.getGson().fromJson(e.getData(), CommodityItemDto.class));
//            }
//        });
        return result;
    }
}
