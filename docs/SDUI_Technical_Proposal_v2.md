# Technical Proposal

**A developer-oriented proposal for SDUI architecture, runtime behavior, and implementation patterns aligned to the current requirements baseline.**

---

> **Scope note (normative):** The requirements summary is the source of truth for commitments. This proposal describes technical implementation direction and examples aligned to those commitments.

## Revision History

| Date | Summary |
|---|---|
| 2026-03-30 | Doc consistency audit. §10 requirement status updated: Request context envelope Gap → Built, Experiment conflict handling Partial → Built (ADR-006 Accepted), Internationalization Gap → Built (section-level stringTable). |
| 2026-03-24 | Doc consistency audit. §2c token alignment table expanded (4 → 8 categories: added font weight, backgrounds, elevation, button variants). Requirement status updated: Layout manager Gap → Partial (ADR-008 accepted), Impression semantics Gap → Partial (ADR-009 accepted). `FormRenderer` → `Form` aligned with schema enum across AGENTS.md and requirements summary. |
| 2026-03-14 | Added §2b (Section vs. Atomic Decision Framework — decision tree, rationale for lifecycle/SDK/state boundaries, concrete examples: GamePanel, SubscribeHero, AdSlot, BoxscoreTable, TabGroup). Added §2c (Figma Design System Integration — token alignment, three-level CI validation pipeline). |
| 2026-03-13 | Atomic rendering layer. Updated §2 (dual-layer model: semantic sections + atomic primitives coexisting via AtomicComposite). Added §2a (AtomicElement types, AtomicComposite bridge, Grid vs. Section Decision Tree). Updated §8 (AtomicRouter + 10 atomic renderers per platform). Added §9s (atomic layer performance contract). Updated §10 (atomic rendering layer status). |
| 2026-03-13 | Added offline/degraded connectivity strategy (9r) with ADR-010 reference. Stale-while-offline approach using platform HTTP cache, staleness UX per `cacheability` class, fire-and-forget local queue. |
| 2026-03-13 | Replaced `variant`/`backgroundImageUrl` on `GamePanelData` with server-driven `displayConfig` (`GamePanelDisplayConfig`). Introduced shared `Background` union type (solid/gradient/image) reused by `AtomicElement`, `SubscribeHeroData`, `SubscribeBannerData`. Three layout presets (`standardConfig`, `featuredConfig`, `scoreboardConfig`) replace client-side variant branching. |
| 2026-03-12 | Merged `FeaturedGamePanel` into `GamePanel` with `variant` discriminator. `FeaturedGamePanelData` removed from schema; `GamePanelData` gains `variant`, `backgroundImageUrl`, `badgeText`, `visualLabel` fields. Server composers emit `type: "GamePanel"` with `variant: "featured"`. Android and Web renderers branch on variant. `FeaturedGamePanelRenderer` deleted on both platforms. Section type count: 20 → 18. |
| 2026-03-12 | Server-control gaps closed: `SectionLayoutHints` and `SectionStates` added to schema + codegen. Web client: `SectionErrorBoundary`, `SectionSkeleton`, `useImpressionTracking`, `useAnalyticsContext` built. Server: `sectionStates` emitted on live sections. ADR-008 accepted (Option C), ADR-009 accepted. Bug fixes: `interactive` contentType enum, platform header threading (`X-Platform` required from clients, no server default), silent deserialization failures now logged on Android. |
| 2026-03-12 | Prototype sync. Renderer table updated — BoxscoreTable, Form, SectionHeader, FollowingRail, SeasonLeadersTable now Built on Web and Android. Added image fallback pattern (section 8a). Updated requirement status for tabular data and forms. |
| 2026-03-11 | ErrorState added to renderer table. Error handling status updated (Gap → Built for ErrorState, runtime `sectionStates` planned). Client-side visibility expressions evaluated and deferred — server-side composition handles section show/hide. |
| 2026-03-04 | Added `parentUri` to Screen response contract and example. Added client URI resolution convention. |
| 2026-03-04 | Added tabular data sections and forms. New semantic types (`BoxscoreTable`, `Form`) in schema design (section 2). Parameterized refresh on actions (section 4). Sort and form state patterns (section 5). Platform coverage update (section 8). Gap 9q. Requirement status updates. Appendix C boxscore response example. |
| 2026-02-27 | Replaced `entitlements` with `device` in request envelope (governance, 9o, Appendix A). Aligned analytics field names (`event`/`params`) and mutate field names (`target`/`operation`/`value`) with requirements summary. Moved `schemaVersion` to `meta` object. Added `onBlur` trigger. |
| 2026-02-25 | Established platform-aware composition as settled architectural position: shared schema, shared data pipeline, per-platform-family composition. Renamed `fallbackUrl` → `webUrl` in action contract (section 4). Updated platform coverage (section 8). |
| 2026-02-25 | Added `stringKeys` to binding JSON examples in section 3 and Appendix B. Added JSON snippet with explanation to i18n section (9p). Added revision history. |
| 2026-02-24 | Added i18n section (9p) with server-resolved default + `stringKeys` on data bindings. Removed `layoutHints` from schema and appendix examples (moved to ADR-008 for evaluation). Added locale transport policy to 9o and 9p. |
| 2026-02-20 | Initial v2 — full technical proposal aligned with requirements baseline and ADR-driven decisions. Covers schema, data binding, action system, screen state, codegen, platform coverage, and gap analysis (9i–9o). Appendix A/B with typed request envelope and expanded response examples. |

---

## Current Governance (This Document)

- Requirements define **what** must be true. ADRs capture **why** and final cross-functional decisions.
- This proposal is technical and implementation-focused.
- Areas mapped to pending ADRs are directional until cross-functional approval is complete.

Current decisions reflected here:

- Composition ownership: SDUI composer/aggregator is the semantic source of truth.
- **Platform-aware composition: shared schema and data pipeline; per-platform-family composition. Not an ADR — the only practical architecture for phone, tablet, web, and TV.**
- Action precedence: nested/subsection > section > screen-default.
- Navigate actions: `targetUri` for native deeplinks, `webUrl` for web — both are first-class, not primary/fallback.
- Experiment model: client-authoritative assignment via Amplitude SDK; server trusts assignments and uses for composition. Server kill switch can reject disabled variants.
- Transport policy: support GET and POST by route context and cacheability needs.
- Ad boundary: auction/targeting is delegated; SDUI carries ad placement contract.
- Caching direction: section-first caching with optional screen snapshot caching.
- Entitlement/restriction resolution: server-authoritative. The client provides device context via `device` in the request envelope; the composition service resolves entitlements and restrictions server-side.

---

## What SDUI Is

Server-Driven UI is an architecture where the server controls what sections appear on screen, their order, their data, and interaction behavior. Clients render the contract with native platform UI systems (SwiftUI, Jetpack Compose, React).

The server owns **composition**. Platform teams own **rendering quality** (native motion, accessibility, focus/gesture behavior, platform idioms).

This is not a shared runtime or webview wrapper. It is a shared schema + composition contract with native renderers per platform.

### Platform-Aware Composition (Settled Position)

The SDUI schema is universal — all platforms share the same vocabulary of section types, action types, and data models. However, the composition layer produces **platform-family-specific responses**. A tvOS 10-foot UI, a phone, and a web browser have fundamentally different information density, interaction models, and layout requirements. Serving identical responses to all platforms forces a lowest-common-denominator experience.

**Architecture:**

```
┌─────────────────────────────────────────┐
│            SDUI Schema                   │
│  (shared vocabulary, codegen, contract)  │
└──────────────────┬──────────────────────┘
                   │
     ┌─────────────┼─────────────┐
     ▼             ▼             ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│  Mobile  │ │   Web    │ │    TV    │
│ Composer │ │ Composer │ │ Composer │
└─────┬────┘ └─────┬────┘ └────┬─────┘
      │            │            │
      ▼            ▼            ▼
  Platform-tailored responses using
  the same section types and structure
```

**What is shared across all platforms:**

- Schema definitions (JSON Schema) — the universal contract
- Codegen (typed models per platform from one schema)
- Upstream data fetching and transformation — one integration pipeline
- Section-type semantics — a `GamePanel` means the same thing everywhere
- Action and data binding structure

**What differs per platform family:**

- Which sections are composed and in what order
- Information density and layout intent
- Action URIs (`targetUri` for native deeplinks, `webUrl` for web)
- Image dimensions and density parameters
- Interaction triggers (touch vs. D-pad vs. hover)

The client identifies its platform family via the request envelope (`platform.name`, `platform.deviceClass`) or the `X-Platform` header (values: `android`, `ios`, `web`). The composition service routes to the appropriate composer. In the prototype, the `X-Platform` header controls platform-specific field values — for example, Form `layout` is `horizontal` for web (inline controls) and `vertical` for mobile (stacked controls). The cost is additional server-side composition logic — this is acceptable and is where the investment should go. The alternative (one response for all platforms) produces a mediocre experience everywhere.

---

## 1. Core Architecture

### Data Flow

```
Client App -> Composition Service -> Upstream Data/Eligibility/Experiment Systems
```

1. Client requests a screen with schema version and request context.
2. Composition service assembles sections, resolves variants/context, and returns a schema-compliant response.
3. Client router maps sections to native renderers.
4. Runtime systems execute refresh policy, data bindings, and action chains.

### Server Controls vs Client Controls


| Server controls                     | Client controls                     |
| ----------------------------------- | ----------------------------------- |
| section composition and ordering    | native component rendering          |
| section payload fields and defaults | platform gesture/focus handling     |
| refresh policies and bindings       | platform networking primitives      |
| action definitions and sequencing   | integration with nav/analytics SDKs |
| experiment/variant composition      | platform accessibility mechanics    |


### Composition Ownership Requirement

SDUI semantic composition is owned by the SDUI composer/aggregator. CoreAPI-derived composition can be used as transitional input, but is not the long-term semantic owner.

Reference: ADR-002

### Response Hierarchy

Every response follows `Screen -> Section -> Component`, where each section can carry data, refresh policy, bindings, actions, and subsections.

```json
{
  "meta": { "schemaVersion": "1.1" },
  "screen": {
    "id": "game-detail",
    "parentUri": "nba://scoreboard",
    "sections": [
      {
        "id": "scoreboard-001",
        "type": "GamePanel",
        "data": { "displayConfig": { "scoreTextStyle": "compact" }, "gameId": "0022500384", "...": "..." },
        "refreshPolicy": { "type": "sse", "channel": "{gameId}:linescore" },
        "dataBindings": { "bindings": [ { "sourcePath": "$.home.score", "targetPath": "home.score" } ] },
        "actions": [ { "trigger": "onTap", "type": "navigate", "targetUri": "nba://game/0022500384/boxscore" } ]
      }
    ]
  }
}
```

### Client URI Resolution Convention

Navigate action URIs use the `nba://` scheme.  Clients resolve these to
server endpoints using a simple convention:

```
nba://{path}  →  GET /sdui/{path}
```

Special case: `nba://game/{id}` → `GET /sdui/game-detail/{id}?gameState=live`
(preserves backward compatibility with the existing game-detail endpoint).

This convention means new screens require **no client code changes** —
adding a server endpoint and including its URI in navigation items or
action targets is sufficient.

---

## 2. Schema Design

### Key Schema Decisions

- Schema is the contract source of truth.
- Schema defines a dual-layer model: **semantic section types** (GamePanel, BoxscoreTable, TabGroup, etc.) for domain-specific rendering with client-owned state, and **atomic element types** (Container, Text, Image, etc.) for server-composed generic layouts. The two layers coexist via the `AtomicComposite` bridge section type. See §2a.
- Codegen generates typed models only, not UI code.
- Schema evolution is additive-first and version-aware.
- Unknown section/action types must degrade gracefully (skip/no-op).
- **Subsection actions are required** at nested component level.
- **Request context is contract input** and must be typed.
- **Tabular data uses semantic per-domain types** — server describes *what* (domain-typed data), clients own *how* (rendering, sort UX, frozen columns). See *Tabular Data and Form Sections* below.

### Schema Fragment Example

```json
{
  "$id": "https://nba.example/sdui-schema.json",
  "definitions": {
    "Section": {
      "type": "object",
      "required": ["id", "type", "data"],
      "properties": {
        "id": { "type": "string" },
        "type": { "type": "string" },
        "data": { "type": "object" },
        "refreshPolicy": { "$ref": "#/definitions/RefreshPolicy" },
        "dataBindings": { "$ref": "#/definitions/DataBindings" },
        "actions": {
          "type": "array",
          "items": { "$ref": "#/definitions/Action" }
        },
        "subsections": {
          "type": "array",
          "items": { "$ref": "#/definitions/Subsection" }
        }
      }
    }
  }
}
```

### Tabular Data and Form Sections

Tabular stat views (boxscore, roster, standings) share UX patterns — frozen first column, horizontal scroll, sortable columns, aggregation rows — but each has a distinct domain-specific data shape. The schema uses **semantic per-domain section types** rather than a generic `DataTable`.

**Key decision factors:**

- **Semantic types over generic DataTable.** Different tables have genuinely different data shapes — a boxscore row (player + game stats) is fundamentally different from a roster row (player + bio/contract). Forcing different data into a generic container creates implicit contracts that break silently. Semantic types provide compile-time safety, meaningful analytics events, and platform-specific rendering freedom.
- **Generic Form is the correct exception.** Form pickers genuinely share shape regardless of domain — a season picker and a position picker have the same structure (`label`, `options[]`, `stateKey`). `Form` is correctly generic; tables are not.
- **Client-side sort.** Payloads are small (≤15 rows). Sort via `mutate` action updating screen state. No server round-trip. Sort state survives live poll refreshes.
- **Clients own rendering reuse internally.** Client teams share a `BaseDataTable` component across `BoxscoreTableRenderer`, future `RosterTableRenderer`, etc. This reuse is a client implementation detail invisible to the schema.

**`BoxscoreTableData`** — domain-typed player statistics with `teamTotals` (frozen bottom row excluded from sort), `emptyMessage` (pre-game states), and `additionalStatistics` (forward-compatible escape hatch for new stats without schema changes). See [Appendix C](#appendix-c-boxscore-screen-response-example) for a full composed example.

**`FormData`** — extensible settings fields with `picker` (dropdown), `segmented` (button group), `toggle` (switch), `datePicker`, and `text` field types. Field changes accumulate in `Screen.state`; submit fires the `refresh` action with `paramBindings` resolved from current state values. See [Appendix D](#appendix-d-form-section-response-example) for a full example.

### 2a. Atomic Element Layer

The schema defines a second layer of **atomic element types** alongside the semantic section types described above. While semantic sections carry domain-typed data and rely on client-owned renderers (sort state, frozen columns, form submission), atomic elements are **server-composed primitives** rendered generically by an `AtomicRouter` with no client-side business logic.

**AtomicElement types (10 in schema enum — 9 rendering primitives + 1 bridge):**

| Type | Purpose | Key properties |
|---|---|---|
| **Container** | Flex layout wrapper | `children`, `direction`, `padding`, `gap`, `alignment`, `background` |
| **Text** | Styled text | `content`, `variant`, `color`, `maxLines`, `alignment` |
| **Image** | Remote image | `src`, `alt`, `width`, `height`, `fit` |
| **Button** | Interactive element | `label`, `actions`, `variant` |
| **Spacer** | Fixed space | `width`, `height` |
| **Divider** | Line separator | `orientation`, `thickness`, `color` |
| **ScrollContainer** | Scrollable region | `children`, `direction`, `paging`, `snapAlignment`, `gap` |
| **Conditional** | State-driven branching | `condition`, `trueChild`, `falseChild` |
| **DisplayGrid** | Display-only text grid | `columns`, `rows`, `headerVariant`, `cellVariant`, `striped` |
| **SectionSlot** | Embed a full section | `section` (a complete Section object) |

**Bridge mechanism — `AtomicComposite` section type:**

The `AtomicComposite` section type bridges the section and atomic layers. When `SectionRouter` encounters `type: "AtomicComposite"`, it delegates rendering to `AtomicRouter`. The section's `data` contains:

- `ui` — the root `AtomicElement` tree (rendering instructions)
- `content` — reserved for domain data used with data-binding support

`SectionSlot` provides the reverse bridge: an atomic tree can embed a full section renderer (e.g., an `AdSlot` inside an atomic layout), enabling bidirectional delegation between the two layers. A recursion guard (`MAX_SECTION_SLOT_DEPTH = 2`) prevents infinite nesting.

**Grid vs. Section Decision Tree:**

> **Why "DisplayGrid" and not "DataTable"?** The Tabular Data section above explicitly rejected a generic `DataTable` at the section level because different tables have genuinely different data shapes. The atomic `DisplayGrid` is a deliberately different primitive: a **display-only, non-interactive, server-ordered grid of text cells**. The name makes the non-interactive boundary self-documenting.

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

### 2b. Section vs. Atomic Decision Framework

The dual-layer architecture requires a principled decision for every UI surface: should it be a server-composed atomic tree or a client-owned semantic section? The decision is driven by three factors: **network-driven lifecycle**, **platform SDK integration**, and **client-owned interaction state**.

**Decision tree — when a section must remain native:**

```
New UI surface needed?
├─ Manages network-driven lifecycle?            → Section
│  (connects to Ably/poll, selects UI variant
│   based on live data state)
├─ Integrates a platform-native SDK?            → Section
│  (billing, ads, auth — SDK owns view lifecycle)
├─ Has complex client-owned interaction state?   → Section
│  (sort, frozen scroll sync, form input,
│   tab selection, pagination)
├─ Nests other sections as children?             → Section
│  (TabGroup — section container)
└─ None of the above?                            → AtomicComposite
   (server-composed, no app release needed)
```

**Why these boundaries exist — concrete examples:**

| Factor | Example | Why it cannot be atomic |
|---|---|---|
| **Network-driven lifecycle** | **GamePanel** — as a game progresses, the client connects to Ably SSE or switches to polling, determines the visual variant (pre-game → in-game → post-game) from live `gameStatus` data, and renders the appropriate layout. The variant selection, channel subscription, and reconnection logic are lifecycle concerns the server cannot drive after the initial response. | An atomic tree is a static render instruction. It cannot subscribe to a data source, evaluate state transitions over time, or select among layout variants based on ongoing network events. |
| **Platform SDK integration** | **SubscribeHero / SubscribeBanner** — target integration is In-App Purchase (Google Play Billing Library on Android, StoreKit 2 on iOS). The purchase flow requires a state machine (loading products → purchasing → verifying → success/failure), entitlement checks before rendering CTA text, and localized pricing from the store — not the server. The billing SDK owns the transaction lifecycle. | The SDK manages its own view lifecycle, authentication, and platform-specific purchase flow. An atomic `Button` cannot host a billing sheet or verify purchase receipts. |
| **Platform SDK integration** | **AdSlot** — target integration is Google Ad Manager (GAM). `AdManagerAdView` owns its own view lifecycle (load, refresh, destroy), consent management (UMP/TCF), MRC-compliant viewability tracking, fill-rate fallback UI, and timed refresh cadence. | An atomic `Container` cannot host a native ad view or participate in the ad SDK's lifecycle callbacks. |
| **Client-owned interaction state** | **BoxscoreTable** — frozen column + horizontal scroll sync, client-side sort with `remember{}`, starter/bench divider logic, DNP player handling, totals row. The sort and scroll coordination are tightly coupled local UI state. | Atomic primitives have no `remember{}`/`useState` equivalent. Coordinated scroll positions and sort state require client-owned render logic. |
| **Section container** | **TabGroup** — tab selection fires `mutate` updating `screenState`, which drives which child sections render. It nests other sections. | Atomic trees cannot contain section children (except via `SectionSlot`, which is the escape hatch for embedding one section inside an atomic layout — not for nesting arbitrary section trees). |

**When a section can migrate to atomic:**

A section is a migration candidate when it is stateless (no `remember{}`, no `mutableStateOf`, no scroll state), has no platform SDK dependencies, does not nest other sections, and its visual variants are deterministic from server-sent data fields alone. The server emits the correct atomic tree directly — the client's `AtomicRouter` paints it without branching.

Migrated sections (9 types): ErrorState, SectionHeader, PromoBanner, ContentRail, FollowingRail, HeroPanel, StatLine, VideoCarousel, NbaTvSchedule — server-composed `AtomicComposite` with no client renderers. Schema definitions pruned; only the 8 permanent section types + `AtomicComposite` remain in the `Section.type` enum.

### 2c. Figma Design System Integration

The atomic layer creates a natural bridge between the NBA Figma design system and rendered output. The NBA app already has typed design tokens in Figma (e.g., `nba/display-xxl`, `nba/headline-10`) referenced by both native codebases (Android `NbaTypography.kt`, iOS `Font+NBA.swift`). Atomic primitives reference the same token vocabulary.

**Token alignment:**

| Category | Figma Token | Schema Mapping | Alignment Path |
|---|---|---|---|
| Typography | `nba/headline-10` | `TextVariant` enum (`heading1`, `body`, etc.) | Expand `TextVariant` to match Figma taxonomy; publish shared mapping file |
| Font weight | `nba/weight-bold` | `TextWeight` enum (`regular`, `medium`, `semibold`, `bold`) | Direct match to Figma font weight tokens |
| Colors | `text.primary`, `surface.primary` | Literal hex or semantic token (`"color": "text.primary"`) | Clients resolve semantic tokens via existing theme map; Figma variables map 1:1 |
| Backgrounds | `surface.card`, `surface.overlay` | `Background` union (solid color, `BackgroundGradient`, `BackgroundImage`) | Gradient stops and image overlays map to Figma fill layers |
| Spacing | 4/8/16/20/32 dp cluster | Integer dp values (`gap`, `padding`) | Keep integers; optionally accept named tokens (`"gap": "md"` → 16dp) later |
| Corners | 12dp sheets, 4dp default | `cornerRadius` on Container and Image | Direct match to Figma corner radius |
| Elevation | `shadow/sm`, `shadow/md` | `elevation` on `GamePanelDisplayConfig` | Map Figma shadow tokens to integer dp elevation values |
| Button variants | `button/primary`, `button/secondary` | `ButtonVariant` enum (`primary`, `secondary`, `tertiary`, `text`) | Figma component variants map 1:1 to schema enum; CI validates fill, border, text color per variant |

**Three-level CI validation pipeline:**

1. **Token contract** — export Figma design tokens as JSON; validate every `TextVariant`, `ButtonVariant`, color, and spacing value in the schema and examples exists in the export. Catches variant drift.
2. **Component structure** — export Figma component auto-layout trees via REST API; compare direction, padding, gap, child count, and text styles against the corresponding `AtomicComposite` template JSON. Catches layout drift.
3. **Visual regression** — render `AtomicComposite` JSON through platform renderers (Compose Preview, web Storybook); compare screenshots against Figma frame image exports. Catches rendering differences that structural checks miss.

This pipeline means a designer can change a Figma component and CI detects whether the `AtomicComposite` template (or vice versa) needs updating — closing the design-to-code gap that atomic primitives are uniquely positioned to bridge.

---

## 3. Data Binding System

The server declares how section fields stay fresh after initial render.

### Refresh Policy Types


| Type     | Mechanism           | Typical use           |
| -------- | ------------------- | --------------------- |
| `static` | no refresh          | editorial content     |
| `poll`   | interval HTTP fetch | slower-changing stats |
| `sse`    | realtime channel    | live score/clock      |


### Field-Level Binding Example

```json
"dataBindings": {
  "bindings": [
    { "sourcePath": "$.homeTeam.score", "targetPath": "homeTeam.score" },
    { "sourcePath": "$.awayTeam.score", "targetPath": "awayTeam.score" },
    { "sourcePath": "$.gameStatusText", "targetPath": "gameStatusText" },
    { "sourcePath": "$.period", "targetPath": "period" }
  ],
  "stringKeys": {
    "gameStatusText": "game_status_text"
  }
}
```

`stringKeys` is optional — omit when no bound fields need client-side translation. See [9p. Internationalization](#9p-internationalization-i18n).

Binding semantics must be behaviorally identical across Android, iOS, and Web through shared fixtures and contract tests.

---

## 4. Action System

Actions are supported at three scopes:

- screen defaults (optional)
- section actions
- nested/subsection actions

### Triggers


| Trigger       | Fires when                |
| ------------- | ------------------------- |
| `onTap`       | touch/click/remote select |
| `onLongPress` | hold gesture              |
| `onVisible`   | enters viewport           |
| `onSwipe`     | directional swipe         |
| `onFocus`     | TV focus enters item      |
| `onBlur`      | TV focus leaves item      |


### Action Types


| Type        | Purpose              | Key fields                                 |
| ----------- | -------------------- | ------------------------------------------ |
| `navigate`  | route/deeplink       | `targetUri` (native deeplink), `webUrl` (web equivalent) |
| `fireAndForget` | fire beacon          | `event`, `params`, `destinations`          |
| `mutate`    | update screen state  | `target`, `operation`, `value`             |
| `dismiss`   | close overlay/screen | `target`                                   |
| `refresh`   | force fetch          | `target`, optional `endpoint`, optional `paramBindings` |
| `toast`     | show notification    | `message`, `duration`                      |


### Parameterized Refresh

The `refresh` action type supports two optional fields for server-driven settings interactions:

- **`endpoint`** (string, optional) — target URL for the refresh. Defaults to the current screen endpoint if omitted.
- **`paramBindings`** (object, optional) — map of query parameter name → screen state key. At execution time, the client resolves each state key's current value and appends the result as query parameters to the refresh endpoint.

This enables Form submit buttons to say "refresh the screen with `season={state.season}&seasonType={state.seasonType}`" without any client-side knowledge of what parameters mean. Existing `refresh` actions with only `target` continue to work unchanged — the extension is backward-compatible.

```json
{
  "trigger": "onTap",
  "type": "refresh",
  "endpoint": "/sdui/stats/leaders",
  "paramBindings": {
    "season": "season",
    "seasonType": "seasonType"
  }
}
```


### Precedence and Composability

- Precedence rule: nested/subsection > section > screen-default.
- A single trigger can execute multiple actions in sequence.
- Fire-and-forget first is recommended before navigation/dismiss

Reference: ADR-005

### Failure Behavior

Each action carries two optional fields governing failure semantics:

- **`onFailure`** (`halt` | `continue` | `silent`) — sequence behavior on failure. Clients apply per-type defaults when absent (navigate → halt, fireAndForget/dismiss/toast → silent, mutate/refresh → continue).
- **`failureFeedback`** (`{ message?: string, style?: "snackbar" | "toast" | "inline" }`) — server-provided error message. Client falls back to generic localized string when absent.

The executor resolves failure policy per action:

```
policy = action.onFailure ?? default_for(action.type)
```

| Policy | Behavior |
|---|---|
| `silent` | Swallow — no user feedback, sequence continues |
| `continue` | Log warning, apply side effect (e.g., stale indicator), proceed |
| `halt` | Show error feedback (server message or fallback), stop sequence |

Navigate success also halts — navigation takes over the screen. Already-fired actions are committed (no rollback).

```
trigger fires → execute actions[0..N] in order
  │
  for each action[i]:
    resolve policy = action.onFailure ?? default_for(action.type)
    execute action[i]
    │
    ├─ success + navigate → HALT (navigation takes over)
    ├─ success + other   → continue to [i+1]
    └─ failure:
         ├─ silent   → swallow → continue
         ├─ continue → log, side-effect → continue
         └─ halt     → show feedback → HALT
```

### Precedence Example

```json
{
  "screenDefaults": {
    "actions": [{ "trigger": "onTap", "type": "fireAndForget", "event": "screen_tap" }]
  },
  "section": {
    "actions": [{ "trigger": "onTap", "type": "navigate", "targetUri": "nba://game/0022500384" }],
    "subsections": [
      {
        "id": "home-team-hotspot",
        "actions": [
          { "trigger": "onTap", "type": "fireAndForget", "event": "home_team_tap" },
          { "trigger": "onTap", "type": "navigate", "targetUri": "nba://team/1610612752" }
        ]
      }
    ]
  }
}
```

---

## 5. Screen-Level State Management

Screen state handles cross-section UI state that changes locally through `mutate` actions.

Examples:

- selected tab
- expanded/collapsed section ids
- selected filter chip
- **sort column and direction per table section**
- **form field selections (season, season type, etc.)**

```json
{
  "state": {
    "selectedTab": "recap",
    "expandedCards": []
  }
}
```

### Sort and Form State Conventions

The server pre-populates `Screen.state` with default values for sort and form fields. This means:

- Tables render with the correct initial sort on first paint
- Forms display the correct initial selections
- After a parameterized refresh, the new response includes submitted values in state so the form reflects what was applied

State keys are **namespaced per section** to avoid collisions when multiple tables or forms appear on the same screen.

```json
{
  "state": {
    "boxscore_team": "NYK",
    "boxscore_home_sortCol": "points",
    "boxscore_home_sortDir": "desc",
    "boxscore_away_sortCol": "points",
    "boxscore_away_sortDir": "desc"
  }
}
```

Sort state survives live poll refreshes because `Screen.state` is managed by the client — when a poll replaces section data, the sort column/direction remain unchanged and the client re-sorts the new data locally.

---

## 6. Client Infrastructure - Written Once

Each client platform builds these systems once:


| System             | Responsibility                                | Inputs                           |
| ------------------ | --------------------------------------------- | -------------------------------- |
| Section Router     | section type -> native renderer mapping       | `section.type`                   |
| State Manager      | apply binding patches, hold observable models | `refreshPolicy`, `dataBindings`  |
| Action Executor    | dispatch action chains to app systems         | `actions`                        |
| Screen State Store | shared mutable screen state                   | `screen.state`, `mutate` actions |
| Channel Manager    | polling/SSE lifecycle and reconnect policy    | section refresh config           |


### Section Router Example (Android)

```kotlin
@Composable
fun SectionRouter(section: SduiSection, onAction: (SduiAction) -> Unit) {
    when (section.type) {
        "GamePanel" -> GamePanelRenderer(section, onAction)
        "BoxscoreTable" -> BoxscoreTableRenderer(section, onAction)
        "TabGroup" -> TabGroupRenderer(section, onAction)
        "AdSlot" -> AdSlotRenderer(section, onAction)
        "AtomicComposite" -> AtomicCompositeRenderer(section, onAction)
        else -> Unit // Unknown type -> graceful skip
    }
}
```

### Section Renderer Example (Android)

```kotlin
@Composable
fun GamePanelRenderer(section: SduiSection, onAction: (SduiAction) -> Unit) {
    val data = mapGamePanel(section)
    val config = data.displayConfig
    // displayConfig drives layout: logo size, card height, score style, background
    GameCard(data, config, onAction)
}
```

---

## 7. Codegen Pipeline

```
schema/sdui-schema.json
  -> jsonschema2pojo -> Java models (server + Android)
  -> quicktype -> Swift models (iOS)
  -> quicktype -> TypeScript models (Web)
```

- Schema remains the source of truth.
- Generated models are never hand-edited.
- Renderers map generated models to platform-native UI components.

---

## 8. Platform Coverage

Each platform family receives a tailored composition from the server while sharing the same schema, codegen, and data pipeline. The client sends its platform identity in the request envelope; the composition service routes to the appropriate composer.

| Platform family | Platforms              | Renderer                 | Transport                    | Interaction model         | Composition profile |
| --------------- | ---------------------- | ------------------------ | ---------------------------- | ------------------------- | ------------------- |
| Mobile          | Android phone/tablet   | Jetpack Compose          | OkHttp + SSE                 | touch gestures            | Full section set, responsive breakpoints |
| Mobile          | iOS phone/tablet       | SwiftUI                  | URLSession + SSE/EventSource | touch gestures            | Full section set, responsive breakpoints |
| Web             | Browser                | React                    | fetch + SSE/EventSource      | mouse/touch/keyboard      | Wide layout, `webUrl` preferred for navigation |
| TV              | tvOS, Fire TV          | Compose/SwiftUI variants | platform-specific            | D-pad focus + select      | Reduced sections, large art, simplified interactions |

**Section type coverage across platforms:**

| Section type | Web (React) | Android (Compose) | iOS (SwiftUI) |
|---|---|---|---|
| GamePanel (server-driven `displayConfig` for standard, featured, scoreboard layouts) | Built | Built | Designed |
| TabGroup | Built | Built | Designed |
| BoxscoreTable | Built | Built | Gap |
| Form | Built | Built | Gap |
| SeasonLeadersTable | Built | Built | Gap |
| SubscribeBanner | Built | Built | Gap |
| SubscribeHero | Built | Built | Gap |
| AdSlot | Built | Built | Gap |
| AtomicComposite (server-composed atomic trees; bridges section + atomic layers) | Built | Built | Gap |

> **Note:** 9 former section types (ErrorState, SectionHeader, PromoBanner, ContentRail, FollowingRail, HeroPanel, StatLine, VideoCarousel, NbaTvSchedule) have been migrated to server-composed AtomicComposite. They no longer appear in the `Section.type` schema enum and require no dedicated client renderers.

**Atomic element coverage across platforms:**

| Atomic element type | Web (React) | Android (Compose) | iOS (SwiftUI) |
|---|---|---|---|
| Container | Built | Built | Gap |
| Text | Built | Built | Gap |
| Image | Built | Built | Gap |
| Button | Built | Built | Gap |
| Spacer | Built | Built | Gap |
| Divider | Built | Built | Gap |
| ScrollContainer | Built | Built | Gap |
| Conditional | Built | Built | Gap |
| DisplayGrid | Built | Built | Gap |
| SectionSlot | Built | Built | Gap |
| **AtomicRouter** | **Built** | **Built** | **Gap** |

The `AtomicRouter` dispatches rendering for all 10 element types. `AtomicComposite` is the 9th section type in `SectionRouter` (8 permanent + AtomicComposite). 9 former section types have been migrated to server-composed AtomicComposite layouts with no client renderers.


---

## 8a. Image Fallback Pattern

The composition service may reference external image URLs (CDN thumbnails, editorial images) that can become stale or return 404s. To prevent broken images on screen, SDUI uses a **server-driven fallback** pattern:

1. **Server provides `fallbackThumbnailUrl`** — sections that contain images include an optional `fallbackThumbnailUrl` field in their data payload. This URL points to a known-good generic image.
2. **Client applies fallback on error** — if the primary image URL fails to load, the client swaps to the fallback URL.

### Web (React)

The `<img>` tag uses an `onError` handler:

```tsx
const fallbackUrl = data.fallbackThumbnailUrl;

<img
  src={item.imageUrl}
  onError={(e) => {
    const img = e.currentTarget;
    if (fallbackUrl && img.src !== fallbackUrl) img.src = fallbackUrl;
  }}
/>
```

The `img.src !== fallbackUrl` guard prevents infinite loops if the fallback itself fails.

### Android (Jetpack Compose / Coil)

Coil's `SubcomposeAsyncImage` renders a fallback `AsyncImage` in its `error` slot:

```kotlin
val fallbackUrl = (section.data as? Map<*, *>)
    ?.get("fallbackThumbnailUrl")?.toString()

SubcomposeAsyncImage(
    model = imageUrl,
    contentDescription = null,
    error = {
        if (fallbackUrl != null) {
            AsyncImage(
                model = fallbackUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
    }
)
```

### Design Principles

- **Server-driven**: The server decides what the fallback image is — clients never hardcode fallback URLs.
- **Graceful**: If both primary and fallback fail, the image area remains empty (no crash, no broken icon).
- **Per-section**: Different sections can specify different fallbacks appropriate to their content type.
- **Sections using this pattern**: VideoCarousel, ContentRail, HeroPanel, NbaTvSchedule, PromoBanner, FollowingRail, SubscribeBanner, SubscribeHero.

---

## 9. Gaps - Production Capabilities Still To Lock

### 9i. Impression Deduplication

- Finalize threshold and dwell policy.
- Finalize dedup scope (screen/session/server policy model).

Reference: ADR-009

### 9j. A/B Testing Integration

- Client-authoritative assignment via Amplitude SDK.
- Server trusts assignments and uses them for composition branching.
- Server kill switch can reject disabled variants (fallback to control).
- Response must echo final assignment used.

Reference: ADR-006

### 9m. Form-Factor Layout Manager

- Server provides layout intent.
- Client applies native best-fit rules.
- Contract defines fallback behavior for unsupported hints.

Reference: ADR-008

### 9n. Ad Support as First-Class Primitive

- Represent ads as explicit SDUI primitive (for example `AdSlot`).
- Define semantic ad fields and failure/no-fill behavior (provider, ad unit path, sizes, targeting, collapse behavior).
- Keep ad auction/targeting delegated outside SDUI contract execution.

Reference: ADR-007

### 9o. Composition Input and Request Model

Composition must support a typed request envelope:

- screen/entity identifiers
- platform/app version/device class
- locale/region/timezone
- auth context (`Authorization` header)
- device context (device ID, ZIP code, country code, region)
- experiment assignments
- client capabilities
- traceId

Transport and caching:

- Support GET and POST based on screen/section context.
- Never put auth tokens in query params.
- Cacheability classes: `public`, `contextual`, `personalized`, `live`.

References: ADR-003, ADR-004

### 9p. Internationalization (i18n)

The composition server pre-translates all text in the initial response based on locale (`locale` query parameter on GET, body field on POST; default `en`). However, real-time updates (Ably SSE) and direct CDN polling bypass the server — the DataBindingApplier writes external source values directly into section data with no translation opportunity.

**Proposed implementation: server default + optional string keys on data bindings.**

- **Initial load:** Server resolves locale from the request and sends pre-translated text in all string fields. Clients render as-is.
- **Binding updates from external sources:** An optional `stringKeys` map on `DataBinding` allows the server to attach translation keys to any bound field where the external source may deliver untranslated strings. After applying a binding, the client checks for a corresponding string key and resolves via platform-native i18n (Android `strings.xml`, iOS `Localizable.strings`, web i18n library). Falls back to the raw value if no local translation exists.

**Schema addition:**

- Add optional `stringKeys: { [targetPath]: string }` map to the `DataBinding` definition.
- General-purpose mechanism — applies to any string field arriving via a binding (status text, player names, team names, labels, etc.). The server decides which fields need keys.

```json
"dataBindings": {
  "bindings": [
    { "sourcePath": "$.homeTeam.score", "targetPath": "homeTeam.score" },
    { "sourcePath": "$.gameStatusText", "targetPath": "gameStatusText" }
  ],
  "stringKeys": {
    "gameStatusText": "game_status_text"
  }
}
```

In this example, `homeTeam.score` is numeric and needs no translation. `gameStatusText` may arrive untranslated from the SSE source, so the server attaches a string key. After applying the binding, the client resolves `game_status_text` via platform-native i18n; if no local translation exists, the raw value is used.

**Rejected alternatives:**

- Upstream localization (route Ably/CDN through translation layer) — unacceptable fanout infrastructure cost.
- Server-side SSE proxy (subscribe, translate, re-publish) — adds latency and infrastructure for no benefit.
- Format templates (server sends patterns, client fills values) — cannot guarantee all translatable content fits a template.

### 9q. Tabular Data Sections and Forms

Semantic table section types (`BoxscoreTable`, future `RosterTable`, `StandingsTable`) with domain-typed data. Generic `Form` section for settings pickers. Client-side sort via `mutate` actions. Parameterized refresh for form submission.

Key decisions settled (follows established semantic pattern — no ADR needed):

- Semantic types over generic DataTable — server describes *what*, clients own *how*
- Generic Form — pickers share shape regardless of domain
- Client-side sort — small payloads, no server round-trip, state survives poll refreshes
- Parameterized refresh — backward-compatible Action extension with `endpoint` + `paramBindings`
- Server-namespaced state keys — avoids collisions with multiple tables on screen

See Section 2 (Schema Design) for data shapes and Section 4 (Action System) for parameterized refresh details.

### 9r. Offline and Degraded Connectivity

SDUI is server-driven by design — when the network is unavailable, there is no server to drive the UI. Without an explicit strategy, cold launch on a disconnected device shows a blank screen or unhandled error.

The recommended approach is **stale-while-offline**: serve the last-known-good SDUI response from platform HTTP cache when the network is unavailable, show a non-blocking connectivity indicator, and allow pull-to-refresh to retry. This leverages existing cache infrastructure (OkHttp on Android, browser HTTP cache on Web) and the `cacheability` classification from ADR-004.

Key principles:

- **Proportional to value.** Sports content degrades quickly — live scores stale in seconds, schedules in hours. A full offline-first database is disproportionate; a cached last-response is sufficient.
- **`cacheability` governs staleness behavior.** Sections with `cacheability: "live"` show a placeholder ("Live data unavailable") instead of stale data. `public`/`contextual`/`personalized` sections serve stale content with a timestamp indicator.
- **No new server requirements.** Clients use existing HTTP cache headers per ADR-004. The server does not need an offline-specific API.
- **Actions degrade gracefully.** Network-dependent actions (navigate, refresh, mutate) show a "No connection" message. Fire-and-forget events queue locally and flush when connectivity resumes.

Options evaluated: offline-first local DB (rejected — cost disproportionate to value), stale-while-offline with platform cache (accepted), service worker for Web (deferred to v2), pre-seeded bundled content (optional v2 enhancement).

Reference: ADR-010

### 9s. Atomic Layer Performance Contract

The atomic element layer introduces server-composed UI trees rendered generically by `AtomicRouter`. To prevent unbounded complexity, the following performance limits are enforced:

| Limit | Value | Enforcement |
|---|---|---|
| Max tree depth | 6 | Server validation + client defensive depth guard (renderer returns null beyond limit) |
| Max children per container | 20 | Server validation |
| Max total nodes per atomic tree | 50 | Server validation |
| Max SectionSlot nesting depth | 2 | Client recursion guard (`sectionSlotDepth` parameter) |

These limits ensure atomic trees remain a lightweight composition mechanism, not a general-purpose layout engine. If a layout exceeds these constraints, it should be implemented as a semantic section type with a dedicated renderer.

---

## 10. Requirement Status


| Requirement                    | Status  | ADR     | Notes                                               |
| ------------------------------ | ------- | ------- | --------------------------------------------------- |
| Core composition model         | Partial | ADR-002 | implemented directionally, transition still active  |
| Request context envelope       | Built   | ADR-003, ADR-004 | `SduiRequestContext` POJO + `BracketParamResolver` (bracket-notation GET, POST fallback). Android & web `RequestEnvelopeBuilder`. All fields optional with defaults. |
| Action scope and precedence    | Partial | ADR-005 | rule set clear; needs broader fixtures              |
| Experiment conflict handling   | Built   | ADR-006 | ADR-006 Accepted. Client-authoritative assignment via experiment SDK. Server trusts `experiments` map from request envelope. Kill switch is client-side. Exposure tracking via fireAndForget actions. |
| Ad primitive contract          | Gap     | ADR-007 | required field policy + fallback semantics pending  |
| Layout manager contract        | Partial | ADR-008 | SectionLayoutHints built on Web (margins, dividers, priority). ADR-008 accepted (Option C). Android wiring pending. |
| Impression semantics           | Partial | ADR-009 | Built on web (IntersectionObserver + dedup registry). ADR-009 accepted. Android/iOS pending. |
| Error handling (ErrorState)    | Built   | —       | server-composed `ErrorState` section type built on Web and Android. Per-section runtime error/loading states (`sectionStates`) planned. |
| Contract testing/observability | Gap     | —       | broader test corpus + dashboards pending            |
| Internationalization (i18n)    | Built   | —       | Section-level `stringTable` stamped by server per locale. Server pre-translates initial text. Clients consume `stringTable` from each section. Parameterized strings via atomic decomposition. |
| Tabular data (BoxscoreTable)   | Built   | —       | semantic table type, domain-typed data, client-side sort. Built on Web and Android. |
| Form section (generic)         | Built   | —       | extensible field types, parameterized refresh. Built on Web and Android. |
| Parameterized refresh          | Built   | —       | `endpoint` + `paramBindings` Action extension. Working via Form submit. |
| SeasonLeadersTable             | Built   | —       | domain-typed leaders table with form-driven parameterized refresh |
| Image fallback                 | Built   | —       | server-driven `fallbackThumbnailUrl` with client-side error handling |
| Offline / degraded connectivity| Gap     | ADR-010 | stale-while-offline via platform HTTP cache; staleness UX per cacheability class |
| Atomic rendering layer         | Built   | —       | AtomicRouter + 9 primitives + SectionSlot bridge on Android and Web. AtomicComposite section type. DisplayGrid for non-interactive grids. Performance contract enforced (depth 6, children 20, nodes 50). |


---

## ADR Approvals Pending

All pending ADRs are tracked in the Requirement Status table above. Until approved, those requirements remain directional and may be refined.


---

## 11. Non-Normative Context: Risks, Rationale, and Next Steps

1. Schema governance is permanent platform work.
2. Debugging crosses composition, schema, codegen, runtime, and renderer layers.
3. Cold start overhead requires caching and strict performance budgets.
4. Contract-test depth is the primary control against cross-platform drift.
5. Runtime binding consistency across platforms is mandatory.

### 11a. Why Build It Anyway

The alternative is duplicated platform composition logic and drift in feature behavior. SDUI centralizes composition velocity while preserving native rendering quality.

### 11b. Recommended Next Steps

1. Lock request envelope and transport/cache policy (ADR-003, ADR-004).
2. Lock action scope/precedence and experiment model (ADR-005, ADR-006).
3. Ship subsection actions + request-envelope schema updates with fixtures.
4. Introduce ad primitive (ADR-007) and resolve layout strategy (ADR-008).
5. Finalize impression semantics and enforce fire-and-forget/runtime conformance (ADR-009).
6. Implement offline/degraded connectivity strategy per ADR-010 — platform cache fallback, staleness UX, fire-and-forget queue.

---

## Appendix A: Composition Request Example (Typed Envelope)

```json
{
  "screenId": "game-detail",
  "entity": { "type": "game", "id": "0022500384" },
  "platform": {
    "name": "android",
    "appVersion": "8.3.0",
    "osVersion": "14",
    "deviceClass": "phone",
    "capabilities": {
      "sse": true,
      "onFocus": false
    }
  },
  "device": {
    "deviceId": "a1b2c3d4-5678-90ef-ghij-klmnopqrstuv",
    "zipCode": "10001",
    "countryCode": "US",
    "region": "ny"
  },
  "experiments": { "gd_tab_order_v2": "client_hint_variant_b" },
  "traceId": "8d68d9d5-4512-4ff4-8d55-1f1b20ea4e11"
}
```

The `device` object carries device signals that the composition service may use for any purpose — entitlement resolution, geo-based content filtering, personalization, restriction checks, or other server-side decisions. The envelope does not prescribe how these inputs are consumed. Content-specific fields (production ID, league ID, content type) are known to the composer from the content being composed and do not need to appear in the client request. Auth identity is carried via the `Authorization` header.

---

## Appendix B: Complete SDUI Response Example (Expanded)

```json
{
  "meta": {
    "traceId": "8d68d9d5-4512-4ff4-8d55-1f1b20ea4e11",
    "schemaVersion": "1.1",
    "assignment": {
      "gd_tab_order_v2": "server_variant_a"
    },
    "cacheability": "contextual"
  },
  "screen": {
    "id": "game-detail",
    "state": {
      "selectedTab": "recap",
      "expandedCards": []
    },
    "actions": [
      {
        "trigger": "onTap",
        "type": "fireAndForget",
        "event": "screen_tap",
        "params": { "screenId": "game-detail" }
      }
    ],
    "sections": [
      {
        "id": "scoreboard-001",
        "type": "GamePanel",
        "data": {
          "displayConfig": { "scoreTextStyle": "compact" },
          "homeTeam": {
            "teamId": "1610612752",
            "teamTricode": "NYK",
            "teamName": "New York Knicks",
            "score": 104,
            "logoUrl": "https://cdn.nba.com/logos/nba/1610612752/global/L/logo.svg"
          },
          "awayTeam": {
            "teamId": "1610612751",
            "teamTricode": "BKN",
            "teamName": "Brooklyn Nets",
            "score": 98,
            "logoUrl": "https://cdn.nba.com/logos/nba/1610612751/global/L/logo.svg"
          },
          "gameStatusText": "4:32 Q3",
          "period": 3
        },
        "refreshPolicy": {
          "type": "sse",
          "channel": "0022500384:linescore",
          "retryMs": 1500
        },
        "dataBindings": {
          "bindings": [
            { "sourcePath": "$.homeTeam.score", "targetPath": "homeTeam.score" },
            { "sourcePath": "$.awayTeam.score", "targetPath": "awayTeam.score" },
            { "sourcePath": "$.gameStatusText", "targetPath": "gameStatusText" },
            { "sourcePath": "$.period", "targetPath": "period" }
          ],
          "stringKeys": {
            "gameStatusText": "game_status_text"
          }
        },
        "actions": [
          {
            "trigger": "onTap",
            "type": "fireAndForget",
            "event": "scoreboard_tapped",
            "params": {
              "gameId": "0022500384",
              "sectionId": "scoreboard-001"
            }
          },
          {
            "trigger": "onTap",
            "type": "navigate",
            "targetUri": "nba://game/0022500384/boxscore",
            "webUrl": "https://www.nba.com/game/0022500384"
          }
        ],
        "subsections": [
          {
            "id": "home-team-hotspot",
            "actions": [
              {
                "trigger": "onTap",
                "type": "fireAndForget",
                "event": "home_team_tapped",
                "params": { "teamId": "1610612752" }
              },
              {
                "trigger": "onTap",
                "type": "navigate",
                "targetUri": "nba://team/1610612752"
              }
            ]
          }
        ]
      },
      {
        "id": "stats-001",
        "type": "AtomicComposite",
        "data": {
          "root": {
            "type": "Container",
            "direction": "vertical",
            "children": [
              { "type": "Text", "content": "Top Performers", "variant": "titleMedium" },
              { "type": "Text", "content": "Jalen Brunson — NYK — 28 PTS, 7 AST, 3 REB", "variant": "bodyMedium" },
              { "type": "Text", "content": "Mikal Bridges — BKN — 22 PTS, 4 AST, 5 REB", "variant": "bodyMedium" }
            ]
          }
        },
        "refreshPolicy": {
          "type": "poll",
          "intervalSec": 30,
          "url": "https://cdn.nba.com/static/json/liveData/boxscore/boxscore_0022500384.json"
        },
        "actions": [
          {
            "trigger": "onVisible",
            "type": "fireAndForget",
            "event": "section_impression",
            "params": { "sectionId": "stats-001", "policy": "once_per_screen" }
          }
        ]
      },
      {
        "id": "tab-group-001",
        "type": "TabGroup",
        "data": {
          "tabs": [
            { "id": "recap", "label": "Recap" },
            { "id": "boxscore", "label": "Box Score" },
            { "id": "play-by-play", "label": "Play-by-Play" }
          ]
        },
        "actions": [
          { "trigger": "onTap", "type": "mutate", "target": "selectedTab", "operation": "set", "value": "boxscore" },
          { "trigger": "onTap", "type": "fireAndForget", "event": "tab_selected", "params": { "tabId": "boxscore" } }
        ]
      },
      {
        "id": "adslot-001",
        "type": "AdSlot",
        "data": {
          "provider": "gam",
          "ad_unit_path": "/21234567/sports/nba/homepage_top",
          "sizes": [[320, 50], [728, 90]],
          "targeting": {
            "section": "homepage",
            "content_type": "live_game"
          },
          "collapse_on_empty": true
        },
        "refreshPolicy": { "type": "static" }
      },
      {
        "id": "content-rail-001",
        "type": "AtomicComposite",
        "data": {
          "root": {
            "type": "ScrollContainer",
            "direction": "horizontal",
            "children": [
              { "type": "Container", "direction": "vertical", "children": [
                { "type": "Text", "content": "LAL vs GSW", "variant": "titleSmall" },
                { "type": "Text", "content": "7:30 PM ET", "variant": "bodySmall" }
              ]},
              { "type": "Container", "direction": "vertical", "children": [
                { "type": "Text", "content": "BOS vs MIA", "variant": "titleSmall" },
                { "type": "Text", "content": "8:00 PM ET", "variant": "bodySmall" }
              ]}
            ]
          }
        },
        "refreshPolicy": { "type": "static" },
        "actions": [
          { "trigger": "onTap", "type": "navigate", "targetUri": "nba://game/{itemId}" }
        ]
      }
    ]
  }
}
```

This expanded example demonstrates:

- typed request envelope as composition input
- server-echoed experiment assignment
- screen/section/subsection action scopes
- precedence-compatible nested actions
- mixed refresh policies (sse, poll, static)
- field-level data binding patch behavior
- ad section as explicit primitive
- screen state mutation with fireAndForget chaining

---

## Appendix C: Boxscore Screen Response Example

A composed boxscore screen using `TabGroup` to toggle between teams, each tab containing a `BoxscoreTable` with domain-typed player data. Demonstrates semantic table composition, server-namespaced state keys, and sort state pre-population.

```json
{
  "meta": {
    "traceId": "c4f29a11-7e33-4b8a-a1d2-9f3c5e8b7a01",
    "schemaVersion": "1.2",
    "cacheability": "live"
  },
  "screen": {
    "id": "game-boxscore",
    "state": {
      "boxscore_team": "BKN",
      "boxscore_away_sortCol": "points",
      "boxscore_away_sortDir": "desc",
      "boxscore_home_sortCol": "points",
      "boxscore_home_sortDir": "desc"
    },
    "sections": [
      {
        "id": "boxscore-tabs",
        "type": "TabGroup",
        "data": {
          "tabs": [
            { "id": "away", "label": "BKN" },
            { "id": "home", "label": "NYK" }
          ],
          "stateKey": "boxscore_team",
          "loadingStrategy": "eager"
        }
      },
      {
        "id": "boxscore-away",
        "type": "BoxscoreTable",
        "data": {
          "teamId": 1610612751,
          "teamTricode": "BKN",
          "teamName": "Brooklyn Nets",
          "teamLogoUrl": "https://cdn.nba.com/logos/nba/1610612751/global/L/logo.svg",
          "players": [
            {
              "playerId": 1629029,
              "name": "Mikal Bridges",
              "nameAbbreviated": "M. Bridges",
              "headshotUrl": "https://cdn.nba.com/headshots/nba/latest/260x190/1629029.png",
              "jerseyNum": "1",
              "position": "F",
              "starter": true,
              "played": true,
              "statistics": {
                "minutes": "36:15",
                "points": 22,
                "rebounds": 5,
                "assists": 4,
                "steals": 2,
                "blocks": 1,
                "turnovers": 3,
                "personalFouls": 2,
                "fieldGoalsMade": 8,
                "fieldGoalsAttempted": 18,
                "fieldGoalPercentage": 0.444,
                "threePointersMade": 3,
                "threePointersAttempted": 7,
                "threePointPercentage": 0.429,
                "freeThrowsMade": 3,
                "freeThrowsAttempted": 4,
                "freeThrowPercentage": 0.750,
                "reboundsOffensive": 1,
                "reboundsDefensive": 4,
                "plusMinus": -6
              }
            }
          ],
          "teamTotals": {
            "points": 98,
            "rebounds": 40,
            "assists": 21,
            "steals": 7,
            "blocks": 4,
            "turnovers": 14,
            "personalFouls": 22,
            "fieldGoalsMade": 36,
            "fieldGoalsAttempted": 88,
            "fieldGoalPercentage": 0.409,
            "threePointersMade": 10,
            "threePointersAttempted": 32,
            "threePointPercentage": 0.313,
            "freeThrowsMade": 16,
            "freeThrowsAttempted": 20,
            "freeThrowPercentage": 0.800,
            "reboundsOffensive": 10,
            "reboundsDefensive": 30
          },
          "sortStateKey": "boxscore_away_sortCol",
          "sortDirectionStateKey": "boxscore_away_sortDir",
          "emptyMessage": null
        },
        "refreshPolicy": {
          "type": "poll",
          "intervalSec": 30,
          "url": "https://cdn.nba.com/static/json/liveData/boxscore/boxscore_0022500384.json"
        },
        "actions": [
          {
            "trigger": "onVisible",
            "type": "fireAndForget",
            "event": "section_impression",
            "params": { "sectionType": "BoxscoreTable", "teamTricode": "BKN" }
          }
        ]
      }
    ]
  }
}
```

This example demonstrates:

- Flat composition: `TabGroup` wrapping `BoxscoreTable` sections (no nesting)
- Server-namespaced state keys (`boxscore_away_sortCol`, `boxscore_home_sortCol`) avoiding collisions
- Domain-typed player data with statistics, headshot URLs, and position metadata
- `teamTotals` as a pre-computed aggregation row (frozen at bottom, excluded from sort)
- Poll-based refresh for live game updates (sort state in `Screen.state` survives refresh)
- Semantic analytics (`sectionType: "BoxscoreTable"`) vs. generic "viewed DataTable"

---

## Appendix D: Form Section Response Example

A `Form` section driving a `SeasonLeadersTable` refresh via parameterized server query. Demonstrates extensible field types, screen state accumulation, and `paramBindings`-based refresh.

```json
{
  "type": "Form",
  "data": {
    "fields": [
      {
        "id": "season",
        "label": "Season",
        "stateKey": "season",
        "defaultValue": "2025-26",
        "fieldType": "picker",
        "options": [
          { "label": "2025-26", "value": "2025-26" },
          { "label": "2024-25", "value": "2024-25" }
        ]
      },
      {
        "id": "seasonType",
        "label": "Season Type",
        "stateKey": "seasonType",
        "defaultValue": "Regular Season",
        "fieldType": "segmented",
        "options": [
          { "label": "Regular Season", "value": "Regular Season" },
          { "label": "Playoffs", "value": "Playoffs" }
        ]
      }
    ],
    "submitAction": {
      "trigger": "onTap",
      "type": "refresh",
      "endpoint": "/sdui/stats/leaders",
      "paramBindings": {
        "season": "season",
        "seasonType": "seasonType"
      }
    },
    "submitLabel": "Apply",
    "layout": "inline"
  }
}
```

This example demonstrates:

- Extensible field types: `picker` renders as a dropdown, `segmented` as a button group (also supports `toggle`, `datePicker`, `text`)
- Each field's `stateKey` maps to a `Screen.state` entry — field changes accumulate in screen state without server round-trips
- `submitAction` uses `paramBindings` to resolve state values at action time: `{"season": "season"}` means "read the current value of `Screen.state["season"]` and send it as the `season` query parameter"
- The `target` on the refresh action specifies which section receives the updated response (surgical section merge preserves form state)

