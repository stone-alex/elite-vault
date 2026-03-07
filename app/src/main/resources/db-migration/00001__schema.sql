-- ============================================================================
-- Elite Vault — Database Schema v1
-- MariaDB 10.3
-- Tables only. Procedure and event are in 00002__procedures.sql
-- ============================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET COLLATION_CONNECTION = 'utf8mb4_unicode_ci';


-- ============================================================================
-- Commodity type lookup  (~300 rows, fully cached in application)
-- ============================================================================

CREATE TABLE IF NOT EXISTS commodity_type (
    id   SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    UNIQUE KEY uk_ct_name(name)
) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- ============================================================================
-- Core reference tables
-- ============================================================================

CREATE TABLE IF NOT EXISTS star_system (
    systemAddress BIGINT PRIMARY KEY,
    starName      VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    sector        VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    x             DOUBLE                                                        NOT NULL,
    y             DOUBLE                                                        NOT NULL,
    z             DOUBLE                                                        NOT NULL,
    discovered_at DATETIME                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 2D point for fast bounding-box / MBR-based spatial queries in galactic plane
    pos           POINT                                                         NOT NULL,
    SPATIAL INDEX idx_ss_pos(pos),

    INDEX idx_ss_name(starName),
    INDEX idx_ss_sector(sector),
    INDEX idx_ss_x(x),
    INDEX idx_ss_y(y),
    INDEX idx_ss_z(z)
) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- ============================================================================
-- Stations  (marketId = stationId from EDDN — same value, clearer name)
-- ============================================================================

CREATE TABLE IF NOT EXISTS stations (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress           BIGINT                                                        NOT NULL,
    marketId                BIGINT NOT NULL,
    realName                VARCHAR(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    controllingFaction      VARCHAR(100) DEFAULT NULL,
    controllingFactionState VARCHAR(60)  DEFAULT NULL,
    distanceToArrival       DOUBLE                                                        NOT NULL DEFAULT 0,
    primaryEconomy          VARCHAR(60)  DEFAULT NULL,
    economies               TEXT         DEFAULT NULL,
    government              VARCHAR(60)  DEFAULT NULL,
    services                TEXT         DEFAULT NULL,
    stationType             VARCHAR(80)  DEFAULT NULL,
    hasLargePad             BOOLEAN                                                       NOT NULL DEFAULT FALSE,
    hasMediumPad            BOOLEAN                                                       NOT NULL DEFAULT FALSE,
    hasSmallPad             BOOLEAN                                                       NOT NULL DEFAULT FALSE,
    received_at             DATETIME                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_stations_market(marketId),
    UNIQUE KEY uk_stations_sys_mkt(systemAddress, marketId),
    INDEX idx_stations_system(systemAddress),
    INDEX idx_stations_name(realName(40))
) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- ============================================================================
-- Commodity  (hot table — partitioned, pruned to 3-hour window)
--
-- Ingest pattern: full snapshot replace per station
--   1. DELETE FROM commodity WHERE marketId = ?
--   2. Bulk INSERT new rows
--
-- Partition key (received_at) must be part of the PRIMARY KEY in MariaDB 10.3.
-- DATETIME rejected as partition expression (timezone-dependent) — stored as
-- INT UNSIGNED epoch seconds instead. Pure integer arithmetic is deterministic.
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

    PRIMARY KEY (marketId, commodityId, received_at),

    INDEX idx_c_sell(commodityId, sellPrice DESC, stock DESC, systemAddress),
    INDEX idx_c_buy(commodityId, buyPrice ASC, demand DESC, systemAddress),
    INDEX idx_c_market(marketId)

) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
    PARTITION BY RANGE (received_at DIV 3600) (
        PARTITION p_future VALUES LESS THAN MAXVALUE
        );


-- ============================================================================
-- Stellar bodies
-- ============================================================================

CREATE TABLE IF NOT EXISTS stellar_object (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress         BIGINT   NOT NULL,
    bodyId                BIGINT   NOT NULL DEFAULT 0,
    bodyName              VARCHAR(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
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
) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS parents (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress BIGINT NOT NULL,
    bodyId        BIGINT NOT NULL,
    parentBodyId  BIGINT NOT NULL,
    parentType    VARCHAR(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,

    UNIQUE KEY uk_parents_system_body(systemAddress, bodyId),
    INDEX idx_parents_parent(parentBodyId)
) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- ============================================================================
-- Materials
-- ============================================================================

CREATE TABLE IF NOT EXISTS materials (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress BIGINT                                                       NOT NULL,
    bodyId        BIGINT                                                       NOT NULL,
    materialName  VARCHAR(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    percent       DOUBLE                                                       NOT NULL,

    UNIQUE KEY uk_materials_body_material(systemAddress, bodyId, materialName(40)),
    INDEX idx_materials_system(systemAddress),
    INDEX idx_materials_name(materialName(40))
) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- ============================================================================
-- Rings
-- ============================================================================

CREATE TABLE IF NOT EXISTS rings (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress BIGINT                                                       NOT NULL,
    bodyId        BIGINT                                                       NOT NULL,
    ringType      VARCHAR(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    mass          DOUBLE                                                       NOT NULL,
    innerRadius   DOUBLE                                                       NOT NULL,
    outerRadius   DOUBLE                                                       NOT NULL,
    signals       VARCHAR(255) DEFAULT NULL,

    UNIQUE KEY uk_rings_system_body(systemAddress, bodyId),
    INDEX idx_rings_system(systemAddress)
) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- ============================================================================
-- Factions
-- ============================================================================

CREATE TABLE IF NOT EXISTS factions (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress BIGINT                                                        NOT NULL,
    factionName   VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    allegiance   VARCHAR(40)       DEFAULT NULL,
    government   VARCHAR(60)       DEFAULT NULL,
    influence    DECIMAL(5, 4)     DEFAULT NULL,
    factionState VARCHAR(40)       DEFAULT NULL,
    happiness    VARCHAR(40)       DEFAULT NULL,
    received_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_factions_system(systemAddress),
    INDEX idx_factions_name(factionName(40))
) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


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
    powers              TEXT              DEFAULT NULL,
    powerplayState      VARCHAR(60)       DEFAULT NULL,
    controllingPower    VARCHAR(60)       DEFAULT NULL,
    controlProgress     DOUBLE            DEFAULT NULL,
    reinforcement       INT               DEFAULT NULL,
    undermining         INT               DEFAULT NULL,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;