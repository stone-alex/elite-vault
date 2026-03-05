package elite.vault.api.services;

import elite.vault.eddn.dto.EDDN_CommodityItemDto;
import io.javalin.http.Context;
import io.javalin.openapi.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.vault.Singletons.SINGLETONS;
import static elite.vault.util.NumUtils.getIntSafely;

public class CommoditiesService {

    private static final Logger log = LogManager.getLogger(CommoditiesService.class);
    @OpenApi(
            summary = "Search commodities in local EDDN vault",
            description = "Filter market data by commodity, system, station, demand/supply, min profit. Paginated results from your stored data.",
            operationId = "searchCommodities",
            tags = {"Commodities", "Market"},
            path = "/api/v1/search/commodities",
            methods = {HttpMethod.GET},
            queryParams = {
                    @OpenApiParam(name = "commodity", description = "Commodity name (e.g. Painite)"),
                    @OpenApiParam(name = "startingLocationStarSystem", description = "Starting location star system name (e.g. Sol)"),
                    @OpenApiParam(name = "maxDistance", type = Integer.class, description = "Range in light years from the starting point")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = {@OpenApiContent(from = EDDN_CommodityItemDto[].class)}),
                    @OpenApiResponse(status = "500", content = {@OpenApiContent(from = String.class)})
            }
    )
    public static void searchCommodities(Context ctx) {
        String commodity = ctx.queryParam("commodity");
        String startingLocationStarSystem = ctx.queryParam("startingLocationStarSystem");
        int maxDistance = getIntSafely(ctx.queryParam("maxDistance"));
        if (maxDistance == 0) maxDistance = 50; // default fallback
        ctx.json(SINGLETONS.getMarketManager().findCommodities(commodity, startingLocationStarSystem, maxDistance));
    }
}
