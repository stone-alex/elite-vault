package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;
import elite.vault.util.BaseDto;

import java.util.List;
import java.util.Map;

public class ScanDto extends BaseDto {

    @SerializedName("type")
    private String type;

    @SerializedName("subType")
    private String subType;



    @SerializedName("AscendingNode")
    private Double ascendingNode;

    @SerializedName("Atmosphere")
    private String atmosphere;

    @SerializedName("AtmosphereComposition")
    private List<AtmosphereCompositionDto> atmosphereComposition;

    @SerializedName("AtmosphereType")
    private String atmosphereType;

    @SerializedName("AxialTilt")
    private Double axialTilt;

    @SerializedName("BodyID")
    private Long bodyId;

    @SerializedName("BodyName")
    private String bodyName;

    @SerializedName("Composition")
    private CompositionDto composition;

    @SerializedName("DistanceFromArrivalLS")
    private Double distanceFromArrivalLs;

    @SerializedName("Eccentricity")
    private Double eccentricity;

    @SerializedName("Landable")
    private Boolean landable;

    @SerializedName("MassEM")
    private Double massEm;

    @SerializedName("MeanAnomaly")
    private Double meanAnomaly;

    @SerializedName("OrbitalInclination")
    private Double orbitalInclination;

    @SerializedName("OrbitalPeriod")
    private Double orbitalPeriod;

    @SerializedName("Parents")
    private List<Map<String, Integer>> parents;

    @SerializedName("Periapsis")
    private Double periapsis;

    @SerializedName("PlanetClass")
    private String planetClass;

    @SerializedName("Radius")
    private Double radius;

    @SerializedName("RotationPeriod")
    private Double rotationPeriod;

    @SerializedName("ScanType")
    private String scanType;

    @SerializedName("SemiMajorAxis")
    private Double semiMajorAxis;

    @SerializedName("StarPos")
    private List<Double> starPos;

    @SerializedName("StarSystem")
    private String starSystem;

    @SerializedName("SurfaceGravity")
    private Double surfaceGravity;

    @SerializedName("SurfacePressure")
    private Double surfacePressure;

    @SerializedName("SurfaceTemperature")
    private Double surfaceTemperature;

    @SerializedName("SystemAddress")
    private Long systemAddress;

    @SerializedName("TerraformState")
    private String terraformState;

    @SerializedName("TidalLock")
    private Boolean tidalLock;

    @SerializedName("Volcanism")
    private String volcanism;

    @SerializedName("WasDiscovered")
    private Boolean wasDiscovered;

    @SerializedName("WasFootfalled")
    private Boolean wasFootfalled;

    @SerializedName("WasMapped")
    private Boolean wasMapped;

    @SerializedName("Rings")
    private List<RingDto> rings;                    // array of rings (very common on gas giants & some stars)

    @SerializedName("Materials")
    private List<Material> materials;

    @SerializedName("StarType")
    private String starType;                        // "O", "M", "Black Hole", "Neutron", etc.

    @SerializedName("Luminosity")
    private String luminosity;                      // "V", "VII", "Vz", etc.

    @SerializedName("SpectralClass")
    private String spectralClass;                   // "O0", "T7", "Y2", etc.

    @SerializedName("Age_MY")
    private Long ageMy;                             // star age in millions of years

    @SerializedName("ReserveLevel")
    private String reserveLevel;                    // "Pristine", "Depleted", "Common", etc. (for rings)

    // === Nice-to-have (FSS signals & modern extras) ===
    @SerializedName("Signals")
    private List<SignalDto> signals;                // from FSSBodySignals (bio, geo, etc.)

    @SerializedName("Genus")
    private List<String> genus;                     // Odyssey biology genus

    @SerializedName("SolidComposition")
    private Map<String, Double> solidComposition;   // for rocky worlds

    @SerializedName("IsLandable")
    private Boolean isLandable;                     // some tools send this instead of Landable

    // === Optional but harmless (Spanhs compatibility) ===
    @SerializedName("mainStar")
    private Boolean mainStar;

    @SerializedName("updateTime")
    private String updateTime;

    public static class RingDto {
        @SerializedName("Name") private String name;
        @SerializedName("RingClass") private String ringClass;   // "Metal Rich", "Icy", etc.
        @SerializedName("MassMT") private Double massMt;
        @SerializedName("InnerRad") private Double innerRad;
        @SerializedName("OuterRad") private Double outerRad;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRingClass() {
            return ringClass;
        }

        public void setRingClass(String ringClass) {
            this.ringClass = ringClass;
        }

        public Double getMassMt() {
            return massMt;
        }

        public void setMassMt(Double massMt) {
            this.massMt = massMt;
        }

        public Double getInnerRad() {
            return innerRad;
        }

        public void setInnerRad(Double innerRad) {
            this.innerRad = innerRad;
        }

        public Double getOuterRad() {
            return outerRad;
        }

        public void setOuterRad(Double outerRad) {
            this.outerRad = outerRad;
        }
    }

    public static class Material {
        @SerializedName("Name")
        private String name;

        @SerializedName("Percent")
        private Double percent;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Double getPercent() {
            return percent;
        }

        public void setPercent(Double percent) {
            this.percent = percent;
        }
    }

    public static class SignalDto {
        @SerializedName("Type") private String type;   // "Biological", "Geological", etc.
        @SerializedName("Count") private Integer count;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }

    public Double getAscendingNode() {
        return ascendingNode;
    }

    public void setAscendingNode(Double ascendingNode) {
        this.ascendingNode = ascendingNode;
    }

    public String getAtmosphere() {
        return atmosphere;
    }

    public void setAtmosphere(String atmosphere) {
        this.atmosphere = atmosphere;
    }

    public List<AtmosphereCompositionDto> getAtmosphereComposition() {
        return atmosphereComposition;
    }

    public void setAtmosphereComposition(List<AtmosphereCompositionDto> atmosphereComposition) {
        this.atmosphereComposition = atmosphereComposition;
    }

    public String getAtmosphereType() {
        return atmosphereType;
    }

    public void setAtmosphereType(String atmosphereType) {
        this.atmosphereType = atmosphereType;
    }

    public Double getAxialTilt() {
        return axialTilt;
    }

    public void setAxialTilt(Double axialTilt) {
        this.axialTilt = axialTilt;
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

    public CompositionDto getComposition() {
        return composition;
    }

    public void setComposition(CompositionDto composition) {
        this.composition = composition;
    }

    public Double getDistanceFromArrivalLs() {
        return distanceFromArrivalLs;
    }

    public void setDistanceFromArrivalLs(Double distanceFromArrivalLs) {
        this.distanceFromArrivalLs = distanceFromArrivalLs;
    }

    public Double getEccentricity() {
        return eccentricity;
    }

    public void setEccentricity(Double eccentricity) {
        this.eccentricity = eccentricity;
    }

    public Boolean getLandable() {
        return landable;
    }

    public void setLandable(Boolean landable) {
        this.landable = landable;
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

    public List<Map<String, Integer>> getParents() {
        return parents;
    }

    public void setParents(List<Map<String, Integer>> parents) {
        this.parents = parents;
    }

    public Double getPeriapsis() {
        return periapsis;
    }

    public void setPeriapsis(Double periapsis) {
        this.periapsis = periapsis;
    }

    public String getPlanetClass() {
        return planetClass;
    }

    public void setPlanetClass(String planetClass) {
        this.planetClass = planetClass;
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

    public String getScanType() {
        return scanType;
    }

    public void setScanType(String scanType) {
        this.scanType = scanType;
    }

    public Double getSemiMajorAxis() {
        return semiMajorAxis;
    }

    public void setSemiMajorAxis(Double semiMajorAxis) {
        this.semiMajorAxis = semiMajorAxis;
    }

    public List<Double> getStarPos() {
        return starPos;
    }

    public void setStarPos(List<Double> starPos) {
        this.starPos = starPos;
    }

    public String getStarSystem() {
        return starSystem;
    }

    public void setStarSystem(String starSystem) {
        this.starSystem = starSystem;
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

    public Long getSystemAddress() {
        return systemAddress;
    }

    public void setSystemAddress(Long systemAddress) {
        this.systemAddress = systemAddress;
    }

    public String getTerraformState() {
        return terraformState;
    }

    public void setTerraformState(String terraformState) {
        this.terraformState = terraformState;
    }

    public Boolean getTidalLock() {
        return tidalLock;
    }

    public void setTidalLock(Boolean tidalLock) {
        this.tidalLock = tidalLock;
    }

    public String getVolcanism() {
        return volcanism;
    }

    public void setVolcanism(String volcanism) {
        this.volcanism = volcanism;
    }

    public Boolean getWasDiscovered() {
        return wasDiscovered;
    }

    public void setWasDiscovered(Boolean wasDiscovered) {
        this.wasDiscovered = wasDiscovered;
    }

    public Boolean getWasFootfalled() {
        return wasFootfalled;
    }

    public void setWasFootfalled(Boolean wasFootfalled) {
        this.wasFootfalled = wasFootfalled;
    }

    public Boolean getWasMapped() {
        return wasMapped;
    }

    public void setWasMapped(Boolean wasMapped) {
        this.wasMapped = wasMapped;
    }

    public List<RingDto> getRings() {

        return rings;
    }

    public void setRings(List<RingDto> rings) {
        this.rings = rings;
    }

//    public Map<String, Double> getMaterials() {
//        return materials;
//    }
//
//    public void setMaterials(Map<String, Double> materials) {
//        this.materials = materials;
//    }

    public String getStarType() {
        return starType;
    }

    public void setStarType(String starType) {
        this.starType = starType;
    }

    public String getLuminosity() {
        return luminosity;
    }

    public void setLuminosity(String luminosity) {
        this.luminosity = luminosity;
    }

    public String getSpectralClass() {
        return spectralClass;
    }

    public void setSpectralClass(String spectralClass) {
        this.spectralClass = spectralClass;
    }

    public Long getAgeMy() {
        return ageMy;
    }

    public void setAgeMy(Long ageMy) {
        this.ageMy = ageMy;
    }

    public String getReserveLevel() {
        return reserveLevel;
    }

    public void setReserveLevel(String reserveLevel) {
        this.reserveLevel = reserveLevel;
    }

    public List<SignalDto> getSignals() {
        return signals;
    }

    public void setSignals(List<SignalDto> signals) {
        this.signals = signals;
    }

    public List<String> getGenus() {
        return genus;
    }

    public void setGenus(List<String> genus) {
        this.genus = genus;
    }

    public Map<String, Double> getSolidComposition() {
        return solidComposition;
    }

    public void setSolidComposition(Map<String, Double> solidComposition) {
        this.solidComposition = solidComposition;
    }

    public Boolean getMainStar() {
        return mainStar;
    }

    public void setMainStar(Boolean mainStar) {
        this.mainStar = mainStar;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public List<Material> getMaterials() {
        return materials;
    }

    public void setMaterials(List<Material> materials) {
        this.materials = materials;
    }
}