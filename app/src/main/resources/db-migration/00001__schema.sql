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
    systemAddress BIGINT                                                        NOT NULL,
    x             DOUBLE                                                        NOT NULL,
    y             DOUBLE                                                        NOT NULL,
    z             DOUBLE                                                        NOT NULL,
    data          MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci   NOT NULL
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