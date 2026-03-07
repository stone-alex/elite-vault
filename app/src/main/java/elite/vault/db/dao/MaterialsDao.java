package elite.vault.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;

@RegisterRowMapper(MaterialsDao.MaterialMapper.class)
public interface MaterialsDao {

    @SqlUpdate("""
            INSERT INTO materials (systemAddress, bodyId, materialName, percent)
            VALUES (:systemAddress, :bodyId, :materialName, :percent)
            ON DUPLICATE KEY UPDATE
                materialName = VALUES(materialName),
                percent      = VALUES(percent)
            """)
    void upsert(@BindBean MaterialsDao.Material data);


    class MaterialMapper implements RowMapper<Material> {
        @Override public Material map(ResultSet rs, StatementContext ctx) throws SQLException {
            Material entity = new Material();
            entity.setSystemAddress(rs.getLong("systemAddress"));
            entity.setBodyId(rs.getLong("bodyId"));
            entity.setMaterialName(rs.getString("materialName"));
            entity.setPercent(rs.getDouble("percent"));
            return entity;
        }
    }

    class Material {
        private Long systemAddress;
        private Long bodyId;
        private String materialName;
        private Double percent;

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

        public String getMaterialName() {
            return materialName;
        }

        public void setMaterialName(String materialName) {
            this.materialName = materialName;
        }

        public Double getPercent() {
            return percent;
        }

        public void setPercent(Double percent) {
            this.percent = percent;
        }
    }
}
