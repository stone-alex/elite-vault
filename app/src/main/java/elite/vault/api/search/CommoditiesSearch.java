package elite.vault.api.search;

import elite.vault.eddn.dto.CommodityItemDto;
import io.javalin.http.Context;
import io.javalin.openapi.*;

import java.util.List;

import static elite.vault.Singletons.INSTANCE;

public class CommoditiesSearch {

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
    public static void searchCommodities(Context ctx) {
        String commodity = ctx.queryParam("commodity");
        String system = ctx.queryParam("system");
        String station = ctx.queryParam("station");
        boolean hasDemand = "true".equals(ctx.queryParam("hasDemand"));
        int minProfit = ctx.queryParamAsClass("minProfit", Integer.class).getOrDefault(0);
        int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(50);
        int offset = ctx.queryParamAsClass("offset", Integer.class).getOrDefault(0);

        // Call your manager (add method if needed)
        List<CommodityItemDto> results = INSTANCE.getMarketManager().findCommodities(
                commodity, system, station, hasDemand, minProfit, limit, offset
        );
        ctx.json(results);
    }
}
