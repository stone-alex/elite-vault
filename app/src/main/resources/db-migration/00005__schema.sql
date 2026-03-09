-- ============================================================================
-- Elite Vault — Database Schema v5
-- MariaDB 10.3
--
-- Adds dirty flag to stations for incremental trade pair calculation.
-- When a commodity snapshot arrives for a station, the station is marked
-- dirty. The calculate_trade_pairs procedure only processes dirty stations,
-- dramatically reducing work per run.
-- ============================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET COLLATION_CONNECTION = 'utf8mb4_unicode_ci';

-- Add dirty flag. Defaults TRUE so all existing stations get processed
-- on the first run after this migration is applied.
ALTER TABLE stations
ADD COLUMN IF NOT EXISTS dirty BOOLEAN NOT NULL DEFAULT TRUE;

-- Index so the procedure can cheaply find all dirty stations
-- without scanning the full stations table.
ALTER TABLE stations
ADD INDEX IF NOT EXISTS idx_stations_dirty(dirty);