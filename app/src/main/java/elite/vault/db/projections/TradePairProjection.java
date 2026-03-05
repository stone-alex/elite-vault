package elite.vault.db.projections;

public class TradePairProjection {
    private String buySystem;
    private String buyStation;
    private String sellSystem;
    private String sellStation;
    private String commodity;
    private double buyPrice;
    private double sellPrice;
    private double profitPerUnit;
    private int buyStock;
    private int sellDemand;
    private long buyMarketId;
    private long sellMarketId;
    private long buySystemAddress;
    private long sellSystemAddress;
    private double buyDistanceLy;
    private double legDistanceLy;
    private double buyX;
    private double buyY;
    private double buyDistToArrival;
    private double sellDistToArrival;

    // getters & setters for all fields
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

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public double getProfitPerUnit() {
        return profitPerUnit;
    }

    public void setProfitPerUnit(double profitPerUnit) {
        this.profitPerUnit = profitPerUnit;
    }

    public int getBuyStock() {
        return buyStock;
    }

    public void setBuyStock(int buyStock) {
        this.buyStock = buyStock;
    }

    public int getSellDemand() {
        return sellDemand;
    }

    public void setSellDemand(int sellDemand) {
        this.sellDemand = sellDemand;
    }

    public long getBuyMarketId() {
        return buyMarketId;
    }

    public void setBuyMarketId(long buyMarketId) {
        this.buyMarketId = buyMarketId;
    }

    public long getSellMarketId() {
        return sellMarketId;
    }

    public void setSellMarketId(long sellMarketId) {
        this.sellMarketId = sellMarketId;
    }

    public long getBuySystemAddress() {
        return buySystemAddress;
    }

    public void setBuySystemAddress(long buySystemAddress) {
        this.buySystemAddress = buySystemAddress;
    }

    public long getSellSystemAddress() {
        return sellSystemAddress;
    }

    public void setSellSystemAddress(long sellSystemAddress) {
        this.sellSystemAddress = sellSystemAddress;
    }

    public double getBuyDistanceLy() {
        return buyDistanceLy;
    }

    public void setBuyDistanceLy(double buyDistanceLy) {
        this.buyDistanceLy = buyDistanceLy;
    }

    public double getLegDistanceLy() {
        return legDistanceLy;
    }

    public void setLegDistanceLy(double legDistanceLy) {
        this.legDistanceLy = legDistanceLy;
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

    public double getBuyDistToArrival() {
        return buyDistToArrival;
    }

    public void setBuyDistToArrival(double buyDistToArrival) {
        this.buyDistToArrival = buyDistToArrival;
    }

    public double getSellDistToArrival() {
        return sellDistToArrival;
    }

    public void setSellDistToArrival(double sellDistToArrival) {
        this.sellDistToArrival = sellDistToArrival;
    }
}