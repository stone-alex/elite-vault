package elite.vault.api.server;

import elite.vault.api.services.CarrierRouteService;
import elite.vault.api.services.CommoditiesService;
import elite.vault.api.services.PirateHuntingService;
import elite.vault.api.services.TradeRouteService;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;

import java.util.Map;

public class ApiServer {

    private Javalin server;

    public void start(int port) {
        server = Javalin.create(config -> {
            config.registerPlugin(new OpenApiPlugin(openApiConfig -> {
                openApiConfig.withDefinitionConfiguration((version, definition) -> {
                    definition.info(info -> {
                        info.title("Elite Vault API");
                        info.version("v1");
                        info.description("Self-hosted EDDN data vault for Elite Dangerous");
                    });
                });
            }));

            config.registerPlugin(new SwaggerPlugin(swaggerConfig -> {
                swaggerConfig.withTitle("Elite Vault API Docs");
            }));

            /// Routes
            // Carrier route
            config.routes.post("/api/v1/search/carrier/route", CarrierRouteService::queueCarrierRoute);
            config.routes.get("/api/v1/search/carrier/route/{job}", CarrierRouteService::getCarrierRoute);

            // Trade route
            config.routes.post("/api/v1/search/traderoute", TradeRouteService::queueTradeRoute);
            config.routes.get("/api/v1/search/traderoute/{job}", TradeRouteService::getTradeRoute);

            // Commodities
            config.routes.post("/api/v1/search/commodities", CommoditiesService::queueCommoditySearch);
            config.routes.get("/api/v1/search/commodities/{job}", CommoditiesService::getCommoditySearch);

            // Pirate hunting
            config.routes.post("/api/v1/pirate/hunting-grounds", PirateHuntingService::queueHuntingGrounds);
            config.routes.get("/api/v1/pirate/hunting-grounds/{job}", PirateHuntingService::getHuntingGrounds);

            /// error
            config.routes.exception(Exception.class, (e, ctx) -> {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
            });
        }).start(port);

        System.out.println("Elite Vault API running on http://localhost:" + port);
        System.out.println("  - OpenAPI spec: http://localhost:" + port + "/openapi");
        System.out.println("  - Swagger UI:   http://localhost:" + port + "/swagger");
    }

    public void stop() {
        if (server != null) server.stop();
    }
}