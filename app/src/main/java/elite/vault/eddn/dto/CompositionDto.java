package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class CompositionDto {

    @SerializedName("Ice")
    private Double ice;

    @SerializedName("Metal")
    private Double metal;

    @SerializedName("Rock")
    private Double rock;

    @SerializedName("atmosphereComposition")  // optional if you rename in JSON
    private Map<String, Double> atmosphere = new HashMap<>();

    public Double getIce() {
        return ice;
    }

    public void setIce(Double ice) {
        this.ice = ice;
    }

    public Double getMetal() {
        return metal;
    }

    public void setMetal(Double metal) {
        this.metal = metal;
    }

    public Double getRock() {
        return rock;
    }

    public void setRock(Double rock) {
        this.rock = rock;
    }

    public Map<String, Double> getAtmosphere() {
        return atmosphere;
    }

    public void setAtmosphere(Map<String, Double> atmosphere) {
        this.atmosphere = atmosphere;
    }
}