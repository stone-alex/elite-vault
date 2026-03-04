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
                insert into factions (systemAddress, factionName, allegiance, government, influence, factionState, happiness)
                values (:systemAddress,  :factionName, :allegiance, :government, :influence, :factionState, :happiness)
                    on duplicate key update
                    systemAddress = VALUES(systemAddress),
                    factionName = VALUES(factionName),
                    allegiance = VALUES(allegiance),
                    government = VALUES(government),
                    influence = VALUES(influence),
                    factionState = VALUES(factionState),
                    happiness = VALUES(happiness)
            """)
    void upsert(@BindBean FactionsDao.Faction data);


    @SqlQuery("""
            select * from factions where systemAddress = :systemAddress
            """)
    List<Faction> getFactions(Long systemAddress);


    class FactionMapper implements RowMapper<Faction> {
        @Override public Faction map(ResultSet rs, StatementContext ctx) throws SQLException {
            Faction entity = new Faction();
            entity.setSystemAddress(rs.getLong("systemAddress"));
            entity.setFactionName(rs.getString("factionName"));
            entity.setFactionState(rs.getString("factionState"));
            entity.setAllegiance(rs.getString("allegiance"));
            entity.setGovernment(rs.getString("government"));
            entity.setInfluence(rs.getDouble("influence"));
            entity.setHappiness(rs.getString("happiness"));
            return entity;
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

        public Long getSystemAddress() {
            return systemAddress;
        }

        public void setSystemAddress(Long systemAddress) {
            this.systemAddress = systemAddress;
        }

        public String getFactionName() {
            return factionName;
        }

        public void setFactionName(String factionName) {
            this.factionName = factionName;
        }

        public String getAllegiance() {
            return allegiance;
        }

        public void setAllegiance(String allegiance) {
            this.allegiance = allegiance;
        }

        public String getGovernment() {
            return government;
        }

        public void setGovernment(String government) {
            this.government = government;
        }

        public Double getInfluence() {
            return influence;
        }

        public void setInfluence(Double influence) {
            this.influence = influence;
        }

        public String getFactionState() {
            return factionState;
        }

        public void setFactionState(String factionState) {
            this.factionState = factionState;
        }

        public String getHappiness() {
            return happiness;
        }

        public void setHappiness(String happiness) {
            this.happiness = happiness;
        }
    }
}
