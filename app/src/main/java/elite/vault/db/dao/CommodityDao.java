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

@RegisterRowMapper(CommodityDao.CommodityMapper.class)
public interface CommodityDao {

    @SqlUpdate("""
            INSERT INTO market_commodity (marketId,commodity, buyPrice, sellPrice, stock, demand, systemAddress, x, y, z, timestamp, pos)
            VALUES (:marketId, :commodity, :buyPrice, :sellPrice, :stock, :demand, :systemAddress, :x, :y, :z, :timestamp, ST_PointFromText(CONCAT('POINT(', :x, ' ', :y, ')')))
            ON DUPLICATE KEY UPDATE
                commodity = VALUES(commodity),
                buyPrice  = VALUES(buyPrice),
                sellPrice = VALUES(sellPrice),
                stock     = VALUES(stock),
                demand    = VALUES(demand),
                timestamp = VALUES(timestamp),
                x = VALUES(x),
                y = VALUES(y),
                z = VALUES(z),
                pos = ST_PointFromText(CONCAT('POINT(', :x, ' ', :y, ')'))
            """)
    void upsert(@BindBean CommodityDao.Commodity data);


    @SqlQuery("""
            SELECT
                    ss.starName as starName,
                    m.stationName as stationName,
                    mc.commodity as commodity,
                    mc.sellPrice as sellPrice,
                    mc.stock as stock,
                    ROUND(ST_Distance(POINT(:refX, :refY), mc.pos), 1) AS distanceLy,
                    mc.marketId as marketId,
                    mc.systemAddress as systemAddress
                FROM market_commodity mc
                INNER JOIN star_system ss ON ss.systemAddress = mc.systemAddress
                INNER JOIN market      m  ON m.marketId = mc.marketId
                WHERE mc.commodity = :commodity
                  AND mc.stock > 0
                  AND mc.sellPrice > 0
                  AND MBRContains(ST_Buffer(POINT(:refX, :refY), :maxLy), mc.pos)
                  AND ST_Distance(POINT(:refX, :refY), mc.pos) <= :maxLy
                ORDER BY mc.sellPrice DESC, mc.stock DESC
                LIMIT 20
            """)
    List<CommodityOfferProjection> findBestCommodityOffers(
            @Bind("commodity") String commodity,
            @Bind("maxLy") double maxLy,
            @Bind("refX") double refX,
            @Bind("refY") double refY
    );
    record CommodityOfferProjection(
            String starName,
            String stationName,
            String commodity,
            double sellPrice,
            int stock,
            double distanceLy,
            long marketId,
            long systemAddress
    ) {
    }


    class CommodityMapper implements RowMapper<Commodity> {

        @Override public Commodity map(ResultSet rs, StatementContext ctx) throws SQLException {
            Commodity entity = new Commodity();
            entity.setCommodity(rs.getString("commodity"));
            entity.setMarketId(rs.getLong("marketId"));
            entity.setBuyPrice(rs.getDouble("buyPrice"));
            entity.setSellPrice(rs.getDouble("sellPrice"));
            entity.setStock(rs.getInt("stock"));
            entity.setDemand(rs.getInt("demand"));
            entity.setSystemAddress(rs.getLong("systemAddress"));
            entity.setX(rs.getDouble("x"));
            entity.setY(rs.getDouble("y"));
            entity.setZ(rs.getDouble("z"));
            entity.setTimestamp(rs.getString("timestamp"));
            return entity;
        }
    }

    class Commodity {
        Long marketId;
        String commodity;
        Double buyPrice;
        Double sellPrice;
        Integer stock;
        Integer demand;
        Long systemAddress;
        Double x, y, z;
        String timestamp;

        public String getCommodity() {
            return commodity;
        }

        public void setCommodity(String commodity) {
            this.commodity = commodity;
        }

        public Double getBuyPrice() {
            return buyPrice;
        }

        public void setBuyPrice(Double buyPrice) {
            this.buyPrice = buyPrice;
        }

        public Double getSellPrice() {
            return sellPrice;
        }

        public void setSellPrice(Double sellPrice) {
            this.sellPrice = sellPrice;
        }

        public Integer getStock() {
            return stock;
        }

        public void setStock(Integer stock) {
            this.stock = stock;
        }

        public Integer getDemand() {
            return demand;
        }

        public void setDemand(Integer demand) {
            this.demand = demand;
        }

        public Long getSystemAddress() {
            return systemAddress;
        }

        public void setSystemAddress(Long systemAddress) {
            this.systemAddress = systemAddress;
        }

        public Double getX() {
            return x;
        }

        public void setX(Double x) {
            this.x = x;
        }

        public Double getY() {
            return y;
        }

        public void setY(Double y) {
            this.y = y;
        }

        public Double getZ() {
            return z;
        }

        public void setZ(Double z) {
            this.z = z;
        }

        public Long getMarketId() {
            return marketId;
        }

        public void setMarketId(Long marketId) {
            this.marketId = marketId;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getPosWkt() {
            return String.format("POINT(%f %f)", getX(), getY());
        }
    }
}
