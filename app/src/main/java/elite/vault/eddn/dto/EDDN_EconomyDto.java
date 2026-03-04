package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

public class EDDN_EconomyDto {
    @SerializedName("Name")
    private String name;          // "$economy_Industrial;", etc.

    @SerializedName("Proportion")
    private Double proportion;    // 0.0 to 1.0

    // getters + setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getProportion() {
        return proportion;
    }

    public void setProportion(Double proportion) {
        this.proportion = proportion;
    }
}