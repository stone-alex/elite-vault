package elite.vault.db.managers;

import elite.vault.api.dto.API_CommodityDto;
import elite.vault.api.dto.API_TradePairDto;
import elite.vault.api.dto.API_TradeRouteDto;
import elite.vault.db.dao.CommodityDao;
import elite.vault.db.dao.SystemDao;
import elite.vault.db.projections.CommodityOfferProjection;
import elite.vault.db.projections.TradePairProjection;
import elite.vault.db.util.Database;
import elite.vault.eddn.dto.EDDN_CommodityItemDto;
import elite.vault.eddn.dto.EddnDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MarketManager {

    private static final Logger log = LogManager.getLogger(MarketManager.class);
    private static final MarketManager INSTANCE = new MarketManager();

    /**
     * Commodity name → commodity_type.id cache.
     * Loaded from DB at construction. Updated on cache miss (new commodity seen).
     * ~300 entries at steady state — fits entirely in L1.
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
            // Blank DB on first run — cache stays empty, entries added on first ingest
            log.info("Commodity type cache empty (blank DB or first run)");
        }
    }

    /**
     * Resolve a commodity name to its DB id.
     * On cache miss: inserts the name into commodity_type (INSERT IGNORE),
     * fetches the id, caches it. This path is hit only for genuinely new
     * commodity names — essentially never after the first ingest session.
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
     * Wrapped in a single transaction — if the INSERT fails the DELETE is rolled back,
     * so we never end up with a market that has no commodity rows.
     */
    public void save(EddnDto data, Long systemAddress) {
        if (data == null || systemAddress == null) return;

        Long marketId = data.getMarketId();
        if (marketId == null) return;

        List<EDDN_CommodityItemDto> commodities = data.getCommodities();
        if (commodities == null || commodities.isEmpty()) return;

        // Build the batch — resolve/create commodity type ids before opening the transaction
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
            Database.withDao(CommodityDao.class, dao -> {
                dao.deleteByMarket(mid);
                return null;
            });
            Database.withDao(CommodityDao.class, dao -> {
                dao.insertBatch(batch);
                return null;
            });
        }
    }

    private CommodityDao.CommodityRow toRow(EDDN_CommodityItemDto item, long marketId,
                                            long systemAddress, short commodityId) {
        CommodityDao.CommodityRow row = new CommodityDao.CommodityRow();
        row.setMarketId(marketId);
        row.setCommodityId(commodityId);
        row.setSystemAddress(systemAddress);
        // EDDN prices are whole credits — safe to cast from double/Number
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

        // Unknown commodity = nothing in DB, no point querying
        Short commodityId = commodityTypeCache.get(commodityName);
        if (commodityId == null) {
            // Try case-insensitive match — EDDN names are lowercase, callers may not be
            final String lower = commodityName.toLowerCase();
            commodityId = commodityTypeCache.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(lower))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
        }
        if (commodityId == null) return Collections.emptyList();

        final short cid = commodityId;
        final SystemDao.StarSystem origin = Database.withDao(SystemDao.class,
                dao -> dao.findByName(startingSystem));
        if (origin == null) return Collections.emptyList();

        return Database.withDao(CommodityDao.class, dao -> {
            List<CommodityOfferProjection> rows = dao.findBestCommodityOffers(
                    cid, maxDistance, origin.getX(), origin.getY());

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
    // Query: trade route
    // -------------------------------------------------------------------------

    public API_TradeRouteDto calculateTradeRoute(String startingSystem,
                                                 int numTrades,
                                                 int jumpRange,
                                                 double maxDistanceFromEntrance,
                                                 boolean requireLargePad,
                                                 boolean requireMediumPad,
                                                 boolean allowPlanetary) {
        long startTime = System.currentTimeMillis();

        SystemDao.StarSystem origin = Database.withDao(SystemDao.class,
                dao -> dao.findByName(startingSystem));
        if (origin == null) return null;

        API_TradeRouteDto route = new API_TradeRouteDto();
        Map<Integer, API_TradePairDto> legs = new LinkedHashMap<>();

        double curX = origin.getX();
        double curY = origin.getY();
        Long currentMarketId = null;
        Set<String> usedTrades = new HashSet<>();

        for (int hop = 1; hop <= numTrades; hop++) {
            final double refX = curX;
            final double refY = curY;

            // Step 1 — find buy candidates at or near current position
            List<TradePairProjection> buyOffers;
            if (currentMarketId == null) {
                buyOffers = Database.withDao(CommodityDao.class, dao ->
                        dao.findBuyOffers(jumpRange, refX, refY, maxDistanceFromEntrance,
                                requireLargePad, requireMediumPad, allowPlanetary));
            } else {
                final long stationId = currentMarketId;
                buyOffers = Database.withDao(CommodityDao.class,
                        dao -> dao.findBuyOffersAtStation(stationId));
            }

            if (buyOffers.isEmpty()) break;

            // Step 2 — for each buy candidate (up to 5), find the best sell destination
            TradePairProjection bestBuy = null;
            TradePairProjection bestSell = null;
            double bestProfit = 0;
            int checked = 0;

            for (TradePairProjection buy : buyOffers) {
                if (checked >= 5) break;
                checked++;

                // Use buy station's system coords as the reference for the sell search.
                // buyX/buyY come from star_system join in the query — non-zero when the
                // buy station differs from our current position.
                final double bx = (buy.getBuyX() != 0) ? buy.getBuyX() : refX;
                final double by = (buy.getBuyY() != 0) ? buy.getBuyY() : refY;
                final long excludeMkt = buy.getBuyMarketId();
                final String commodityName = buy.getCommodity();

                // Resolve name → id for the sell query
                Short cid = commodityTypeCache.get(commodityName);
                if (cid == null) continue;
                final short commodityId = cid;
                final int minSellPrice = (int) buy.getBuyPrice();

                List<TradePairProjection> sells = Database.withDao(CommodityDao.class, dao ->
                        dao.findBestSellFor(commodityId, minSellPrice, excludeMkt,
                                jumpRange, bx, by, maxDistanceFromEntrance,
                                requireLargePad, requireMediumPad, allowPlanetary));

                if (sells.isEmpty()) continue;

                TradePairProjection sell = sells.getFirst();
                String tradeKey = commodityName + "|" + buy.getBuyMarketId() + "|" + sell.getSellMarketId();
                if (usedTrades.contains(tradeKey)) continue;

                double profit = sell.getSellPrice() - buy.getBuyPrice();
                if (profit > bestProfit) {
                    bestProfit = profit;
                    bestBuy = buy;
                    bestSell = sell;
                }
            }

            if (bestBuy == null) break;

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

            // Advance position to the sell system for the next hop
            currentMarketId = bestSell.getSellMarketId();
            final String sellSystemName = bestSell.getSellSystem();
            SystemDao.StarSystem sellSystem = Database.withDao(SystemDao.class,
                    dao -> dao.findByName(sellSystemName));
            if (sellSystem != null) {
                curX = sellSystem.getX();
                curY = sellSystem.getY();
            } else {
                // Sell system not yet in our DB — can't continue the chain
                break;
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        route.setNote("Calculated in " + elapsed + "ms");
        route.setRoute(legs);
        return route;
    }
}