# AGENTS.md — AI agent instructions for aap-arenaoppslag

This is the authoritative source of rules for AI agents working in this repository.
`.github/copilot-instructions.md` points here. Scope-specific additions for a concrete task
go in a copy of `docs/ai/agent-guidelines-template.md`.

Subdirectories may contain their own `AGENTS.md` files with additional rules that apply
when working in that part of the codebase. Those files add to these rules — they do not
override them.

Detailed, task-specific guidance lives in skills under `.agents/skills/`:

| Skill | Use when |
|---|---|
| `add-endpoint` | Adding or changing endpoints: route prefixes, layering, `tilKontrakt()`, checklist |
| `jdbc-repository` | Writing SQL or repositories: raw JDBC, `map { }`, Oracle IN-lists |
| `write-tests` | Writing tests: `H2TestBase`, Flyway sets, integration test setup, test persons |
| `caching` | Adding or changing Caffeine caches and their Micrometer monitoring |
| `kontrakt-change` | Changing the published `kontrakt` module |
| `git-workflow` | Committing, pushing, branches or pull requests |

---

## Project overview

`aap-arenaoppslag` is a read-only SQL lookup service running in the NAV FSS zone,
querying the Arena Oracle database through dedicated views for AAP (Arbeidsavklaringspenger).
Built with Ktor/Kotlin, Azure AD JWT auth, Micrometer/Prometheus metrics, and a `kontrakt`
submodule published to GitHub Packages for external consumers.
It has two runtime environments: Prod and Dev.

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

## Core rules

- All routes except `/actuator/*` require Azure AD JWT authentication.
- Default prefix for new endpoints is `/api/v1/*`. Use `/api/intern/*` for frontend needs.
  **Never add routes under `/intern/*`** — it is frozen (see `add-endpoint`).
- **Personal identifiers** (`fødselsnummer`, `personident`, anything identifying a person)
  must **never** appear in a URL, neither path nor query. Use `POST` with the identifier in the JSON body.
- Layering: Route handler → Service → Repository → DataSource. No business logic in routes,
  no database calls in services.
- Domain objects must never be returned directly from a route. Map with `tilKontrakt()`.
- Follow the repository pattern. Avoid "god class" services; compose services from other services.
- Never add a cache without Micrometer monitoring (see `caching`).

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

Strict rules, detailed in the `git-workflow` skill:

- Never commit without the user's explicit approval of both the changes and the commit message.
- Never push. Tell the user the commit is ready and let them push.
- Never commit on `main`. If on `main`, give the user a loud warning and ask for a branch first.
- Do not create pull requests. Propose a title and description for the user instead.

---

## What NOT to do

- Do not expose personal identifiers in URLs — put them in the POST request body.
- Do not write to the database. This service is read-only.
- Do not use an ORM or any abstraction over JDBC.
- Do not skip Micrometer instrumentation when adding a new Caffeine cache.
- Do not break backwards compatibility in `kontrakt/` without bumping the published version and notifying consumers.
- Do not add new routes under `/intern/` — it is a legacy prefix.
- Do not generate code with English names for variables, functions, or classes — use Norwegian.
- Do not add comments that only describe what the code does — explain why.
