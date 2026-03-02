create table if not exists market (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp   text   not null,
    marketId    bigint not null,
    starSystem  text   not null,
    stationName text   not null,
    data        text   not null
);
create unique index if not exists idx_market_market_id on market(marketId);


create table if not exists stellar_object (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp     text   not null,
    starSystem    text   not null,
    systemAddress bigint not null,
    x double not null,
    y double not null,
    z double not null,
    data          text   not null
);
create unique index if not exists idx_stellar_object_system_address on stellar_object(systemAddress);

CREATE INDEX IF NOT EXISTS idx_stellar_primary ON stellar_object(starSystem)
    WHERE json_extract(data, '$.DistanceFromArrivalLS') = 0;

CREATE INDEX IF NOT EXISTS idx_stellar_x ON stellar_object(x);
CREATE INDEX IF NOT EXISTS idx_stellar_y ON stellar_object(y);
CREATE INDEX IF NOT EXISTS idx_stellar_z ON stellar_object(z);

-- Composite helps a bit more on range queries
CREATE INDEX IF NOT EXISTS idx_stellar_xyz ON stellar_object(x, y, z);