package elite.vault.eddn.events;

import com.fasterxml.jackson.databind.JsonNode;

public record EddnMessageEvent(
        String schemaRef,
        JsonNode messageNode,
        JsonNode headerNode  // for uploaderID, software, etc. if needed for audit/provenance
) {
    public String getEventType() {
        return messageNode.path("event").asText(null);
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

    public JsonNode getMessageNode() {
        return messageNode;
    }

    public JsonNode getHeaderNode() {
        return headerNode;
    }
}
