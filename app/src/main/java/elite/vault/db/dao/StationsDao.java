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

    /**
     * Upsert a station.
     * x, y, z are resolved by StationManager from star_system before calling this.
     * Stations whose system is not yet in star_system are dropped by StationManager
     * and never reach this method.
     */
    @SqlUpdate("""
            INSERT INTO stations (
                systemAddress, marketId, realName,
                controllingFaction, controllingFactionState,
                distanceToArrival, primaryEconomy, economies,
                government, services,
                hasLargePad, hasMediumPad, hasSmallPad,
                stationType,
                x, y, z
            )
            VALUES (
                :systemAddress, :marketId, :realName,
                :controllingFaction, :controllingFactionState,
                :distanceToArrival, :primaryEconomy, :economies,
                :government, :services,
                :hasLargePad, :hasMediumPad, :hasSmallPad,
                :stationType,
                :x, :y, :z
            )
            ON DUPLICATE KEY UPDATE
                realName                = VALUES(realName),
                controllingFaction      = VALUES(controllingFaction),
                controllingFactionState = VALUES(controllingFactionState),
                distanceToArrival       = VALUES(distanceToArrival),
                primaryEconomy          = VALUES(primaryEconomy),
                economies               = VALUES(economies),
                government              = VALUES(government),
                services                = VALUES(services),
                stationType             = VALUES(stationType),
                hasLargePad             = VALUES(hasLargePad),
                hasMediumPad            = VALUES(hasMediumPad),
                hasSmallPad             = VALUES(hasSmallPad),
                x                       = VALUES(x),
                y                       = VALUES(y),
                z                       = VALUES(z)
            """)
    void upsert(@BindBean Station data);


    class StationMapper implements RowMapper<Station> {
        @Override
        public Station map(ResultSet rs, StatementContext ctx) throws SQLException {
            Station e = new Station();
            e.setControllingFaction(rs.getString("controllingFaction"));
            e.setControllingFactionState(rs.getString("controllingFactionState"));
            e.setDistanceToArrival(rs.getDouble("distanceToArrival"));
            e.setRealName(rs.getString("realName"));
            e.setPrimaryEconomy(rs.getString("primaryEconomy"));
            e.setEconomies(rs.getString("economies"));
            e.setGovernment(rs.getString("government"));
            e.setServices(rs.getString("services"));
            e.setHasLargePad(rs.getBoolean("hasLargePad"));
            e.setHasMediumPad(rs.getBoolean("hasMediumPad"));
            e.setHasSmallPad(rs.getBoolean("hasSmallPad"));
            e.setStationType(rs.getString("stationType"));
            e.setX(rs.getDouble("x"));
            e.setY(rs.getDouble("y"));
            e.setZ(rs.getDouble("z"));
            return e;
        }
    }

    class Station {
        private Long systemAddress;
        private Long marketId;
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
        private double x;
        private double y;
        private double z;

        public Long getSystemAddress() {
            return systemAddress;
        }

        public void setSystemAddress(Long v) {
            this.systemAddress = v;
        }

        public Long getMarketId() {
            return marketId;
        }

        public void setMarketId(Long v) {
            this.marketId = v;
        }

        public String getRealName() {
            return realName;
        }

        public void setRealName(String v) {
            this.realName = v;
        }

        public String getControllingFaction() {
            return controllingFaction;
        }

        public void setControllingFaction(String v) {
            this.controllingFaction = v;
        }

        public String getControllingFactionState() {
            return controllingFactionState;
        }

        public void setControllingFactionState(String v) {
            this.controllingFactionState = v;
        }

        public Double getDistanceToArrival() {
            return distanceToArrival;
        }

        public void setDistanceToArrival(Double v) {
            this.distanceToArrival = v;
        }

        public String getPrimaryEconomy() {
            return primaryEconomy;
        }

        public void setPrimaryEconomy(String v) {
            this.primaryEconomy = v;
        }

        public String getEconomies() {
            return economies;
        }

        public void setEconomies(String v) {
            this.economies = v;
        }

        public String getGovernment() {
            return government;
        }

        public void setGovernment(String v) {
            this.government = v;
        }

        public String getServices() {
            return services;
        }

        public void setServices(String v) {
            this.services = v;
        }

        public Boolean getHasLargePad() {
            return hasLargePad;
        }

        public void setHasLargePad(Boolean v) {
            this.hasLargePad = v;
        }

        public Boolean getHasMediumPad() {
            return hasMediumPad;
        }

        public void setHasMediumPad(Boolean v) {
            this.hasMediumPad = v;
        }

        public Boolean getHasSmallPad() {
            return hasSmallPad;
        }

        public void setHasSmallPad(Boolean v) {
            this.hasSmallPad = v;
        }

        public String getStationType() {
            return stationType;
        }

        public void setStationType(String v) {
            this.stationType = v;
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
    }
}