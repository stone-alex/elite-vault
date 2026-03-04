package elite.vault.db.managers;

import elite.vault.api.dto.CommodityDto;
import elite.vault.db.dao.CommodityDao;
import elite.vault.db.dao.MarketDao;
import elite.vault.db.dao.SystemDao;
import elite.vault.db.projections.CommodityOfferProjection;
import elite.vault.db.util.Database;
import elite.vault.db.util.TimeUtil;
import elite.vault.eddn.dto.CommodityItemDto;
import elite.vault.eddn.dto.CommodityMessageDto;

import java.util.Collections;
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

    public List<CommodityDto> findCommodities(String commodity, String startingLocationStarSystem, int maxDistance) {
        final SystemDao.StarSystem starSystem = Database.withDao(SystemDao.class, dao -> dao.findByName(startingLocationStarSystem));
        if (starSystem == null) return Collections.emptyList();
        return Database.withDao(CommodityDao.class, dao -> {
            LinkedList<CommodityDto> result = new LinkedList<>();
            List<CommodityOfferProjection> entity = dao.findBestCommodityOffers(commodity, maxDistance, starSystem.getX(), starSystem.getY());
            for (CommodityOfferProjection e : entity) {
                CommodityDto dto = new CommodityDto();
                dto.setCommodity(e.getCommodity());
                dto.setDistanceLy(e.getDistanceLy());
                dto.setMarketId(e.getMarketId());
                dto.setSellPrice(e.getSellPrice());
                dto.setStock(e.getStock());
                dto.setSystemAddress(e.getSystemAddress());
                dto.setStationName(e.getStationName());
                dto.setStarName(e.getStarName());
                result.add(dto);
            }
            return result;
        });
    }
}
