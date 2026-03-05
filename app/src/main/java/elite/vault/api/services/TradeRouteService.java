package elite.vault.api.services;

import elite.vault.api.dto.API_TradeRouteDto;
import io.javalin.http.Context;
import io.javalin.openapi.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.vault.Singletons.SINGLETONS;

public class TradeRouteService {

    private static final Logger log = LogManager.getLogger(TradeRouteService.class);

    @OpenApi(
            summary = "Calculate trade route EDDN vault",
            description = "Filter market data by commodity, system, station, demand/supply, min profit. Paginated results from your stored data.",
            operationId = "calculateTradeRoute",
            tags = {"Commodities", "Market"},
            path = "/api/v1/search/traderoute",
            methods = {HttpMethod.GET},
            queryParams = {
                    @OpenApiParam(name = "startingLocationStarSystem", description = "Starting location star system name (e.g. Sol)"),
                    @OpenApiParam(name = "numTrades", type = Integer.class, description = "Number of trade hops (pair Buy/Sell)"),
                    @OpenApiParam(name = "numBudget", type = Integer.class, description = "Initial budget for purchases"),
                    @OpenApiParam(name = "maxDistanceFromEntrance", type = Integer.class, description = "Distance to port from entry point in light seconds"),
                    @OpenApiParam(name = "jumpRange", type = Integer.class, description = "Max jump range"),
                    @OpenApiParam(name = "requireLargeLandingPad", type = Boolean.class, description = "Require a large landing pad"),
                    @OpenApiParam(name = "requireMediumLandingPad", type = Boolean.class, description = "Require a large landing pad)"),
                    @OpenApiParam(name = "allowEnemyStrongHolds", type = Boolean.class, description = "Allow enemy strongholds (ports in systems controlled by rival powers)"),
                    @OpenApiParam(name = "allowProhibited", type = Boolean.class, description = "Allow enemy prohibited cargo (if true, you have to smuggle it and sell on black market)"),
                    @OpenApiParam(name = "allowPlanetaryLandings", type = Boolean.class, description = "Allow planetary landings for trade routes")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = {@OpenApiContent(from = API_TradeRouteDto.class)}),
                    @OpenApiResponse(status = "500", content = {@OpenApiContent(from = String.class)})
            }
    )

    public static void calculateTradeRoute(Context ctx) {
        try {
            String startSystem = ctx.queryParam("startingLocationStarSystem");
            int numTrades = ctx.queryParamAsClass("numTrades", Integer.class).getOrDefault(1);
            int maxDistanceFromEntrance = ctx.queryParamAsClass("maxDistanceFromEntrance", Integer.class).getOrDefault(6000);
            int jumpRange = ctx.queryParamAsClass("jumpRange", Integer.class).getOrDefault(6000);

            if (startSystem == null || startSystem.isBlank()) {
                ctx.status(400).result("startingLocationStarSystem is required");
                return;
            }

            API_TradeRouteDto route = SINGLETONS.getMarketManager().calculateTradeRoute(startSystem, numTrades, jumpRange, maxDistanceFromEntrance);

            if (route == null || route.getRoute() == null || route.getRoute().isEmpty()) {
                ctx.status(404).result("No profitable trade routes found from " + startSystem);
                return;
            }

            ctx.json(route);
        } catch (Exception e) {
            log.error("Error calculating trade route", e);
            ctx.status(500).result("Internal server error: " + e.getMessage());
        }
    }
}
