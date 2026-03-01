# Elite Vault

## Self-hosted, real-time data vault for Elite Dangerous

Elite Vault is a lightweight, pure-Java service that connects to the [Elite Dangerous Data Network (EDDN)](https://github.com/EDCD/EDDN)
to ingest live game data (markets, powerplay, systems, discoveries, etc.)
and stores it locally in a relational database. It provides a simple, reliable HTTP API for querying this data.
Perfect for replacing fragile third-party websites like Spansh or INARA in your tools (e.g. [Elite Intel](https://github.com/stone-alex/EliteIntel)).

Designed for self-hosting on Linux servers (or even modest hardware like a repurposed desktop or Raspberry Pi paired with home NAS),
it emphasizes **data freshness** over historical archives: market prices and powerplay states are kept current
(overwritten with latest events), while persistent data like systems and bodies accumulates slowly.

## Why Elite Vault?

Third-party sites are great until the maintainer retires, the server goes offline, or the "unofficial API" breaks.
Elite Vault gives you full control: your data, your uptime, your queries accessible from Elite Intel or any custom tool that implements the REST API.

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

## Architecture

```
[EDDN ZeroMQ Relay] ──► [elite-vault-ingest.jar] ──► SQLite DB (or MariaDB)
        │
        ▼
[elite-vault-api.jar] ──► HTTP / JSON API
        │
        ▼
Elite Intel / Other Clients
```

- **Ingest process**: Connects via ZeroMQ SUB socket ► parses messages ► upserts into DB (focus on latest commodity/powerplay data).
- **API process**: Lightweight HTTP server exposing query endpoints (e.g. best commodity price in range, fleet carrier route planning, current powerplay forts).
- Shared core: Models, DAOs (JDBI), managers, config.

## Tech Stack

- Language: Java (21+)
- Build: Gradle (multi-module: `:commons`, `:ingest`, `:api`)
- Database: SQLite (embedded, default) or MariaDB/MySQL
- DB access: JDBI 3
- JSON: Gson (primary) + Jackson (for YAML if needed)
- HTTP (API): Spring Boot (standalone) or plain HttpServer
- Messaging: JeroMQ (pure-Java ZeroMQ) or native libzmq binding
- Logging: SLF4J + Logback

## Getting Started

### Prerequisites

- Java 21+
- Gradle
- Linux server/desktop (Debian/Ubuntu recommended; tested on modest hardware)
- ~4–8 GB RAM, SSD preferred for SQLite

### Clone & Build

```bash
## ------------------------------------------------------------------
git clone https://github.com/stone-alex/elite-vault.git
cd elite-vault
gradle build

## ------------------------------------------------------------------
# Ingest (background listener)
java -jar elite-vault-ingest.jar --spring.profiles.active=ingest

## ------------------------------------------------------------------
# API (HTTP server)
java -jar elite-vault-api.jar --spring.profiles.active=api
```