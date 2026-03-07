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

@RegisterRowMapper(StellarObjectDao.StellarObjectMapper.class)
public interface StellarObjectDao {

    /**
     * Upserts a stellar object entry.
     * - Inserts new row if systemAddress does not exist
     * - Updates all fields if systemAddress already exists (latest data wins)
     * <p>
     * Uses MariaDB/MySQL native ON DUPLICATE KEY UPDATE syntax.
     * Requires UNIQUE or PRIMARY KEY on systemAddress (already present in schema).
     */
    @SqlUpdate("""
            INSERT INTO stellar_object (bodyId, systemAddress, bodyName, atmosphereType, distanceFromArrivalLs, landable, massEm, meanAnomaly, orbitalInclination, orbitalPeriod, periapsis, planetClass, radius, rotationPeriod, semiMajorAxis, surfaceGravity, surfacePressure, surfaceTemperature, terraformState, tidalLock, volcanism, eccentricity)
              VALUES (:bodyId,  :systemAddress,  :bodyName, :atmosphereType, :distanceFromArrivalLs, :landable, :massEm, :meanAnomaly, :orbitalInclination, :orbitalPeriod, :periapsis, :planetClass, :radius, :rotationPeriod, :semiMajorAxis, :surfaceGravity, :surfacePressure, :surfaceTemperature, :terraformState, :tidalLock, :volcanism, :eccentricity)
            ON DUPLICATE KEY UPDATE
                bodyName    = VALUES(bodyName),
                bodyId      = VALUES(bodyId),
                  atmosphereType = VALUES(atmosphereType),
                  distanceFromArrivalLs = values(distanceFromArrivalLs),
                  landable = values(landable),
                  massEm = values(massEm),
                  meanAnomaly = values(meanAnomaly),
                  orbitalInclination = values(orbitalInclination),
                  orbitalPeriod = values(orbitalPeriod),
                  periapsis = values(periapsis),
                  planetClass = values(planetClass),
                  radius = values(radius),
                  rotationPeriod = values(rotationPeriod),
                  semiMajorAxis = values(semiMajorAxis),
                  surfaceGravity = values(surfaceGravity),
                  surfacePressure = values(surfacePressure),
                  surfaceTemperature = values(surfaceTemperature),
                  terraformState = values(terraformState),
                  tidalLock = values(tidalLock),
                  volcanism = values(volcanism),
                  eccentricity = values(eccentricity)
            """)
    void upsert(@BindBean StellarObject data);

    @SqlQuery("""
                select * from stellar_object where systemAddress = :systemAddress and bodyId = :bodyId
            """)
    StellarObject findBy(@Bind Long systemAddress, @Bind Long bodyId);


    class StellarObjectMapper implements RowMapper<StellarObject> {
        @Override
        public StellarObject map(ResultSet rs, StatementContext ctx) throws SQLException {
            StellarObject entity = new StellarObject();
            entity.setTimestamp(rs.getString("received_at"));
            entity.setBodyName(rs.getString("bodyName"));
            entity.setSystemAddress(rs.getLong("systemAddress"));
            entity.setBodyId(rs.getLong("bodyId"));

            entity.setAtmosphereType(rs.getString("atmosphereType"));
            entity.setPlanetClass(rs.getString("planetClass"));
            entity.setDistanceFromArrivalLs(rs.getDouble("distanceFromArrivalLs"));
            entity.setEccentricity(rs.getDouble("eccentricity"));
            entity.setLandable(rs.getBoolean("landable"));
            entity.setMassEm(rs.getDouble("massEm"));
            entity.setMeanAnomaly(rs.getDouble("meanAnomaly"));
            entity.setOrbitalInclination(rs.getDouble("orbitalInclination"));
            entity.setOrbitalPeriod(rs.getDouble("orbitalPeriod"));
            entity.setPeriapsis(rs.getDouble("periapsis"));
            entity.setRotationPeriod(rs.getDouble("rotationPeriod"));
            entity.setSemiMajorAxis(rs.getDouble("semiMajorAxis"));
            entity.setSurfaceGravity(rs.getDouble("surfaceGravity"));
            entity.setSurfacePressure(rs.getDouble("surfacePressure"));
            entity.setSurfaceTemperature(rs.getDouble("surfaceTemperature"));
            entity.setLandable(rs.getBoolean("landable"));
            entity.setTidalLock(rs.getBoolean("tidalLock"));

            return entity;
        }
    }


    class StellarObject {
        private String timestamp;
        private String bodyName;
        private Long systemAddress;
        private Long bodyId;

        private String atmosphereType;
        private String planetClass;
        private String terraformState;
        private String volcanism;

        private Double eccentricity;
        private Double distanceFromArrivalLs;
        private Double massEm;
        private Double meanAnomaly;
        private Double orbitalInclination;
        private Double orbitalPeriod;
        private Double periapsis;
        private Double radius;
        private Double rotationPeriod;
        private Double semiMajorAxis;
        private Double surfaceGravity;
        private Double surfacePressure;
        private Double surfaceTemperature;
        private Boolean landable;
        private boolean tidalLock;


        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }


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

        public String getBodyName() {
            return bodyName;
        }

        public void setBodyName(String bodyName) {
            this.bodyName = bodyName;
        }

        public String getAtmosphereType() {
            return atmosphereType;
        }

        public void setAtmosphereType(String atmosphereType) {
            this.atmosphereType = atmosphereType;
        }

        public String getPlanetClass() {
            return planetClass;
        }

        public void setPlanetClass(String planetClass) {
            this.planetClass = planetClass;
        }

        public String getTerraformState() {
            return terraformState;
        }

        public void setTerraformState(String terraformState) {
            this.terraformState = terraformState;
        }

        public String getVolcanism() {
            return volcanism;
        }

        public void setVolcanism(String volcanism) {
            this.volcanism = volcanism;
        }

        public Double getEccentricity() {
            return eccentricity;
        }

        public void setEccentricity(Double eccentricity) {
            this.eccentricity = eccentricity;
        }

        public Double getDistanceFromArrivalLs() {
            return distanceFromArrivalLs;
        }

        public void setDistanceFromArrivalLs(Double distanceFromArrivalLs) {
            this.distanceFromArrivalLs = distanceFromArrivalLs;
        }

        public Double getMassEm() {
            return massEm;
        }

        public void setMassEm(Double massEm) {
            this.massEm = massEm;
        }

        public Double getMeanAnomaly() {
            return meanAnomaly;
        }

        public void setMeanAnomaly(Double meanAnomaly) {
            this.meanAnomaly = meanAnomaly;
        }

        public Double getOrbitalInclination() {
            return orbitalInclination;
        }

        public void setOrbitalInclination(Double orbitalInclination) {
            this.orbitalInclination = orbitalInclination;
        }

        public Double getOrbitalPeriod() {
            return orbitalPeriod;
        }

        public void setOrbitalPeriod(Double orbitalPeriod) {
            this.orbitalPeriod = orbitalPeriod;
        }

        public Double getPeriapsis() {
            return periapsis;
        }

        public void setPeriapsis(Double periapsis) {
            this.periapsis = periapsis;
        }

        public Double getRadius() {
            return radius;
        }

        public void setRadius(Double radius) {
            this.radius = radius;
        }

        public Double getRotationPeriod() {
            return rotationPeriod;
        }

        public void setRotationPeriod(Double rotationPeriod) {
            this.rotationPeriod = rotationPeriod;
        }

        public Double getSemiMajorAxis() {
            return semiMajorAxis;
        }

        public void setSemiMajorAxis(Double semiMajorAxis) {
            this.semiMajorAxis = semiMajorAxis;
        }

        public Double getSurfaceGravity() {
            return surfaceGravity;
        }

        public void setSurfaceGravity(Double surfaceGravity) {
            this.surfaceGravity = surfaceGravity;
        }

        public Double getSurfacePressure() {
            return surfacePressure;
        }

        public void setSurfacePressure(Double surfacePressure) {
            this.surfacePressure = surfacePressure;
        }

        public Double getSurfaceTemperature() {
            return surfaceTemperature;
        }

        public void setSurfaceTemperature(Double surfaceTemperature) {
            this.surfaceTemperature = surfaceTemperature;
        }

        public Boolean getLandable() {
            return landable;
        }

        public void setLandable(Boolean landable) {
            this.landable = landable;
        }

        public boolean isTidalLock() {
            return tidalLock;
        }

        public void setTidalLock(boolean tidalLock) {
            this.tidalLock = tidalLock;
        }
    }
}