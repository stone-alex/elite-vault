package elite.vault.api.dto;

import elite.vault.util.Convertable;

import java.util.List;

public class API_HuntingGroundDto extends Convertable {

    private String systemName;
    private double x;
    private double y;
    private double z;
    private double distanceLy;
    private List<String> resGrades;
    private List<String> pirateFactions;
    private int confirmedCount;
    private String lastSeen;                        // ISO string — e.g. "2026-03-10T01:43:45"
    private List<API_MissionProviderDto> missionProviders;

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String v) {
        this.systemName = v;
    }

    public double getX() {
        return x;
    }

    public void setX(double v) {
        this.x = v;
    }

    public double getY() {
        return y;
    }

    public void setY(double v) {
        this.y = v;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double v) {
        this.z = v;
    }

    public double getDistanceLy() {
        return distanceLy;
    }

    public void setDistanceLy(double v) {
        this.distanceLy = v;
    }

    public List<String> getResGrades() {
        return resGrades;
    }

    public void setResGrades(List<String> v) {
        this.resGrades = v;
    }

    public List<String> getPirateFactions() {
        return pirateFactions;
    }

    public void setPirateFactions(List<String> v) {
        this.pirateFactions = v;
    }

    public int getConfirmedCount() {
        return confirmedCount;
    }

    public void setConfirmedCount(int v) {
        this.confirmedCount = v;
    }

    public String getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(String v) {
        this.lastSeen = v;
    }

    public List<API_MissionProviderDto> getMissionProviders() {
        return missionProviders;
    }

    public void setMissionProviders(List<API_MissionProviderDto> v) {
        this.missionProviders = v;
    }
}