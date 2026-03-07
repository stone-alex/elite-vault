package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class EDDN_CommodityItemDto {
    @SerializedName("name")
    private String name;

    @SerializedName("meanPrice")          // ← fixed: lowercase 'm'
    private Integer meanPrice;

    @SerializedName("buyPrice")
    private Integer buyPrice;

    @SerializedName("sellPrice")
    private Integer sellPrice;

    @SerializedName("stock")
    private Integer stock;

    @SerializedName("stockBracket")
    private String stockBracket;          // "" or "0".."3"

    @SerializedName("demand")
    private Integer demand;

    @SerializedName("demandBracket")
    private String demandBracket;

    @SerializedName("statusFlags")
    private List<String> statusFlags;
    // getters + setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMeanPrice() {
        return meanPrice;
    }

    public void setMeanPrice(Integer meanPrice) {
        this.meanPrice = meanPrice;
    }

    public Integer getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(Integer buyPrice) {
        this.buyPrice = buyPrice;
    }

    public Integer getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(Integer sellPrice) {
        this.sellPrice = sellPrice;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getStockBracket() {
        return stockBracket;
    }

    public void setStockBracket(String stockBracket) {
        this.stockBracket = stockBracket;
    }

    public Integer getDemand() {
        return demand;
    }

    public void setDemand(Integer demand) {
        this.demand = demand;
    }

    public String getDemandBracket() {
        return demandBracket;
    }

    public void setDemandBracket(String demandBracket) {
        this.demandBracket = demandBracket;
    }

    public List<String> getStatusFlags() {
        return statusFlags;
    }

    public void setStatusFlags(List<String> statusFlags) {
        this.statusFlags = statusFlags;
    }
}