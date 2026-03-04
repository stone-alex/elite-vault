package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

public class EDDN_FactionDto {
    @SerializedName("Name") private String factionName;
    @SerializedName("Allegiance") private String allegiance;
    @SerializedName("Government") private String government;
    @SerializedName("Influence") private Double influence;
    @SerializedName("FactionState") private String factionState;
    @SerializedName("Happiness") private String happiness;

    public String getFactionName() {
        return factionName;
    }

    public void setFactionName(String factionName) {
        this.factionName = factionName;
    }

    public String getAllegiance() {
        return allegiance;
    }

    public void setAllegiance(String allegiance) {
        this.allegiance = allegiance;
    }

    public String getGovernment() {
        return government;
    }

    public void setGovernment(String government) {
        this.government = government;
    }

    public Double getInfluence() {
        return influence;
    }

    public void setInfluence(Double influence) {
        this.influence = influence;
    }

    public String getFactionState() {
        return factionState;
    }

    public void setFactionState(String factionState) {
        this.factionState = factionState;
    }

    public String getHappiness() {
        return happiness;
    }

    public void setHappiness(String happiness) {
        this.happiness = happiness;
    }
}
