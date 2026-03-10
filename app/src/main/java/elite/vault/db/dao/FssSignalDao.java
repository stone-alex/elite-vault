package elite.vault.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

@RegisterRowMapper(FssSignalDao.FssSignalMapper.class)
public interface FssSignalDao {

    @SqlUpdate("""
            INSERT INTO system_signals
                (systemAddress, starSystem, signalName, signalType, ussType, spawningFaction, spawningState, threatLevel, firstSeen, lastSeen, confirmedCount)
            VALUES
                (:systemAddress, :starSystem, :signalName, :signalType, :ussType, :spawningFaction, :spawningState, :threatLevel, NOW(), NOW(), 1)
            ON DUPLICATE KEY UPDATE
                lastSeen        = NOW(),
                confirmedCount  = confirmedCount + 1,
                starSystem      = VALUES(starSystem),
                signalType      = COALESCE(VALUES(signalType),      signalType),
                ussType         = COALESCE(VALUES(ussType),         ussType),
                spawningFaction = COALESCE(VALUES(spawningFaction), spawningFaction),
                spawningState   = COALESCE(VALUES(spawningState),   spawningState),
                threatLevel     = COALESCE(VALUES(threatLevel),     threatLevel)
            """)
    void upsert(@BindBean FssSignalDao.FssSignal data);


    class FssSignalMapper implements RowMapper<FssSignal> {
        @Override
        public FssSignal map(ResultSet rs, StatementContext ctx) throws SQLException {
            FssSignal e = new FssSignal();
            e.setSystemAddress(rs.getLong("systemAddress"));
            e.setStarSystem(rs.getString("starSystem"));
            e.setSignalName(rs.getString("signalName"));
            e.setSignalType(rs.getString("signalType"));
            e.setUssType(rs.getString("ussType"));
            e.setSpawningFaction(rs.getString("spawningFaction"));
            e.setSpawningState(rs.getString("spawningState"));
            e.setThreatLevel((Integer) rs.getObject("threatLevel")); // nullable
            e.setConfirmedCount(rs.getInt("confirmedCount"));
            e.setFirstSeen(rs.getTimestamp("firstSeen").toLocalDateTime());
            e.setLastSeen(rs.getTimestamp("lastSeen").toLocalDateTime());
            return e;
        }
    }


    class FssSignal {
        private Long systemAddress;
        private String starSystem;
        private String signalName;
        private String signalType;
        private String ussType;
        private String spawningFaction;
        private String spawningState;
        private Integer threatLevel;
        // firstSeen / lastSeen / confirmedCount driven by SQL — read-only from mapper
        private Integer confirmedCount;
        private LocalDateTime firstSeen;
        private LocalDateTime lastSeen;

        public Long getSystemAddress() {
            return systemAddress;
        }

        public void setSystemAddress(Long v) {
            this.systemAddress = v;
        }

        public String getStarSystem() {
            return starSystem;
        }

        public void setStarSystem(String v) {
            this.starSystem = v;
        }

        public String getSignalName() {
            return signalName;
        }

        public void setSignalName(String v) {
            this.signalName = v;
        }

        public String getSignalType() {
            return signalType;
        }

        public void setSignalType(String v) {
            this.signalType = v;
        }

        public String getUssType() {
            return ussType;
        }

        public void setUssType(String v) {
            this.ussType = v;
        }

        public String getSpawningFaction() {
            return spawningFaction;
        }

        public void setSpawningFaction(String v) {
            this.spawningFaction = v;
        }

        public String getSpawningState() {
            return spawningState;
        }

        public void setSpawningState(String v) {
            this.spawningState = v;
        }

        public Integer getThreatLevel() {
            return threatLevel;
        }

        public void setThreatLevel(Integer v) {
            this.threatLevel = v;
        }

        public Integer getConfirmedCount() {
            return confirmedCount;
        }

        public void setConfirmedCount(Integer v) {
            this.confirmedCount = v;
        }

        public LocalDateTime getFirstSeen() {
            return firstSeen;
        }

        public void setFirstSeen(LocalDateTime v) {
            this.firstSeen = v;
        }

        public LocalDateTime getLastSeen() {
            return lastSeen;
        }

        public void setLastSeen(LocalDateTime v) {
            this.lastSeen = v;
        }
    }
}