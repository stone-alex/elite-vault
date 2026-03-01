package elite.vault.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;

@RegisterRowMapper(MarketDao.MarketMapper.class)
public interface MarketDao {

    @SqlUpdate("""
            INSERT OR REPLACE INTO market (marketId, timestamp, starSystem, stationName, data)
            VALUES(:marketId, :timestamp, :starSystem, :stationName, :data)
            ON CONFLICT(marketId) DO UPDATE SET
            data = excluded.data,
            timestamp = excluded.timestamp,
            starSystem = excluded.starSystem,
            stationName = excluded.stationName
            """)
    void upsert(@BindBean MarketDao.Market data);


    class MarketMapper implements RowMapper<Market> {

        @Override public Market map(ResultSet rs, StatementContext ctx) throws SQLException {
            Market entity = new Market();
            entity.setTimestamp(rs.getString("timestamp"));
            entity.setData(rs.getString("data"));
            entity.setStarSystem(rs.getString("starSystem"));
            entity.setStationName(rs.getString("stationName"));
            entity.setMarketId(rs.getLong("marketId"));
            return entity;
        }
    }

    class Market {
        String timestamp;
        String starSystem;
        String stationName;
        Long marketId;
        String data;

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

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
    }
}
