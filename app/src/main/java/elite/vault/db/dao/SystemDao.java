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
     * - Inserts if systemAddress does not exist
     * - Updates coordinates, name and sector if systemAddress already exists
     * (latest data wins — typical for EDDN journal entries)
     * <p>
     * Uses MariaDB/MySQL native ON DUPLICATE KEY UPDATE syntax.
     * Requires PRIMARY KEY or UNIQUE constraint on systemAddress (already present).
     */
    @SqlUpdate("""
            INSERT INTO star_system (systemAddress, starName, x, y, z, sector, pos)
            VALUES (:systemAddress, :starName, :x, :y, :z, :sector, ST_PointFromText(CONCAT('POINT(', :x, ' ', :y, ')')))
            ON DUPLICATE KEY UPDATE
                starName = VALUES(starName),
                x        = VALUES(x),
                y        = VALUES(y),
                z        = VALUES(z),
                sector   = VALUES(sector),
                pos      = ST_PointFromText(CONCAT('POINT(', :x, ' ', :y, ')'))
            """)
    void upsert(@BindBean StarSystem data);


    @SqlQuery("""
            SELECT systemAddress, starName, x, y, z, sector
            FROM star_system
            WHERE starName = :starName
            LIMIT 1
            """)
    StarSystem findByName(@Bind("starName") String starName);


    @SqlQuery("""
            WITH candidates AS (
                    SELECT systemAddress, starName, x, y, z, date, sector,
                           (x - :cx)*(x - :cx) + (y - :cy)*(y - :cy) + (z - :cz)*(z - :cz) AS dist_sq
                    FROM star_system
                    WHERE
                        starName != :currentName
                        AND MBRContains(
                            ST_Envelope(ST_GeomFromText(CONCAT(
                                'LINESTRING(', :minX, ' ', :minY, ',', :maxX, ' ', :maxY, ')'))),
                            pos
                        )
                        AND z BETWEEN :minZ AND :maxZ
                )
                SELECT systemAddress, starName, x, y, z, date, sector
                FROM candidates
                WHERE
                    dist_sq <= 250000.0
                    AND (:minDistSq <= 0 OR dist_sq >= :minDistSq)
                    AND (x - :gx)*(x - :gx) + (y - :gy)*(y - :gy) + (z - :gz)*(z - :gz) <  (:cx - :gx)*(:cx - :gx) + (:cy - :gy)*(:cy - :gy) + (:cz - :gz)*(:cz - :gz)
                ORDER BY (x - :gx)*(x - :gx) + (y - :gy)*(y - :gy) + (z - :gz)*(z - :gz) ASC
                LIMIT 5
            """)
    List<StarSystem> findNeighbors(
            @Bind("minX") double minX, @Bind("maxX") double maxX,
            @Bind("minY") double minY, @Bind("maxY") double maxY,
            @Bind("minZ") double minZ, @Bind("maxZ") double maxZ,
            @Bind("cx") double cx, @Bind("cy") double cy, @Bind("cz") double cz,
            @Bind("gx") double gx, @Bind("gy") double gy, @Bind("gz") double gz,
            @Bind("minDistSq") double minDistSq,  // NEW: dynamic sliver
            @Bind("currentName") String currentName
    );


    @SqlQuery("""
            SELECT systemAddress, starName, x, y, z, sector
            FROM star_system
            WHERE MBRContains(
                    ST_Envelope(ST_GeomFromText(CONCAT(
                        'LINESTRING(', :minX, ' ', :minY, ',', :maxX, ' ', :maxY, ')'))),
                    pos
                  )
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
        private String date;
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

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getSector() {
            return sector;
        }

        public void setSector(String sector) {
            this.sector = sector;
        }

        public String getPosWkt() {
            return String.format("POINT(%f %f)", getX(), getY());
        }
    }
}