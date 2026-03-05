package elite.vault.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;

@RegisterRowMapper(StationsDao.StationMapper.class)
public interface StationsDao {


    @SqlUpdate("""
            INSERT INTO stations (systemaddress, stationid, realname, controllingfaction, controllingfactionstate, distancetoarrival, primaryeconomy, economies, government, services, haslargepad, hasmediumpad, hassmallpad, stationType)
            VALUES (:systemAddress, :stationId, :realName, :controllingFaction, :controllingFactionState, :distanceToArrival, :primaryEconomy, :economies, :government, :services, :hasLargePad, :hasMediumPad, :hasSmallPad, :stationType)
            ON DUPLICATE KEY UPDATE
                realName    = VALUES(realName),
                controllingFaction        = VALUES(controllingFaction),
                controllingFactionState = VALUES(controllingFactionState),
                distanceToArrival = VALUES(distanceToArrival),
                primaryEconomy     = VALUES(primaryEconomy),
                economies     = VALUES(economies),
                government     = VALUES(government),
                services     = VALUES(services),
                stationType     = VALUES(stationType),
                hasLargePad     = VALUES(hasLargePad),
                hasMediumPad     = VALUES(hasMediumPad),
                hasSmallPad     = VALUES(hasSmallPad)
            """)
    void upsert(@BindBean StationsDao.Station data);


    class StationMapper implements RowMapper<Station> {

        @Override public Station map(ResultSet rs, StatementContext ctx) throws SQLException {
            Station entity = new Station();
            entity.setControllingFaction(rs.getString("controllingFaction"));
            entity.setControllingFactionState(rs.getString("controllingFactionState"));
            entity.setDistanceToArrival(rs.getDouble("distanceToArrival"));
            entity.setRealName(rs.getString("realName"));
            entity.setPrimaryEconomy(rs.getString("primaryEconomy"));
            entity.setEconomies(rs.getString("economies"));
            entity.setGovernment(rs.getString("government"));
            entity.setServices(rs.getString("services"));
            entity.setHasLargePad(rs.getBoolean("hasLargePad"));
            entity.setHasMediumPad(rs.getBoolean("hasMediumPad"));
            entity.setHasSmallPad(rs.getBoolean("hasSmallPad"));
            entity.setStationType(rs.getString("stationType"));
            return entity;
        }
    }

    class Station {
        private Long systemAddress;
        private Long stationId;
        private String realName;
        private String stationType;
        private String controllingFaction;
        private String controllingFactionState;
        private Double distanceToArrival;
        private String primaryEconomy;
        private String economies;
        private String government;
        private String services;
        private Boolean hasLargePad;
        private Boolean hasMediumPad;
        private Boolean hasSmallPad;

        public Long getSystemAddress() {
            return systemAddress;
        }

        public void setSystemAddress(Long systemAddress) {
            this.systemAddress = systemAddress;
        }

        public Long getStationId() {
            return stationId;
        }

        public void setStationId(Long stationId) {
            this.stationId = stationId;
        }

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }

        public String getControllingFaction() {
            return controllingFaction;
        }

        public void setControllingFaction(String controllingFaction) {
            this.controllingFaction = controllingFaction;
        }

        public String getControllingFactionState() {
            return controllingFactionState;
        }

        public void setControllingFactionState(String controllingFactionState) {
            this.controllingFactionState = controllingFactionState;
        }

        public Double getDistanceToArrival() {
            return distanceToArrival;
        }

        public void setDistanceToArrival(Double distanceToArrival) {
            this.distanceToArrival = distanceToArrival;
        }

        public String getPrimaryEconomy() {
            return primaryEconomy;
        }

        public void setPrimaryEconomy(String primaryEconomy) {
            this.primaryEconomy = primaryEconomy;
        }

        public String getEconomies() {
            return economies;
        }

        public void setEconomies(String economies) {
            this.economies = economies;
        }

        public String getGovernment() {
            return government;
        }

        public void setGovernment(String government) {
            this.government = government;
        }

        public String getServices() {
            return services;
        }

        public void setServices(String services) {
            this.services = services;
        }

        public Boolean getHasLargePad() {
            return hasLargePad;
        }

        public void setHasLargePad(Boolean hasLargePad) {
            this.hasLargePad = hasLargePad;
        }

        public Boolean getHasMediumPad() {
            return hasMediumPad;
        }

        public void setHasMediumPad(Boolean hasMediumPad) {
            this.hasMediumPad = hasMediumPad;
        }

        public Boolean getHasSmallPad() {
            return hasSmallPad;
        }

        public void setHasSmallPad(Boolean hasSmallPad) {
            this.hasSmallPad = hasSmallPad;
        }

        public String getStationType() {
            return stationType;
        }

        public void setStationType(String stationType) {
            this.stationType = stationType;
        }
    }
}
