package elite.vault.bootstrap;

import com.google.gson.annotations.SerializedName;
import elite.vault.util.BaseDto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class BootstrapEntryDto extends BaseDto {

    @SerializedName("id64")
    private Long systemAddress;

    @SerializedName("bodyId")
    private Long bodyId;

    @SerializedName("name")
    private String bodyName;

    @SerializedName("type")
    private String bodyType;

    @SerializedName("subType")
    private String subType;

    @SerializedName("distanceToArrival")
    private Double distanceToArrival;

    // Planet-specific
    @SerializedName("isLandable")
    private Boolean isLandable;

    @SerializedName("gravity")
    private Double gravity;

    @SerializedName("earthMasses")
    private Double earthMasses;

    @SerializedName("radius")
    private Double radius;

    @SerializedName("surfaceTemperature")
    private Double surfaceTemperature;

    @SerializedName("surfacePressure")
    private Double surfacePressure;

    @SerializedName("volcanismType")
    private String volcanismType;

    @SerializedName("atmosphereType")
    private String atmosphereType;

    @SerializedName("atmosphereComposition")
    private Map<String, Double> atmosphereComposition;

    @SerializedName("solidComposition")
    private Map<String, Double> solidComposition;

    @SerializedName("terraformingState")
    private String terraformingState;

    @SerializedName("signals")
    private Signals signals;

    @SerializedName("rings")
    private List<Ring> rings;

    @SerializedName("rotationalPeriod")
    private Double rotationalPeriod;

    @SerializedName("rotationalPeriodTidallyLocked")
    private Boolean rotationalPeriodTidallyLocked;

    @SerializedName("axialTilt")
    private Double axialTilt;

    @SerializedName("parents")
    private List<Map<String, Long>> parents;

    @SerializedName("orbitalPeriod")
    private Double orbitalPeriod;

    @SerializedName("semiMajorAxis")
    private Double semiMajorAxis;

    @SerializedName("orbitalEccentricity")
    private Double orbitalEccentricity;

    @SerializedName("orbitalInclination")
    private Double orbitalInclination;

    @SerializedName("argOfPeriapsis")
    private Double argOfPeriapsis;

    @SerializedName("meanAnomaly")
    private Double meanAnomaly;

    @SerializedName("ascendingNode")
    private Double ascendingNode;

    @SerializedName("materials")
    private Map<String, Double> materials;

    @SerializedName("stations")
    private List<Station> stations;

    // Star-specific (often null on planets)
    @SerializedName("absoluteMagnitude")
    private Double absoluteMagnitude;

    @SerializedName("age")
    private Long age;

    @SerializedName("luminosity")
    private String luminosity;

    @SerializedName("solarMasses")
    private Double solarMasses;

    @SerializedName("solarRadius")
    private Double solarRadius;


    // ───────────────────────────────────────────────────────────────
    // Classic getters only – no setters (one-way bootstrap DTO)
    // ───────────────────────────────────────────────────────────────

    public Long getSystemAddress() {
        return systemAddress;
    }

    public Long getBodyId() {
        return bodyId;
    }

    public String getBodyName() {
        return bodyName;
    }

    public String getBodyType() {
        return bodyType;
    }

    public String getSubType() {
        return subType;
    }

    public Double getDistanceToArrival() {
        return distanceToArrival;
    }

    public Boolean getIsLandable() {
        return isLandable;
    }

    public Double getGravity() {
        return gravity;
    }

    public Double getEarthMasses() {
        return earthMasses;
    }

    public Double getRadius() {
        return radius;
    }

    public Double getSurfaceTemperature() {
        return surfaceTemperature;
    }

    public Double getSurfacePressure() {
        return surfacePressure;
    }

    public String getVolcanismType() {
        return volcanismType;
    }

    public String getAtmosphereType() {
        return atmosphereType;
    }

    public Map<String, Double> getAtmosphereComposition() {
        return atmosphereComposition;
    }

    public Map<String, Double> getSolidComposition() {
        return solidComposition;
    }

    public String getTerraformingState() {
        return terraformingState;
    }

    public Signals getSignals() {
        return signals;
    }

    public List<Ring> getRings() {
        return rings;
    }

    public Double getRotationalPeriod() {
        return rotationalPeriod;
    }

    public Boolean getRotationalPeriodTidallyLocked() {
        return rotationalPeriodTidallyLocked;
    }

    public Double getAxialTilt() {
        return axialTilt;
    }

    public List<Map<String, Long>> getParents() {
        return parents;
    }

    public Double getOrbitalPeriod() {
        return orbitalPeriod;
    }

    public Double getSemiMajorAxis() {
        return semiMajorAxis;
    }

    public Double getOrbitalEccentricity() {
        return orbitalEccentricity;
    }

    public Double getOrbitalInclination() {
        return orbitalInclination;
    }

    public Double getArgOfPeriapsis() {
        return argOfPeriapsis;
    }

    public Double getMeanAnomaly() {
        return meanAnomaly;
    }

    public Double getAscendingNode() {
        return ascendingNode;
    }

    public Map<String, Double> getMaterials() {
        return materials;
    }

    public List<Station> getStations() {
        return stations;
    }

    // Star getters
    public Double getAbsoluteMagnitude() {
        return absoluteMagnitude;
    }

    public Long getAge() {
        return age;
    }

    public Boolean getLandable() {
        return isLandable;
    }

    public String getLuminosity() {
        return luminosity;
    }

    public Double getSolarMasses() {
        return solarMasses;
    }

    public Double getSolarRadius() {
        return solarRadius;
    }


    // Nested static classes remain unchanged (also immutable/read-only)

    public static class Signals {
        @SerializedName("signals")
        private Map<String, Integer> signalsMap;

        public Map<String, Integer> getSignalsMap() {
            return signalsMap;
        }
    }

    public static class Ring {
        @SerializedName("name")
        private String name;

        @SerializedName("type")
        private String type;

        @SerializedName("mass")
        private Double mass;

        @SerializedName("innerRadius")
        private Double innerRadius;

        @SerializedName("outerRadius")
        private Double outerRadius;

        @SerializedName("id64")
        private Long id64;

        @SerializedName("signals")
        private RingSignals signals;

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public Double getMass() {
            return mass;
        }

        public Double getInnerRadius() {
            return innerRadius;
        }

        public Double getOuterRadius() {
            return outerRadius;
        }

        public Long getId64() {
            return id64;
        }

        public RingSignals getSignals() {
            return signals == null ? null : signals;
        }
    }

    public static class RingSignals {
        @SerializedName("signals")
        private Map<String, Integer> signalsMap;

        public Map<String, Integer> getSignalsMap() {
            return signalsMap;
        }

        @Override public String toString() {
            if (signalsMap == null) return "";
            Set<String> mats = signalsMap.keySet();
            StringBuilder sb = new StringBuilder();
            mats.forEach(s -> sb.append(s).append(", "));
            return sb.toString();
        }
    }

    public static class Station {
        @SerializedName("name")
        private String name;

        @SerializedName("id")
        private Long id;

        @SerializedName("realName")
        private String realName;

        @SerializedName("controllingFaction")
        private String controllingFaction;

        @SerializedName("controllingFactionState")
        private String controllingFactionState;

        @SerializedName("distanceToArrival")
        private Double distanceToArrival;

        @SerializedName("primaryEconomy")
        private String primaryEconomy;

        @SerializedName("economies")
        private Map<String, Double> economies;

        @SerializedName("government")
        private String government;

        @SerializedName("services")
        private List<String> services;

        @SerializedName("type")
        private String type;

        @SerializedName("landingPads")
        private LandingPads landingPads;

        @SerializedName("market")
        private Market market;

        // getters – only the ones you're likely to use
        public String getName() {
            return name;
        }

        public Long getId() {
            return id;
        }

        public String getRealName() {
            return realName;
        }

        public String getControllingFaction() {
            return controllingFaction;
        }

        public String getControllingFactionState() {
            return controllingFactionState;
        }

        public Double getDistanceToArrival() {
            return distanceToArrival;
        }

        public String getPrimaryEconomy() {
            return primaryEconomy;
        }

        public String getEconomies() {
            if (economies == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            Set<String> set = economies.keySet();
            set.forEach(s -> {
                sb.append(s).append(", ");
            });
            return sb.toString();
        }

        public String getGovernment() {
            return government;
        }

        public String getServices() {
            if (services == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            services.forEach(s -> {
                sb.append(s).append(", ");
            });
            return sb.toString();
        }

        public String getType() {
            return type;
        }

        public LandingPads getLandingPads() {
            return landingPads;
        }

        public Market getMarket() {
            return market;
        }
    }

    public static class LandingPads {
        @SerializedName("large")
        private Integer large;

        @SerializedName("medium")
        private Integer medium;

        @SerializedName("small")
        private Integer small;

        public Integer getLarge() {
            return large;
        }

        public Integer getMedium() {
            return medium;
        }

        public Integer getSmall() {
            return small;
        }
    }

    public static class Market {
        @SerializedName("commodities")
        private List<Commodity> commodities;

        @SerializedName("prohibitedCommodities")
        private List<String> prohibitedCommodities;

        public List<Commodity> getCommodities() {
            return commodities;
        }

        public List<String> getProhibitedCommodities() {
            return prohibitedCommodities;
        }
    }

    public static class Commodity {
        @SerializedName("name")
        private String name;

        @SerializedName("symbol")
        private String symbol;

        @SerializedName("category")
        private String category;

        @SerializedName("commodityId")
        private Long commodityId;

        @SerializedName("demand")
        private Integer demand;

        @SerializedName("supply")
        private Integer supply;

        @SerializedName("buyPrice")
        private Integer buyPrice;

        @SerializedName("sellPrice")
        private Integer sellPrice;

        public String getName() {
            return name;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getCategory() {
            return category;
        }

        public Long getCommodityId() {
            return commodityId;
        }

        public Integer getDemand() {
            return demand;
        }

        public Integer getSupply() {
            return supply;
        }

        public Integer getBuyPrice() {
            return buyPrice;
        }

        public Integer getSellPrice() {
            return sellPrice;
        }
    }
}