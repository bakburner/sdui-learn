---
name: requirements-to-plan
description: 'Generate implementation plans from updated requirement and proposal documents. Use when: docs have been updated with new requirements, technical proposals changed, gap sections added, requirement status matrix updated, or new ADRs accepted. Inverse of doc-consistency-audit — this reads doc changes and produces actionable plan files.'
---

# Requirements → Implementation Plan Generator

Review updated requirement documents, technical proposals, and ADRs to identify new or changed requirements, then generate focused implementation plan `.md` files in `docs/` for each workstream.

## When to Use

- After updating `docs/sdui-requirements-summary.md` (new sections, revised requirements, status changes)
- After updating `docs/SDUI_Technical_Proposal_v2.md` (new architecture sections, revised design)
- After adding or accepting new ADRs in `docs/adr/`
- After updating `docs/SDUI_Executive_Summary_v2.md` with new feature areas
- When asked to "plan the new requirements", "create plans from doc updates", "what changed and what do we need to build"

## Input Documents (read these for changes)

| Document | What to look for |
|----------|-----------------|
| `docs/sdui-requirements-summary.md` | New `§9x` gap sections, status matrix changes (Gap/Partial → needs plan), revised requirement text |
| `docs/SDUI_Technical_Proposal_v2.md` | New architecture sections, revised technical approaches, new sequence diagrams |
| `docs/SDUI_Executive_Summary_v2.md` | New feature rows, changed scope or timeline signals || `docs/client-implementors-contract.md` | New algorithm specs, updated build checklist phases, revised conformance requirements, new runtime contracts || `docs/adr/*.md` | Newly accepted ADRs that imply implementation work |
| `AGENTS.md` | Tier classification changes, new key rules, architecture shifts |

## Output

One plan file per workstream, written to `docs/` with the naming convention:

```
docs/plan-<topic-slug>.md
```

Examples: `plan-accessibility.md`, `plan-offline-caching.md`, `plan-schema-versioning.md`

If a plan file already exists for a topic, update it rather than creating a duplicate.

## Procedure

### Step 1 — Identify what changed

Use `git diff` or read the revision history table in `docs/sdui-requirements-summary.md` to find:
- New requirement sections (§9x gap sections)
- Requirements whose status changed in the status matrix (§10)
- New ADRs that were accepted since the last plan generation
- Net-new text in the technical proposal or executive summary

List every changed requirement with its current status (Gap, Partial, Designed, Built).

### Step 2 — Group into workstreams

Cluster related requirements into workstreams. A workstream is a coherent unit of work that can be planned and executed together. Examples:
- "Accessibility" (schema fields + live regions + screen reader support)
- "Offline & Caching" (Room cache + stale-while-revalidate + cold start)
- "Schema Versioning" (version negotiation + multi-version routing + deprecation)

One requirement may appear in multiple workstreams if it's cross-cutting.

### Step 3 — For each workstream, generate a plan file

Use the [plan template](./references/plan-template.md) structure. For each plan:

1. **Summarize the requirement** — quote or reference the specific sections from the source docs
2. **Assess current state** — what's already built (from status matrix), what's gap
3. **Break into tasks** — concrete, actionable implementation tasks scoped to specific layers:
   - Schema changes (if any)
   - Server changes (composers, controllers, models)
   - Android changes (renderers, state, cache)
   - Web changes (components, hooks, state)
   - Documentation updates
   - Test requirements
4. **Identify dependencies** — what must be done first, what can be parallelized
5. **Flag open questions** — anything that needs a decision before implementation

### Step 4 — Cross-reference against existing plans

Check `docs/plan-*.md` for existing plans. If a plan already covers a workstream:
- Update the existing plan with new requirements
- Add new tasks to the existing task list
- Update the "Current State" section

### Step 5 — Validate completeness

Verify:
- Every Gap/Partial requirement from the status matrix has at least one plan covering it
- Every newly accepted ADR has its implementation work captured in a plan
- No plan references types, patterns, or terminology that conflicts with the schema source of truth
- Plans use correct terminology (see doc-consistency-audit skill for terminology rules)
- Any new runtime contracts or algorithms in `docs/client-implementors-contract.md` that imply implementation work are captured in platform-specific plans
- Plans that affect client architecture are consistent with the build phases and conformance checklist in the contract doc

## Architecture Context (for accurate planning)

### Current Type Inventory
- **9 section types** in schema: TabGroup, GamePanel, BoxscoreTable, Form, AdSlot, SeasonLeadersTable, SubscribeBanner, SubscribeHero, AtomicComposite
- **10 atomic element types**: Container, Text, Image, Button, Spacer, Divider, ScrollContainer, Conditional, DisplayGrid, SectionSlot
- **6 action types**: navigate, fireAndForget, mutate, refresh, dismiss, toast
- **9 migrated types** (server-composed AtomicComposite, no client renderers): ErrorState, SectionHeader, PromoBanner, ContentRail, FollowingRail, HeroPanel, StatLine, VideoCarousel, NbaTvSchedule
- **8 permanent sections** (client renderers with owned state): BoxscoreTable, SeasonLeadersTable, Form, TabGroup, GamePanel, SubscribeHero, SubscribeBanner, AdSlot

### Decision Checklist (apply when planning tasks)
1. Can this be solved by server composition only?
2. Can schema/action payload changes suffice?
3. Can it be a server-composed `AtomicComposite` instead of a new section?
4. If client code is needed, can it go in shared infra rather than a specific renderer?

### Tech Stack (for scoping tasks)
- **Server**: Spring Boot 3.2, Java 21, Jackson 2.17, Ably 1.2
- **Android**: Kotlin 2.1, Compose BOM 2024.12, Room 2.6, Coil 2.7
- **Web**: React 18.2, TypeScript 5.9, Vite 5.0, Ably 2.17
- **Schema**: JSON Schema → jsonschema2pojo codegen → Java, TypeScript, Swift outputs
