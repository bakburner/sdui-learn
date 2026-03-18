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
| Kitchen sink live response | `GET http://localhost:8080/sdui/demos` with `X-Platform: android` (requires running server) |

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

### Step 6 — Refresh kitchen sink appendix from live server

The JSON in `docs/appendix-kitchen-sink.md` must match the actual server output. This step starts the server, fetches the demo response, and replaces the JSON block in-place.

#### 6a — Start the server

```bash
cd server && ./gradlew bootRun
```

Run this in the background (`block_until_ms: 0`). It is a long-running process.

#### 6b — Wait for the server to be healthy

Poll until the server responds:

```bash
until curl -sf http://localhost:8080/sdui/demos > /dev/null 2>&1; do sleep 2; done
echo "Server ready"
```

If the server doesn't become healthy within 60 seconds, check the terminal output for errors.

#### 6c — Fetch the demo response

```bash
curl -s -H "X-Platform: android" http://localhost:8080/sdui/demos | python3 -m json.tool > /tmp/sdui-kitchen-sink.json
```

Verify the response is valid JSON and contains the expected top-level keys (`id`, `schemaVersion`, `sections`):

```bash
python3 -c "
import json
data = json.load(open('/tmp/sdui-kitchen-sink.json'))
assert 'sections' in data, 'Missing sections key'
print(f'OK: {len(data[\"sections\"])} sections')
"
```

#### 6d — Replace the JSON block in the appendix

Read the fetched JSON from `/tmp/sdui-kitchen-sink.json` and replace the entire content between the ` ```json ` fence and the closing ` ``` ` in `docs/appendix-kitchen-sink.md`.

**Important**: The markdown header is lines 1–9; the ` ```json ` fence is line 10. Replace only the JSON body (line 11 through the line before the closing ` ``` `) and preserve the closing fence. Then update the `> **Generated**:` line (line 5) with today's date.

#### 6e — Stop the server

Kill the `bootRun` process using the PID from the terminal header:

```bash
kill <pid>
```

### Step 7 — Validate

- Confirm all JSON files are valid (`python3 -c "import json; json.load(open('file'))"`)
- Confirm no dangling `$ref` in schema files
- Confirm the kitchen sink JSON in the appendix is valid:
  ```bash
  python3 -c "
  import re, json
  md = open('docs/appendix-kitchen-sink.md').read()
  match = re.search(r'\`\`\`json\n(.*?)\n\`\`\`', md, re.DOTALL)
  assert match, 'No JSON block found'
  data = json.loads(match.group(1))
  print(f'Kitchen sink JSON valid: {len(data[\"sections\"])} sections')
  "
  ```
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
