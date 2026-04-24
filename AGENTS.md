# AGENTS.md — AI agent instructions for aap-arenaoppslag

This is the authoritative source of rules for AI agents working in this repository.
`.github/copilot-instructions.md` points here. Scope-specific additions for a concrete task
go in a copy of `docs/ai/agent-guidelines-template.md`.

Subdirectories may contain their own `AGENTS.md` files with additional rules that apply
when working in that part of the codebase. Those files add to these rules — they do not
override them.

---

## Project overview

`aap-arenaoppslag` is a read-only SQL lookup service running in the NAV FSS zone,
querying the Arena Oracle database through dedicated views for AAP (Arbeidsavklaringspenger).
Built with Ktor/Kotlin, Azure AD JWT auth, Micrometer/Prometheus metrics, and a `kontrakt`
submodule published to GitHub Packages for external consumers.

---

## Technology stack

| Concern | Choice |
|---|---|
| Language | Kotlin 2.2, JVM 21 |
| Server framework | Ktor (Netty engine) |
| Database (prod) | Oracle via HikariCP |
| Database (tests) | H2 in Oracle-compatibility mode |
| Auth | Azure AD JWT |
| Metrics | Micrometer + Prometheus |
| Cache | Caffeine |
| Build | Gradle (Kotlin DSL), multi-module |
| Deployment | NAIS / Kubernetes on FSS cluster |

---

## Module structure

```
app/        Main application: server, routes, services, repositories, models
kontrakt/   Shared API contract types, published as a Maven package to GitHub Packages
```

---

## Language and naming

- **Use Norwegian** for variable, function, class, and property names — e.g.
  `hentPerioder`, `fodselsnummer`, `Periode`, `tilKontrakt`.
- **Use Norwegian** in code comments.
- **Avoid comments** where the code is self-explanatory. When a comment is necessary,
  it must explain **why** the code does what it does — not just what it does.
  Bad comment: `// setter fødselsnummer`. Good comment:
  `// Oracle støtter ikke listeparametere i PreparedStatement, så vi interpolerer direkte`.

---

## Route layout

All routes except `/actuator/*` require Azure AD JWT authentication.

| Prefix | Purpose                                                                                              | Rule                                                                                                                      |
|---|------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| `/actuator/*` | Health and metrics (unauthenticated)                                                                 | No business logic here                                                                                                    |
| `/intern/*` | Old API used by Kelvin and aap-api-intern. Avoid adding new endpoints here unless explicitly stated. | Breaking changes only allowed through expand-contract pattern. |
| `/api/v1/*` | External API — consumers depend on a stable contract. Default for new endpoints.                     | No breaking changes without versioning                                                                                    |
| `/api/intern/*` | Internal API for frontends — "backend for frontend"                                                  | Breaking changes are allowed                                                                                              |

### Personal identifiers in routes

A personal identifier (`fødselsnummer`, `personident`, or any other value that identifies
a natural person) must **never** appear in a URL — not as a path segment, not as a query parameter.

- Always use `POST` for endpoints that receive a personal identifier, even when the operation
  is logically a lookup that REST would model as `GET`.
- Put the identifier in the JSON request body.

```kotlin
// Correct — identifier in POST body
post("/person/perioder") {
    val request: InternVedtakRequest = call.receive()  // contains personidentifikator
    ...
}

// Wrong — identifier exposed in URL
get("/person/{fodselsnummer}/perioder") { ... }
```

Non-personal identifiers (e.g. `sakId`, which identifies a case rather than a person)
may appear in the URL as normal path parameters. See `GET /api/intern/sak/{sakid}/detaljert`
as the existing example of this pattern.

### Stability requirements per prefix

**`/api/v1/*`** is an external API. Consumers depend on a stable format.
- Add new fields as `optional` (nullable or with a default value) — do not remove or rename existing fields.
- For breaking changes: create `/api/v2/...` and keep the old route until consumers have migrated.
- Always align with the team before deciding whether a change is breaking.
- **All request and response DTO types must be defined in the `kontrakt` module** under `no.nav.aap.arenaoppslag.kontrakt.apiv1`.

**`/intern/*`** is frozen — do not add new routes here. Use `/api/intern/` for new internal needs.
These routes are currently used by `aap-api-intern`. The long-term plan is to migrate to `/api/v1/*`.
- **All request and response DTO types must be defined in the `kontrakt` module** under `no.nav.aap.arenaoppslag.kontrakt.intern`.

**`/api/intern/*`** is "backend for frontend" and may change freely.
- Response objects can be shaped to fit the frontend's exact needs and do not need to follow REST conventions.
- Breaking changes are allowed, but coordinate with consuming teams.

---

## Architecture conventions

### Layering

```
Route handler → Service → Repository → DataSource (Oracle / H2)
```

- **Route handlers** (`ApiRoute.kt`, `InternRoute.kt`): deserialise request, call service,
  serialise response. No business logic.
- **Services** (`service/`): orchestrate repositories, own cache logic, map domain objects to
  response objects. No direct database calls.
- **Repositories** (`database/`): raw JDBC with `PreparedStatement`. No ORM. SQL annotated with
  `@Language("OracleSQL")`. Return domain objects.

### Two model layers

1. **Internal domain objects** in `app/.../modeller/` — used inside the app only.
2. **Contract/response types** in `kontrakt/.../kontrakt/` (for the external API) or
   directly in `modeller/` as route-specific response classes (for `/api/intern/`).

Domain objects must **never** be returned directly from a route — map them to response objects first.

### Mapping from domain to response

Use one of two patterns — pick whichever is most readable in context:

**Pattern 1 — method on the domain class (`tilKontrakt()`):**

```kotlin
data class Periode(val fraOgMedDato: LocalDate, val tilOgMedDato: LocalDate?) {
    fun tilKontrakt(): no.nav.aap.arenaoppslag.kontrakt.modeller.Periode =
        no.nav.aap.arenaoppslag.kontrakt.modeller.Periode(
            fraOgMedDato = fraOgMedDato,
            tilOgMedDato = tilOgMedDato,
        )
}
```

Use this when one domain object maps to one contract/response object.

**Pattern 2 — `companion object { fun fromDomain(...) }` on the response class:**

```kotlin
data class ArenaSakDetaljertRespons(
    val sakId: String,
    // ...
) {
    companion object {
        fun fromDomain(sak: ArenaSakMedVedtak, telleverk: TellerverkPåPerson?) =
            ArenaSakDetaljertRespons(
                sakId = sak.sakId,
                // ...
            )
    }
}
```

Use this when the response is assembled from multiple domain objects at the route level.

In the route handler:

```kotlin
val respons = ArenaSakDetaljertRespons.fromDomain(sak, telleverk)
call.respond(HttpStatusCode.OK, respons)
```

### Database access pattern

- All queries use raw JDBC: `dataSource.connection.use { con -> con.createParameterizedQuery(sql).use { ... } }`.
- The `ResultSet.map { }` extension in `ArenaDatasource.kt` is the standard way to map rows.
- Oracle does not support list parameters in `PreparedStatement`. When a query needs `IN (...)`
  over a list of values, interpolate them directly into the query string using the
  `queryMedFodselsnummerListe` pattern — see `PersonRepository`.
- SQL query timeout: 300 seconds (set in `createParameterizedQuery`).
- The datasource is read-only (`isReadOnly = true`) and uses `isAutoCommit = true` as a performance optimisation.

### Caching

- `HistorikkService`: Caffeine cache for `personId` lookups (max 30 000 entries, no TTL — size-based eviction).
- `InternService`: Caffeine caches for `maksimum`, `saker`, `perioder`, and `perioder_11_17` results
  (max 10 000 entries each, 15-minute TTL, all monitored via Micrometer).
- `SakService`: Caffeine cache for `sakerPerPerson` (max 10 000 entries, no TTL — size-based eviction, monitored via Micrometer).
- `PdlGateway`: Caffeine cache for PDL identity lookups (max 10 000 entries, 15-minute TTL, monitored via Micrometer).
- Never add a cache without also wiring Micrometer monitoring (`CaffeineCacheMetrics.monitor(...)`).

---

## Testing conventions

### Test types

| Type | Base class / setup | What it covers |
|---|---|---|
| Repository tests | Extend `H2TestBase` | SQL queries against in-memory H2 |
| Service unit tests | Use MockK | Service logic, no DB |
| Integration/API tests | Ktor `testApplication` + H2 | Full HTTP stack end-to-end |

### H2TestBase

`H2TestBase` creates one isolated in-memory H2 database per test class (not per test).
Always specify the Flyway migration locations you need:

```kotlin
class MinRepoTest : H2TestBase("flyway/minimumtest") { ... }
```

Available Flyway migration sets (all automatically include `flyway/common`):

| Location | Contents |
|---|---|
| `flyway/common` | Schema DDL, constants, base data (always included) |
| `flyway/minimumtest` | Standard test data with one known person (`12312312312`) and vedtak |
| `flyway/eksisterer` | Historikk-focused test data |
| `flyway/telleverk` | Telleverk (`BEREGNINGSLEDD`) test data |
| `flyway/dsop` | DSOP-specific schema |
| `flyway/saklistetest` | Sak-list test data (multiple saker for person lookups) |
| `flyway/maksimum` | Maksimum/utbetaling test data |

### Integration tests

Use `Fakes` for a lightweight embedded Azure JWKS/token server. Use `TestConfig.default(fakes)` for `AppConfig`.
Wire `AzureTokenGen` to produce tokens with the correct issuer and client ID.

```kotlin
private fun withTestServer(testBlokk: suspend (ArenaOppslagGateway) -> Unit) {
    val config = TestConfig.default(Fakes())
    val tokenProvider = AzureTokenGen(config.azure.issuer, config.azure.clientId)
    testApplication {
        application { server(config, h2, FakePdlGateway()) }
        val gateway = ArenaOppslagGateway(tokenProvider, jsonHttpClient)
        testBlokk(gateway)
    }
}
```

Use `ArenaOppslagGateway` as the HTTP client wrapper in integration tests.
Use `FakePdlGateway` (implements `IPdlGateway`) to stub PDL lookups — it echoes
back the input identifier as the only FOLKEREGISTERIDENT.

### Known test persons

- `"12312312312"` — present in standard `minimumtest` data, used as "known person" in tests.
- `"007"` — used as "unknown person" (does not exist in any test dataset).

---

## After making code changes

After completing any code change, always run the full test suite and fix all failures before
presenting the work as done:

```
./gradlew :app:test
```

This applies to every change — including small, seemingly safe edits — because adding or
renaming fields often breaks existing test assertions or causes compilation errors in tests.
Do not consider a task finished until the build and all tests pass.

---

## Git workflow

Before committing, always:

1. Show the user a summary of the changes to be committed.
2. Propose a commit message and wait for explicit approval before proceeding.
3. Do not commit until the user has confirmed both the changes and the message.

Before pushing, confirm with the user that the commit(s) are ready to be pushed.

Before creating a pull request, always:

1. Propose a PR title and description and present them to the user.
2. Wait for explicit approval — including any requested edits — before creating it.

---

## Patterns to follow and to avoid
Follow the repository pattern. 
Avoid creating the "god class" anti-pattern , for example when creating services. Instead, create a service that uses other services as building blocks to achieve a service that can create rich composite objects. 

## Checklist for adding a new endpoint

1. **Route**: add a new `fun Route.mittEndepunkt(...)` function in `ApiRoute.kt` or a new file.
   Wire it in `App.kt` under the correct route prefix.
2. **Service**: add or extend a service class in `service/`. Simple logic can stay in an existing service.
3. **Repository**: add or extend a repository class in `database/`. Use raw JDBC and the `map { }` extension.
4. **Contract**: if the endpoint must be consumed by external services, add request/response types
   in `kontrakt/`. Keep them backwards-compatible.
5. **Tests**: add a repository test (in `H2TestBase`), a service unit test (MockK), and an API integration test.
6. **Do not add new routes under `/intern/`** — that prefix is frozen. Use `/api/v1/` or `/api/intern/`.

---

## What NOT to do

- Do not expose personal identifiers (`fødselsnummer`, `personident`, etc.) in URLs — put them in the POST request body.
- Do not write to the database. This service is read-only.
- Do not use an ORM or any abstraction over JDBC.
- Do not skip Micrometer instrumentation when adding a new Caffeine cache.
- Do not break backwards compatibility in `kontrakt/` without bumping the published version and notifying consumers.
- Do not add new routes under `/intern/` — it is a legacy prefix.
- Do not generate code with English names for variables, functions, or classes — use Norwegian.
- Do not add comments that only describe what the code does — explain why.
