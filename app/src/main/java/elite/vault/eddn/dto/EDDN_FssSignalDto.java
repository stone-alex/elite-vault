package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a single signal item within the EDDN fsssignaldiscovered/1 schema.
 * <p>
 * Schema: https://eddn.edcd.io/schemas/fsssignaldiscovered/1
 * <p>
 * Notes:
 * - Localised strings (_Localised suffix) are stripped by the EDDN gateway — never present.
 * - TimeRemaining is disallowed by the schema — never present.
 * - USSType is only present for USS signals, never for ResourceExtraction signals.
 * - SpawningFaction / SpawningState / ThreatLevel are only present for USS signals.
 */
public class EDDN_FssSignalDto {

    @SerializedName("timestamp")
    private String timestamp;               // ISO date-time, required

    @SerializedName("SignalName")
    private String signalName;              // required — localisation key e.g. "$MULTIPLAYER_SCENARIO78_TITLE;"
    // or plain name for fleet carriers / installations

    @SerializedName("SignalType")
    private String signalType;              // optional — "ResourceExtraction", "FleetCarrier",
    // "Installation", "NavBeacon", "USS", etc.

    @SerializedName("IsStation")
    private Boolean isStation;             // optional — true for station-like signals

    @SerializedName("USSType")
    private String ussType;                 // optional — USS signals only, e.g. "$USS_Type_Salvage;"
    // "$USS_Type_MissionTarget;" is blocked by schema

    @SerializedName("SpawningFaction")
    private String spawningFaction;         // optional — USS signals only

    @SerializedName("SpawningState")
    private String spawningState;           // optional — USS signals only

    @SerializedName("SpawningPower")
    private String spawningPower;           // optional — powerplay context

    @SerializedName("OpposingPower")
    private String opposingPower;           // optional — powerplay context

    @SerializedName("ThreatLevel")
    private Integer threatLevel;            // optional — USS threat level integer

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    public boolean isResourceExtractionSite() {
        return "ResourceExtraction".equals(signalType);
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSignalName() {
        return signalName;
    }

    public void setSignalName(String signalName) {
        this.signalName = signalName;
    }

    public String getSignalType() {
        return signalType;
    }

    public void setSignalType(String signalType) {
        this.signalType = signalType;
    }

    public Boolean getIsStation() {
        return isStation;
    }

    public void setIsStation(Boolean isStation) {
        this.isStation = isStation;
    }

    public String getUssType() {
        return ussType;
    }

    public void setUssType(String ussType) {
        this.ussType = ussType;
    }

    public String getSpawningFaction() {
        return spawningFaction;
    }

    public void setSpawningFaction(String spawningFaction) {
        this.spawningFaction = spawningFaction;
    }

    public String getSpawningState() {
        return spawningState;
    }

    public void setSpawningState(String spawningState) {
        this.spawningState = spawningState;
    }

    public String getSpawningPower() {
        return spawningPower;
    }

    public void setSpawningPower(String spawningPower) {
        this.spawningPower = spawningPower;
    }

    public String getOpposingPower() {
        return opposingPower;
    }

    public void setOpposingPower(String opposingPower) {
        this.opposingPower = opposingPower;
    }

    public Integer getThreatLevel() {
        return threatLevel;
    }

    public void setThreatLevel(Integer threatLevel) {
        this.threatLevel = threatLevel; }
}