package elite.vault.bootstrap;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import elite.vault.eddn.dto.ScanDto;
import elite.vault.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static elite.vault.Singletons.SINGLETONS;

public class BootstrapImporter {

    private static final Logger log = LogManager.getLogger(BootstrapImporter.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private static final BootstrapImporter instance = new BootstrapImporter();
    private long processedSystems = 0;
    private long upsertedSystems = 0;

    private BootstrapImporter() {
        // singleton!
    }

    public static BootstrapImporter getInstance() {
        return instance;
    }

    public void importFromFile(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path);
             JsonParser parser = JSON_FACTORY.createParser(is)) {

            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IOException("Expected top-level array");
            }

            while (parser.nextToken() == JsonToken.START_OBJECT) {
                JsonNode systemNode = MAPPER.readTree(parser);

                processSystem(systemNode);

                processedSystems++;
                if (processedSystems % 100_000 == 0) {
                    System.out.printf("Processed %,d systems | Upserted systems: %,d%n", processedSystems, upsertedSystems);
                }
            }

            System.out.println("Import finished. Processed: " + processedSystems);
        }
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
        ScanDto scanDto = new ScanDto();
        scanDto.setSystemAddress(sysAddr);
        scanDto.setStarSystem(name);
        scanDto.setStarPos(List.of(x, y, z));
        SINGLETONS.getStarSystemManager().save(scanDto);
        log.info("Saved " + name);

        upsertedSystems++;

        JsonNode bodies = sys.path("bodies");
        if (!bodies.isArray() || bodies.isEmpty()) return;

        for (JsonNode body : bodies) {
            saveBodyAsStellarObject(body, name, sysAddr, x, y, z);
        }
    }

    private void saveBodyAsStellarObject(JsonNode body, String sysName, long sysAddr, double x, double y, double z) {
        EntryDto entry = GsonFactory.getGson().fromJson(body.toPrettyString(), EntryDto.class);
        if ("Barycentre".equalsIgnoreCase(entry.getBodyType())) {
            //skip
        } else if ("Star".equalsIgnoreCase(entry.getBodyType())) {
            SINGLETONS.getStarSystemManager().saveBootStrapData(sysName, sysAddr, x, y, z);
        } else if ("Planet".equalsIgnoreCase(entry.getBodyType())) {
            SINGLETONS.getStellarObjectManager().saveBootStrapData(entry, sysName, sysAddr, x, y, z);
        } else {
            System.out.println(entry.toJson());
        }
    }
}