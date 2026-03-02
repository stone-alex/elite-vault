package elite.vault.api.services;

import elite.vault.db.dao.SystemDao;
import elite.vault.db.managers.StarSystemManager;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;

import java.util.*;

import static elite.vault.Singletons.INSTANCE;

public class CarrierRouteService {

    private static final double CARRIER_MAX_JUMP_LY = 500.0;
    private static final double MAX_JUMP_SQ = CARRIER_MAX_JUMP_LY * CARRIER_MAX_JUMP_LY;

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
                    @OpenApiParam(name = "to", required = true, description = "Destination system name (e.g. Colonia)")
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

        if (from == null || from.trim().isEmpty() || to == null || to.trim().isEmpty()) {
            ctx.status(HttpStatus.BAD_REQUEST).json(
                    new ErrorResponse("Missing or empty 'from' and/or 'to' parameters")
            );
            return;
        }

        StarSystemManager mgr = INSTANCE.getStarSystemManager();

        try {
            SystemDao.StarSystem start = mgr.findByName(from.trim());
            SystemDao.StarSystem goal = mgr.findByName(to.trim());

            if (start == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                        .json(new ErrorResponse("Start system not found in vault: " + from));
                return;
            }
            if (goal == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                        .json(new ErrorResponse("Destination system not found in vault: " + to));
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

            RouteResult result = computeRoute(start, goal, mgr);

            ctx.json(new RouteResponse(
                    from, to, result.jumps(),
                    result.path(),
                    result.jumps() > 0 ? null : "No route found (possibly disconnected at 500 ly)"
            ));

        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("Route calculation failed: " + e.getMessage()));
        }
    }

    private static RouteResult computeRoute(
            SystemDao.StarSystem start,
            SystemDao.StarSystem goal,
            StarSystemManager mgr) {

        Map<String, String> cameFrom = new HashMap<>();
        Map<String, Integer> gScore = new HashMap<>();
        Map<String, Double> fScore = new HashMap<>();

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));

        String startName = start.getStarName();
        gScore.put(startName, 0);
        double h = heuristic(start, goal);
        fScore.put(startName, h);
        openSet.add(new Node(start, 0, h));

        while (!openSet.isEmpty()) {
            Node currNode = openSet.poll();
            SystemDao.StarSystem current = currNode.obj;
            String currName = current.getStarName();

            if (currNode.fScore > fScore.getOrDefault(currName, Double.POSITIVE_INFINITY)) {
                continue;
            }

            if (currName.equals(goal.getStarName())) {
                return new RouteResult(reconstructPath(cameFrom, currName), gScore.get(currName));
            }

            int tentativeG = gScore.get(currName) + 1;

            List<SystemDao.StarSystem> neighbors = mgr.findNeighbors(
                    current.getX() - CARRIER_MAX_JUMP_LY, current.getX() + CARRIER_MAX_JUMP_LY,
                    current.getY() - CARRIER_MAX_JUMP_LY, current.getY() + CARRIER_MAX_JUMP_LY,
                    current.getZ() - CARRIER_MAX_JUMP_LY, current.getZ() + CARRIER_MAX_JUMP_LY,
                    current.getX(), current.getY(), current.getZ(),
                    currName
            );

            for (SystemDao.StarSystem neigh : neighbors) {
                String nName = neigh.getStarName();
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
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) / CARRIER_MAX_JUMP_LY);
    }

    private static List<String> reconstructPath(Map<String, String> cameFrom, String current) {
        List<String> path = new ArrayList<>();
        while (current != null) {
            path.add(current);
            current = cameFrom.get(current);
        }
        Collections.reverse(path);
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