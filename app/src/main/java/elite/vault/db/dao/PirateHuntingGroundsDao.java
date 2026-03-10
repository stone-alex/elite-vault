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
            SELECT
                sys.systemAddress,
                sys.starName,
                sys.x,
                sys.y,
                sys.z,
                SQRT(POW(sys.x - :x, 2) + POW(sys.y - :y, 2) + POW(sys.z - :z, 2)) AS distanceLy,
                GROUP_CONCAT(DISTINCT rsg.grade ORDER BY rsg.grade)                   AS resGrades,
                GROUP_CONCAT(DISTINCT f.factionName ORDER BY f.factionName SEPARATOR '|') AS pirateFactions,
                MAX(ss.confirmedCount)                                                 AS maxConfirmed,
                MAX(ss.lastSeen)                                                       AS lastSeen
            FROM star_system sys
            JOIN system_signals ss
                ON  ss.systemAddress = sys.systemAddress
                AND ss.signalType    = 'ResourceExtraction'
                AND ss.lastSeen      > NOW() - INTERVAL 14 DAY
            JOIN res_signal_grades rsg
                ON  rsg.signalName   = ss.signalName
            JOIN factions f
                ON  f.systemAddress  = sys.systemAddress
                AND f.isPirate       = 1
            WHERE
                sys.x BETWEEN :x - :rangeLy AND :x + :rangeLy
                AND sys.y BETWEEN :y - :rangeLy AND :y + :rangeLy
                AND sys.z BETWEEN :z - :rangeLy AND :z + :rangeLy
            GROUP BY sys.systemAddress, sys.starName, sys.x, sys.y, sys.z
            ORDER BY
                CASE
                    WHEN GROUP_CONCAT(DISTINCT rsg.grade) LIKE '%Hazardous%' THEN 0
                    WHEN GROUP_CONCAT(DISTINCT rsg.grade) LIKE '%High%'      THEN 1
                    WHEN GROUP_CONCAT(DISTINCT rsg.grade) LIKE '%Normal%'    THEN 2
                    ELSE 3
                END,
                MAX(ss.confirmedCount) DESC
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