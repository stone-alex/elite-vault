-- ============================================================================
-- Elite Vault — Database Schema
-- MariaDB 10.3
-- ============================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET COLLATION_CONNECTION = 'utf8mb4_unicode_ci';


-- ============================================================================
-- Commodity type lookup  (~300 rows, fully cached in application)
-- ============================================================================

CREATE TABLE commodity_type (
    id   SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    UNIQUE KEY uk_ct_name(name)
) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- ============================================================================
-- Core reference tables
-- ============================================================================

CREATE TABLE star_system (
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

CREATE TABLE stations (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress           BIGINT                                                        NOT NULL,
    marketId                BIGINT                                                        NOT NULL, -- same value as EDDN stationId
    realName                VARCHAR(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    controllingFaction      VARCHAR(100)                                                           DEFAULT NULL,
    controllingFactionState VARCHAR(60)                                                            DEFAULT NULL,
    distanceToArrival       DOUBLE                                                        NOT NULL DEFAULT 0,
    primaryEconomy          VARCHAR(60)                                                            DEFAULT NULL,
    economies               TEXT                                                                   DEFAULT NULL,
    government              VARCHAR(60)                                                            DEFAULT NULL,
    services                TEXT                                                                   DEFAULT NULL,
    stationType             VARCHAR(80)                                                            DEFAULT NULL,
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
-- This is fine because step 1 always clears old rows before step 2 inserts.
--
-- Pruning: DROP PARTITION (instant metadata op) via maintenance procedure.
-- ============================================================================

CREATE TABLE commodity (
    marketId      BIGINT            NOT NULL,
    commodityId   SMALLINT UNSIGNED NOT NULL, -- FK-like ref to commodity_type.id
    systemAddress BIGINT            NOT NULL, -- denormalized for trade queries, avoids join to stations
    buyPrice      INT UNSIGNED      NOT NULL DEFAULT 0,
    sellPrice     INT UNSIGNED      NOT NULL DEFAULT 0,
    stock         INT UNSIGNED      NOT NULL DEFAULT 0,
    demand        INT UNSIGNED      NOT NULL DEFAULT 0,
    -- Epoch seconds — set explicitly by DAO (UNIX_TIMESTAMP() or System.currentTimeMillis()/1000).
    -- INT UNSIGNED required: DATETIME is timezone-dependent and rejected by MariaDB 10.3
    -- as a partition expression. Pure integer arithmetic on epoch is fully deterministic.
    received_at   INT UNSIGNED      NOT NULL,

    -- Partition key must be in PK (MariaDB 10.3 requirement)
    PRIMARY KEY (marketId, commodityId, received_at),

    -- Trade route: best place to SELL (you have the commodity, want max credits)
    INDEX idx_c_sell(commodityId, sellPrice DESC, stock DESC, systemAddress),
    -- Trade route: best place to BUY (you want to acquire cheaply)
    INDEX idx_c_buy(commodityId, buyPrice ASC, demand DESC, systemAddress),
    -- Ingest DELETE sweep: all rows for a station
    INDEX idx_c_market(marketId)

) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
    PARTITION BY RANGE (received_at DIV 3600) (
        PARTITION p_future VALUES LESS THAN MAXVALUE
        );


-- ============================================================================
-- Stellar bodies
-- ============================================================================

CREATE TABLE stellar_object (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress         BIGINT   NOT NULL,
    bodyId                BIGINT   NOT NULL                                             DEFAULT 0,
    bodyName              VARCHAR(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    received_at           DATETIME NOT NULL                                             DEFAULT CURRENT_TIMESTAMP,

    atmosphereType        VARCHAR(60)                                                   DEFAULT NULL,
    planetClass           VARCHAR(60)                                                   DEFAULT NULL,
    terraformState        VARCHAR(60)                                                   DEFAULT NULL,
    volcanism             VARCHAR(100)                                                  DEFAULT NULL,

    distanceFromArrivalLs DOUBLE                                                        DEFAULT NULL,
    radius                DOUBLE                                                        DEFAULT NULL,
    massEm                DOUBLE                                                        DEFAULT NULL,
    surfaceGravity        DOUBLE                                                        DEFAULT NULL,
    surfaceTemperature    DOUBLE                                                        DEFAULT NULL,
    surfacePressure       DOUBLE                                                        DEFAULT NULL,

    orbitalPeriod         DOUBLE                                                        DEFAULT NULL,
    semiMajorAxis         DOUBLE                                                        DEFAULT NULL,
    eccentricity          DOUBLE                                                        DEFAULT NULL,
    orbitalInclination    DOUBLE                                                        DEFAULT NULL,
    periapsis             DOUBLE                                                        DEFAULT NULL,
    meanAnomaly           DOUBLE                                                        DEFAULT NULL,

    rotationPeriod        DOUBLE                                                        DEFAULT NULL,
    tidalLock             BOOLEAN                                                       DEFAULT NULL,
    landable              BOOLEAN                                                       DEFAULT NULL,

    UNIQUE KEY uk_so_system_body(systemAddress, bodyId),
    INDEX idx_so_system(systemAddress),
    INDEX idx_so_body_name(bodyName(40))
) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


CREATE TABLE parents (
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

CREATE TABLE materials (
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

CREATE TABLE rings (
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

CREATE TABLE factions (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress BIGINT                                                        NOT NULL,
    factionName   VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    allegiance    VARCHAR(40)                                                            DEFAULT NULL,
    government    VARCHAR(60)                                                            DEFAULT NULL,
    influence     DECIMAL(5, 4)                                                          DEFAULT NULL,
    factionState  VARCHAR(40)                                                            DEFAULT NULL,
    happiness     VARCHAR(40)                                                            DEFAULT NULL,
    received_at   DATETIME                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_factions_system(systemAddress),
    INDEX idx_factions_name(factionName(40))
) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- ============================================================================
-- Powerplay state
-- ============================================================================

CREATE TABLE powerplay_state (
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


-- ============================================================================
-- Partition maintenance procedure
--
-- Call at startup and every 30–60 minutes via event.
-- p_hours_ahead : how many future hourly partitions to pre-create (e.g. 72)
-- p_hours_to_keep: safety backstop — drop partitions older than this (e.g. 6)
--                  The 3-hour prune is effectively enforced by the ingest pattern
--                  (DELETE + INSERT), so this is a stale-data safety net.
-- ============================================================================

DELIMITER $$

CREATE PROCEDURE maintain_commodity_partitions(
    IN p_hours_ahead   INT, -- e.g. 72
    IN p_hours_to_keep INT -- e.g. 6
)
BEGIN
    DECLARE v_schema VARCHAR(64) DEFAULT DATABASE();
    DECLARE v_table VARCHAR(64) DEFAULT 'commodity';
    DECLARE v_part_name VARCHAR(64);
    DECLARE v_boundary BIGINT;
    DECLARE v_sql TEXT;
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_hour BIGINT;
    DECLARE v_end_hour BIGINT;

    -- -------------------------------------------------------------------------
    -- 1. Pre-create future partitions in a single REORGANIZE statement
    --    Builds the full partition list from now → now + p_hours_ahead,
    --    then reorganizes p_future in one shot (single metadata lock).
    -- -------------------------------------------------------------------------
    SET v_hour = UNIX_TIMESTAMP(NOW()) DIV 3600;
    SET v_end_hour = v_hour + p_hours_ahead;
    SET v_sql = CONCAT('ALTER TABLE ', v_table, ' REORGANIZE PARTITION p_future INTO (');

    WHILE v_hour <= v_end_hour
        DO
            SET v_part_name = CONCAT('p_h_', FROM_UNIXTIME(v_hour * 3600, '%Y%m%d_%H'));
            SET v_boundary = v_hour + 1;

            -- Only include partitions that don't already exist
            IF NOT EXISTS (SELECT 1
                           FROM INFORMATION_SCHEMA.PARTITIONS
                           WHERE TABLE_SCHEMA = v_schema AND TABLE_NAME = v_table AND PARTITION_NAME = v_part_name)
            THEN
                SET v_sql = CONCAT(v_sql,
                                   'PARTITION ', v_part_name,
                                   ' VALUES LESS THAN (', v_boundary, '),');
            END IF;

            SET v_hour = v_hour + 1;
        END WHILE;

    SET v_sql = CONCAT(v_sql, 'PARTITION p_future VALUES LESS THAN MAXVALUE)');

    PREPARE stmt FROM v_sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    -- -------------------------------------------------------------------------
    -- 2. Drop partitions older than p_hours_to_keep
    --    DROP PARTITION is an instant metadata operation — no row scanning.
    -- -------------------------------------------------------------------------
    BEGIN
        DECLARE cur_old CURSOR FOR
            SELECT PARTITION_NAME
            FROM INFORMATION_SCHEMA.PARTITIONS
            WHERE TABLE_SCHEMA = v_schema AND TABLE_NAME = v_table AND PARTITION_NAME LIKE 'p_h_%' AND
                CAST(PARTITION_DESCRIPTION AS SIGNED) <
                (UNIX_TIMESTAMP(NOW() - INTERVAL p_hours_to_keep HOUR) DIV 3600);

        DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

        OPEN cur_old;
        drop_loop:
        LOOP
            FETCH cur_old INTO v_part_name;
            IF v_done THEN LEAVE drop_loop; END IF;

            SET v_sql = CONCAT('ALTER TABLE ', v_table, ' DROP PARTITION ', v_part_name);
            PREPARE stmt FROM v_sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END LOOP drop_loop;
        CLOSE cur_old;
    END;

END$$

DELIMITER ;


-- ============================================================================
-- Scheduled event: run partition maintenance every hour
-- ============================================================================

CREATE EVENT maintain_commodity_partitions_hourly
    ON SCHEDULE EVERY 1 HOUR
        STARTS CURRENT_TIMESTAMP + INTERVAL 5 MINUTE
    DO
    CALL maintain_commodity_partitions(72, 6);