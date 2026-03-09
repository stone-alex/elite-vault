-- ============================================================================
-- Elite Vault — Database Schema v3
-- MariaDB 10.3
--
-- Pre-calculated trade pairs.
-- Rebuilt every 3 hours by the calculate_trade_pairs EVENT (00004__procedures.sql).
-- ============================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET COLLATION_CONNECTION = 'utf8mb4_unicode_ci';


-- ============================================================================
-- trade_pair
--
-- One row per (commodity, buy station, sell station) triple where:
--   - both stations are in inhabited systems (have a row in stations)
--   - sellPrice > buyPrice
--   - distance between the two systems <= 250 ly (3D)
--   - both sides have stock / demand > 0
--
-- Fully denormalised — system and station names stored directly so the
-- live route query is a single indexed table scan with no joins.
--
-- Rebuilt atomically via RENAME TABLE swap in the stored procedure so
-- readers never see a partially-populated table.
-- ============================================================================

CREATE TABLE IF NOT EXISTS trade_pair (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    commodityId       SMALLINT UNSIGNED                                             NOT NULL,
    commodityName     VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,

    buyMarketId       BIGINT                                                        NOT NULL,
    buySystemAddress  BIGINT                                                        NOT NULL,
    buySystem         VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    buyStation        VARCHAR(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    buyPrice          INT UNSIGNED                                                  NOT NULL,
    buyStock          INT UNSIGNED                                                  NOT NULL,
    buyX              DOUBLE                                                        NOT NULL,
    buyY              DOUBLE                                                        NOT NULL,
    buyZ              DOUBLE                                                        NOT NULL,
    buyHasLargePad    BOOLEAN                                                       NOT NULL DEFAULT FALSE,
    buyHasMediumPad   BOOLEAN                                                       NOT NULL DEFAULT FALSE,
    buyStationType    VARCHAR(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci           DEFAULT NULL,
    buyDistToArrival  DOUBLE                                                        NOT NULL DEFAULT 0,

    sellMarketId      BIGINT                                                        NOT NULL,
    sellSystemAddress BIGINT                                                        NOT NULL,
    sellSystem        VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    sellStation       VARCHAR(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    sellPrice         INT UNSIGNED                                                  NOT NULL,
    sellDemand        INT UNSIGNED                                                  NOT NULL,
    sellHasLargePad   BOOLEAN                                                       NOT NULL DEFAULT FALSE,
    sellHasMediumPad  BOOLEAN                                                       NOT NULL DEFAULT FALSE,
    sellStationType   VARCHAR(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci           DEFAULT NULL,
    sellDistToArrival DOUBLE                                                        NOT NULL DEFAULT 0,

    profitPerUnit     INT                                                           NOT NULL,
    distanceLy        FLOAT                                                         NOT NULL,

    -- Most important index: route query filters by position + profit
    INDEX idx_tp_buy_sys_profit(buySystemAddress, profitPerUnit DESC),
    INDEX idx_tp_sell_sys_profit(sellSystemAddress, profitPerUnit DESC),

    -- Commodity lookup (for commodity-specific searches)
    INDEX idx_tp_commodity(commodityId, profitPerUnit DESC),

    -- Pad size filters
    INDEX idx_tp_large_pad(buyHasLargePad, sellHasLargePad, profitPerUnit DESC),
    INDEX idx_tp_medium_pad(buyHasMediumPad, sellHasMediumPad, profitPerUnit DESC),

    -- Distance filter
    INDEX idx_tp_distance(distanceLy),

    -- Bounding box for spatial route search
    INDEX idx_tp_buy_xyz(buyX, buyY, buyZ)

) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- ============================================================================
-- trade_pair_meta
--
-- Single-row bookkeeping table. Records the outcome of each recalculation
-- run so the REST API can tell callers how old the data is.
-- ============================================================================

CREATE TABLE IF NOT EXISTS trade_pair_meta (
    id               TINYINT UNSIGNED PRIMARY KEY DEFAULT 1,           -- always row 1
    last_started_at  DATETIME                     DEFAULT NULL,
    last_finished_at DATETIME                     DEFAULT NULL,
    last_duration_ms BIGINT UNSIGNED              DEFAULT NULL,
    last_row_count   INT UNSIGNED                 DEFAULT NULL,
    status           VARCHAR(20)                  DEFAULT 'never_run', -- 'running' | 'ok' | 'error'
    error_message    VARCHAR(500)                 DEFAULT NULL,

    CONSTRAINT chk_single_row CHECK (id = 1)
) ENGINE = InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Seed the single meta row so UPDATEs never have to INSERT
INSERT IGNORE INTO trade_pair_meta (id)
VALUES (1);