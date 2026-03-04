package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

public class EDDN_ConflictDto {
    @SerializedName("WarType")
    private String warType;                     // "war", "civilwar", "election"

    @SerializedName("Status")
    private String status;                      // "pending", "active", "resolved" (pending is common in FSDJump)

    @SerializedName("Faction1")
    private EDDN_ConflictFactionDto faction1;

    @SerializedName("Faction2")
    private EDDN_ConflictFactionDto faction2;

    // getters + setters
    public String getWarType() {
        return warType;
    }

    public void setWarType(String warType) {
        this.warType = warType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public EDDN_ConflictFactionDto getFaction1() {
        return faction1;
    }

    public void setFaction1(EDDN_ConflictFactionDto faction1) {
        this.faction1 = faction1;
    }

    public EDDN_ConflictFactionDto getFaction2() {
        return faction2;
    }

    public void setFaction2(EDDN_ConflictFactionDto faction2) {
        this.faction2 = faction2;
    }
}