-- ============================================================================
-- Elite Vault - Partition maintenance procedure and event
-- MySQL 8.0+
--
-- IMPORTANT MySQL 8 vs MariaDB difference:
--   PREPARE...FROM requires a user variable (@var), NOT a local DECLARE'd
--   variable. MariaDB accepted local vars; MySQL 8 rejects them with a
--   syntax error. All dynamic SQL is built into @ev_sql (user variable)
--   before being prepared.
-- ============================================================================

DROP PROCEDURE IF EXISTS maintain_commodity_partitions;

/*SPLIT*/

CREATE PROCEDURE maintain_commodity_partitions(
    IN p_hours_ahead   INT,
    IN p_hours_to_keep INT
)
BEGIN
    -- All DECLARE statements must come first in MySQL 8.
    DECLARE v_schema VARCHAR(64) DEFAULT DATABASE();
    DECLARE v_table VARCHAR(64) DEFAULT 'commodity';
    DECLARE v_part_name VARCHAR(64);
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_hour BIGINT;
    DECLARE v_end_hour BIGINT;
    DECLARE v_boundary BIGINT;

    -- -------------------------------------------------------------------------
    -- Phase 1: Add future partitions by reorganising p_future.
    -- @ev_sql is a user-session variable — MySQL 8 PREPARE FROM only accepts
    -- user variables or string literals, not local DECLARE'd variables.
    -- -------------------------------------------------------------------------
    SET v_hour = UNIX_TIMESTAMP(NOW()) DIV 3600;
    SET v_end_hour = v_hour + p_hours_ahead;
    SET @ev_sql = CONCAT('ALTER TABLE `', v_schema, '`.`', v_table,
                         '` REORGANIZE PARTITION p_future INTO (');

    WHILE v_hour <= v_end_hour
        DO
            SET v_part_name = CONCAT('p_h_', DATE_FORMAT(FROM_UNIXTIME(v_hour * 3600), '%Y%m%d_%H'));
            SET v_boundary = v_hour + 1;

            IF NOT EXISTS (SELECT 1
                           FROM INFORMATION_SCHEMA.PARTITIONS
                           WHERE TABLE_SCHEMA = v_schema AND TABLE_NAME = v_table AND PARTITION_NAME = v_part_name)
            THEN
                SET @ev_sql = CONCAT(@ev_sql,
                                     'PARTITION `', v_part_name, '` VALUES LESS THAN (', v_boundary, '),');
            END IF;

            SET v_hour = v_hour + 1;
        END WHILE;

    SET @ev_sql = CONCAT(@ev_sql, 'PARTITION p_future VALUES LESS THAN MAXVALUE)');

    PREPARE ev_stmt FROM @ev_sql;
    EXECUTE ev_stmt;
    DEALLOCATE PREPARE ev_stmt;

    -- -------------------------------------------------------------------------
    -- Phase 2: Drop partitions older than p_hours_to_keep.
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

            SET @ev_sql = CONCAT('ALTER TABLE `', v_schema, '`.`', v_table,
                                 '` DROP PARTITION `', v_part_name, '`');
            PREPARE ev_stmt FROM @ev_sql;
            EXECUTE ev_stmt;
            DEALLOCATE PREPARE ev_stmt;
        END LOOP drop_loop;
        CLOSE cur_old;
    END;

END;

/*SPLIT*/

DROP EVENT IF EXISTS maintain_commodity_partitions_hourly;

/*SPLIT*/

CREATE EVENT maintain_commodity_partitions_hourly
    ON SCHEDULE EVERY 1 HOUR
        STARTS CURRENT_TIMESTAMP + INTERVAL 5 MINUTE
    DO
    CALL maintain_commodity_partitions(72, 6);

/*SPLIT*/

SET GLOBAL event_scheduler = ON;