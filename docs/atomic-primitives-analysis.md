# Atomic Primitives + Sections — Hybrid Architecture Plan

> **Date**: 2026-03-12  
> **Status**: Draft  
> **Context**: Analysis of 19 existing section renderers across Android (Compose), Web (React/TSX), and the JSON Schema to determine the right level of abstraction for a dual-layer SDUI rendering model.

---

## Executive Summary

Introduce a **dual-layer rendering model** with clear naming:

- **Atomic layer** — a small set of styleable layout primitives (Container, Text, Image, Button, Spacer, Divider, ScrollContainer, Conditional) that the server can compose into arbitrary UI trees.
- **Section layer** — the existing named section types (BoxscoreTable, GamePanel, Form, etc.) that encapsulate complex stateful domain logic the client owns. These keep their current names and structure.

The schema already defines 8 atomic primitive types under the `Component` definition — they've just never been activated. This plan:

1. Renames and sharpens the vocabulary across schema, routers, models, and packages.
2. Activates atomic renderers on Android and Web.
3. Classifies all 19 sections into Replace (7), Hybrid (4), and Keep-as-Section (8).
4. Migrates the 7 replaceable sections to server-composed atomic trees.

---

## Naming Convention

The old naming (`Component` vs `Section`) is ambiguous — every framework has "components" and "sections." The new convention makes the two layers immediately identifiable at every level:

| Layer | Old Term | New Term | Purpose |
|---|---|---|---|
| Primitives | `Component` | **`Atomic`** | Server-composed layout building blocks |
| Domain units | `Section` | **`Section`** (unchanged) | Client-owned, stateful domain renderers — no rename needed |

### Where the names appear

> **Naming principle**: `Atomic` prefix applies to all **net-new** atomic system files. Existing `Section`/`SectionRouter`/`SduiSection` names stay as-is — they're deeply ingrained in the codebase and the atomic layer is entirely new code, making its rename trivial.

| Artifact | Current Name | Change |
|---|---|---|
| Schema definition | `Component` | Rename → `AtomicElement` (schema only, no existing code uses this) |
| Schema enum values | `Container`, `Text`, … | unchanged |
| Bridge section type | *(new)* | Add `AtomicComposite` as a new section type |
| Android router | `SectionRouter` | **Keep as-is** — add `AtomicComposite` case that delegates to `AtomicRouter` |
| Android router (new) | — | `AtomicRouter` (new file) |
| Android package | `renderer/sections/` | **Keep as-is** |
| Android package (new) | — | `renderer/atomic/` (new directory) |
| Android models | `SduiSection` | **Keep as-is** |
| Android models (new) | — | `AtomicElement` data class (new file) |
| Web router | `SectionRouter.tsx` | **Keep as-is** — add `AtomicComposite` case |
| Web router (new) | — | `AtomicRouter.tsx` (new file) |
| Web directory | `components/sections/` | **Keep as-is** |
| Web directory (new) | — | `components/atomic/` (new directory) |
| TypeScript models | `Section` | **Keep as-is** |
| TypeScript models (new) | — | `AtomicElement` (new type) |
| Swift models | `Section` | **Keep as-is** |
| Swift models (new) | — | `AtomicElement` (new type) |

---

## Phase 1: Activate Atomic Renderers

### 1.1 `AtomicRouter` — Android (Compose)

New file: `renderer/atomic/AtomicRouter.kt`

```kotlin
@Composable
fun AtomicRouter(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    when (element.type) {
        "Container"       -> AtomicContainer(element, screenState, onAction, modifier)
        "Text"            -> AtomicText(element, modifier)
        "Image"           -> AtomicImage(element, onAction, modifier)
        "Button"          -> AtomicButton(element, onAction, modifier)
        "Spacer"          -> AtomicSpacer(element, modifier)
        "Divider"         -> AtomicDivider(element, modifier)
        "ScrollContainer" -> AtomicScrollContainer(element, screenState, onAction, modifier)
        "Conditional"     -> AtomicConditional(element, screenState, onAction, modifier)
        "DisplayGrid"    -> AtomicDisplayGrid(element, screenState, onAction, modifier)
        else              -> { /* unknown element — skip gracefully */ }
    }
}
```

### 1.2 Atomic Primitive Implementations — Android

Each maps 1:1 to Compose primitives. The schema already defines all necessary properties.

| Atomic Element | Compose Mapping | Key Schema Properties |
|---|---|---|
| **Container** | `Column`/`Row` via `direction` | `children`, `direction`, `alignment`, `crossAlignment`, `gap`, `padding`, `backgroundColor`, `backgroundGradient` (new) |
| **Text** | `Text` + typography mapping from `variant` | `content`, `variant`→MaterialTheme.typography, `weight`, `color`, `maxLines` |
| **Image** | `AsyncImage` (Coil) | `src`, `aspectRatio`, `fit`→ContentScale, `width`, `height`, `alt` |
| **Button** | `Button`/`OutlinedButton`/`TextButton` via `buttonVariant` | `label`, `buttonVariant`, `icon`, `disabled`, `actions` |
| **Spacer** | `Spacer` + `Modifier.size()` | `width`, `height`, `size` |
| **Divider** | `HorizontalDivider`/`VerticalDivider` via `orientation` | `orientation`, `thickness`, `color` |
| **ScrollContainer** | `LazyRow`/`LazyColumn` via `direction` | `children`, `direction`, `paging`, `snapAlignment`, `gap` |
| **Conditional** | if/else evaluating `condition` against `screenState` | `condition`, `trueChild`, `falseChild` |
| **DisplayGrid** | `LazyColumn` > header `Row` + data `Row`s with `weight()` cells | `columns` (key, label, align, width), `rows` (key-value records), `headerVariant`, `cellVariant`, `striped` |

### Grid vs. Section Decision Tree

Use this to decide whether tabular data belongs in a **DisplayGrid** (atomic) or a dedicated **section** renderer.

> **Why "DisplayGrid" and not "DataTable"?** The Technical Proposal (§ Tabular Data) explicitly rejected a generic `DataTable` at the *section* level because different tables have genuinely different data shapes — a boxscore row is fundamentally different from a roster row. The atomic `DisplayGrid` is a deliberately different primitive: a **display-only, non-interactive, server-ordered grid of text cells**. The name makes the non-interactive boundary self-documenting and avoids confusion with the rejected generic-table pattern.

```
Need tabular data?
├─ Needs client-side sort?                  → Section (BoxscoreTable / SeasonLeadersTable)
├─ Needs frozen columns + horizontal scroll → Section
├─ Needs pagination?                        → Section
├─ Needs interactive rows (expand/select)?  → Section
├─ Needs domain-typed row models?           → Section (compile-time safety)
├─ Needs per-row actions (tap/swipe)?       → Section
└─ Display-only, server-ordered grid of text
   with no client interaction?              → DisplayGrid atomic primitive
```

**DisplayGrid boundary (hard contract)**:
- ✅ Server decides column order, row order, and display values — client paints them.
- ✅ Cell values are pre-formatted strings. No client-side formatting or computation.
- ✅ Zero client interaction — no sort, no filter, no expand, no select, no tap.
- ❌ The moment ANY of the above constraints break, promote to a semantic section type.

**When to use DisplayGrid**: simple stat snapshots, schedule lookups, standings summaries, and any case where the grid is purely cosmetic output.

**When to use a section table**: any table where the user can sort, scroll sync frozen columns, expand rows, paginate, or where row data carries domain-typed models that benefit from compile-time safety.

### 1.3 `AtomicRouter` — Web (React)

New file: `components/atomic/AtomicRouter.tsx`

Same 9 elements, each mapping to semantic HTML + CSS flexbox. Web is already natively atomic — `<div>`, `<span>`, `<img>`, `<button>`, `<table>` — so these are thin wrappers applying the schema's style tokens.

### 1.4 Bridge into `SectionRouter`

Add a new section type `AtomicComposite` whose data is `{ ui: AtomicElement }`. When `SectionRouter` encounters `type: "AtomicComposite"`, it delegates to `AtomicRouter`. The `ui` key holds rendering instructions (the atomic element tree); an optional `content` key is reserved for domain data used with future data-binding support.

```kotlin
// In SectionRouter — add this case alongside existing section types
"AtomicComposite" -> {
    val uiTree = parseAtomicElement(section.data)
    if (uiTree != null) {
        AtomicRouter(
            element = uiTree,
            screenState = screenState,
            onAction = onAction,
            modifier = modifier
        )
    }
}
```

---

## Phase 2: Section Classification

### Analysis Method

Every existing renderer was decomposed by:
- Compose primitives used (Column, Row, Box, Text, Image, Button, LazyRow, etc.)
- Internal state (remember{}, mutableStateOf, scroll state)
- User interaction complexity (tap, scroll, sort, form input, tab selection)
- Data properties bound from section data
- Conditional rendering branches
- Lines of UI code (excluding data extraction)

### Tier 1 — REPLACEABLE by Atomic Primitives (5 sections)

Stateless, ≤80 lines of UI, expressible entirely as Container/Text/Image/Button/Spacer trees, with **no platform SDK dependencies**. These become server-composed `AtomicComposite` sections.

| Current Section | UI Lines | Atomic Equivalent | Why Replaceable |
|---|---|---|---|
| **ErrorState** | ~50 | `Container(column, center) > Text + Text + Button` | Stateless, icon→emoji map movable to server |
| **SectionHeader** | ~50 | `Container(row) > Text(heading) + Spacer + Button(text, action)` | Stateless, 3 data props, universal pattern |
| **PromoBanner** | ~65 | `Container(column, gradient) > Image + Text + Text + Button` | Stateless *banner-with-CTA*, gradient bg |
| **ContentRail** | ~35 | `Container(column) > Text + ScrollContainer(row) > [children]` | Stateless, delegates to item renderer |
| **FollowingRail** | ~80 | `Container(column) > Text + ScrollContainer(row) > Container(circle) > Image + Text` | Stateless circular avatars |

**Impact**: Eliminates ~280 lines of platform-specific rendering code per platform. These 5 sections are pure layout with zero SDK dependency risk.

### Tier 2 — HYBRID Candidates (6 sections)

Stateless or nearly stateless, but with moderate structural complexity (multiple visual variants, conditional branches, repeated item patterns). Evaluate after Tier 1 proves out.

| Section | UI Lines | Complexity Factor | Disposition |
|---|---|---|---|
| **HeroPanel** | ~95 | Image overlay + duration badge + contentType badge. Used as item delegate inside ContentRail. | Likely pulled into Tier 1 when ContentRail migrates — the items need to be atomic too. |
| **ScoreboardHeader** | ~85 | Fixed team-vs-team layout. Could be `Container > Row > Image + Text`. | *Could* be atomic, but team-matchup is a core domain pattern. Evaluate whether it's worth keeping as a section for brand consistency. |
| **StatLine** | ~160 | Layout variant (h/v) + player image + name + stat value. Server already sends `layout` hint. | The horizontal/vertical variant is expressible via `Container(direction)`. Strong candidate if atomic proves reliable. |
| **VideoCarousel** | ~140 | Structurally identical to ContentRail + HeroPanel. Has rememberScrollState. | Merge candidate with ContentRail via `ScrollContainer(row) > [card children]`. |
| **GamePanel** | ~380 (incl. adapter) | 2 variants (standard/featured) × 3 visual states (PRE/LIVE/FINAL). **Zero internal state** — no `remember{}`, no `mutableStateOf`. The `mapGamePanel()` adapter computes `visualState` from `gameStatus` (a trivial `when/switch`) and formats leader lines — both movable server-side. Standard variant: Card > team row + conditional score/status + leaders. Featured variant: gradient Box > team columns + scores + badges. All branching is on server-sent data fields (`gameStatus`, `variant`). | **Strong atomic candidate.** The server already knows `gameStatus` and `variant` — it can emit the correct atomic tree directly, eliminating the client-side branching entirely. Conditional element handles remaining edge cases. Deferred to Tier 2 due to tree depth and the need for the server composition layer to support game-state-aware template selection. |
| **NbaTvSchedule** | ~230 | Hero image with gradient overlay + slot list with LIVE badges. **Zero internal state** — no `remember{}`, no `mutableStateOf`, no scroll state. Only client logic is ISO time parsing (`startTime.substringAfter("T").take(5)`) — trivially moved server-side. Each slot row is Container(row) > Text(time) + Column(title/subtitle) + Conditional(LIVE badge). | **Strong atomic candidate.** Hero section = Container(stack) > Image + gradient Container + Column(badge + title + subtitle). Slot list = Column > repeated slot rows. The `isLive` conditional is a natural fit for the Conditional atomic element. Deferred to Tier 2 because the slot list is an N-item repeating pattern — needs either inline repetition (server sends all slot trees) or atomic template support (future). |

### Tier 3 — MUST REMAIN Sections (6 sections)

These have genuine internal state (`remember{}`, `mutableStateOf`, coordinated scroll positions), complex user interactions (sort, paginate, form input, tab switching), nest other sections, or **integrate with platform-native SDKs** (ad SDKs, billing SDKs, auth SDKs) that own their own view lifecycle and state. They **cannot** be reasonably expressed as atomic trees.

| Section | UI Lines | Why It Must Stay a Section |
|---|---|---|
| **BoxscoreTable** | ~350 | Frozen column + horizontal scroll sync, client-side sort with `remember{}`, starter/bench divider logic, DNP player handling, totals row. The interaction model (sort by column tap, scroll state coordination) requires client-owned state. |
| **SeasonLeadersTable** | ~280 | Same frozen-column + coordinated scroll pattern, pagination display, sort column highlighting. |
| **FormRenderer** | ~380 | 5 field types (select, segmented, toggle, radio, text), field-level `mutableStateOf`, dropdown expansion state, `screenState` coordination for every field, submit action resolving `paramBindings`. |
| **TabGroup** | ~35 lines but… | Tab selection fires `mutate` action updating `screenState`, which drives which child *sections* render. This is a **section container** — it nests other sections and manages state transitions. |
| **SubscribeHero / SubscribeBanner** | ~225 combined | **Platform SDK dependency.** Target integration is In-App Purchase (Google Play Billing Library / StoreKit 2). Requires a purchase state machine (loading products → purchasing → verifying → success/failure), entitlement checks before rendering CTA text, and localized pricing from the store — not the server. SubscribeHero adds pricing tier cards with feature lists, multi-CTA buttons, badge rendering, and gradient overlays with dynamic tier count. Both surfaces share the billing SDK dependency and should remain native sections. |
| **AdSlot** | ~35 | **Platform SDK dependency.** Currently a placeholder, but target integration is Google Ad Manager. GAM's `AdManagerAdView` owns its own view lifecycle (load, refresh, destroy), consent management (UMP/TCF), MRC-compliant viewability tracking, fill-rate fallback UI, and timed refresh cadence — all fundamentally native. An atomic `Container > Text` cannot host a native ad view. |

### Reclassified: Row (Tier 1, stays a section)

Row (~50 LOC) is stateless with zero client logic — just a responsive breakpoint check. It's trivial enough to be Tier 1, but it nests other sections (calls `SectionRouter` for each child), so it can't become an `AtomicComposite` without a mixed-children model. **Keep as a thin section shell** — the ~50 lines per platform aren't worth the architectural complexity of mixing atomic and section children.

---

## Phase 3: Schema Evolution

### 3.1 Rename `Component` → `AtomicElement` in Schema

In `sdui-schema.json`, rename the definition:

```json
"AtomicElement": {
    "type": "object",
    "required": ["type"],
    "properties": {
        "type": {
            "type": "string",
            "enum": ["Container", "Text", "Image", "Button", "Spacer", "Divider", "ScrollContainer", "Conditional", "DisplayGrid"]
        },
        ...existing properties unchanged...
    }
}
```

### 3.2 Add `AtomicComposite` Section Type

Add to the section type enum and define its data shape:

```json
"AtomicCompositeData": {
    "type": "object",
    "required": ["ui"],
    "properties": {
        "ui": { "$ref": "#/definitions/AtomicElement" },
        "content": { "type": "object", "additionalProperties": true }
    }
}
```

### 3.3 Add `backgroundGradient` to Container

Required for PromoBanner faithfulness:

```json
"backgroundGradient": {
    "type": "object",
    "properties": {
        "colors": { "type": "array", "items": { "type": "string" } },
        "direction": { "type": "string", "enum": ["horizontal", "vertical"] }
    }
}
```

### 3.4 Regenerate Platform Models

Run `codegen/generate.sh` — Swift (`AtomicElement` struct) and TypeScript (`AtomicElement` interface) models auto-generate.

### 3.5 (Phase 5) Add `SectionSlot` Element Type

Future schema addition for the bidirectional bridge. Add `"SectionSlot"` to the `AtomicElement.type` enum and add a `section` property:

```json
"type": {
    "type": "string",
    "enum": ["Container", "Text", "Image", "Button", "Spacer", "Divider", "ScrollContainer", "Conditional", "DisplayGrid", "SectionSlot"]
},
"section": {
    "$ref": "#/definitions/Section",
    "description": "Full section object to render via SectionRouter. Only used when type is SectionSlot."
}
```

See **Phase 5** for implementation details.

### 3.6 AtomicComposite Performance Contract

Server-composed atomic trees are unconstrained by default — a careless composition could emit a deeply nested tree with hundreds of nodes, causing layout thrashing, stack overflows, or excessive memory pressure on the client. This isn't a theoretical risk; it's an inevitable outcome of any tree-authoring system without guardrails.

The following hard limits are validated **server-side** at composition time. Payloads that exceed any limit are rejected before reaching the client.

| Constraint | Limit | Rationale |
|---|---|---|
| **Maximum tree depth** | 6 | Prevents deep nesting that causes Compose measure-pass stack depth issues and deeply nested DOM trees. 6 levels is sufficient for any realistic layout (e.g., Card > Row > Column > Row > Text/Image). |
| **Maximum children per Container** | 20 | Prevents single containers from becoming unbounded lists — use a proper `ScrollContainer` or paginated section for long lists. |
| **Maximum total node count per AtomicComposite** | 50 | Bounds the total element count in a single AtomicComposite tree. Prevents composition bloat and keeps render time predictable. |

**Enforcement**:

- **Server (composition service)**: Validate every `AtomicComposite.ui` tree before serialization. Trees that violate any constraint are rejected with a structured error (logged, not served to clients). This is the primary enforcement point.
- **Client (AtomicRouter)**: Defensive depth counter in the recursive render path. If depth exceeds the limit, render a placeholder and log a warning. This catches malformed payloads that bypass server validation (e.g., manual JSON authoring, stale cached payloads).
- **Schema**: Constraints are documented in the schema description fields but NOT enforced via JSON Schema (JSON Schema lacks tree-depth validation). Runtime validation is authoritative.

```kotlin
// Android — AtomicRouter depth guard
@Composable
fun AtomicRouter(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier,
    depth: Int = 0
) {
    if (depth > MAX_TREE_DEPTH) {
        // Log warning, render nothing — tree is malformed
        return
    }
    // ... dispatch to element renderers, passing depth + 1 to children
}

private const val MAX_TREE_DEPTH = 6
```

```typescript
// Web — AtomicRouter depth guard
function AtomicRouter({ element, screenState, onAction, depth = 0 }: Props) {
  if (depth > MAX_TREE_DEPTH) return null;
  // ... dispatch to element renderers, passing depth + 1 to children
}

const MAX_TREE_DEPTH = 6;
```

> **Note**: The recursion guard in Phase 5 (SectionSlot) is separate — it prevents `AtomicComposite → SectionSlot → AtomicComposite` cycles. The depth guard here prevents deep nesting within a single atomic tree.

---

## Phase 4: Migrate Tier 1 Sections

### Migration order (simplest first)

For each section:
1. Author example `AtomicComposite` JSON using an atomic element tree
2. Validate against updated schema
3. Verify `AtomicRouter` renders it visually identical to the dedicated section renderer
4. Update server feed generation to emit `AtomicComposite` instead of the old section type
5. Remove the dedicated section renderer

| Step | Section → Atomic Composition |
|---|---|
| 1 | **ErrorState** → `Container(column, center) > Text(emoji) + Text(title) + Text(message) + Button(retry)` |
| 2 | **SectionHeader** → `Container(row, spaceBetween) > Text(title, heading3) + Button(text, "See All", action)` |
| 3 | **PromoBanner** → `Container(column, gradient) > Image(logo) + Text(title) + Text(headline) + Text(subhead) + Button(primary, action)` |
| 4 | **ContentRail** → `Container(column) > Text(title) + ScrollContainer(row, gap:12) > [AtomicComposite items]` |
| 5 | **FollowingRail** → `Container(column) > Text(title) + ScrollContainer(row, gap:16) > Container(column, center) > Image(circle) + Text(name)` |

---

## Phase 4b: Tier 2 Evaluation Gate

Tier 2 sections (HeroPanel, ScoreboardHeader, StatLine, VideoCarousel, GamePanel, NbaTvSchedule) are deferred until Tier 1 validates. This phase defines the evaluation criteria and process — it is NOT automatic migration.

### Entry Criteria (all must be true before starting evaluation)

- [ ] Phase 4 complete: all 5 Tier 1 sections migrated to `AtomicComposite` on both platforms.
- [ ] Visual parity confirmed for every migrated section (screenshot comparison, no regressions).
- [ ] Performance contract validated: no Tier 1 `AtomicComposite` tree exceeds depth 6 / node count 50.
- [ ] Server composition layer handles the Tier 1 templates reliably (no composition-time errors in staging).
- [ ] Team retrospective on Tier 1 completed — pain points, composition authoring friction, and debugging experience documented.

### Per-Section Evaluation Checklist

For each Tier 2 candidate, evaluate:

| Question | Pass? | If No |
|---|---|---|
| Can the server produce the correct atomic tree for ALL visual variants? | | Section stays — variant complexity exceeds composition capability |
| Does the tree stay within the performance contract (depth ≤ 6, nodes ≤ 50)? | | Section stays — tree is too deep/complex |
| Is there genuine client state (`remember{}`, scroll coordination, live region)? | | Section stays — atomic layer doesn't own state |
| Is the domain pattern better served by a named section (brand consistency, analytics)? | | Section stays — semantic identity matters more than atomic flexibility |
| Does moving it to atomic reduce client code without increasing server composition complexity disproportionately? | | Section stays — net negative trade |

### Expected Outcomes (directional, not prescriptive)

| Section | Likely Outcome | Key Factor |
|---|---|---|
| **HeroPanel** | Migrate | Pulled in by ContentRail — items must be atomic when the rail is |
| **VideoCarousel** | Migrate | Structurally identical to ContentRail + HeroPanel |
| **StatLine** | Migrate | `direction` variant maps directly to `Container(direction)` |
| **NbaTvSchedule** | Migrate | Slot list is inline repetition; server sends all slot trees |
| **ScoreboardHeader** | Evaluate | Could be atomic, but team-matchup is a core brand pattern — may keep for domain identity |
| **GamePanel** | Evaluate | Strong atomic candidate, but 2 variants × 3 states is a large composition matrix. Depends on server composition layer maturity |

### Output

A written decision per section (migrate / keep / defer further) added to the Decisions table below. Sections that stay as sections are final — no re-evaluation unless the architecture changes fundamentally.

---

## Phase 5: SectionSlot — Bidirectional Bridge

### Problem

The atomic rendering pipeline is a closed system — `AtomicRouter` only knows 8 primitive element types. But real-world compositions need to embed sections that have platform SDK dependencies (AdSlot with GAM, SubscribeBanner with IAP), client-owned state (FormRenderer), or section-nesting semantics (TabGroup) inside an atomic layout. Today the bridge is one-directional:

```
SectionRouter ──(AtomicComposite)──▶ AtomicRouter
```

### Solution: `SectionSlot` Element

A new atomic element type that delegates rendering **back** to `SectionRouter`, completing the bidirectional bridge:

```
SectionRouter ──(AtomicComposite)──▶ AtomicRouter
AtomicRouter  ──(SectionSlot)──────▶ SectionRouter
```

When `AtomicRouter` encounters a `SectionSlot`, it extracts the embedded `section` object and hands it to `SectionRouter` with the full section lifecycle (refreshPolicy, actions, sectionStates, analytics).

### Example: Game Card with Inline Ad

```json
{
  "id": "game-card-LAL-BOS",
  "type": "AtomicComposite",
  "data": {
    "ui": {
      "type": "Container",
      "direction": "column",
      "padding": { "start": 16, "end": 16, "top": 12, "bottom": 12 },
      "backgroundColor": "#1A1A2E",
      "children": [
        { "type": "Text", "content": "LAL vs BOS", "variant": "titleMedium", "weight": "bold" },
        { "type": "Text", "content": "Q3 5:42 • LAL 87 - BOS 82", "variant": "bodySmall", "color": "#7a8baa" },
        { "type": "Container", "direction": "row", "gap": 8, "alignment": "center", "children": [
          { "type": "Button", "label": "Watch Now", "buttonVariant": "filled",
            "actions": [{ "trigger": "onTap", "type": "navigate", "targetUri": "nba://watch/0020000670" }] },
          { "type": "Button", "label": "Boxscore", "buttonVariant": "text",
            "actions": [{ "trigger": "onTap", "type": "navigate", "targetUri": "nba://game/0020000670/boxscore" }] }
        ]},
        { "type": "Divider", "color": "#333", "thickness": 1 },
        {
          "type": "SectionSlot",
          "id": "inline-ad",
          "section": {
            "id": "game-card-ad-LAL-BOS",
            "type": "AdSlot",
            "data": { "adUnitPath": "/nba/game-card-inline", "size": "banner" },
            "refreshPolicy": { "type": "static" },
            "sectionStates": { "error": { "hideOnError": true } }
          }
        }
      ]
    }
  }
}
```

The AdSlot gets its full section lifecycle — GAM SDK loads, viewability tracking, consent, fill-rate fallback — while the atomic tree controls where it renders within the card layout.

### Implementation Sketch

**Android:**
```kotlin
// In AtomicRouter — add this case
"SectionSlot" -> {
    element.section?.let { sectionMap ->
        val section = parseSduiSection(sectionMap)
        if (section != null) {
            SectionRouter(
                section = section,
                screenState = screenState,
                onAction = onAction,
                onStateChange = onStateChange,
                modifier = modifier
            )
        }
    }
}
```

**Web:**
```tsx
case 'SectionSlot': {
  if (!element.section) return null;
  return (
    <SectionRouter
      section={element.section as Section}
      state={state}
      onAction={onAction}
      onStateChange={onStateChange}
    />
  );
}
```

### Required Changes

| Change | Scope | Notes |
|---|---|---|
| Add `"SectionSlot"` to element type enum | Schema | See Phase 3.5 |
| Add `section?: Section` property to `AtomicElement` | Schema + models | Full section object with its own envelope |
| Handle `"SectionSlot"` in `AtomicRouter` | Android + Web | Delegates to `SectionRouter` |
| Thread `onStateChange` through `AtomicRouter` | Android + Web | Currently only `onAction` is passed. Needed for stateful sections (Form, TabGroup). Simple parameter addition. |
| Recursion guard | Both platforms | Prevent infinite loops: `AtomicComposite` → `SectionSlot` → `AtomicComposite` → `SectionSlot`… Add a depth counter or disallow `AtomicComposite` inside `SectionSlot`. |

### Use Cases Unlocked

| Use Case | SectionSlot Contents |
|---|---|
| Inline ad in game card | `AdSlot` — GAM native view with full SDK lifecycle |
| Subscribe CTA in promo layout | `SubscribeBanner` — with IAP flow (Play Billing / StoreKit) |
| Inline form in card | `FormRenderer` — field state, validation, submit |
| Tabs within a composed layout | `TabGroup` — mutate actions, section nesting |
| Pricing tiers inside promo | `SubscribeHero` — tier cards, feature lists |

### Phase 5 is not blocking Phase 1

`SectionSlot` is additive — it extends the element type enum without breaking existing atomic trees. Phase 1 proves the 5 Tier 1 sections work as pure atomic compositions. Phase 5 adds the escape hatch for mixed compositions when the first real use case (likely inline ads in game cards) lands.

### Complete AtomicComposite Server Response Examples

The following are full SDUI server response snippets showing how Tier 1 sections look as `AtomicComposite` section payloads within the `sections[]` array. These carry the same behavioral envelope (actions, refreshPolicy, analyticsId, impression tracking) as their section-based equivalents.

#### Example 1: SectionHeader → AtomicComposite

**Before (section-based):**
```json
{
  "id": "related-header",
  "type": "SectionHeader",
  "analyticsId": "section_header_related",
  "actions": [
    { "trigger": "onVisible", "type": "analytics", "event": "section_header_viewed",
      "params": { "title": "Related Content" },
      "impression": { "dedup": "once-per-screen", "threshold": { "visibility": 0.5, "dwellMs": 1000 } } }
  ],
  "data": {
    "title": "Related Content",
    "action": {
      "trigger": "onTap",
      "type": "navigate",
      "targetUri": "nba://content/related?gameId=0042300102",
      "webUrl": "https://www.nba.com/game/0042300102/related"
    }
  }
}
```

**After (AtomicComposite):**
```json
{
  "id": "related-header",
  "type": "AtomicComposite",
  "analyticsId": "section_header_related",
  "refreshPolicy": { "type": "static" },
  "actions": [
    { "trigger": "onVisible", "type": "analytics", "event": "section_header_viewed",
      "params": { "title": "Related Content" },
      "impression": { "dedup": "once-per-screen", "threshold": { "visibility": 0.5, "dwellMs": 1000 } } }
  ],
  "layoutHints": { "marginTop": 24, "dividerAbove": true },
  "data": {
    "ui": {
      "type": "Container",
      "direction": "row",
      "alignment": "spaceBetween",
      "crossAlignment": "center",
      "padding": { "start": 16, "end": 16, "top": 12, "bottom": 12 },
      "children": [
        {
          "type": "Text",
          "id": "title",
          "content": "Related Content",
          "variant": "heading3",
          "weight": "bold",
          "color": "text.primary"
        },
        {
          "type": "Button",
          "id": "see-all-btn",
          "label": "See All",
          "buttonVariant": "text",
          "actions": [
            {
              "trigger": "onTap",
              "type": "navigate",
              "targetUri": "nba://content/related?gameId=0042300102",
              "webUrl": "https://www.nba.com/game/0042300102/related"
            },
            {
              "trigger": "onTap",
              "type": "analytics",
              "event": "see_all_tapped",
              "params": { "section": "related_content" }
            }
          ]
        }
      ]
    }
  }
}
```

**What to observe:**
- The `Section` envelope is identical — `id`, `analyticsId`, `refreshPolicy`, `actions[]`, `layoutHints` all carry over unchanged.
- The `data.ui` replaces the typed `SectionHeaderData` with an atomic element tree — rendering instructions, not domain data.
- The "See All" button carries both a `navigate` and an `analytics` action — multi-action on a single trigger, exactly matching the existing action system.
- `color: "text.primary"` uses the proposed semantic token convention. Clients resolve it through their theme map; the server doesn't need to know light/dark mode.
- `id` fields on atomic elements (`"title"`, `"see-all-btn"`) enable ID-based data binding if this section ever becomes dynamic.

---

#### Example 2: PromoBanner → AtomicComposite (with gradient, image, i18n, impression tracking)

**Before (section-based):**
```json
{
  "id": "promo-banner",
  "type": "PromoBanner",
  "analyticsId": "promo_banner_leaguepass",
  "refreshPolicy": { "type": "static" },
  "data": {
    "title": "NBA League Pass",
    "description": "Watch every out-of-market game live or on demand.",
    "imageUrl": "https://cdn.nba.com/promo/league-pass-banner.jpg",
    "actions": [
      {
        "trigger": "onTap",
        "type": "navigate",
        "targetUri": "nba://leaguepass",
        "webUrl": "https://www.nba.com/watch/league-pass"
      }
    ]
  }
}
```

**After (AtomicComposite):**
```json
{
  "id": "promo-banner",
  "type": "AtomicComposite",
  "analyticsId": "promo_banner_leaguepass",
  "refreshPolicy": { "type": "poll", "intervalMs": 3600000 },
  "actions": [
    {
      "trigger": "onVisible",
      "type": "analytics",
      "event": "promo_banner_impression",
      "params": { "campaign": "league-pass-2026", "placement": "game-detail" },
      "destinations": ["adobe", "firebase"],
      "impression": {
        "dedup": "once-per-session",
        "threshold": { "visibility": 0.5, "dwellMs": 500 }
      }
    }
  ],
  "dataBindings": {
    "bindings": [
      { "sourcePath": "$.headline", "targetPath": "#headline-text.content" },
      { "sourcePath": "$.subhead", "targetPath": "#subhead-text.content" },
      { "sourcePath": "$.ctaLabel", "targetPath": "#cta-button.label" },
      { "sourcePath": "$.imageUrl", "targetPath": "#hero-image.src" }
    ],
    "stringKeys": {
      "#headline-text.content": "promo.leaguepass.headline",
      "#subhead-text.content": "promo.leaguepass.subhead",
      "#cta-button.label": "promo.leaguepass.cta"
    }
  },
  "sectionStates": {
    "loading": { "skeleton": "shimmer", "minHeightDp": 180 },
    "error": { "hideOnError": true }
  },
  "layoutHints": { "marginTop": 16, "marginBottom": 16 },
  "data": {
    "ui": {
      "type": "Container",
      "id": "promo-root",
      "direction": "column",
      "alignment": "center",
      "crossAlignment": "center",
      "padding": { "start": 24, "end": 24, "top": 32, "bottom": 32 },
      "backgroundGradient": {
        "colors": ["#1D428A", "#C8102E"],
        "direction": "horizontal"
      },
      "cornerRadius": 12,
      "children": [
        {
          "type": "Image",
          "id": "hero-image",
          "src": "https://cdn.nba.com/promo/league-pass-banner.jpg",
          "width": 200,
          "height": 60,
          "fit": "contain",
          "alt": "NBA League Pass logo"
        },
        {
          "type": "Spacer",
          "height": 16
        },
        {
          "type": "Text",
          "id": "headline-text",
          "content": "NBA League Pass",
          "stringKey": "promo.leaguepass.headline",
          "variant": "heading2",
          "weight": "bold",
          "color": "#FFFFFF",
          "maxLines": 2
        },
        {
          "type": "Spacer",
          "height": 8
        },
        {
          "type": "Text",
          "id": "subhead-text",
          "content": "Watch every out-of-market game live or on demand.",
          "stringKey": "promo.leaguepass.subhead",
          "variant": "body",
          "color": "#FFFFFFCC"
        },
        {
          "type": "Spacer",
          "height": 20
        },
        {
          "type": "Button",
          "id": "cta-button",
          "label": "Subscribe Now",
          "stringKey": "promo.leaguepass.cta",
          "buttonVariant": "primary",
          "actions": [
            {
              "trigger": "onTap",
              "type": "navigate",
              "targetUri": "nba://leaguepass",
              "webUrl": "https://www.nba.com/watch/league-pass",
              "presentation": "modal",
              "modalHeight": "full"
            },
            {
              "trigger": "onTap",
              "type": "analytics",
              "event": "promo_cta_tapped",
              "params": { "campaign": "league-pass-2026", "cta": "subscribe-now" },
              "destinations": ["adobe", "firebase"]
            }
          ]
        }
      ]
    }
  }
}
```

**What to observe:**
- **Full behavioral envelope**: `refreshPolicy` (hourly poll for campaign updates), `actions` (impression tracking with session-level dedup and dual analytics destinations), `dataBindings` (4 bindable fields), `sectionStates` (shimmer loading + hide-on-error), `layoutHints` (margins).
- **Data bindings use ID-based targeting**: `#headline-text.content` targets the atomic element with `id: "headline-text"` and updates its `content` property. No fragile array-index paths.
- **i18n at two levels**: `stringKeys` on the section-level `dataBindings` (for dynamically bound fields) AND `stringKey` directly on Text/Button elements (for statically rendered content). Clients resolve the translation key at render time, falling back to the literal `content`/`label` value.
- **`backgroundGradient`** on the root Container replaces the gradient logic that was hardcoded in the PromoBanner renderer. The server controls the brand gradient; the client just applies it.
- **`cornerRadius: 12`** on the root Container — matches the native design system's sheet/card radius (see mobile audit: Android 12dp, Apple similar).
- **Multi-action per trigger**: The CTA button fires both `navigate` (with modal presentation) and `analytics` on a single `onTap`. Platform action dispatchers already support this — actions are an array, dispatched sequentially.
- **Campaign-level server control**: To A/B test a different promo (e.g., NBA TV vs League Pass), the composition service simply returns a different atomic tree with different text, image, colors, and analytics params. No client code change. No app store release.

---

#### Side-by-side: Section vs Atomic response size

| Section | Section JSON (bytes) | AtomicComposite JSON (bytes) | Delta |
|---|---|---|---|
| SectionHeader | ~320 | ~780 | +144% |
| PromoBanner | ~380 | ~2,100 | +453% |

The AtomicComposite payloads are larger because they carry the full layout tree that was previously implicit in the client renderer. This is the fundamental trade-off: **bytes on the wire vs. deploys to the app store**. For context, the full `game-detail-live.json` response is ~12KB; adding 1-2KB per migrated section is negligible on modern networks and well within gzip compression benefits (atomic trees compress exceptionally well due to repeated key names like `type`, `children`, `content`).

---

## Primitive Usage Heat Map

How frequently each atomic element type is used across the 5 Tier 1 migrations:

| Atomic Element | Used In | Frequency |
|---|---|---|
| **Container** | All 5 | ██████ 5/5 |
| **Text** | All 5 | ██████ 5/5 |
| **Image** | PromoBanner, FollowingRail, ContentRail (items) | ████ 3/5 |
| **Button** | ErrorState, SectionHeader, PromoBanner | ████ 3/5 |
| **ScrollContainer** | ContentRail, FollowingRail | ███ 2/5 |
| **Spacer** | SectionHeader (between title and link) | █ 1/5 |
| **Divider** | — | 0/5 |
| **Conditional** | — | 0/5 |
| **DisplayGrid** | — | 0/5 |

Container and Text are universal. Image and Button cover most visual needs. ScrollContainer handles the two rail patterns. Divider, Conditional, and DisplayGrid become relevant in Tier 2 scenarios (e.g. schedule grids, simple stat tables).

---

## File Impact Map

### New files

| File | Purpose |
|---|---|
| `android/.../renderer/atomic/AtomicRouter.kt` | Dispatches `AtomicElement.type` → composable |
| `android/.../renderer/atomic/AtomicContainer.kt` | Container element renderer |
| `android/.../renderer/atomic/AtomicText.kt` | Text element renderer |
| `android/.../renderer/atomic/AtomicImage.kt` | Image element renderer |
| `android/.../renderer/atomic/AtomicButton.kt` | Button element renderer |
| `android/.../renderer/atomic/AtomicSpacer.kt` | Spacer element renderer |
| `android/.../renderer/atomic/AtomicDivider.kt` | Divider element renderer |
| `android/.../renderer/atomic/AtomicScrollContainer.kt` | ScrollContainer element renderer |
| `android/.../renderer/atomic/AtomicConditional.kt` | Conditional element renderer |
| `android/.../renderer/atomic/AtomicDisplayGrid.kt` | DisplayGrid element renderer |
| `android/.../models/AtomicElement.kt` | `AtomicElement` data class |
| `web/src/components/atomic/AtomicRouter.tsx` | Web atomic dispatcher |
| `web/src/components/atomic/` (9 files) | Web atomic element renderers (incl. AtomicDisplayGrid) |

### Modified files

| File | Change |
|---|---|
| `schema/sdui-schema.json` | Rename `Component`→`AtomicElement`, add `AtomicComposite` section type, add `backgroundGradient` |
| `android/.../renderer/SectionRouter.kt` | Add `AtomicComposite` case delegating to `AtomicRouter` (no rename) |
| `web/src/components/SectionRouter.tsx` | Add `AtomicComposite` case (no rename) |
| `android/.../models/SduiModels.kt` | Add `AtomicElement` data class (or separate file) |
| `codegen/generate.sh` | Regenerate after schema rename |

### Removed after Tier 1 migration

| Platform | Files Removed |
|---|---|
| Android | `ErrorStateRenderer.kt`, `SectionHeaderRenderer.kt`, `PromoBannerRenderer.kt`, `ContentRailRenderer.kt`, `FollowingRailRenderer.kt` |
| Web | `ErrorState.tsx`, `SectionHeader.tsx`, `PromoBanner.tsx`, `ContentRail.tsx`, `FollowingRail.tsx` |

---

## Verification

| Check | Method |
|---|---|
| Schema valid | All existing example JSON files (`schema/examples/*.json`) still validate after rename. New `AtomicComposite` examples validate. |
| Visual parity | Screenshot section renderer output vs. AtomicRouter output for each migrated section — pixel-comparable. |
| Android build | `cd android && ./gradlew assembleDebug` passes after adding `atomic/` package and removing migrated section renderers. |
| Web build | `cd web && npm run build` passes after same changes. |
| Codegen | `cd codegen && ./generate.sh` regenerates Swift + TypeScript models with `AtomicElement`. |
| Tier 3 regression | BoxscoreTable, SeasonLeadersTable, Form, TabGroup, GamePanel, SubscribeHero, NbaTvSchedule, Row all render identically — no regressions from schema/router changes. |

---

## Phase 6: Governance Document Updates

Once the atomic layer is validated (Phases 1–4 verified), update the three upstream governance documents to reflect the dual-layer model. This is deferred until after implementation stabilizes so the documents describe proven behavior, not aspirational design.

### 6.1 Executive Summary (`SDUI_Executive_Summary_v2.md`)

- **"What Was Built" table**: Add row for the atomic rendering layer (AtomicRouter, 9 primitive renderers on Android + Web, AtomicComposite bridge section type).
- **JSON Schema row**: Update "8 atomic primitives" → "9 atomic primitives" (DisplayGrid added).
- **Key Architectural Concepts**: Expand "Two-tier primitives" bullet to name `AtomicComposite` as the built bridge mechanism and `DisplayGrid` as the 9th primitive.
- **Revision history note** referencing the atomic layer work.

### 6.2 Technical Proposal (`SDUI_Technical_Proposal_v2.md`)

- **§2 Schema Design**: Remove or qualify "Schema defines semantic section types, not atomic layout primitives" — the schema now defines both. Add subsection documenting `AtomicElement` (9 types), `AtomicComposite` section type, `AtomicCompositeData` (ui + content), and the `DisplayGrid` primitive with its non-interactive boundary.
- **§2 Schema Design (Grid vs. Section Decision Tree)**: Include the Grid vs. Section Decision Tree from this plan as a normative reference. It defines the hard boundary between `DisplayGrid` (atomic) and semantic table sections — this is a schema-level contract, not just a plan-level guideline.
- **§8 Platform Coverage / Renderer table**: Add `AtomicRouter` and 9 atomic renderers per platform. Note `AtomicComposite` as the 20th section type in the router.
- **§9 Gaps**: Add atomic layer performance contract (tree depth 6, child count 20, node count 50) as a documented constraint.
- **§10 Requirement Status table**: Add row for "Atomic rendering layer / AtomicComposite" with status "Built."
- **Revision History**: Add entry for atomic layer addition.

### 6.3 Requirements Summary (`sdui-requirements-summary.md`)

- **Key Schema Decisions**: Update the "Semantic section types — not atomic primitives" bullet to acknowledge the dual-layer model: semantic sections for stateful domain logic, atomic primitives for server-composed generic layouts, coexisting via `AtomicComposite`.
- **Key Schema Decisions (Grid vs. Section Decision Tree)**: Include the Grid vs. Section Decision Tree as a requirement-level boundary. This ensures the non-interactive contract for `DisplayGrid` is captured alongside the existing "Semantic types over generic DataTable" decision — providing both the rationale (requirements) and the decision tree (when to use which).
- **Requirement Status table**: Add row for "Atomic rendering layer (AtomicRouter, 9 primitives, AtomicComposite bridge)" with status "Built."

### 6.4 Trigger

Execute Phase 6 when:
- Phase 1 (activate renderers) is verified on both platforms.
- Phase 3 (schema evolution) changes are committed and codegen passes.
- Phase 4 (Tier 1 migration) has at least one section migrated end-to-end with visual parity confirmed.

Do NOT update governance documents during active iteration — only after the design has stabilized through implementation.

---

## Decisions

| Decision | Choice | Rationale |
|---|---|---|
| **Naming** | `Atomic` prefix for new layer; `Section` stays as-is | Atomic is net-new code (easy to name correctly from scratch). Section naming is deeply ingrained — renaming adds churn with no functional benefit. |
| **Primitive set** | 9 elements | Container, Text, Image, Button, Spacer, Divider, ScrollContainer, Conditional, DisplayGrid. DisplayGrid (formerly DataTable) added for display-only, non-interactive, server-ordered grids (see Grid vs. Section Decision Tree). Name explicitly disambiguates from the generic DataTable pattern rejected at the section level. `backgroundGradient` added on Container for PromoBanner. |
| **Bridge mechanism** | New `AtomicComposite` section type | Explicit intent, no ambiguity (vs. optional `components` field on all sections). `SectionRouter` delegates to `AtomicRouter`. |
| **Migration scope** | Tier 1 only (7 sections) | Tier 2 deferred until Tier 1 validates. Tier 3 stays as sections permanently. |
| **iOS renderers** | Out of scope | Swift models auto-generate. SwiftUI `AtomicRouter` follows the same `switch` pattern as Compose/React — separate work item. |
| **Router rename** | `SectionRouter` stays as-is | No rename — existing name is well-understood and deeply referenced. Only the new `AtomicRouter` gets the `Atomic` prefix. |

---

## Behavioral Feature Analysis: Atomic Readiness

The SDUI schema provides six behavioral capabilities beyond pure layout. This section audits whether each capability works correctly when a section is replaced by an `AtomicComposite`, identifies gaps in the `AtomicElement` definition, and assesses i18n / data-refresh / UX use cases.

### Feature inventory on `Section` vs `AtomicElement`

| Behavioral Feature | Available on `Section`? | Available on `AtomicElement`? | Gap? |
|---|---|---|---|
| **`actions[]`** (navigate, analytics, mutate, refresh, dismiss, toast) | Yes | Yes | **No gap** — atomic elements carry actions directly. All 6 triggers (onTap, onLongPress, onVisible, onSwipe, onFocus, onBlur) are available. |
| **`refreshPolicy`** (static / poll / sse) | Yes | No | **No gap at AtomicComposite level** — the outer Section envelope carries the policy. Individual atomic elements don't refresh independently; the whole composite refreshes. This matches how sections work today. |
| **`dataBindings`** (JSONPath source → dot-path target + `stringKeys`) | Yes | No | **Partial gap** — see detailed analysis below. |
| **`sectionStates`** (loading skeleton, error message, retry) | Yes | No | **No gap at AtomicComposite level** — the section envelope carries loading/error states. Atomic trees don't need independent loading states. |
| **`subsections[]`** (nested interaction targets with their own actions) | Yes | No | **Eliminated** — atomic elements already carry per-element `actions[]`, which is more granular than subsections. No need for subsections when every node in the tree can be tapped independently. |
| **`analyticsId`** | Yes | No (but actions carry `event` + `params`) | **Acceptable** — the `AtomicComposite` section carries the top-level `analyticsId`. Individual atomic elements use `onVisible` analytics actions for impression tracking. Consider adding an optional `analyticsId` to `AtomicElement` for debugging/observability. |
| **`layoutHints`** (margin, dividers, priority) | Yes | No | **No gap** — these are section-envelope concerns. The `AtomicComposite` section carries them. |
| **`padding` / `backgroundColor`** | Yes | Yes (via Container) | **No gap** — Container already has `padding` and `backgroundColor`. |

### Data Binding in Atomic Context — Detailed Analysis

**How it works today (sections):**
The `game-detail-live.json` example shows the pattern:
```json
{
  "id": "scoreboard",
  "type": "ScoreboardHeader",
  "refreshPolicy": { "type": "sse", "channel": "{gameId}:linescore" },
  "dataBindings": {
    "bindings": [
      { "sourcePath": "$.homeTeam.score", "targetPath": "homeTeam.score" },
      { "sourcePath": "$.gameStatusText", "targetPath": "gameStatusText" }
    ]
  },
  "data": { "homeTeam": { "score": 110 }, "gameStatusText": "Q4 5:32" }
}
```
The `DataBindingResolver` receives an SSE/poll message, extracts `$.homeTeam.score` from it, and writes the value to `data.homeTeam.score`.

**How it works for AtomicComposite:**
The `data` for an `AtomicComposite` is `{ "ui": { "type": "Container", "children": [...] } }`. Data binding target paths would navigate into the atomic tree:
```json
{
  "id": "promo-banner",
  "type": "AtomicComposite",
  "refreshPolicy": { "type": "poll", "intervalMs": 60000 },
  "dataBindings": {
    "bindings": [
      { "sourcePath": "$.headline", "targetPath": "ui.children[1].content" },
      { "sourcePath": "$.imageUrl", "targetPath": "ui.children[0].src" }
    ]
  },
  "data": {
    "ui": {
      "type": "Container", "direction": "column",
      "children": [
        { "type": "Image", "src": "https://cdn.nba.com/promo.jpg" },
        { "type": "Text", "content": "Watch Tonight", "variant": "heading2" }
      ]
    }
  }
}
```

**Assessment**: This works with the existing `DataBindingResolver` if it supports array-index notation in target paths (`ui.children[1].content`). The current implementation uses dot-notation traversal — **array indexing support needs verification or a small extension**.

**Alternative — named element binding**: Instead of positional paths, atomic elements could use their `id` field as a binding key:
```json
"bindings": [
  { "sourcePath": "$.headline", "targetPath": "#headline-text.content" }
]
```
Where `#headline-text` resolves to the atomic element with `"id": "headline-text"`. This is more resilient to tree restructuring than positional indexing.

**Recommendation**: Support both. Positional paths work for simple cases. ID-based targeting (`#elementId.property`) is more robust for data-bound composites.

### Translation / i18n in Atomic Context

**Current schema support:** `DataBinding.stringKeys` maps target paths to translation keys. The `DataBindingResolver` already acknowledges these keys but defers actual resolution (noted as a prototype gap).

**For AtomicComposite:** The same `stringKeys` mechanism works — the translation key maps to a target path inside the atomic tree. Additionally, Text elements could carry a `stringKey` property directly:

```json
{ "type": "Text", "content": "Watch Tonight", "stringKey": "promo.watchTonight" }
```

The client resolves `stringKey` → localized string at render time, falling back to `content` if the key is unknown.

**For real-time bound fields:** When an SSE message updates a Text element's content via data binding, the `stringKeys` map tells the client to resolve the new value through the translation layer before rendering.

**Schema change needed:** Add optional `stringKey: string` property to `AtomicElement` (specifically relevant for Text and Button elements).

### Refresh Policy Use Cases

| Use Case | Refresh Strategy | Works with AtomicComposite? |
|---|---|---|
| **Static content** (PromoBanner, SectionHeader) | `"type": "static"` — no refresh | Yes — section envelope carries the policy. |
| **Periodic content** (ContentRail editorial picks, FollowingRail) | `"type": "poll", "intervalMs": 60000` | Yes — poll response replaces the entire atomic tree. |
| **Live data** (ScoreboardHeader during game) | `"type": "sse", "channel": "{gameId}:linescore"` | Yes — SSE messages are applied via data bindings to specific atomic properties (score text, status text). The tree structure stays stable; only bound values change. |
| **Partial section refresh** (refresh action targeting a section ID) | `refresh` action with `target: "section-id"` | Yes — the composition service re-serves the `AtomicComposite` JSON and the client replaces the tree. |

**Key insight**: For Tier 1 sections (stateless, no live data), refresh policy is either `static` or slow `poll`. No Tier 1 section currently uses SSE. Data binding complexity is a Tier 2+ concern. This means the atomic migration path is unblocked for Tier 1 without any data binding changes.

### Common UX Use Cases — Atomic Capability Matrix

| UX Use Case | Schema Mechanism | Atomic Support |
|---|---|---|
| **Tap → navigate to detail** | `actions: [{ trigger: "onTap", type: "navigate", targetUri: "nba://..." }]` | **Full** — Button and Image elements carry `actions[]`. Any element can be wrapped in a Container with an action. |
| **Impression tracking** | `actions: [{ trigger: "onVisible", type: "analytics", event: "section_viewed", impression: { dedup: "once-per-screen" } }]` | **Full** — any atomic element can carry `onVisible` analytics actions with impression policy. AtomicRouter needs to wire up `IntersectionObserver` (web) / visibility callbacks (Android). |
| **Tap → mutate screen state** (e.g., toggle, tab switch) | `actions: [{ trigger: "onTap", type: "mutate", target: "stateKey", operation: "set", value: "x" }]` | **Full** — Button elements fire mutate actions. Conditional elements react to screen state changes. TabGroup remains a section; this covers simpler toggle scenarios. |
| **Conditional rendering** (show/hide based on state) | `Conditional` element: evaluates `condition` against `screenState`, renders `trueChild` or `falseChild` | **Full** — Conditional is one of the 8 atomic types. Enables server-controlled A/B variations within a tree. |
| **Pull-to-refresh** | `refresh` action dispatched by parent scroll container | **Indirect** — the atomic tree lives inside a section container that owns pull-to-refresh. Not a concern for atomic elements. |
| **Toast / dismiss** | `actions: [{ trigger: "onTap", type: "toast", message: "Copied!" }]` or `type: "dismiss"` | **Full** — action types are platform-level, not renderer-level. |
| **Deep link fallback** | `targetUri` + `webUrl` on navigate actions | **Full** — navigate actions on atomic elements carry both fields. |
| **Form input** | FormField stateKey binding, validation, submit | **Not applicable** — Forms stay as sections (Tier 3). Atomic elements are not form controls. |
| **Live score updates** | SSE + data binding to specific properties | **Works** for AtomicComposite via section-level data bindings. Not needed for Tier 1 (no live sections). |
| **Dark mode / theming** | Currently literal colors; needs semantic tokens | **Same gap as sections** — both layers need token support. See Figma integration section below. |

---

## Figma Design System Integration

The NBA app already has a well-structured design system in Figma with typed typography variants (e.g., `nba/display-xxl`, `nba/headline-10`) referenced by both Android (via `NbaTypography.kt`, `NbaColors.kt`, `NbaShapes.kt`) and Apple (via `Color+Extensions.swift`, `Font+NBA.swift`) codebases. The SDUI atomic layer creates a natural bridge between Figma components and rendered output.

### Design Token Alignment

The mobile audit revealed consistent token clusters across platforms:

| Token Category | Native Values | Schema Mapping Today | Proposed Alignment |
|---|---|---|---|
| **Typography** | Android: `NbaTypography.headline1..10`, `body`, `label`, etc. Apple: `Font.nba(.headline10)`, etc. Figma: `nba/headline-10` | `TextVariant` enum: `heading1..3`, `body`, `bodySmall`, `caption`, `label`, `score` | Expand `TextVariant` to match Figma/native taxonomy. Map `nba/headline-10` → `heading1`, `nba/body-md` → `body`, etc. Publish the mapping as a shared token file. |
| **Colors** | Android: `surface.primary`, `text.onSurface`, `live`, `positive`, `negative`. Apple: semantic color extensions with server-name mapping. | Literal hex strings (`backgroundColor`, `color`) | Add semantic token syntax: `"color": "text.primary"` alongside literal values. Clients resolve tokens via their existing theme map. Figma variables map 1:1. |
| **Spacing** | Both platforms: 4, 8, 16, 20, 32 dp cluster | Integer dp values (`gap`, `padding`) | Keep integers for now. Optional: allow named spacing tokens (`"gap": "md"` → 16dp) in a future schema version. |
| **Corners / Shapes** | Android: 4dp default, 12dp sheets, 24dp tracks. Apple: similar. | Not in schema | Add optional `cornerRadius` to Container. Values are integers; clients can also accept semantic names. |

### Figma → Schema Validation Pipeline

The dual-layer architecture enables three levels of Figma-to-code validation:

#### Level 1: Token Contract Validation (CI-automatable)
- Export Figma design tokens (via Figma Variables API or Tokens Studio plugin) as a JSON token file.
- Validate that every `TextVariant`, `ButtonVariant`, color token, and spacing value used in the SDUI schema and example JSON files exists in the Figma token export.
- **Catches**: Schema references to tokens that don't exist in Figma (or vice versa). Typography variant drift across platforms.

```
figma-tokens.json:
  { "typography": { "heading1": "nba/display-xxl", ... },
    "colors": { "text.primary": { "light": "#1A1A2E", "dark": "#F5F5F5" }, ... } }

CI check:
  For each AtomicComposite example JSON:
    For each Text element with variant V:
      Assert V exists in figma-tokens.typography
    For each element with color C:
      Assert C exists in figma-tokens.colors OR is a valid hex literal
```

#### Level 2: Component Structure Validation (semi-automated)
- Figma components have a known structure: auto-layout direction, padding, gap, child order, text styles.
- Figma's REST API (or a plugin) can export a component's layout tree as structured JSON.
- Compare Figma component structure against the `AtomicComposite` JSON template for the same UI pattern.

```
Figma "SectionHeader" component:
  AutoLayout(row, spaceBetween, padding: 16)
    Text("Title", style: nba/headline-10)
    Spacer
    Text("See All", style: nba/body-md, color: text.link)

AtomicComposite equivalent:
  Container(row, spaceBetween, padding: {start:16, end:16, top:16, bottom:16})
    Text(content: "Title", variant: heading3)
    Spacer()
    Button(label: "See All", buttonVariant: text, actions: [...])

CI check:
  Assert direction matches (row ↔ row)
  Assert padding values match token resolution
  Assert child count matches
  Assert text styles map to correct TextVariant
```

This catches layout drift — when a designer changes the SectionHeader layout in Figma but the `AtomicComposite` template hasn't been updated (or vice versa).

#### Level 3: Visual Regression via Screenshot Comparison (manual + CI)
- Render `AtomicComposite` JSON through the platform renderers (Android Compose Preview, Web Storybook/Chromatic).
- Compare rendered screenshots against Figma frame exports (via Figma API image export endpoint).
- Use pixel-diff tools (e.g., Percy, Chromatic, or custom threshold-based diffing) to detect regressions.
- **Catches**: Subtle rendering differences (font metrics, padding rounding, image sizing) that structural checks miss.

### Figma Component ↔ Architecture Layer Mapping

| Figma Artifact | SDUI Architecture Layer | Validation Approach |
|---|---|---|
| **Primitives** (Text, Button, Image, Icon, Divider) | `AtomicElement` types | Token contract: variant/style names match Figma text styles and component variants. |
| **Simple compositions** (SectionHeader, PromoBanner, ErrorState) | `AtomicComposite` JSON templates | Structure validation: Figma auto-layout tree matches atomic element tree. |
| **Complex domain components** (BoxscoreTable, GamePanel, Scoreboard) | Section renderers (existing) | Visual regression only: renderer output compared against Figma frame screenshots. Structural validation not applicable — client owns the layout. |
| **Design tokens** (colors, typography, spacing, elevation) | Schema enums + token files | Token contract: shared token export is the source of truth for both Figma and schema. |
| **Page layouts** (feed, game detail, standings) | `Screen.sections[]` composition | Composition validation: section order, types, and layout hints in example JSON match Figma page frames. |

### i18n and Figma Text Layers

Figma text layers use placeholder content ("Watch Tonight", "See All"). For i18n validation:
- Figma text layers should be annotated with their `stringKey` (e.g., via a Figma plugin that reads a `stringKey` property on text nodes).
- The SDUI token export includes the `stringKey` → default-locale mapping.
- CI validates that every `stringKey` in AtomicComposite templates has a corresponding entry in the translation catalog AND a matching Figma text node annotation.
- This prevents: shipping a new promo banner where the Figma design says "Subscribe Now" but the translation key maps to "Subscribe Today" or doesn't exist at all.

### Where This Changes the Schema

Based on this analysis, the following schema additions support Figma alignment and behavioral completeness:

| Addition | Where | Purpose |
|---|---|---|
| `stringKey?: string` | `AtomicElement` | Direct i18n key on Text/Button elements, resolved client-side. |
| `cornerRadius?: integer` | `AtomicElement` (Container) | Matches Figma corner radius on frames. |
| `elevation?: integer` | `AtomicElement` (Container) | Matches Figma drop-shadow / elevation tokens. |
| `tokenRef` convention | Color/spacing values | Allow `"color": "text.primary"` alongside literal hex. Clients resolve via theme token map. |
| `analyticsId?: string` | `AtomicElement` | Observability: ties atomic elements to Figma component names for debugging. |
| `section?: Section` | `AtomicElement` (SectionSlot) | Full section object for bidirectional bridge. See Phase 5. |
| Array-index + ID-based binding | `DataBindingPath.targetPath` | `ui.children[1].content` or `#elementId.content` for atomic tree binding. |

### Workflow Summary

```
┌─────────────┐    tokens.json     ┌──────────────┐    schema.json    ┌────────────────┐
│   Figma DS   │ ──────export────→ │  Token File  │ ──────validate──→ │  SDUI Schema   │
│  (source of  │                   │  (colors,    │                   │  (AtomicElement │
│   truth)     │                   │  typography, │                   │   enums,        │
└──────┬───────┘                   │  spacing)    │                   │   token refs)   │
       │                           └──────────────┘                   └───────┬─────────┘
       │ component export                                                     │ codegen
       ▼                                                                      ▼
┌─────────────┐    structure diff   ┌──────────────┐                  ┌────────────────┐
│ Figma Frame │ ────────────────→  │  CI Pipeline │ ←────────────────│ Platform Code  │
│   (auto-    │                    │  (token +    │    screenshot     │  (Android/Web/ │
│   layout    │                    │  structure + │    comparison     │   iOS renders) │
│   tree)     │                    │  visual)     │                   └────────────────┘
└─────────────┘                    └──────────────┘
```

---

## Open Questions

1. ~~**Gradient on Container**~~ — **Resolved**: `backgroundGradient: { colors, direction }` added to AtomicElement. PromoBanner uses it. SubscribeBanner reclassified to Tier 3 (platform billing SDK dependency) so no longer relevant here.

2. **HeroPanel pull-through** — HeroPanel is the item delegate inside ContentRail. If ContentRail becomes an atomic `ScrollContainer`, its children (HeroPanel items) also need to be atomic. This may pull HeroPanel from Tier 2 into Tier 1 sooner than planned.

3. **Action threading** — The `onAction` callback in `SectionRouter` needs to flow into `AtomicRouter` identically. Verify `navigate`, `mutate`, `track`, `submit`, and `refresh` action types all work when dispatched from an atomic context.

4. **Atomic template registry** — For content items repeated N times in a ScrollContainer (e.g., ContentRail cards), should the server send the full atomic tree for every item, or define a named template once and reference it? This is a future optimization, not blocking for Tier 1.

5. ~~**`SduiSection` rename**~~ — **Resolved**: `SduiSection` stays as-is. No rename. The `Section` naming is deeply ingrained and only the new `Atomic` layer gets the `Atomic` prefix.
