package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

public class CompositionDto {

    @SerializedName("Ice")
    private Double ice;

    @SerializedName("Metal")
    private Double metal;

    @SerializedName("Rock")
    private Double rock;

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
}