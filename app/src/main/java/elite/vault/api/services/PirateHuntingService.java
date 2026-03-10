package elite.vault.api.services;

import elite.vault.api.dto.API_HuntingGroundDto;
import io.javalin.http.Context;
import io.javalin.openapi.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.vault.Singletons.SINGLETONS;
import static elite.vault.util.NumUtils.getDoubleSafely;

public class PirateHuntingService {

    private static final Logger log = LogManager.getLogger(PirateHuntingService.class);

    private static final double DEFAULT_RANGE_LY = 50.0;
    private static final double MAX_RANGE_LY = 200.0;

    @OpenApi(
            summary = "Find pirate hunting grounds near a location",
            description = """
                    Returns star systems within range that have both Resource Extraction Sites
                    and known pirate factions present. Results are ordered by RES grade
                    (Hazardous first, then High, Normal, Low) then by commander confirmation count.
                    RES data older than 14 days is excluded.
                    """,
            operationId = "findPirateHuntingGrounds",
            tags = {"Pirate", "Hunting"},
            path = "/api/v1/pirate/hunting-grounds",
            methods = {HttpMethod.GET},
            queryParams = {
                    @OpenApiParam(name = "x", type = Double.class, description = "X coordinate of search origin", required = true),
                    @OpenApiParam(name = "y", type = Double.class, description = "Y coordinate of search origin", required = true),
                    @OpenApiParam(name = "z", type = Double.class, description = "Z coordinate of search origin", required = true),
                    @OpenApiParam(name = "rangeLy", type = Double.class, description = "Search radius in light years (default 50, max 200)")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = {@OpenApiContent(from = API_HuntingGroundDto[].class)}),
                    @OpenApiResponse(status = "400", content = {@OpenApiContent(from = String.class)}),
                    @OpenApiResponse(status = "500", content = {@OpenApiContent(from = String.class)})
            }
    )
    public static void findHuntingGrounds(Context ctx) {
        String xParam = ctx.queryParam("x");
        String yParam = ctx.queryParam("y");
        String zParam = ctx.queryParam("z");

        if (xParam == null || yParam == null || zParam == null) {
            ctx.status(400).json("Required parameters: x, y, z");
            return;
        }

        double x = getDoubleSafely(xParam);
        double y = getDoubleSafely(yParam);
        double z = getDoubleSafely(zParam);
        double rangeLy = getDoubleSafely(ctx.queryParam("rangeLy"));

        if (rangeLy <= 0) rangeLy = DEFAULT_RANGE_LY;
        if (rangeLy > MAX_RANGE_LY) rangeLy = MAX_RANGE_LY;

        ctx.json(SINGLETONS.getPirateHuntingGroundsManager().findHuntingGrounds(x, y, z, rangeLy));
    }
}