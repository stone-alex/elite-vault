CREATE OR REPLACE PROCEDURE calculate_trade_pairs(
    IN p_max_distance_ly FLOAT,
    IN p_min_stock       INT,
    IN p_min_demand      INT
)
calculate_trade_pairs:
BEGIN
    DECLARE v_start DATETIME DEFAULT NOW();
    DECLARE v_rows INT DEFAULT 0;
    DECLARE v_commodity SMALLINT UNSIGNED;
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_error VARCHAR(500) DEFAULT NULL;

    -- Cursor over all known commodity types (~300 rows, tiny).
    DECLARE cur_commodities CURSOR FOR
        SELECT id FROM commodity_type ORDER BY id;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
        BEGIN
            GET DIAGNOSTICS CONDITION 1 v_error = MESSAGE_TEXT;
            UPDATE trade_pair_meta
            SET status        = 'error',
                error_message = v_error
            WHERE id = 1;
            RESIGNAL;
        END;

    -- Skip if a previous run is still in progress
    IF (SELECT status FROM trade_pair_meta WHERE id = 1) = 'running'
    THEN
        LEAVE calculate_trade_pairs;
    END IF;

    -- Mark as running
    UPDATE trade_pair_meta
    SET last_started_at  = v_start,
        last_finished_at = NULL,
        status           = 'running',
        error_message    = NULL
    WHERE id = 1;

    -- Truncate the live table once up front.
    -- Each commodity loop iteration does a short INSERT then moves on.
    -- Ingest connections are never blocked for more than a few seconds.
    TRUNCATE TABLE trade_pair;

    OPEN cur_commodities;

    commodity_loop:
    LOOP
        FETCH cur_commodities INTO v_commodity;
        IF v_done
        THEN
            LEAVE commodity_loop;
        END IF;

        INSERT INTO trade_pair (commodityId, commodityName,
                                buyMarketId, buySystemAddress, buySystem, buyStation,
                                buyPrice, buyStock, buyX, buyY, buyZ,
                                buyHasLargePad, buyHasMediumPad, buyStationType, buyDistToArrival,
                                sellMarketId, sellSystemAddress, sellSystem, sellStation,
                                sellPrice, sellDemand,
                                sellHasLargePad, sellHasMediumPad, sellStationType, sellDistToArrival,
                                profitPerUnit, distanceLy)
        SELECT b.commodityId,
            ct.name,
            b.marketId, b.systemAddress, bss.starName, bst.realName,
            b.buyPrice, b.stock, bss.x, bss.y, bss.z,
            bst.hasLargePad, bst.hasMediumPad, bst.stationType, bst.distanceToArrival,
            s.marketId, s.systemAddress, sss.starName, sst.realName,
            s.sellPrice, s.demand,
            sst.hasLargePad, sst.hasMediumPad, sst.stationType, sst.distanceToArrival,
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
        WHERE b.commodityId = v_commodity AND b.buyPrice > 0 AND b.stock >= p_min_stock AND sss.x BETWEEN bss.x - p_max_distance_ly AND bss.x + p_max_distance_ly AND sss.y BETWEEN bss.y - p_max_distance_ly AND bss.y + p_max_distance_ly AND
            sss.z BETWEEN bss.z - p_max_distance_ly AND bss.z + p_max_distance_ly AND
            POW(bss.x - sss.x, 2) + POW(bss.y - sss.y, 2) + POW(bss.z - sss.z, 2)
                <= POW(p_max_distance_ly, 2)
        GROUP BY b.commodityId, b.marketId, s.marketId;

        SET v_rows = v_rows + ROW_COUNT();

    END LOOP commodity_loop;

    CLOSE cur_commodities;

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
    CALL calculate_trade_pairs(25.0, 1, 1);

/*SPLIT*/

SET GLOBAL event_scheduler = ON;