package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CommodityMessageDto extends BaseDto {  // timestamp & horizons/odyssey from BaseDto

    // No "event" field in commodity schema → can ignore or add custom if storing mixed

    @SerializedName("StarSystem")
    private String starSystem;

    @SerializedName("StationName")
    private String stationName;

    @SerializedName("MarketID")
    private Long marketId;

    @SerializedName("StationType")
    private String stationType;           // optional in some cases

    @SerializedName("CarrierDockingAccess")
    private String carrierDockingAccess;  // optional, e.g. for fleet carriers

    @SerializedName("Economies")
    private List<EconomyDto> economies;

    @SerializedName("Prohibited")
    private List<String> prohibited;

    @SerializedName("Commodities")
    private List<CommodityItemDto> commodities;

    // getters + setters

    public String getStarSystem() {
        return starSystem;
    }

    public void setStarSystem(String starSystem) {
        this.starSystem = starSystem;
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

    public String getCarrierDockingAccess() {
        return carrierDockingAccess;
    }

    public void setCarrierDockingAccess(String carrierDockingAccess) {
        this.carrierDockingAccess = carrierDockingAccess;
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
}