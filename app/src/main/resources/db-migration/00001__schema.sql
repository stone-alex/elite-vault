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
    X             double not null,
    Y             double not null,
    Z             double not null,
    data          text   not null
);
create unique index if not exists idx_stellar_object_system_address on stellar_object(systemAddress);