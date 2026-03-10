package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;
import elite.vault.util.BaseDto;

import java.util.List;

/**
 * DTO for EDDN commodity/3 schema.
 * <p>
 * Schema ref: https://eddn.edcd.io/schemas/commodity/3
 * <p>
 * Required fields: systemName, marketId, stationName, commodities
 */
public class EDDN_CommodityMessageDto extends BaseDto {

    @SerializedName("systemName")
    private String systemName;

    @SerializedName("stationName")
    private String stationName;

    @SerializedName("MarketID")
    private Long MarketId;

    @SerializedName("marketId")
    private Long marketId;

    @SerializedName("StationName")
    private String StationName;

    @SerializedName("commodities")
    private List<EDDN_CommodityItemDto> commodities;

    @SerializedName("prohibited")
    private List<String> prohibited;

    @SerializedName("economies")
    private List<EDDN_EconomyDto> economies;

    @SerializedName("CarrierDockingAccess")
    private String carrierDockingAccess;

    public String getSystemName() {
        return systemName;
    }

    public String getStationName() {
        if (stationName != null) return stationName;
        return StationName;
    }

    public Long getMarketId() {
        if (marketId != null) return marketId;
        return MarketId;
    }

    public List<EDDN_CommodityItemDto> getCommodities() {
        return commodities;
    }

    public List<String> getProhibited() {
        return prohibited;
    }

    public List<EDDN_EconomyDto> getEconomies() {
        return economies;
    }

    public String getCarrierDockingAccess() {
        return carrierDockingAccess;
    }
}