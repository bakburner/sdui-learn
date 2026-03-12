# ADR-008: Form-Factor Layout Manager

- Status: Accepted (Option C — Hybrid)
- Date: 2026-02-20
- Accepted: 2026-03-11
- Decision owners: Adrian Robinson (interim), platform leads, backend leads

## Decision

**Option C (Hybrid) adopted.** Structural layout uses semantic section types (`Row`, future `Grid`/`SplitPane`) — already validated in the prototype. Inter-section fine-tuning uses `layoutHints` on Section (margins, dividers, priority). This gives the server control over the most impactful layout knobs without over-engineering a full layout system.

Schema additions: `SectionLayoutHints` definition with `marginTop`, `marginBottom`, `dividerAbove`, `dividerBelow`, `priority`. Added as optional `layoutHints` field on `Section`.

## Context

The same semantic screen must support phone, tablet, web, and TV without duplicating composition logic or over-constraining client rendering systems. Three approaches were evaluated.

## Options Evaluated

### Option A: Layout Hints on Sections

Server attaches optional hint metadata to existing section types. Clients interpret hints using platform-native layout systems. Unknown hints are ignored.

```json
{
  "id": "content-rail-001",
  "type": "ContentRail",
  "layoutHints": {
    "phone": { "columns": 1 },
    "tablet": { "columns": 2 },
    "tv": { "columns": 3 }
  },
  "data": { "title": "More Games Tonight", "items": [ "..." ] }
}
```

- Server suggests layout; client decides final rendering
- No new section types needed
- Risk: hints are loosely typed and may diverge in interpretation across platforms
- Risk: no compositional structure — hints can't express relationships between sections (e.g., side-by-side)

### Option B: Semantic Layout Section Types

Server composes layout structurally using dedicated layout section types (`Row`, `Grid`, `SplitPane`) that contain child sections. Clients implement each layout type as a renderer.

```json
{
  "id": "team-leaders-row",
  "type": "Row",
  "data": {
    "spacing": 16,
    "breakpoint": 600,
    "children": [
      { "id": "home-leaders", "type": "StatLine", "data": { "title": "BOS Leaders", "stats": ["..."] } },
      { "id": "away-leaders", "type": "StatLine", "data": { "title": "MIA Leaders", "stats": ["..."] } }
    ]
  }
}
```

- Server controls composition and spatial arrangement — consistent with SDUI philosophy
- Already validated in prototype (`Row` section type with `children`, `spacing`, `breakpoint`)
- Layout types are thin renderers (~30 lines), same as content types
- Risk: server must know enough about form factors to compose correctly
- Risk: new layout types require renderer wiring on each platform

### Option C: Hybrid (Semantic Layouts + Optional Hints)

Server uses semantic layout types for structural composition. Individual sections can carry optional hints for fine-tuning (e.g., priority, density) that clients apply at their discretion.

```json
{
  "id": "team-leaders-row",
  "type": "Row",
  "data": {
    "spacing": 16,
    "breakpoint": 600,
    "children": [
      { "id": "home-leaders", "type": "StatLine", "data": { "title": "BOS Leaders" } },
      { "id": "away-leaders", "type": "StatLine", "data": { "title": "MIA Leaders" } }
    ]
  },
  "layoutHints": { "priority": "high" }
}
```

- Structural layout is server-controlled (compositional)
- Fine-grained adjustments are client-interpreted (adaptive)
- Combines the strengths of both approaches
- Risk: two mechanisms to learn and govern

## Evaluation Criteria

- Cross-platform consistency from same response
- Alignment with SDUI server-control principle
- Fallback behavior when a layout type or hint is unsupported
- Renderer cost per platform
- Ability to express section relationships (side-by-side, grid, split-pane)

## Resolved Questions

- **Which option:** Option C (Hybrid) — structural layout via semantic types, fine-tuning via `layoutHints`.
- **Canonical hints:** `marginTop`, `marginBottom`, `dividerAbove`, `dividerBelow`, `priority` (high/normal/low).
- **Fallback rules:** Unknown hints are ignored. Missing `layoutHints` = default behavior (no margins, no dividers, normal priority).
- **Cross-platform parity:** Clients apply hints best-effort using platform-native layout systems.
