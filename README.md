# Elite Vault

## Self-hosted, real-time data platform for Elite Dangerous

Elite Vault is a lightweight, pure-Java service that connects to the
[Elite Dangerous Data Network (EDDN)](https://github.com/EDCD/EDDN)
to ingest live game data (markets, powerplay, systems, discoveries, etc.)
and stores it locally in a relational database. It exposes a stable, versioned
REST API for querying that data.

Designed as a **developer platform and self-hosting alternative** to third-party
sites like Spansh, EDSM, and EDDB — which expose unofficial website APIs that
break without notice, go offline, or disappear entirely when a single maintainer
moves on.

---

### Why Elite Vault?

Third-party Elite Dangerous data sites are great until the maintainer retires,
the server goes down, or the "unofficial API" breaks because the frontend
was redesigned. Elite Vault gives you full control:

- your data
- your uptime
- your query contracts
- accessible from [Elite Intel](https://github.com/stone-alex/EliteIntel),
  COVAS:NEXT, EDCoPilot, or any tool that speaks REST

The goal is to solve the ecosystem's structural pain points:

| Problem                     | Elite Vault answer                                   |
|-----------------------------|------------------------------------------------------|
| Unstable upstreams          | Local data hub, no external dependency at query time |
| Unofficial scraping         | Stable versioned REST API with documented contracts  |
| Partial datasets            | Bootstrap from Spansh bulk dump + live EDDN fill     |
| Scattered APIs              | Single consistent schema, one integration point      |
| Single-maintainer SPOFs     | Self-hosted; future mesh/federation planned          |
| Poor integration ergonomics | Sane JSON, predictable endpoints, no surprises       |

---

### Key Features

- **Live EDDN ingestion** — Subscribes to the ZeroMQ relay for real-time
  commodity markets, powerplay events, nav routes, discoveries, and more.
- **Freshness-first storage** — Markets and dynamic states are updated in-place
  (UPSERT); old data is not retained unless useful.
- **Hybrid schema** — Normalized core tables (systems, stations, commodities)
  plus JSON blobs for schema resilience against EDDN format changes.
- **Two decoupled processes**:
    - **Ingest** — Background listener and DB writer. Steady, low-CPU IO.
      Designed to run 24/7 on modest hardware.
    - **API** — Lightweight HTTP server. Can run on a separate, more powerful
      machine than the DB host.
- **Bootstrap importer** — One-shot import of Spansh nightly bulk dumps to
  seed the database without waiting for EDDN coverage.
- **Fleet carrier filtering** — Fleet carriers are excluded from trade routes:
  they move, their market access is player-controlled, and they are too
  numerous to be reliable routing targets.
- **Pure Java** — No heavy frameworks. JDBI for DB access, Javalin for HTTP,
  Gson/Jackson for serialization, JeroMQ for EDDN.
- **Self-hosted and private** — No cloud dependency. Runs entirely on your
  own hardware.
- **Future-proof** — Designed with eventual peer-to-peer sync and
  decentralization in mind.

---

### Tech Stack

| Layer       | Library / Version         |
|-------------|---------------------------|
| Language    | Java 21+                  |
| Build       | Gradle                    |
| Database    | MariaDB 10.3+             |
| DB access   | JDBI 3                    |
| HTTP / REST | Javalin 7                 |
| JSON        | Gson + Jackson            |
| Messaging   | JeroMQ (pure-Java ZeroMQ) |
| Logging     | SLF4J + Logback           |

---

### Modules

Three fat JARs built from a single Gradle project. Each is intended to run
as a systemd service (or equivalent process manager).

| Module      | Role                             | Run continuously?     |
|-------------|----------------------------------|-----------------------|
| `ingest`    | Subscribes to EDDN, writes to DB | Yes — 24/7            |
| `api`       | Serves REST API                  | Yes, or on-demand     |
| `bootstrap` | One-shot Spansh dump importer    | No — run once to seed |

All three modules share the same core (models, DAOs, managers, config) and
require access to the same database instance.

---

### Hardware Requirements

#### Minimum (R&D / personal use)

This configuration is functional for development and light personal use.
Query performance is limited by NAS hardware and network IO.

| Component            | Minimum                                                              |
|----------------------|----------------------------------------------------------------------|
| Ingest + API process | Raspberry Pi 5, 8 GB RAM                                             |
| Database             | Synology NAS running MariaDB 10.3                                    |
| Network              | Wired gigabit between Pi and NAS                                     |
| Storage              | NAS SSD (do **not** use Pi SD card for DB writes — it will burn out) |

> **Note:** MariaDB 10.3 is the only version available as a Synology package.
> If running DB on a separate Linux machine, MariaDB 10.6+ or MySQL 8 is
> preferred. PostgreSQL is **not recommended** on NFS-mounted storage due to
> strict directory ownership and file-locking requirements.

#### Recommended (stable self-hosting / small team)

| Component      | Recommended                                             |
|----------------|---------------------------------------------------------|
| All-in-one box | Repurposed i5/i7 desktop or small form factor PC        |
| RAM            | 32 GB minimum, 64 GB preferred                          |
| Storage        | 1–2 TB NVMe SSD                                         |
| Network        | Wired gigabit                                           |
| OS             | Any modern Linux (Debian/Ubuntu LTS recommended)        |
| DB             | MariaDB 10.6+ or MySQL 8, local socket (no network hop) |

A decommissioned office machine (e.g. Dell Optiplex with i7) with an NVMe
swap is sufficient and typically available cheaply on the second-hand market.
This is the minimum sensible spec for hosting the full Spansh-equivalent
dataset (60M+ star systems, 500k+ stations, full commodity history).

> **Why the RAM?** The full galaxy coordinate dataset alone is several GB.
> A properly sized InnoDB buffer pool eliminates disk IO for hot queries
> after warmup. On the minimum NAS setup, the 64 MB buffer pool is the
> primary performance constraint.

---

### Running

```bash
# Seed the database from a Spansh bulk dump (run once)
java -jar elite-vault-bootstrap.jar

# Start the EDDN ingest listener
java -jar elite-vault-ingest.jar

# Start the REST API server
java -jar elite-vault-api.jar
```

All three read from the same `config.properties` (or environment variables)
for DB connection and port configuration.

---

### API Example

Base URL: `http://<host>:8085/api/v1`

| Endpoint                   | Description                                          |
|----------------------------|------------------------------------------------------|
| `GET /search/traderoute`   | Multi-hop trade route from a position or system name |
| `GET /search/commodity`    | Best prices for a commodity within a radius          |
| *(more endpoints planned)* | Powerplay, fleet carriers, nav, bodies, factions…    |

#### Trade Route Parameters

| Parameter          | Type    | Default  | Description                                  |
|--------------------|---------|----------|----------------------------------------------|
| `x`, `y`, `z`      | Double  | —        | Starting coordinates (preferred)             |
| `startSystem`      | String  | —        | Starting system name (fallback if no coords) |
| `numHops`          | Integer | 3        | Number of legs (max 20)                      |
| `hopDistance`      | Double  | 250.0 ly | Max travel distance per hop (max 5000)       |
| `maxDistToArrival` | Double  | 6000 ls  | Max station distance from star               |
| `cargoCap`         | Integer | 512 t    | Cargo capacity for profit estimate           |
| `requireLargePad`  | Boolean | false    | Large pad required at both stations          |
| `requireMediumPad` | Boolean | false    | Medium or large pad required                 |
| `allowPlanetary`   | Boolean | false    | Include surface/planetary stations           |

Fleet carriers are always excluded regardless of parameters.

---

### Data Model Notes

- **`star_system`** — 763k+ rows (grows via EDDN FSDJump and Spansh bootstrap).
  Spatial index on `(x, y)` for corridor queries.
- **`stations`** — 14k+ rows. Stores `x, y, z` coordinates directly (copied
  from `star_system` at upsert time) to avoid joins in hot route queries.
  Fleet carriers included in storage but excluded at query time.
- **`commodity`** — Rolling hot table. Previous snapshot for a market is
  deleted and replaced on each ingest event. ~216k rows steady state.
- **`commodity_type`** — ~300 rows. Fully cached in-process at startup.

Stations whose `systemAddress` is not yet present in `star_system` are
silently dropped on ingest and will be re-ingested when next visited by
a player. This avoids orphaned stations with unknown coordinates.

---

### Project Status

**R&D / early development.** Core ingest and trade route API are functional.
Additional endpoints (powerplay, bodies, factions, fleet carriers, neutron
routing) are planned. The API surface will grow as data coverage improves.

Contributions welcome. If you're building a tool on top of Elite Dangerous
data and are tired of depending on sites you don't control — this is for you.
