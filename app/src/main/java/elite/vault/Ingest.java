package elite.vault;

import elite.vault.db.util.Database;

public class Ingest {

    public static void main(String[] args) {
        Database.init();
        System.out.print("Vault is running");
    }
}