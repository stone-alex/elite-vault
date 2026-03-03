package elite.vault.api.services;

import elite.vault.eddn.dto.CommodityItemDto;
import io.javalin.http.Context;
import io.javalin.openapi.*;

import java.util.List;

import static elite.vault.Singletons.SINGLETONS;

public class CommoditiesService {

    @OpenApi(
            summary = "Search commodities in local EDDN vault",
            description = "Filter market data by commodity, system, station, demand/supply, min profit. Paginated results from your stored data.",
            operationId = "searchCommodities",
            tags = {"Commodities", "Market"},
            path = "/api/v1/search/commodities",
            methods = {HttpMethod.GET},
            queryParams = {
                    @OpenApiParam(name = "commodity", description = "Commodity name (e.g. Painite)"),
                    @OpenApiParam(name = "hasDemand", type = Boolean.class, description = "Only entries with demand > 0")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = {@OpenApiContent(from = CommodityItemDto[].class)}),
                    @OpenApiResponse(status = "500", content = {@OpenApiContent(from = String.class)})
            }
    )
    public static void searchCommodities(Context ctx) {
        String commodity = ctx.queryParam("commodity");
        boolean hasDemand = "true".equals(ctx.queryParam("hasDemand"));

        // Call your manager (add method if needed)
        List<CommodityItemDto> results = SINGLETONS.getMarketManager().findCommodities(commodity, hasDemand);
        ctx.json(results);
    }
}
