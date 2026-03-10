package elite.vault.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@RegisterRowMapper(MissionProviderDao.MissionProviderMapper.class)
public interface MissionProviderDao {

    @SqlQuery("""
            SELECT
                st.realName                                                                         AS stationName,
                st.stationType,
                st.distanceToArrival,
                st.hasLargePad,
                st.systemAddress,
                f.factionName                                                                       AS controllingFaction,
                f.factionState,
                f.influence,
                sys.starName                                                                        AS systemName,
                SQRT(POW(st.x - :huntX, 2) + POW(st.y - :huntY, 2) + POW(st.z - :huntZ, 2))      AS distanceLy
            FROM stations st
            JOIN star_system sys
                ON  sys.systemAddress = st.systemAddress
            JOIN factions f
                ON  f.systemAddress   = st.systemAddress
                AND f.factionName     = st.controllingFaction
                AND f.isPirate        = 0
            WHERE
                st.x BETWEEN :huntX - 20 AND :huntX + 20
                AND st.y BETWEEN :huntY - 20 AND :huntY + 20
                AND st.z BETWEEN :huntZ - 20 AND :huntZ + 20
                AND st.systemAddress  != :huntSystemAddress
                AND st.stationType    != 'FleetCarrier'
                AND JSON_CONTAINS(st.services, '"dock"')
            ORDER BY
                CASE f.factionState
                    WHEN 'War'      THEN 0
                    WHEN 'CivilWar' THEN 1
                    WHEN 'Boom'     THEN 2
                    ELSE 3
                END,
                f.influence DESC,
                distanceLy ASC
            LIMIT 10
            """)
    List<MissionProvider> findProviders(
            @Bind("huntX") double huntX,
            @Bind("huntY") double huntY,
            @Bind("huntZ") double huntZ,
            @Bind("huntSystemAddress") long huntSystemAddress
    );


    class MissionProviderMapper implements RowMapper<MissionProvider> {
        @Override
        public MissionProvider map(ResultSet rs, StatementContext ctx) throws SQLException {
            MissionProvider e = new MissionProvider();
            e.setStationName(rs.getString("stationName"));
            e.setStationType(rs.getString("stationType"));
            e.setSystemName(rs.getString("systemName"));
            e.setDistanceLy(rs.getDouble("distanceLy"));
            e.setDistanceToArrival(rs.getDouble("distanceToArrival"));
            e.setHasLargePad(rs.getBoolean("hasLargePad"));
            e.setControllingFaction(rs.getString("controllingFaction"));
            e.setFactionState(rs.getString("factionState"));
            e.setInfluence(rs.getDouble("influence"));
            return e;
        }
    }


    class MissionProvider {
        private String stationName;
        private String stationType;
        private String systemName;
        private double distanceLy;
        private double distanceToArrival;
        private boolean hasLargePad;
        private String controllingFaction;
        private String factionState;
        private double influence;

        public String getStationName() {
            return stationName;
        }

        public void setStationName(String v) {
            this.stationName = v;
        }

        public String getStationType() {
            return stationType;
        }

        public void setStationType(String v) {
            this.stationType = v;
        }

        public String getSystemName() {
            return systemName;
        }

        public void setSystemName(String v) {
            this.systemName = v;
        }

        public double getDistanceLy() {
            return distanceLy;
        }

        public void setDistanceLy(double v) {
            this.distanceLy = v;
        }

        public double getDistanceToArrival() {
            return distanceToArrival;
        }

        public void setDistanceToArrival(double v) {
            this.distanceToArrival = v;
        }

        public boolean isHasLargePad() {
            return hasLargePad;
        }

        public void setHasLargePad(boolean v) {
            this.hasLargePad = v;
        }

        public String getControllingFaction() {
            return controllingFaction;
        }

        public void setControllingFaction(String v) {
            this.controllingFaction = v;
        }

        public String getFactionState() {
            return factionState;
        }

        public void setFactionState(String v) {
            this.factionState = v;
        }

        public double getInfluence() {
            return influence;
        }

        public void setInfluence(double v) {
            this.influence = v;
        }
    }
}