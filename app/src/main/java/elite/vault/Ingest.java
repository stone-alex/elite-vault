package elite.vault;

import elite.vault.db.util.Database;
import elite.vault.eddn.EdDnClient;
import elite.vault.eddn.MarketPruneScheduler;
import elite.vault.eddn.SubscriberRegistration;

import static elite.vault.Singletons.SINGLETONS;

public class Ingest {

    public static void main(String[] args) {
        Database.init();
        SINGLETONS.initialize();
        SubscriberRegistration.registerSubscribers();

        EdDnClient client = EdDnClient.getInstance();
        client.start();

        MarketPruneScheduler marketPruneScheduler = MarketPruneScheduler.getInstance();
        marketPruneScheduler.start();
    }
}