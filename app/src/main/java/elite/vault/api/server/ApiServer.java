package elite.vault.api.server;

import elite.vault.api.search.CommoditiesSearch;
import elite.vault.db.managers.MarketManager;
import elite.vault.db.managers.StellarObjectManager;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;

import java.util.Map;

import static elite.vault.Singletons.INSTANCE;


public class ApiServer {

    private final MarketManager marketManager;
    private final StellarObjectManager stellarManager;
    private Javalin app;

    public ApiServer() {
        this.marketManager = INSTANCE.getMarketManager();
        this.stellarManager = INSTANCE.getStellarObjectManager();
    }

    public void start(int port) {
        app = Javalin.create(config -> {
            // Optional: built-in dev logging
            // config.bundledPlugins.enableDevLogging();

            // Register core OpenAPI plugin (serves /openapi)
            config.registerPlugin(new OpenApiPlugin(openApiConfig -> {
                openApiConfig.withDefinitionConfiguration((version, definition) -> {
                    definition.info(info -> {
                        info.title("Elite Vault API");
                        info.version("v1");
                        info.description("Self-hosted EDDN data vault for Elite Dangerous market & scan queries");
                    });
                });
            }));

            // Register Swagger UI (at /swagger)
            config.registerPlugin(new SwaggerPlugin(swaggerConfig -> {
                swaggerConfig.withTitle("Elite Vault API Docs");
            }));

            // Routes
            config.routes.get("/test", ctx -> ctx.result("Routes registered OK!"));
            config.routes.get("/api/v1/health", ctx -> ctx.result("OK"));
            config.routes.get("/api/v1/commodities/search", CommoditiesSearch::searchCommodities);
            config.routes.exception(Exception.class, (e, ctx) -> {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
            });

            config.routes.exception(Exception.class, (e, ctx) -> {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
            });
        }).start(port);

        System.out.println("Elite Vault API running on http://localhost:" + port);
        System.out.println("  - OpenAPI spec: http://localhost:" + port + "/openapi");
        System.out.println("  - Swagger UI:   http://localhost:" + port + "/swagger");
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }

/*

    @OpenApi(
            summary = "Search commodities in local EDDN vault",
            description = "Filter market data by commodity, system, station, demand/supply, min profit. Paginated results from your stored data.",
            operationId = "searchCommodities",
            tags = {"Commodities", "Market"},
            path = "/api/v1/commodities/search",
            methods = {HttpMethod.GET},
            queryParams = {
                    @OpenApiParam(name = "commodity", description = "Commodity name (e.g. Painite)"),
                    @OpenApiParam(name = "system", description = "System name (e.g. Sol)"),
                    @OpenApiParam(name = "station", description = "Station name"),
                    @OpenApiParam(name = "hasDemand", type = Boolean.class, description = "Only entries with demand > 0"),
                    @OpenApiParam(name = "minProfit", type = Integer.class, description = "Minimum sellPrice - buyPrice"),
                    @OpenApiParam(name = "limit", type = Integer.class, description = "Results per page", example = "50"),
                    @OpenApiParam(name = "offset", type = Integer.class, description = "Page offset", example = "0")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = {@OpenApiContent(from = CommodityItemDto[].class)}),
                    @OpenApiResponse(status = "500", content = {@OpenApiContent(from = String.class)})
            }
    )
    private void searchCommodities(Context ctx) {
        String commodity = ctx.queryParam("commodity");
        String system = ctx.queryParam("system");
        String station = ctx.queryParam("station");
        boolean hasDemand = "true".equals(ctx.queryParam("hasDemand"));
        int minProfit = ctx.queryParamAsClass("minProfit", Integer.class).getOrDefault(0);
        int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(50);
        int offset = ctx.queryParamAsClass("offset", Integer.class).getOrDefault(0);

        // Call your manager (add method if needed)
        List<CommodityItemDto> results = marketManager.findCommodities(
                commodity, system, station, hasDemand, minProfit, limit, offset
        );

        ctx.json(results);
    }
*/

    // Example for another endpoint
    // private void getStationsInSystem(Context ctx) { ... }
}