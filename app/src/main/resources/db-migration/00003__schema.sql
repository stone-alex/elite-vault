DROP EVENT IF EXISTS maintain_commodity_partitions_hourly;

CREATE EVENT maintain_commodity_partitions_hourly
    ON SCHEDULE EVERY 1 HOUR
        STARTS CURRENT_TIMESTAMP + INTERVAL 5 MINUTE
    DO
    CALL maintain_commodity_partitions(72, 3);