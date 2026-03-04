package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

public class EDDN_FactionReferenceDto {
    @SerializedName("Name")
    private String name;

    // getters + setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}