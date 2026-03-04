package elite.vault.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;

@RegisterRowMapper(StellarObjectDao.StellarObjectMapper.class)
public interface StellarObjectDao {

    /**
     * Upserts a stellar object entry.
     * - Inserts new row if systemAddress does not exist
     * - Updates all fields if systemAddress already exists (latest data wins)
     * <p>
     * Uses MariaDB/MySQL native ON DUPLICATE KEY UPDATE syntax.
     * Requires UNIQUE or PRIMARY KEY on systemAddress (already present in schema).
     */
    @SqlUpdate("""
            INSERT INTO stellar_object (starSystem, bodyId, timestamp, systemAddress, x, y, z)
            VALUES (:starSystem, :bodyId, :timestamp, :systemAddress, :x, :y, :z)
            ON DUPLICATE KEY UPDATE
                starSystem  = VALUES(starSystem),
                bodyId      = VALUES(bodyId),
                timestamp   = VALUES(timestamp),
                x           = VALUES(x),
                y           = VALUES(y),
                z           = VALUES(z)
            """)
    void upsert(@BindBean StellarObject data);


    class StellarObjectMapper implements RowMapper<StellarObject> {
        @Override
        public StellarObject map(ResultSet rs, StatementContext ctx) throws SQLException {
            StellarObject entity = new StellarObject();
            entity.setTimestamp(rs.getString("timestamp"));
            entity.setStarSystem(rs.getString("starSystem"));
            entity.setSystemAddress(rs.getLong("systemAddress"));
            entity.setBodyId(rs.getLong("bodyId"));
            entity.setX(rs.getDouble("x"));
            entity.setY(rs.getDouble("y"));
            entity.setZ(rs.getDouble("z"));
            return entity;
        }
    }


    class StellarObject {
        private String timestamp;
        private String starSystem;
        private Long systemAddress;
        private Long bodyId;
        private double x, y, z;

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

        public Long getBodyId() {
            return bodyId;
        }

        public void setBodyId(Long bodyId) {
            this.bodyId = bodyId;
        }
    }
}