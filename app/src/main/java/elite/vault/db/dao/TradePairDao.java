package elite.vault.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.time.LocalDateTime;
import java.util.List;

public interface TradePairDao {

    // -------------------------------------------------------------------------
    // Meta - data age for API responses
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


    // -------------------------------------------------------------------------
    // Route hop query
    //
    // Finds the top N best trade pairs reachable from the current position.
    // Called once per hop in MarketManager.calculateTradeRoute.
    //
    // Index usage:
    //   idx_tp_buy_xyz     - bounding box on buyX/Y/Z prunes the candidate set
    //   idx_tp_buy_sys_profit - tiebreak / sort support
    //   idx_tp_large_pad / idx_tp_medium_pad - pad filter support
    //
    // <planetaryTypes> is injected via @Define - JDBI defineList turns it into
    // a quoted IN-clause at query-build time (not a bind parameter), which is
    // required because MariaDB does not support binding a list as an IN value.
    //
    // runValue = profitPerUnit * LEAST(buyStock, sellDemand, cargoCap)
    //   - proxy for "how much money does one full cargo run make?"
    //   - used for ORDER BY so we favour high-volume low-margin pairs over
    //     high-margin low-stock pairs when they yield more total profit.
    // -------------------------------------------------------------------------

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
            WHERE
                -- Distance to arrival filter (both ends)
                tp.buyDistToArrival  <= :maxDistFromEntrance
                AND tp.sellDistToArrival <= :maxDistFromEntrance
                -- Must be profitable
                AND tp.profitPerUnit > 0
                -- Enough supply and demand to fill at least half a hold
                AND tp.buyStock  >= GREATEST(:cargoCap / 2, 10)
                AND tp.sellDemand >= GREATEST(:cargoCap / 2, 10)
                -- Pad size filter:
                --   requireLargePad  → both stations must have a large pad
                --   requireMediumPad → both stations must have large OR medium
                --   neither          → no pad restriction
                AND (
                    :requireLargePad = FALSE
                    OR (tp.buyHasLargePad = TRUE AND tp.sellHasLargePad = TRUE)
                )
                AND (
                    :requireMediumPad = FALSE
                    OR (
                        (tp.buyHasLargePad  = TRUE OR tp.buyHasMediumPad  = TRUE)
                        AND
                        (tp.sellHasLargePad = TRUE OR tp.sellHasMediumPad = TRUE)
                    )
                )
                -- Planetary landing filter: exclude surface/settlement types unless allowed
                AND (
                    :allowPlanetary = TRUE
                    OR tp.buyStationType  NOT IN (<planetaryTypes>)
                )
                AND (
                    :allowPlanetary = TRUE
                    OR tp.sellStationType NOT IN (<planetaryTypes>)
                )
                -- Reachability: buy station must be within jumpRange of current position.
                -- Bounding cube first (hits idx_tp_buy_xyz), exact sphere check second.
                AND tp.buyX BETWEEN :refX - :jumpRange AND :refX + :jumpRange
                AND tp.buyY BETWEEN :refY - :jumpRange AND :refY + :jumpRange
                AND tp.buyZ BETWEEN :refZ - :jumpRange AND :refZ + :jumpRange
                AND POW(tp.buyX - :refX, 2) + POW(tp.buyY - :refY, 2) + POW(tp.buyZ - :refZ, 2)
                    <= POW(:jumpRange, 2)
            ORDER BY
                tp.profitPerUnit * LEAST(tp.buyStock, tp.sellDemand, :cargoCap) DESC
            LIMIT :limit
            """)
    @RegisterBeanMapper(TradePairRow.class)
    List<TradePairRow> findBestHop(
            // Current position (hop 1 = player system, hop N = previous sell system)
            @Bind("refX") double refX,
            @Bind("refY") double refY,
            @Bind("refZ") double refZ,
            // How far the player is willing to travel to reach the buy station
            @Bind("jumpRange") double jumpRange,
            // Station distance-to-arrival cap (light seconds)
            @Bind("maxDistFromEntrance") double maxDistFromEntrance,
            // Cargo capacity - used for runValue calculation and stock/demand threshold
            @Bind("cargoCap") int cargoCap,
            // Pad and environment filters
            @Bind("requireLargePad") boolean requireLargePad,
            @Bind("requireMediumPad") boolean requireMediumPad,
            @Bind("allowPlanetary") boolean allowPlanetary,
            // Injected IN-clause, e.g. 'SurfaceStation','CraterPort',...
            @Define("planetaryTypes") String planetaryTypes,
            // How many candidates to return (caller picks best unused one)
            @Bind("limit") int limit
    );


    // -------------------------------------------------------------------------
    // Supporting type - maps directly from the trade_pair table columns.
    // runValue is a computed column alias, not stored in the table.
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