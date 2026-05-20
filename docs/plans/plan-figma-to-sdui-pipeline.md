# Plan: Figma-to-SDUI Wire Extraction Pipeline

**Status:** Proposed
**Created:** 2026-05-08
**Scope:** Tooling to extract Figma designs and produce SDUI wire JSON
**Depends on:** plan-design-token-coverage-from-sitebuilder.md (token registries must exist for snapping)
**Reference:** Sitebuilder `backend/src/services/figma/` (extractor, registry, skeleton, tokens, responsive)

---

## Objectives

1. Build a Figma extraction pipeline that reads a Figma frame and produces structured SDUI wire JSON (Container/Text/Image/Button nodes with token references)
2. Implement token snapping — raw Figma values (hex colors, px spacing, font specs) resolve to the nearest SDUI token
3. Implement a component registry that maps Figma component names/IDs to SDUI composer functions or semantic section types
4. Support multi-frame form-factor diffing — compare phone/tablet/web Figma frames to populate form-factor-specific token overrides

---

## Acceptance Criteria

- [ ] Given a Figma URL, the pipeline fetches the node tree via Figma REST API and produces a structured `DesignBrief` (sections, colors, fonts, image nodes)
- [ ] Given a Figma frame with auto-layout, the pipeline emits valid SDUI JSON: `Container` with `direction`, `gap` (token ref), `padding` (token ref), `backgrounds`
- [ ] Given a Figma text node using a Kinetic text style, the pipeline emits `Text` with correct `TextVariant` and `color` token reference
- [ ] Given a Figma image fill, the pipeline emits `Image` with `src` placeholder and `size`/`fit` from the frame constraints
- [ ] Given a raw hex `#1D428A` in a fill, the pipeline snaps to `token:nba.color.blue.30` (exact match from `color-tokens.json` primitives)
- [ ] Given `16px` gap, the pipeline snaps to `token:nba.spacing.lg` (nearest semantic from `spacing-tokens.json`)
- [ ] Given `8px` corner radius, the pipeline snaps to `token:nba.radius.md`
- [ ] Given a frame tagged `#component:scoreboard`, the registry maps it to `sectionType: "BoxscoreTable"` (semantic section)
- [ ] Given desktop (1440px) and phone (375px) sibling frames, the pipeline diffs layout changes and annotates form-factor-specific overrides
- [ ] Output JSON validates against `sdui-schema.json`
- [ ] Pipeline runs as a CLI tool (`make figma-extract URL=<figma-url>`) and as a server endpoint

---

## Non-Goals

- Natural language interpretation of designs (sitebuilder's AI loop) — this pipeline is structural extraction only
- Generating live data bindings or action handlers — output is a static UI skeleton
- Replacing server composers — output is a starting point for engineers, not production composition

---

## Architecture

```
Figma REST API
     │
     ▼
┌─────────────┐     ┌──────────────┐     ┌───────────────┐     ┌──────────────┐
│ Figma Client │────▶│  Extractor   │────▶│ Token Snapper │────▶│ SDUI Emitter │
│ (fetch file) │     │ (node walk)  │     │ (resolve refs)│     │ (wire JSON)  │
└─────────────┘     └──────────────┘     └───────────────┘     └──────────────┘
                           │                     │
                    ┌──────┴──────┐        ┌─────┴──────┐
                    │  Component  │        │   Token    │
                    │  Registry   │        │ Registries │
                    │ (YAML map)  │        │ (JSON)     │
                    └─────────────┘        └────────────┘
```

### Implementation location

Add to `sdui-prototype/tools/figma/` as a standalone Node.js or Kotlin CLI. Reuse sitebuilder's extraction logic as a reference but implement against SDUI's token format.

---

## Phase 1: Figma Client

### 1.1 Figma URL parser

Parse `https://www.figma.com/file/{key}/...?node-id={id}` into `{ fileKey, nodeIds }`.

Reference: sitebuilder's `parse_figma_url()` in `services/figma/client.rs`.

### 1.2 Figma API client

- `GET /v1/files/{key}` — full file metadata
- `GET /v1/files/{key}/nodes?ids={ids}` — specific nodes
- `GET /v1/images/{key}?ids={ids}&format=png` — image export

Auth: `FIGMA_ACCESS_TOKEN` env var, passed as `X-Figma-Token` header.

### 1.3 Node tree types

Define types matching Figma's node schema:
- `FigmaNode { id, name, type, children, absoluteBoundingBox, fills, effects, style, layoutMode, itemSpacing, paddingTop/Right/Bottom/Left, cornerRadius, componentId, visible }`
- `Fill { type, color, opacity, gradientStops, gradientHandlePositions }`
- `Effect { type, color, offset, radius, spread, visible }`
- `TextStyle { fontFamily, fontSize, fontWeight, lineHeightPx, letterSpacing, textCase }`

---

## Phase 2: Extractor

### 2.1 Section classification

Walk top-level children of the root frame. For each visible FRAME/COMPONENT/INSTANCE node:

1. Parse `#component:` / `#include:` tags from the frame name (same convention as sitebuilder)
2. Sort by Y position (top-to-bottom ordering)
3. Skip `#include:header` / `#include:footer` (shared chrome)
4. For `#component:` tagged frames → look up in component registry
5. For untagged frames → extract design specs and generate SDUI JSON

Reference: sitebuilder's `classify_sections()` in `services/figma/extractor/nodes.rs`.

### 2.2 Design spec extraction

For each section, extract:

| Figma property | SDUI mapping |
|---|---|
| `layoutMode` VERTICAL/HORIZONTAL | `Container.direction` column/row |
| `itemSpacing` | `Container.gap` → snap to spacing token |
| `paddingTop/Right/Bottom/Left` | `Container.padding` → snap to spacing token |
| `fills[].color` | `Container.backgrounds[].color` → snap to color token |
| `fills[].type == GRADIENT_LINEAR` | `Container.backgrounds[].gradient` |
| `cornerRadius` | `Container.cornerRadius` → snap to radius token |
| `effects[].type == DROP_SHADOW` | `Container.shadows[]` → snap to shadow token |
| TEXT node `style` | `Text.variant` → match to TextVariant by family+weight+textCase |
| TEXT node `fills[0].color` | `Text.color` → snap to color token |
| IMAGE fill | `Image` node with placeholder `src` |

### 2.3 Recursive child walk

Walk children up to 4 levels deep (matching sitebuilder). Each child becomes a nested SDUI node. Stop recursion at:
- Leaf TEXT nodes → `Text`
- Leaf IMAGE fill nodes → `Image`
- Component instances matching known SDUI primitives (Button, Icon, Badge, Divider)

---

## Phase 3: Token Snapper

### 3.1 Color snapping

Load `schema/color-tokens.json`. Build a lookup map:
- Exact hex match against `primitives` → `token:nba.color.{family}.{step}`
- Exact hex match against `ui` light/dark values → `token:nba.{semantic}`
- No match → emit raw hex with a warning

Priority: semantic UI tokens > primitives (prefer `token:nba.bg.primary` over `token:nba.color.grey.100`).

### 3.2 Spacing snapping

Load `schema/spacing-tokens.json`. For a raw px value:
1. Check exact match against palette `phone` values
2. If no exact match, find nearest semantic alias
3. If distance > 2px, emit raw value with a warning

### 3.3 Radius snapping

Same approach as spacing, against `schema/corner-radius-tokens.json`.

### 3.4 Typography snapping

Given a Figma text node's `fontFamily`, `fontWeight`, `fontSize`, `textCase`:
1. Match family+weight+textCase to a `TextVariant` from `schema/typography-tokens.json`
2. If multiple matches, prefer the variant whose `web.wide` size is closest to the Figma fontSize
3. If no match → emit variant `bodyMedium` with a warning

### 3.5 Shadow snapping

Given a Figma drop shadow effect, compare against `schema/shadow-tokens.json` tiers. Match by blur radius proximity.

---

## Phase 4: SDUI Emitter

### 4.1 JSON generation

Convert the extracted + snapped tree into valid SDUI wire JSON. Each node emits:

```json
{
  "type": "Container",
  "direction": "vertical",
  "gap": "token:nba.spacing.md",
  "padding": { "top": "token:nba.spacing.lg", "start": "token:nba.spacing.lg", "bottom": "token:nba.spacing.lg", "end": "token:nba.spacing.lg" },
  "backgrounds": [{ "color": "token:nba.bg.primary" }],
  "cornerRadius": "token:nba.radius.lg",
  "children": [
    { "type": "Text", "text": "TONIGHT'S GAME", "variant": "labelMedium", "color": "token:nba.label.primary" },
    { "type": "Image", "src": "{{placeholder:hero-image}}", "size": { "width": "fill", "aspectRatio": 1.78 }, "fit": "cover" }
  ]
}
```

### 4.2 Schema validation

Validate output against `schema/sdui-schema.json` using JSON Schema. Report violations as warnings (non-blocking).

### 4.3 Output formats

- `--format json` — raw SDUI wire JSON (default)
- `--format composer` — Java `AtomicCompositeBuilder` call chain (starter code for server composers)
- `--format brief` — markdown summary (section list, token usage report, unresolved warnings)

---

## Phase 5: Component Registry

### 5.1 Create `tools/figma/sdui-component-registry.yml`

```yaml
components:
  # Atomic composites — server builds the UI tree
  - figma_name: "hero-banner*"
    sdui_type: atomic_composite
    composer: "AtomicCompositeBuilder.heroBanner()"
    
  - figma_name: "card*"
    sdui_type: atomic_composite
    composer: "AtomicCompositeBuilder.card()"

  - figma_name: "section-heading*"
    sdui_type: atomic_composite
    composer: "AtomicCompositeBuilder.sectionHeading()"

  # Semantic sections — client renders
  - figma_name: "scoreboard*"
    sdui_type: semantic
    section_type: "BoxscoreTable"

  - figma_name: "video-player*"
    sdui_type: semantic
    section_type: "VideoPlayer"

  - figma_name: "tabs*"
    sdui_type: semantic
    section_type: "TabGroup"

  - figma_name: "standings-table*"
    sdui_type: semantic
    section_type: "BoxscoreTable"

  - figma_name: "stats-table*"
    sdui_type: semantic
    section_type: "SeasonLeadersTable"
```

### 5.2 Registry matching

Same 3-tier matching as sitebuilder: exact component ID → exact name → glob name. Return `{ sdui_type, composer|section_type }`.

---

## Phase 6: Multi-Frame Form-Factor Diffing

### 6.1 Frame detection

On a Figma page, detect sibling frames by width:
- 375px → `phone`
- 768px → `tablet`
- 1440px → `web.wide`
- 1920px → `tv`

### 6.2 Child matching

Match children across frames by name or component ID (same algorithm as sitebuilder's `responsive.rs`).

### 6.3 Diff and annotate

For each matched pair, diff layout properties. Where values differ, emit form-factor-specific overrides instead of `"*"` wildcard tokens. Example:

Desktop: `gap: 24px` → `token:nba.spacing.xl`
Phone: `gap: 12px` → `token:nba.spacing.md`

Output: annotate the Container with `{ "gap": { "phone": "token:nba.spacing.md", "web.wide": "token:nba.spacing.xl" } }` so composers know the intent differs by form factor.

---

## Phase 7: CLI and Server Integration

### 7.1 CLI tool

```bash
# Extract a single frame
make figma-extract URL="https://figma.com/file/abc/..." 

# Extract with form-factor diffing
make figma-extract URL="https://figma.com/file/abc/..." DIFF=true

# Output as composer starter code
make figma-extract URL="https://figma.com/file/abc/..." FORMAT=composer
```

### 7.2 Server endpoint (optional)

`POST /api/tools/figma-extract` on the SDUI server. Accepts `{ figmaUrl, exportImages, format }`. Returns `{ sections, sduiJson, warnings, tokenUsage }`.

Gated behind `FIGMA_ENABLED=true` env var (same pattern as sitebuilder).
