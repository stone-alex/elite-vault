package elite.vault.db.dao;

import elite.vault.db.projections.CommodityOfferProjection;
import elite.vault.db.projections.TradePairProjection;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface CommodityDao {

    // -------------------------------------------------------------------------
    // Commodity type cache support
    // -------------------------------------------------------------------------

    /**
     * Load all known commodity types into a name→id map on startup.
     * Returns empty map on a blank DB — that is normal.
     */
    @SqlQuery("SELECT name, id FROM commodity_type")
    @RegisterBeanMapper(CommodityTypeRow.class)
    List<CommodityTypeRow> loadAllCommodityTypes();

    /**
     * Insert a new commodity type name if it does not already exist.
     * INSERT IGNORE means no error on race/duplicate.
     */
    @SqlUpdate("INSERT IGNORE INTO commodity_type (name) VALUES (:name)")
    void insertCommodityTypeIfAbsent(@Bind("name") String name);

    /**
     * Fetch the id for a commodity name. Called once after insertIfAbsent on a cache miss.
     */
    @SqlQuery("SELECT id FROM commodity_type WHERE name = :name")
    Short findCommodityTypeId(@Bind("name") String name);


    // -------------------------------------------------------------------------
    // Snapshot replace  (called inside Database.withTransaction)
    // -------------------------------------------------------------------------

    /**
     * Delete all commodity rows for a market. Step 1 of the snapshot replace.
     * Hits idx_c_market — single partition scan.
     */
    @SqlUpdate("DELETE FROM commodity WHERE marketId = :marketId")
    void deleteByMarket(@Bind("marketId") long marketId);

    /**
     * Bulk insert the new commodity snapshot. Step 2 of the snapshot replace.
     * rewriteBatchedStatements=true in the JDBC URL turns this into a single
     * multi-row INSERT on the wire.
     */
    @SqlBatch("""
            INSERT INTO commodity (marketId, commodityId, systemAddress, buyPrice, sellPrice, stock, demand, received_at)
            VALUES (:marketId, :commodityId, :systemAddress, :buyPrice, :sellPrice, :stock, :demand, UNIX_TIMESTAMP())
            """)
    void insertBatch(@BindBean List<CommodityRow> rows);


    // -------------------------------------------------------------------------
    // Trade queries
    // -------------------------------------------------------------------------

    /**
     * Find the best places to sell a specific commodity within a radius.
     * Spatial filter on star_system.pos; commodity resolved to id by caller.
     */
    @RegisterBeanMapper(CommodityOfferProjection.class)
    @SqlQuery("""
            SELECT
                ss.starName                                              AS starName,
                st.realName                                              AS stationName,
                ct.name                                                  AS commodity,
                c.sellPrice                                              AS sellPrice,
                c.stock                                                  AS stock,
                ROUND(ST_Distance(POINT(:refX, :refY), ss.pos), 1)      AS distanceLy,
                c.marketId                                               AS marketId,
                c.systemAddress                                          AS systemAddress
            FROM commodity c
            INNER JOIN commodity_type ct ON ct.id          = c.commodityId
            INNER JOIN star_system     ss ON ss.systemAddress = c.systemAddress
            INNER JOIN stations        st ON st.marketId    = c.marketId
            WHERE c.commodityId = :commodityId
              AND c.stock       > 0
              AND c.sellPrice   > 0
              AND MBRContains(ST_Buffer(POINT(:refX, :refY), :maxLy), ss.pos)
              AND ST_Distance(POINT(:refX, :refY), ss.pos) <= :maxLy
            ORDER BY c.sellPrice DESC, c.stock DESC
            LIMIT 20
            """)
    List<CommodityOfferProjection> findBestCommodityOffers(
            @Bind("commodityId") short commodityId,
            @Bind("maxLy") double maxLy,
            @Bind("refX") double refX,
            @Bind("refY") double refY
    );

    /**
     * Find the best buy offers near a reference point.
     * Used for the first hop of a trade route, or when arriving at a new system.
     * <p>
     * Returns one row per commodity (best buy price), filtered by:
     * - station distance from system entry point
     * - jump range radius from reference coords
     * - optional landing pad size
     * - optional planetary landing filter
     */
    @RegisterBeanMapper(TradePairProjection.class)
    @SqlQuery("""
            SELECT
                ss.starName                                              AS buySystem,
                st.realName                                              AS buyStation,
                ct.name                                                  AS commodity,
                c.buyPrice                                               AS buyPrice,
                c.stock                                                  AS buyStock,
                c.marketId                                               AS buyMarketId,
                c.systemAddress                                          AS buySystemAddress,
                ss.x                                                     AS buyX,
                ss.y                                                     AS buyY,
                st.distanceToArrival                                     AS buyDistToArrival,
                ROUND(ST_Distance(POINT(:refX, :refY), ss.pos), 1)      AS buyDistanceLy
            FROM (
                SELECT c.*,
                       ROW_NUMBER() OVER (PARTITION BY c.commodityId ORDER BY c.buyPrice ASC, c.stock DESC) AS rn
                FROM commodity c
                INNER JOIN stations st ON st.marketId = c.marketId
                WHERE c.buyPrice           > 0
                  AND c.stock              > 0
                  AND st.distanceToArrival <= :maxDistFromEntrance
                  AND (:requireLargePad  = FALSE OR st.hasLargePad  = TRUE)
                  AND (:requireMediumPad = FALSE OR st.hasMediumPad = TRUE)
                  AND (:allowPlanetary   = TRUE  OR st.stationType NOT IN ('SurfaceStation','OnFootSettlement','CraterOutpost','CraterPort'))
            ) c
            INNER JOIN commodity_type ct ON ct.id             = c.commodityId
            INNER JOIN star_system     ss ON ss.systemAddress  = c.systemAddress
            INNER JOIN stations        st ON st.marketId       = c.marketId
            WHERE c.rn = 1
              AND MBRContains(ST_Buffer(POINT(:refX, :refY), :jumpRange), ss.pos)
              AND ST_Distance(POINT(:refX, :refY), ss.pos) <= :jumpRange
            ORDER BY c.buyPrice ASC
            LIMIT 10
            """)
    List<TradePairProjection> findBuyOffers(
            @Bind("jumpRange") double jumpRange,
            @Bind("refX") double refX,
            @Bind("refY") double refY,
            @Bind("maxDistFromEntrance") double maxDistFromEntrance,
            @Bind("requireLargePad") boolean requireLargePad,
            @Bind("requireMediumPad") boolean requireMediumPad,
            @Bind("allowPlanetary") boolean allowPlanetary
    );

    /**
     * Find the best single sell destination for a specific commodity,
     * reachable within jumpRange of the given reference coords.
     * Excludes the station where it was bought.
     */
    @RegisterBeanMapper(TradePairProjection.class)
    @SqlQuery("""
            SELECT
                ss.starName                                              AS sellSystem,
                st.realName                                              AS sellStation,
                ct.name                                                  AS commodity,
                c.sellPrice                                              AS sellPrice,
                c.demand                                                 AS sellDemand,
                c.marketId                                               AS sellMarketId,
                c.systemAddress                                          AS sellSystemAddress,
                st.distanceToArrival                                     AS sellDistToArrival,
                ROUND(ST_Distance(POINT(:refX, :refY), ss.pos), 1)      AS legDistanceLy
            FROM commodity c
            INNER JOIN commodity_type ct ON ct.id             = c.commodityId
            INNER JOIN star_system     ss ON ss.systemAddress  = c.systemAddress
            INNER JOIN stations        st ON st.marketId       = c.marketId
            WHERE c.commodityId         = :commodityId
              AND c.sellPrice           > :minSellPrice
              AND c.demand              > 0
              AND c.marketId           != :excludeMarketId
              AND st.distanceToArrival  <= :maxDistFromEntrance
              AND (:requireLargePad  = FALSE OR st.hasLargePad  = TRUE)
              AND (:requireMediumPad = FALSE OR st.hasMediumPad = TRUE)
              AND (:allowPlanetary   = TRUE  OR st.stationType NOT IN ('SurfaceStation', 'OnFootSettlement', 'CraterOutpost', 'CraterPort'))
              AND MBRContains(ST_Buffer(POINT(:refX, :refY), :jumpRange), ss.pos)
              AND ST_Distance(POINT(:refX, :refY), ss.pos) <= :jumpRange
            ORDER BY c.sellPrice DESC
            LIMIT 1
            """)
    List<TradePairProjection> findBestSellFor(
            @Bind("commodityId") short commodityId,
            @Bind("minSellPrice") int minSellPrice,
            @Bind("excludeMarketId") long excludeMarketId,
            @Bind("jumpRange") double jumpRange,
            @Bind("refX") double refX,
            @Bind("refY") double refY,
            @Bind("maxDistFromEntrance") double maxDistFromEntrance,
            @Bind("requireLargePad") boolean requireLargePad,
            @Bind("requireMediumPad") boolean requireMediumPad,
            @Bind("allowPlanetary") boolean allowPlanetary
    );

    /**
     * Find all buy offers at a specific station (used for subsequent hops —
     * the pilot is already docked, so no spatial filter needed).
     */
    @RegisterBeanMapper(TradePairProjection.class)
    @SqlQuery("""
            SELECT
                ss.starName          AS buySystem,
                st.realName          AS buyStation,
                ct.name              AS commodity,
                c.buyPrice           AS buyPrice,
                c.stock              AS buyStock,
                c.marketId           AS buyMarketId,
                c.systemAddress      AS buySystemAddress,
                ss.x                 AS buyX,
                ss.y                 AS buyY,
                st.distanceToArrival AS buyDistToArrival,
                0.0                  AS buyDistanceLy
            FROM commodity c
            INNER JOIN commodity_type ct ON ct.id             = c.commodityId
            INNER JOIN star_system     ss ON ss.systemAddress  = c.systemAddress
            INNER JOIN stations        st ON st.marketId       = c.marketId
            WHERE c.marketId  = :marketId
              AND c.buyPrice  > 0
              AND c.stock     > 0
            ORDER BY c.buyPrice ASC
            LIMIT 10
            """)
    List<TradePairProjection> findBuyOffersAtStation(@Bind("marketId") long marketId);


    // -------------------------------------------------------------------------
    // Supporting types
    // -------------------------------------------------------------------------

    /**
     * Used for loading the commodity_type cache on startup.
     */
    class CommodityTypeRow {
        private String name;
        private short id;

        public String getName() {
            return name;
        }

        public void setName(String n) {
            this.name = n;
        }

        public short getId() {
            return id;
        }

        public void setId(short id) {
            this.id = id;
        }
    }

    /**
     * One row to be inserted into the commodity table.
     * Prices/stock/demand use int — Elite credits are whole numbers,
     * and INT UNSIGNED in the schema covers the full range.
     */
    class CommodityRow {
        private long marketId;
        private short commodityId;
        private long systemAddress;
        private int buyPrice;
        private int sellPrice;
        private int stock;
        private int demand;

        public long getMarketId() {
            return marketId;
        }

        public void setMarketId(long v) {
            this.marketId = v;
        }

        public short getCommodityId() {
            return commodityId;
        }

        public void setCommodityId(short v) {
            this.commodityId = v;
        }

        public long getSystemAddress() {
            return systemAddress;
        }

        public void setSystemAddress(long v) {
            this.systemAddress = v;
        }

        public int getBuyPrice() {
            return buyPrice;
        }

        public void setBuyPrice(int v) {
            this.buyPrice = v;
        }

        public int getSellPrice() {
            return sellPrice;
        }

        public void setSellPrice(int v) {
            this.sellPrice = v;
        }

        public int getStock() {
            return stock;
        }

        public void setStock(int v) {
            this.stock = v;
        }

        public int getDemand() {
            return demand;
        }

        public void setDemand(int v) {
            this.demand = v;
        }
    }
}