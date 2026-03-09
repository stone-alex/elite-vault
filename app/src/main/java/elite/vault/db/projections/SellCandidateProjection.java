package elite.vault.db.projections;

/**
 * Projection returned by CommodityDao.findBestSell.
 * Coordinates come from stations.x/y/z — no star_system join needed.
 * sellSystem is omitted; if needed for display, look up via systemAddress.
 */
public class SellCandidateProjection {

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
    private double distanceFromBuy;

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

    public double getDistanceFromBuy() {
        return distanceFromBuy;
    }

    public void setDistanceFromBuy(double v) {
        this.distanceFromBuy = v;
    }
}