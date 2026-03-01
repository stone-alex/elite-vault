package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

public class MyDto extends BaseDto {

    @SerializedName("Status")
    private String status;

    @SerializedName("Name")
    private String name;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
