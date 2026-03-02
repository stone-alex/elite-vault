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

@RegisterRowMapper(StellarObjectDao.StellarObjectMapper.class)
public interface StellarObjectDao {

    @SqlUpdate("""
            INSERT OR REPLACE INTO stellar_object (starSystem, timestamp, systemAddress, x,y,z, data)
            VALUES(:starSystem, :timestamp, :systemAddress, :x, :y, :z, :data)
            ON CONFLICT(systemAddress) DO UPDATE SET
            data = excluded.data,
            starSystem = excluded.starSystem,
            timestamp = excluded.timestamp,
            x = excluded.x,
            y = excluded.y,
            z = excluded.z
            """)
    void upsert(@BindBean StellarObjectDao.StellarObject data);


    /**
     * Find primary star by exact name (case-sensitive; adjust COLLATE if needed).
     * Returns null if not found.
     */
    @SqlQuery("""
            SELECT *
                FROM stellar_object
                WHERE starSystem = :starSystem
                AND json_extract(data, '$.DistanceFromArrivalLS') = 0
            LIMIT 1;
            """)
    StellarObjectDao.StellarObject findByName(String starSystem);

    /**
     * Get all potential carrier-accessible neighbors within ~500 ly.
     * Uses axis-aligned bounding box + euclidean distance filter.
     * Very fast with indexes on X,Y,Z.
     */
    @SqlQuery("""
            SELECT *
            FROM stellar_object
            WHERE json_extract(data, '$.DistanceFromArrivalLS') = 0
              AND x BETWEEN :minX AND :maxX
              AND y BETWEEN :minY AND :maxY
              AND z BETWEEN :minZ AND :maxZ
              AND (x - :cx)*(x - :cx) + (y - :cy)*(y - :cy) + (z - :cz)*(z - :cz) <= 250000.0
              AND starSystem != :currentName
            """)
    List<StellarObjectDao.StellarObject> findNeighbors(
            @Bind("minX") double minX, @Bind("maxX") double maxX,
            @Bind("minY") double minY, @Bind("maxY") double maxY,
            @Bind("minZ") double minZ, @Bind("maxZ") double maxZ,
            @Bind("cx") double cx, @Bind("cy") double cy, @Bind("cz") double cz,
            @Bind("currentName") String currentName
    );

    // Optional: count total primary stars (for monitoring / debug)
    @SqlQuery("SELECT COUNT(*) FROM stellar_object WHERE json_extract(data, '$.DistanceFromArrivalLS') = 0")
    long countPrimaryStars();

    class StellarObjectMapper implements RowMapper<StellarObject> {
        @Override
        public StellarObject map(ResultSet rs, StatementContext ctx) throws SQLException {
            StellarObject entity = new StellarObject();
            entity.setTimestamp(rs.getString("timestamp"));
            entity.setStarSystem(rs.getString("starSystem"));
            entity.setSystemAddress(rs.getLong("systemAddress"));
            entity.setX(rs.getDouble("x"));
            entity.setY(rs.getDouble("y"));
            entity.setZ(rs.getDouble("z"));
            entity.setData(rs.getString("data"));
            return entity;
        }
    }


    class StellarObject {
        String timestamp;
        String starSystem;
        Long systemAddress;
        double x, y, z;
        String data;

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getStarSystem() {
            return starSystem;
        }

        public void setStarSystem(String starSystem) {
            this.starSystem = starSystem;
        }

        public Long getSystemAddress() {
            return systemAddress;
        }

        public void setSystemAddress(Long systemAddress) {
            this.systemAddress = systemAddress;
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

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }
}
