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


    @SqlUpdate("""
            INSERT OR REPLACE INTO system (systemAddress, starName, x,y,z)
            VALUES(:systemAddress, :starName, :x, :y, :z)
            ON CONFLICT(systemAddress) DO UPDATE SET
            starName = excluded.starName,
            x = excluded.x,
            y = excluded.y,
            z = excluded.z
            """)
    void upsert(@BindBean SystemDao.StarSystem data);


    @SqlQuery("""
            SELECT *
                FROM system
                WHERE starName = :starName
            LIMIT 1;
            """)
    SystemDao.StarSystem findByName(String starName);


    @SqlQuery("""
            SELECT *
            FROM system
            WHERE
              starName != :currentName
              AND x BETWEEN :minX AND :maxX
              AND y BETWEEN :minY AND :maxY
              AND z BETWEEN :minZ AND :maxZ
              AND (x - :cx)*(x - :cx) + (y - :cy)*(y - :cy) + (z - :cz)*(z - :cz) <= 250000.0
            """)
    List<SystemDao.StarSystem> findNeighbors(
            @Bind("minX") double minX, @Bind("maxX") double maxX,
            @Bind("minY") double minY, @Bind("maxY") double maxY,
            @Bind("minZ") double minZ, @Bind("maxZ") double maxZ,
            @Bind("cx") double cx, @Bind("cy") double cy, @Bind("cz") double cz,
            @Bind("currentName") String currentName
    );


    class SystemMapper implements RowMapper<StarSystem> {

        @Override public StarSystem map(ResultSet rs, StatementContext ctx) throws SQLException {
            StarSystem entity = new StarSystem();
            entity.setDate(rs.getString("date"));
            entity.setSystemAddress(rs.getLong("systemAddress"));
            entity.setStarName(rs.getString("starName"));
            entity.setX(rs.getDouble("x"));
            entity.setY(rs.getDouble("y"));
            entity.setZ(rs.getDouble("z"));
            return entity;
        }
    }


    class StarSystem {
        long systemAddress;
        String starName;
        double x, y, z;
        String date;


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
    }
}
