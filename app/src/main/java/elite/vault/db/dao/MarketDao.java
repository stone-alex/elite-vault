package elite.vault.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlScript;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;

@RegisterRowMapper(MarketDao.MarketMapper.class)
public interface MarketDao {

    @SqlUpdate("""
            INSERT INTO market (marketId, timestamp, starSystem, stationName, systemAddress, data)
            VALUES (:marketId, CURRENT_TIMESTAMP, :starSystem, :stationName, :systemAddress, :data)
            ON DUPLICATE KEY UPDATE
                timestamp     = CURRENT_TIMESTAMP,
                starSystem    = VALUES(starSystem),
                stationName   = VALUES(stationName),
                systemAddress = VALUES(systemAddress),
                data          = VALUES(data)
            """)
    void upsert(@BindBean Market data);

    @SqlScript("""
            delete from market_commodity
            where timestamp < CURRENT_TIMESTAMP - interval 3 hour
            limit 1000;
            
            delete from market
            where timestamp < CURRENT_TIMESTAMP - interval 3 hour
            limit 500;
            """)
    void prune();

    class MarketMapper implements RowMapper<Market> {

        @Override
        public Market map(ResultSet rs, StatementContext ctx) throws SQLException {
            Market entity = new Market();
            entity.setData(rs.getString("data"));
            entity.setStarSystem(rs.getString("starSystem"));
            entity.setStationName(rs.getString("stationName"));
            entity.setMarketId(rs.getLong("marketId"));
            entity.setSystemAddress(rs.getLong("systemAddress"));
            return entity;
        }
    }

    class Market {
        private String starSystem;
        private String stationName;
        Long systemAddress;
        private Long marketId;
        private String data;

        public String getStarSystem() {
            return starSystem;
        }

        public void setStarSystem(String starSystem) {
            this.starSystem = starSystem;
        }

        public String getStationName() {
            return stationName;
        }

        public void setStationName(String stationName) {
            this.stationName = stationName;
        }

        public Long getMarketId() {
            return marketId;
        }

        public void setMarketId(Long marketId) {
            this.marketId = marketId;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public Long getSystemAddress() {
            return systemAddress;
        }

        public void setSystemAddress(Long systemAddress) {
            this.systemAddress = systemAddress;
        }
    }
}