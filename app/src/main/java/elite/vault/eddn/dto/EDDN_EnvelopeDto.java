package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

public class EDDN_EnvelopeDto {

    @SerializedName("$schemaRef")
    private String schemaRef;

    @SerializedName("header")
    private EDDN_HeaderDto header;

    @SerializedName("message")
    private EddnDto message;

    public String getSchemaRef() {
        return schemaRef;
    }

    public void setSchemaRef(String schemaRef) {
        this.schemaRef = schemaRef;
    }

    public EDDN_HeaderDto getHeader() {
        return header;
    }

    public void setHeader(EDDN_HeaderDto header) {
        this.header = header;
    }

    public EddnDto getMessage() {
        return message;
    }

    public void setMessage(EddnDto message) {
        this.message = message;
    }
}