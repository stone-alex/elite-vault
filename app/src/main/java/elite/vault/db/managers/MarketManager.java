package elite.vault.db.managers;

import elite.vault.api.dto.API_CommodityDto;
import elite.vault.api.dto.API_TradePairDto;
import elite.vault.api.dto.API_TradeRouteDto;
import elite.vault.db.dao.CommodityDao;
import elite.vault.db.dao.CommodityHasher;
import elite.vault.db.dao.SystemDao;
import elite.vault.db.projections.CommodityOfferProjection;
import elite.vault.db.projections.HopPairProjection;
import elite.vault.db.util.Database;
import elite.vault.eddn.dto.EDDN_CommodityItemDto;
import elite.vault.eddn.dto.EDDN_CommodityMessageDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class MarketManager {

    private static final Logger log = LogManager.getLogger(MarketManager.class);
    private static final MarketManager INSTANCE = new MarketManager();

    private static final int DEFAULT_CARGO_CAP = 512;
    private static final int HOP_PAIR_LIMIT = 40;  // candidates returned per hop query
    private static final int MIN_STOCK = 10;
    private static final int MIN_DEMAND = 10;

    private static final String PLANETARY_TYPES_IN_CLAUSE =
            CommodityDao.PLANETARY_STATION_TYPES.stream()
                    .map(t -> "'" + t.replace("'", "''") + "'")
                    .collect(Collectors.joining(","));

    private final ConcurrentHashMap<String, Short> commodityTypeCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Object> marketLocks = new ConcurrentHashMap<>();

    // systemAddress → starName. Unbounded but stable — system names never change.
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

    public void save(EDDN_CommodityMessageDto data, Long systemAddress) {
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
        // -----------------------------------------------------------------------
        // Deduplication gate — compute hash of incoming snapshot and compare to
        // the last accepted hash for this market. If unchanged, skip the replace
        // entirely. This prevents the common EDDN pattern of 4 commanders docking
        // at the same station within a second, all sending identical snapshots,
        // from causing 4 DELETE + INSERT cycles on the commodity partition.
        //
        // isHashUnchanged and upsertHash are kept outside the transaction because
        // market_last_seen is a tiny separate table with no partition involvement.
        // -----------------------------------------------------------------------
        long hash = CommodityHasher.hash(batch);
        int unchanged = Database.withDao(CommodityDao.class,
                dao -> dao.isHashUnchanged(mid, hash));
        if (unchanged > 0) {
            log.trace("Market {} snapshot unchanged (hash={}), skipping replace", mid, hash);
            return;
        }

        int attempts = 0;
        while (true) {
            try {
                // DELETE + INSERT in a single transaction on one connection.
                // Prevents a concurrent read from seeing an empty market between
                // the delete and the insert.
                Database.withTransaction(handle -> {
                    CommodityDao dao = handle.attach(CommodityDao.class);
                    dao.deleteByMarket(mid);
                    dao.insertBatch(batch);
                    return null;
                });

                // Update the hash after successful replace.
                Database.withDao(CommodityDao.class, dao -> {
                    dao.upsertHash(mid, hash);
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
    //
    // Single query per hop via CommodityDao.findBestHopPairs (MySQL 8 CTE
    // self-join). Returns up to HOP_PAIR_LIMIT ranked pairs; Java picks the
    // best unused (buy market, sell market) combination and advances position
    // to the sell station for the next hop.
    //
    // Query count per hop: 1 (was: up to 21 with the old findBuyCandidates
    // + N×findBestSell pattern).
    // -------------------------------------------------------------------------

    public API_TradeRouteDto calculateTradeRoute(double startX, double startY, double startZ,
                                                 int numHops, double hopDistance,
                                                 double maxDistToArrival,
                                                 boolean requireLargePad, boolean requireMediumPad,
                                                 boolean allowPlanetary, int cargoCapacity) {
        long startTime = System.currentTimeMillis();

        Map<Integer, API_TradePairDto> legs = new LinkedHashMap<>();
        double curX = startX, curY = startY, curZ = startZ;
        Set<Long> usedBuyMarkets = new HashSet<>();
        Set<Long> usedSellMarkets = new HashSet<>();

        for (int hop = 1; hop <= numHops; hop++) {
            final double refX = curX, refY = curY, refZ = curZ;
            final int cap = cargoCapacity;

            // Single query returns top HOP_PAIR_LIMIT (buy, sell) pairs ranked
            // by runValue = profitPerUnit * LEAST(stock, demand, cargoCap).
            List<HopPairProjection> pairs = Database.withDao(CommodityDao.class,
                    dao -> dao.findBestHopPairs(
                            refX, refY, refZ,
                            hopDistance,
                            MIN_STOCK, MIN_DEMAND,
                            cap,
                            maxDistToArrival,
                            requireLargePad, requireMediumPad, allowPlanetary,
                            PLANETARY_TYPES_IN_CLAUSE,
                            HOP_PAIR_LIMIT));

            if (pairs.isEmpty()) {
                log.debug("Hop {}: no pairs near ({}, {}, {})", hop, refX, refY, refZ);
                break;
            }

            // Pick the best pair that doesn't reuse a market from a previous hop.
            HopPairProjection best = null;
            for (HopPairProjection p : pairs) {
                if (usedBuyMarkets.contains(p.getBuyMarketId())) continue;
                if (usedSellMarkets.contains(p.getSellMarketId())) continue;
                best = p;
                break;  // pairs are already sorted by runValue DESC by the query
            }

            if (best == null) {
                log.debug("Hop {}: all candidate pairs use already-visited markets", hop);
                break;
            }

            usedBuyMarkets.add(best.getBuyMarketId());
            usedSellMarkets.add(best.getSellMarketId());

            int effectiveUnits = Math.min(
                    Math.min(best.getBuyStock(), best.getSellDemand()), cargoCapacity);

            API_TradePairDto dto = new API_TradePairDto();
            dto.setSourceSystem(resolveSystemName(best.getBuySystemAddress()));
            dto.setSourceStation(best.getBuyStation());
            dto.setDestinationSystem(resolveSystemName(best.getSellSystemAddress()));
            dto.setDestinationStation(best.getSellStation());
            dto.setSourceMarketId(best.getBuyMarketId());
            dto.setDestinationMarketId(best.getSellMarketId());
            dto.setCommodity(best.getCommodityName());
            dto.setBuyPrice(best.getBuyPrice());
            dto.setSellPrice(best.getSellPrice());
            dto.setProfitPerUnit(best.getProfitPerUnit());
            dto.setStock(best.getBuyStock());
            dto.setDemand(best.getSellDemand());
            dto.setDistanceLy((float) best.getDistanceFromRef());
            dto.setEstimatedRunProfit((long) best.getProfitPerUnit() * effectiveUnits);

            legs.put(hop, dto);

            // Advance position to sell station for next hop.
            curX = best.getSellX();
            curY = best.getSellY();
            curZ = best.getSellZ();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        API_TradeRouteDto route = new API_TradeRouteDto();
        route.setRoute(legs);
        route.setNote(String.format("Calculated %d leg(s) in %dms.", legs.size(), elapsed));
        return route;
    }
}