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
- Periodic consistency review ("review the docs", "sync docs", "dedup docs")

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

## Documents to Audit

Audit these files in order (highest-visibility first):

1. `AGENTS.md` — tier classification, section/atomic counts, architecture summary, development rules
2. `README.md` — renderer counts, recent changes, migration status
3. `docs/SDUI_Executive_Summary_v2.md` — renderer counts, atomic layer description, feature table
4. `docs/SDUI_Technical_Proposal_v2.md` — tier classification, migration status, architecture details
5. `docs/sdui-requirements-summary.md` — section inventory table, renderer counts, atomic layer row
6. `docs/appendix-kitchen-sink.md` — section type labels, demo section list
7. `docs/adr/*.md` — architecture decision records (check for stale type names, deprecated terminology)

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

### Step 2 — Cross-reference routers against schema

Verify:
- Every schema section type has a case in both SectionRouters
- Every schema atomic type has a case in both AtomicRouters
- No router handles a type NOT in the schema
- `sdui-all-types.json` refs all resolve to existing schema definitions (no dangling `$ref`)

### Step 3 — Audit each document

For each doc in the audit list, check the [consistency checklist](./references/consistency-checklist.md).

### Step 4 — Report inconsistencies

**Before making any edits**, present a numbered table of every inconsistency found:

```markdown
| # | File | Line | Problem | Fix |
|---|------|------|---------|-----|
| 1 | README.md | ~127 | "18 section types (17 semantic + AtomicComposite)" | → "9 section types (8 permanent + AtomicComposite)" |
| 2 | ... | ... | ... | ... |
```

Include:
- The **file path** and approximate line number
- The **problematic text** (quoted)
- What it **should say** (the fix)

This gives the user visibility into what will change before edits are applied.

### Step 5 — Apply fixes

After presenting the report, apply all fixes. Group edits per file for efficiency.

### Step 6 — Validate

- Confirm all JSON files are valid (`python3 -c "import json; json.load(open('file'))"`)
- Confirm no dangling `$ref` in schema files
- Run a final grep for any remaining stale type names across the project

## Key Terminology Rules

These terms have specific meanings — do not interchange:

| Correct | Incorrect alternatives to catch |
|---------|--------------------------------|
| `fireAndForget` | `analytics`, `fire-and-forget`, `beacon` |
| `AtomicComposite` | `Composite`, `atomic composite` (always PascalCase) |
| `SectionSlot` | `section slot`, `slot` (always PascalCase when referring to the type) |
| `Migrated to atomic` | `Tier 1`, `Tier 2` (these labels are retired) |
| `Permanent sections` | `Tier 3` (this label is retired) |
| `Container` (with flex/breakpoint) | `Row` (Row was removed; its function is now Container) |
