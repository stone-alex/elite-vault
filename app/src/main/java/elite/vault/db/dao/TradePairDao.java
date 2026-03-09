package elite.vault.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.time.LocalDateTime;
import java.util.List;

public interface TradePairDao {

    // -------------------------------------------------------------------------
    // Meta — data age for API responses
    // -------------------------------------------------------------------------

    /**
     * Returns the timestamp of the last completed recalculation, or null if
     * the procedure has never run successfully. The REST layer uses this to
     * tell callers how stale the trade data is.
     */
    @SqlQuery("""
            SELECT last_finished_at
            FROM   trade_pair_meta
            WHERE  id = 1
            """)
    LocalDateTime getLastCalculatedAt();

    @SqlQuery("""
            SELECT status
            FROM   trade_pair_meta
            WHERE  id = 1
            """)
    String getStatus();


    // -------------------------------------------------------------------------
    // Route queries
    // -------------------------------------------------------------------------

    /**
     * Find the best trade pairs buyable from a given system (or within jump
     * range of it).
     * <p>
     * Filters applied:
     * - buy system within jumpRange of the reference point (3D bounding cube
     * + exact sphere — buyX/Y/Z indexes make the cube cheap)
     * - distanceLy <= jumpRange  (buy→sell leg fits in one jump sequence)
     * - station distance from system entry point
     * - pad size (large required → buyHasLargePad AND sellHasLargePad;
     * medium required → (large OR medium) on both sides)
     * - planetary station type exclusion on both buy and sell sides
     * <p>
     * Ranked by: profitPerUnit × min(buyStock, sellDemand, cargoCap) DESC
     * so high-volume runs rank above small-stock outliers.
     * <p>
     * Note: the planetary type IN-clause (<planetaryTypes>) must be injected
     * via handle.define() — see MarketManager. Same pattern as before.
     */
    @RegisterBeanMapper(TradePairRow.class)
    @SqlQuery("""
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
            WHERE tp.distanceLy              <= :jumpRange
              AND tp.buyDistToArrival        <= :maxDistFromEntrance
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
              AND tp.buyX BETWEEN :refX - :jumpRange AND :refX + :jumpRange
              AND tp.buyY BETWEEN :refY - :jumpRange AND :refY + :jumpRange
              AND tp.buyZ BETWEEN :refZ - :jumpRange AND :refZ + :jumpRange
              AND POW(tp.buyX - :refX, 2) + POW(tp.buyY - :refY, 2) + POW(tp.buyZ - :refZ, 2)
                      <= POW(:jumpRange, 2)
            ORDER BY runValue DESC
            LIMIT :limit
            """)
    List<TradePairRow> findBestPairsNear(
            @Bind("refX") double refX,
            @Bind("refY") double refY,
            @Bind("refZ") double refZ,
            @Bind("jumpRange") double jumpRange,
            @Bind("maxDistFromEntrance") double maxDistFromEntrance,
            @Bind("requireLargePad") boolean requireLargePad,
            @Bind("requireMediumPad") boolean requireMediumPad,
            @Bind("allowPlanetary") boolean allowPlanetary,
            @Bind("cargoCap") int cargoCap,
            @Bind("limit") int limit
    );


    // -------------------------------------------------------------------------
    // Supporting type — maps directly from the trade_pair table columns
    // -------------------------------------------------------------------------

    class TradePairRow {
        private short commodityId;
        private String commodityName;

        private long buyMarketId;
        private long buySystemAddress;
        private String buySystem;
        private String buyStation;
        private int buyPrice;
        private int buyStock;
        private double buyX;
        private double buyY;
        private double buyZ;
        private boolean buyHasLargePad;
        private boolean buyHasMediumPad;
        private String buyStationType;
        private double buyDistToArrival;

        private long sellMarketId;
        private long sellSystemAddress;
        private String sellSystem;
        private String sellStation;
        private int sellPrice;
        private int sellDemand;
        private boolean sellHasLargePad;
        private boolean sellHasMediumPad;
        private String sellStationType;
        private double sellDistToArrival;
        private double sellX;
        private double sellY;
        private double sellZ;

        private int profitPerUnit;
        private float distanceLy;
        private long runValue;

        public short getCommodityId() {
            return commodityId;
        }

        public void setCommodityId(short v) {
            this.commodityId = v;
        }

        public String getCommodityName() {
            return commodityName;
        }

        public void setCommodityName(String v) {
            this.commodityName = v;
        }

        public long getBuyMarketId() {
            return buyMarketId;
        }

        public void setBuyMarketId(long v) {
            this.buyMarketId = v;
        }

        public long getBuySystemAddress() {
            return buySystemAddress;
        }

        public void setBuySystemAddress(long v) {
            this.buySystemAddress = v;
        }

        public String getBuySystem() {
            return buySystem;
        }

        public void setBuySystem(String v) {
            this.buySystem = v;
        }

        public String getBuyStation() {
            return buyStation;
        }

        public void setBuyStation(String v) {
            this.buyStation = v;
        }

        public int getBuyPrice() {
            return buyPrice;
        }

        public void setBuyPrice(int v) {
            this.buyPrice = v;
        }

        public int getBuyStock() {
            return buyStock;
        }

        public void setBuyStock(int v) {
            this.buyStock = v;
        }

        public double getBuyX() {
            return buyX;
        }

        public void setBuyX(double v) {
            this.buyX = v;
        }

        public double getBuyY() {
            return buyY;
        }

        public void setBuyY(double v) {
            this.buyY = v;
        }

        public double getBuyZ() {
            return buyZ;
        }

        public void setBuyZ(double v) {
            this.buyZ = v;
        }

        public boolean isBuyHasLargePad() {
            return buyHasLargePad;
        }

        public void setBuyHasLargePad(boolean v) {
            this.buyHasLargePad = v;
        }

        public boolean isBuyHasMediumPad() {
            return buyHasMediumPad;
        }

        public void setBuyHasMediumPad(boolean v) {
            this.buyHasMediumPad = v;
        }

        public String getBuyStationType() {
            return buyStationType;
        }

        public void setBuyStationType(String v) {
            this.buyStationType = v;
        }

        public double getBuyDistToArrival() {
            return buyDistToArrival;
        }

        public void setBuyDistToArrival(double v) {
            this.buyDistToArrival = v;
        }

        public long getSellMarketId() {
            return sellMarketId;
        }

        public void setSellMarketId(long v) {
            this.sellMarketId = v;
        }

        public long getSellSystemAddress() {
            return sellSystemAddress;
        }

        public void setSellSystemAddress(long v) {
            this.sellSystemAddress = v;
        }

        public String getSellSystem() {
            return sellSystem;
        }

        public void setSellSystem(String v) {
            this.sellSystem = v;
        }

        public String getSellStation() {
            return sellStation;
        }

        public void setSellStation(String v) {
            this.sellStation = v;
        }

        public int getSellPrice() {
            return sellPrice;
        }

        public void setSellPrice(int v) {
            this.sellPrice = v;
        }

        public int getSellDemand() {
            return sellDemand;
        }

        public void setSellDemand(int v) {
            this.sellDemand = v;
        }

        public boolean isSellHasLargePad() {
            return sellHasLargePad;
        }

        public void setSellHasLargePad(boolean v) {
            this.sellHasLargePad = v;
        }

        public boolean isSellHasMediumPad() {
            return sellHasMediumPad;
        }

        public void setSellHasMediumPad(boolean v) {
            this.sellHasMediumPad = v;
        }

        public String getSellStationType() {
            return sellStationType;
        }

        public void setSellStationType(String v) {
            this.sellStationType = v;
        }

        public double getSellDistToArrival() {
            return sellDistToArrival;
        }

        public void setSellDistToArrival(double v) {
            this.sellDistToArrival = v;
        }

        public double getSellX() {
            return sellX;
        }

        public void setSellX(double v) {
            this.sellX = v;
        }

        public double getSellY() {
            return sellY;
        }

        public void setSellY(double v) {
            this.sellY = v;
        }

        public double getSellZ() {
            return sellZ;
        }

        public void setSellZ(double v) {
            this.sellZ = v;
        }

        public int getProfitPerUnit() {
            return profitPerUnit;
        }

        public void setProfitPerUnit(int v) {
            this.profitPerUnit = v;
        }

        public float getDistanceLy() {
            return distanceLy;
        }

        public void setDistanceLy(float v) {
            this.distanceLy = v;
        }

        public long getRunValue() {
            return runValue;
        }

        public void setRunValue(long v) {
            this.runValue = v;
        }
    }
}