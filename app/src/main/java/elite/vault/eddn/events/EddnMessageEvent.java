package elite.vault.eddn.events;

import com.fasterxml.jackson.databind.JsonNode;

public record EddnMessageEvent(
        String schemaRef,
        String rawJson,
        JsonNode headerNode
) {
    public boolean isJournal() {
        return schemaRef != null && schemaRef.contains("journal/");
    }

    public boolean matchesSchema(String pattern) {
        return schemaRef != null && schemaRef.contains(pattern);
    }

    public String getSchemaRef() {
        return schemaRef;
    }

    public String getRawJson() {
        return rawJson;
    }

    public JsonNode getHeaderNode() {
        return headerNode;
    }
}
