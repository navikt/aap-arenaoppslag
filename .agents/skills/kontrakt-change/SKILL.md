---
name: kontrakt-change
description: Use when adding or changing request/response types in the kontrakt module of aap-arenaoppslag, which is published to GitHub Packages for external consumers - backwards compatibility, packages and release versioning.
---

# Changing the `kontrakt` module

`kontrakt/` is published as a Maven package to GitHub Packages. External consumers (e.g. `aap-api-intern`, Kelvin) depend on it.

## Packages (`kontrakt/src/main/kotlin/no/nav/aap/arenaoppslag/kontrakt/`)

- `apiv1`: DTOs for `/api/v1/*`
- `intern`: DTOs for `/intern/*` (frozen)
- `migrering`: DTOs for `/api/migrering/*`
- `modeller`: shared types (`Periode`, `Maksimum`)

`/api/intern/*` responses live in `app/.../modeller/` instead and are not part of the contract.

## Rules

- Add new fields as optional (nullable or with a default value).
- Do not remove or rename existing fields or change their types.
- For breaking changes in `/api/v1`: either create a `v2` type and route and keep the old one until consumers have migrated, or use expand-contract if you don't want to version it.
- Expand-contract (allowed for both `/intern` and `/api/v1`): add the new field or type alongside the old one, migrate consumers, then remove the old one.
- Always align with the team before deciding whether a change is breaking.
- Notify consumers of breaking changes.
- Map domain objects with `tilKontrakt()` (see `add-endpoint`).

## Versioning and release

Versions are not bumped manually in a file. `.github/workflows/release.yml` uses
`paulhatch/semantic-version` with `change_path: "kontrakt/"` and `bump_each_commit: true`,
then publishes with `./gradlew -Pversion=<tag> publish`.

- Any change under `kontrakt/` that is merged produces a new version.
- For a major (breaking) bump, include `(MAJOR)` in the commit message.

## Checklist

1. Make the change backwards-compatible.
2. Check JSON (de)serialisation of new fields with defaults.
3. If it is unavoidably breaking, agree with the team and include `(MAJOR)` in the commit message.
4. Run `./gradlew :app:test`.
5. Tell the user which consumers need to be notified.
