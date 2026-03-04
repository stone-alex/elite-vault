package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

public class EDDN_FssSignalDto {
    @SerializedName("SignalName")
    private String signalName;          // e.g. "$MULTIPLAYER_SCENARIO77_TITLE;", "Trujillo Relay", "Bernhard's Progress"

    @SerializedName("SignalType")
    private String signalType;          // e.g. "ResourceExtraction", "Installation", "StationCoriolis", "NavBeacon", "StationONeilOrbis"

    @SerializedName("timestamp")
    private String timestamp;           // ISO timestamp per signal, often close to message timestamp

    @SerializedName("IsStation")
    private boolean isStation;          // true for station-like signals (e.g. Coriolis, Orbis); absent otherwise

    // Optional / rarer fields that appear in some signals
    // (e.g. in conflict zones, megaships, beacons – safe to leave nullable)
    @SerializedName("SignalSubtype")    // Sometimes present for more detail (e.g. "Megaship", "ConflictZone")
    private String signalSubtype;

    @SerializedName("IsWanted")         // Rare, for certain criminal/pirate signals
    private boolean isWanted;

    // getters + setters for all
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

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean getIsStation() {
        return isStation;
    }

    public void setIsStation(boolean isStation) {
        this.isStation = isStation;
    }

    public String getSignalSubtype() {
        return signalSubtype;
    }

    public void setSignalSubtype(String signalSubtype) {
        this.signalSubtype = signalSubtype;
    }

    public boolean getIsWanted() {
        return isWanted;
    }

    public void setIsWanted(boolean isWanted) {
        this.isWanted = isWanted;
    }
}
