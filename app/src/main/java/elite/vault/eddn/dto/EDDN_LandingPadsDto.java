package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

public class EDDN_LandingPadsDto {
    @SerializedName("Large")
    private Integer large;

    @SerializedName("Medium")
    private Integer medium;

    @SerializedName("Small")
    private Integer small;

    // getters + setters
    public Integer getLarge() {
        return large;
    }

    public void setLarge(Integer large) {
        this.large = large;
    }

    public Integer getMedium() {
        return medium;
    }

    public void setMedium(Integer medium) {
        this.medium = medium;
    }

    public Integer getSmall() {
        return small;
    }

    public void setSmall(Integer small) {
        this.small = small;
    }
}