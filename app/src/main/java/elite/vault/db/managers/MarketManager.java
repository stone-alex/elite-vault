package elite.vault.db.managers;

import elite.vault.api.dto.API_CommodityDto;
import elite.vault.api.dto.API_TradePairDto;
import elite.vault.api.dto.API_TradeRouteDto;
import elite.vault.db.dao.CommodityDao;
import elite.vault.db.dao.StationsDao;
import elite.vault.db.dao.SystemDao;
import elite.vault.db.dao.TradePairDao;
import elite.vault.db.projections.CommodityOfferProjection;
import elite.vault.db.util.Database;
import elite.vault.eddn.dto.EDDN_CommodityItemDto;
import elite.vault.eddn.dto.EddnDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MarketManager {

    private static final Logger log = LogManager.getLogger(MarketManager.class);
    private static final MarketManager INSTANCE = new MarketManager();

    /**
     * Default assumed cargo capacity when the caller does not specify one.
     * Represents a mid-range trading ship (e.g. Type-7, Krait Mk II with cargo build).
     */
    private static final int DEFAULT_CARGO_CAP = 512;

    // -------------------------------------------------------------------------
    // SQL for queries that require a JDBI defineList for the planetary type
    // SQL for querying the pre-calculated trade_pair table.
    // Uses define() for <planetaryTypes> - same pattern as the live queries.
    private static final String FIND_BEST_PAIRS_SQL = """
            SELECT
                tp.commodityId,
                tp.commodityName,
                tp.buyMarketId,
                tp.buySystemAddress,
                tp.buySystem,
                tp.buyStation,
                tp.buyPrice,
                tp.buyStock,
                tp.buyX,
                tp.buyY,
                tp.buyZ,
                tp.buyHasLargePad,
                tp.buyHasMediumPad,
                tp.buyStationType,
                tp.buyDistToArrival,
                tp.sellMarketId,
                tp.sellSystemAddress,
                tp.sellSystem,
                tp.sellStation,
                tp.sellPrice,
                tp.sellDemand,
                tp.sellHasLargePad,
                tp.sellHasMediumPad,
                tp.sellStationType,
                tp.sellDistToArrival,
                tp.sellX,
                tp.sellY,
                tp.sellZ,
                tp.profitPerUnit,
                tp.distanceLy,
                tp.profitPerUnit * LEAST(tp.buyStock, tp.sellDemand, :cargoCap) AS runValue
            FROM trade_pair tp
            WHERE tp.buyDistToArrival        <= :maxDistFromEntrance
              AND tp.sellDistToArrival       <= :maxDistFromEntrance
              AND tp.profitPerUnit            > 0
              AND tp.buyStock                >= GREATEST(:cargoCap / 2, 10)
              AND tp.sellDemand              >= GREATEST(:cargoCap / 2, 10)
              AND (
                  :requireLargePad = FALSE
                  OR (tp.buyHasLargePad = TRUE AND tp.sellHasLargePad = TRUE)
              )
              AND (
                  :requireMediumPad = FALSE
                  OR ((tp.buyHasLargePad = TRUE  OR tp.buyHasMediumPad = TRUE)
                  AND (tp.sellHasLargePad = TRUE OR tp.sellHasMediumPad = TRUE))
              )
              AND (
                  :allowPlanetary = TRUE
                  OR tp.buyStationType  NOT IN (<planetaryTypes>)
              )
              AND (
                  :allowPlanetary = TRUE
                  OR tp.sellStationType NOT IN (<planetaryTypes>)
              )
              -- Buy station must be reachable from current position (sphere check).
              -- For hop 1 this is the player's current system.
              -- For hop 2..N this is the sell system of the previous leg.
              -- :searchRadius is the user-configured search radius, not ship jump range.
              AND tp.buyX BETWEEN :refX - :searchRadius AND :refX + :searchRadius
              AND tp.buyY BETWEEN :refY - :searchRadius AND :refY + :searchRadius
              AND tp.buyZ BETWEEN :refZ - :searchRadius AND :refZ + :searchRadius
              AND POW(tp.buyX - :refX, 2) + POW(tp.buyY - :refY, 2) + POW(tp.buyZ - :refZ, 2)
                      <= POW(:searchRadius, 2)
            ORDER BY tp.profitPerUnit * LEAST(tp.buyStock, tp.sellDemand, :cargoCap) DESC
            LIMIT :limit
            """;

    /**
     * Commodity name → commodity_type.id cache.
     * Loaded from DB at construction. Updated on cache miss (new commodity seen).
     * ~300 entries at steady state - fits entirely in L1.
     */
    private final ConcurrentHashMap<String, Short> commodityTypeCache = new ConcurrentHashMap<>();

    /**
     * Per-market ingest lock.
     * When two EDDN snapshots for the same station arrive concurrently (two pilots
     * opening the market panel within milliseconds), both would try to DELETE then
     * INSERT the same rows and deadlock. Keying a lock on marketId serializes ingest
     * for that station while allowing full concurrency across different stations.
     */
    private final ConcurrentHashMap<Long, Object> marketLocks = new ConcurrentHashMap<>();

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
            // Blank DB on first run - cache stays empty, entries added on first ingest
            log.info("Commodity type cache empty (blank DB or first run)");
        }
    }

    /**
     * Resolve a commodity name to its DB id.
     * On cache miss: inserts the name into commodity_type (INSERT IGNORE),
     * fetches the id, caches it. This path is hit only for genuinely new
     * commodity names - essentially never after the first ingest session.
     */
    private short resolveCommodityId(String name) {
        return commodityTypeCache.computeIfAbsent(name, n ->
                Database.withDao(CommodityDao.class, dao -> {
                    dao.insertCommodityTypeIfAbsent(n);
                    return dao.findCommodityTypeId(n);
                })
        );
    }


    // -------------------------------------------------------------------------
    // Ingest
    // -------------------------------------------------------------------------

    /**
     * Persist a full market snapshot from EDDN.
     * Pattern: DELETE existing rows for this market, then bulk INSERT the new snapshot.
     * If the INSERT fails the DELETE is effectively lost - the market will be empty
     * until the next snapshot arrives, which is acceptable given the 3-hour window.
     */
    public void save(EddnDto data, Long systemAddress) {
        if (data == null || systemAddress == null) return;

        Long marketId = data.getMarketId();
        if (marketId == null) return;

        List<EDDN_CommodityItemDto> commodities = data.getCommodities();
        if (commodities == null || commodities.isEmpty()) return;

        // Build the batch - resolve/create commodity type ids before opening the lock
        List<CommodityDao.CommodityRow> batch = new ArrayList<>(commodities.size());
        for (EDDN_CommodityItemDto item : commodities) {
            if (item == null || item.getName() == null) continue;
            short commodityId = resolveCommodityId(item.getName());
            batch.add(toRow(item, marketId, systemAddress, commodityId));
        }

        if (batch.isEmpty()) return;

        final long mid = marketId;
        Object lock = marketLocks.computeIfAbsent(mid, k -> new Object());
        synchronized (lock) {
            saveWithRetry(mid, batch);
        }
    }

    /**
     * Executes the commodity delete/insert/markDirty sequence with deadlock retry.
     * Deadlocks (SQLState 40001) are expected when the trade pair procedure and ingest
     * both touch the same station rows concurrently. MariaDB kills the younger
     * transaction - we simply retry it up to 3 times with brief backoff.
     */
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
                Database.withDao(StationsDao.class, dao -> {
                    dao.markDirty(mid);
                    return null;
                });
                return; // success
            } catch (Exception e) {
                String sqlState = extractSqlState(e);
                if ("40001".equals(sqlState) && attempts < 3) {
                    attempts++;
                    log.debug("Deadlock on market {} - retry {}/3", mid, attempts);
                    try {
                        Thread.sleep(50L * attempts); // 50ms, 100ms, 150ms
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    throw e; // not a deadlock or retries exhausted - let it propagate
                }
            }
        }
    }

    /**
     * Walks the exception chain looking for an SQLException with a SQLState.
     * JDBI wraps the underlying SQLException in UnableToExecuteStatementException.
     */
    private String extractSqlState(Throwable t) {
        while (t != null) {
            if (t instanceof java.sql.SQLException) {
                return ((java.sql.SQLException) t).getSQLState();
            }
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
                    .findFirst()
                    .orElse(null);
        }
        if (commodityId == null) return Collections.emptyList();

        final short cid = commodityId;
        final SystemDao.StarSystem origin = Database.withDao(SystemDao.class, dao -> dao.findByName(startingSystem));
        if (origin == null) return Collections.emptyList();

        return Database.withDao(CommodityDao.class, dao -> {
            List<CommodityOfferProjection> rows = dao.findBestCommodityOffers(cid, maxDistance, origin.getX(), origin.getY(), origin.getZ());

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
                dto.setStarName(r.getStarName());
                result.add(dto);
            }
            return result;
        });
    }


    // -------------------------------------------------------------------------
    // Query: trade route  (reads from pre-calculated trade_pair table)
    // -------------------------------------------------------------------------

    public API_TradeRouteDto calculateTradeRoute(String startingSystem,
                                                 int numTrades,
                                                 int jumpRange,
                                                 double maxDistanceFromEntrance,
                                                 boolean requireLargePad,
                                                 boolean requireMediumPad,
                                                 boolean allowPlanetary) {
        return calculateTradeRoute(startingSystem, numTrades, jumpRange,
                maxDistanceFromEntrance, requireLargePad, requireMediumPad,
                allowPlanetary, DEFAULT_CARGO_CAP);
    }

    public API_TradeRouteDto calculateTradeRoute(String startingSystem,
                                                 int numTrades,
                                                 int jumpRange,
                                                 double maxDistanceFromEntrance,
                                                 boolean requireLargePad,
                                                 boolean requireMediumPad,
                                                 boolean allowPlanetary,
                                                 int cargoCapacity) {
        long startTime = System.currentTimeMillis();

        SystemDao.StarSystem origin = Database.withDao(SystemDao.class,
                dao -> dao.findByName(startingSystem));
        if (origin == null) {
            log.warn("Starting system not found in DB: {}", startingSystem);
            return null;
        }

        // Pre-quoted IN-clause for planetary type filter
        String planetaryTypesInClause = CommodityDao.PLANETARY_STATION_TYPES.stream()
                .map(t -> "'" + t.replace("'", "''") + "'")
                .collect(java.util.stream.Collectors.joining(","));

        // Data age - read once before the loop, add to response
        LocalDateTime lastCalcAt = Database.withDao(TradePairDao.class,
                TradePairDao::getLastCalculatedAt);

        API_TradeRouteDto route = new API_TradeRouteDto();
        Map<Integer, API_TradePairDto> legs = new LinkedHashMap<>();

        double curX = origin.getX();
        double curY = origin.getY();
        double curZ = origin.getZ();

        Set<String> usedTrades = new HashSet<>();

        for (int hop = 1; hop <= numTrades; hop++) {
            final double refX = curX;
            final double refY = curY;
            final double refZ = curZ;

            // Single indexed scan against trade_pair - no heavy joins
            final String ptClause = planetaryTypesInClause;
            List<TradePairDao.TradePairRow> candidates = Database.withHandle(handle ->
                    handle.createQuery(FIND_BEST_PAIRS_SQL)
                            .bind("refX", refX)
                            .bind("refY", refY)
                            .bind("refZ", refZ)
                            .bind("searchRadius", (double) jumpRange)
                            .bind("maxDistFromEntrance", maxDistanceFromEntrance)
                            .bind("requireLargePad", requireLargePad)
                            .bind("requireMediumPad", requireMediumPad)
                            .bind("allowPlanetary", allowPlanetary)
                            .bind("cargoCap", cargoCapacity)
                            .bind("limit", 50)
                            .define("planetaryTypes", ptClause)
                            .mapToBean(TradePairDao.TradePairRow.class)
                            .list());

            if (candidates.isEmpty()) {
                log.debug("Hop {}: no trade pairs found near ({}, {}, {})", hop, refX, refY, refZ);
                break;
            }

            // Pick the best candidate not already used in this route
            TradePairDao.TradePairRow best = null;
            for (TradePairDao.TradePairRow row : candidates) {
                // Dedup on sell station - avoid visiting the same destination twice.
                // Also skip if this sell station was a buy station earlier (would fly empty back).
                String sellKey = String.valueOf(row.getSellMarketId());
                String buyKey = String.valueOf(row.getBuyMarketId());
                if (!usedTrades.contains(sellKey) && !usedTrades.contains("buy:" + buyKey + "|sell:" + sellKey)) {
                    best = row;
                    usedTrades.add(sellKey);
                    usedTrades.add("buy:" + buyKey + "|sell:" + sellKey);
                    break;
                }
            }

            if (best == null) {
                log.debug("Hop {}: all candidates already used", hop);
                break;
            }

            int effectiveUnits = Math.min(
                    Math.min(best.getBuyStock(), best.getSellDemand()),
                    cargoCapacity);

            API_TradePairDto dto = new API_TradePairDto();
            dto.setSourceSystem(best.getBuySystem());
            dto.setSourceStation(best.getBuyStation());
            dto.setDestinationSystem(best.getSellSystem());
            dto.setDestinationStation(best.getSellStation());
            dto.setSourceMarketId(best.getBuyMarketId());
            dto.setDestinationMarketId(best.getSellMarketId());
            dto.setCommodity(best.getCommodityName());
            dto.setBuyPrice(best.getBuyPrice());
            dto.setSellPrice(best.getSellPrice());
            dto.setProfitPerUnit(best.getProfitPerUnit());
            dto.setStock(best.getBuyStock());
            dto.setDemand(best.getSellDemand());
            dto.setDistanceLy(best.getDistanceLy());
            dto.setEstimatedRunProfit((long) best.getProfitPerUnit() * effectiveUnits);

            legs.put(hop, dto);

            // Advance to the sell system for the next hop.
            // Coords come directly from the trade_pair row - no DB lookup needed.
            curX = best.getSellX();
            curY = best.getSellY();
            curZ = best.getSellZ();

            if (curX == 0 && curY == 0 && curZ == 0) {
                // Sell coords missing from trade_pair row - fall back to DB lookup
                final String sellSysName = best.getSellSystem();
                SystemDao.StarSystem sellSys = Database.withDao(SystemDao.class,
                        dao -> dao.findByName(sellSysName));
                if (sellSys == null) {
                    log.warn("Hop {}: sell system '{}' not in DB, stopping chain", hop, sellSysName);
                    break;
                }
                curX = sellSys.getX();
                curY = sellSys.getY();
                curZ = sellSys.getZ();
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;

        String dataAgeNote;
        if (lastCalcAt == null) {
            dataAgeNote = "Trade data: not yet calculated";
        } else {
            long minutesAgo = java.time.Duration.between(lastCalcAt, java.time.LocalDateTime.now()).toMinutes();
            if (minutesAgo < 60) {
                dataAgeNote = "Trade data last updated " + minutesAgo + " minute(s) ago";
            } else {
                dataAgeNote = "Trade data last updated " + (minutesAgo / 60) + " hour(s) ago";
            }
        }

        route.setLastCalculatedAt(lastCalcAt);
        route.setNote(String.format("Calculated %d leg(s) in %dms. %s.", legs.size(), elapsed, dataAgeNote));
        route.setRoute(legs);
        return route;
    }
}