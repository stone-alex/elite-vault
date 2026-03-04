package elite.vault.eddn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import elite.vault.eddn.dto.EddnDto;
import elite.vault.eddn.events.EddnMessageEvent;
import elite.vault.eddn.events.EventBusManager;
import elite.vault.json.GsonFactory;
import elite.vault.util.ZMQUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class EdDnClient {

    private static final Logger log = LogManager.getLogger(EdDnClient.class);

    private static final String SUB_ENDPOINT = "tcp://eddn.edcd.io:9500";
    private static final Object lock = new Object();
    private static volatile EdDnClient instance;
    private final ZContext context;
    private final ZMQ.Socket subscriber;
    private final ExecutorService executor;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile boolean running = false;


    private EdDnClient() {
        context = new ZContext();
        subscriber = context.createSocket(SocketType.SUB);
        subscriber.connect(SUB_ENDPOINT);
        subscriber.subscribe("".getBytes(ZMQ.CHARSET));
        executor = Executors.newSingleThreadExecutor();
    }

    public static EdDnClient getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new EdDnClient();
                }
            }
        }
        return instance;
    }

    public void start() {
        if (running) return;
        startListening(jsonNode -> {
            try {
                JsonNode schemaRefNode = jsonNode.path("$schemaRef");
                if (!schemaRefNode.isTextual()) return;
                JsonNode messageNode = jsonNode.path("message");
                if (messageNode.isMissingNode()) return;

                EddnDto data = GsonFactory.getGson().fromJson(messageNode.toPrettyString(), EddnDto.class);
                System.out.print(".");
                EventBusManager.publish(new EddnMessageEvent(schemaRefNode.asText(), data, jsonNode.path("header")));

            } catch (Exception e) {
                log.error("Error processing EDDN message: {}", e.getMessage(), e);
            }
        });
    }


    ///
    public void startListening(Consumer<JsonNode> handler) {
        if (running) return;
        running = true;
        executor.submit(() -> {
            while (running) {
                byte[] compressed = subscriber.recv(0);
                if (compressed == null || compressed.length == 0) continue;

                byte[] decompressed = ZMQUtil.decompress(compressed);
                if (decompressed.length == 0) continue;

                try {
                    JsonNode msg = mapper.readTree(decompressed);
                    handler.accept(msg);
                } catch (Exception e) {
                    // silently skip malformed JSON
                }
            }
        });
    }


    ///
    public void stop() {
        if (!running) return;
        running = false;
        executor.shutdownNow();
        subscriber.close();
        context.destroy();
    }
}
