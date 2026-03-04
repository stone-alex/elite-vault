package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

public class EDDN_PowerConflictProgressDto {
    @SerializedName("Power")
    private String power;

    @SerializedName("ConflictProgress")
    private Double conflictProgress;

    public String getPower() {
        return power;
    }

    public void setPower(String power) {
        this.power = power;
    }

    public Double getConflictProgress() {
        return conflictProgress;
    }

    public void setConflictProgress(Double conflictProgress) {
        this.conflictProgress = conflictProgress;
    }
}