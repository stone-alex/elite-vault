package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CommodityMessageDto extends BaseDto {

    @SerializedName("systemName")
    private String systemName;

    @SerializedName("stationName")
    private String stationName;

    @SerializedName("marketId")
    private Long marketId;

    @SerializedName("stationType")
    private String stationType;

    @SerializedName("CarrierDockingAccess")
    private String carrierDockingAccess;

    @SerializedName("economies")
    private List<EconomyDto> economies;

    @SerializedName("prohibited")
    private List<String> prohibited;

    @SerializedName("commodities")
    private List<CommodityItemDto> commodities;

    // getters + setters – update names to match Java convention
    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public Long getMarketId() {
        return marketId;
    }

    public void setMarketId(Long marketId) {
        this.marketId = marketId;
    }

    public String getStationType() {
        return stationType;
    }

    public void setStationType(String stationType) {
        this.stationType = stationType;
    }

    public List<EconomyDto> getEconomies() {
        return economies;
    }

    public void setEconomies(List<EconomyDto> economies) {
        this.economies = economies;
    }

    public List<String> getProhibited() {
        return prohibited;
    }

    public void setProhibited(List<String> prohibited) {
        this.prohibited = prohibited;
    }

    public List<CommodityItemDto> getCommodities() {
        return commodities;
    }

    public void setCommodities(List<CommodityItemDto> commodities) {
        this.commodities = commodities;
    }

    public String getCarrierDockingAccess() {
        return carrierDockingAccess;
    }

    // Optional: add toString() for easier debugging
    @Override
    public String toString() {
        return "CommodityMessageDto{" +
                "systemName='" + systemName + '\'' +
                ", stationName='" + stationName + '\'' +
                ", marketId=" + marketId +
                ", stationType='" + stationType + '\'' +
                ", commoditiesCount=" + (commodities != null ? commodities.size() : 0) +
                '}';
    }
}