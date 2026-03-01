package elite.vault;

import elite.vault.eddn.EdDnClient;
import elite.vault.eddn.SubscriberRegistration;

import static elite.vault.Singletons.INSTANCE;

public class Ingest {

    public static void main(String[] args) {
        INSTANCE.initialize();
        SubscriberRegistration.registerSubscribers();

        //Database.init();
        EdDnClient client = EdDnClient.getInstance();
        client.start();
    }
}