package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

public class EDDN_ConflictFactionDto {
    @SerializedName("Name")
    private String name;

    @SerializedName("Stake")
    private String stake;

    @SerializedName("WonDays")
    private Integer wonDays;

    // getters + setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStake() {
        return stake;
    }

    public void setStake(String stake) {
        this.stake = stake;
    }

    public Integer getWonDays() {
        return wonDays;
    }

    public void setWonDays(Integer wonDays) {
        this.wonDays = wonDays;
    }
}