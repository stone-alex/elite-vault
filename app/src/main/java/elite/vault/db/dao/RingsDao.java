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
import java.util.List;

import static elite.vault.db.dao.SqlFragments.NEIGHBOR_SYSTEMS_CTE;

@RegisterRowMapper(RingsDao.RingMawpper.class)
public interface RingsDao {

    @SqlUpdate("""
            INSERT INTO rings (systemAddress, bodyId, ringType, mass, innerRadius, outerRadius, signals)
            VALUES (:systemAddress, :bodyId, :ringType, :mass, :innerRadius, :outerRadius, :signals)
            ON DUPLICATE KEY UPDATE
                ringType    = VALUES(ringType),
                mass        = VALUES(mass),
                innerRadius = VALUES(innerRadius),
                outerRadius = VALUES(outerRadius),
                signals     = VALUES(signals)
            """)
    void upsert(@BindBean RingsDao.Ring data);


    @SqlQuery(NEIGHBOR_SYSTEMS_CTE + """
            # noinspection SqlResolve
            SELECT r.*
            FROM rings r
            JOIN neighbors n ON n.systemAddress = r.systemAddress
            """)
    List<Ring> findByNeighborSystem(
            @Bind("starName") String starName,
            @Bind("range") double range
    );


    class RingMawpper implements RowMapper<Ring> {

        @Override public Ring map(ResultSet rs, StatementContext ctx) throws SQLException {
            Ring entity = new Ring();
            entity.setSystemAddress(rs.getLong("systemAddress"));
            entity.setBodyId(rs.getLong("bodyId"));
            entity.setRingType(rs.getString("ringType"));
            entity.setMass(rs.getDouble("mass"));
            entity.setInnerRadius(rs.getDouble("innerRadius"));
            entity.setOuterRadius(rs.getDouble("outerRadius"));
            entity.setSignals(rs.getString("signals"));
            return entity;
        }
    }

    class Ring {
        private Long systemAddress;
        private Long bodyId;
        private String ringType;
        private Double mass;
        private Double innerRadius;
        private Double outerRadius;
        private String signals;

        public Long getSystemAddress() {
            return systemAddress;
        }

        public void setSystemAddress(Long systemAddress) {
            this.systemAddress = systemAddress;
        }

        public Long getBodyId() {
            return bodyId;
        }

        public void setBodyId(Long bodyId) {
            this.bodyId = bodyId;
        }

        public String getRingType() {
            return ringType;
        }

        public void setRingType(String ringType) {
            this.ringType = ringType;
        }

        public Double getMass() {
            return mass;
        }

        public void setMass(Double mass) {
            this.mass = mass;
        }

        public Double getInnerRadius() {
            return innerRadius;
        }

        public void setInnerRadius(Double innerRadius) {
            this.innerRadius = innerRadius;
        }

        public Double getOuterRadius() {
            return outerRadius;
        }

        public void setOuterRadius(Double outerRadius) {
            this.outerRadius = outerRadius;
        }

        public String getSignals() {
            return signals;
        }

        public void setSignals(String signals) {
            this.signals = signals;
        }
    }
}
