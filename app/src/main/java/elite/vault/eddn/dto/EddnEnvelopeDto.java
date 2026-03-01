package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;

public class EddnEnvelopeDto {

    @SerializedName("$schemaRef")
    private String schemaRef;

    @SerializedName("header")
    private HeaderDto header;

    @SerializedName("message")
    private ScanDto message;

    public String getSchemaRef() {
        return schemaRef;
    }

    public void setSchemaRef(String schemaRef) {
        this.schemaRef = schemaRef;
    }

    public HeaderDto getHeader() {
        return header;
    }

    public void setHeader(HeaderDto header) {
        this.header = header;
    }

    public ScanDto getMessage() {
        return message;
    }

    public void setMessage(ScanDto message) {
        this.message = message;
    }
}