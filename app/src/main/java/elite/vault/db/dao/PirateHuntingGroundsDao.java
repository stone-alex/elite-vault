package elite.vault.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RegisterRowMapper(PirateHuntingGroundsDao.HuntingGroundMapper.class)
public interface PirateHuntingGroundsDao {

    @SqlQuery("""
            WITH res_systems AS (
                -- Per-system RES signal summary (deduplicated grades, max staleness/count)
                SELECT ss.systemAddress,
                       GROUP_CONCAT(DISTINCT rsg.grade) AS resGrades,
                       MAX(ss.confirmedCount)           AS maxConfirmed,
                       MAX(ss.lastSeen)                 AS lastSeen
                FROM system_signals ss
                JOIN res_signal_grades rsg ON rsg.signalName = ss.signalName
                WHERE ss.signalType = 'ResourceExtraction'
                  AND ss.lastSeen   > datetime('now', '-14 days')
                GROUP BY ss.systemAddress
            ),
            pirate_factions AS (
                -- Deduplicate faction names per system, then concatenate with pipe
                SELECT systemAddress,
                       GROUP_CONCAT(factionName, '|') AS pirateFactions
                FROM (SELECT DISTINCT systemAddress, factionName
                      FROM factions
                      WHERE isPirate = 1)
                GROUP BY systemAddress
            )
            SELECT
                sys.systemAddress,
                sys.starName,
                sys.x,
                sys.y,
                sys.z,
                sqrt((sys.x - :x)*(sys.x - :x) + (sys.y - :y)*(sys.y - :y) + (sys.z - :z)*(sys.z - :z)) AS distanceLy,
                rs.resGrades,
                pf.pirateFactions,
                rs.maxConfirmed,
                rs.lastSeen
            FROM star_system sys
            JOIN res_systems     rs ON rs.systemAddress = sys.systemAddress
            JOIN pirate_factions pf ON pf.systemAddress = sys.systemAddress
            WHERE
                sys.x BETWEEN :x - :rangeLy AND :x + :rangeLy
                AND sys.y BETWEEN :y - :rangeLy AND :y + :rangeLy
                AND sys.z BETWEEN :z - :rangeLy AND :z + :rangeLy
            ORDER BY
                CASE
                    WHEN rs.resGrades LIKE '%Hazardous%' THEN 0
                    WHEN rs.resGrades LIKE '%High%'      THEN 1
                    WHEN rs.resGrades LIKE '%Normal%'    THEN 2
                    ELSE 3
                END,
                rs.maxConfirmed DESC
            LIMIT 20
            """)
    List<HuntingGround> findHuntingGrounds(
            @Bind("x") double x,
            @Bind("y") double y,
            @Bind("z") double z,
            @Bind("rangeLy") double rangeLy
    );


    class HuntingGroundMapper implements RowMapper<HuntingGround> {
        @Override
        public HuntingGround map(ResultSet rs, StatementContext ctx) throws SQLException {
            HuntingGround e = new HuntingGround();
            e.setSystemAddress(rs.getLong("systemAddress"));
            e.setStarName(rs.getString("starName"));
            e.setX(rs.getDouble("x"));
            e.setY(rs.getDouble("y"));
            e.setZ(rs.getDouble("z"));
            e.setDistanceLy(rs.getDouble("distanceLy"));
            e.setMaxConfirmed(rs.getInt("maxConfirmed"));
            e.setLastSeen(rs.getTimestamp("lastSeen").toLocalDateTime());

            String grades = rs.getString("resGrades");
            e.setResGrades(grades != null
                    ? Arrays.asList(grades.split(","))
                    : Collections.emptyList());

            String factions = rs.getString("pirateFactions");
            e.setPirateFactions(factions != null
                    ? Arrays.asList(factions.split("\\|"))
                    : Collections.emptyList());

            return e;
        }
    }


    class HuntingGround {
        private Long systemAddress;
        private String starName;
        private double x;
        private double y;
        private double z;
        private double distanceLy;
        private List<String> resGrades;
        private List<String> pirateFactions;
        private int maxConfirmed;
        private LocalDateTime lastSeen;

        public String getStarName() {
            return starName;
        }

        public void setStarName(String v) {
            this.starName = v;
        }

        public double getX() {
            return x;
        }

        public void setX(double v) {
            this.x = v;
        }

        public double getY() {
            return y;
        }

        public void setY(double v) {
            this.y = v;
        }

        public double getZ() {
            return z;
        }

        public void setZ(double v) {
            this.z = v;
        }

        public double getDistanceLy() {
            return distanceLy;
        }

        public void setDistanceLy(double v) {
            this.distanceLy = v;
        }

        public List<String> getResGrades() {
            return resGrades;
        }

        public void setResGrades(List<String> v) {
            this.resGrades = v;
        }

        public List<String> getPirateFactions() {
            return pirateFactions;
        }

        public void setPirateFactions(List<String> v) {
            this.pirateFactions = v;
        }

        public int getMaxConfirmed() {
            return maxConfirmed;
        }

        public void setMaxConfirmed(int v) {
            this.maxConfirmed = v;
        }

        public LocalDateTime getLastSeen() {
            return lastSeen;
        }

        public void setLastSeen(LocalDateTime v) {
            this.lastSeen = v;
        }

        public Long getSystemAddress() {
            return systemAddress;
        }

        public void setSystemAddress(Long systemAddress) {
            this.systemAddress = systemAddress;
        }
    }
}