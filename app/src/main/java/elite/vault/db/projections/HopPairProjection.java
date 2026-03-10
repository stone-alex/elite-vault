package elite.vault.db.projections;

/**
 * Projection returned by CommodityDao.findBestHopPairs.
 * <p>
 * Contains both buy and sell sides of a trade hop in a single row.
 * Replaces the old pattern of BuyCandidateProjection + SellCandidateProjection
 * which required up to 21 separate queries per hop.
 * <p>
 * Column names must match the SQL aliases exactly (JavaBean convention).
 * See projections/README.md.
 * <p>
 * runValue      = profitPerUnit * LEAST(buyStock, sellDemand, cargoCap)
 * Proxy for total profit from one full cargo run.
 * Used for ORDER BY in the query — favours high-volume
 * low-margin pairs over high-margin low-stock pairs when
 * they yield more total credit.
 * <p>
 * distanceBuyToSell = straight-line ly between buy and sell station.
 * Informational only — does not affect route selection.
 * <p>
 * distanceFromRef   = straight-line ly from the caller's current position
 * to the buy station.
 */
public class HopPairProjection {

    // -- Buy side --
    private short commodityId;
    private String commodityName;
    private long buyMarketId;
    private long buySystemAddress;
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
    private double distanceFromRef;

    // -- Sell side --
    private long sellMarketId;
    private long sellSystemAddress;
    private String sellStation;
    private int sellPrice;
    private int sellDemand;
    private double sellX;
    private double sellY;
    private double sellZ;
    private boolean sellHasLargePad;
    private boolean sellHasMediumPad;
    private String sellStationType;
    private double sellDistToArrival;

    // -- Computed --
    private int profitPerUnit;
    private double distanceBuyToSell;
    private long runValue;


    // -------------------------------------------------------------------------
    // Buy-side getters / setters
    // -------------------------------------------------------------------------

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

    public double getDistanceFromRef() {
        return distanceFromRef;
    }

    public void setDistanceFromRef(double v) {
        this.distanceFromRef = v;
    }


    // -------------------------------------------------------------------------
    // Sell-side getters / setters
    // -------------------------------------------------------------------------

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


    // -------------------------------------------------------------------------
    // Computed getters / setters
    // -------------------------------------------------------------------------

    public int getProfitPerUnit() {
        return profitPerUnit;
    }

    public void setProfitPerUnit(int v) {
        this.profitPerUnit = v;
    }

    public double getDistanceBuyToSell() {
        return distanceBuyToSell;
    }

    public void setDistanceBuyToSell(double v) {
        this.distanceBuyToSell = v;
    }

    public long getRunValue() {
        return runValue;
    }

    public void setRunValue(long v) {
        this.runValue = v;
    }
}