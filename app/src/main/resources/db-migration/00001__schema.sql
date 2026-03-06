-- Force consistent character set and collation for the session
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET COLLATION_CONNECTION = 'utf8mb4_unicode_ci';

-- ----------------------------------------------------------------------------
-- Tables (created or altered with IF NOT EXISTS / safe defaults)
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS market (
    marketId      BIGINT PRIMARY KEY,
    systemAddress BIGINT                                                        NOT NULL,
    timestamp     text                                                          NOT NULL,
    starSystem    VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    stationName   VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    data          MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci   NOT NULL
) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS star_system (
    systemAddress BIGINT PRIMARY KEY,
    starName VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    sector   VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    x        DOUBLE                                                        NOT NULL,
    y        DOUBLE                                                        NOT NULL,
    z        DOUBLE                                                        NOT NULL,
    date     DATETIME                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stellar_object (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    bodyId        BIGINT                                                        NOT NULL DEFAULT 0,
    timestamp     text                                                          NOT NULL,
    starSystem    VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    bodyName VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    systemAddress BIGINT                                                        NOT NULL,
    x             DOUBLE                                                        NOT NULL,
    y             DOUBLE                                                        NOT NULL,
    z        DOUBLE NOT NULL
) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- Indexes (safe to re-run — MariaDB ignores if already exists)
-- ----------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS idx_market_market_id ON market(marketId);
CREATE INDEX IF NOT EXISTS idx_system_starname ON star_system(starName);
CREATE INDEX IF NOT EXISTS idx_system_x_y_z ON star_system(x, y, z);
CREATE INDEX IF NOT EXISTS idx_system_x ON star_system(x);
CREATE INDEX IF NOT EXISTS idx_system_y ON star_system(y);
CREATE INDEX IF NOT EXISTS idx_system_z ON star_system(z);
CREATE UNIQUE INDEX IF NOT EXISTS idx_stellar_object_system_address ON stellar_object(systemAddress);
CREATE INDEX IF NOT EXISTS idx_stellar_primary ON stellar_object(starSystem);
CREATE INDEX IF NOT EXISTS idx_stellar_x ON stellar_object(x);
CREATE INDEX IF NOT EXISTS idx_stellar_y ON stellar_object(y);
CREATE INDEX IF NOT EXISTS idx_stellar_z ON stellar_object(z);
CREATE INDEX IF NOT EXISTS idx_stellar_xyz ON stellar_object(x, y, z);

-- ----------------------------------------------------------------------------
-- Commodity
-- ----------------------------------------------------------------------------
create table if not exists market_commodity (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    timestamp     text    NOT NULL,
    marketId      bigint  not null,
    commodity     text    not null,
    buyPrice      double  not null,
    sellPrice     double  not null,
    stock         integer not null,
    demand        integer not null,
    systemAddress BIGINT  NOT NULL,
    x             DOUBLE  NOT NULL,
    y             DOUBLE  NOT NULL,
    z             DOUBLE  NOT NULL
);

create index if not exists idx_market_commodity_system_address on market_commodity(systemaddress);
create index if not exists idx_market_commodity_x on market_commodity(x);
create index if not exists idx_market_commodity_y on market_commodity(y);
create index if not exists idx_market_commodity_z on market_commodity(z);


-- /// -- /// -- /// -- /// -- /// -- /// -- /// -- /// -- /// -- /// -- /// -- /// -- /// -- /// -- /// -- /// --

create table if not exists materials (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress bigint not null,
    bodyId        bigint not null,
    bodyName      text   not null,
    materialName  text   not null,
    percent       double not null,
    UNIQUE KEY uk_materials_system_body(systemAddress, bodyId)
);
create index if not exists idx_material_system_address on materials(systemAddress);

-- //
create table if not exists rings (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress bigint not null,
    bodyId        bigint not null,
    ringType      text   not null,
    mass          double not null,
    innerRadius   double not null,
    outerRadius   double not null,
    signals       text,
    UNIQUE KEY uk_rings_system_body(systemAddress, bodyId)
);
create index if not exists idx_rings_system_address on rings(systemAddress);

-- //
create table if not exists stations (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress           bigint  not null,
    stationId               bigint  not null,
    realName                text    not null,
    controllingFaction      text,
    controllingFactionState text,
    distanceToArrival       double  not null,
    primaryEconomy          text,
    economies               text,
    government              text,
    services                text,
    stationType text,
    hasLargePad             boolean not null default 0,
    hasMediumPad            boolean not null default 0,
    hasSmallPad             boolean not null default 0,

    UNIQUE KEY uk_stations_system_stationId(systemAddress, stationId)
);
create index if not exists idx_stations_system_address on stations(systemAddress);

-- improvements for speed

-- // stars
ALTER TABLE star_system
ADD COLUMN IF NOT EXISTS pos POINT AFTER z;
UPDATE star_system
SET pos = ST_GeomFromText(CONCAT('POINT(', x, ' ', y, ')'))
WHERE pos IS NULL;
ALTER TABLE star_system
MODIFY COLUMN pos POINT NOT NULL;
ALTER TABLE star_system
ADD SPATIAL INDEX IF NOT EXISTS idx_pos_spatial(pos);

-- // market commodity
ALTER TABLE market_commodity
ADD COLUMN IF NOT EXISTS pos POINT AFTER z;
UPDATE market_commodity
SET pos = ST_GeomFromText(CONCAT('POINT(', x, ' ', y, ')'))
WHERE pos IS NULL;
UPDATE market_commodity
SET pos = ST_GeomFromText(CONCAT('POINT(', x, ' ', y, ')'))
WHERE pos IS NULL AND id >= @start AND id < @start + 500000
LIMIT 500000;
ALTER TABLE market_commodity
MODIFY COLUMN pos POINT NOT NULL;
ALTER TABLE market_commodity
ADD SPATIAL INDEX IF NOT EXISTS idx_mc_pos(pos);


-- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- //

create table if not exists factions (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress bigint not null,
    factionName   text,
    allegiance    text,
    government    text,
    influence     text,
    factionState  text,
    happiness     text
);
create index if not exists idx_factions on factions(systemAddress);

create table if not exists powerplay_state (
    systemAddress                 BIGINT PRIMARY KEY,
    systemAllegiance              text,
    systemEconomy                 text,
    systemSecondEconomy           text,
    systemGovernment              text,
    systemSecurity                text,
    controllingFaction            text,
    powers                        text,
    powerplayState                text,
    controllingPower              text,
    powerplayStateControlProgress double,
    powerplayStateReinforcement   integer,
    powerplayStateUndermining     integer
);
create index if not exists idx_powerplay_state on powerplay_state(systemAddress);


-- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- //

alter table stellar_object
drop column starSystem;
alter table stellar_object
add column atmosphereType text;
alter table stellar_object
add column planetClass text;
alter table stellar_object
add column terraformState text;
alter table stellar_object
add column volcanism text;

alter table stellar_object
add column eccentricity double;
alter table stellar_object
add column distanceFromArrivalLs double;
alter table stellar_object
add column massEm double;
alter table stellar_object
add column meanAnomaly double;
alter table stellar_object
add column orbitalInclination double;
alter table stellar_object
add column orbitalPeriod double;
alter table stellar_object
add column periapsis double;
alter table stellar_object
add column radius double;
alter table stellar_object
add column rotationPeriod double;
alter table stellar_object
add column semiMajorAxis double;
alter table stellar_object
add column surfaceGravity double;
alter table stellar_object
add column surfacePressure double;
alter table stellar_object
add column surfaceTemperature double;

alter table stellar_object
add column landable boolean;
alter table stellar_object
add column tidalLock boolean;


create table if not exists parents (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    systemAddress bigint not null,
    bodyId        bigint not null,
    parentBodyId  bigint not null,
    parentType    varchar(12),
    unique key uk_parents_system_body(systemAddress, bodyId)
);

-- // -- -- // -- -- // -- -- // -- -- // -- -- // -- -- // -- -- // --
