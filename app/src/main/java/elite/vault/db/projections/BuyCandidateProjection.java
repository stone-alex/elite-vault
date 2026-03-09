package elite.vault.db.projections;

/**
 * Projection returned by CommodityDao.findBuyCandidates.
 * Coordinates come from stations.x/y/z — no star_system join needed.
 * buySystem is omitted; if needed for display, look up via systemAddress.
 */
public class BuyCandidateProjection {

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
}