---
name: jdbc-repository
description: Use when writing or changing SQL queries or repository classes in aap-arenaoppslag - raw JDBC against Oracle, ResultSet.map, IN-lists with fødselsnummer, query timeout and read-only datasource conventions.
---

# JDBC repositories and queries

Repositories live in `database/`. They use raw JDBC with `PreparedStatement` and return domain objects.
No ORM, no abstraction over JDBC. The service is **read-only**: never write to the database.

## Conventions

- Annotate SQL with `@Language("OracleSql")` (most files use this spelling; `PersonRepository` uses `"OracleSQL"`).
- Use the standard pattern:
  `dataSource.connection.use { con -> con.createParameterizedQuery(sql).use { ... } }`
- Map rows with the `ResultSet.map { }` extension in `ArenaDatasource.kt`.
- Query timeout is 300 seconds, set in `createParameterizedQuery`.
- The datasource is read-only (`isReadOnly = true`) and `isAutoCommit = true` (performance optimisation).
- Use bind parameters for every value that is not a list. Never concatenate user input into SQL.

## IN-lists

Oracle does not support list parameters in `PreparedStatement`. For `IN (...)` over a list of values,
interpolate them into the query string.

- Fødselsnumre: the `queryMedFodselsnummerListe` pattern in `PersonRepository`. It replaces the
  `?:fodselsnummer` token and wraps each value in single quotes **without escaping**, so only pass
  validated fødselsnumre.
- Numeric IDs: `MeldekortperiodeRepository.perioderForMeldekortSql` joins `Long` values from the database
  directly (no injection risk since they are `Long`).
- Oracle allows at most 1000 elements in an `IN` list, so split large lists with `chunked(999)`.
- Never interpolate raw request strings.

## Comments

Write comments in Norwegian and explain **why**, e.g.
`// Oracle støtter ikke listeparametere i PreparedStatement, så vi interpolerer direkte`.

## Testing

Every repository change needs a repository test extending `H2TestBase` with the right Flyway
location. See the `write-tests` skill. H2 runs in Oracle-compatibility mode, so check that
new SQL functions work in both Oracle and H2.
