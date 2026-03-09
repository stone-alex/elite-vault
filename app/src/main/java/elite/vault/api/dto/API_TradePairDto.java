package elite.vault.api.dto;

import elite.vault.util.Convertable;

public class API_TradePairDto extends Convertable {

    private String sourceSystem;
    private String sourceStation;
    private String destinationSystem;
    private String destinationStation;
    private Long sourceMarketId;
    private Long destinationMarketId;
    private String commodity;
    private double buyPrice;
    private double sellPrice;
    private double profitPerUnit;
    private int stock;
    private int demand;
    private double distanceLy;
    private long estimatedProfit;

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getSourceStation() {
        return sourceStation;
    }

    public void setSourceStation(String sourceStation) {
        this.sourceStation = sourceStation;
    }

    public String getDestinationSystem() {
        return destinationSystem;
    }

    public void setDestinationSystem(String destinationSystem) {
        this.destinationSystem = destinationSystem;
    }

    public String getDestinationStation() {
        return destinationStation;
    }

    public void setDestinationStation(String destinationStation) {
        this.destinationStation = destinationStation;
    }

    public Long getSourceMarketId() {
        return sourceMarketId;
    }

    public void setSourceMarketId(Long sourceMarketId) {
        this.sourceMarketId = sourceMarketId;
    }

    public Long getDestinationMarketId() {
        return destinationMarketId;
    }

    public void setDestinationMarketId(Long destinationMarketId) {
        this.destinationMarketId = destinationMarketId;
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

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getDemand() {
        return demand;
    }

    public void setDemand(int demand) {
        this.demand = demand;
    }

    public double getDistanceLy() {
        return distanceLy;
    }

    public void setDistanceLy(double distanceLy) {
        this.distanceLy = distanceLy;
    }

    public void setEstimatedRunProfit(long estimate) {
        this.estimatedProfit = estimate;
    }

    public long getEstimatedProfit() {
        return estimatedProfit;
    }
}