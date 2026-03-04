package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

public class EDDN_AtmosphereCompositionDto {

    @SerializedName("Name")
    private String name;

    @SerializedName("Percent")
    private Double percent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPercent() {
        return percent;
    }

    public void setPercent(Double percent) {
        this.percent = percent;
    }
}