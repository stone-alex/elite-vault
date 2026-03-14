package elite.vault.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@RegisterRowMapper(SystemDao.SystemMapper.class)
public interface SystemDao {

    /**
     * Upserts a star system entry.
     * pos column removed — MySQL 8 spatial is 2D geographic only, not useful
     * for Elite's Cartesian coordinate space. Plain BETWEEN on x/y/z via
     * idx_ss_xyz handles all bounding-box queries correctly and faster.
     */
    @SqlUpdate("""
            INSERT INTO star_system (systemAddress, starName, x, y, z, sector)
            VALUES (:systemAddress, :starName, :x, :y, :z, :sector)
            ON DUPLICATE KEY UPDATE
                starName = VALUES(starName),
                x        = VALUES(x),
                y        = VALUES(y),
                z        = VALUES(z),
                sector   = VALUES(sector)
            """)
    void upsert(@BindBean StarSystem data);


    @SqlQuery("""
            SELECT systemAddress, starName, x, y, z, sector
            FROM star_system
            WHERE starName = :starName
            LIMIT 1
            """)
    StarSystem findByName(@Bind("starName") String starName);


    /**
     * Look up a star system by its EDDN systemAddress.
     * Used by StationManager to resolve coordinates before upserting a station.
     * Returns null if the system has not yet been seen via EDDN.
     */
    @SqlQuery("""
            SELECT systemAddress, starName, x, y, z, sector
            FROM star_system
            WHERE systemAddress = :systemAddress
            LIMIT 1
            """)
    StarSystem findByAddress(@Bind("systemAddress") long systemAddress);


    /**
     * Find nearby star systems suitable as jump waypoints.
     * <p>
     * Used by route planning (trade hops, fleet carrier waypoints).
     * Pure BETWEEN bounding box on idx_ss_xyz — no spatial functions.
     * <p>
     * Parameters:
     * cx/cy/cz     — current position (center of search sphere)
     * gx/gy/gz     — goal position (used to order candidates toward destination)
     * minX..maxZ   — precomputed bounding box (cx ± range)
     * minDistSq    — exclude systems too close (avoids re-visiting current system)
     * currentName  — exclude current system by name
     * <p>
     * Returns up to 5 systems within 500 ly of current position, ordered by
     * proximity to the goal position (greedy nearest-to-goal selection).
     * The 500 ly hard limit (250000.0 = 500^2) matches fleet carrier max hop range;
     * trade route callers pass a smaller hopDistance so the bounding box naturally
     * limits candidates before the sphere check.
     */
    @SqlQuery("""
            WITH candidates AS (
                SELECT systemAddress, starName, x, y, z, sector,
                       (x - :cx)*(x - :cx) + (y - :cy)*(y - :cy) + (z - :cz)*(z - :cz) AS dist_sq
                FROM star_system
                WHERE
                    starName != :currentName
                    AND x BETWEEN :minX AND :maxX
                    AND y BETWEEN :minY AND :maxY
                    AND z BETWEEN :minZ AND :maxZ
            )
            SELECT systemAddress, starName, x, y, z, sector
            FROM candidates
            WHERE
                dist_sq <= 250000.0
                AND (:minDistSq <= 0 OR dist_sq >= :minDistSq)
                AND (x - :gx)*(x - :gx) + (y - :gy)*(y - :gy) + (z - :gz)*(z - :gz)
                    < (:cx - :gx)*(:cx - :gx) + (:cy - :gy)*(:cy - :gy) + (:cz - :gz)*(:cz - :gz)
            ORDER BY
                (x - :gx)*(x - :gx) + (y - :gy)*(y - :gy) + (z - :gz)*(z - :gz) ASC
            LIMIT 100
            """)
    List<StarSystem> findNeighbors(
            @Bind("minX") double minX, @Bind("maxX") double maxX,
            @Bind("minY") double minY, @Bind("maxY") double maxY,
            @Bind("minZ") double minZ, @Bind("maxZ") double maxZ,
            @Bind("cx") double cx, @Bind("cy") double cy, @Bind("cz") double cz,
            @Bind("gx") double gx, @Bind("gy") double gy, @Bind("gz") double gz,
            @Bind("minDistSq") double minDistSq,
            @Bind("currentName") String currentName
    );


    /**
     * Find all star systems within a 3D bounding box corridor.
     * Used to pre-load candidate waypoints along a route before the Java-side
     * path-finding step narrows them to an actual jump sequence.
     *
     * minX..maxZ define the corridor box — caller computes these from the
     * route endpoints plus a lateral tolerance.
     */
    @SqlQuery("""
            SELECT systemAddress, starName, x, y, z, sector
            FROM star_system
            WHERE
                x BETWEEN :minX AND :maxX
                AND y BETWEEN :minY AND :maxY
                AND z BETWEEN :minZ AND :maxZ
            """)
    List<StarSystem> findSystemsInCorridor(
            @Bind("minX") double minX, @Bind("maxX") double maxX,
            @Bind("minY") double minY, @Bind("maxY") double maxY,
            @Bind("minZ") double minZ, @Bind("maxZ") double maxZ
    );


    class SystemMapper implements RowMapper<StarSystem> {

        @Override
        public StarSystem map(ResultSet rs, StatementContext ctx) throws SQLException {
            StarSystem entity = new StarSystem();
            entity.setSystemAddress(rs.getLong("systemAddress"));
            entity.setStarName(rs.getString("starName"));
            entity.setX(rs.getDouble("x"));
            entity.setY(rs.getDouble("y"));
            entity.setZ(rs.getDouble("z"));
            entity.setSector(rs.getString("sector"));
            return entity;
        }
    }


    class StarSystem {
        private long systemAddress;
        private String starName;
        private double x, y, z;
        private String sector;

        public long getSystemAddress() {
            return systemAddress;
        }

        public void setSystemAddress(long systemAddress) {
            this.systemAddress = systemAddress;
        }

        public String getStarName() {
            return starName;
        }

        public void setStarName(String starName) {
            this.starName = starName;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public double getZ() {
            return z;
        }

        public void setZ(double z) {
            this.z = z;
        }

        public String getSector() {
            return sector;
        }

        public void setSector(String sector) {
            this.sector = sector;
        }
    }
}