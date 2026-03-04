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
            INSERT INTO powerplay_state (systemAddress, systemAllegiance, systemEconomy, systemSecondEconomy, 
                                                     systemGovernment, systemSecurity, controllingFaction, powers, 
                                                     powerplayState, controllingPower, powerplayStateControlProgress, 
                                                     powerplayStateReinforcement, powerplayStateUndermining
            )
            VALUES (:systemAddress, :systemAllegiance, :systemEconomy, :systemSecondEconomy, :systemGovernment, 
                                :systemSecurity, :controllingFaction, :powers, :powerplayState, :controllingPower, 
                                :powerplayStateControlProgress, :powerplayStateReinforcement, :powerplayStateUndermining
            )
            ON DUPLICATE KEY UPDATE
                systemAllegiance                = VALUES(systemAllegiance),
                systemEconomy                   = VALUES(systemEconomy),
                systemSecondEconomy             = VALUES(systemSecondEconomy),
                systemGovernment                = VALUES(systemGovernment),
                systemSecurity                  = VALUES(systemSecurity),
                controllingFaction              = VALUES(controllingFaction),
                powers                          = VALUES(powers),
                powerplayState                  = VALUES(powerplayState),
                controllingPower                = VALUES(controllingPower),
                powerplayStateControlProgress   = VALUES(powerplayStateControlProgress),
                powerplayStateReinforcement     = VALUES(powerplayStateReinforcement),
                powerplayStateUndermining       = VALUES(powerplayStateUndermining)
            """)
    void upsert(@BindBean PowerPlayStateDao.PowerPlayState data, @Bind Long systemAddress);


    @SqlQuery("""
            select * from powerplay_state where systemAddress = :systemAddress
            """)
    PowerPlayStateDao.PowerPlayState getPowerState(@Bind Long systemAddress);


    class PowerPlayStateMapper implements RowMapper<PowerPlayState> {

        @Override public PowerPlayState map(ResultSet rs, StatementContext ctx) throws SQLException {
            PowerPlayState entity = new PowerPlayState();
            entity.setSystemAddress(rs.getLong("systemAddress"));
            entity.setSystemAllegiance(rs.getString("systemAllegiance"));
            entity.setSystemEconomy(rs.getString("systemEconomy"));
            entity.setSystemSecondEconomy(rs.getString("systemSecondEconomy"));
            entity.setSystemGovernment(rs.getString("systemGovernment"));
            entity.setSystemSecurity(rs.getString("systemSecurity"));
            entity.setControllingPower(rs.getString("controllingPower"));
            entity.setControllingFaction(rs.getString("controllingFaction"));
            entity.setPowerplayState(rs.getString("powerplayState"));
            entity.setPowerplayStateControlProgress(rs.getDouble("powerplayStateControlProgress"));
            entity.setPowerplayStateReinforcement(rs.getInt("powerplayStateReinforcement"));
            entity.setPowerplayStateUndermining(rs.getInt("powerplayStateUndermining"));
            return entity;
        }
    }

    class PowerPlayState {
        private Long systemAddress;
        private String systemAllegiance;
        private String systemEconomy;// $economy_Extraction;
        private String systemSecondEconomy; // $economy_HighTech;
        private String systemGovernment; // $government_Democracy;
        private String systemSecurity;//  $SYSTEM_SECURITY_low;
        private String systemFaction;
        private String powers; // this | that
        private String powerplayState;
        private String controllingPower;
        private String controllingFaction;
        private Double powerplayStateControlProgress;
        private Integer powerplayStateReinforcement;
        private Integer powerplayStateUndermining;


        public Long getSystemAddress() {
            return systemAddress;
        }

        public void setSystemAddress(Long systemAddress) {
            this.systemAddress = systemAddress;
        }

        public String getSystemAllegiance() {
            return systemAllegiance;
        }

        public void setSystemAllegiance(String systemAllegiance) {
            this.systemAllegiance = systemAllegiance;
        }

        public String getSystemEconomy() {
            return systemEconomy;
        }

        public void setSystemEconomy(String systemEconomy) {
            this.systemEconomy = systemEconomy;
        }

        public String getSystemSecondEconomy() {
            return systemSecondEconomy;
        }

        public void setSystemSecondEconomy(String systemSecondEconomy) {
            this.systemSecondEconomy = systemSecondEconomy;
        }

        public String getSystemGovernment() {
            return systemGovernment;
        }

        public void setSystemGovernment(String systemGovernment) {
            this.systemGovernment = systemGovernment;
        }

        public String getSystemSecurity() {
            return systemSecurity;
        }

        public void setSystemSecurity(String systemSecurity) {
            this.systemSecurity = systemSecurity;
        }

        public String getSystemFaction() {
            return systemFaction;
        }

        public void setSystemFaction(String systemFaction) {
            this.systemFaction = systemFaction;
        }

        public String getPowers() {
            return powers;
        }

        public void setPowers(String powers) {
            this.powers = powers;
        }

        public String getPowerplayState() {
            return powerplayState;
        }

        public void setPowerplayState(String powerplayState) {
            this.powerplayState = powerplayState;
        }

        public String getControllingPower() {
            return controllingPower;
        }

        public void setControllingPower(String controllingPower) {
            this.controllingPower = controllingPower;
        }

        public Double getPowerplayStateControlProgress() {
            return powerplayStateControlProgress;
        }

        public void setPowerplayStateControlProgress(Double powerplayStateControlProgress) {
            this.powerplayStateControlProgress = powerplayStateControlProgress;
        }

        public Integer getPowerplayStateReinforcement() {
            return powerplayStateReinforcement;
        }

        public void setPowerplayStateReinforcement(Integer powerplayStateReinforcement) {
            this.powerplayStateReinforcement = powerplayStateReinforcement;
        }

        public Integer getPowerplayStateUndermining() {
            return powerplayStateUndermining;
        }

        public void setPowerplayStateUndermining(Integer powerplayStateUndermining) {
            this.powerplayStateUndermining = powerplayStateUndermining;
        }

        public String getControllingFaction() {
            return controllingFaction;
        }

        public void setControllingFaction(String controllingFaction) {
            this.controllingFaction = controllingFaction;
        }
    }
}
