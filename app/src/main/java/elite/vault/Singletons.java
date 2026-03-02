package elite.vault;

import com.fasterxml.jackson.databind.ObjectMapper;
import elite.vault.db.managers.MarketManager;
import elite.vault.db.managers.StarSystemManager;
import elite.vault.db.managers.StellarObjectManager;

public enum Singletons {

    INSTANCE;

    ///
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StellarObjectManager stellarObjectManager = StellarObjectManager.getInstance();
    private final MarketManager marketManager = MarketManager.getInstance();
    private final StarSystemManager starSystemManager = StarSystemManager.getInstance();

    ///
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public StellarObjectManager getStellarObjectManager() {
        return stellarObjectManager;
    }

    public MarketManager getMarketManager() {
        return marketManager;
    }

    public StarSystemManager getStarSystemManager() {
        return starSystemManager;
    }

    public void initialize() {
        System.out.println("Singletons initialized");
    }
}