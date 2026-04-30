---
name: doc-consistency-audit
description: 'Audit and sync SDUI documentation against the current code state. Use when: reviewing docs for consistency, deduplicating documentation, updating section counts, syncing terminology between code and docs, checking tier classifications, verifying renderer counts, cross-referencing schema enums against doc lists.'
---

# Documentation Consistency Audit

Sync all SDUI governance documentation against the schema and code as the single source of truth. Fix terminology drift, stale counts, dead references, and duplicated content.

## When to Use

- After schema changes (added/removed section types, action types, atomic elements)
- After migrating section types to/from atomic
- After renaming action types or other schema enums
- After implementing features that close gaps (request transport, i18n, experimentation, etc.)
- After ADR status changes (Proposed → Accepted)
- When the working tree contains uncommitted changes that introduce new capabilities or patterns
- After any multi-file implementation work (new infrastructure, new execution models, new helpers)
- Periodic consistency review ("review the docs", "sync docs", "dedup docs")
- After server composition changes (the kitchen sink appendix JSON is refreshed from the live server)

## Source of Truth (code reads first)

Extract current facts from code before auditing docs. These files define reality:

| Fact | Source File |
|------|------------|
| Section type enum | `schema/sdui-schema.json` → `definitions.Section.properties.type.enum` |
| Action type enum | `schema/sdui-schema.json` → `definitions.ActionType.enum` |
| Atomic element type enum | `schema/sdui-schema.json` → `definitions.AtomicElement.properties.type.enum` |
| All `$ref` types in `data.oneOf` | `schema/sdui-schema.json` → `definitions.Section.properties.data.oneOf` |
| Codegen type refs | `schema/sdui-all-types.json` → `properties` keys |
| Android routed sections | `android/sdui-core/.../renderer/SectionRouter.kt` → when block |
| Web routed sections | `web/src/components/SectionRouter.tsx` → switch cases |
| Android routed atomics | `android/sdui-core/.../renderer/atomic/AtomicRouter.kt` → when block |
| Web routed atomics | `web/src/components/atomic/AtomicRouter.tsx` → switch cases |
| Migrated types (server-composed) | `server/.../AtomicCompositeBuilder.java` → public `build*` methods |
| Action handler types | `android/sdui-core/.../state/ActionHandler.kt` + `web/src/runtime/ActionHandler.ts` |
| ADR statuses | `docs/adr/*.md` → `Status:` line in YAML/header (Proposed / Accepted / Superseded) |
| Request envelope implementation | `server/.../SduiRequestContext.java` + `android/.../RequestEnvelopeBuilder.kt` + `web/src/request/RequestEnvelopeBuilder.ts` |
| i18n implementation | `server/.../SduiUtils.java` → `stampStringTableOnSections` + `schema/sdui-schema.json` → `stringTable` on Section |
| Experiment implementation | `server/.../SduiCompositionService.java` → experiment resolution + `docs/adr/006-experiment-assignment-model.md` status |
| Recent commits (feature status) | `git log --oneline -10` — scan for feat/fix commits that close gaps |
| Working tree changes (new facts) | `git status --short` + `git diff HEAD` — uncommitted capabilities, patterns, new requirements |
| Kitchen sink live response | `GET http://localhost:8080/sdui/demos` with `X-Platform: android` (requires running server) |

## Documents to Audit

Audit these files in order (highest-visibility first):

1. `AGENTS.md` — tier classification, section/atomic counts, architecture summary, development rules
2. `README.md` — renderer counts, recent changes, migration status
3. `docs/client-implementors-contract.md` — build checklist, pseudocode algorithms, section/atomic type lists, conformance checklist
4. `docs/SDUI_Executive_Summary_v2.md` — renderer counts, atomic layer description, feature table
5. `docs/SDUI_Technical_Proposal_v2.md` — tier classification, migration status, architecture details
6. `docs/sdui-requirements-summary.md` — section inventory table, renderer counts, atomic layer row
7. `docs/sdui-design-system` — for accuracy, model , completeness
8. `docs/adr/*.md` — architecture decision records (check for stale type names, deprecated terminology)
9. `prompts/agents/client-builder.agent.md` — platform adaptation table, type inventory, conformance rules
10. `docs/glossary.md` — remove deprecated terminology. add new architectural pattern vocabulary

Do not interleave notes about older implementations that have been updated. Only leave history in the revision history. otherwise focus on the current state and only put future plans in existing future enhancements sections.

**Out of scope:** `prompts/agency-agents/` (unrelated to SDUI architecture).

## Procedure

### Step 1 — Extract code facts

Run the extraction script from the project root:

```bash
python3 prompts/skills/doc-consistency-audit/extract-facts.py
```

This prints section types, action types, atomic element types, data oneOf refs, all definitions, sdui-all-types.json keys, and checks for dangling refs.

Then read the router files to record:
- Which types the Android/Web SectionRouters route
- Which types the Android/Web AtomicRouters route
- What the AtomicCompositeBuilder builds (migrated types)

### Step 1b — Extract working tree changes

The working tree often contains new capabilities, patterns, or architectural decisions that docs don't yet reflect. This step captures them **before** the doc audit so new information propagates forward.

1. **Staged + unstaged changes** — run `git diff --stat HEAD` and `git diff --cached --stat` (or `git status --short`) to identify all modified/added files.
2. **Classify each changed file** by domain:
   - Schema changes → new types, fields, enums that need documenting
   - Server composition changes → new composers, new patterns, new endpoints
   - Client renderer/infrastructure changes → new capabilities, fixed gaps, new patterns
   - Runtime/action changes → new action semantics, new execution models
   - Config/build changes → new tooling, new makefile targets
3. **Read the diffs** for substantive files — `git diff HEAD -- <file>` for each file with meaningful changes (skip formatting-only or generated output). Summarize:
   - What new capability or pattern was introduced?
   - Does it close a previously documented gap?
   - Does it introduce a new requirement, constraint, or architectural pattern?
   - Does it change the execution model or contract for an existing feature?
4. **Build a "new facts" list** — for each substantive change, record:
   - The fact (e.g. "onActivate now executes all matching actions as a batch sequence with failure policy")
   - Which documents should reflect it (requirements summary, technical proposal, executive summary, contract, etc.)
   - Whether it closes a gap, adds a new requirement, or modifies an existing one

This new-facts list feeds into Steps 3–5: when auditing each document, check whether the new facts are present. If not, add them.

### Step 1c — Extract feature status facts

The extraction script covers schema structure. Feature status requires additional checks:

1. **ADR statuses** — read the `Status:` line from each ADR in `docs/adr/`. Record which are Proposed vs. Accepted.
2. **Recent git history** — run `git log --oneline -10` and scan for `feat:` commits that implement capabilities (request transport, experimentation, i18n, etc.).
3. **Key implementation files** — check whether these exist and contain substantive code:
   - `server/.../SduiRequestContext.java` (request envelope)
   - `android/.../RequestEnvelopeBuilder.kt` + `web/src/request/RequestEnvelopeBuilder.ts` (client envelope builders)
   - `server/.../SduiUtils.java` → `stampStringTableOnSections` (i18n)
   - `schema/sdui-schema.json` → `stringTable` on Section definition (i18n schema)
   - ADR-006 status (experiment model)
4. **Build the status truth table** — for each requirement in the status matrices, determine the ground-truth status (Gap / Partial / Built) based on the code evidence above.

This truth table is then compared against every status matrix in the audited documents.

### Step 2 — Cross-reference routers against schema

Verify:
- Every schema section type has a case in both SectionRouters
- Every schema atomic type has a case in both AtomicRouters
- No router handles a type NOT in the schema
- `sdui-all-types.json` refs all resolve to existing schema definitions (no dangling `$ref`)

### Step 3 — Audit each document

For each doc in the audit list, check the [consistency checklist](./references/consistency-checklist.md).

Additionally, for each document, check the **new-facts list** from Step 1b:
- If a new fact belongs in this document (per the classification in Step 1b), verify it is present.
- If absent, mark it as an inconsistency to fix — new capabilities, requirements, and architectural patterns must be documented where they apply.
- For requirements documents (`sdui-requirements-summary.md`): add new requirements or update existing ones, update status matrices, and add to the relevant subsections.
- For the technical proposal: update architecture descriptions, feature tables, and status sections.
- For the executive summary: update feature tables and capability descriptions.
- For the client contract: update pseudocode, algorithms, and conformance checklists if the working-tree changes alter the client contract.
- For `AGENTS.md`: update semantic section inventories, rule clarifications, or operational rules if the working-tree changes affect governance.

### Step 4 — Report inconsistencies

**Before making any edits**, present a numbered table of every inconsistency found:

```markdown
| # | File | Line | Problem | Fix |
|---|------|------|---------|-----|
| 1 | README.md | ~127 | "10 atomic element types" | → "12 atomic element types (`OverlayContainer` added)" |
| 2 | ... | ... | ... | ... |
```

Include:
- The **file path** and approximate line number
- The **problematic text** (quoted)
- What it **should say** (the fix)

This gives the user visibility into what will change before edits are applied.

### Step 5 — Apply fixes

After presenting the report, apply all fixes. Group edits per file for efficiency.

### Step 5a — Update revision tables

For every audited document that was **modified** and has a `## Revision History` table, add a new row summarizing today's changes. The entry should list the specific fixes applied (e.g., terminology corrections, status updates, count changes). Do not add revision entries to documents that were not modified — that is unnecessary noise.

### Step 6 — Validate

- Confirm all JSON files are valid (`python3 -c "import json; json.load(open('file'))"`)
- Confirm no dangling `$ref` in schema files
- Run a final grep for any remaining stale type names across the project

> **Kitchen sink refresh** is out of scope for this skill — the `docs/appendix-kitchen-sink.md`

## Key Terminology Rules

These terms have specific meanings — do not interchange:

| Correct | Incorrect alternatives to catch |
|---------|--------------------------------|
| `fireAndForget` | `analytics`, `fire-and-forget`, `beacon` |
| `AtomicComposite` | `Composite`, `atomic composite` (always PascalCase) |
| `SectionSlot` | `section slot`, `slot` (always PascalCase when referring to the type) |
