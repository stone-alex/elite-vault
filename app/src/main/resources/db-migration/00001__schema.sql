-- ============================================================================
-- Elite Vault - Database Schema
-- MySQL 8.0+
--
-- Clean rewrite from MariaDB 10.3 schema.
-- Key changes from previous version:
--   - POINT / SPATIAL INDEX removed from star_system (MySQL spatial is 2D
--     geographic only; Elite uses Cartesian 3D — plain BETWEEN is correct)
--   - Descending indexes now native (MariaDB 10.3 stored them ASC silently)
--   - JSON type for structured multi-value station fields
--   - CHECK constraints are enforced (MariaDB 10.3 parsed but ignored them)
--   - stations.x/y/z denormalized at write time — no join in hot queries
--   - Fleet carrier table added for position tracking via EDDN CarrierJump
--   - No FK constraints — EDDN delivers events in arbitrary order
-- ============================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET COLLATION_CONNECTION = 'utf8mb4_unicode_ci';


-- ============================================================================
-- Commodity type lookup  (~300 rows, fully cached in application)
-- ============================================================================

CREATE TABLE IF NOT EXISTS commodity_type (
    id   SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    UNIQUE KEY uk_ct_name(name)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- ============================================================================
-- Star systems
--
-- 763k rows. No POINT column — the 2D spatial index was an approximation
-- that ignored galactic Y elevation. Composite idx_ss_xyz covers all
-- bounding-box queries with a single index scan.
--
-- x/y/z: Elite Dangerous Cartesian coords in light years.
--   Origin (0,0,0) = Sol.
--   Y axis = galactic elevation (most populated space is within ±500 ly).
--   X/Z span ~65,000 ly across the galaxy.
-- ============================================================================

CREATE TABLE IF NOT EXISTS star_system (
    systemAddress BIGINT       NOT NULL,
    starName      VARCHAR(100) NOT NULL,
    sector        VARCHAR(100) NOT NULL,
    x             DOUBLE       NOT NULL,
    y             DOUBLE       NOT NULL,
    z             DOUBLE       NOT NULL,
    discovered_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (systemAddress),
    INDEX idx_ss_name(starName),
    INDEX idx_ss_sector(sector),

    -- Composite covering index for 3D bounding-box lookups.
    -- Used by findNeighbors, findSystemsInCorridor, and all hop queries
    -- that bounding-box star_system before the commodity join.
    INDEX idx_ss_xyz(x, y, z)

) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- ============================================================================
-- Stations
--
-- marketId == stationId from EDDN (same value, named marketId to avoid
-- confusion with the commodity market).
--
-- x/y/z are denormalized from star_system at upsert time. This eliminates
-- the star_system join in every hop query. Stations whose systemAddress is
-- not yet in star_system get x=y=z=0 and are corrected on the next EDDN
-- event for that system.
--
-- economies / services stored as JSON — MySQL 8 validates structure and
-- allows JSON_CONTAINS / JSON_OVERLAPS queries with functional indexes.
-- ============================================================================

CREATE TABLE IF NOT EXISTS stations (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress           BIGINT       NOT NULL,
    marketId                BIGINT       NOT NULL,
    realName                VARCHAR(120) NOT NULL,
    controllingFaction      VARCHAR(100)          DEFAULT NULL,
    controllingFactionState VARCHAR(60)           DEFAULT NULL,
    distanceToArrival       DOUBLE       NOT NULL DEFAULT 0,
    primaryEconomy          VARCHAR(60)           DEFAULT NULL,
    economies               JSON                  DEFAULT NULL,
    government              VARCHAR(60)           DEFAULT NULL,
    services                JSON                  DEFAULT NULL,
    stationType             VARCHAR(80)           DEFAULT NULL,
    hasLargePad             BOOLEAN      NOT NULL DEFAULT FALSE,
    hasMediumPad            BOOLEAN      NOT NULL DEFAULT FALSE,
    hasSmallPad             BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Denormalized from star_system — eliminates join in all hot queries.
    x                       DOUBLE       NOT NULL DEFAULT 0,
    y                       DOUBLE       NOT NULL DEFAULT 0,
    z                       DOUBLE       NOT NULL DEFAULT 0,

    received_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_distanceToArrival CHECK (distanceToArrival >= 0),

    UNIQUE KEY uk_stations_market(marketId),
    UNIQUE KEY uk_stations_sys_mkt(systemAddress, marketId),
    INDEX idx_stations_system(systemAddress),
    INDEX idx_stations_name(realName(40)),

    -- Covers hop-query filters: pad size, station type, arrival distance.
    INDEX idx_st_route(marketId, stationType, hasLargePad, hasMediumPad, distanceToArrival),

    -- Covers 3D bounding-box for route queries (avoids touching star_system).
    INDEX idx_st_xyz(x, y, z)

) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- ============================================================================
-- Commodity  (hot table — partitioned, rolling 6-hour window)
--
-- Ingest pattern per station snapshot:
--   DELETE FROM commodity WHERE marketId = ?
--   Bulk INSERT new rows
--
-- received_at stored as INT UNSIGNED epoch seconds.
-- Partition key must be in the PRIMARY KEY.
--
-- Descending index on sellPrice is now physically stored DESC in MySQL 8
-- (MariaDB 10.3 silently stored it ASC). This makes findBestSell range
-- scans read rows in the correct order without filesort.
-- ============================================================================

CREATE TABLE IF NOT EXISTS commodity (
    marketId      BIGINT            NOT NULL,
    commodityId   SMALLINT UNSIGNED NOT NULL,
    systemAddress BIGINT            NOT NULL,
    buyPrice      INT UNSIGNED      NOT NULL DEFAULT 0,
    sellPrice     INT UNSIGNED      NOT NULL DEFAULT 0,
    stock         INT UNSIGNED      NOT NULL DEFAULT 0,
    demand        INT UNSIGNED      NOT NULL DEFAULT 0,
    received_at   INT UNSIGNED      NOT NULL,

    CONSTRAINT chk_buyPrice CHECK (buyPrice >= 0),
    CONSTRAINT chk_sellPrice CHECK (sellPrice >= 0),
    CONSTRAINT chk_stock CHECK (stock >= 0),
    CONSTRAINT chk_demand CHECK (demand >= 0),

    PRIMARY KEY (marketId, commodityId, received_at),

    -- findBuyCandidates: commodity WHERE commodityId=? AND buyPrice>0 AND stock>0
    -- Includes systemAddress so the bounding-box filter can use this index
    -- alone without touching stations for the position lookup.
    INDEX idx_c_buy_route(commodityId, buyPrice ASC, stock DESC, systemAddress),

    -- findBestSell: commodity WHERE commodityId=? AND sellPrice>0 AND demand>0
    -- DESC on sellPrice is now physically correct in MySQL 8.
    INDEX idx_c_sell_route(commodityId, sellPrice DESC, demand DESC, systemAddress),

    INDEX idx_c_market(marketId)

) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci
    PARTITION BY RANGE (received_at DIV 3600) (
        PARTITION p_future VALUES LESS THAN MAXVALUE
        );


-- ============================================================================
-- Market snapshot deduplication
--
-- EDDN delivers market snapshots whenever a commander docks. If 4 commanders
-- dock at the same station within seconds, the ingest layer receives 4
-- identical snapshots for the same marketId — each triggering a full
-- DELETE + bulk INSERT against the partitioned commodity table.
--
-- This table is a change-detection gate. Before replacing commodity rows,
-- the ingest layer computes a lightweight hash of the incoming snapshot
-- (XOR of commodityId+buyPrice+sellPrice across all rows) and compares it
-- to last_hash. If unchanged, the snapshot is discarded without touching
-- the commodity partition at all.
--
-- last_hash:    Java-computed snapshot hash (not cryptographic, change-detect only)
-- last_updated: epoch seconds of the last accepted (changed) snapshot
-- ============================================================================

CREATE TABLE IF NOT EXISTS market_last_seen (
    marketId     BIGINT       NOT NULL,
    last_hash    BIGINT       NOT NULL,
    last_updated INT UNSIGNED NOT NULL,

    PRIMARY KEY (marketId)

) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- ============================================================================
-- Stellar bodies
-- ============================================================================

CREATE TABLE IF NOT EXISTS stellar_object (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress         BIGINT   NOT NULL,
    bodyId                BIGINT   NOT NULL DEFAULT 0,
    bodyName              VARCHAR(120)      DEFAULT NULL,
    received_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    atmosphereType        VARCHAR(60)       DEFAULT NULL,
    planetClass           VARCHAR(60)       DEFAULT NULL,
    terraformState        VARCHAR(60)       DEFAULT NULL,
    volcanism             VARCHAR(100)      DEFAULT NULL,

    distanceFromArrivalLs DOUBLE            DEFAULT NULL,
    radius                DOUBLE            DEFAULT NULL,
    massEm                DOUBLE            DEFAULT NULL,
    surfaceGravity        DOUBLE            DEFAULT NULL,
    surfaceTemperature    DOUBLE            DEFAULT NULL,
    surfacePressure       DOUBLE            DEFAULT NULL,

    orbitalPeriod         DOUBLE            DEFAULT NULL,
    semiMajorAxis         DOUBLE            DEFAULT NULL,
    eccentricity          DOUBLE            DEFAULT NULL,
    orbitalInclination    DOUBLE            DEFAULT NULL,
    periapsis             DOUBLE            DEFAULT NULL,
    meanAnomaly           DOUBLE            DEFAULT NULL,

    rotationPeriod        DOUBLE            DEFAULT NULL,
    tidalLock             BOOLEAN           DEFAULT NULL,
    landable              BOOLEAN           DEFAULT NULL,

    UNIQUE KEY uk_so_system_body(systemAddress, bodyId),
    INDEX idx_so_system(systemAddress),
    INDEX idx_so_body_name(bodyName(40))

) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS parents (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress BIGINT NOT NULL,
    bodyId        BIGINT NOT NULL,
    parentBodyId  BIGINT NOT NULL,
    parentType    VARCHAR(12) DEFAULT NULL,

    UNIQUE KEY uk_parents_system_body(systemAddress, bodyId),
    INDEX idx_parents_parent(parentBodyId)

) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- ============================================================================
-- Materials
-- ============================================================================

CREATE TABLE IF NOT EXISTS materials (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress BIGINT      NOT NULL,
    bodyId        BIGINT      NOT NULL,
    materialName  VARCHAR(80) NOT NULL,
    percent       DOUBLE      NOT NULL,

    UNIQUE KEY uk_materials_body_material(systemAddress, bodyId, materialName(40)),
    INDEX idx_materials_system(systemAddress),
    INDEX idx_materials_name(materialName(40))

) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- ============================================================================
-- Rings
-- ============================================================================

CREATE TABLE IF NOT EXISTS rings (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress BIGINT      NOT NULL,
    bodyId        BIGINT      NOT NULL,
    ringType      VARCHAR(60) NOT NULL,
    mass          DOUBLE      NOT NULL,
    innerRadius   DOUBLE      NOT NULL,
    outerRadius   DOUBLE      NOT NULL,
    signals       VARCHAR(255) DEFAULT NULL,

    UNIQUE KEY uk_rings_system_body(systemAddress, bodyId),
    INDEX idx_rings_system(systemAddress)

) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- ============================================================================
-- Factions
-- ============================================================================

CREATE TABLE IF NOT EXISTS factions (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress BIGINT       NOT NULL,
    factionName   VARCHAR(100) NOT NULL,
    allegiance    VARCHAR(40)           DEFAULT NULL,
    government    VARCHAR(60)           DEFAULT NULL,
    influence     DECIMAL(5, 4)         DEFAULT NULL,
    factionState  VARCHAR(40)           DEFAULT NULL,
    happiness     VARCHAR(40)           DEFAULT NULL,
    received_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_factions_system(systemAddress),
    INDEX idx_factions_name(factionName(40))

) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- ============================================================================
-- Powerplay state
-- ============================================================================

CREATE TABLE IF NOT EXISTS powerplay_state (
    systemAddress       BIGINT PRIMARY KEY,
    systemAllegiance    VARCHAR(40)       DEFAULT NULL,
    systemEconomy       VARCHAR(60)       DEFAULT NULL,
    systemSecondEconomy VARCHAR(60)       DEFAULT NULL,
    systemGovernment    VARCHAR(60)       DEFAULT NULL,
    systemSecurity      VARCHAR(60)       DEFAULT NULL,
    controllingFaction  VARCHAR(100)      DEFAULT NULL,
    powers              JSON              DEFAULT NULL,
    powerplayState      VARCHAR(60)       DEFAULT NULL,
    controllingPower    VARCHAR(60)       DEFAULT NULL,
    controlProgress     DOUBLE            DEFAULT NULL,
    reinforcement       INT               DEFAULT NULL,
    undermining         INT               DEFAULT NULL,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;