package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;
import elite.vault.util.BaseDto;

import java.util.List;

/**
 * DTO for EDDN fsssignaldiscovered schema.
 * <p>
 * Schema ref: https://eddn.edcd.io/schemas/fsssignaldiscovered/1
 * <p>
 * Required fields: systemAddress, starSystem, signals
 * <p>
 * Note: EDDN uses lowercase "signals" for this schema (not "Signals" used by SAASignalsFound).
 * These are USS / RES / Nav Beacon signals detected via the FSS scanner.
 */
public class EDDN_FssSignalMessageDto extends BaseDto {
    @SerializedName("SystemAddress")
    private Long systemAddress;         // required

    @SerializedName("StarSystem")
    private String starSystem;          // required

    @SerializedName("StarPos")
    private List<Double> starPos;       // required — [x, y, z]

    @SerializedName("signals")
    private List<EDDN_FssSignalDto> signals;  // required, minItems 1

    public Long getSystemAddress() {
        return systemAddress;
    }

    public void setSystemAddress(Long systemAddress) {
        this.systemAddress = systemAddress;
    }

    public String getStarSystem() {
        return starSystem;
    }

    public void setStarSystem(String starSystem) {
        this.starSystem = starSystem;
    }

    public List<Double> getStarPos() {
        return starPos;
    }

    public void setStarPos(List<Double> starPos) {
        this.starPos = starPos;
    }

    public List<EDDN_FssSignalDto> getSignals() {
        return signals;
    }

    public void setSignals(List<EDDN_FssSignalDto> signals) {
        this.signals = signals;
    }
}