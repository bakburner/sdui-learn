# SDUI Prototype — Claude CLI Configuration

## Project Rules

Read and follow all development rules in `AGENTS.md`.

## Skills

When asked to audit docs or sync documentation, read and follow:
- `prompts/skills/doc-consistency-audit/SKILL.md`

When asked to generate implementation plans from requirements, read and follow:
- `prompts/skills/requirements-to-plan/SKILL.md`

## Agents

Specialized agent personas are available in `prompts/agents/`:
- `backend-architect.agent.md` — Spring Boot server composition
- `frontend-developer.agent.md` — React/TypeScript web client
- `mobile-app-builder.agent.md` — Kotlin/Compose Android client
- `senior-developer.agent.md` — Full-stack cross-cutting concerns
- `client-builder.agent.md` — Platform-agnostic client implementation guide

When asked to build a new SDUI client for any platform (iOS, Flutter, desktop, TV, etc.),
use the `client-builder` agent with `docs/client-implementors-contract.md` as the
primary reference.
