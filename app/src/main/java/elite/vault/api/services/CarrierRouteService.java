package elite.vault.api.services;

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
    private static final Logger log = LogManager.getLogger(CarrierRouteService.class);

    @OpenApi(
            summary = "Find optimal fleet carrier route",
            description = "Computes the shortest carrier route (fewest jumps ≤ 500 ly each) " +
                    "between two star systems using local EDDN stellar data. " +
                    "Uses A* with admissible heuristic for optimality. " +
                    "Systems must exist in your vault (primary stars only).",
            operationId = "findCarrierRoute",
            tags = {"Navigation", "Carrier"},
            path = "/api/v1/search/carrier/route",
            methods = {HttpMethod.GET},
            queryParams = {
                    @OpenApiParam(name = "from", required = true, description = "Starting system name (e.g. Sol)"),
                    @OpenApiParam(name = "to", required = true, description = "Destination system name (e.g. Colonia)"),
                    @OpenApiParam(name = "jumpRange", required = true, type = Integer.class, description = "Distance in ly between jumps. Lower numbers are more accurate but slower")

            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Route found",
                            content = {@OpenApiContent(from = RouteResponse.class)}),
                    @OpenApiResponse(status = "400", description = "Missing parameters",
                            content = {@OpenApiContent(from = ErrorResponse.class)}),
                    @OpenApiResponse(status = "404", description = "System not found or no route exists",
                            content = {@OpenApiContent(from = ErrorResponse.class)}),
                    @OpenApiResponse(status = "500", description = "Internal error",
                            content = {@OpenApiContent(from = ErrorResponse.class)})
            }
    )
    public static void findCarrierRoute(Context ctx) {
        String from = ctx.queryParam("from");
        String to = ctx.queryParam("to");
        int jumpRange = getIntSafely(ctx.queryParam("jumpRange"));
        if (jumpRange == 0) jumpRange = 450;
        if (jumpRange > 500) jumpRange = 450;

        if (from == null || from.trim().isEmpty() || to == null || to.trim().isEmpty()) {
            ctx.status(HttpStatus.BAD_REQUEST).json(
                    new ErrorResponse("Missing or empty 'from' and/or 'to' parameters")
            );
            return;
        }

        StarSystemManager mgr = SINGLETONS.getStarSystemManager();

        try {
            SystemDao.StarSystem start = mgr.findByName(from.trim());
            SystemDao.StarSystem goal = mgr.findByName(to.trim());

            if (start == null) {
                ctx.status(HttpStatus.NOT_FOUND).json(new ErrorResponse("Start system not found in vault: " + from));
                return;
            }
            if (goal == null) {
                ctx.status(HttpStatus.NOT_FOUND).json(new ErrorResponse("Destination system not found in vault: " + to));
                return;
            }

            if (start.getStarName().equals(goal.getStarName())) {
                ctx.json(new RouteResponse(
                        from, to, 0,
                        List.of(start.getStarName()),
                        "Same system – no jumps required"
                ));
                return;
            }
            long startTime = System.currentTimeMillis();
            RouteResult result = computeRoute(jumpRange, start, goal, mgr);
            long endTime = System.currentTimeMillis();

            ctx.json(new RouteResponse(
                    from, to, result.jumps(),
                    result.path(),
                    result.jumps() > 0 ? "Route calculated in " + ((endTime - startTime) / 1000) + " seconds." : "No route found (possibly disconnected at 500 ly range to to lack of available data. try smaller jump range)"
            ));

        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(new ErrorResponse("Route calculation failed: " + e.getMessage()));
        }
    }

    private static RouteResult computeRoute(Integer jumpRange, SystemDao.StarSystem start, SystemDao.StarSystem goal, StarSystemManager mgr) {

        Map<String, String> cameFrom = new HashMap<>();
        Map<String, Integer> gScore = new HashMap<>();
        Map<String, Double> fScore = new HashMap<>();

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));

        String startName = start.getStarName();
        gScore.put(startName, 0);
        double h = heuristic(start, goal);
        fScore.put(startName, h);
        openSet.add(new Node(start, 0, h));
        long startTime = System.currentTimeMillis();

        while (!openSet.isEmpty()) {
            Node currNode = openSet.poll();
            SystemDao.StarSystem current = currNode.obj;
            String currName = current.getStarName();
            log.info("Processing node: " + currName + " " + current.getX() + " " + current.getY() + " " + current.getZ());
            if (currNode.fScore > fScore.getOrDefault(currName, Double.POSITIVE_INFINITY)) {
                continue;
            }

            if (currName.equals(goal.getStarName())) {
                long endTime = System.currentTimeMillis();
                log.info("Route found in " + (endTime - startTime) + "ms");

                /// NOTE TODO: fetch fuel spots before sending.
                return new RouteResult(reconstructPath(cameFrom, currName), gScore.get(currName));
            }
            int tentativeG = gScore.get(currName) + 1;

            double remainingLy = Math.hypot(Math.hypot(current.getX() - goal.getX(), current.getY() - goal.getY()), current.getZ() - goal.getZ());

            if (remainingLy < 500) jumpRange = 0;
            double minDistSq = jumpRange * jumpRange;
            log.info("Tentative Goal " + tentativeG + " " + currName + "\n");

            List<SystemDao.StarSystem> neighbors = mgr.findNeighbors(
                    current.getX() - CARRIER_MAX_JUMP_LY, current.getX() + CARRIER_MAX_JUMP_LY,
                    current.getY() - CARRIER_MAX_JUMP_LY, current.getY() + CARRIER_MAX_JUMP_LY,
                    current.getZ() - CARRIER_MAX_JUMP_LY, current.getZ() + CARRIER_MAX_JUMP_LY,
                    current.getX(), current.getY(), current.getZ(),   // current node
                    goal.getX(), goal.getY(), goal.getZ(),            // goal (forward cone)
                    minDistSq,
                    currName
            );

            for (SystemDao.StarSystem neigh : neighbors) {
                String nName = neigh.getStarName();
                log.info("Neighbor node: " + nName + " " + neigh.getX() + " " + neigh.getY() + " " + neigh.getZ());
                if (tentativeG < gScore.getOrDefault(nName, Integer.MAX_VALUE)) {
                    cameFrom.put(nName, currName);
                    gScore.put(nName, tentativeG);
                    double fNew = tentativeG + heuristic(neigh, goal);
                    fScore.put(nName, fNew);
                    openSet.add(new Node(neigh, tentativeG, fNew));
                }
            }
        }

        // No route found
        return new RouteResult(List.of(), -1);
    }


    private static double heuristic(SystemDao.StarSystem a, SystemDao.StarSystem b) {
        double dx = Math.abs(a.getX() - b.getX());
        double dy = Math.abs(a.getY() - b.getY());
        double dz = Math.abs(a.getZ() - b.getZ());
        double straight = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return Math.ceil(straight / 480.0);  // slight optimism: assume avg 480 ly effective
    }

    private static List<String> reconstructPath(Map<String, String> cameFrom, String current) {
        List<String> path = new ArrayList<>();

        // Start from the *second*-to-last node (skip origin)
        String at = current;
        while (at != null) {
            path.add(at);
            at = cameFrom.get(at);
        }

        Collections.reverse(path);

        // Remove the first element (which is now the start system)
        if (!path.isEmpty()) {
            path.removeFirst();
        }
        return path;
    }

    // DTOs for clean JSON
    public record RouteResponse(
            String from,
            String to,
            int jumps,
            List<String> route,
            String note
    ) {
    }

    public record ErrorResponse(String error) {
    }

    private record RouteResult(List<String> path, int jumps) {
    }

    private record Node(SystemDao.StarSystem obj, int g, double fScore) {
    }
}