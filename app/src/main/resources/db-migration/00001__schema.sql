-- ============================================================================
-- Elite Vault — SQLite schema (bubble only, ~16K systems, ~24K stations)
--
-- Design notes:
--   - No partitioning — commodity TTL enforced by scheduled DELETE (3h window)
--   - No FK constraints — EDDN delivers events in arbitrary order
--   - BOOLEAN stored as INTEGER (0/1) — SQLite has no native boolean type
--   - JSON stored as TEXT — SQLite 3.38+ json_each() used for queries
--   - Timestamps: received_at / last_updated use unixepoch() (INTEGER)
--                 firstSeen / lastSeen / discovered_at use datetime() (TEXT)
--   - All indexes defined separately (SQLite CREATE TABLE only supports UNIQUE)
-- ============================================================================

-- ============================================================================
-- Commodity type lookup (~300 rows, fully cached in application at startup)
-- ============================================================================

CREATE TABLE IF NOT EXISTS commodity_type (
                                              id   INTEGER PRIMARY KEY AUTOINCREMENT,
                                              name TEXT NOT NULL,
                                              UNIQUE (name)
);

-- ============================================================================
-- Star systems (bubble only — ingest filter rejects anything >500 ly from Sol)
-- ============================================================================

CREATE TABLE IF NOT EXISTS star_system (
                                           systemAddress INTEGER NOT NULL PRIMARY KEY,
                                           starName      TEXT    NOT NULL,
                                           sector        TEXT    NOT NULL DEFAULT '',
                                           x             REAL    NOT NULL,
                                           y             REAL    NOT NULL,
                                           z             REAL    NOT NULL,
                                           discovered_at TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_ss_name ON star_system (starName);
CREATE INDEX IF NOT EXISTS idx_ss_xyz ON star_system (x, y, z);

-- ============================================================================
-- Stations
--
-- x/y/z denormalized from star_system at upsert time — eliminates the join
-- in every hop query. Stations whose system is not yet in star_system are
-- dropped by StationManager and never reach this table.
--
-- economies / services stored as TEXT (JSON arrays).
-- ============================================================================

CREATE TABLE IF NOT EXISTS stations (
                                        id                      INTEGER PRIMARY KEY AUTOINCREMENT,
                                        systemAddress           INTEGER NOT NULL,
                                        marketId                INTEGER NOT NULL,
                                        realName                TEXT    NOT NULL,
                                        controllingFaction      TEXT,
                                        controllingFactionState TEXT,
                                        distanceToArrival       REAL    NOT NULL DEFAULT 0,
                                        primaryEconomy          TEXT,
                                        economies               TEXT,
                                        government              TEXT,
                                        services                TEXT,
                                        stationType             TEXT,
                                        hasLargePad             INTEGER NOT NULL DEFAULT 0,
                                        hasMediumPad            INTEGER NOT NULL DEFAULT 0,
                                        hasSmallPad             INTEGER NOT NULL DEFAULT 0,
                                        x                       REAL    NOT NULL DEFAULT 0,
                                        y                       REAL    NOT NULL DEFAULT 0,
                                        z                       REAL    NOT NULL DEFAULT 0,
                                        received_at             TEXT    NOT NULL DEFAULT (datetime('now')),
                                        UNIQUE (marketId),
                                        UNIQUE (systemAddress, marketId)
);

CREATE INDEX IF NOT EXISTS idx_stations_system ON stations (systemAddress);
CREATE INDEX IF NOT EXISTS idx_stations_name ON stations (realName);
CREATE INDEX IF NOT EXISTS idx_st_route ON stations (marketId, stationType, hasLargePad, hasMediumPad, distanceToArrival);
CREATE INDEX IF NOT EXISTS idx_st_xyz ON stations (x, y, z);

-- ============================================================================
-- Commodity  (no partitioning — rolling 3-hour window via scheduled DELETE)
--
-- PRIMARY KEY is (marketId, commodityId) — one row per commodity per market.
-- Ingest pattern: DELETE WHERE marketId = ? → batch INSERT new snapshot.
-- TTL: DELETE WHERE received_at < unixepoch() - 10800  (runs every 15 min)
-- ============================================================================

CREATE TABLE IF NOT EXISTS commodity (
                                         marketId      INTEGER NOT NULL,
                                         commodityId   INTEGER NOT NULL,
                                         systemAddress INTEGER NOT NULL,
                                         buyPrice      INTEGER NOT NULL DEFAULT 0,
                                         sellPrice     INTEGER NOT NULL DEFAULT 0,
                                         stock         INTEGER NOT NULL DEFAULT 0,
                                         demand        INTEGER NOT NULL DEFAULT 0,
                                         received_at   INTEGER NOT NULL,

                                         PRIMARY KEY (marketId, commodityId)
);

CREATE INDEX IF NOT EXISTS idx_c_buy_route ON commodity (commodityId, buyPrice, stock, systemAddress);
CREATE INDEX IF NOT EXISTS idx_c_sell_route ON commodity (commodityId, sellPrice, demand, systemAddress);
CREATE INDEX IF NOT EXISTS idx_c_market ON commodity (marketId);
CREATE INDEX IF NOT EXISTS idx_c_ttl ON commodity (received_at);

-- ============================================================================
-- Market snapshot deduplication
-- ============================================================================

CREATE TABLE IF NOT EXISTS market_last_seen (
                                                marketId     INTEGER NOT NULL PRIMARY KEY,
                                                last_hash    INTEGER NOT NULL,
                                                last_updated INTEGER NOT NULL
);

-- ============================================================================
-- Factions
-- ============================================================================

CREATE TABLE IF NOT EXISTS factions
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    systemAddress INTEGER NOT NULL,
    factionName   TEXT    NOT NULL,
    allegiance    TEXT,
    government    TEXT,
    influence     REAL,
    factionState  TEXT,
    happiness     TEXT,
    isPirate      INTEGER NOT NULL DEFAULT 0,
    received_at   TEXT    NOT NULL DEFAULT (datetime('now')),
    UNIQUE (systemAddress, factionName)
);

CREATE INDEX IF NOT EXISTS idx_factions_system ON factions (systemAddress);
CREATE INDEX IF NOT EXISTS idx_factions_name ON factions (factionName);
CREATE INDEX IF NOT EXISTS idx_factions_pirate ON factions (isPirate);

-- ============================================================================
-- Powerplay state
-- ============================================================================

CREATE TABLE IF NOT EXISTS powerplay_state
(
    systemAddress       INTEGER PRIMARY KEY,
    systemAllegiance    TEXT,
    systemEconomy       TEXT,
    systemSecondEconomy TEXT,
    systemGovernment    TEXT,
    systemSecurity      TEXT,
    controllingFaction  TEXT,
    powers              TEXT,
    powerplayState      TEXT,
    controllingPower    TEXT,
    controlProgress     REAL,
    reinforcement       INTEGER,
    undermining         INTEGER,
    updated_at          TEXT NOT NULL DEFAULT (datetime('now'))
);

-- ============================================================================
-- FSS signals — RES sites and USS for pirate hunting
-- (stellar_object, parents, materials, rings were removed in Phase 3 — not needed for bubble service)
-- ============================================================================

CREATE TABLE IF NOT EXISTS system_signals
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    systemAddress   INTEGER NOT NULL,
    starSystem      TEXT    NOT NULL,
    signalName      TEXT    NOT NULL,
    signalType      TEXT,
    ussType         TEXT,
    spawningFaction TEXT,
    spawningState   TEXT,
    threatLevel     INTEGER,
    firstSeen       TEXT    NOT NULL DEFAULT (datetime('now')),
    lastSeen        TEXT    NOT NULL DEFAULT (datetime('now')),
    confirmedCount  INTEGER NOT NULL DEFAULT 1,
    UNIQUE (systemAddress, signalName)
);

CREATE INDEX IF NOT EXISTS idx_sig_system ON system_signals (systemAddress);
CREATE INDEX IF NOT EXISTS idx_sig_usstype ON system_signals (ussType);

-- ============================================================================
-- Pirate faction keywords
-- ============================================================================

CREATE TABLE IF NOT EXISTS pirate_faction_keywords
(
    keyword TEXT PRIMARY KEY
);

INSERT OR IGNORE INTO pirate_faction_keywords (keyword)
VALUES
    ('Cartel'), ('Mafia'), ('Syndicate'), ('Camorra'), ('Raiders'), ('Gang'), ('Mob'), ('Clan'), ('Pirates'), ('Brotherhood'), ('Crimson'), ('Purple'), ('Blue'), ('Gold'), ('Silver'), ('Jet'), ('Black'), ('Rats'), ('Posse'), ('Ring'), ('Crew');

-- ============================================================================
-- RES signal grade lookup
-- ============================================================================

CREATE TABLE IF NOT EXISTS res_signal_grades
(
    signalName TEXT PRIMARY KEY,
    grade      TEXT NOT NULL
);

INSERT OR IGNORE INTO res_signal_grades (signalName, grade)
VALUES
    ('$MULTIPLAYER_SCENARIO14_TITLE;', 'Low'), ('$MULTIPLAYER_SCENARIO77_TITLE;', 'Normal'), ('$MULTIPLAYER_SCENARIO78_TITLE;', 'High'), ('$MULTIPLAYER_SCENARIO81_TITLE;', 'Hazardous');
