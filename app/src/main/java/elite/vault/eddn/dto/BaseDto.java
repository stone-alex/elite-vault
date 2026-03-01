package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;
import elite.vault.json.GsonFactory;
import elite.vault.json.ToJsonConvertible;
import elite.vault.yaml.ToYamlConvertable;
import elite.vault.yaml.YamlFactory;

import java.time.ZonedDateTime;

public class BaseDto implements ToJsonConvertible, ToYamlConvertable {

    @SerializedName("timestamp")
    private ZonedDateTime timestamp;

    @SerializedName("event")
    private String event;

    @SerializedName("horizons")
    private Boolean horizons;

    @SerializedName("odyssey")
    private Boolean odyssey;

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Boolean getHorizons() {
        return horizons;
    }

    public void setHorizons(Boolean horizons) {
        this.horizons = horizons;
    }

    public Boolean getOdyssey() {
        return odyssey;
    }

    public void setOdyssey(Boolean odyssey) {
        this.odyssey = odyssey;
    }


    @Override public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override public String toYaml() {
        return YamlFactory.toYaml(this);
    }
}
