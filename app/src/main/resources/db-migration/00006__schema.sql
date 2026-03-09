CREATE OR REPLACE PROCEDURE calculate_trade_pairs(
    IN p_max_distance_ly FLOAT,
    IN p_min_stock       INT,
    IN p_min_demand      INT
)
calculate_trade_pairs:
BEGIN
    DECLARE v_start DATETIME DEFAULT NOW();
    DECLARE v_rows INT DEFAULT 0;
    DECLARE v_station_rows INT DEFAULT 0;
    DECLARE v_market_id BIGINT;
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_error VARCHAR(500) DEFAULT NULL;

    -- Cursor over dirty stations only.
    -- On first run after migration all stations are dirty (DEFAULT TRUE).
    -- Subsequently only stations that received a new commodity snapshot
    -- since the last run are processed.
    DECLARE cur_dirty CURSOR FOR
        SELECT marketId FROM stations WHERE dirty = TRUE;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
        BEGIN
            GET DIAGNOSTICS CONDITION 1 v_error = MESSAGE_TEXT;
            SET SESSION low_priority_updates = 0;
            UPDATE trade_pair_meta
            SET status        = 'error',
                error_message = v_error
            WHERE id = 1;
            RESIGNAL;
        END;

    -- Skip if already running
    IF (SELECT status FROM trade_pair_meta WHERE id = 1) = 'running'
    THEN
        LEAVE calculate_trade_pairs;
    END IF;

    UPDATE trade_pair_meta
    SET last_started_at  = v_start,
        last_finished_at = NULL,
        status           = 'running',
        error_message    = NULL
    WHERE id = 1;

    -- Yield to ingest connections — trade pair inserts are background work.
    -- Any waiting INSERT/UPDATE on commodity or stations will be served first.
    SET SESSION low_priority_updates = 1;

    -- -----------------------------------------------------------------------
    -- Per dirty station loop
    --
    -- For each dirty station:
    --   1. Delete all existing trade_pair rows where this station is the
    --      buy side OR the sell side (both halves are stale)
    --   2. Re-insert pairs where this station is the buy side
    --      (sell-side rows for this station will be rebuilt when the
    --      corresponding buy-side station is processed)
    --   3. Clear the dirty flag
    --
    -- Each iteration is a contained unit of work. The transaction is short
    -- and releases immediately — ingest is never blocked for more than
    -- the time it takes to process one station.
    -- -----------------------------------------------------------------------

    OPEN cur_dirty;

    station_loop:
    LOOP
        FETCH cur_dirty INTO v_market_id;
        IF v_done
        THEN
            LEAVE station_loop;
        END IF;

        -- Skip if this station was calculated recently.
        -- Multiple EDDN messages within the cooldown window are the same
        -- server tick reported by different pilots — data hasn't changed.
        IF (SELECT TIMESTAMPDIFF(MINUTE, IFNULL(last_pair_calc, '2000-01-01'), NOW())
            FROM stations
            WHERE marketId = v_market_id) < 15
        THEN
            UPDATE stations SET dirty = FALSE WHERE marketId = v_market_id;
            ITERATE station_loop;
        END IF;

        -- Step 1: remove stale pairs involving this station on either side
        DELETE
        FROM trade_pair
        WHERE buyMarketId = v_market_id OR sellMarketId = v_market_id;

        -- Step 2: re-insert pairs where this station is the buy side.
        -- Covers all commodities available at this station in one query.
        INSERT INTO trade_pair (commodityId, commodityName,
                                buyMarketId, buySystemAddress, buySystem, buyStation,
                                buyPrice, buyStock, buyX, buyY, buyZ,
                                buyHasLargePad, buyHasMediumPad, buyStationType, buyDistToArrival,
                                sellMarketId, sellSystemAddress, sellSystem, sellStation,
                                sellPrice, sellDemand,
                                sellHasLargePad, sellHasMediumPad, sellStationType, sellDistToArrival,
                                sellX, sellY, sellZ,
                                profitPerUnit, distanceLy)
        SELECT b.commodityId,
            ct.name,
            b.marketId, b.systemAddress, bss.starName, bst.realName,
            b.buyPrice, b.stock, bss.x, bss.y, bss.z,
            bst.hasLargePad, bst.hasMediumPad, bst.stationType, bst.distanceToArrival,
            s.marketId, s.systemAddress, sss.starName, sst.realName,
            s.sellPrice, s.demand,
            sst.hasLargePad, sst.hasMediumPad, sst.stationType, sst.distanceToArrival,
            sss.x, sss.y, sss.z,
            (s.sellPrice - b.buyPrice),
            ROUND(SQRT(POW(bss.x - sss.x, 2) + POW(bss.y - sss.y, 2) + POW(bss.z - sss.z, 2)), 2)
        FROM commodity b
                 INNER JOIN commodity_type ct ON ct.id = b.commodityId
                 INNER JOIN star_system bss ON bss.systemAddress = b.systemAddress
                 INNER JOIN stations bst ON bst.marketId = b.marketId
                 INNER JOIN commodity s ON s.commodityId = b.commodityId
            AND s.marketId != b.marketId
            AND s.sellPrice > b.buyPrice
            AND s.demand >= p_min_demand
                 INNER JOIN star_system sss ON sss.systemAddress = s.systemAddress
                 INNER JOIN stations sst ON sst.marketId = s.marketId
        WHERE b.marketId = v_market_id AND b.buyPrice > 0 AND b.stock >= p_min_stock AND sss.x BETWEEN bss.x - p_max_distance_ly AND bss.x + p_max_distance_ly AND sss.y BETWEEN bss.y - p_max_distance_ly AND bss.y + p_max_distance_ly AND
            sss.z BETWEEN bss.z - p_max_distance_ly AND bss.z + p_max_distance_ly AND
            POW(bss.x - sss.x, 2) + POW(bss.y - sss.y, 2) + POW(bss.z - sss.z, 2)
                <= POW(p_max_distance_ly, 2)
        GROUP BY b.commodityId, b.marketId, s.marketId;

        SET v_station_rows = ROW_COUNT();
        SET v_rows = v_rows + v_station_rows;

        -- Step 3: clear dirty flag and record calculation time
        UPDATE stations SET dirty = FALSE, last_pair_calc = NOW() WHERE marketId = v_market_id;

    END LOOP station_loop;

    CLOSE cur_dirty;

    -- Restore default session priority
    SET SESSION low_priority_updates = 0;

    UPDATE trade_pair_meta
    SET last_finished_at = NOW(),
        last_duration_ms = TIMESTAMPDIFF(SECOND, v_start, NOW()) * 1000,
        last_row_count   = v_rows,
        status           = 'ok',
        error_message    = NULL
    WHERE id = 1;

END;

/*SPLIT*/

DROP EVENT IF EXISTS calculate_trade_pairs_event;

/*SPLIT*/

CREATE EVENT calculate_trade_pairs_event
    ON SCHEDULE EVERY 1 HOUR
        STARTS CURRENT_TIMESTAMP + INTERVAL 10 MINUTE
    DO
    CALL calculate_trade_pairs(250.0, 1, 1);

/*SPLIT*/

SET GLOBAL event_scheduler = ON;