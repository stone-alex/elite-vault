package elite.vault.db.dao;

import elite.vault.db.projections.BuyCandidateProjection;
import elite.vault.db.projections.CommodityOfferProjection;
import elite.vault.db.projections.SellCandidateProjection;
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
    // Commodity search (used by CommoditiesService)
    //
    // Bounding box on stations.x/y/z (idx_st_xyz) — no star_system join.
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
    // Route hop query — step 1: find buy candidates
    //
    // Bounding box on stations.x/y/z (idx_st_xyz) prunes to stations within
    // hopDistance. No star_system join — coordinates live on stations directly.
    // stations(~14k) is far smaller than star_system(763k) so the scan is tiny.
    // -------------------------------------------------------------------------

    @SqlQuery("""
            SELECT
                c.commodityId                                                                           AS commodityId,
                ct.name                                                                                 AS commodityName,
                c.marketId                                                                              AS buyMarketId,
                c.systemAddress                                                                         AS buySystemAddress,
                st.realName                                                                             AS buyStation,
                c.buyPrice                                                                              AS buyPrice,
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
                    :requireLargePad = FALSE
                    OR st.hasLargePad = TRUE
                )
                AND (
                    :requireMediumPad = FALSE
                    OR st.hasLargePad = TRUE
                    OR st.hasMediumPad = TRUE
                )
                AND (
                    :allowPlanetary = TRUE
                    OR st.stationType NOT IN (<planetaryTypes>)
                )
                AND st.stationType != 'FleetCarrier'
            ORDER BY c.buyPrice ASC
            LIMIT :limit
            """)
    @RegisterBeanMapper(BuyCandidateProjection.class)
    List<BuyCandidateProjection> findBuyCandidates(
            @Bind("refX") double refX,
            @Bind("refY") double refY,
            @Bind("refZ") double refZ,
            @Bind("hopDistance") double hopDistance,
            @Bind("minStock") int minStock,
            @Bind("maxDistToArrival") double maxDistToArrival,
            @Bind("requireLargePad") boolean requireLargePad,
            @Bind("requireMediumPad") boolean requireMediumPad,
            @Bind("allowPlanetary") boolean allowPlanetary,
            @Define("planetaryTypes") String planetaryTypes,
            @Bind("limit") int limit
    );


    // -------------------------------------------------------------------------
    // Route hop query — step 2: find best sell destination
    //
    // Same pattern — bounding box on stations.x/y/z around the buy station.
    // No star_system join needed.
    // -------------------------------------------------------------------------

    @SqlQuery("""
            SELECT
                c.marketId                                                                                  AS sellMarketId,
                c.systemAddress                                                                             AS sellSystemAddress,
                st.realName                                                                                 AS sellStation,
                c.sellPrice                                                                                 AS sellPrice,
                c.demand                                                                                    AS sellDemand,
                st.x                                                                                        AS sellX,
                st.y                                                                                        AS sellY,
                st.z                                                                                        AS sellZ,
                st.hasLargePad                                                                              AS sellHasLargePad,
                st.hasMediumPad                                                                             AS sellHasMediumPad,
                st.stationType                                                                              AS sellStationType,
                st.distanceToArrival                                                                        AS sellDistToArrival,
                ROUND(SQRT(POW(st.x - :buyX, 2) + POW(st.y - :buyY, 2) + POW(st.z - :buyZ, 2)), 2)        AS distanceFromBuy
            FROM stations st
            INNER JOIN commodity c ON c.marketId    = st.marketId
                                  AND c.commodityId = :commodityId
                                  AND c.sellPrice   > :buyPrice
                                  AND c.demand     >= :minDemand
                                  AND c.marketId   != :buyMarketId
            WHERE
                st.x BETWEEN :buyX - :hopDistance AND :buyX + :hopDistance
                AND st.y BETWEEN :buyY - :hopDistance AND :buyY + :hopDistance
                AND st.z BETWEEN :buyZ - :hopDistance AND :buyZ + :hopDistance
                AND POW(st.x - :buyX, 2) + POW(st.y - :buyY, 2) + POW(st.z - :buyZ, 2)
                    <= POW(:hopDistance, 2)
                AND st.distanceToArrival <= :maxDistToArrival
                AND (
                    :requireLargePad = FALSE
                    OR st.hasLargePad = TRUE
                )
                AND (
                    :requireMediumPad = FALSE
                    OR st.hasLargePad = TRUE
                    OR st.hasMediumPad = TRUE
                )
                AND (
                    :allowPlanetary = TRUE
                    OR st.stationType NOT IN (<planetaryTypes>)
                )
                AND st.stationType != 'FleetCarrier'
            ORDER BY c.sellPrice DESC
            LIMIT 1
            """)
    @RegisterBeanMapper(SellCandidateProjection.class)
    SellCandidateProjection findBestSell(
            @Bind("commodityId") short commodityId,
            @Bind("buyMarketId") long buyMarketId,
            @Bind("buyPrice") int buyPrice,
            @Bind("buyX") double buyX,
            @Bind("buyY") double buyY,
            @Bind("buyZ") double buyZ,
            @Bind("hopDistance") double hopDistance,
            @Bind("minDemand") int minDemand,
            @Bind("maxDistToArrival") double maxDistToArrival,
            @Bind("requireLargePad") boolean requireLargePad,
            @Bind("requireMediumPad") boolean requireMediumPad,
            @Bind("allowPlanetary") boolean allowPlanetary,
            @Define("planetaryTypes") String planetaryTypes
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