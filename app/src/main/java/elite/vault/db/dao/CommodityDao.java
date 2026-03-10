package elite.vault.db.dao;

import elite.vault.db.projections.CommodityOfferProjection;
import elite.vault.db.projections.HopPairProjection;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Set;

public interface CommodityDao {

    // -------------------------------------------------------------------------
    // Planetary station type classification
    // -------------------------------------------------------------------------

    Set<String> PLANETARY_STATION_TYPES = Set.of(
            "SurfaceStation",
            "OnFootSettlement",
            "CraterOutpost",
            "CraterPort",
            "Settlement",
            "Planetary Outpost",
            "PlanetaryConstructionDepot",
            "Planetary Construction Depot",
            "Planetary Port"
    );


    // -------------------------------------------------------------------------
    // Commodity type cache support
    // -------------------------------------------------------------------------

    @SqlQuery("SELECT name, id FROM commodity_type")
    @RegisterBeanMapper(CommodityTypeRow.class)
    List<CommodityTypeRow> loadAllCommodityTypes();

    @SqlUpdate("INSERT IGNORE INTO commodity_type (name) VALUES (:name)")
    void insertCommodityTypeIfAbsent(@Bind("name") String name);

    @SqlQuery("SELECT id FROM commodity_type WHERE name = :name")
    Short findCommodityTypeId(@Bind("name") String name);


    // -------------------------------------------------------------------------
    // Snapshot deduplication gate
    //
    // Call checkAndUpdateHash before every DELETE+INSERT cycle.
    // Returns 1 if the hash was new/changed (proceed with replace).
    // Returns 0 if the hash matches the stored value (skip replace entirely).
    //
    // Uses INSERT ... ON DUPLICATE KEY UPDATE so the check and update are
    // atomic — no race between the SELECT and the UPDATE under concurrent
    // ingest from multiple commanders at the same station.
    //
    // The hash is computed in Java (CommodityHasher) as a lightweight XOR
    // across all (commodityId, buyPrice, sellPrice) tuples in the snapshot.
    // Not cryptographic — change detection only.
    // -------------------------------------------------------------------------

    @SqlQuery("""
            SELECT COUNT(*) FROM market_last_seen
            WHERE marketId = :marketId AND last_hash = :hash
            """)
    int isHashUnchanged(@Bind("marketId") long marketId, @Bind("hash") long hash);

    @SqlUpdate("""
            INSERT INTO market_last_seen (marketId, last_hash, last_updated)
            VALUES (:marketId, :hash, UNIX_TIMESTAMP())
            ON DUPLICATE KEY UPDATE
                last_hash    = VALUES(last_hash),
                last_updated = VALUES(last_updated)
            """)
    void upsertHash(@Bind("marketId") long marketId, @Bind("hash") long hash);


    // -------------------------------------------------------------------------
    // Snapshot replace
    // -------------------------------------------------------------------------

    @SqlUpdate("DELETE FROM commodity WHERE marketId = :marketId")
    void deleteByMarket(@Bind("marketId") long marketId);

    @SqlBatch("""
            INSERT INTO commodity (marketId, commodityId, systemAddress, buyPrice, sellPrice, stock, demand, received_at)
            VALUES (:marketId, :commodityId, :systemAddress, :buyPrice, :sellPrice, :stock, :demand, UNIX_TIMESTAMP())
            """)
    void insertBatch(@BindBean List<CommodityRow> rows);


    // -------------------------------------------------------------------------
    // Primary route hop query — single query per hop (MySQL 8 window functions)
    //
    // Replaces the old findBuyCandidates + N×findBestSell pattern (up to 21
    // queries per hop) with a single self-join query.
    //
    // Algorithm:
    //   buy_side CTE  — stations within hopDistance of current ref position
    //                   that have stock of a commodity for sale. One row per
    //                   (station, commodity) with buyPrice > 0.
    //   sell_side CTE — stations within hopDistance*2 of ref position
    //                   (geometrically: if buy ≤ D from ref, sell ≤ D from buy
    //                   means sell ≤ 2D from ref) that have demand > 0.
    //                   The sell box is centered on ref, not on each buy station,
    //                   which is what makes a single static query possible.
    //   JOIN          — match buy and sell on commodityId, exclude same market,
    //                   require sellPrice > buyPrice (profitable).
    //   ORDER BY      — profitPerUnit * LEAST(stock, demand, cargoCap) DESC
    //                   ranks by total run value, not just margin per unit.
    //   LIMIT         — returns top :limit pairs; Java picks the best unused one
    //                   and advances position to the sell station for the next hop.
    //
    // Pad and planetary filters apply to both sides independently.
    // Fleet carriers are excluded on both sides via stationType != 'FleetCarrier'.
    // -------------------------------------------------------------------------

    @SqlQuery("""
            WITH buy_side AS (
                SELECT
                    c.commodityId,
                    ct.name                                                                                 AS commodityName,
                    c.marketId                                                                              AS buyMarketId,
                    c.systemAddress                                                                         AS buySystemAddress,
                    st.realName                                                                             AS buyStation,
                    c.buyPrice,
                    c.stock                                                                                 AS buyStock,
                    st.x                                                                                    AS buyX,
                    st.y                                                                                    AS buyY,
                    st.z                                                                                    AS buyZ,
                    st.hasLargePad                                                                          AS buyHasLargePad,
                    st.hasMediumPad                                                                         AS buyHasMediumPad,
                    st.stationType                                                                          AS buyStationType,
                    st.distanceToArrival                                                                    AS buyDistToArrival,
                    ROUND(SQRT(POW(st.x - :refX, 2) + POW(st.y - :refY, 2) + POW(st.z - :refZ, 2)), 2)   AS distanceFromRef
                FROM stations st
                INNER JOIN commodity      c  ON c.marketId  = st.marketId
                                            AND c.buyPrice  > 0
                                            AND c.stock    >= :minStock
                INNER JOIN commodity_type ct ON ct.id       = c.commodityId
                WHERE
                    st.x BETWEEN :refX - :hopDistance AND :refX + :hopDistance
                    AND st.y BETWEEN :refY - :hopDistance AND :refY + :hopDistance
                    AND st.z BETWEEN :refZ - :hopDistance AND :refZ + :hopDistance
                    AND POW(st.x - :refX, 2) + POW(st.y - :refY, 2) + POW(st.z - :refZ, 2)
                        <= POW(:hopDistance, 2)
                    AND st.distanceToArrival <= :maxDistToArrival
                    AND (
                        :requireLargePad  = FALSE OR st.hasLargePad = TRUE
                    )
                    AND (
                        :requireMediumPad = FALSE OR st.hasLargePad = TRUE OR st.hasMediumPad = TRUE
                    )
                    AND (
                        :allowPlanetary   = TRUE  OR st.stationType NOT IN (<planetaryTypes>)
                    )
                    AND st.stationType != 'FleetCarrier'
            ),
            sell_side AS (
                SELECT
                    c.commodityId,
                    c.marketId                                                                              AS sellMarketId,
                    c.systemAddress                                                                         AS sellSystemAddress,
                    st.realName                                                                             AS sellStation,
                    c.sellPrice,
                    c.demand                                                                                AS sellDemand,
                    st.x                                                                                    AS sellX,
                    st.y                                                                                    AS sellY,
                    st.z                                                                                    AS sellZ,
                    st.hasLargePad                                                                          AS sellHasLargePad,
                    st.hasMediumPad                                                                         AS sellHasMediumPad,
                    st.stationType                                                                          AS sellStationType,
                    st.distanceToArrival                                                                    AS sellDistToArrival
                FROM stations st
                INNER JOIN commodity c ON c.marketId  = st.marketId
                                      AND c.sellPrice > 0
                                      AND c.demand   >= :minDemand
                WHERE
                    -- Sell box is hopDistance*2 from ref.
                    -- Rationale: buy station is at most hopDistance from ref,
                    -- sell station is at most hopDistance from buy station,
                    -- therefore sell station is at most hopDistance*2 from ref.
                    st.x BETWEEN :refX - :hopDistance * 2 AND :refX + :hopDistance * 2
                    AND st.y BETWEEN :refY - :hopDistance * 2 AND :refY + :hopDistance * 2
                    AND st.z BETWEEN :refZ - :hopDistance * 2 AND :refZ + :hopDistance * 2
                    AND st.distanceToArrival <= :maxDistToArrival
                    AND (
                        :requireLargePad  = FALSE OR st.hasLargePad = TRUE
                    )
                    AND (
                        :requireMediumPad = FALSE OR st.hasLargePad = TRUE OR st.hasMediumPad = TRUE
                    )
                    AND (
                        :allowPlanetary   = TRUE  OR st.stationType NOT IN (<planetaryTypes>)
                    )
                    AND st.stationType != 'FleetCarrier'
            )
            SELECT
                b.commodityId,
                b.commodityName,
                b.buyMarketId,
                b.buySystemAddress,
                b.buyStation,
                b.buyPrice,
                b.buyStock,
                b.buyX,
                b.buyY,
                b.buyZ,
                b.buyHasLargePad,
                b.buyHasMediumPad,
                b.buyStationType,
                b.buyDistToArrival,
                b.distanceFromRef,
                s.sellMarketId,
                s.sellSystemAddress,
                s.sellStation,
                s.sellPrice,
                s.sellDemand,
                s.sellX,
                s.sellY,
                s.sellZ,
                s.sellHasLargePad,
                s.sellHasMediumPad,
                s.sellStationType,
                s.sellDistToArrival,
                (s.sellPrice - b.buyPrice)                                                                 AS profitPerUnit,
                ROUND(SQRT(POW(s.sellX - b.buyX, 2) + POW(s.sellY - b.buyY, 2) + POW(s.sellZ - b.buyZ, 2)), 2) AS distanceBuyToSell,
                (s.sellPrice - b.buyPrice) * LEAST(b.buyStock, s.sellDemand, :cargoCap)                   AS runValue
            FROM buy_side  b
            INNER JOIN sell_side s ON s.commodityId  = b.commodityId
                                  AND s.sellMarketId != b.buyMarketId
                                  AND s.sellPrice     > b.buyPrice
            ORDER BY runValue DESC
            LIMIT :limit
            """)
    @RegisterBeanMapper(HopPairProjection.class)
    List<HopPairProjection> findBestHopPairs(
            @Bind("refX") double refX,
            @Bind("refY") double refY,
            @Bind("refZ") double refZ,
            @Bind("hopDistance") double hopDistance,
            @Bind("minStock") int minStock,
            @Bind("minDemand") int minDemand,
            @Bind("cargoCap") int cargoCap,
            @Bind("maxDistToArrival") double maxDistToArrival,
            @Bind("requireLargePad") boolean requireLargePad,
            @Bind("requireMediumPad") boolean requireMediumPad,
            @Bind("allowPlanetary") boolean allowPlanetary,
            @Define("planetaryTypes") String planetaryTypes,
            @Bind("limit") int limit
    );


    // -------------------------------------------------------------------------
    // Commodity search (used by CommoditiesService)
    // -------------------------------------------------------------------------

    @SqlQuery("""
            SELECT
                st.realName                                                                              AS stationName,
                ct.name                                                                                  AS commodity,
                c.sellPrice                                                                              AS sellPrice,
                c.stock                                                                                  AS stock,
                ROUND(SQRT(POW(st.x - :refX, 2) + POW(st.y - :refY, 2) + POW(st.z - :refZ, 2)), 1)    AS distanceLy,
                c.marketId                                                                               AS marketId,
                c.systemAddress                                                                          AS systemAddress
            FROM stations st
            INNER JOIN commodity      c  ON c.marketId    = st.marketId
                                        AND c.commodityId = :commodityId
                                        AND c.stock       > 0
                                        AND c.sellPrice   > 0
            INNER JOIN commodity_type ct ON ct.id         = c.commodityId
            WHERE
                st.x BETWEEN :refX - :maxLy AND :refX + :maxLy
                AND st.y BETWEEN :refY - :maxLy AND :refY + :maxLy
                AND st.z BETWEEN :refZ - :maxLy AND :refZ + :maxLy
                AND POW(st.x - :refX, 2) + POW(st.y - :refY, 2) + POW(st.z - :refZ, 2) <= POW(:maxLy, 2)
                AND st.stationType != 'FleetCarrier'
            ORDER BY c.sellPrice DESC, c.stock DESC
            LIMIT 1
            """)
    @RegisterBeanMapper(CommodityOfferProjection.class)
    List<CommodityOfferProjection> findBestCommodityOffers(
            @Bind("commodityId") short commodityId,
            @Bind("maxLy") double maxLy,
            @Bind("refX") double refX,
            @Bind("refY") double refY,
            @Bind("refZ") double refZ
    );


    // -------------------------------------------------------------------------
    // Supporting types
    // -------------------------------------------------------------------------

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