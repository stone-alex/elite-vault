# Elite Vault

## Self-hosted, real-time data vault for Elite Dangerous

Elite Vault is a lightweight, pure-Java service that connects to the [Elite Dangerous Data Network (EDDN)](https://github.com/EDCD/EDDN)
to ingest live game data (markets, power play, systems, discoveries, etc.)
and stores it locally in a relational database. It provides a simple, custom HTTP API for querying this data.

Designed for self-hosting on Linux servers (or even modest hardware like a repurposed desktop or Synology NAS
paired with Raspberry Pi 5), it emphasizes **data freshness** over historical archives: market prices
and power play states are kept current (overwritten with latest events), while persistent data like systems
and bodies accumulate slowly.

----

### Why Elite Vault?

Custom REST API for for commodity prices, powerplay data and galaxy navigation.

Third-party sites are great until the maintainer retires, the server goes offline, or the "unofficial API" breaks.
Elite Vault gives you full control: your data, your uptime, your queries are accessible from [Elite Intel](https://github.com/stone-alex/EliteIntel)
or any custom tool that implements the REST API.

----
### Key Features

- **Live EDDN ingestion** ► Subscribes to the ZeroMQ relay for real-time commodity markets, powerplay events, nav routes, discoveries, and more.
- **Freshness-first storage** ► Markets and dynamic states are updated in-place (UPSERT); old data is not retained unless useful.
- **Hybrid schema** ► Normalized core tables (systems, stations) + JSON blobs for schema resilience (easy adaptation to EDDN format changes).
- **Two decoupled processes**:
    - Ingest: Background listener & DB writer (steady, low-CPU IO).
    - API: HTTP server for queries (JSON endpoints for trade routes, best prices, fleet carrier jumps, powerplay status, etc.).
- **Pure Java** ► No heavy frameworks beyond what's needed (Spring Boot optional for API, JDBI for DB, Gson/Jackson for serialization).
- **Self-hosted & private** ► Run on your own hardware; no cloud dependency.
- **Future-proof** ► Designed with eventual peer-to-peer sync / decentralization in mind.

- **Ingest process**: Connects via ZeroMQ SUB socket ► parses messages ► upserts into DB (focus on latest commodity/powerplay data).
- **API process**: Lightweight HTTP server exposing query endpoints (e.g. best commodity price in range, fleet carrier route planning, current powerplay forts).
- Shared core: Models, DAOs (JDBI), managers, config.

----

### Tech Stack
- Language: Java (21+)
- Build: Gradle
- Database: MariaDB
- DB access: JDBI 3
- REST API - JSON: Gson + Jackson
- HTTP (API): Javalin
- Messaging: JeroMQ (pure-Java ZeroMQ)
- Logging: SLF4J + Logback

----

## Modules

The server has three modules meant to run with systemctl or other process manager.

1) API - provides REST API to query data
2) Ingest - subscribes to EDDN and continuously updates the local data
3) Bootsttrap - optional, allows to ingest initial nightly data dump from Spansh to get you started.

- The server runs on Raspberry Pi 5 (minimum hardware requirement).
- The database can run on Synology NAS.
- Or run this on a Linux Desktop or a dedicated headless LINUX.
- Database is MariaDB 10.3 or newer.
- The services can be run on the same or different machines, as long as they have access to the same database.

## Examples:

### Fleet Carrier from Sol to Colonia

Request:

```shell
curl -X 'GET' \
  'http://localhost:8085/api/v1/search/carrier/route?from=Sol&to=Colonia&jumpRange=450' \
  -H 'accept: application/json'
```

Response:

```json
{
  "from": "Sol",
  "to": "Colonia",
  "jumps": 51,
  "route": [
    "Col 359 Sector IO-A a31-0",
    "B133 Sector LI-R b5-7",
    "HR 7119",
    "HD 177907",
    "HD 177532",
    "Aucopp FB-X f1-1",
    "Droju WV-C d43",
    "Smojue UL-S b18-0",
    "Smojue IM-W f1-1",
    "Drojeae QZ-D d13-35",
    "Blae Drye VK-K b14-5",
    "Blae Drye MK-Z c16-14",
    "Blae Drye MU-D d13-70",
    "Gria Drye MU-S c6-19",
    "Gria Drye DP-Y b33-9",
    "Gria Drye GU-D d13-36",
    "Nyeajeau YU-Y f0",
    "Nyeajaae ZE-A g11",
    "Nyeajaae DL-Y g2",
    "Flyiedgiae YE-A g1",
    "Flyiedgiae ZW-V b21-0",
    "Flyiedgiae EW-W f1-1",
    "Flyae Eaec YE-A g4",
    "Skaude ZE-A g6",
    "Skaude YJ-A f0",
    "Skaude OR-A b18-9",
    "Skaude EW-W f1-1",
    "Skaudai UA-I c25-1",
    "Prua Phoe ZE-A g6",
    "Prua Phoe DL-Y g4",
    "Prua Phoe SF-D d13-196",
    "Clooku WZ-W d2-1738",
    "Clooku SK-M d8-363",
    "Clooku SP-M b53-48",
    "Nuekuae KA-S c6-105",
    "Stuelou NX-T e3-1913",
    "Stuelou DL-Y g8",
    "Blua Eaec AA-A h43",
    "Blua Eaec YE-A g4",
    "Blua Eaec AA-A h51",
    "Boelts QT-Z d44",
    "Boelts LP-M b24-33",
    "Boeph TT-H d10-188",
    "Eoch Flyuae AA-A h66",
    "Eoch Flyuae SE-S b20-51",
    "Eoch Flyuae KY-H d10-3462",
    "Dryio Flyuae AA-A g4",
    "Dryio Flyuae XL-L c8-14",
    "Dryooe Flyou WY-I d9-273",
    "Eol Prou GB-E b77",
    "Colonia"
  ],
  "note": "Route calculated in 10 seconds."
}
```

----

### Calculate trade route with 6 stops

Request:

```shell
curl -X 'GET' \
  'http://localhost:8085/api/v1/search/traderoute?startingLocationStarSystem=Sol&numTrades=6&numBudget=500000000&maxDistanceFromEntrance=6000&jumpRange=100&requireLargeLandingPad=true&requireMediumLandingPad=false&allowEnemyStrongHolds=false&allowProhibited=false&allowPlanetaryLandings=true' \
  -H 'accept: application/json'
```

Response:

```json
{
  "route": {
    "1": {
      "sourceSystem": "AD Leonis",
      "sourceStation": "Scortia Hub",
      "destinationSystem": "Puppis Sector BQ-P a5-0",
      "destinationStation": "Suomi Town",
      "sourceMarketId": 4290133763,
      "destinationMarketId": 4217430275,
      "commodity": "aquaponicsystems",
      "buyPrice": 15,
      "sellPrice": 2321,
      "profitPerUnit": 2306,
      "stock": 213759,
      "demand": 128968,
      "distanceLy": 68.8
    },
    "2": {
      "sourceSystem": "Puppis Sector BQ-P a5-0",
      "sourceStation": "Suomi Town",
      "destinationSystem": "Ross 614",
      "destinationStation": "Pasteur Hub",
      "sourceMarketId": 4217430275,
      "destinationMarketId": 4226299907,
      "commodity": "foodcartridges",
      "buyPrice": 132,
      "sellPrice": 767,
      "profitPerUnit": 635,
      "stock": 92047,
      "demand": 195715,
      "distanceLy": 58.7
    },
    "3": {
      "sourceSystem": "Ross 614",
      "sourceStation": "Pasteur Hub",
      "destinationSystem": "Duamta",
      "destinationStation": "Davis Terminal",
      "sourceMarketId": 4226299907,
      "destinationMarketId": 3228791808,
      "commodity": "Biowaste",
      "buyPrice": 143,
      "sellPrice": 639,
      "profitPerUnit": 496,
      "stock": 17465,
      "demand": 13534554,
      "distanceLy": 9.5
    },
    "4": {
      "sourceSystem": "Duamta",
      "sourceStation": "Davis Terminal",
      "destinationSystem": "DEN 0255-4700",
      "destinationStation": "Darnley-Smith Point",
      "sourceMarketId": 3228791808,
      "destinationMarketId": 4218247171,
      "commodity": "Leather",
      "buyPrice": 188,
      "sellPrice": 2393,
      "profitPerUnit": 2205,
      "stock": 30862,
      "demand": 662544,
      "distanceLy": 16.4
    },
    "5": {
      "sourceSystem": "DEN 0255-4700",
      "sourceStation": "Darnley-Smith Point",
      "destinationSystem": "Duamta",
      "destinationStation": "Davis Terminal",
      "sourceMarketId": 4218247171,
      "destinationMarketId": 3228791808,
      "commodity": "biowaste",
      "buyPrice": 52,
      "sellPrice": 639,
      "profitPerUnit": 587,
      "stock": 5381,
      "demand": 13534554,
      "distanceLy": 16.4
    },
    "6": {
      "sourceSystem": "Duamta",
      "sourceStation": "Davis Terminal",
      "destinationSystem": "Luyten's Star",
      "destinationStation": "Rescue Ship Hutner",
      "sourceMarketId": 3228791808,
      "destinationMarketId": 129020287,
      "commodity": "water",
      "buyPrice": 52,
      "sellPrice": 1746,
      "profitPerUnit": 1694,
      "stock": 13339,
      "demand": 1364352840,
      "distanceLy": 6.1
    }
  },
  "note": "Route calculated in 183 seconds"
}
```