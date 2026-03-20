# Agent guidelines — template for subdirectory AGENTS.md files

Copy this file as `AGENTS.md` into any subdirectory where you want to give AI agents
specific instructions that apply when they work in that area of the codebase.

The root `AGENTS.md` always applies. A subdirectory `AGENTS.md` only adds to it —
it never overrides the root rules.

## How to use

1. Copy this file to the target directory and rename it `AGENTS.md`.
   Example: `app/src/main/kotlin/no/nav/aap/arenaoppslag/database/AGENTS.md`
2. Replace the placeholder text in each section with real instructions.
3. Delete any section that has nothing to add — fewer words cost fewer tokens.
4. Delete this "How to use" section from the copy.

---

# AGENTS.md — [folder name]

> Supplements the root `AGENTS.md`. Rules here apply whenever an agent reads or
> modifies files in this directory.

## Purpose of this directory

<!-- What lives here and why. One short paragraph. -->

Example:
> This directory contains all JDBC repository classes. Each class is responsible for
> querying one logical area of the Arena database and returning domain objects to the
> service layer.

## Conventions specific to this directory

<!-- Patterns, rules, or gotchas that are local to this directory and not already
     covered in the root AGENTS.md. -->

Example:
> - Every repository class takes a `DataSource` as its only constructor parameter.
> - Public query methods delegate to private companion object functions that take a
>   `Connection` — this makes the SQL testable without a full DataSource.
> - SQL strings must be annotated with `@Language("OracleSQL")`.
> - Never use `connection.prepareStatement` directly for IN-list queries — use the
>   `queryMedFodselsnummerListe` pattern to interpolate the list into the SQL string,
>   because Oracle does not support list parameters in `PreparedStatement`.

## What to avoid here

<!-- Mistakes or temptations that are especially relevant in this directory. -->

Example:
> - Do not add write queries — this service is strictly read-only.
> - Do not introduce an abstraction layer over raw JDBC (no ORM, no query builder).
> - Do not call other repositories from within a repository — that belongs in the service layer.

## Relevant test setup

<!-- Point to the test counterpart and any non-obvious test setup needed. -->

Example:
> - Repository tests live in `app/src/test/kotlin/.../database/`.
> - Extend `H2TestBase` and pick the Flyway migration set that seeds the data you need.
> - Use `"12312312312"` as the known test person and `"007"` as the unknown one.
