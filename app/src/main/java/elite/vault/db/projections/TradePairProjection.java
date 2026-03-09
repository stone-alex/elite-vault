package elite.vault.db.projections;

/**
 * Projection used by trade route queries.
 * <p>
 * Buy-side fields are populated by findBuyOffers / findBuyOffersAtStation.
 * Sell-side fields are populated by findBestSellFor.
 * A single instance never holds both sides at the same time — the two halves
 * are matched in MarketManager.calculateTradeRoute.
 */
public class TradePairProjection {

    // -- Buy side --
    private String buySystem;
    private String buyStation;
    private String commodity;
    private double buyPrice;
    private int buyStock;
    private long buyMarketId;
    private long buySystemAddress;
    private double buyX;
    private double buyY;
    private double buyZ;           // 3D — was missing in original
    private double buyDistToArrival;
    private double buyDistanceLy;

    // -- Sell side --
    private String sellSystem;
    private String sellStation;
    private double sellPrice;
    private int sellDemand;
    private long sellMarketId;
    private long sellSystemAddress;
    private double sellX;          // 3D — new
    private double sellY;          // 3D — new
    private double sellZ;          // 3D — new
    private double sellDistToArrival;
    private double legDistanceLy;


    // -------------------------------------------------------------------------
    // Buy-side getters / setters
    // -------------------------------------------------------------------------

    public String getBuySystem() {
        return buySystem;
    }

    public void setBuySystem(String buySystem) {
        this.buySystem = buySystem;
    }

    public String getBuyStation() {
        return buyStation;
    }

    public void setBuyStation(String buyStation) {
        this.buyStation = buyStation;
    }

    public String getCommodity() {
        return commodity;
    }

    public void setCommodity(String commodity) {
        this.commodity = commodity;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public int getBuyStock() {
        return buyStock;
    }

    public void setBuyStock(int buyStock) {
        this.buyStock = buyStock;
    }

    public long getBuyMarketId() {
        return buyMarketId;
    }

    public void setBuyMarketId(long buyMarketId) {
        this.buyMarketId = buyMarketId;
    }

    public long getBuySystemAddress() {
        return buySystemAddress;
    }

    public void setBuySystemAddress(long buySystemAddress) {
        this.buySystemAddress = buySystemAddress;
    }

    public double getBuyX() {
        return buyX;
    }

    public void setBuyX(double buyX) {
        this.buyX = buyX;
    }

    public double getBuyY() {
        return buyY;
    }

    public void setBuyY(double buyY) {
        this.buyY = buyY;
    }

    public double getBuyZ() {
        return buyZ;
    }

    public void setBuyZ(double buyZ) {
        this.buyZ = buyZ;
    }

    public double getBuyDistToArrival() {
        return buyDistToArrival;
    }

    public void setBuyDistToArrival(double buyDistToArrival) {
        this.buyDistToArrival = buyDistToArrival;
    }

    public double getBuyDistanceLy() {
        return buyDistanceLy;
    }

    public void setBuyDistanceLy(double buyDistanceLy) {
        this.buyDistanceLy = buyDistanceLy;
    }


    // -------------------------------------------------------------------------
    // Sell-side getters / setters
    // -------------------------------------------------------------------------

    public String getSellSystem() {
        return sellSystem;
    }

    public void setSellSystem(String sellSystem) {
        this.sellSystem = sellSystem;
    }

    public String getSellStation() {
        return sellStation;
    }

    public void setSellStation(String sellStation) {
        this.sellStation = sellStation;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public int getSellDemand() {
        return sellDemand;
    }

    public void setSellDemand(int sellDemand) {
        this.sellDemand = sellDemand;
    }

    public long getSellMarketId() {
        return sellMarketId;
    }

    public void setSellMarketId(long sellMarketId) {
        this.sellMarketId = sellMarketId;
    }

    public long getSellSystemAddress() {
        return sellSystemAddress;
    }

    public void setSellSystemAddress(long sellSystemAddress) {
        this.sellSystemAddress = sellSystemAddress;
    }

    public double getSellX() {
        return sellX;
    }

    public void setSellX(double sellX) {
        this.sellX = sellX;
    }

    public double getSellY() {
        return sellY;
    }

    public void setSellY(double sellY) {
        this.sellY = sellY;
    }

    public double getSellZ() {
        return sellZ;
    }

    public void setSellZ(double sellZ) {
        this.sellZ = sellZ;
    }

    public double getSellDistToArrival() {
        return sellDistToArrival;
    }

    public void setSellDistToArrival(double sellDistToArrival) {
        this.sellDistToArrival = sellDistToArrival;
    }

    public double getLegDistanceLy() {
        return legDistanceLy;
    }

    public void setLegDistanceLy(double legDistanceLy) {
        this.legDistanceLy = legDistanceLy; }
}