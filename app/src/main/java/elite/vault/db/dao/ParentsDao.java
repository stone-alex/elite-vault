package elite.vault.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;

@RegisterRowMapper(ParentsDao.ParentMapper.class)
public interface ParentsDao {


    @SqlUpdate("""
            insert into parents (systemAddress, bodyId, parentBodyId, parentType)
            values (:systemAddress, :bodyId, :parentBodyId, :parentType)
            on duplicate key update
                parentBodyId = values(parentBodyId),
                parentType = values(parentType)
            """)
    void upsert(@BindBean ParentsDao.Parent data);


    class ParentMapper implements RowMapper<Parent> {

        @Override public Parent map(ResultSet rs, StatementContext ctx) throws SQLException {
            Parent entity = new Parent();
            entity.setSystemAddress(rs.getLong("systemAddress"));
            entity.setBodyId(rs.getLong("bodyId"));
            entity.setParentBodyId(rs.getInt("parentBodyId"));
            entity.setParentType(rs.getString("parentType"));
            return entity;
        }
    }

    class Parent {
        private Long systemAddress;
        private Long bodyId;
        private Integer parentBodyId;
        private String parentType;

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

        public Integer getParentBodyId() {
            return parentBodyId;
        }

        public void setParentBodyId(Integer parentBodyId) {
            this.parentBodyId = parentBodyId;
        }

        public String getParentType() {
            return parentType;
        }

        public void setParentType(String parentType) {
            this.parentType = parentType;
        }
    }
}
