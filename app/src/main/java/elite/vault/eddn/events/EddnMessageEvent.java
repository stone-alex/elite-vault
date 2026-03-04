package elite.vault.eddn.events;

import com.fasterxml.jackson.databind.JsonNode;
import elite.vault.eddn.dto.EddnDto;

public record EddnMessageEvent(
        String schemaRef,
        EddnDto data,
        JsonNode headerNode  // for uploaderID, software, etc. if needed for audit/provenance
) {
    public String getEventType() {
        return data.getEvent();
    }

    public boolean isJournal() {
        return schemaRef != null && schemaRef.contains("journal/");
    }

    public boolean matchesSchema(String pattern) {
        return schemaRef != null && schemaRef.contains(pattern);
    }

    public String getSchemaRef() {
        return schemaRef;
    }

    public EddnDto getData() {
        return data;
    }

    public JsonNode getHeaderNode() {
        return headerNode;
    }
}
