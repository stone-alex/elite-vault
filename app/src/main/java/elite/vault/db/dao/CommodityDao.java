package elite.vault.db.dao;

import elite.vault.db.projections.CommodityOfferProjection;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Set;

public interface CommodityDao {

    // -------------------------------------------------------------------------
    // Planetary station type classification
    //
    // Used by the allowPlanetary filter in all trade queries.
    // Add new types here as they are discovered in EDDN data.
    // Kept as a constant so it is easy to update in one place.
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

    /**
     * Load all known commodity types into a name→id map on startup.
     * Returns empty list on a blank DB — that is normal.
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
    // Snapshot replace
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
     * Find the best places to sell a specific commodity near a reference point.
     * <p>
     * Distance is 3D: bounding cube on indexed x/y/z columns for the initial
     * candidate set, then exact sqrt distance for the final filter and sort.
     * The 2D spatial index (pos) is intentionally NOT used here — it only stores
     * X and Y and would silently under-filter in a 3D galaxy.
     */
    @RegisterBeanMapper(CommodityOfferProjection.class)
    @SqlQuery("""
            SELECT
                ss.starName                                                                          AS starName,
                st.realName                                                                          AS stationName,
                ct.name                                                                              AS commodity,
                c.sellPrice                                                                          AS sellPrice,
                c.stock                                                                              AS stock,
                ROUND(SQRT(POW(ss.x - :refX, 2) + POW(ss.y - :refY, 2) + POW(ss.z - :refZ, 2)), 1)   AS distanceLy,
                c.marketId                                                                           AS marketId,
                c.systemAddress                                                                      AS systemAddress
            FROM commodity c
            INNER JOIN commodity_type ct ON ct.id             = c.commodityId
            INNER JOIN star_system     ss ON ss.systemAddress  = c.systemAddress
            INNER JOIN stations        st ON st.marketId       = c.marketId
            WHERE c.commodityId = :commodityId
              AND c.stock       > 0
              AND c.sellPrice   > 0
              AND ss.x BETWEEN :refX - :maxLy AND :refX + :maxLy
              AND ss.y BETWEEN :refY - :maxLy AND :refY + :maxLy
              AND ss.z BETWEEN :refZ - :maxLy AND :refZ + :maxLy
              AND POW(ss.x - :refX, 2) + POW(ss.y - :refY, 2) + POW(ss.z - :refZ, 2) <= POW(:maxLy, 2)
            ORDER BY c.sellPrice DESC, c.stock DESC
            LIMIT 20
            """)
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