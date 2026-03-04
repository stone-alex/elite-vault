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