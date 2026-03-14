package elite.vault.api.services;

import elite.vault.api.dto.API_TradeRouteDto;
import elite.vault.api.jobs.JobDtos;
import elite.vault.api.jobs.JobStore;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.vault.Singletons.SINGLETONS;

public class TradeRouteService {

    private static final Logger log = LogManager.getLogger(TradeRouteService.class);

    private static final JobStore<API_TradeRouteDto> JOB_STORE = new JobStore<>("trade-route", 2);

    // -------------------------------------------------------------------------
    // POST /api/v1/search/traderoute
    // -------------------------------------------------------------------------
    @OpenApi(
            summary = "Queue a trade route calculation",
            description = """
                    Queues an async trade route job. Returns a job ID immediately.
                    Poll GET /api/v1/search/traderoute/{job} for results.
                    Supply x+y+z directly — saves a DB round-trip and works even when
                    the player is in a system with no market.
                    """,
            operationId = "queueTradeRoute",
            tags = {"Commodities", "Market"},
            path = "/api/v1/search/traderoute",
            methods = {HttpMethod.POST},
            queryParams = {
                    @OpenApiParam(name = "x", type = Double.class, description = "Player X coordinate (galactic)"),
                    @OpenApiParam(name = "y", type = Double.class, description = "Player Y coordinate (galactic)"),
                    @OpenApiParam(name = "z", type = Double.class, description = "Player Z coordinate (galactic)"),
                    @OpenApiParam(name = "numHops", type = Integer.class, description = "Number of trade hops (default 3, max 20)"),
                    @OpenApiParam(name = "hopDistance", type = Double.class, description = "Max ly between stops (default 250)"),
                    @OpenApiParam(name = "maxDistToArrival", type = Double.class, description = "Max station distance from entry in ls (default 6000)"),
                    @OpenApiParam(name = "cargoCap", type = Integer.class, description = "Cargo capacity in units (default 512)"),
                    @OpenApiParam(name = "requireLargePad", type = Boolean.class, description = "Require large landing pad"),
                    @OpenApiParam(name = "requireMediumPad", type = Boolean.class, description = "Require at least medium landing pad"),
                    @OpenApiParam(name = "allowPlanetary", type = Boolean.class, description = "Allow planetary/surface stations")
            },
            responses = {
                    @OpenApiResponse(status = "202", description = "Job queued",
                            content = {@OpenApiContent(from = JobDtos.JobAcceptedResponse.class)}),
                    @OpenApiResponse(status = "400", description = "Missing parameters",
                            content = {@OpenApiContent(from = JobDtos.JobErrorResponse.class)})
            }
    )
    public static void queueTradeRoute(Context ctx) {
        double x = ctx.queryParamAsClass("x", Double.class).getOrDefault(0d);
        double y = ctx.queryParamAsClass("y", Double.class).getOrDefault(0d);
        double z = ctx.queryParamAsClass("z", Double.class).getOrDefault(0d);
        int numHops = ctx.queryParamAsClass("numHops", Integer.class).getOrDefault(3);
        double hopDistance = ctx.queryParamAsClass("hopDistance", Double.class).getOrDefault(250.0);
        double maxDistToArrival = ctx.queryParamAsClass("maxDistToArrival", Double.class).getOrDefault(6000.0);
        int cargoCap = ctx.queryParamAsClass("cargoCap", Integer.class).getOrDefault(512);
        boolean requireLargePad = ctx.queryParamAsClass("requireLargePad", Boolean.class).getOrDefault(false);
        boolean requireMediumPad = ctx.queryParamAsClass("requireMediumPad", Boolean.class).getOrDefault(false);
        boolean allowPlanetary = ctx.queryParamAsClass("allowPlanetary", Boolean.class).getOrDefault(false);

        // Sanity bounds
        final int numHopsFinal = Math.max(1, Math.min(numHops, 20));
        final double hopDistanceFinal = Math.max(1.0, Math.min(hopDistance, 5000.0));
        final double maxDistToArrivalFinal = maxDistToArrival;
        final int cargoCapFinal = Math.max(1, Math.min(cargoCap, 10000));
        final double xF = x, yF = y, zF = z;
        final boolean largePadFinal = requireLargePad;
        final boolean mediumPadFinal = requireMediumPad;
        final boolean planetaryFinal = allowPlanetary;

        String jobId = JOB_STORE.submit(() ->
                SINGLETONS.getMarketManager().calculateTradeRoute(
                        xF, yF, zF,
                        numHopsFinal, hopDistanceFinal, maxDistToArrivalFinal,
                        largePadFinal, mediumPadFinal, planetaryFinal, cargoCapFinal)
        );

        ctx.status(HttpStatus.ACCEPTED).json(new JobDtos.JobAcceptedResponse(jobId));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/search/traderoute/{job}
    // -------------------------------------------------------------------------
    @OpenApi(
            summary = "Poll for trade route result",
            description = "Returns 202 while calculating. Returns 200 with route when complete — job flushed on delivery. " +
                    "Returns 404 if job unknown or expired.",
            operationId = "getTradeRoute",
            tags = {"Commodities", "Market"},
            path = "/api/v1/search/traderoute/{job}",
            methods = {HttpMethod.GET},
            pathParams = {
                    @OpenApiParam(name = "job", required = true, description = "Job ID returned by POST")
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Route ready",
                            content = {@OpenApiContent(from = API_TradeRouteDto.class)}),
                    @OpenApiResponse(status = "202", description = "Still calculating",
                            content = {@OpenApiContent(from = JobDtos.JobAcceptedResponse.class)}),
                    @OpenApiResponse(status = "404", description = "Job not found or expired",
                            content = {@OpenApiContent(from = JobDtos.JobErrorResponse.class)})
            }
    )
    public static void getTradeRoute(Context ctx) {
        String jobId = ctx.pathParam("job");
        JobStore.Job<API_TradeRouteDto> job = JOB_STORE.poll(jobId);

        if (job == null) {
            ctx.status(HttpStatus.NOT_FOUND).json(
                    new JobDtos.JobErrorResponse("Job not found or expired: " + jobId));
            return;
        }
        if (!job.isDone()) {
            ctx.status(HttpStatus.ACCEPTED).json(new JobDtos.JobAcceptedResponse(jobId));
            return;
        }

        JOB_STORE.flush(jobId);

        if (job.isFailed()) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(
                    new JobDtos.JobErrorResponse(job.getError()));
            return;
        }

        API_TradeRouteDto result = job.getResult();
        if (result == null || result.getRoute() == null || result.getRoute().isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).json(
                    new JobDtos.JobErrorResponse("No profitable trade route found from the given position."));
            return;
        }

        ctx.status(HttpStatus.OK).json(result);
    }
}