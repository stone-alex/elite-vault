package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

public class EDDN_SignalDto {
    @SerializedName("Type") private String type;   // "Biological", "Geological", etc.
    @SerializedName("Count") private Integer count;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
