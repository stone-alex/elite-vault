-- ============================================================================
-- Elite Vault — Database Schema v6
-- MariaDB 10.3
--
-- Replaces 00003 / 00004 / 00005 / 00006 / 00007 / 00008.
-- All trade_pair infrastructure is removed.
-- Trade routes are calculated entirely in Java (MarketManager) using two
-- lightweight indexed queries against the commodity table per hop.
--
-- No pre-calculation. No background procedure. No event scheduler.
-- No dirty flag. No last_pair_calc.
--
-- Query pattern per hop:
--   1. findBuyCandidates  — cheap buy stations within hopDistance of current pos
--   2. findBestSell       — best sell station within hopDistance of buy station
--   Java picks the best (buy, sell) pair and advances position to sell coords.
-- ============================================================================

SET NAMES utf8mb4;

/*SPLIT*/

SET CHARACTER SET utf8mb4;

/*SPLIT*/

SET COLLATION_CONNECTION = 'utf8mb4_unicode_ci';

/*SPLIT*/

-- ============================================================================
-- Add indexes to commodity that make the two hop queries fast.
--
-- idx_c_buy_route:  covers findBuyCandidates
--   Filter:  commodityId, buyPrice > 0, stock > 0
--   Join:    systemAddress → star_system for bounding box
--
-- idx_c_sell_route: covers findBestSell
--   Filter:  commodityId, sellPrice DESC, demand > 0
--   Join:    systemAddress → star_system for bounding box
--
-- Both queries already have idx_c_market and idx_c_buy / idx_c_sell from
-- 00001. The composite indexes below add systemAddress to avoid a separate
-- lookup for the star_system join in the common case.
-- ============================================================================

ALTER TABLE commodity
ADD INDEX IF NOT EXISTS idx_c_buy_route(commodityId, buyPrice, stock, systemAddress),
ADD INDEX IF NOT EXISTS idx_c_sell_route(commodityId, sellPrice, demand, systemAddress);

/*SPLIT*/

-- ============================================================================
-- star_system: add composite index on (x, y, z) for bounding box lookups.
-- The existing single-column indexes (idx_ss_x, idx_ss_y, idx_ss_z) require
-- MariaDB to intersect three index scans. A composite index lets the planner
-- satisfy the three BETWEEN clauses in one scan.
-- ============================================================================

ALTER TABLE star_system
ADD INDEX IF NOT EXISTS idx_ss_xyz(x, y, z);

/*SPLIT*/

-- ============================================================================
-- stations: add index on (marketId, stationType, hasLargePad, hasMediumPad,
-- distanceToArrival) to cover the pad-size and arrival-distance filters in
-- both hop queries without a full stations scan.
-- ============================================================================

ALTER TABLE stations
ADD INDEX IF NOT EXISTS idx_st_route(marketId, stationType, hasLargePad, hasMediumPad, distanceToArrival);


-- Add coordinate columns. Default 0 — backfilled below.
ALTER TABLE stations
ADD COLUMN IF NOT EXISTS x DOUBLE NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS y DOUBLE NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS z DOUBLE NOT NULL DEFAULT 0;

/*SPLIT*/

-- Backfill coordinates from star_system for all existing stations.
-- Stations whose systemAddress is not yet in star_system get x=y=z=0
-- and will be updated the next time their system is seen via EDDN.
UPDATE stations st
    INNER JOIN star_system ss ON ss.systemAddress = st.systemAddress
SET st.x = ss.x,
    st.y = ss.y,
    st.z = ss.z;

/*SPLIT*/

-- Composite index for bounding-box route queries.
-- Replaces the need to touch star_system in findBuyCandidates / findBestSell.
ALTER TABLE stations
ADD INDEX IF NOT EXISTS idx_st_xyz(x, y, z);
