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

@RegisterRowMapper(PowerPlayStateDao.PowerPlayStateMapper.class)
public interface PowerPlayStateDao {

    @SqlUpdate("""
            INSERT INTO powerplay_state (
                systemAddress, systemAllegiance, systemEconomy, systemSecondEconomy,
                systemGovernment, systemSecurity, controllingFaction, powers,
                powerplayState, controllingPower, controlProgress,
                reinforcement, undermining
            )
            VALUES (
                :systemAddress, :systemAllegiance, :systemEconomy, :systemSecondEconomy,
                :systemGovernment, :systemSecurity, :controllingFaction, :powers,
                :powerplayState, :controllingPower, :controlProgress,
                :reinforcement, :undermining
            )
            ON DUPLICATE KEY UPDATE
                systemAllegiance    = VALUES(systemAllegiance),
                systemEconomy       = VALUES(systemEconomy),
                systemSecondEconomy = VALUES(systemSecondEconomy),
                systemGovernment    = VALUES(systemGovernment),
                systemSecurity      = VALUES(systemSecurity),
                controllingFaction  = VALUES(controllingFaction),
                powers              = VALUES(powers),
                powerplayState      = VALUES(powerplayState),
                controllingPower    = VALUES(controllingPower),
                controlProgress     = VALUES(controlProgress),
                reinforcement       = VALUES(reinforcement),
                undermining         = VALUES(undermining)
            """)
    void upsert(@BindBean PowerPlayState data);

    @SqlQuery("""
            SELECT
                systemAddress, systemAllegiance, systemEconomy, systemSecondEconomy,
                systemGovernment, systemSecurity, controllingFaction, powers,
                powerplayState, controllingPower, controlProgress,
                reinforcement, undermining
            FROM powerplay_state
            WHERE systemAddress = :systemAddress
            """)
    PowerPlayState getPowerState(@Bind("systemAddress") long systemAddress);


    class PowerPlayStateMapper implements RowMapper<PowerPlayState> {

        @Override
        public PowerPlayState map(ResultSet rs, StatementContext ctx) throws SQLException {
            PowerPlayState e = new PowerPlayState();
            e.setSystemAddress(rs.getLong("systemAddress"));
            e.setSystemAllegiance(rs.getString("systemAllegiance"));
            e.setSystemEconomy(rs.getString("systemEconomy"));
            e.setSystemSecondEconomy(rs.getString("systemSecondEconomy"));
            e.setSystemGovernment(rs.getString("systemGovernment"));
            e.setSystemSecurity(rs.getString("systemSecurity"));
            e.setControllingFaction(rs.getString("controllingFaction"));
            e.setPowers(rs.getString("powers"));
            e.setPowerplayState(rs.getString("powerplayState"));
            e.setControllingPower(rs.getString("controllingPower"));
            e.setControlProgress(rs.getDouble("controlProgress"));
            e.setReinforcement(rs.getInt("reinforcement"));
            e.setUndermining(rs.getInt("undermining"));
            return e;
        }
    }

    class PowerPlayState {
        private Long systemAddress;
        private String systemAllegiance;
        private String systemEconomy;
        private String systemSecondEconomy;
        private String systemGovernment;
        private String systemSecurity;
        private String controllingFaction;
        private String powers;
        private String powerplayState;
        private String controllingPower;
        private Double controlProgress;
        private Integer reinforcement;
        private Integer undermining;

        public Long getSystemAddress() {
            return systemAddress;
        }

        public void setSystemAddress(Long v) {
            this.systemAddress = v;
        }

        public String getSystemAllegiance() {
            return systemAllegiance;
        }

        public void setSystemAllegiance(String v) {
            this.systemAllegiance = v;
        }

        public String getSystemEconomy() {
            return systemEconomy;
        }

        public void setSystemEconomy(String v) {
            this.systemEconomy = v;
        }

        public String getSystemSecondEconomy() {
            return systemSecondEconomy;
        }

        public void setSystemSecondEconomy(String v) {
            this.systemSecondEconomy = v;
        }

        public String getSystemGovernment() {
            return systemGovernment;
        }

        public void setSystemGovernment(String v) {
            this.systemGovernment = v;
        }

        public String getSystemSecurity() {
            return systemSecurity;
        }

        public void setSystemSecurity(String v) {
            this.systemSecurity = v;
        }

        public String getControllingFaction() {
            return controllingFaction;
        }

        public void setControllingFaction(String v) {
            this.controllingFaction = v;
        }

        public String getPowers() {
            return powers;
        }

        public void setPowers(String v) {
            this.powers = v;
        }

        public String getPowerplayState() {
            return powerplayState;
        }

        public void setPowerplayState(String v) {
            this.powerplayState = v;
        }

        public String getControllingPower() {
            return controllingPower;
        }

        public void setControllingPower(String v) {
            this.controllingPower = v;
        }

        public Double getControlProgress() {
            return controlProgress;
        }

        public void setControlProgress(Double v) {
            this.controlProgress = v;
        }

        public Integer getReinforcement() {
            return reinforcement;
        }

        public void setReinforcement(Integer v) {
            this.reinforcement = v;
        }

        public Integer getUndermining() {
            return undermining;
        }

        public void setUndermining(Integer v) {
            this.undermining = v;
        }
    }
}