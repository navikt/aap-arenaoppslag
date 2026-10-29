---
name: caching
description: Use when adding, changing or reviewing Caffeine caches in aap-arenaoppslag services - existing cache inventory, sizes, TTLs and the mandatory Micrometer monitoring.
---

# Caching

**Never add a cache without wiring Micrometer monitoring** (`CaffeineCacheMetrics.monitor(prometheus, cache, "arenaoppslag_...")`).
Caches belong in services (and `PdlGateway`), not in routes or repositories.
Register the monitoring in the class `init` block.

## Existing caches

All caches are monitored via Micrometer.

| Owner | Cache | Key | Max size | Expiry |
|---|---|---|---|---|
| `HistorikkService` | `personIdCache` (`arenaoppslag_person_id`) | fnr (String) -> Int | 30 000 | none, `recordStats()`. Only database hits are stored |
| `PersonService` | `personIdCache` (`arenaoppslag_person_id`) | fnr (String) -> `PersonId` | 30 000 | none, `recordStats()` |
| `InternService` | `maksimum`, `saker`, `perioder`, `perioder_11_17` | | 10 000 each | `expireAfterWrite` 15 min |
| `SakService` | `sakerCache` (`arenaoppslag_saker_per_person`) | | 10 000 | `expireAfterAccess` 30 min |
| `TilkjentYtelserService` | `tilkjentYtelseCache` (`arenaoppslag_tilkjent_ytelse_per_sak`) | | 10 000 | `expireAfterWrite` 15 min |
| `TelleverkService` | `telleverkCache`, `kvotebrukCache` | personId (Int) | 10 000 each | `expireAfterWrite` 15 min |
| `PdlGateway` | PDL identity lookups (`pdl_identer_cache`) | | 10 000 | `expireAfterWrite` 15 min |

Note: `HistorikkService` and `PersonService` both register a metric named `arenaoppslag_person_id`.

Copy the setup of an existing cache (e.g. in `InternService`) when adding a new one.

## Guidelines

- Choose size and TTL deliberately. Arena data can change, so use an expiry unless the mapping is stable (like fnr -> personId).
- Cached values must not be mutated by callers.
- Add a Norwegian comment explaining why the chosen size and expiry fit.
- Test that a second call does not hit the repository (MockK `verify(exactly = 1)`).
- Update the table above when you add or change a cache.
