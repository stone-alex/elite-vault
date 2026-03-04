package elite.vault.api.dto;

import elite.vault.util.Convertable;

public class API_CommodityDto extends Convertable {
    private String starName;
    private String stationName;
    private String commodity;
    private double sellPrice;
    private int stock;
    private double distanceLy;
    private long marketId;
    private long systemAddress;

    public String getStarName() {
        return starName;
    }

    public void setStarName(String starName) {
        this.starName = starName;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getCommodity() {
        return commodity;
    }

    public void setCommodity(String commodity) {
        this.commodity = commodity;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public double getDistanceLy() {
        return distanceLy;
    }

    public void setDistanceLy(double distanceLy) {
        this.distanceLy = distanceLy;
    }

    public long getMarketId() {
        return marketId;
    }

    public void setMarketId(long marketId) {
        this.marketId = marketId;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public void setSystemAddress(long systemAddress) {
        this.systemAddress = systemAddress;
    }
}
