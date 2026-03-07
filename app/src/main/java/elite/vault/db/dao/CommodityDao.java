package elite.vault.db.dao;

import elite.vault.db.projections.CommodityOfferProjection;
import elite.vault.db.projections.TradePairProjection;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
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
            INSERT INTO market_commodity (marketId, commodity, buyPrice, sellPrice, stock, demand, systemAddress, x, y, z, timestamp, pos)
            VALUES (:marketId, :commodity, :buyPrice, :sellPrice, :stock, :demand, :systemAddress, :x, :y, :z, CURRENT_TIMESTAMP, ST_PointFromText(CONCAT('POINT(', :x, ' ', :y, ')')))
            ON DUPLICATE KEY UPDATE
                commodity = VALUES(commodity),
                buyPrice  = VALUES(buyPrice),
                sellPrice = VALUES(sellPrice),
                stock     = VALUES(stock),
                demand    = VALUES(demand),
                timestamp = CURRENT_TIMESTAMP,
                x = VALUES(x),
                y = VALUES(y),
                z = VALUES(z),
                pos = ST_PointFromText(CONCAT('POINT(', :x, ' ', :y, ')'))
            """)
    void upsert(@BindBean CommodityDao.Commodity data);

    @RegisterBeanMapper(CommodityOfferProjection.class)
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
                WHERE LOWER(mc.commodity) = LOWER(:commodity)
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

    @RegisterBeanMapper(TradePairProjection.class)
    @SqlQuery("""
            SELECT
                ss.starName           AS buySystem,
                m.stationName         AS buyStation,
                mc.commodity          AS commodity,
                MIN(mc.buyPrice)      AS buyPrice,
                mc.stock              AS buyStock,
                mc.marketId           AS buyMarketId,
                mc.systemAddress      AS buySystemAddress,
                mc.x                  AS buyX,
                mc.y                  AS buyY,
                st.distanceToArrival  AS buyDistToArrival,
                ROUND(ST_Distance(POINT(:refX, :refY), mc.pos), 1) AS buyDistanceLy
            FROM market_commodity mc
            INNER JOIN star_system ss ON ss.systemAddress = mc.systemAddress
            INNER JOIN market      m  ON m.marketId       = mc.marketId
            INNER JOIN stations    st ON st.systemAddress  = mc.systemAddress AND st.stationId = mc.marketId
            WHERE mc.buyPrice > 0
              AND mc.stock > 0
              AND st.distanceToArrival <= :maxDistFromEntrance
              AND MBRContains(ST_Buffer(POINT(:refX, :refY), :jumpRange), mc.pos)
              AND ST_Distance(POINT(:refX, :refY), mc.pos) <= :jumpRange
            GROUP BY mc.commodity
            ORDER BY buyPrice ASC
            LIMIT 10
            """)
    List<TradePairProjection> findBuyOffers(
            @Bind("jumpRange") double jumpRange,
            @Bind("refX") double refX,
            @Bind("refY") double refY,
            @Bind("maxDistFromEntrance") double maxDistFromEntrance
    );

    @RegisterBeanMapper(TradePairProjection.class)
    @SqlQuery("""
            SELECT
                ss.starName           AS sellSystem,
                m.stationName         AS sellStation,
                mc.commodity          AS commodity,
                mc.sellPrice          AS sellPrice,
                mc.demand             AS sellDemand,
                mc.marketId           AS sellMarketId,
                mc.systemAddress      AS sellSystemAddress,
                st.distanceToArrival  AS sellDistToArrival,
                ROUND(ST_Distance(POINT(:refX, :refY), mc.pos), 1) AS legDistanceLy
            FROM market_commodity mc
            INNER JOIN star_system ss ON ss.systemAddress = mc.systemAddress
            INNER JOIN market      m  ON m.marketId       = mc.marketId
            INNER JOIN stations    st ON st.systemAddress  = mc.systemAddress
                                     AND st.stationId     = mc.marketId
            WHERE LOWER(mc.commodity) = LOWER(:commodity)
              AND mc.sellPrice > :minSellPrice
              AND mc.demand > 0
              AND mc.marketId != :excludeMarketId
              AND st.distanceToArrival <= :maxDistFromEntrance
              AND MBRContains(ST_Buffer(POINT(:refX, :refY), :jumpRange), mc.pos)
              AND ST_Distance(POINT(:refX, :refY), mc.pos) <= :jumpRange
            ORDER BY mc.sellPrice DESC
            LIMIT 1
            """)
    List<TradePairProjection> findBestSellFor(
            @Bind("commodity") String commodity,
            @Bind("minSellPrice") double minSellPrice,
            @Bind("excludeMarketId") long excludeMarketId,
            @Bind("jumpRange") double jumpRange,
            @Bind("refX") double refX,
            @Bind("refY") double refY,
            @Bind("maxDistFromEntrance") double maxDistFromEntrance
    );

    @RegisterBeanMapper(TradePairProjection.class)
    @SqlQuery("""
            SELECT
                ss.starName           AS buySystem,
                m.stationName         AS buyStation,
                mc.commodity          AS commodity,
                mc.buyPrice           AS buyPrice,
                mc.stock              AS buyStock,
                mc.marketId           AS buyMarketId,
                mc.systemAddress      AS buySystemAddress,
                mc.x                  AS buyX,
                mc.y                  AS buyY,
                0                     AS buyDistToArrival,
                0                     AS buyDistanceLy
            FROM market_commodity mc
            INNER JOIN star_system ss ON ss.systemAddress = mc.systemAddress
            INNER JOIN market      m  ON m.marketId       = mc.marketId
            WHERE mc.marketId = :marketId
              AND mc.buyPrice > 0
              AND mc.stock > 0
            ORDER BY mc.buyPrice ASC
            LIMIT 10
            """)
    List<TradePairProjection> findBuyOffersAtStation(
            @Bind("marketId") long marketId
    );

    class CommodityMapper implements RowMapper<Commodity> {

        @Override
        public Commodity map(ResultSet rs, StatementContext ctx) throws SQLException {
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

        public String getPosWkt() {
            return String.format("POINT(%f %f)", getX(), getY());
        }
    }
}