package elite.vault.api.services;

import elite.vault.api.dto.API_CommodityDto;
import elite.vault.api.jobs.JobDtos;
import elite.vault.api.jobs.JobStore;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static elite.vault.Singletons.SINGLETONS;
import static elite.vault.util.NumUtils.getIntSafely;

public class CommoditiesService {

    private static final Logger log = LogManager.getLogger(CommoditiesService.class);

    private static final JobStore<List<API_CommodityDto>> JOB_STORE = new JobStore<>("commodities", 2);

    // -------------------------------------------------------------------------
    // POST /api/v1/search/commodities
    // -------------------------------------------------------------------------
    @OpenApi(
            summary = "Queue a commodity search",
            description = "Queues an async commodity search. Returns a job ID immediately. " +
                    "Poll GET /api/v1/search/commodities/{job} for results.",
            operationId = "queueCommoditySearch",
            tags = {"Commodities", "Market"},
            path = "/api/v1/search/commodities",
            methods = {HttpMethod.POST},
            queryParams = {
                    @OpenApiParam(name = "commodity", description = "Commodity name (e.g. Painite)"),
                    @OpenApiParam(name = "startingLocationStarSystem", description = "Starting system name (e.g. Sol)"),
                    @OpenApiParam(name = "maxDistance", type = Integer.class, description = "Search radius in ly (default 50)")
            },
            responses = {
                    @OpenApiResponse(status = "202", description = "Job queued",
                            content = {@OpenApiContent(from = JobDtos.JobAcceptedResponse.class)})
            }
    )
    public static void queueCommoditySearch(Context ctx) {
        String commodity = ctx.queryParam("commodity");
        String startingLocationStarSystem = ctx.queryParam("startingLocationStarSystem");
        int maxDistance = getIntSafely(ctx.queryParam("maxDistance"));
        if (maxDistance == 0) maxDistance = 50;

        final String commodityFinal = commodity;
        final String systemFinal = startingLocationStarSystem;
        final int distanceFinal = maxDistance;

        String jobId = JOB_STORE.submit(() ->
                SINGLETONS.getMarketManager().findCommodities(commodityFinal, systemFinal, distanceFinal)
        );

        ctx.status(HttpStatus.ACCEPTED).json(new JobDtos.JobAcceptedResponse(jobId));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/search/commodities/{job}
    // -------------------------------------------------------------------------
    @OpenApi(
            summary = "Poll for commodity search result",
            description = "Returns 202 while calculating. Returns 200 with results when complete — job flushed on delivery. " +
                    "Returns 404 if job unknown or expired.",
            operationId = "getCommoditySearch",
            tags = {"Commodities", "Market"},
            path = "/api/v1/search/commodities/{job}",
            methods = {HttpMethod.GET},
            pathParams = {
                    @OpenApiParam(name = "job", required = true, description = "Job ID returned by POST")
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Results ready",
                            content = {@OpenApiContent(from = API_CommodityDto[].class)}),
                    @OpenApiResponse(status = "202", description = "Still calculating",
                            content = {@OpenApiContent(from = JobDtos.JobAcceptedResponse.class)}),
                    @OpenApiResponse(status = "404", description = "Job not found or expired",
                            content = {@OpenApiContent(from = JobDtos.JobErrorResponse.class)})
            }
    )
    public static void getCommoditySearch(Context ctx) {
        String jobId = ctx.pathParam("job");
        JobStore.Job<List<API_CommodityDto>> job = JOB_STORE.poll(jobId);

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

        ctx.status(HttpStatus.OK).json(job.getResult());
    }
}