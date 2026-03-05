package elite.vault.db.managers;

import elite.vault.api.dto.API_CommodityDto;
import elite.vault.api.dto.API_TradePairDto;
import elite.vault.api.dto.API_TradeRouteDto;
import elite.vault.db.dao.CommodityDao;
import elite.vault.db.dao.MarketDao;
import elite.vault.db.dao.SystemDao;
import elite.vault.db.projections.CommodityOfferProjection;
import elite.vault.db.projections.TradePairProjection;
import elite.vault.db.util.Database;
import elite.vault.db.util.TimeUtil;
import elite.vault.eddn.dto.EDDN_CommodityItemDto;
import elite.vault.eddn.dto.EddnDto;

import java.util.*;

public final class MarketManager {

    private static final MarketManager INSTANCE = new MarketManager();

    private MarketManager() {
    }

    public static MarketManager getInstance() {
        return INSTANCE;
    }

    public void save(EddnDto data, Long systemAddress, double x, double y, double z) {
        Database.withDao(MarketDao.class, dao -> {
            dao.prune();
            return Void.TYPE;
        });

        Database.withDao(MarketDao.class, dao -> {
            dao.upsert(toEntity(data), systemAddress);
            List<EDDN_CommodityItemDto> commodities = data.getCommodities();
            saveCommodities(commodities, data.getMarketId(), systemAddress, x, y, z);
            return Void.TYPE;
        });
    }

    private void saveCommodities(List<EDDN_CommodityItemDto> data, long marketId, long systemAddress, double x, double y, double z) {
        for (EDDN_CommodityItemDto d : data) {
            Database.withDao(CommodityDao.class, dao -> {
                dao.upsert(toEntity(d, marketId, systemAddress, x, y, z));
                return Void.TYPE;
            });
        }
    }

    private CommodityDao.Commodity toEntity(EDDN_CommodityItemDto data, long marketId, long systemAddress, double x, double y, double z) {
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


    private MarketDao.Market toEntity(EddnDto data) {
        MarketDao.Market entity = new MarketDao.Market();
        entity.setTimestamp(TimeUtil.toEntityDateTime(data.getTimestamp()));
        entity.setData(data.toJson());
        entity.setMarketId(data.getMarketId());
        entity.setStarSystem(data.getStarSystem());
        entity.setStationName(data.getStationName());
        return entity;
    }

    public List<API_CommodityDto> findCommodities(String commodity, String startingLocationStarSystem, int maxDistance) {
        final SystemDao.StarSystem starSystem = Database.withDao(SystemDao.class, dao -> dao.findByName(startingLocationStarSystem));
        if (starSystem == null) return Collections.emptyList();
        return Database.withDao(CommodityDao.class, dao -> {
            LinkedList<API_CommodityDto> result = new LinkedList<>();
            List<CommodityOfferProjection> entity = dao.findBestCommodityOffers(commodity, maxDistance, starSystem.getX(), starSystem.getY());
            for (CommodityOfferProjection e : entity) {
                API_CommodityDto dto = new API_CommodityDto();
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

    public API_TradeRouteDto calculateTradeRoute(String startingSystem, int numTrades, int jumpRange, double maxDistanceFromEntrance) {
        long startTime = System.currentTimeMillis();
        SystemDao.StarSystem origin = Database.withDao(SystemDao.class, dao -> dao.findByName(startingSystem));
        if (origin == null) return null;

        API_TradeRouteDto route = new API_TradeRouteDto();
        Map<Integer, API_TradePairDto> legs = new LinkedHashMap<>();

        double curX = origin.getX();
        double curY = origin.getY();

        // For hop 1, we search for buy offers near the player.
        // For hops 2+, we look at what's available to buy AT the previous sell station.
        Long currentStationMarketId = null; // null means "search nearby" (hop 1)
        Set<String> usedTrades = new HashSet<>();

        for (int hop = 1; hop <= numTrades; hop++) {
            final double refX = curX;
            final double refY = curY;

            // Step 1: Get buy offers
            List<TradePairProjection> buyOffers;
            if (currentStationMarketId == null) {
                // Hop 1: search for buy offers near starting location
                buyOffers = Database.withDao(CommodityDao.class,
                        dao -> dao.findBuyOffers(jumpRange, refX, refY, maxDistanceFromEntrance)
                );
            } else {
                // Hop 2+: what can we buy AT the station we just sold at?
                final long stationId = currentStationMarketId;
                buyOffers = Database.withDao(CommodityDao.class,
                        dao -> dao.findBuyOffersAtStation(stationId)
                );
            }

            if (buyOffers.isEmpty()) break;

            // Step 2: For each buy candidate, find best sell destination
            TradePairProjection bestBuy = null;
            TradePairProjection bestSell = null;
            double bestProfit = 0;
            int checked = 0;

            for (TradePairProjection buy : buyOffers) {
                if (checked >= 5) break;
                checked++;

                // For hop 1, use the buy station's coords as sell search origin.
                // For hop 2+, the buy station IS the current station, use its coords.
                final double bx = (buy.getBuyX() != 0) ? buy.getBuyX() : refX;
                final double by = (buy.getBuyY() != 0) ? buy.getBuyY() : refY;
                final double minSell = buy.getBuyPrice();
                final long excludeMarket = buy.getBuyMarketId();
                final String commodity = buy.getCommodity();

                List<TradePairProjection> sells = Database.withDao(CommodityDao.class,
                        dao -> dao.findBestSellFor(commodity, minSell, excludeMarket,
                                jumpRange, bx, by, maxDistanceFromEntrance)
                );

                if (!sells.isEmpty()) {
                    TradePairProjection sell = sells.getFirst();
                    String tradeKey = commodity + "|" + buy.getBuyMarketId() + "|" + sell.getSellMarketId();
                    if (usedTrades.contains(tradeKey)) continue;

                    double profit = sell.getSellPrice() - buy.getBuyPrice();
                    if (profit > bestProfit) {
                        bestProfit = profit;
                        bestBuy = buy;
                        bestSell = sell;
                    }
                }
            }

            if (bestBuy == null || bestSell == null) break;

            String tradeKey = bestBuy.getCommodity() + "|" + bestBuy.getBuyMarketId() + "|" + bestSell.getSellMarketId();
            usedTrades.add(tradeKey);

            API_TradePairDto dto = new API_TradePairDto();
            dto.setSourceSystem(bestBuy.getBuySystem());
            dto.setSourceStation(bestBuy.getBuyStation());
            dto.setDestinationSystem(bestSell.getSellSystem());
            dto.setDestinationStation(bestSell.getSellStation());
            dto.setSourceMarketId(bestBuy.getBuyMarketId());
            dto.setDestinationMarketId(bestSell.getSellMarketId());
            dto.setCommodity(bestBuy.getCommodity());
            dto.setBuyPrice(bestBuy.getBuyPrice());
            dto.setSellPrice(bestSell.getSellPrice());
            dto.setProfitPerUnit(bestProfit);
            dto.setStock(bestBuy.getBuyStock());
            dto.setDemand(bestSell.getSellDemand());
            dto.setDistanceLy(bestSell.getLegDistanceLy());

            legs.put(hop, dto);

            // Next hop: we're now AT the sell station
            currentStationMarketId = bestSell.getSellMarketId();

            final String sellSystemName = bestSell.getSellSystem();
            SystemDao.StarSystem sellSystem = Database.withDao(SystemDao.class,
                    dao -> dao.findByName(sellSystemName)
            );
            if (sellSystem != null) {
                curX = sellSystem.getX();
                curY = sellSystem.getY();
            } else {
                break;
            }
        }

        long endTime = System.currentTimeMillis();
        route.setNote("Time to complete " + ((endTime - startTime) / 1000) + " seconds");
        route.setRoute(legs);
        return route;
    }
}
