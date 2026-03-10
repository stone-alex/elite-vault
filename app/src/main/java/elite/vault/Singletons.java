package elite.vault;

import com.fasterxml.jackson.databind.ObjectMapper;
import elite.vault.db.managers.*;

public enum Singletons {

    SINGLETONS;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StellarObjectManager stellarObjectManager = StellarObjectManager.getInstance();
    private final MarketManager marketManager = MarketManager.getInstance();
    private final StarSystemManager starSystemManager = StarSystemManager.getInstance();
    private final FssSignalManager fssSignalManager = FssSignalManager.getInstance();
    private final PirateHuntingGroundsManager pirateHuntingGroundsManager = PirateHuntingGroundsManager.getInstance();
    private final StationManager stationManager = StationManager.getInstance();

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

    public PirateHuntingGroundsManager getPirateHuntingGroundsManager() {
        return pirateHuntingGroundsManager;
    }

    public StationManager getStationManager() {
        return stationManager;
    }

    public void initialize() {
        System.out.println("Singletons initialized");
    }
}