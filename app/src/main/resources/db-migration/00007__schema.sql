-- ============================================================================
-- Elite Vault — Database Schema v7
-- MariaDB 10.3
--
-- Adds sell system coordinates to trade_pair so the route calculation
-- can advance position hop-to-hop without a star_system lookup.
-- ============================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET COLLATION_CONNECTION = 'utf8mb4_unicode_ci';

ALTER TABLE trade_pair
ADD COLUMN IF NOT EXISTS sellX DOUBLE NOT NULL DEFAULT 0 AFTER sellDistToArrival,
ADD COLUMN IF NOT EXISTS sellY DOUBLE NOT NULL DEFAULT 0 AFTER sellX,
ADD COLUMN IF NOT EXISTS sellZ DOUBLE NOT NULL DEFAULT 0 AFTER sellY;

ALTER TABLE stations
ADD COLUMN last_pair_calc DATETIME NULL DEFAULT NULL;