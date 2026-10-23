---
name: add-endpoint
description: Use when adding or changing an HTTP endpoint in aap-arenaoppslag - choosing route prefix (/api/v1, /api/intern, /api/migrering), layering route/service/repository, mapping domain objects with tilKontrakt(), and stability rules per prefix.
---

# Adding or changing an endpoint

## Route prefixes

All routes except `/actuator/*` require Azure AD JWT authentication.

| Prefix | Purpose | Rule |
|---|---|---|
| `/actuator/*` | Health and metrics (unauthenticated) | No business logic here |
| `/intern/*` | Old API used by Kelvin and aap-api-intern. Frozen. | Breaking changes only through expand-contract |
| `/api/v1/*` | External API, stable contract. Default for new endpoints. | No breaking changes without versioning |
| `/api/intern/*` | Internal API for frontends ("backend for frontend") | Breaking changes allowed, coordinate with consumers |
| `/api/migrering/*` | Only for migration from Arena to Kelvin | Breaking changes allowed |

### Personal identifiers

A personal identifier (`fødselsnummer`, `personident`, or anything identifying a natural person)
must **never** appear in a URL, neither as path segment nor query parameter.

- Use `POST` for endpoints that receive a personal identifier, even for lookups.
- Put the identifier in the JSON request body.

```kotlin
// Correct
post("/person/perioder") {
    val request: InternVedtakRequest = call.receive()  // contains personidentifikator
    ...
}

// Wrong
get("/person/{fodselsnummer}/perioder") { ... }
```

Non-personal identifiers (e.g. `sakId`) may be path parameters. Example:
`GET /api/intern/sak/{sakid}/detaljert`.

### Stability per prefix

**`/api/v1/*`**
- Add new fields as optional (nullable or default value). Never remove or rename fields.
- Breaking change: create `/api/v2/...` and keep the old route until consumers have migrated, or use expand-contract (add new alongside old, migrate consumers, then remove old) if you don't want to version it.
- Always align with the team before deciding whether a change is breaking.
- All request/response DTOs must live in `kontrakt` under `no.nav.aap.arenaoppslag.kontrakt.apiv1`.

**`/intern/*`** is frozen. Do not add routes. Use `/api/intern/` for new internal needs.
- All DTOs must live in `kontrakt` under `no.nav.aap.arenaoppslag.kontrakt.intern`.

**`/api/intern/*`** may change freely. Responses can be shaped to the frontend's needs.

## Layering

```
Route handler -> Service -> Repository -> DataSource (Oracle / H2)
```

- **Route handlers** (`ApiRoute.kt`, `InternRoute.kt`): deserialise request, call service,
  serialise response. No business logic.
- **Services** (`service/`): orchestrate repositories, own cache logic, map domain objects to
  response objects. No direct database calls.
- **Repositories** (`database/`): raw JDBC. See the `jdbc-repository` skill.

Avoid "god class" services. Build services that compose other services as building blocks
to produce rich composite objects.

## Two model layers

1. Internal domain objects in `app/.../modeller/`, used only inside the app.
2. Contract types in `kontrakt/.../kontrakt/` (external API) or route-specific response
   classes in `modeller/` (for `/api/intern/`).

Domain objects must **never** be returned directly from a route. Map them first.

## Mapping: `tilKontrakt()`

Put a `tilKontrakt()` method on the domain class. Use it for 1:1 mappings and richer responses.

```kotlin
data class Periode(val fraOgMedDato: LocalDate, val tilOgMedDato: LocalDate?) {
    fun tilKontrakt(): no.nav.aap.arenaoppslag.kontrakt.modeller.Periode =
        no.nav.aap.arenaoppslag.kontrakt.modeller.Periode(
            fraOgMedDato = fraOgMedDato,
            tilOgMedDato = tilOgMedDato,
        )
}
```

In route handlers call `tilKontrakt()` and respond with the contract type.

## Checklist

1. **Route**: add `fun Route.mittEndepunkt(...)` in `ApiRoute.kt` or a new file. Wire it in `App.kt` under the correct prefix.
2. **Service**: add or extend a class in `service/`.
3. **Repository**: add or extend a class in `database/` (see `jdbc-repository`).
4. **Contract**: if consumed externally, add request/response types in `kontrakt/` (see `kontrakt-change`).
5. **Tests**: repository test (`H2TestBase`), service unit test (MockK), API integration test (see `write-tests`).
6. Never add routes under `/intern/`.
7. Run `./gradlew :app:test`.
