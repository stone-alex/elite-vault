package elite.vault.api.services;

import elite.vault.api.dto.API_HuntingGroundDto;
import elite.vault.api.jobs.JobDtos;
import elite.vault.api.jobs.JobStore;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static elite.vault.Singletons.SINGLETONS;
import static elite.vault.util.NumUtils.getDoubleSafely;

public class PirateHuntingService {

    private static final Logger log = LogManager.getLogger(PirateHuntingService.class);
    private static final double DEFAULT_RANGE_LY = 50.0;
    private static final double MAX_RANGE_LY = 200.0;

    private static final JobStore<List<API_HuntingGroundDto>> JOB_STORE = new JobStore<>("pirate-hunting", 2);

    // -------------------------------------------------------------------------
    // POST /api/v1/pirate/hunting-grounds
    // -------------------------------------------------------------------------
    @OpenApi(
            summary = "Queue a pirate hunting grounds search",
            description = """
                    Queues an async search for pirate hunting grounds near a location.
                    Returns a job ID immediately. Poll GET /api/v1/pirate/hunting-grounds/{job} for results.
                    Returns star systems within range that have both Resource Extraction Sites
                    and known pirate factions present. Results ordered by RES grade
                    (Hazardous first) then by confirmation count. RES data older than 14 days excluded.
                    """,
            operationId = "queuePirateHuntingGrounds",
            tags = {"Pirate", "Hunting"},
            path = "/api/v1/pirate/hunting-grounds",
            methods = {HttpMethod.POST},
            queryParams = {
                    @OpenApiParam(name = "x", type = Double.class, required = true, description = "X coordinate of search origin"),
                    @OpenApiParam(name = "y", type = Double.class, required = true, description = "Y coordinate of search origin"),
                    @OpenApiParam(name = "z", type = Double.class, required = true, description = "Z coordinate of search origin"),
                    @OpenApiParam(name = "rangeLy", type = Double.class, required = false, description = "Search radius in ly (default 50, max 200)")
            },
            responses = {
                    @OpenApiResponse(status = "202", description = "Job queued",
                            content = {@OpenApiContent(from = JobDtos.JobAcceptedResponse.class)}),
                    @OpenApiResponse(status = "400", description = "Missing x/y/z parameters",
                            content = {@OpenApiContent(from = JobDtos.JobErrorResponse.class)})
            }
    )
    public static void queueHuntingGrounds(Context ctx) {
        String xParam = ctx.queryParam("x");
        String yParam = ctx.queryParam("y");
        String zParam = ctx.queryParam("z");

        if (xParam == null || yParam == null || zParam == null) {
            ctx.status(HttpStatus.BAD_REQUEST).json(
                    new JobDtos.JobErrorResponse("Required parameters: x, y, z"));
            return;
        }

        double x = getDoubleSafely(xParam);
        double y = getDoubleSafely(yParam);
        double z = getDoubleSafely(zParam);
        double rangeLy = getDoubleSafely(ctx.queryParam("rangeLy"));
        if (rangeLy <= 0) rangeLy = DEFAULT_RANGE_LY;
        if (rangeLy > MAX_RANGE_LY) rangeLy = MAX_RANGE_LY;

        final double xF = x, yF = y, zF = z, rangeF = rangeLy;

        String jobId = JOB_STORE.submit(() ->
                SINGLETONS.getPirateHuntingGroundsManager().findHuntingGrounds(xF, yF, zF, rangeF)
        );

        ctx.status(HttpStatus.ACCEPTED).json(new JobDtos.JobAcceptedResponse(jobId));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/pirate/hunting-grounds/{job}
    // -------------------------------------------------------------------------
    @OpenApi(
            summary = "Poll for pirate hunting grounds result",
            description = "Returns 202 while calculating. Returns 200 with results when complete — job flushed on delivery. " +
                    "Returns 404 if job unknown or expired.",
            operationId = "getPirateHuntingGrounds",
            tags = {"Pirate", "Hunting"},
            path = "/api/v1/pirate/hunting-grounds/{job}",
            methods = {HttpMethod.GET},
            pathParams = {
                    @OpenApiParam(name = "job", required = true, description = "Job ID returned by POST")
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Results ready",
                            content = {@OpenApiContent(from = API_HuntingGroundDto[].class)}),
                    @OpenApiResponse(status = "202", description = "Still calculating",
                            content = {@OpenApiContent(from = JobDtos.JobAcceptedResponse.class)}),
                    @OpenApiResponse(status = "404", description = "Job not found or expired",
                            content = {@OpenApiContent(from = JobDtos.JobErrorResponse.class)})
            }
    )
    public static void getHuntingGrounds(Context ctx) {
        String jobId = ctx.pathParam("job");
        JobStore.Job<List<API_HuntingGroundDto>> job = JOB_STORE.poll(jobId);

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