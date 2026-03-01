package elite.vault;

import com.fasterxml.jackson.databind.ObjectMapper;

public enum Singletons {

    INSTANCE;

    ///
    private final ObjectMapper objectMapper = new ObjectMapper();

    ///
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void initialize() {
        System.out.println("Singletons initialized");
    }
}