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

    @SqlUpdate("""
            INSERT OR REPLACE INTO stellar_object (starSystem, timestamp, systemAddress, X,Y,Z, data)
            VALUES(:starSystem, :timestamp, :systemAddress, :x, :y, :z, :data)
            ON CONFLICT(systemAddress) DO UPDATE SET
            data = excluded.data,
            starSystem = excluded.starSystem,
            timestamp = excluded.timestamp,
            X = excluded.x,
            Y = excluded.y,
            Z = excluded.z
            """)
    void upsert(@BindBean StellarObjectDao.StellarObject data);


    class StellarObjectMapper implements RowMapper<StellarObject> {
        @Override public StellarObject map(ResultSet rs, StatementContext ctx) throws SQLException {
            StellarObject enetity = new StellarObject();
            enetity.setTimestamp(rs.getString("timestamp"));
            enetity.setStarSystem(rs.getString("starSystem"));
            enetity.setSystemAddress(rs.getLong("systemAddress"));
            enetity.setX(rs.getDouble("x"));
            enetity.setY(rs.getDouble("y"));
            enetity.setZ(rs.getDouble("z"));
            enetity.setData(rs.getString("data"));
            return enetity;
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
