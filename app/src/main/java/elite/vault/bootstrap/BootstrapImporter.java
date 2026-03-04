package elite.vault.bootstrap;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import elite.vault.eddn.dto.CompositionDto;
import elite.vault.eddn.dto.ScanDto;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static elite.vault.Singletons.SINGLETONS;

public class BootstrapImporter {

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
        System.out.println("Saved " + name);

        upsertedSystems++;

        JsonNode bodies = sys.path("bodies");
        if (!bodies.isArray() || bodies.isEmpty()) return;

        for (JsonNode body : bodies) {
            saveBodyAsStellarObject(body, name, sysAddr, x, y, z);
        }
    }

    private void saveBodyAsStellarObject(JsonNode body, String sysName, long sysAddr, double x, double y, double z) {
        double distanceToArrival = body.path("distanceToArrival").asDouble(0);
        if (distanceToArrival == 0) return; // this is the primary star.

        ScanDto entry = new ScanDto();
        entry.setStarSystem(sysName);
        entry.setSystemAddress(sysAddr);
        entry.setStarPos(List.of(x, y, z));
        entry.setBodyId(body.path("bodyId").asLong(0));
        entry.setBodyName(body.path("name").asText(null));
        entry.setDistanceFromArrivalLs(distanceToArrival);
        entry.setMassEm(body.path("earthMasses").asDouble(0));
        entry.setRadius(body.path("radius").asDouble(0));
        entry.setSurfaceTemperature(body.path("surfaceTemperature").asDouble(0));
        entry.setSurfaceGravity(body.path("gravity").asDouble(0));
        entry.setAtmosphere(body.path("atmosphere").asText(null));
        entry.setType(body.path("type").asText(null));
        entry.setSubType(body.path("subType").asText(null));
        entry.setMainStar(body.path("mainStar").asBoolean(false));
        entry.setLandable(body.path("isLandable").asBoolean(false));
        entry.setOrbitalPeriod(body.path("orbitalPeriod").asDouble(0));
        entry.setOrbitalInclination(body.path("orbitalInclination").asDouble(0));


        CompositionDto composition = new CompositionDto();
        JsonNode solidNode = body.path("solidComposition");
        if (solidNode.isObject()) {
            composition.setIce(solidNode.path("Ice").asDouble());
            composition.setMetal(solidNode.path("Metal").asDouble());
            composition.setRock(solidNode.path("Rock").asDouble());
        }

        JsonNode atmoNode = body.path("atmosphereComposition");
        if (atmoNode.isObject()) {
            Map<String, Double> atmoMap = new HashMap<>();
            atmoNode.properties().forEach(field -> {
                String key = field.getKey();
                Double value = field.getValue().asDouble();  // null if not number
                atmoMap.put(key, value);
            });
            composition.setAtmosphere(atmoMap);
        }
        entry.setComposition(composition);
        System.out.println(entry.getBodyName() + " " + entry.getType() + " " + entry.getSubType());
        SINGLETONS.getStellarObjectManager().save(entry);
    }
}