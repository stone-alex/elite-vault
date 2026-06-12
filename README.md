# Elite Vault (pre-alpha - proof of concept)

## Self-hosted, real-time data platform for Elite Dangerous

There is no development happening on this project right now.

Elite Vault is a lightweight, pure-Java service that connects to the
[Elite Dangerous Data Network (EDDN)](https://github.com/EDCD/EDDN)
to ingest live game data (markets, powerplay, systems, discoveries, etc.)
and stores it locally in a relational database. It exposes a stable, versioned
REST API for querying that data.

Designed as a **developer platform and self-hosting alternative** to third-party
sites like Spansh, EDSM, and EDDB — which expose unofficial website APIs that
break without notice, go offline, or disappear entirely when a single maintainer
moves on.

### Project Status

**R&D / early development.** Core ingest and trade route API are functional.
Additional endpoints (powerplay, bodies, factions, fleet carriers, neutron
routing) are planned. The API surface will grow as data coverage improves.

Contributions welcome. If you're building a tool on top of Elite Dangerous
data and are tired of depending on sites you don't control — this is for you.
