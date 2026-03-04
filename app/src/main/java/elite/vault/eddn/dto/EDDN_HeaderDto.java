package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

import java.time.ZonedDateTime;

public class EDDN_HeaderDto {

    @SerializedName("gamebuild")
    private String gamebuild;

    @SerializedName("gameversion")
    private String gameversion;

    @SerializedName("gatewayTimestamp")
    private ZonedDateTime gatewayTimestamp;

    @SerializedName("softwareName")
    private String softwareName;

    @SerializedName("softwareVersion")
    private String softwareVersion;

    @SerializedName("uploaderID")
    private String uploaderId;

    public String getGamebuild() {
        return gamebuild;
    }

    public void setGamebuild(String gamebuild) {
        this.gamebuild = gamebuild;
    }

    public String getGameversion() {
        return gameversion;
    }

    public void setGameversion(String gameversion) {
        this.gameversion = gameversion;
    }

    public ZonedDateTime getGatewayTimestamp() {
        return gatewayTimestamp;
    }

    public void setGatewayTimestamp(ZonedDateTime gatewayTimestamp) {
        this.gatewayTimestamp = gatewayTimestamp;
    }

    public String getSoftwareName() {
        return softwareName;
    }

    public void setSoftwareName(String softwareName) {
        this.softwareName = softwareName;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public String getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(String uploaderId) {
        this.uploaderId = uploaderId;
    }
}