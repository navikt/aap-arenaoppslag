---
name: write-tests
description: Use when writing or fixing tests in aap-arenaoppslag - H2TestBase repository tests, Flyway test data sets, MockK service tests, Ktor testApplication integration tests with Fakes, AzureTokenGen and FakePdlGateway.
---

# Writing tests

## Test types

| Type | Base class / setup | Covers |
|---|---|---|
| Repository tests | Extend `H2TestBase` | SQL against in-memory H2 |
| Service unit tests | MockK | Service logic, no DB |
| Integration/API tests | Ktor `testApplication` + H2 | Full HTTP stack |

## H2TestBase

Creates one isolated in-memory H2 database per test class (not per test).
Always specify the Flyway locations you need:

```kotlin
class MinRepoTest : H2TestBase("flyway/minimumtest") { ... }
```

All sets automatically include `flyway/common`.

| Location | Contents |
|---|---|
| `flyway/common` | Schema DDL, constants, base data (always included) |
| `flyway/minimumtest` | Standard data with one known person (`12312312312`) and vedtak |
| `flyway/eksisterer` | Historikk-focused data |
| `flyway/telleverk` | Telleverk (`BEREGNINGSLEDD`) data |
| `flyway/dsop` | DSOP-specific schema |
| `flyway/saklistetest` | Multiple saker for person lookups |
| `flyway/maksimum` | Maksimum/utbetaling data |
| `flyway/oppgave` | Oppgave data (`V_OPPGAVE`) |
| `flyway/meldekort` | Meldekort with `MELDEGRUPPEKODE` (ATTF vs. other) for startuke lookups |

## Integration tests

Use `Fakes` (embedded Texas JWKS/token server) and `TestConfig.default(fakes)` for `AppConfig`.
Wire `AzureTokenGen` to produce tokens with the right issuer and client ID.

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

- Use `ArenaOppslagGateway` as the HTTP client wrapper.
- Use `FakePdlGateway` (implements `IPdlGateway`) to stub PDL. It echoes the input identifier as the only FOLKEREGISTERIDENT.

## Known test persons

- `"12312312312"`: in standard `minimumtest` data, the "known person".
- `"007"`: the "unknown person", exists in no dataset.

## Finish

Run `./gradlew :app:test` and fix all failures.
