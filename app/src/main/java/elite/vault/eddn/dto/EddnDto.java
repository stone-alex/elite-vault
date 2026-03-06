package elite.vault.eddn.dto;

import com.google.gson.annotations.SerializedName;
import elite.vault.util.BaseDto;

import java.util.List;
import java.util.Map;

public class EddnDto extends BaseDto {

    @SerializedName("type")
    private String type;

    @SerializedName("subType")
    private String subType;

    @SerializedName("AscendingNode")
    private Double ascendingNode;

    @SerializedName("Atmosphere")
    private String atmosphere;

    @SerializedName("AtmosphereComposition")
    private List<EDDN_AtmosphereCompositionDto> atmosphereComposition;

    @SerializedName("AtmosphereType")
    private String atmosphereType;

    @SerializedName("AxialTilt")
    private Double axialTilt;

    @SerializedName("BodyID")
    private Long bodyId;

    @SerializedName("BodyName")
    private String bodyName;

    @SerializedName("Composition")
    private EDDN_CompositionDto composition;

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
    private List<EDDN_RingDto> rings;                    // array of rings (very common on gas giants & some stars)

    @SerializedName("Materials")
    private List<EDDN_MaterialDto> materials;

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
    private List<EDDN_SignalDto> signals;                // from FSSBodySignals (bio, geo, etc.)

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

    @SerializedName("Body")
    private String body;                    // "Hyades Sector MS-J b9-1" – full body name (sometimes same as BodyName)

    @SerializedName("BodyType")
    private String bodyType;                // "Star", "Planet", "Barycentre", etc.

    @SerializedName("Population")
    private Long population;                // can be 0 for uninhabited / stars

    @SerializedName("SystemAllegiance")
    private String systemAllegiance;        // "Independent", "Empire", "Federation", "None", etc.

    @SerializedName("SystemEconomy")
    private String systemEconomy;           // localised string reference e.g. "$economy_Industrial;"

    @SerializedName("SystemSecondEconomy")
    private String systemSecondEconomy;     // usually "$economy_None;" or another economy

    @SerializedName("SystemGovernment")
    private String systemGovernment;        // localised e.g. "$government_Dictatorship;"

    @SerializedName("SystemSecurity")
    private String systemSecurity;          // localised e.g. "$SYSTEM_SECURITY_low;"

    @SerializedName("SystemFaction")
    private EDDN_FactionReferenceDto systemFaction;  // minimal reference object { "Name": "..." }

    @SerializedName("Factions")
    private List<EDDN_FactionDto> factions;      // detailed list of factions in the system

    @SerializedName("Powers")
    private List<String> powers;            // e.g. ["A. Lavigny-Duval"]

    @SerializedName("PowerplayState")
    private String powerplayState;          // "Unoccupied", "Contested", etc.

    @SerializedName("PowerplayConflictProgress")
    private List<EDDN_PowerConflictProgressDto> powerplayConflictProgress;

    // Add these fields to ScanDto (or refactor into a StationInfoDto if you prefer composition)

    @SerializedName("DistFromStarLS")
    private Double distFromStarLs;          // Distance from arrival / main star in LS (common in Docked/Location)

    @SerializedName("marketId")
    private Long marketId;                  // Unique ID for the market / station

    @SerializedName("stationName")
    private String stationName;             // "Celsius Prospect", etc.

    @SerializedName("StationType")
    private String stationType;             // "Ocellus", "Coriolis", "Orbis", "Outpost", "FleetCarrier", etc.

    @SerializedName("StationEconomy")
    private String stationEconomy;          // Primary economy, localised ref like "$economy_Terraforming;"

    @SerializedName("StationEconomies")
    private List<EDDN_EconomyDto> stationEconomies;  // Array of {Name, Proportion} – supports multi-economy stations

    @SerializedName("StationGovernment")
    private String stationGovernment;       // "$government_Corporate;", etc.

    @SerializedName("StationFaction")
    private EDDN_FactionReferenceDto stationFaction; // {Name, FactionState?}

    @SerializedName("StationServices")
    private List<String> stationServices;   // Array of strings like "dock", "outfitting", "shipyard", "engineer", etc.

    @SerializedName("LandingPads")
    private EDDN_LandingPadsDto landingPads;     // Nested object with Large/Medium/Small counts
// === Optional / less common but appear in some FSDJump + Scan messages ===

    @SerializedName("Multicrew")
    private Boolean multicrew;

    @SerializedName("Taxi")
    private Boolean taxi;

    @SerializedName("signals")
    private List<EDDN_FssSignalDto> fssSignals;

    @SerializedName("Conflicts")
    private List<EDDN_ConflictDto> conflicts;                // Array of pending/active wars/civil wars/elections

    @SerializedName("ControllingPower")
    private String controllingPower;                    // e.g. "Archon Delaine" – the power currently controlling the system

    @SerializedName("PowerplayStateControlProgress")
    private Double powerplayStateControlProgress;       // e.g. 0.313089 – progress toward control/exploitation

    @SerializedName("PowerplayStateReinforcement")
    private Integer powerplayStateReinforcement;        // e.g. 297 – fortification / reinforcement points

    @SerializedName("PowerplayStateUndermining")
    private Integer powerplayStateUndermining;          // e.g. 2315 – undermining effort against the power

    @SerializedName("systemName")
    private String systemName;

    @SerializedName("CarrierDockingAccess")
    private String carrierDockingAccess;

    @SerializedName("economies")
    private List<EDDN_EconomyDto> economies;

    @SerializedName("prohibited")
    private List<String> prohibited;

    @SerializedName("commodities")
    private List<EDDN_CommodityItemDto> commodities;

    public List<EDDN_FssSignalDto> getFssSignals() {
        return fssSignals;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getCarrierDockingAccess() {
        return carrierDockingAccess;
    }

    public void setCarrierDockingAccess(String carrierDockingAccess) {
        this.carrierDockingAccess = carrierDockingAccess;
    }

    public List<EDDN_EconomyDto> getEconomies() {
        return economies;
    }

    public void setEconomies(List<EDDN_EconomyDto> economies) {
        this.economies = economies;
    }

    public List<String> getProhibited() {
        return prohibited;
    }

    public void setProhibited(List<String> prohibited) {
        this.prohibited = prohibited;
    }

    public List<EDDN_CommodityItemDto> getCommodities() {
        return commodities;
    }

    public void setCommodities(List<EDDN_CommodityItemDto> commodities) {
        this.commodities = commodities;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public Long getPopulation() {
        return population;
    }

    public void setPopulation(Long population) {
        this.population = population;
    }

    public String getSystemAllegiance() {
        return systemAllegiance;
    }

    public void setSystemAllegiance(String systemAllegiance) {
        this.systemAllegiance = systemAllegiance;
    }

    public String getSystemEconomy() {
        return systemEconomy;
    }

    public void setSystemEconomy(String systemEconomy) {
        this.systemEconomy = systemEconomy;
    }

    public String getSystemSecondEconomy() {
        return systemSecondEconomy;
    }

    public void setSystemSecondEconomy(String systemSecondEconomy) {
        this.systemSecondEconomy = systemSecondEconomy;
    }

    public String getSystemGovernment() {
        return systemGovernment;
    }

    public void setSystemGovernment(String systemGovernment) {
        this.systemGovernment = systemGovernment;
    }

    public String getSystemSecurity() {
        return systemSecurity;
    }

    public void setSystemSecurity(String systemSecurity) {
        this.systemSecurity = systemSecurity;
    }

    public EDDN_FactionReferenceDto getSystemFaction() {
        return systemFaction;
    }

    public void setSystemFaction(EDDN_FactionReferenceDto systemFaction) {
        this.systemFaction = systemFaction;
    }

    public List<EDDN_FactionDto> getFactions() {
        return factions;
    }

    public void setFactions(List<EDDN_FactionDto> factions) {
        this.factions = factions;
    }

    public List<String> getPowers() {
        return powers;
    }

    public void setPowers(List<String> powers) {
        this.powers = powers;
    }

    public String getPowerplayState() {
        return powerplayState;
    }

    public void setPowerplayState(String powerplayState) {
        this.powerplayState = powerplayState;
    }

    public List<EDDN_PowerConflictProgressDto> getPowerplayConflictProgress() {
        return powerplayConflictProgress;
    }

    public void setPowerplayConflictProgress(List<EDDN_PowerConflictProgressDto> powerplayConflictProgress) {
        this.powerplayConflictProgress = powerplayConflictProgress;
    }

    public Double getDistFromStarLs() {
        return distFromStarLs;
    }

    public void setDistFromStarLs(Double distFromStarLs) {
        this.distFromStarLs = distFromStarLs;
    }

    public Long getMarketId() {
        return marketId;
    }

    public void setMarketId(Long marketId) {
        this.marketId = marketId;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getStationType() {
        return stationType;
    }

    public void setStationType(String stationType) {
        this.stationType = stationType;
    }

    public String getStationEconomy() {
        return stationEconomy;
    }

    public void setStationEconomy(String stationEconomy) {
        this.stationEconomy = stationEconomy;
    }

    public List<EDDN_EconomyDto> getStationEconomies() {
        return stationEconomies;
    }

    public void setStationEconomies(List<EDDN_EconomyDto> stationEconomies) {
        this.stationEconomies = stationEconomies;
    }

    public String getStationGovernment() {
        return stationGovernment;
    }

    public void setStationGovernment(String stationGovernment) {
        this.stationGovernment = stationGovernment;
    }

    public EDDN_FactionReferenceDto getStationFaction() {
        return stationFaction;
    }

    public void setStationFaction(EDDN_FactionReferenceDto stationFaction) {
        this.stationFaction = stationFaction;
    }

    public List<String> getStationServices() {
        return stationServices;
    }

    public void setStationServices(List<String> stationServices) {
        this.stationServices = stationServices;
    }

    public EDDN_LandingPadsDto getLandingPads() {
        return landingPads;
    }

    public void setLandingPads(EDDN_LandingPadsDto landingPads) {
        this.landingPads = landingPads;
    }

    public Boolean getMulticrew() {
        return multicrew;
    }

    public void setMulticrew(Boolean multicrew) {
        this.multicrew = multicrew;
    }

    public Boolean getTaxi() {
        return taxi;
    }

    public void setTaxi(Boolean taxi) {
        this.taxi = taxi;
    }

    public List<EDDN_ConflictDto> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<EDDN_ConflictDto> conflicts) {
        this.conflicts = conflicts;
    }

    public String getControllingPower() {
        return controllingPower;
    }

    public void setControllingPower(String controllingPower) {
        this.controllingPower = controllingPower;
    }

    public Double getPowerplayStateControlProgress() {
        return powerplayStateControlProgress;
    }

    public void setPowerplayStateControlProgress(Double powerplayStateControlProgress) {
        this.powerplayStateControlProgress = powerplayStateControlProgress;
    }

    public Integer getPowerplayStateReinforcement() {
        return powerplayStateReinforcement;
    }

    public void setPowerplayStateReinforcement(Integer powerplayStateReinforcement) {
        this.powerplayStateReinforcement = powerplayStateReinforcement;
    }

    public Integer getPowerplayStateUndermining() {
        return powerplayStateUndermining;
    }

    public void setPowerplayStateUndermining(Integer powerplayStateUndermining) {
        this.powerplayStateUndermining = powerplayStateUndermining;
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

    public List<EDDN_AtmosphereCompositionDto> getAtmosphereComposition() {
        return atmosphereComposition;
    }

    public void setAtmosphereComposition(List<EDDN_AtmosphereCompositionDto> atmosphereComposition) {
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

    public EDDN_CompositionDto getComposition() {
        return composition;
    }

    public void setComposition(EDDN_CompositionDto composition) {
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

    public List<EDDN_RingDto> getRings() {

        return rings;
    }

    public void setRings(List<EDDN_RingDto> rings) {
        this.rings = rings;
    }

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

    public List<EDDN_SignalDto> getSignals() {
        return signals;
    }

    public void setSignals(List<EDDN_SignalDto> signals) {
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

    public List<EDDN_MaterialDto> getMaterials() {
        return materials;
    }

    public void setMaterials(List<EDDN_MaterialDto> materials) {
        this.materials = materials;
    }
}
