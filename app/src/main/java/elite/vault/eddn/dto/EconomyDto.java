package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

public class EconomyDto {

    @SerializedName("name")
    private String name;

    @SerializedName("proportion")
    private Double proportion;

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