package elite.vault.api.services;

import elite.vault.api.jobs.JobDtos;
import elite.vault.api.jobs.JobStore;
import elite.vault.db.dao.SystemDao;
import elite.vault.db.managers.StarSystemManager;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static elite.vault.Singletons.SINGLETONS;
import static elite.vault.util.NumUtils.getIntSafely;

public class CarrierRouteService {

    private static final double CARRIER_MAX_JUMP_LY = 500.0;
    private static final int MAX_HOPS = 5000;
    private static final Logger log = LogManager.getLogger(CarrierRouteService.class);

    private static final JobStore<RouteResponse> JOB_STORE = new JobStore<>("carrier-route", 2);

    // -------------------------------------------------------------------------
    // POST /api/v1/search/carrier/route
    // -------------------------------------------------------------------------
    @OpenApi(
            summary = "Queue a fleet carrier route calculation",
            description = "Queues an async carrier route job. Returns a job ID immediately. " +
                    "Poll GET /api/v1/search/carrier/route/{job} for results.",
            operationId = "queueCarrierRoute",
            tags = {"Navigation", "Carrier"},
            path = "/api/v1/search/carrier/route",
            methods = {HttpMethod.POST},
            queryParams = {
                    @OpenApiParam(name = "from", required = true, description = "Starting system name"),
                    @OpenApiParam(name = "to", required = true, description = "Destination system name"),
                    @OpenApiParam(name = "jumpRange", required = false, type = Integer.class,
                            description = "Max jump distance in ly (≤ 500, default 450)")
            },
            responses = {
                    @OpenApiResponse(status = "202", description = "Job queued",
                            content = {@OpenApiContent(from = JobDtos.JobAcceptedResponse.class)}),
                    @OpenApiResponse(status = "400", description = "Missing parameters",
                            content = {@OpenApiContent(from = JobDtos.JobErrorResponse.class)})
            }
    )
    public static void queueCarrierRoute(Context ctx) {
        String from = ctx.queryParam("from");
        String to = ctx.queryParam("to");
        int jumpRange = getIntSafely(ctx.queryParam("jumpRange"));
        if (jumpRange <= 0 || jumpRange > 500) jumpRange = 450;

        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST).json(
                    new JobDtos.JobErrorResponse("Missing or empty 'from' and/or 'to' parameters"));
            return;
        }

        final String fromFinal = from.trim();
        final String toFinal = to.trim();
        final int jumpFinal = jumpRange;

        String jobId = JOB_STORE.submit(() -> computeRoute(fromFinal, toFinal, jumpFinal));
        ctx.status(HttpStatus.ACCEPTED).json(new JobDtos.JobAcceptedResponse(jobId));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/search/carrier/route/{job}
    // -------------------------------------------------------------------------
    @OpenApi(
            summary = "Poll for carrier route result",
            description = "Returns 202 while calculating. Returns 200 with route when complete — job flushed on delivery. " +
                    "Returns 404 if job unknown or expired.",
            operationId = "getCarrierRoute",
            tags = {"Navigation", "Carrier"},
            path = "/api/v1/search/carrier/route/{job}",
            methods = {HttpMethod.GET},
            pathParams = {
                    @OpenApiParam(name = "job", required = true, description = "Job ID returned by POST")
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Route ready",
                            content = {@OpenApiContent(from = RouteResponse.class)}),
                    @OpenApiResponse(status = "202", description = "Still calculating",
                            content = {@OpenApiContent(from = JobDtos.JobAcceptedResponse.class)}),
                    @OpenApiResponse(status = "404", description = "Job not found or expired",
                            content = {@OpenApiContent(from = JobDtos.JobErrorResponse.class)})
            }
    )
    public static void getCarrierRoute(Context ctx) {
        String jobId = ctx.pathParam("job");
        JobStore.Job<RouteResponse> job = JOB_STORE.poll(jobId);

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

    // -------------------------------------------------------------------------
    // Computation
    // -------------------------------------------------------------------------
    private static RouteResponse computeRoute(String from, String to, int jumpRange) {
        StarSystemManager mgr = SINGLETONS.getStarSystemManager();

        SystemDao.StarSystem start = mgr.findByName(from);
        SystemDao.StarSystem goal = mgr.findByName(to);

        if (start == null)
            return new RouteResponse(from, to, 0, List.of(), "Start system not found in vault: " + from);
        if (goal == null)
            return new RouteResponse(from, to, 0, List.of(), "Destination system not found in vault: " + to);
        if (start.getStarName().equals(goal.getStarName()))
            return new RouteResponse(from, to, 0, List.of(), "Same system – no jumps required");

        long startTime = System.currentTimeMillis();
        RouteResult result = search(jumpRange, start, goal, mgr);
        long elapsed = System.currentTimeMillis() - startTime;

        String note = result.jumps() > 0
                ? "Route calculated in " + elapsed + "ms"
                : "No route found – insufficient data coverage at " + jumpRange + " ly. Try a larger jump range.";

        return new RouteResponse(from, to, result.jumps(), result.path(), note);
    }

    private static RouteResult search(int jumpRange, SystemDao.StarSystem start,
                                      SystemDao.StarSystem goal, StarSystemManager mgr) {
        double minDistSq = (double) jumpRange * jumpRange;

        List<String> path = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Deque<BacktrackFrame> backtrackStack = new ArrayDeque<>();

        SystemDao.StarSystem current = start;
        visited.add(current.getStarName());

        while (true) {
            double remainingLy = dist3d(current, goal);

            if (remainingLy <= CARRIER_MAX_JUMP_LY) {
                path.add(goal.getStarName());
                return new RouteResult(path, path.size());
            }

            double effectiveMinDistSq = (remainingLy > CARRIER_MAX_JUMP_LY * 1.5) ? minDistSq : 0.0;
            double range = CARRIER_MAX_JUMP_LY;

            List<SystemDao.StarSystem> candidates = mgr.findNeighbors(
                    current.getX() - range, current.getX() + range,
                    current.getY() - range, current.getY() + range,
                    current.getZ() - range, current.getZ() + range,
                    current.getX(), current.getY(), current.getZ(),
                    goal.getX(), goal.getY(), goal.getZ(),
                    effectiveMinDistSq,
                    current.getStarName()
            );

            candidates.removeIf(s -> visited.contains(s.getStarName()));
            SystemDao.StarSystem next = candidates.isEmpty() ? null : candidates.getFirst();

            if (next == null) {
                if (backtrackStack.isEmpty()) return new RouteResult(List.of(), -1);
                BacktrackFrame frame = backtrackStack.peek();
                boolean found = false;
                for (int i = frame.nextCandidateIdx; i < frame.candidates.size(); i++) {
                    SystemDao.StarSystem alt = frame.candidates.get(i);
                    if (!visited.contains(alt.getStarName())) {
                        path.removeLast();
                        frame.nextCandidateIdx = i + 1;
                        current = alt;
                        visited.add(current.getStarName());
                        path.add(current.getStarName());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    backtrackStack.pop();
                    if (!path.isEmpty()) path.removeLast();
                    if (backtrackStack.isEmpty()) return new RouteResult(List.of(), -1);
                    current = backtrackStack.peek().system;
                }
            } else {
                backtrackStack.push(new BacktrackFrame(current, candidates, 1));
                current = next;
                visited.add(current.getStarName());
                path.add(current.getStarName());
            }

            if (path.size() > MAX_HOPS) {
                log.warn("Route search exceeded {} hops, aborting", MAX_HOPS);
                return new RouteResult(List.of(), -1);
            }
        }
    }

    private static double dist3d(SystemDao.StarSystem a, SystemDao.StarSystem b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static class BacktrackFrame {
        final SystemDao.StarSystem system;
        final List<SystemDao.StarSystem> candidates;
        int nextCandidateIdx;

        BacktrackFrame(SystemDao.StarSystem system, List<SystemDao.StarSystem> candidates, int nextIdx) {
            this.system = system;
            this.candidates = candidates;
            this.nextCandidateIdx = nextIdx;
        }
    }

    public record RouteResponse(String from, String to, int jumps, List<String> route, String note) {
    }

    private record RouteResult(List<String> path, int jumps) {
    }
}