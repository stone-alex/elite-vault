package elite.vault.eddn.events;

import com.fasterxml.jackson.databind.JsonNode;

public record EddnMessageEvent(
        String schemaRef,
        JsonNode messageNode,
        JsonNode headerNode  // for uploaderID, software, etc. if needed for audit/provenance
) {
    public String eventType() {
        return messageNode.path("event").asText(null);
    }

    public boolean isJournal() {
        return schemaRef != null && schemaRef.contains("journal/");
    }

    public boolean matchesSchema(String pattern) {
        return schemaRef != null && schemaRef.contains(pattern);
    }
}
