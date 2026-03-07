package elite.vault.eddn.subscribers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class EventProcessingExecutor {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, new ThreadFactory() {
        private int counter = 1;

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "eddn-worker-" + counter++);
            t.setDaemon(true);
            return t;
        }
    });

    private EventProcessingExecutor() {
    }

    public static void submit(Runnable task) {
        EXECUTOR.submit(task);
    }

    public static void shutdown() {
        EXECUTOR.shutdown();
    }
}