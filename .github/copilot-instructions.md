# GitHub Copilot instructions

The authoritative AI agent instructions for this repository are in [`AGENTS.md`](../AGENTS.md) at the repository root.

Read that file first. It covers:
- Project overview and technology stack
- Module structure (`app/` and `kontrakt/`)
- Language and naming (Norwegian, comment rules)
- Route layout and which prefixes are frozen
- Architecture conventions (layering, two model layers, JDBC pattern, caching rules)
- Testing conventions (H2TestBase, Flyway migration sets, integration test wiring, known test persons)
- Checklist for adding a new endpoint
- What NOT to do

For scope-specific additions that apply only to a particular task or sub-area,
see [`docs/ai/agent-guidelines-template.md`](../docs/ai/agent-guidelines-template.md).
