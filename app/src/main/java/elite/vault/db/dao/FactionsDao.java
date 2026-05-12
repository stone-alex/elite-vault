package elite.vault.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@RegisterRowMapper(FactionsDao.FactionMapper.class)
public interface FactionsDao {

    @SqlUpdate("""
            INSERT INTO factions
                (systemAddress, factionName, allegiance, government, influence, factionState, happiness, isPirate)
            VALUES
                (:systemAddress, :factionName, :allegiance, :government, :influence, :factionState, :happiness,
                    CASE WHEN
                        (SELECT COUNT(*) FROM pirate_faction_keywords WHERE :factionName LIKE '%' || keyword || '%') > 0
                        OR :government = 'Anarchy'
                    THEN 1 ELSE 0 END
                )
            ON CONFLICT(systemAddress, factionName) DO UPDATE SET
                allegiance   = excluded.allegiance,
                government   = excluded.government,
                influence    = excluded.influence,
                factionState = excluded.factionState,
                happiness    = excluded.happiness,
                received_at  = datetime('now')
                -- isPirate intentionally not updated - set once on insert, stable
            """)
    void upsert(@BindBean Faction data);


    @SqlQuery("SELECT * FROM factions WHERE systemAddress = :systemAddress")
    List<Faction> getFactions(long systemAddress);


    class FactionMapper implements RowMapper<Faction> {
        @Override
        public Faction map(ResultSet rs, StatementContext ctx) throws SQLException {
            Faction e = new Faction();
            e.setSystemAddress(rs.getLong("systemAddress"));
            e.setFactionName(rs.getString("factionName"));
            e.setAllegiance(rs.getString("allegiance"));
            e.setGovernment(rs.getString("government"));
            e.setInfluence(rs.getDouble("influence"));
            e.setFactionState(rs.getString("factionState"));
            e.setHappiness(rs.getString("happiness"));
            e.setPirate(rs.getBoolean("isPirate"));
            return e;
        }
    }


    class Faction {
        private Long systemAddress;
        private String factionName;
        private String allegiance;
        private String government;
        private Double influence;
        private String factionState;
        private String happiness;
        private boolean isPirate;

        public Long getSystemAddress() {
            return systemAddress;
        }

        public void setSystemAddress(Long v) {
            this.systemAddress = v;
        }

        public String getFactionName() {
            return factionName;
        }

        public void setFactionName(String v) {
            this.factionName = v;
        }

        public String getAllegiance() {
            return allegiance;
        }

        public void setAllegiance(String v) {
            this.allegiance = v;
        }

        public String getGovernment() {
            return government;
        }

        public void setGovernment(String v) {
            this.government = v;
        }

        public Double getInfluence() {
            return influence;
        }

        public void setInfluence(Double v) {
            this.influence = v;
        }

        public String getFactionState() {
            return factionState;
        }

        public void setFactionState(String v) {
            this.factionState = v;
        }

        public String getHappiness() {
            return happiness;
        }

        public void setHappiness(String v) {
            this.happiness = v;
        }

        public boolean isPirate() {
            return isPirate;
        }

        public void setPirate(boolean v) {
            this.isPirate = v;
        }
    }
}