package elite.vault.api;

import elite.vault.api.server.ApiServer;

public class RunApi {

    public static void main(String[] args) {
        ApiServer apiServer = new ApiServer();
        apiServer.start(8085);  // or read from args/config/env

        // Optional: add shutdown hook for clean stop
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            apiServer.stop();
            System.out.println("API server stopped cleanly");
        }));
    }
}
