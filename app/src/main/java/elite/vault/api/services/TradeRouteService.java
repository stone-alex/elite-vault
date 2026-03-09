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
            summary = "Calculate trade route",
            description = """
                    Calculates a multi-hop trade route starting from the given position.
                    Supply either startSystem (name lookup) or x+y+z directly.
                    x/y/z is preferred — saves a DB round-trip and works even when
                    the player is in a system with no market.
                    Returns ordered buy/sell pairs: where to go, what to buy, where to sell it.
                    """,
            operationId = "calculateTradeRoute",
            tags = {"Commodities", "Market"},
            path = "/api/v1/search/traderoute",
            methods = {HttpMethod.GET},
            queryParams = {
                    @OpenApiParam(name = "x", type = Double.class, description = "Player X coordinate (galactic)"),
                    @OpenApiParam(name = "y", type = Double.class, description = "Player Y coordinate (galactic)"),
                    @OpenApiParam(name = "z", type = Double.class, description = "Player Z coordinate (galactic)"),
                    @OpenApiParam(name = "numHops", type = Integer.class, description = "Number of trade hops to calculate (default 3)"),
                    @OpenApiParam(name = "hopDistance", type = Double.class, description = "Max distance in ly the player is willing to travel between stops (default 250)"),
                    @OpenApiParam(name = "maxDistToArrival", type = Double.class, description = "Max station distance from entry point in light-seconds (default 6000)"),
                    @OpenApiParam(name = "cargoCap", type = Integer.class, description = "Cargo capacity in units (default 512)"),
                    @OpenApiParam(name = "requireLargePad", type = Boolean.class, description = "Require a large landing pad at both stations"),
                    @OpenApiParam(name = "requireMediumPad", type = Boolean.class, description = "Require at least a medium landing pad (large pad also satisfies this)"),
                    @OpenApiParam(name = "allowPlanetary", type = Boolean.class, description = "Allow planetary / surface stations in the route")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = {@OpenApiContent(from = API_TradeRouteDto.class)}),
                    @OpenApiResponse(status = "400", content = {@OpenApiContent(from = String.class)}),
                    @OpenApiResponse(status = "404", content = {@OpenApiContent(from = String.class)}),
                    @OpenApiResponse(status = "500", content = {@OpenApiContent(from = String.class)})
            }
    )
    public static void calculateTradeRoute(Context ctx) {
        try {
            // ----------------------------------------------------------------
            // Position — prefer x/y/z, fall back to system name lookup
            // ----------------------------------------------------------------
            Double x = ctx.queryParamAsClass("x", Double.class).getOrDefault(0d);
            Double y = ctx.queryParamAsClass("y", Double.class).getOrDefault(0d);
            Double z = ctx.queryParamAsClass("z", Double.class).getOrDefault(0d);

            // ----------------------------------------------------------------
            // Route parameters
            // ----------------------------------------------------------------
            int numHops = ctx.queryParamAsClass("numHops", Integer.class).getOrDefault(3);
            double hopDistance = ctx.queryParamAsClass("hopDistance", Double.class).getOrDefault(250.0);
            double maxDistToArrival = ctx.queryParamAsClass("maxDistToArrival", Double.class).getOrDefault(6000.0);
            int cargoCap = ctx.queryParamAsClass("cargoCap", Integer.class).getOrDefault(512);
            boolean requireLargePad = ctx.queryParamAsClass("requireLargePad", Boolean.class).getOrDefault(false);
            boolean requireMediumPad = ctx.queryParamAsClass("requireMediumPad", Boolean.class).getOrDefault(false);
            boolean allowPlanetary = ctx.queryParamAsClass("allowPlanetary", Boolean.class).getOrDefault(false);

            // Sanity bounds
            numHops = Math.max(1, Math.min(numHops, 20));
            hopDistance = Math.max(1.0, Math.min(hopDistance, 5000.0));
            cargoCap = Math.max(1, Math.min(cargoCap, 10000));

            // ----------------------------------------------------------------
            // Dispatch
            // ----------------------------------------------------------------
            API_TradeRouteDto route;

            route = SINGLETONS.getMarketManager().calculateTradeRoute(
                    x, y, z,
                    numHops, hopDistance, maxDistToArrival,
                    requireLargePad, requireMediumPad, allowPlanetary, cargoCap);


            if (route.getRoute() == null || route.getRoute().isEmpty()) {
                ctx.status(404).result("No profitable trade route found from the given position.");
                return;
            }

            ctx.json(route);

        } catch (Exception e) {
            log.error("Error calculating trade route", e);
            ctx.status(500).result("Internal server error: " + e.getMessage());
        }
    }
}