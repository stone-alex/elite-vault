-- ============================================================================
-- Elite Vault - Partition maintenance procedure and event
-- MariaDB 10.3
--
-- NOTE: This file is NOT executed via JDBI's createScript().
-- It is handled specially by DatabaseMigrator using raw JDBC calls
-- because DELIMITER is a mysql client directive, not valid JDBC SQL.
-- Each statement is executed individually via Connection.prepareCall()
-- or Statement.execute().
-- ============================================================================

CREATE OR REPLACE PROCEDURE maintain_commodity_partitions(
    IN p_hours_ahead   INT,
    IN p_hours_to_keep INT
)
BEGIN
    DECLARE v_schema VARCHAR(64) DEFAULT DATABASE();
    DECLARE v_table VARCHAR(64) DEFAULT 'commodity';
    DECLARE v_part_name VARCHAR(64);
    DECLARE v_boundary BIGINT;
    DECLARE v_sql TEXT;
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_hour BIGINT;
    DECLARE v_end_hour BIGINT;

    SET v_hour = UNIX_TIMESTAMP(NOW()) DIV 3600;
    SET v_end_hour = v_hour + p_hours_ahead;
    SET v_sql = CONCAT('ALTER TABLE ', v_table, ' REORGANIZE PARTITION p_future INTO (');

    WHILE v_hour <= v_end_hour
        DO
            SET v_part_name = CONCAT('p_h_', FROM_UNIXTIME(v_hour * 3600, '%Y%m%d_%H'));
            SET v_boundary = v_hour + 1;

            IF NOT EXISTS (SELECT 1
                           FROM INFORMATION_SCHEMA.PARTITIONS
                           WHERE TABLE_SCHEMA = v_schema AND TABLE_NAME = v_table AND PARTITION_NAME = v_part_name)
            THEN
                SET v_sql = CONCAT(v_sql,
                                   'PARTITION ', v_part_name,
                                   ' VALUES LESS THAN (', v_boundary, '),');
            END IF;

            SET v_hour = v_hour + 1;
        END WHILE;

    SET v_sql = CONCAT(v_sql, 'PARTITION p_future VALUES LESS THAN MAXVALUE)');

    PREPARE stmt FROM v_sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

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

            SET v_sql = CONCAT('ALTER TABLE ', v_table, ' DROP PARTITION ', v_part_name);
            PREPARE stmt FROM v_sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
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