package elite.vault.db.managers;

import elite.vault.api.dto.API_CommodityDto;
import elite.vault.api.dto.API_TradePairDto;
import elite.vault.api.dto.API_TradeRouteDto;
import elite.vault.db.dao.CommodityDao;
import elite.vault.db.dao.SystemDao;
import elite.vault.db.projections.BuyCandidateProjection;
import elite.vault.db.projections.CommodityOfferProjection;
import elite.vault.db.projections.SellCandidateProjection;
import elite.vault.db.util.Database;
import elite.vault.eddn.dto.EDDN_CommodityItemDto;
import elite.vault.eddn.dto.EddnDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class MarketManager {

    private static final Logger log = LogManager.getLogger(MarketManager.class);
    private static final MarketManager INSTANCE = new MarketManager();

    private static final int DEFAULT_CARGO_CAP = 512;
    private static final int BUY_CANDIDATE_LIMIT = 20;
    private static final int MIN_STOCK = 10;
    private static final int MIN_DEMAND = 10;

    private static final String PLANETARY_TYPES_IN_CLAUSE =
            CommodityDao.PLANETARY_STATION_TYPES.stream()
                    .map(t -> "'" + t.replace("'", "''") + "'")
                    .collect(Collectors.joining(","));

    private final ConcurrentHashMap<String, Short> commodityTypeCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Object> marketLocks = new ConcurrentHashMap<>();

    // Cache systemAddress → starName to avoid repeated lookups when building DTOs.
    // Unbounded but star_system grows slowly and names never change.
    private final ConcurrentHashMap<Long, String> systemNameCache = new ConcurrentHashMap<>();

    private MarketManager() {
        loadCommodityTypeCache();
    }

    public static MarketManager getInstance() {
        return INSTANCE;
    }


    // -------------------------------------------------------------------------
    // Cache management
    // -------------------------------------------------------------------------

    private void loadCommodityTypeCache() {
        try {
            List<CommodityDao.CommodityTypeRow> rows = Database.withDao(CommodityDao.class,
                    CommodityDao::loadAllCommodityTypes);
            for (CommodityDao.CommodityTypeRow row : rows) {
                commodityTypeCache.put(row.getName(), row.getId());
            }
            log.info("Commodity type cache loaded: {} entries", commodityTypeCache.size());
        } catch (Exception e) {
            log.info("Commodity type cache empty (blank DB or first run)");
        }
    }

    private short resolveCommodityId(String name) {
        return commodityTypeCache.computeIfAbsent(name, n ->
                Database.withDao(CommodityDao.class, dao -> {
                    dao.insertCommodityTypeIfAbsent(n);
                    return dao.findCommodityTypeId(n);
                })
        );
    }

    /**
     * Resolves a systemAddress to a star name.
     * Cached after first lookup — system names are stable.
     */
    private String resolveSystemName(long systemAddress) {
        return systemNameCache.computeIfAbsent(systemAddress, addr -> {
            SystemDao.StarSystem sys = Database.withDao(SystemDao.class,
                    dao -> dao.findByAddress(addr));
            return sys != null ? sys.getStarName() : "Unknown";
        });
    }


    // -------------------------------------------------------------------------
    // Ingest
    // -------------------------------------------------------------------------

    public void save(EddnDto data, Long systemAddress) {
        if (data == null || systemAddress == null) return;
        Long marketId = data.getMarketId();
        if (marketId == null) return;
        List<EDDN_CommodityItemDto> commodities = data.getCommodities();
        if (commodities == null || commodities.isEmpty()) return;

        List<CommodityDao.CommodityRow> batch = new ArrayList<>(commodities.size());
        for (EDDN_CommodityItemDto item : commodities) {
            if (item == null || item.getName() == null) continue;
            batch.add(toRow(item, marketId, systemAddress, resolveCommodityId(item.getName())));
        }
        if (batch.isEmpty()) return;

        final long mid = marketId;
        synchronized (marketLocks.computeIfAbsent(mid, k -> new Object())) {
            saveWithRetry(mid, batch);
        }
    }

    private void saveWithRetry(long mid, List<CommodityDao.CommodityRow> batch) {
        int attempts = 0;
        while (true) {
            try {
                Database.withDao(CommodityDao.class, dao -> {
                    dao.deleteByMarket(mid);
                    return null;
                });
                Database.withDao(CommodityDao.class, dao -> {
                    dao.insertBatch(batch);
                    return null;
                });
                return;
            } catch (Exception e) {
                String state = extractSqlState(e);
                if ("40001".equals(state) && attempts < 3) {
                    attempts++;
                    log.debug("Deadlock on market {} — retry {}/3", mid, attempts);
                    try {
                        Thread.sleep(50L * attempts);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    private String extractSqlState(Throwable t) {
        while (t != null) {
            if (t instanceof java.sql.SQLException) return ((java.sql.SQLException) t).getSQLState();
            t = t.getCause();
        }
        return null;
    }

    private CommodityDao.CommodityRow toRow(EDDN_CommodityItemDto item, long marketId,
                                            long systemAddress, short commodityId) {
        CommodityDao.CommodityRow row = new CommodityDao.CommodityRow();
        row.setMarketId(marketId);
        row.setCommodityId(commodityId);
        row.setSystemAddress(systemAddress);
        row.setBuyPrice((int) item.getBuyPrice());
        row.setSellPrice((int) item.getSellPrice());
        row.setStock((int) item.getStock());
        row.setDemand((int) item.getDemand());
        return row;
    }


    // -------------------------------------------------------------------------
    // Query: commodity offers
    // -------------------------------------------------------------------------

    public List<API_CommodityDto> findCommodities(String commodityName,
                                                  String startingSystem,
                                                  int maxDistance) {
        if (commodityName == null || startingSystem == null) return Collections.emptyList();

        Short commodityId = commodityTypeCache.get(commodityName);
        if (commodityId == null) {
            final String lower = commodityName.toLowerCase();
            commodityId = commodityTypeCache.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(lower))
                    .map(Map.Entry::getValue)
                    .findFirst().orElse(null);
        }
        if (commodityId == null) return Collections.emptyList();

        final short cid = commodityId;
        SystemDao.StarSystem origin = Database.withDao(SystemDao.class,
                dao -> dao.findByName(startingSystem));
        if (origin == null) return Collections.emptyList();

        return Database.withDao(CommodityDao.class, dao -> {
            List<CommodityOfferProjection> rows = dao.findBestCommodityOffers(
                    cid, maxDistance, origin.getX(), origin.getY(), origin.getZ());
            List<API_CommodityDto> result = new ArrayList<>(rows.size());
            for (CommodityOfferProjection r : rows) {
                API_CommodityDto dto = new API_CommodityDto();
                dto.setCommodity(r.getCommodity());
                dto.setDistanceLy(r.getDistanceLy());
                dto.setMarketId(r.getMarketId());
                dto.setSellPrice(r.getSellPrice());
                dto.setStock(r.getStock());
                dto.setSystemAddress(r.getSystemAddress());
                dto.setStationName(r.getStationName());
                dto.setStarName(resolveSystemName(r.getSystemAddress()));
                result.add(dto);
            }
            return result;
        });
    }


    // -------------------------------------------------------------------------
    // Query: trade route
    // -------------------------------------------------------------------------

/*
    public API_TradeRouteDto calculateTradeRoute(String startingSystem,
                                                 int numHops, double hopDistance,
                                                 double maxDistToArrival,
                                                 boolean requireLargePad, boolean requireMediumPad,
                                                 boolean allowPlanetary) {
        return calculateTradeRoute(startingSystem, numHops, hopDistance, maxDistToArrival,
                requireLargePad, requireMediumPad, allowPlanetary, DEFAULT_CARGO_CAP);
    }
*/

/*
    public API_TradeRouteDto calculateTradeRoute(String startingSystem,
                                                 int numHops, double hopDistance,
                                                 double maxDistToArrival,
                                                 boolean requireLargePad, boolean requireMediumPad,
                                                 boolean allowPlanetary, int cargoCapacity) {
        SystemDao.StarSystem origin = Database.withDao(SystemDao.class,
                dao -> dao.findByName(startingSystem));
        if (origin == null) {
            log.warn("Starting system not found: {}", startingSystem);
            return null;
        }
        return calculateTradeRoute(origin.getX(), origin.getY(), origin.getZ(),
                numHops, hopDistance, maxDistToArrival,
                requireLargePad, requireMediumPad, allowPlanetary, cargoCapacity);
    }
*/

    public API_TradeRouteDto calculateTradeRoute(double startX, double startY, double startZ,
                                                 int numHops, double hopDistance,
                                                 double maxDistToArrival,
                                                 boolean requireLargePad, boolean requireMediumPad,
                                                 boolean allowPlanetary, int cargoCapacity) {
        long startTime = System.currentTimeMillis();

        Map<Integer, API_TradePairDto> legs = new LinkedHashMap<>();
        double curX = startX, curY = startY, curZ = startZ;
        Set<String> usedMarkets = new HashSet<>();

        for (int hop = 1; hop <= numHops; hop++) {
            final double refX = curX, refY = curY, refZ = curZ;

            // Step 1: buy candidates near current position — stations only, no star_system join
            List<BuyCandidateProjection> buyCandidates = Database.withDao(CommodityDao.class,
                    dao -> dao.findBuyCandidates(
                            refX, refY, refZ, hopDistance, MIN_STOCK, maxDistToArrival,
                            requireLargePad, requireMediumPad, allowPlanetary,
                            PLANETARY_TYPES_IN_CLAUSE, BUY_CANDIDATE_LIMIT));

            if (buyCandidates.isEmpty()) {
                log.debug("Hop {}: no buy candidates near ({}, {}, {})", hop, refX, refY, refZ);
                break;
            }

            // Step 2: for each candidate find best sell, pick highest profit
            BuyCandidateProjection bestBuy = null;
            SellCandidateProjection bestSell = null;
            int bestProfit = 0;

            for (BuyCandidateProjection buy : buyCandidates) {
                if (usedMarkets.contains("buy:" + buy.getBuyMarketId())) continue;

                SellCandidateProjection sell = Database.withDao(CommodityDao.class,
                        dao -> dao.findBestSell(
                                buy.getCommodityId(), buy.getBuyMarketId(), buy.getBuyPrice(),
                                buy.getBuyX(), buy.getBuyY(), buy.getBuyZ(),
                                hopDistance, MIN_DEMAND, maxDistToArrival,
                                requireLargePad, requireMediumPad, allowPlanetary,
                                PLANETARY_TYPES_IN_CLAUSE));

                if (sell == null) continue;
                if (usedMarkets.contains("sell:" + sell.getSellMarketId())) continue;

                int profit = sell.getSellPrice() - buy.getBuyPrice();
                if (profit > bestProfit) {
                    bestProfit = profit;
                    bestBuy = buy;
                    bestSell = sell;
                }
            }

            if (bestBuy == null) {
                log.debug("Hop {}: no profitable pair found", hop);
                break;
            }

            usedMarkets.add("buy:" + bestBuy.getBuyMarketId());
            usedMarkets.add("sell:" + bestSell.getSellMarketId());

            int effectiveUnits = Math.min(
                    Math.min(bestBuy.getBuyStock(), bestSell.getSellDemand()), cargoCapacity);

            // Resolve system names from cache (cheap — no extra DB query if cached)
            String buySystemName = resolveSystemName(bestBuy.getBuySystemAddress());
            String sellSystemName = resolveSystemName(bestSell.getSellSystemAddress());

            API_TradePairDto dto = new API_TradePairDto();
            dto.setSourceSystem(buySystemName);
            dto.setSourceStation(bestBuy.getBuyStation());
            dto.setDestinationSystem(sellSystemName);
            dto.setDestinationStation(bestSell.getSellStation());
            dto.setSourceMarketId(bestBuy.getBuyMarketId());
            dto.setDestinationMarketId(bestSell.getSellMarketId());
            dto.setCommodity(bestBuy.getCommodityName());
            dto.setBuyPrice(bestBuy.getBuyPrice());
            dto.setSellPrice(bestSell.getSellPrice());
            dto.setProfitPerUnit(bestProfit);
            dto.setStock(bestBuy.getBuyStock());
            dto.setDemand(bestSell.getSellDemand());
            dto.setDistanceLy((float) bestBuy.getDistanceFromRef());
            dto.setEstimatedRunProfit((long) bestProfit * effectiveUnits);

            legs.put(hop, dto);

            curX = bestSell.getSellX();
            curY = bestSell.getSellY();
            curZ = bestSell.getSellZ();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        API_TradeRouteDto route = new API_TradeRouteDto();
        route.setRoute(legs);
        route.setNote(String.format("Calculated %d leg(s) in %dms.", legs.size(), elapsed));
        return route;
    }
}