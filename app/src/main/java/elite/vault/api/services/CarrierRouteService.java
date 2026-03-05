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
    private static final double CARRIER_MAX_JUMP_SQ = CARRIER_MAX_JUMP_LY * CARRIER_MAX_JUMP_LY;
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

        // === Phase 1: Bulk-load all systems in a corridor ===
        double corridorPadding = CARRIER_MAX_JUMP_LY; // extra width around the straight line
        double minX = Math.min(start.getX(), goal.getX()) - corridorPadding;
        double maxX = Math.max(start.getX(), goal.getX()) + corridorPadding;
        double minY = Math.min(start.getY(), goal.getY()) - corridorPadding;
        double maxY = Math.max(start.getY(), goal.getY()) + corridorPadding;
        double minZ = Math.min(start.getZ(), goal.getZ()) - corridorPadding;
        double maxZ = Math.max(start.getZ(), goal.getZ()) + corridorPadding;

        log.info("Loading corridor systems...");
        long loadStart = System.currentTimeMillis();
        List<SystemDao.StarSystem> corridor = mgr.findSystemsInCorridor(minX, maxX, minY, maxY, minZ, maxZ);
        long loadEnd = System.currentTimeMillis();
        log.info("Loaded {} systems in {}ms", corridor.size(), loadEnd - loadStart);

        if (corridor.isEmpty()) {
            return new RouteResult(List.of(), -1);
        }

        // Build a lookup by name and ensure start/goal are present
        Map<String, SystemDao.StarSystem> byName = new HashMap<>(corridor.size());
        for (SystemDao.StarSystem s : corridor) {
            byName.put(s.getStarName(), s);
        }
        byName.putIfAbsent(start.getStarName(), start);
        byName.putIfAbsent(goal.getStarName(), goal);

        // === Phase 2: Build spatial grid for fast neighbor lookups in memory ===
        double cellSize = CARRIER_MAX_JUMP_LY;
        Map<Long, List<SystemDao.StarSystem>> grid = new HashMap<>();
        for (SystemDao.StarSystem s : byName.values()) {
            long key = gridKey(s.getX(), s.getY(), s.getZ(), cellSize);
            grid.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }

        // === Phase 3: Greedy walk with backtracking — entirely in memory ===
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

            // Find neighbors from grid (in memory — microseconds, not seconds)
            double effectiveMinDistSq = (remainingLy > CARRIER_MAX_JUMP_LY * 1.5) ? minDistSq : 0.0;
            List<SystemDao.StarSystem> candidates = findNeighborsInMemory(
                    current, goal, grid, cellSize, effectiveMinDistSq, visited
            );

            SystemDao.StarSystem next = candidates.isEmpty() ? null : candidates.getFirst();

            if (next == null) {
                // Dead end — backtrack
                if (backtrackStack.isEmpty()) {
                    return new RouteResult(List.of(), -1);
                }
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
                    if (backtrackStack.isEmpty()) {
                        return new RouteResult(List.of(), -1);
                    }
                    current = backtrackStack.peek().system;
                    continue;
                }
            } else {
                backtrackStack.push(new BacktrackFrame(current, candidates, 1));
                current = next;
                visited.add(current.getStarName());
                path.add(current.getStarName());
            }

            if (path.size() > 5000) {
                log.warn("Route search exceeded 5000 hops, aborting");
                return new RouteResult(List.of(), -1);
            }
        }
    }

    /**
     * In-memory neighbor search using spatial grid. Returns candidates sorted by
     * distance-to-goal ascending (closest to goal first), filtered by forward-cone
     * and jump range constraints.
     */
    private static List<SystemDao.StarSystem> findNeighborsInMemory(
            SystemDao.StarSystem current,
            SystemDao.StarSystem goal,
            Map<Long, List<SystemDao.StarSystem>> grid,
            double cellSize,
            double minDistSq,
            Set<String> visited
    ) {
        double cx = current.getX(), cy = current.getY(), cz = current.getZ();
        double gx = goal.getX(), gy = goal.getY(), gz = goal.getZ();
        double currentToGoalSq = (cx - gx) * (cx - gx) + (cy - gy) * (cy - gy) + (cz - gz) * (cz - gz);

        int cellRadius = 1; // 500 ly jump / 500 ly cell = check adjacent cells
        int baseCX = (int) Math.floor(cx / cellSize);
        int baseCY = (int) Math.floor(cy / cellSize);
        int baseCZ = (int) Math.floor(cz / cellSize);

        List<SystemDao.StarSystem> result = new ArrayList<>();

        for (int dx = -cellRadius; dx <= cellRadius; dx++) {
            for (int dy = -cellRadius; dy <= cellRadius; dy++) {
                for (int dz = -cellRadius; dz <= cellRadius; dz++) {
                    long key = packKey(baseCX + dx, baseCY + dy, baseCZ + dz);
                    List<SystemDao.StarSystem> cell = grid.get(key);
                    if (cell == null) continue;

                    for (SystemDao.StarSystem s : cell) {
                        if (visited.contains(s.getStarName())) continue;

                        double ddx = s.getX() - cx, ddy = s.getY() - cy, ddz = s.getZ() - cz;
                        double distSq = ddx * ddx + ddy * ddy + ddz * ddz;

                        if (distSq > CARRIER_MAX_JUMP_SQ) continue;  // too far to jump
                        if (distSq < minDistSq) continue;            // too short (wasteful hop)

                        // Forward-cone: neighbor must be closer to goal than current
                        double neighToGoalSq = (s.getX() - gx) * (s.getX() - gx)
                                + (s.getY() - gy) * (s.getY() - gy)
                                + (s.getZ() - gz) * (s.getZ() - gz);
                        if (neighToGoalSq >= currentToGoalSq) continue;

                        result.add(s);
                    }
                }
            }
        }

        // Sort by distance to goal (best candidates first)
        result.sort(Comparator.comparingDouble(s -> {
            double ddx = s.getX() - gx, ddy = s.getY() - gy, ddz = s.getZ() - gz;
            return ddx * ddx + ddy * ddy + ddz * ddz;
        }));

        // Keep top candidates for backtracking
        if (result.size() > 5) {
            return result.subList(0, 5);
        }
        return result;
    }

    private static long gridKey(double x, double y, double z, double cellSize) {
        return packKey(
                (int) Math.floor(x / cellSize),
                (int) Math.floor(y / cellSize),
                (int) Math.floor(z / cellSize)
        );
    }

    private static long packKey(int ix, int iy, int iz) {
        // Pack 3 ints into a long: 21 bits each (enough for ±1M range with 500 ly cells)
        return ((long) (ix & 0x1FFFFF) << 42) | ((long) (iy & 0x1FFFFF) << 21) | (iz & 0x1FFFFF);
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
}