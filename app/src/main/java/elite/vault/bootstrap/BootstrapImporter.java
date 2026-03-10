package elite.vault.bootstrap;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import elite.vault.eddn.dto.EDDN_JournalDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static elite.vault.Singletons.SINGLETONS;

public class BootstrapImporter {

    private static final Logger log = LogManager.getLogger(BootstrapImporter.class);

    private static final ObjectMapper MAPPER = createObjectMapper();
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Configure Jackson to recognize Gson's @SerializedName annotations
        mapper.setAnnotationIntrospector(new com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector() {
            @Override
            public com.fasterxml.jackson.databind.PropertyName findNameForSerialization(com.fasterxml.jackson.databind.introspect.Annotated a) {
                com.google.gson.annotations.SerializedName annotation = a.getAnnotation(com.google.gson.annotations.SerializedName.class);
                return annotation != null ? com.fasterxml.jackson.databind.PropertyName.construct(annotation.value()) : super.findNameForSerialization(a);
            }

            @Override
            public com.fasterxml.jackson.databind.PropertyName findNameForDeserialization(com.fasterxml.jackson.databind.introspect.Annotated a) {
                com.google.gson.annotations.SerializedName annotation = a.getAnnotation(com.google.gson.annotations.SerializedName.class);
                return annotation != null ? com.fasterxml.jackson.databind.PropertyName.construct(annotation.value()) : super.findNameForDeserialization(a);
            }
        });
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    private static final BootstrapImporter instance = new BootstrapImporter();
    private final AtomicLong processedSystems = new AtomicLong(0);
    private final AtomicLong upsertedSystems = new AtomicLong(0);

    private static final int BATCH_SIZE = 2000;
    private static final int NUM_WORKERS = Runtime.getRuntime().availableProcessors();
    private static final int QUEUE_CAPACITY = NUM_WORKERS * 4;

    private BootstrapImporter() {
        // singleton!
    }

    public static BootstrapImporter getInstance() {
        return instance;
    }

    public void importFromFile(Path path) throws IOException, InterruptedException {
        BlockingQueue<List<JsonNode>> workQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        ExecutorService executor = Executors.newFixedThreadPool(NUM_WORKERS);

        log.info("Starting import with {} worker threads, batch size: {}", NUM_WORKERS, BATCH_SIZE);

        // Start worker threads
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < NUM_WORKERS; i++) {
            futures.add(executor.submit(new Worker(workQueue)));
        }

        // Read and distribute batches
        try (InputStream is = Files.newInputStream(path);
             JsonParser parser = JSON_FACTORY.createParser(is)) {

            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IOException("Expected top-level array");
            }

            List<JsonNode> batch = new ArrayList<>(BATCH_SIZE);
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                JsonNode systemNode = MAPPER.readTree(parser);
                batch.add(systemNode);

                if (batch.size() >= BATCH_SIZE) {
                    workQueue.put(batch);
                    batch = new ArrayList<>(BATCH_SIZE);
                }
            }

            // Submit remaining items
            if (!batch.isEmpty()) {
                workQueue.put(batch);
            }

            // Signal workers to stop
            for (int i = 0; i < NUM_WORKERS; i++) {
                workQueue.put(List.of()); // poison pill
            }
        }

        // Wait for all workers to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                log.error("Worker failed", e.getCause());
            }
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        System.out.printf("Import finished. Processed: %,d | Upserted: %,d%n",
                processedSystems.get(), upsertedSystems.get());
    }

    private void processSystem(JsonNode sys) {
        long sysAddr = sys.path("id64").asLong(0);
        if (sysAddr == 0) return; // skip invalid

        String name = sys.path("name").asText(null);
        if (name == null || name.isBlank()) return;

        JsonNode coords = sys.path("coords");
        double x = coords.path("x").asDouble(0);
        double y = coords.path("y").asDouble(0);
        double z = coords.path("z").asDouble(0);

        // Upsert core system (even if no bodies)
        EDDN_JournalDto eddnDto = new EDDN_JournalDto();
        eddnDto.setSystemAddress(sysAddr);
        eddnDto.setStarSystem(name);
        eddnDto.setStarPos(List.of(x, y, z));
        SINGLETONS.getStarSystemManager().save(eddnDto);

        upsertedSystems.incrementAndGet();

        JsonNode bodies = sys.path("bodies");
        if (!bodies.isArray() || bodies.isEmpty()) return;

        for (JsonNode body : bodies) {
            saveBodyAsStellarObject(body, name, sysAddr, x, y, z);
        }
    }

    private void saveBodyAsStellarObject(JsonNode body, String sysName, long sysAddr, double x, double y, double z) {
        try {
            BootstrapEntryDto entry = MAPPER.treeToValue(body, BootstrapEntryDto.class);
            if ("Barycentre".equalsIgnoreCase(entry.getBodyType())) {
                //skip
            } else if ("Star".equalsIgnoreCase(entry.getBodyType())) {
                SINGLETONS.getStarSystemManager().saveBootStrapData(sysName, sysAddr, x, y, z);
                log.info("Saved " + entry.getBodyType() + " " + entry.getSystemAddress());
            } else if ("Planet".equalsIgnoreCase(entry.getBodyType())) {
                SINGLETONS.getStellarObjectManager().saveBootStrapData(entry, sysAddr, x, y, z);
                log.info("Saved " + entry.getBodyType() + " " + entry.getSystemAddress());
            } else {
                System.out.println(entry.toJson());
            }
        } catch (Exception e) {
            log.error("Failed to parse body: {}", body.path("name").asText(), e);
        }
    }

    // Worker thread that processes batches
    private class Worker implements Runnable {
        private final BlockingQueue<List<JsonNode>> workQueue;

        Worker(BlockingQueue<List<JsonNode>> workQueue) {
            this.workQueue = workQueue;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    List<JsonNode> batch = workQueue.take();

                    // Empty list is poison pill
                    if (batch.isEmpty()) {
                        break;
                    }

                    // Process batch
                    for (JsonNode systemNode : batch) {
                        processSystem(systemNode);
                    }

                    long processed = processedSystems.addAndGet(batch.size());
                    if (processed % 100_000 < batch.size()) {
                        System.out.printf("Processed %,d systems | Upserted: %,d%n",
                                processed, upsertedSystems.get());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Worker interrupted", e);
            }
        }
    }
}