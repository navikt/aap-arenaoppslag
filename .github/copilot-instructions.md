# GitHub Copilot instructions

The authoritative AI agent instructions for this repository are in [`AGENTS.md`](../AGENTS.md) at the repository root.

Read that file first. It covers:
- Project overview and technology stack
- Build, test (including single tests) and lint commands
- Module structure (`app/` and `kontrakt/`)
- Language and naming (Norwegian, comment rules)
- Route layout and which prefixes are frozen
- Architecture conventions and core rules
- What NOT to do

Read the relevant skill for task-specific guidance:
- [Endpoints](../.agents/skills/add-endpoint/SKILL.md): route stability, model layers, `tilKontrakt()` and endpoint checklist
- [JDBC repositories](../.agents/skills/jdbc-repository/SKILL.md): query helpers, Oracle IN-lists and timeouts
- [Tests](../.agents/skills/write-tests/SKILL.md): H2TestBase, Flyway data sets, API test wiring and test persons
- [Caching](../.agents/skills/caching/SKILL.md): Caffeine configuration and mandatory Micrometer monitoring
- [Contract changes](../.agents/skills/kontrakt-change/SKILL.md): compatibility and release versioning
- [Git workflow](../.agents/skills/git-workflow/SKILL.md): approval, branch and publishing rules

For scope-specific additions that apply only to a particular task or sub-area,
see [`docs/ai/agent-guidelines-template.md`](../docs/ai/agent-guidelines-template.md).
