package elite.vault.api.dto;

import elite.vault.util.Convertable;

public class API_MissionProviderDto extends Convertable {

    private String stationName;
    private String stationType;
    private String systemName;
    private double distanceLy;         // distance from hunting ground system
    private double distanceToArrival;  // supercruise distance in ls
    private boolean hasLargePad;
    private String controllingFaction;
    private String factionState;
    private double influence;

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String v) {
        this.stationName = v;
    }

    public String getStationType() {
        return stationType;
    }

    public void setStationType(String v) {
        this.stationType = v;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String v) {
        this.systemName = v;
    }

    public double getDistanceLy() {
        return distanceLy;
    }

    public void setDistanceLy(double v) {
        this.distanceLy = v;
    }

    public double getDistanceToArrival() {
        return distanceToArrival;
    }

    public void setDistanceToArrival(double v) {
        this.distanceToArrival = v;
    }

    public boolean isHasLargePad() {
        return hasLargePad;
    }

    public void setHasLargePad(boolean v) {
        this.hasLargePad = v;
    }

    public String getControllingFaction() {
        return controllingFaction;
    }

    public void setControllingFaction(String v) {
        this.controllingFaction = v;
    }

    public String getFactionState() {
        return factionState;
    }

    public void setFactionState(String v) {
        this.factionState = v;
    }

    public double getInfluence() {
        return influence;
    }

    public void setInfluence(double v) {
        this.influence = v;
    }
}