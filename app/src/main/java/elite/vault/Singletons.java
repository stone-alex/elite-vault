package elite.vault;

import com.fasterxml.jackson.databind.ObjectMapper;
import elite.vault.db.managers.FssSignalManager;
import elite.vault.db.managers.MarketManager;
import elite.vault.db.managers.StarSystemManager;
import elite.vault.db.managers.StellarObjectManager;

public enum Singletons {

    SINGLETONS;

    ///
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StellarObjectManager stellarObjectManager = StellarObjectManager.getInstance();
    private final MarketManager marketManager = MarketManager.getInstance();
    private final StarSystemManager starSystemManager = StarSystemManager.getInstance();
    private final FssSignalManager fssSignalManager = FssSignalManager.getInstance();

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

    public FssSignalManager getFssSignalManager() {
        return fssSignalManager;
    }

    public void initialize() {
        System.out.println("Singletons initialized");
    }
}