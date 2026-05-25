# SDUI Platform — Requirements Summary & Key Decisions

> Consolidated from design sessions covering schema, codegen, rendering, data binding, action system, and state management. Includes gap analysis against Airbnb (Ghost Platform), Lyft, DoorDash, and Spotify SDUI implementations.

> **Prototype naming note:** This document uses conceptual names (SDUIStateManager, SDUIActionExecutor, SDUIScreenStateManager) to describe the architectural roles. The working prototype uses simplified names (e.g., `SduiScreenViewModel`, `ActionHandler`, `StateManager`) — the concepts are the same, the class names differ.

> **Scope note (normative):** This document is the requirements source of truth. Diagrams and payload snippets are illustrative and non-normative unless explicitly called out as requirement statements.

## 5-Minute Reader Guide

- Start here for current commitments: `Current Governance (This Document)`
- Architecture requirements: `1. Core Architecture` and `2. Schema Design`
- Runtime behavior requirements: `3. Data Binding`, `4. Action System`, `5. Screen-Level State`
- Open decision approvals: `ADR Status Summary`
- Implementation status at a glance: `10. Requirement Status Matrix`

## Governance Header Template

Use this header at the top of requirement documents to keep ownership, approval, and decision linkage explicit.

```yaml
document: <requirements-doc-name>
version: <semver-or-date-version>
status: draft|review|approved
owner: <primary-owner>
last_updated: <yyyy-mm-dd>
decision_policy:
  - "Create ADRs for decisions requiring platform and backend input."
  - "Requirements document is source of truth for what; ADRs capture why."
approvers:
  - <name-or-group>
related_adrs:
  - <adr-id-and-title>
required_inputs:
  platform: true
  backend: true
locked_decisions:
  action_precedence: "nested > section > screen-default"
  experiment_assignment: "client-authoritative via Amplitude; server trusts assignments, kill switch for disabled variants"
  request_method_policy: "GET or POST based on section/screen cacheability and context"
  ad_boundary: "ad auction/targeting delegated; SDUI carries placement contract"
  cache_policy_owner: "platform + backend"
  transition_policy: "avoid CoreAPI-adapter unless necessary; decide case-by-case"
```

### Current Governance (This Document)

- **Owner:** Adrian Robinson (temporary decision authority)
- **ADR policy:** create ADRs for decisions needing platform/backend input
- **Platform-aware composition (settled):** shared schema and data pipeline; per-platform-family composition responses. Not an ADR — the only practical architecture for phone, tablet, web, and TV. Server-side composition cost is acceptable.
- **Navigate action URIs (settled):** `targetUri` for native deeplinks, `webUrl` for web — both are first-class platform-appropriate targets, not primary/fallback.
- **Action precedence:** nested > section > screen-default
- **Experiment strategy:** client-authoritative assignment via Amplitude SDK; server trusts and uses assignments for composition
- **Experiment conflict rule:** client assignment is authoritative; server may reject disabled variants via kill switch; response echoes final variant used
- **Request method:** support both GET and POST depending on section/screen needs
- **Ads boundary:** delegated ad auction/targeting, SDUI provides placement contract
- **Cache policy ownership:** platform + backend input required
- **CoreAPI adapter transition:** avoid unless necessary; evaluate case-by-case
- **Entitlement/restriction resolution (short term):** support current client-side resolution until server-authoritative resolution is available
- **Entitlement/restriction resolution (long term):** move to server-authoritative resolution in SDUI composition; client provides device context via `device` in the request envelope
- **Caching strategy:** section-first caching + optional screen snapshot cache

---

## 1. Core Architecture

The platform follows a **server-driven composition** model where the server controls what appears on screen, how data stays fresh, and what happens on every user interaction. The client implements generic infrastructure that executes server instructions using native rendering.

**Composition ownership requirement:** SDUI semantic composition is owned by the SDUI composition layer (composer/aggregator), not by platform-specific client card logic. CoreAPI-derived composition may be used as a transitional input, but it is not the long-term semantic source of truth.

**Platform-aware composition (settled position):** The SDUI schema is universal — all platforms share the same vocabulary of section types, action types, and data models. The composition layer produces **per-platform-family responses**. A tvOS 10-foot UI, a phone, and a web browser have fundamentally different information density, interaction models, and layout requirements. Serving identical responses to all platforms forces a lowest-common-denominator experience that is unacceptable.

What is shared across all platforms:
- Schema definitions (JSON Schema) — the universal contract
- Codegen (typed models per platform from one schema)
- Upstream data fetching and transformation — one integration pipeline
- Section-type semantics — a `BoxscoreTable` means the same thing everywhere

What differs per platform family:
- Which sections are composed and in what order
- Information density and layout intent
- Action URIs (`targetUri` for native deeplinks, `webUrl` for web)
- Image dimensions and density parameters
- Interaction triggers (touch vs. D-pad focus vs. hover)

The client sends its platform identity in the request envelope. The composition service routes to the appropriate composer. The cost is additional server-side composition logic — this is acceptable and is where the investment should go.

```mermaid
graph TD
    A[Composition Service] -->|SDUI Response JSON| B[Client App]
    B --> C[Section Router]
    C --> D[SDUIStateManager<br/>Data Binding & Refresh]
    C --> E[SDUIActionExecutor<br/>Navigate / FireAndForget / Mutate]
    C --> F[SDUIScreenStateManager<br/>Tabs, Toggles, Accordions]
    D --> G[Section Renderer<br/>Thin Wiring Layer]
    E --> G
    F --> G
    G --> H[Design System Components<br/>NBAScoreboard, NBAStatLine, etc.]
    
    style A fill:#1B4F72,color:#fff
    style H fill:#27AE60,color:#fff
    style D fill:#E67E22,color:#fff
    style E fill:#8E44AD,color:#fff
    style F fill:#2980B9,color:#fff
```

### Server Controls vs. Client Controls

| Server Controls | Client Controls |
|---|---|
| Which sections appear and in what order | Native rendering (SwiftUI / Compose) |
| What data each section shows | Design system theming and animation |
| How data stays fresh (dataBinding) | Platform-specific transport (URLSession / OkHttp) |
| What happens on every interaction (actions) | Accessibility traits |
| Which analytics fire and where | TV focus management |
| Navigation destinations and presentation style | Gesture recognition |
| Tab structure and selection behavior | Deeplink resolution logic |
| A/B test variants | Analytics SDK integration |
| Back navigation target (`parentUri`) | Back button rendering and gesture |

---

## 2. Schema Design

The schema is a JSON Schema document that your org defines and owns. It is the **single source of truth** that drives codegen (typed data models), server responses, and contract testing.

### Section Anatomy — Every Section Carries Five Things

```mermaid
graph LR
    S[SDUI Section] --> ID["Identity<br/>id + type"]
    S --> DATA["Initial Data<br/>Renders on first paint"]
    S --> DB["Data Binding<br/>How data stays fresh"]
    S --> ACT["Actions<br/>What happens on interaction"]
    S --> STATE["Screen State<br/>Initial values for mutate targets"]
    S --> SURF["Surface<br/>Server-driven outer frame"]
    
    DB -->|Processed by| SM[SDUIStateManager]
    ACT -->|Processed by| AE[SDUIActionExecutor]
    STATE -->|Processed by| SSM[SDUIScreenStateManager]
    SURF -->|Applied by| SC[SectionContainer]
    
    style S fill:#1B4F72,color:#fff
    style SM fill:#E67E22,color:#fff
    style AE fill:#8E44AD,color:#fff
    style SSM fill:#2980B9,color:#fff
    style SC fill:#27AE60,color:#fff
```

### Key Schema Decisions

- **Dual-layer type model** — 9 **section types in schema** (8 permanent with client renderers: BoxscoreTable, SeasonLeadersTable, Form, TabGroup, SubscribeBanner, SubscribeHero, AdSlot, VideoPlayer — plus the `AtomicComposite` bridge), plus 12 **atomic element types** (Container, Text, Image, Button, Spacer, Divider, ScrollContainer, Conditional, DisplayGrid, OverlayContainer, SectionSlot, LiveClock) for server-composed generic layouts. Stateless layout surfaces (headers, rails, hero panels, promo banners, stat lines, video carousels, schedule layouts, error states, live-score cards) are server-composed as `AtomicComposite` trees driven by `bindRef` leaf-level data resolution plus the `LiveClock` primitive for client-owned tick animation. Semantic sections handle stateful domain logic (sort, frozen columns, forms, platform SDK integration — IAP, ads, video); atomic primitives handle server-composed layouts with no client business logic. See *Grid vs. Section Decision Tree* in §9q.
- **Codegen produces data models only** — not UI code. Platform teams write a thin renderer layer (~30 lines per section type) that wires generated models to existing design system components.
- **Schema is versioned** — client sends its schema version, server responds with a compatible payload. Fields can never be removed without a major version bump.
- **Subsection actions are required** — `actions` must be supported at section and nested component/subsection level (for example, tapping home team area within a game section).
- **Request context is contract input** — composition must support a typed request envelope (platform, app version, locale, device context, experiments, capabilities, traceId).
- **Server-driven back navigation** — `Screen.parentUri` (optional) tells the client where the back button should navigate.  Omit for root screens.  Clients always show the back button on non-root screens.

### Section Surface (Server-Driven Outer Frame)

Every section carries an optional `surface` field — a server-driven spec for the visual wrapper (frame) that sits beneath the section's content. The shared `SectionContainer` on each platform reads `surface` and applies it; semantic-section renderers never set their own outer padding, margin, corner radius, shadow, border, or background.

**`SectionSurface` properties:**

| Property | Type | Purpose |
|---|---|---|
| `margin` | `Spacing` | Outer space between the surface and its siblings / screen edge |
| `padding` | `Spacing` | Inner space between the surface edge and the content it wraps |
| `background` | `Background` | Surface fill — solid color, gradient, or image |
| `cornerRadius` | `LayoutScalar` | Corner radius with overflow clip |
| `shadow` | `Shadow` | Drop shadow |
| `border` | `Border` | Outer stroke |

**Requirements:**

- A single shared `SectionContainer` component per platform owns surface application — one owner, one code path
- Semantic-section renderers must not set outer padding, margin, corner radius, shadow, border, or background themselves — that is the surface's job
- When `surface` is omitted, sections render flush (no frame) unless the server emits a default surface
- `SectionSurface` mirrors the inline styling vocabulary on atomic `Container`, so semantic sections have schema parity with composed `AtomicComposite` sections
- For `AtomicComposite` sections, the root `Container` in `data.ui` provides its own frame via inline props — `surface` is typically omitted since the atomic tree is self-describing
- Surface ownership does not expand sideways: owning the frame does not grant a renderer ownership of content, actions, or refresh policy

This separation ensures the server can adjust spacing, card treatments, and visual grouping of any section without a client release.

### Section Placement Traceability (Future)

Section position is implicit in the `sections[]` array index. When an upstream content management system (e.g., Playmaker) requires analytics correlation between composition intent and rendered position, a `placement` string field may be added to `Section`. Design constraints for that future field:

- Slot names must describe **screen position** (e.g., `"top-primary"`, `"mid-1"`), not content type — the same slot may carry different sections depending on cohort or business override
- `placement` would be for analytics/debugging only — clients must not use it for layout or rendering
- Until that integration exists, the array index is sufficient for position tracking in beacons

---

## 3. Data Binding System

Addresses real-time requirements for live game data. The server specifies how each section's data stays fresh after first render.

### Channel Types

| Channel | Use Case | Mechanism |
|---|---|---|
| `static` | Content that never changes mid-session | No refresh |
| `poll` | Standings, stats (slow-changing) | Configurable interval (e.g., 30s) |
| `sse` | Live scores, game clock | Real-time stream (Ably in prototype) |

> **Note:** `websocket` was considered during design for full-duplex use cases (play-by-play, real-time chat) but is not in the current schema or prototype. It can be added as a future channel type.

### Binding Contract

The server specifies field-level bindings using JSONPath resolution:

```json
"dataBindings": {
  "bindings": [
    { "sourcePath": "$.homeTeam.score", "targetPath": "homeTeam.score" },
    { "sourcePath": "$.awayTeam.score", "targetPath": "awayTeam.score" },
    { "sourcePath": "$.period",         "targetPath": "period" },
    { "sourcePath": "$.gameStatusText", "targetPath": "gameStatusText" }
  ],
  "stringKeys": {
    "gameStatusText": "game_status_text"
  }
}
```

The refresh policy defines the channel separately:

```json
"refreshPolicy": {
  "type": "sse",
  "channel": "{gameId}:linescore"
}
```

**Key decisions:** The SDUIStateManager on each platform opens the channel, receives messages, resolves JSONPath sources, and patches the corresponding fields in section data. Bindings also support transforms (e.g., format a timestamp). Initial data is always included so there is **no loading spinner on first paint**.

### 3a. Leaf-Level Data Resolution (`bindRef`)

`bindRef` is the mechanism that connects live data updates to individual atomic elements inside an `AtomicComposite` tree. While `dataBindings` writes incoming SSE/poll data into `section.data.content`, `bindRef` on each leaf element reads from that content object at render time.

**Contract:**

- `bindRef` is an optional string property on every `AtomicElement`
- Value is a dot-path into the enclosing `AtomicComposite`'s `data.content` object (e.g., `"homeTeam.score"`)
- When set, the renderer resolves the leaf's canonical field from `data.content[bindRef]` at render time
- When the path is absent or resolves to null, the renderer falls back to the element's inline value
- No client-invented defaults — the inline field is the only fallback

**Canonical field per element type:**

| Element Type | Resolved Field | Type |
|---|---|---|
| Text | `content` | String (numbers/booleans coerced to string) |
| Button | `label` | String |
| Image | `src` | String (URL) |
| LiveClock | Object | `{snapshotSeconds, snapshotAt, isRunning}` |

**Data flow for live updates:**

```
SSE message arrives
  → DataBindingResolver reads sourcePath (JSONPath) from message
  → Writes value into section.data.content at targetPath (e.g., "content.homeTeam.score")
  → Leaf elements re-resolve bindRef on next render
  → Updated value appears in UI without tree reconstruction
```

**Architectural rationale:** Placing the binding identifier on the consuming node (rather than in a centrally-declared path-into-tree mapping) lets the server reshape the `ui` tree freely without breaking real-time updates. Elements can be moved, nested differently, or duplicated — each carries its own data reference. The `dataBindings` section envelope continues to write into `content.*` regardless of tree structure.

**Requirements:**

- All platforms must implement `BindRefResolver` with identical dot-path traversal and type coercion semantics
- Resolution must be non-destructive: missing paths return the inline fallback, never crash or show error UI
- The server composer is responsible for populating both the inline value (first paint) and the `data.content` entry (live updates) for every element with a `bindRef`
- `data.content` is the reserved namespace for bindable domain data; `data.ui` is the atomic element tree — these two concerns must not be mixed

### 3b. Binding Path Resilience

Schema evolution is additive-first, but bindings may reference paths that disappear from the data shape over time — either because the server stops sending a field, because a schema migration removes it, or because a cached layout targets a path that no longer exists in live messages. The binding runtime must handle all three cases gracefully.

#### Missing-Path Scenarios

| # | Scenario | Example | Normative Behavior |
|---|----------|---------|-------------------|
| MP-1 | Source path absent in incoming message | Server SSE message omits `$.period` for a post-game state | **Keep previous value.** Do not overwrite the target with null or a zero-value. Log a structured warning: `{ sectionId, sourcePath, traceId, reason: "source_absent" }`. |
| MP-2 | Source path removed by schema evolution | Server v2 renames `$.gameStatusText` to `$.statusLabel`; cached layouts still bind to the old path | **Keep previous value.** Binding is silently stale. The consecutive-miss counter (below) detects this condition in production. |
| MP-3 | Target path absent in cached section data | A cached layout references `homeTeam.seed` but the live response no longer includes a `seed` field in the initial data | **Auto-create intermediate objects** and set the value. This is already the behavior of `setTargetPath()`. No special handling needed. |

All three cases MUST be non-destructive: the client MUST NOT crash, show error UI, or clear the field. The keep-previous-value behavior is mandatory across all platforms.

#### Consecutive-Miss Counter

When a binding's source path is absent (MP-1 or MP-2) across multiple consecutive refresh cycles, it indicates a systemic mismatch rather than a transient omission. Clients MUST track consecutive misses per binding per section:

- **Counter key:** `{sectionId}:{sourcePath}`
- **Increment:** Each time `resolveSourcePath()` returns null/undefined for that binding during a refresh or SSE message
- **Reset:** When `resolveSourcePath()` successfully returns a non-null value for that binding
- **Threshold:** After `MISS_THRESHOLD` (default: 3) consecutive misses, log at WARN level with structured fields: `{ sectionId, sourcePath, consecutiveMisses, traceId }`
- **Analytics (deferred):** When analytics infrastructure is available, emit a `binding_path_missing` event at the threshold so the team can detect stale bindings in production dashboards

The existing `applyBindings()` null-guard (`IF sourceValue IS NULL: CONTINUE`) is the runtime implementation of MP-1 and MP-2. The consecutive-miss counter adds observability on top of that existing behavior.

#### Cleanup

When a section is removed from the screen (e.g., navigation away, screen refresh replaces section list), clients MUST clear the miss counters for that section to prevent memory leaks.

### 3c. Per-Section Staleness Tracking

Each section with a non-static `refreshPolicy` has its own data channel (SSE or poll). These channels can fail independently — one section's Ably subscription may disconnect while another section's poll continues succeeding. Clients MUST track staleness at the section level, not just at the screen level.

This is **orthogonal** to the screen-level `isStale` flag from ADR-010 (which indicates the entire layout was served from HTTP cache). Per-section staleness indicates a specific data channel is degraded while the layout may be fresh.

#### Staleness Rules

| Channel Type | Stale When | Clear When |
|---|---|---|
| `sse` | Ably connection enters `disconnected` state for >**10 seconds** | Next successful SSE message received for that section |
| `poll` | **2 consecutive** poll failures (HTTP error or network timeout) | Next successful poll response |
| `static` | Never stale (no data channel) | N/A |

#### Client Requirements

- Track a `staleSections: Set<sectionId>` that is the **union** of action-refresh failures (existing behavior) and channel-health failures (new behavior)
- Render a section-level staleness badge on affected sections (visual treatment is platform-specific)
- Section staleness badges are **independent** of the screen-level offline banner — both can be shown simultaneously (see ADR-010 Two-Phase Staleness failure mode matrix)
- When a stale section recovers (channel succeeds), clear its badge immediately — do not wait for a full screen refresh

#### Poll Backoff

When poll failures trigger staleness, apply exponential backoff to the poll interval to avoid hammering a degraded endpoint:
- On each failure: double the current interval (minimum: original `intervalMs`)
- Cap at **30 seconds** regardless of original interval
- On success: restore the original `intervalMs` immediately

---

## 4. Action System

Defines server-controlled interactivity. Every interactive component carries an `actions` array where each action declares its `trigger`. When a trigger fires, the client collects **all** actions matching that trigger (in declared order) and passes the full array to the action executor as a single sequence. Failure policies (`halt`/`continue`/`silent`) are evaluated across the batch — not per-action independently.

**Scope requirement:** actions are supported at multiple levels:
- screen-level defaults (optional)
- section-level actions
- nested/subcomponent actions (subsection interaction targets)
- **element-level actions** (atomic primitives: Container, Button, Image, Text)

When both parent and child define actions for the same trigger, child action scope takes precedence unless explicitly composed.

### Interaction Triggers (8 types — all in current schema)

```mermaid
graph LR
    subgraph "In Schema"
        A[onActivate]
        T[onTap — deprecated alias]
        LP[onLongPress]
        V[onVisible]
        SW[onSwipe]
        F[onFocus]
        BL[onBlur]
        S[onSubmit]
    end
    
    A -->|"neutral activation intent (tap, click, Enter, TV select)"| EX[Action Executor]
    T -->|"touch / click / remote select (deprecated — use onActivate)"| EX
    LP -->|"press-and-hold"| EX
    V -->|"viewport entry — impressions"| EX
    SW -->|"directional swipe"| EX
    F -->|"D-pad focus lands"| EX
    BL -->|"D-pad focus leaves"| EX
    S -->|"form submission (Enter, return key, submit button)"| EX
    
    style EX fill:#8E44AD,color:#fff
```

  ### Current Trigger Hosting Matrix

  | Trigger | Web | Android | iOS |
  |---|---|---|---|
  | `onActivate` / `onTap` | Element-level | Element-level | Element-level |
  | `onVisible` | Element-level | Element-level | Element-level |
  | `onLongPress` | Not hosted on atomic primitives; debug-log only | Element-level on supported atomics | Element-level on supported atomics |
  | `onFocus` / `onBlur` | Focusable primitives only | Focusable primitives only | Focusable primitives only |
  | `onSubmit` | Form-context submit path | Form section submit path through shared executor | Form-context submit path |
  | `onSwipe` | `ScrollContainer`-level only | `ScrollContainer`-level only | `ScrollContainer`-level only |

  Action-field alignment is tracked in [docs/plans/plan-action-trigger-and-mutate-alignment.md](plans/plan-action-trigger-and-mutate-alignment.md). Owner follow-up: `docs/appendix-kitchen-sink.md` needs a manual sweep for the same trigger/field terminology.

### Action Types (6 categories)

| Type | Purpose | Key Fields |
|---|---|---|
| **navigate** | Go somewhere | `targetUri` (native deeplink), `webUrl` (web equivalent), `presentation` (push/modal/fullscreen/replace/external), `modalHeight` (compact/half/full) |
| **fireAndForget** | Fire a beacon | `event` (name), `params` (arbitrary k/v), `destinations` (adobe/firebase/internal/all) |
| **mutate** | Change local UI state | `target` (state key), `operation` (set/toggle/increment/append), `value` |
| **dismiss** | Close modal/overlay/screen | `target` (modal/overlay/screen) |
| **refresh** | Force re-fetch | `target` (section ID, or omit for full screen); `endpoint` + `paramBindings` for parameterized form-submit refresh |
| **toast** | Show notification | `message`, `duration` |

### Parameterized Refresh (Form Submit)

A `refresh` action may carry an `endpoint` and `paramBindings` map. This is the mechanism used by `Form` sections to re-fetch server-composed content with user-selected filter values.

**Request:** the client appends resolved `paramBindings` values as URL query params and calls the shared `fetchScreen` primitive — same transport as every other composition fetch.

**Response contract:** the server returns a **screen-shaped envelope** (`id`, `schemaVersion`, `state`, `sections[]`) containing _all visually affected sections_ — both the re-composed data section(s) and the re-composed form section with the new selections already reflected. The client merges the returned sections into the current screen in place; no navigation occurs.

**Endpoint namespace:** `/v1/sdui/screen/refresh/{handlerId}` — screen prefix because the response is a screen-shaped envelope consumed by `fetchScreen`. The `handlerId` is a server-assigned identifier, not a navigable screen name.

**Why the form section must be in the response:** the form's chip/field selections are embedded in the server-composed section. Omitting the form from the response and relying on a `state` echo side-channel instead creates two owners for the same visual concern. Including the form in the response keeps the server as the single source of truth for what is displayed.

**Registry:** server-side `ParameterizedRefreshService` maps handler IDs to resolver lambdas registered at startup (`@PostConstruct`). Unknown handler IDs return 404.

### Composability — Single Trigger, Multiple Actions

```mermaid
sequenceDiagram
    participant User
    participant Renderer
    participant ActionExecutor
    participant BeaconDispatcher
    participant Navigator

    User->>Renderer: Taps a game-card AtomicComposite (scoreboard)
    Renderer->>ActionExecutor: execute(onActivate actions)
    ActionExecutor->>BeaconDispatcher: fire("scoreboard_tapped", {gameId, status})
    BeaconDispatcher-->>ActionExecutor: ack
    ActionExecutor->>Navigator: push("nba://game/0022500384/boxscore")
    Navigator-->>User: Box Score screen
```

Actions fire in order. Fire-and-forget actions execute before navigation so the beacon is guaranteed to send even if navigation takes over the UI.

### Action Failure Semantics

Each action carries an optional `onFailure` field (`halt` | `continue` | `silent`) that tells the client what to do when the action fails. If absent, the client applies a per-type default:

| Action Type | Default `onFailure` | User Feedback | Sequence Effect |
|---|---|---|---|
| **fireAndForget** | `silent` | None | Continue |
| **mutate** | `continue` | None (log warning) | Continue |
| **navigate** | `halt` | Platform-native error with server message or generic localized fallback | Halt |
| **refresh** | `continue` | Stale indicator on affected section | Continue |
| **dismiss** | `silent` | None | Continue |
| **toast** | `silent` | None | Continue |

Actions may also carry an optional `failureFeedback` object with a server-provided `message` and presentation `style` hint (`snackbar` | `toast` | `inline`). Clients fall back to a generic localized string (e.g., "Unable to open page") when this field is absent.

### Sequence Execution Contract

Actions on a single trigger execute **in declared order**. The executor resolves failure policy for each action as:

```
policy = action.onFailure ?? default_for(action.type)
```

Rules:

1. **`silent`** failures are swallowed — no user feedback, sequence continues.
2. **`halt`** failures stop the sequence — user feedback is shown (server `failureFeedback.message` if present, else generic localized fallback). No subsequent actions fire.
3. **`continue`** failures log a warning, apply any type-specific side effect (e.g., stale indicator for refresh), and proceed to the next action.
4. **Already-fired actions are committed.** There is no rollback. Fire-and-forget actions capture what the user *attempted*, not what succeeded.
5. **Navigate success also halts** — navigation takes over the screen, so subsequent actions are moot regardless of `onFailure` value.

### Element-Level Action Execution (Atomic Primitives)

Any atomic element (`Container`, `Button`, `Image`, `Text`) may carry an `actions` array. When the element's primary activation trigger fires (tap, click, Enter, TV select), clients **must**:

1. **Filter** — collect all actions where `trigger` is `onActivate` or the deprecated alias `onTap`.
2. **Batch** — pass the filtered array (in declared order) to the action executor as a single sequence.
3. **Execute with failure policy** — the executor runs the sequence using the same halt/continue/silent rules defined above.

Clients must **not** fire only the first action and ignore the rest. A typical server pattern attaches `[fireAndForget(analytics), navigate(target)]` so the analytics beacon fires before navigation takes over. Firing only the navigate loses the analytics event; firing only the analytics loses the navigation.

**Server composition responsibility:** The server owns the ordering of action sequences. Because `navigate` success halts the sequence (the screen changes), any actions declared *after* a navigate are unreachable. The client does not reorder, deduplicate, or "fix" a poorly composed sequence — it executes in declared order and stops when halt conditions are met. If the server places a toast after a navigate, the toast is dead code. That is a server composition bug, not a client concern.

**Container activation:** A Container element with `actions` is clickable/tappable as a unit. The entire container surface is the tap target. This enables card-level activation (e.g., game cards, content tiles) without requiring a nested Button.

---

## 5. Screen-Level State Management

Separate from section-level data binding. Holds state variables that `mutate` actions modify — enabling tabs, toggles, accordions, and expand/collapse without hardcoded client behavior.

```mermaid
sequenceDiagram
    participant User
    participant TabRenderer
    participant ActionExecutor
    participant ScreenStateManager
    participant ContentArea

    User->>TabRenderer: Taps "Box Score" tab
    TabRenderer->>ActionExecutor: execute(onActivate actions)
    ActionExecutor->>ActionExecutor: fire fireAndForget("tab_selected")
    ActionExecutor->>ScreenStateManager: setState("selectedTab", "boxscore")
    ScreenStateManager-->>ContentArea: recompose (SwiftUI @Published / Compose StateFlow)
    ContentArea-->>User: Box Score content appears
```

**Key decision:** Screen state is scoped to the current screen and not persisted. The server defines initial values in the section's `state` field. The client's SDUIScreenStateManager is initialized from these values and shared across all sections on the screen.

---

# Analytics Beacon Management & Impression Deduplication

> Standalone section — merge into the main SDUI Requirements Summary where appropriate.

---

## How Analytics Beacons Work in the SDUI Platform

Analytics is fully server-driven. The composition service attaches fireAndForget actions to section triggers, the client's generic action executor dispatches them to existing analytics SDKs. No per-section analytics code exists on the client.

```mermaid
graph LR
    CS[Composition Service] -->|"Attaches fireAndForget actions<br/>to section triggers"| RESP[SDUI Response]
    RESP --> REND[Section Renderer]
    REND -->|"onVisible / onActivate / onFocus<br/>triggers fire"| EXEC[SDUIActionExecutor]
    EXEC -->|"Reads event, params,<br/>destinations"| DISP[AnalyticsDispatcher]
    DISP --> ADOBE[Adobe Analytics]
    DISP --> FIRE[Firebase]
    DISP --> INT[Internal Pipeline]

    style CS fill:#1B4F72,color:#fff
    style EXEC fill:#8E44AD,color:#fff
    style DISP fill:#E67E22,color:#fff
```

### Responsibility Split

| Layer | Responsibility |
|---|---|
| **Composition Service** | Decides what beacons to attach — event name, every param, which backends receive it. Owns the event taxonomy. |
| **Action Schema** | Defines the contract — what a fireAndForget action object looks like (type, event, params, destinations). |
| **SDUIActionExecutor** | Dispatches fireAndForget actions to the existing AnalyticsDispatcher. Does not know what section type fired it. Generic across all sections. |
| **AnalyticsDispatcher** | Existing SDK already in the app. Routes to Adobe, Firebase, internal pipeline based on `destinations` array. Unchanged by SDUI. |
| **Section Renderer** | Wires triggers (onVisible, onActivate, onFocus) to the executor. Does not know the action is fireAndForget — just passes the action array through. |

### What the Server Controls

The server defines the complete analytics payload. The client never assembles beacon data from section fields.

```json
{
  "type": "fireAndForget",
  "event": "section_impressed",
  "params": {
    "sectionId": "scoreboard-001",
    "sectionType": "AtomicComposite",
    "gameId": "0022500384",
    "gameStatus": "live",
    "variant": "live-game-v2",
    "position": 0,
    "experimentId": "game-detail-redesign",
    "experimentVariant": "treatment-b"
  },
  "destinations": ["adobe", "internal"]
}
```

To add a param, change which backends receive an event, or add analytics to a section that previously didn't track anything — it's a server-side change. No app update, no PR to five platforms, no app store review.

### Beacon Trigger Types

| Trigger | Analytics Use Case | Fires When |
|---|---|---|
| `onVisible` | Impression tracking | Section enters viewport |
| `onActivate` | Engagement tracking | User activates (tap, click, Enter, TV select) |
| `onTap` | Engagement tracking (deprecated — use `onActivate`) | User taps/clicks/selects |
| `onLongPress` | Secondary engagement | User long-presses (mobile) |
| `onFocus` | Browse tracking (TV) | D-pad focus lands on section |
| `onBlur` | Dwell time calculation | D-pad focus leaves section |
| `onSwipe` | Carousel interaction | User swipes within section |

---

## Impression Deduplication

### The Problem

Without deduplication, `onVisible` fires every time a section enters the viewport. User scrolls past the scoreboard, scrolls back up — the impression fires twice. For a long screen with 10+ sections, casual scrolling generates dozens of duplicate impressions per session, polluting analytics data and inflating impression counts.

### Server-Defined Dedup Policy

Deduplication policy is defined per fireAndForget action in the server response, not hardcoded in the client. This allows the analytics team to tune behavior without app updates.

```json
{
  "type": "fireAndForget",
  "event": "section_impressed",
  "params": { "sectionId": "scoreboard-001" },
  "destinations": ["adobe", "internal"],
  "impression": {
    "dedup": "once-per-screen",
    "threshold": {
      "visibility": 0.5,
      "dwellMs": 1000
    }
  }
}
```

### Dedup Strategies

| Strategy | Behavior | Use Case |
|---|---|---|
| `none` | Fire every time trigger activates | Scroll depth tracking, heatmaps |
| `once-per-screen` | Fire once per screen visit. Re-entering the screen (navigate away and back) resets. | Standard impression tracking — **recommended default** |
| `once-per-session` | Fire once per app session. Survives screen transitions, resets on app restart. | High-value impressions (hero banner, premium placement) |
| `once-per-interval` | Fire at most once per N seconds. Useful for scroll-heavy sections. | Play-by-play, infinite scroll content |

### Visibility Threshold

Controls how much of a section must be visible and for how long before the impression counts.

| Field | Type | Default | Description |
|---|---|---|---|
| `visibility` | float (0.0–1.0) | `0.5` | Fraction of section area visible in viewport. 0.5 = 50% visible. |
| `dwellMs` | integer | `1000` | Milliseconds the section must remain visible before firing. Prevents rapid-scroll false positives. |

```mermaid
stateDiagram-v2
    [*] --> OffScreen: Section not in viewport
    OffScreen --> PartiallyVisible: Scroll brings section into view
    PartiallyVisible --> ThresholdMet: visibility >= 0.5
    ThresholdMet --> DwellTimer: Start 1000ms timer
    DwellTimer --> Fired: Timer completes while still visible
    DwellTimer --> ThresholdMet: Scrolled away before timer — reset
    Fired --> Deduped: Marked as fired per dedup strategy
    Deduped --> Deduped: Subsequent visibility events ignored
    
    PartiallyVisible --> OffScreen: Scrolled away below threshold
    ThresholdMet --> PartiallyVisible: Dropped below threshold
```

### How It Integrates with the Action Executor

The impression tracker sits between the renderer's visibility report and the action executor's dispatch:

```mermaid
sequenceDiagram
    participant Renderer
    participant VisibilityObserver
    participant ImpressionTracker
    participant ActionExecutor
    participant AnalyticsSDK

    Renderer->>VisibilityObserver: Section 60% visible
    VisibilityObserver->>ImpressionTracker: visibility(scoreboard-001, 0.6)
    Note over ImpressionTracker: threshold = 0.5 ✓
    ImpressionTracker->>ImpressionTracker: startDwell(1000ms timer)
    Note over ImpressionTracker: 1000ms passes, still visible
    ImpressionTracker->>ImpressionTracker: shouldFire? dedup=once-per-screen
    Note over ImpressionTracker: Not yet fired ✓
    ImpressionTracker->>ActionExecutor: execute(onVisible fireAndForget action)
    ActionExecutor->>AnalyticsSDK: fire("section_impressed", params)
    Note over ImpressionTracker: Mark as fired for this screen visit

    Note over Renderer: User scrolls away and back
    Renderer->>VisibilityObserver: Section 80% visible again
    VisibilityObserver->>ImpressionTracker: visibility(scoreboard-001, 0.8)
    ImpressionTracker->>ImpressionTracker: shouldFire? Already fired for this screen
    Note over ImpressionTracker: Suppressed — dedup active
```

---

## Decisions Made

| Decision | Choice | Rationale |
|---|---|---|
| Where does dedup policy live? | Server-defined per fireAndForget action | Analytics team tunes without app updates |
| Default dedup strategy | `once-per-screen` | Industry standard — Airbnb, DoorDash use similar |
| Default visibility threshold | 50% area visible | IAB standard for digital ad impressions |
| Default dwell time | 1000ms | Filters rapid-scroll false positives without losing legitimate impressions |
| Where does dedup tracking live? | `SDUIImpressionTracker` (new class, used by ActionExecutor) | Keeps executor clean, tracker is testable in isolation |
| Tap/engagement analytics dedup? | No dedup — always fire | Every tap is intentional user action, should always track |
| Screen scope reset? | On screen re-entry (navigate away + back) | Standard session analytics behavior |

## Open Decisions

Pending impression semantics decisions are tracked in [ADR-009](adr/009-impression-dedup-and-visibility-semantics.md) and reviewed through the ADR approval flow.


---

## 6. Client Infrastructure — Written Once

Four platform-agnostic systems, each implemented once per platform (iOS/tvOS and Android/Fire TV):

| System | Responsibility | Inputs |
|---|---|---|
| **SDUIStateManager** | Opens SSE/poll channels, patches data fields via bindings | `dataBindings` from section |
| **SDUIActionExecutor** | Dispatches navigate/fireAndForget/mutate/dismiss/refresh to existing app systems | `actions` from section |
| **SDUIScreenStateManager** | Holds mutable screen state for tabs/toggles/accordions | `state` from section |
| **Section Renderer** | Thin wiring: reads state, maps to design system component, attaches gesture triggers | All of the above |

The action executor and state manager **do not replace** existing app infrastructure (navigation stack, analytics SDK, deeplink router). They **call** those systems based on server instructions.

---

## 7. Codegen Pipeline

```mermaid
graph LR
    SCHEMA[sdui-schema.json] -->|quicktype| KOTLIN[Kotlin Models<br/>Jackson annotations]
    SCHEMA -->|quicktype| SWIFT[Swift Models<br/>SduiModels.swift]
    SCHEMA -->|quicktype| TS[TypeScript Models<br/>SduiModels.ts]
    
    KOTLIN --> ANDROID[Android/Fire TV Renderer]
    SWIFT --> IOS[iOS/tvOS Renderer]
    TS --> WEB[Web Renderer]
    
    style SCHEMA fill:#1B4F72,color:#fff
```

**Key decision:** Codegen generates **typed data models only** — not UI code. The renderer layer is hand-written (~30 lines per section type) because it bridges the gap between generic SDUI data and your specific design system components. This is a one-time cost per section type, not per feature change.

---

## 8. Platform Coverage

Each platform family receives a tailored composition from the server while sharing the same schema, codegen, and data pipeline. The client identifies itself via the request envelope; the composition service routes to the appropriate composer.

| Platform family | Platforms              | Renderer                         | Transport                    | Interaction model     | Composition profile |
|---|---|---|---|---|---|
| Mobile | iOS (iPhone/iPad) | SwiftUI | URLSession + EventSource | Touch gestures | Full section set, responsive breakpoints |
| Mobile | Android (Phone/Tablet) | Jetpack Compose | OkHttp + SSE client | Touch gestures | Full section set, responsive breakpoints |
| Web | Browser | React + design system | fetch + EventSource | Mouse/touch/keyboard | Wide layout, `webUrl` preferred for navigation |
| TV | tvOS (Apple TV) | SwiftUI + focusable() | URLSession + EventSource | tvOS Focus Engine | Reduced sections, large art, focus-driven |
| TV | Fire TV | Compose + onFocusChanged | OkHttp + SSE client | D-pad focus | Reduced sections, large art, focus-driven |

---

## 9. Gaps — What Airbnb, Lyft, and Others Also Handle

The following are important inclusions and decisions that production SDUI platforms deal with that we have not yet addressed:

### 9a. Accessibility (A11y) Descriptors

Airbnb's Ghost Platform and Lyft both embed accessibility metadata in the server response so screen readers, VoiceOver, and TalkBack receive correct labels without client-side hardcoding.

**What's needed:**
- `AccessibilityProperties` definition in schema with `label`, `role`, `hidden`, `headingLevel`, `liveRegion`, `sortOrder`, `hint` — referenced via `accessibility` field on `Section`, `Subsection`, and `AtomicElement`
- Server controls announcement text (e.g., "Nets 98, Knicks 104, 4:32 remaining in the third quarter") instead of relying on clients to assemble it from data fields
- Live region behavior for real-time sections (`liveRegion: "polite"` / `"assertive"` — score updates announce without user interaction)
- Focus ordering hints for TV platforms (`sortOrder` overrides default traversal order)

**Settled:** Accessibility metadata is nested under a dedicated `accessibility` field (type `AccessibilityProperties`) on `Section`, `Subsection`, and `AtomicElement`. Implemented in schema, Android (Compose `semantics {}`), and Web (ARIA attributes).

### 9b. Conditional Rendering / Visibility Rules

Server-driven platforms need to show or hide sections based on conditions that vary across platforms and form factors.

**Settled:** Platform-level section filtering is handled **server-side** via platform-aware composition (see Core Architecture). The server sends different section sets to different platform families — a tvOS response does not include sections meant for mobile, and vice versa. This eliminates the need for client-side `{ "when": "platform", "is": "tvOS" }` conditional rules for cross-platform differences.

**Remaining gap:** Client-side responsive breakpoints within a platform family (e.g., phone vs. tablet within mobile). The atomic `Container` with `flex` and `breakpoint` properties handles simple cases. More complex responsive rules (e.g., hide a section below 768dp) may still need a lightweight client-side visibility mechanism.

**Deferred:** Client-side visibility expressions (a `visibility` field with condition evaluation on sections) were evaluated and rejected. The server already controls which sections appear via composition — feature flags, A/B gating, user-segment filtering, and time-based conditions are all resolved server-side. Adding a client-side condition evaluator would duplicate server responsibility and violate the core SDUI principle that the server owns composition. If a narrow need for state-gated visibility emerges later, it can be revisited.

**Deferred:** Client-side visibility expressions rejected. Within-family responsive rules deferred pending evidence of need beyond atomic `flex` and `breakpoint` properties.

### 9c. Error Handling & Fallback Sections

Production SDUI screens need graceful degradation when individual sections fail.

**Built:** server-composed `ErrorState` surfaces (via `AtomicComposite` / `SduiUtils.buildErrorSection()`) on Android, iOS, and web. The server can compose an explicit error section at composition time with `title`, `message`, `icon`, and optional `retryAction`. This handles cases where the server knows at composition time that data is unavailable. Server utility `SduiUtils.buildErrorSection()` standardizes error section construction.

**Built (runtime error/loading states):** `SectionStates` added to schema and codegen. The server declares `sectionStates` on each section with live data, specifying:
- `loading.skeleton` (shimmer, spinner, placeholder, none) and `loading.minHeightDp` for loading UX
- `error.message`, `error.retryAction`, `error.hideOnError` for runtime failure UX

Web client implements `SectionErrorBoundary` (React Error Boundary catching render crashes, displaying server-defined error message and retry) and `SectionSkeleton` (renders server-hinted loading skeleton). Server composers (`GameDetailComposer`, `BoxscoreComposer`) emit `sectionStates` on all SSE and poll sections via `SduiUtils.buildSectionStates()`.

**Remaining gaps:**
- Screen-level error state when the entire SDUI response fails to load
- Timeout configuration per data binding channel
- Android `sectionStates` rendering (schema and codegen done; renderer wiring pending)

```mermaid
stateDiagram-v2
    [*] --> Rendering: SDUI Response received
    Rendering --> Live: DataBinding connected
    Live --> Stale: Connection lost
    Stale --> Live: Reconnected
    Stale --> Fallback: Timeout exceeded
    Fallback --> Live: Retry succeeds
    Rendering --> Error: Initial load fails
    Error --> Rendering: Retry
```

**Settled:** Server-defined per section via `SectionStates` (loading skeleton hint, error message, retry action, hide-on-error). Client applies a global default for sections without explicit `sectionStates`.

### 9d. Section Lifecycle & Lazy Loading

> **Status: Built** (visibility-gated refresh).

Airbnb and DoorDash implement section-level lazy loading for long scrolling screens.

**What's needed:**
- `loading` field: `"eager"` (load immediately) vs. `"lazy"` (load when approaching viewport)
- Lazy sections include a placeholder/skeleton definition in the initial response but fetch their actual data only when scrolled into view
- This is critical for the game detail page which could have 10+ sections — you don't want to open 10 SSE connections simultaneously on screen load
- Section lifecycle management: connect data binding when visible, disconnect when scrolled far out of viewport

**What's built:**
- `pauseWhenOffScreen` field on `RefreshPolicy` (boolean, default `true`) — server controls which sections pause when off-screen
- App background/foreground lifecycle pause (Phase 0): all refresh activity stops when the app/tab is backgrounded
- Viewport visibility detection: 1.5× viewport lookahead with 500ms exit debounce (web `IntersectionObserver`, Android `LazyListState`, iOS `LazyVStack`)
- Poll gating: poll loops suspend when section leaves viewport, resume with immediate fetch on re-entry
- SSE gating: messages are buffered (latest only) when section is off-screen; applied on re-entry
- Server sets `pauseWhenOffScreen: false` on critical live-score composites that should refresh continuously

**Remaining:** Eager/lazy initial-load trigger (§9d original `loading` field) is not yet built — the current implementation gates ongoing refresh but does not defer initial data fetch.

### 9d-ii. Section-Level Refresh — Mutual Exclusivity Requirement

> **Status: Built** (section-level refresh via `sectionEndpoint`).

`sectionEndpoint` section-level refresh and a non-static `screen.defaultRefreshPolicy` are **mutually exclusive** and MUST NOT appear together on the same screen.

**Rule:** A screen whose sections carry `sectionEndpoint` MUST declare `screen.defaultRefreshPolicy.type = "static"`. A screen with a non-static `defaultRefreshPolicy` (poll or SSE) MUST NOT emit `sectionEndpoint` on any of its sections.

**Rationale:** When the screen refreshes periodically, every section is replaced by the new server composition. Adding independent per-section `sectionEndpoint` polls alongside a screen-level refresh creates duplicate refreshes, conflicting lifecycle ownership (two owners for the same section content), and race conditions between screen-level and section-level results arriving at different times.

**Allowed combinations:**
- `screen.defaultRefreshPolicy: static` + sections with `sectionEndpoint` (section-level targeted refresh)
- `screen.defaultRefreshPolicy: poll/SSE` + sections with `url` (raw data overlay) or `channel` (SSE data overlay)
- `screen.defaultRefreshPolicy: static` + sections with `url` or `channel`

**Not allowed:**
- `screen.defaultRefreshPolicy: poll/SSE` + sections with `sectionEndpoint`

**Client defensive guard:** If a client receives a screen violating this rule, it MUST log a warning, skip the `sectionEndpoint` poll for the offending sections, and let the screen-level refresh policy own those sections. This prevents silent double-refresh and undefined behavior on malformed server payloads.

**What's built:**
- Schema: `sectionEndpoint` field on `RefreshPolicy` (mutually exclusive with `url`; `sectionEndpoint` takes precedence)
- Server: dual-mounted `GET/POST /v1/sdui/section/{sectionId}`; `SectionRefreshService` content-source-prefix registry; `GameDetailComposer` registers resolver at startup
- Clients: `fetchSection()` / `fetchSduiSection()` via shared envelope transport; `restartRealtimeForSection(sectionId)` for scoped teardown before merge; poll loop with error semantics (404→stop, 5xx→backoff+stale); re-evaluates new section's `refreshPolicy` enabling poll→SSE transition
- Web: policy fingerprint key (`sectionPolicyKey`) for React remount on policy change

**Mock data must not use SSE/Ably.** When a section displays mock or fallback data (i.e., the real data source is unavailable), the server MUST emit a poll `sectionEndpoint` refresh policy, not an SSE channel. Connecting Ably for data that will never arrive wastes a connection slot and creates confusing log noise. Section resolvers that serve mock data as a fallback must also return the poll policy — when real data becomes available, the resolver switches to the SSE policy on the next poll cycle, enabling a clean mock→live transition without a client release.

**GameDetail section refresh policies (implemented):**
- Pre-game: 60 s poll via `sectionEndpoint`, `pauseWhenOffScreen: true`
- Live game + real data: SSE (`{gameId}:linescore` channel), `pauseWhenOffScreen: false`
- Live game + mock/fallback data: 60 s poll via `sectionEndpoint`, `pauseWhenOffScreen: false`
- Post-game: 60 s poll via `sectionEndpoint`, `pauseWhenOffScreen: true`
- Screen-level `defaultRefreshPolicy` is `static` on the GameDetail screen; all refresh is section-owned.

### 9e. Caching & Offline Support

**What's needed:**
- Client-side cache of the last SDUI response for each screen, keyed by screen ID + context (e.g., game ID)
- Enables instant render on return visits (show cached, refresh in background)
- Cache TTL defined per-screen in the response: `"cache": { "ttl": 300, "strategy": "stale-while-revalidate" }`
- Offline mode: render cached response with a "last updated" indicator
- Critical for cold start performance — eliminates the 50–150ms parsing penalty on repeat visits

**Decision required:** Cache storage mechanism (memory vs. disk), eviction policy, and whether individual section data is cached independently or only the full screen response.

### 9f. Schema Versioning & Backward Compatibility

**What's needed:**
- Client sends schema version as a query parameter: `?schemaVersion=2.3`
- Server returns compatible response — never uses fields the client doesn't know about
- Unknown section types gracefully ignored (client skips, doesn't crash)
- Unknown action types gracefully ignored (executor skips, logs warning)
- Feature flags in schema for progressive rollout of new section types

```mermaid
sequenceDiagram
    participant App as Client (v2.3)
    participant Server as Composition Service
    
    App->>Server: GET /v1/sdui/screen/game-detail/123?schemaVersion=2.3
    Server->>Server: Check v2.3 capabilities
    Server->>Server: Exclude v2.4+ section types
    Server-->>App: Response (v2.3 compatible)
    App->>App: Parse sections
    App->>App: Skip any unknown types (defensive)
```

**Decision required:** Versioning strategy (semver vs. integer), how long to support old versions, and whether the server maintains multiple response templates or dynamically filters.

### 9g. Theming & Dark Mode

**Status:** Built. Three-layer design system implemented across all platforms. See [`sdui-design-system.md`](sdui-design-system.md) for the full reference.

**Requirements:**

- **Three-layer system:** (1) inline style primitives on atomic elements, (2) semantic variants per primitive type (`ContainerVariant`, `ImageVariant`, `TextVariant`, `ButtonVariant`) resolved per OS tier, (3) two-tier color token registry (palette primitives + semantic aliases)
- **Variant resolution:** Each platform resolves `variant` to its native design language with per-OS-tier realization. Dark-mode specs are mandatory at every declared tier. Per-variant override matrices govern whether inline props override variant defaults. Registry: [`schema/style-tokens.json`](../schema/style-tokens.json)
- **Color token resolution:** `ColorToken` wire type accepts literal hex (`#RRGGBB` / `#RRGGBBAA`) or semantic reference (`token:color.*`). Clients resolve at render time against the OS color scheme using the registry's palette/semantic alias chain. Unknown tokens log and fall back to caller default. Registry: [`schema/color-tokens.json`](../schema/color-tokens.json)
- **Codified token families:** The theming/runtime surface now includes explicit registries for spacing, radius, typography, motion, and shadow (plus font-family metadata) in addition to color and style variants; these are all build-time codified registries, not ad-hoc runtime values.
- **Scope boundary:** Variant surface colors resolve through the platform's native semantic palette (not the color-token registry). Brand and content colors on atomic element properties resolve through `ColorTokenResolver`. No overlap
- **Inline exemptions:** Alpha-bearing hex literals (compositing alpha) stay inline — not tokens. Team brand colors are now in the bundled registry (`schema/color-tokens.json` `team` section) and resolved via `nba.team.*` tokens
- **Dark mode is client-owned:** No `X-Theme` header exists or is planned. The server is not told which mode the user is in
- **Brand theme takeovers** (All-Star, Playoffs, sponsor windows) are out of scope by design — not deferral
- **Figma pipeline:** Deferred. Registry is ref-app-seeded with a Kinetic-compatible shape. See §9s

### 9h. Animation & Transition Hints

**What's needed:**
- Server can suggest entry/exit animations for sections: `"animation": { "enter": "fadeIn", "exit": "fadeOut" }`
- Score change animations (pulse, highlight) triggered by data binding updates
- Transition definitions on navigate actions already partially covered (`transition` field), but section-level animation on data change is missing

**Decision required:** Whether animations are server-suggested or purely client-side design system behavior. Recommendation: client-side default animations from the design system, with server-side override capability for special cases.

### 9i. Impression Deduplication

**Built (web):** Impression tracking implemented with `useImpressionTracking` hook using `IntersectionObserver` for viewport detection, `AnalyticsProvider` context for deduplication registry, and enhanced `ActionHandler` analytics dispatch. Supports server-defined dedup policies (`once-per-screen`, `once-per-interval`), visibility thresholds, and dwell time. ADR-009 accepted.

**Built (all platforms — visibility infrastructure):** `SectionVisibilityTracker` wired on Android, iOS, and web. `onVisible` triggers dispatch through the action executor. Dedup registry and dwell-time threshold enforcement are in place.

**Gap (server-side analytics composition):** No server composer currently attaches `fireAndForget` actions to `onVisible` triggers for impression analytics. The client infrastructure is ready — visibility detection, dedup policy enforcement, and action dispatch are built — but idle until the composition layer emits impression beacons.

**Remaining gap:** Cross-platform dedup registry parity (exposure vs. impression contract, offline buffering).

**ADR tracking:** [ADR-009](adr/009-impression-dedup-and-visibility-semantics.md) — **Accepted**

### 9j. A/B Testing Integration

**What's needed:**
- Server includes experiment metadata: `"experiment": { "id": "game-detail-v2", "variant": "treatment-b" }`
- FireAndForget actions automatically include experiment context in every beacon
- Section ordering, content, and even action behavior can vary per variant
- The composition service resolves experiment assignments from the request envelope and uses them for composition branching

**Resolved decisions (see [ADR-006](adr/006-experiment-assignment-model.md)):**

- **Client is fully authoritative.** Clients resolve assignments via experiment SDK (Amplitude) at app start (per-session) and send them as `experiments[experimentId]=variantName` in the request envelope.
- **Server trusts assignments.** `resolveVariant(experimentId, default)` reads the experiments map and branches composition. No server-side experiment resolution service.
- **Kill switch is client-side.** To disable a variant, the client stops sending that experiment. Server never sees it, falls back to default.
- **No response echo.** The client already knows its assignments — the server does not echo them back.
- **Exposure tracking is client-side** via `fireAndForget` actions. No server-side exposure logging — tracking once is sufficient.
- **Experiments are natural cache keys.** Assignments travel as query parameters, so different variants produce different cache entries.
- **`variant` param removed.** Replaced by `experiments[variant]` placeholder that exercises the real experiment code path.
- **Amplitude SDK integration deferred** — not part of this plan. Experiments are manually set for now.

**ADR tracking:** [ADR-006](adr/006-experiment-assignment-model.md) — **Accepted**

### 9k. Pagination & Infinite Scroll

**What's needed:**
- Sections that contain lists (play-by-play, player stats) need server-defined pagination
- `"pagination": { "type": "cursor", "nextCursor": "abc123", "pageSize": 20 }`
- Load-more trigger: can be an `onVisible` action on a sentinel element, or an explicit "Load More" button with a refresh action targeting the section

**Decision required:** Whether pagination is a section-level concern (section fetches its own next page) or screen-level (composition service returns next batch of items within the section data).

### 9l. Debugging & Observability

**What's needed:**
- Every SDUI response carries a `traceId` that follows it from composition through rendering
- Section-level timing: when each section started rendering, when data binding connected, when first data arrived
- Action execution logging: every action fired with timestamp, trigger, and outcome
- Visual debug overlay (dev builds): tap a section to see its raw JSON, binding status, and action definitions
- Server-side: composition service logs which sections were included, which were filtered by version/experiment/platform

**Decision required:** Observability tooling — whether to build a custom SDUI debug inspector or integrate with existing tools (New Relic, etc.).

---

### 9m. Form-Factor Layout Manager

**Settled (cross-platform):** Cross-form-factor differences (phone vs. TV vs. web) are handled by platform-aware composition — each platform family receives a response composed for its form factor. The server owns section selection and ordering per platform.

**Within-family layout hints:** The prior `SectionLayoutHints` mechanism was removed (2026-05-25) because it duplicated the chrome ownership that `Section.surface` already provides. Inter-section margins now route through `Section.surface.margin`; section outer chrome is owned exclusively by `SectionContainer` reading `section.surface`. See `docs/sdui-design-system.md §2` (Box-model cascade) for the active model.

**Remaining gap:** Advanced layout features (multi-column, placement slots) deferred to surface expansion.

**ADR tracking:** [ADR-008](adr/008-form-factor-layout-manager.md) — **Superseded by [ADR-015](adr/015-section-chrome-single-ownership.md)**, which ratifies the single-ownership path for section outer chrome (`Section.surface` consumed by `SectionContainer`) and references the box-model cascade in `docs/sdui-design-system.md §2`.

### 9n. Ad Support as First-Class Primitive

**What's needed:**
- Ad placement represented in SDUI as explicit primitive/section type (for example `AdSlot`) instead of implicit module/card side-effects.
- Required fields (provider, ad unit path, sizes, targeting, collapse-on-empty behavior, refresh policy, fallback policy).
- Standard behavior for no-fill, failure, and retry without breaking screen composition.

**Decision required:** where ad auction/targeting context is resolved (composer vs ad SDK boundary), and which ad metadata fields are mandatory in contract.  
**ADR tracking:** [ADR-007](adr/007-ad-boundary-and-contract.md)

### 9o. Composition Input and Request Model

**What's needed:**
- A typed request envelope that composition uses for deterministic output:
  - screen/entity identifiers
  - platform/app version/device class
  - locale/region/timezone
  - auth context (identity token in header)
  - device context (device ID, ZIP code, country code, region)
  - experiment assignments
  - client capabilities (e.g., SSE support)
  - traceId
- Contract guarantees for required vs optional context fields.
- Auth via `Authorization` header (bearer JWT/session token); do not pass tokens in query params.
- Request method policy:
  - support both `GET` and `POST` depending on screen/section context
  - use `POST` when request context is large/sensitive or requires complex composition inputs
  - allow authenticated `GET` for read-only compositions when appropriate
- Locale transport policy:
  - `GET` requests: `locale` as a query parameter (e.g., `?locale=es`) — naturally part of the CDN cache key
  - `POST` requests: `locale` in the request body alongside the rest of the typed context
  - Default: `en` if omitted in either case
  - Do not use `Accept-Language` header — cache fragmentation from browser-specific header values is unacceptable
- Cacheability classification per screen/section (`public`, `contextual`, `personalized`, `live`) to drive edge/private/no-store policy.
- Caching strategy:
  - section-first caching as primary strategy
  - optional screen snapshot caching for fast first paint/fallback

**Resolved decisions:**

- GET-first with bracket-notation nested params (`platform[deviceClass]=phone`, `platform[capabilities]=sse`, `experiments[exp_id]=variant_b`). Composition-relevant context travels as query parameters — naturally part of the CDN cache key.
- POST fallback on the same URL with a JSON body of the same shape, when query string exceeds 8192 characters.
- `Authorization` is the only required header. Analytics/observability headers (`X-Platform`, `X-App-Version`, `X-OS-Version`, `X-Trace-Id`, `X-Request-Id`) travel as headers, not query params. Edge-injected headers (`X-Resolved-Country`, `X-Resolved-Market-Cohort`) supply geo context.
- Platform identity travels as the `X-Platform` header (analytics only); `schemaVersion` remains in the query. Device context for composition uses `platform[deviceClass]` and `platform[capabilities]` in the envelope.
- All device context fields are optional — server tolerates missing fields gracefully with sensible defaults.
- All timestamps in UTC — no timezone in the request envelope. Timezone-aware formatting is a client presentation concern.
- `variant` query param removed — all variant resolution uses the `experiments` map exclusively.
- Cache-Control headers set per route cacheability class: `public` (shared screens), `contextual` (locale-varying), `personalized` (user-specific), `live` (real-time, `no-cache`). See D7 route→cacheability mapping.
- **URL namespace — three prefixes, three response shapes:**
  - `/v1/sdui/screen/` — SDUI screen compositions. Response is a full screen envelope (`id`, `navigation`, `sections[]`, `state`). Used for initial loads, navigation, pull-to-refresh, and parameterized form-submit refresh (`/v1/sdui/screen/refresh/{handlerId}`).
  - `/v1/sdui/section/` — SDUI section re-compositions. Response is a single `Section` object. Used for section-level polling and SSE-triggered re-composition.
  - `/v1/api/` — Raw domain data (not SDUI-shaped). Consumed via `dataBinding` path rules; never decoded as a screen or section.
- **All composition routes are dual-mounted GET + POST to the same handler.** This includes `/v1/sdui/screen/refresh/{handlerId}`, the parameterized-refresh endpoint that backs Form-driven section refreshes. There are no GET-only or POST-only composition endpoints.
- **Every composition fetch on every client routes through one shared primitive** (`SduiRepository.fetchScreen` / `fetchSduiScreen` on web), regardless of whether it's an initial load, a navigation, a pull-to-refresh, or an action-driven `refresh` with `paramBindings`. That primitive owns baseURL resolution, envelope serialization, GET/POST length-fallback, RFC-3986 percent-encoding, deterministic key ordering, and `X-Trace-Id` propagation. Hand-rolled URL strings or per-action transports are not allowed.
- **User-supplied filter params** (Form bindings such as `season=2025-26`, refresh `paramBindings`) ride the URL query string regardless of HTTP method, so the server reads them through `@RequestParam` on either side. They participate in the GET/POST length decision alongside the envelope.

**ADR tracking:** [ADR-003](adr/003-composition-api-contract.md), [ADR-004](adr/004-transport-and-caching-policy.md)

### 9p. Internationalization (i18n)

**What's needed:**

The SDUI platform must support multilingual content delivery. The composition server controls the initial response and can pre-translate all text, but real-time updates (Ably SSE) and direct CDN polling bypass the server — the DataBindingApplier writes values from external sources directly into section data. If those sources deliver untranslated strings, the client has no translation opportunity.

**Two-layer strategy:**

1. **Server-resolved text (default):** The composition service receives the user's locale via a `locale` query parameter (e.g., `?locale=es`). If omitted, the server defaults to `en`. All text in the response is pre-translated. Clients render strings as-is. This covers the initial response and any data that flows through the composition service. The `locale` query parameter is part of the URL and therefore naturally part of the CDN cache key — no `Vary` header needed, no cache fragmentation from browser-specific `Accept-Language` strings.

2. **Optional string keys on data bindings:** For fields that may be updated via data bindings from external sources (SSE channels, direct CDN polling), the server can attach optional string keys to the binding configuration. The client checks for a string key after applying a binding — if one exists, it resolves the translation locally using platform-native i18n. If no key exists, the raw value is used as-is.

**Schema addition:**

Add an optional `stringKeys` map to the `DataBinding` definition. This keeps translation keys co-located with the bindings they apply to:

- `stringKeys` is a map of `targetPath` to a translation key string
- Optional — omit entirely when no bound fields need client-side translation
- The server decides which bound fields get keys; the mechanism is general-purpose and applies to any string field that arrives via a binding

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

**Client responsibility:**

- On initial load: render server-provided strings directly (no i18n work)
- On binding update: after applying a binding, check if the target path has a corresponding `stringKeys` entry. If so, resolve using the section-level `stringTable` (server-provided per-locale string map). Fall back to the raw value if no local translation exists.
- For parameterized strings: if the stringTable value contains `{0}`, substitute the raw bound value into the template. Single-value `{0}` templates only; multi-value deferred.

**Server responsibility:**

- Resolve locale from the `locale` query parameter (default: `en`) and send pre-translated text in all string fields
- Populate section-level `stringTable` with localized strings for any fields that need client-side resolution
- Attach `stringKeys` entries to data bindings for any bound fields where the external data source may deliver untranslated strings — **deferred** (schema + client support built; no composer sets stringKeys yet)
- Ensure all section-level refresh endpoints (direct-URL polling) include `locale` so refreshed sections carry the correct string table
- Locale is part of the URL and naturally part of the CDN cache key
- Parameterized strings (~1% of total) handled by server-side atomic decomposition — no client interpolation code needed

**Decision required:** Governance for string key taxonomy, ownership of server-side translation bundles, and which platform i18n libraries are standard per client.

### 9q. Tabular Data Sections and Forms

**Problem:** Apps display tabular stat views (boxscore, roster, standings, league leaders) with shared UX patterns — frozen first column, horizontal scroll, sortable columns, aggregation rows — but each table has a distinct domain-specific data shape and platform-specific rendering needs (headshots, badges, combined value formatting). Users also need settings controls (season picker, season-type toggle) to drive which dataset is displayed.

**Decision: semantic table types over generic DataTable.** The server describes *what* the data is (e.g., `BoxscoreTable` with typed player statistics); clients decide *how* to render it (column order, frozen behavior, headshot rendering, sort UX). Client teams own rendering reuse via internal base components (e.g., `BaseDataTable` shared across `BoxscoreTableRenderer`, future `RosterTableRenderer`, `StandingsTableRenderer`). This follows the established semantic pattern used for all existing section types.

Key factors:
- Different tables have genuinely different data shapes — a boxscore row (player + game stats) is fundamentally different from a roster row (player + bio/contract) or a standings row (team + record/GB)
- Platform-specific rendering needs per table type (circular headshots in boxscore, position badges in roster, clinch indicators in standings) would require complex cell metadata in a generic approach
- Semantic types provide meaningful analytics events ("viewed BoxscoreTable" vs. "viewed DataTable")
- The "schema explosion" concern is overstated — each new table type is ~30 lines of JSON schema; codegen handles type generation; clients share a base table renderer internally

**Decision: generic `Form` section for settings.** Unlike tables, form pickers genuinely share shape regardless of domain — a season picker and a position picker have the same structure (`label`, `options[]`, `stateKey`). A single `Form` section type with extensible field types (`picker`, `segmented`, `toggle`, `datePicker`, `text`) serves boxscore, roster, and future table settings.

**Decision: client-side sort for tabular data.** Tabular data payloads are small (≤15 rows for a boxscore). Sort is performed client-side via `mutate` actions updating `Screen.state`, with no server round-trip. Sort state persists across live poll refreshes because it resides in `Screen.state`, not in section data.

**Decision: parameterized refresh actions.** The `refresh` action type is extended with optional `endpoint` (target URL) and `paramBindings` (map of param name → state key). At execution time, the client resolves state values for each binding and appends them as query parameters to the refresh endpoint. This lets a Form submit button say "refresh the screen with `season={state.season}&seasonType={state.seasonType}`" — reusable for any server-driven settings interaction.

**Requirement statements:**

- New section types (`BoxscoreTable`, `Form`) must follow the existing section anatomy contract (data, actions, refreshPolicy, dataBindings)
- Sort state must survive live poll refreshes (state lives in `Screen.state`, refresh replaces section data only)
- Server must pre-populate `Screen.state` with default sort column/direction per table and current form field values
- State keys must be namespaced per section to avoid collisions when multiple tables appear on the same screen (e.g., `boxscore_home_sortCol`, `boxscore_away_sortCol`)
- `BoxscoreTable` must support an `emptyMessage` field for pre-game or missing-data states; server may also omit the section entirely
- `BoxscoreTable` must include a `teamTotals` aggregation row that renders as a frozen bottom row excluded from client-side sorting
- `Form` field changes accumulate in `Screen.state`; submit fires a `refresh` action with `paramBindings` resolved from current state
- Parameterized refresh (`endpoint` + `paramBindings` on Action) must be backward-compatible — existing refresh actions with only `target` continue to work unchanged

**Grid vs. Section Decision Tree** — defines the hard boundary between `DisplayGrid` (atomic primitive) and semantic table sections:

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

`DisplayGrid` is a deliberately different primitive from the generic `DataTable` rejected above: a **display-only, non-interactive, server-ordered grid of text cells**. Zero client interaction — no sort, no filter, no expand, no select, no tap. The moment any of those constraints break, promote to a semantic section type. Use for simple stat snapshots, schedule lookups, standings summaries, and any case where the grid is purely cosmetic output.

### 9r. Section vs. Atomic Classification — Implementation Details

Every section renderer must be classified as either a semantic section (client-owned native renderer) or an `AtomicComposite` (server-composed atomic tree). The classification is based on three implementation-level criteria that determine whether the server can fully describe the surface at composition time.

**`data.ui` on semantic sections — the presentation/state boundary:**

Semantic sections exist because the client must own some behavior (tab switching, sort state, SDK lifecycle). That behavioral justification does not extend to the section's *visuals*. When a semantic section carries an optional `data.ui` tree, it means:

- **Server** owns the presentation — the atomic tree is rendered wholesale, exactly as in `AtomicComposite`. Visual redesigns ship server-side without a client release.
- **Client** owns the stateful behavior that justified the section's existence (interaction state, SDK integration, or runtime lifecycle).

The client renders `data.ui` as-is for the sub-surface it governs. It does not cherry-pick portions of the tree or merge server-sent visuals with client templates. When `data.ui` is absent, the client falls back to its native template for that sub-surface.

This split prevents semantic sections from becoming all-client black boxes where every visual change requires a release, while preserving the client's ownership of behavior the server cannot execute at composition time.

**Classification criteria:**

| Criterion | What it means | Section examples | Implementation impact |
|---|---|---|---|
| **Network-driven lifecycle** | Client subscribes to a live data source (Ably SSE, polling) after initial render and updates visual state based on incoming data | **BoxscoreTable** — connects to Ably channel `{gameId}:boxscore` for real-time stat updates. Ably SSE subscriptions also drive live-score `AtomicComposite` surfaces, where `section.dataBindings` writes into `content.*` and leaf primitives read via `bindRef`; the `LiveClock` primitive owns the client-side tick animation between authoritative snapshots. The channel subscription, reconnection on network change, and live-state rendering are runtime lifecycle concerns. | Client owns `refreshPolicy` execution and SSE channel lifecycle per section. For live-score surfaces, the pipeline is: SSE message → `DataBindingResolver.applyBindings` writes into `content.*` → descendants re-resolve `bindRef` on next render. `LiveClock` re-anchors on each snapshot change — no drift accumulation. |
| **Platform SDK integration** | Section delegates rendering or transaction flow to a platform-native SDK that owns its own view lifecycle, authentication, and state machine | **SubscribeHero / SubscribeBanner** — Google Play Billing Library (Android) / StoreKit 2 (iOS). SDK manages: product loading, purchase initiation, receipt verification, entitlement caching, localized price formatting. CTA text depends on entitlement state (`Subscribe` vs `Subscribed` vs `Upgrade`). **AdSlot** — Google Ad Manager. SDK manages: ad request, fill/no-fill, viewability tracking (MRC-compliant), consent (UMP/TCF), timed refresh, and creative rendering. | Client section renderer instantiates SDK views, wires lifecycle callbacks, handles SDK-specific error states. Atomic `Button`/`Container` cannot host native SDK views or participate in SDK lifecycle callbacks. `SectionSlot` is the escape hatch when an SDK-dependent section must be embedded inside an atomic layout. |
| **Client-owned interaction state** | Section manages `remember{}`/`useState` for coordinated scroll, sort, selection, form input, or nested section orchestration | **BoxscoreTable** — frozen column position + horizontal scroll offset synchronized via `ScrollState`. Sort column/direction in `remember{mutableStateOf()}`. Starter/bench divider insertion. **Form** — per-field expansion state, dropdown open/close, field validation. **TabGroup** — `mutate` action updates `screenState`, drives child section list. | Atomic elements have no local state primitive. All state lives in `screenState`, which covers simple key-value cases (tab selection, toggle). Coordinated multi-axis scroll and per-field form state require client render logic. |

**Section classification inventory:**

| Tier | Sections | Criterion | Disposition |
|---|---|---|---|
| **Atomic surfaces** | Headers, rails (content / following / story-circle), hero panels, promo banners, stat lines, video carousels, schedule layouts, live-score cards, error states | Stateless, no SDK deps, no lifecycle. Live-state surfaces use `section.dataBindings` to write into `content.*`, leaves read via `bindRef`, and the `LiveClock` primitive owns the tick animation. | Server-composed `AtomicComposite`. Not enumerated in the `Section.type` enum — all share the single `AtomicComposite` section type. |
| **Semantic sections** | BoxscoreTable, SeasonLeadersTable | Client-owned interaction state (frozen scroll sync, sort) | Client manages coordinated scroll and sort state. |
| **Semantic sections** | Form | Client-owned interaction state (field expansion, validation, submit) | Client manages per-field state. |
| **Semantic sections** | TabGroup | Client-owned interaction state (nests child sections) | Section container — orchestrates child section rendering. |
| **Semantic sections** | SubscribeHero, SubscribeBanner | Platform SDK (Play Billing / StoreKit 2) | Client section integrates billing SDK lifecycle. |
| **Semantic sections** | AdSlot | Platform SDK (Google Ad Manager) | Client section integrates ad SDK lifecycle. |
| **Semantic sections** | VideoPlayer | Platform SDK (AVPlayer / ExoPlayer / Media3 / HLS.js) | Client section drives HLS/DASH playback, PiP, AirPlay / Chromecast, background audio, fullscreen rotation. `playerType` discriminator dispatches to the right SDK entry point. |

Breakpoint-responsive horizontal containers are handled by atomic `Container(direction=row)` with `flex` and `breakpoint` properties.

**Implementation contract for new sections:**

When adding a new section type, apply the classification criteria above. If all three criteria are absent (no lifecycle, no SDK, no local state), implement as an `AtomicComposite` template in the server composition layer — no client code needed. If any criterion is present, implement a semantic section renderer on each platform.

### 9s. Figma Design Token Integration

**Status:** Partial. Token registries are codified and in use, while the upstream
Figma export pipeline remains deferred.

**Requirements:**

- Eight machine-readable token families are codified with Kinetic-compatible shapes:
  - Color: [`schema/color-tokens.json`](../schema/color-tokens.json)
  - Icon: [`schema/icon-tokens.json`](../schema/icon-tokens.json)
  - Style: [`schema/style-tokens.json`](../schema/style-tokens.json)
  - Spacing: [`schema/spacing-tokens.json`](../schema/spacing-tokens.json)
  - Radius: [`schema/corner-radius-tokens.json`](../schema/corner-radius-tokens.json)
  - Typography: [`schema/typography-tokens.json`](../schema/typography-tokens.json)
  - Motion: [`schema/motion-tokens.json`](../schema/motion-tokens.json)
  - Shadow: [`schema/shadow-tokens.json`](../schema/shadow-tokens.json)
- Font-family metadata is codified in [`schema/font-tokens.json`](../schema/font-tokens.json) for platform resolution (Android assets, iOS PostScript/system, web Google Fonts/local).
- Server-side token emission must be consistency-checked against the registry at startup — a mismatch must fail the build, not surface silently at runtime
- Each client must resolve color tokens at render time against the OS color scheme (light/dark) using the registry's palette/semantic alias chain
- CI validators must assert registry structure, hex format, alias integrity, and no dangling references
- The Figma export pipeline itself remains deferred; when it lands, it replaces registry values wholesale while keeping the existing registry shape as the contract

Implementation details for server plumbing, per-platform resolvers, and CI scripts are in [`sdui-design-system.md`](sdui-design-system.md).

---

## ADR Status Summary

The following requirements are tracked via ADRs. Some remain pending final cross-functional approval; others have been accepted.

| Topic | ADR | Current State |
|---|---|---|
| SDUI runtime vs legacy card refactor | [ADR-001](adr/001-sdui-runtime-vs-legacy-card-refactor.md) | Proposed |
| Composition ownership and transition | [ADR-002](adr/002-composition-ownership-and-transition.md) | Proposed |
| Composition API contract (request/response) | [ADR-003](adr/003-composition-api-contract.md) | Proposed |
| Transport and caching policy | [ADR-004](adr/004-transport-and-caching-policy.md) | Proposed |
| Action scope and precedence | [ADR-005](adr/005-action-scope-and-precedence.md) | Proposed |
| Experiment assignment model | [ADR-006](adr/006-experiment-assignment-model.md) | Accepted |
| Ads boundary and contract | [ADR-007](adr/007-ad-boundary-and-contract.md) | Proposed |
| Form-factor layout manager | [ADR-008](adr/008-form-factor-layout-manager.md) | Superseded by ADR-015 |
| Impression dedup and visibility semantics | [ADR-009](adr/009-impression-dedup-and-visibility-semantics.md) | Accepted |
| Offline and degraded connectivity | [ADR-010](adr/010-offline-and-degraded-connectivity.md) | Proposed |
| Data classification and freshness model | [ADR-011](adr/011-data-classification-and-freshness-model.md) | Proposed |
| Client data architecture | [ADR-012](adr/012-client-data-architecture.md) | Proposed |
| Style tokens for atomic primitives | [ADR-013](adr/013-style-tokens-for-atomic-primitives.md) | Accepted |
| Dynamic conditional properties | [ADR-014](adr/014-dynamic-conditional-properties.md) | Proposed |
| Section chrome single ownership | [ADR-015](adr/015-section-chrome-single-ownership.md) | Accepted |

Until approved, these remain directional requirements and may be refined.

---

## 10. Requirement Status Matrix

| Requirement | Status | Notes |
|---|---|---|
| Schema definition (section types, data shapes) | **Built** | JSON Schema with semantic types. Prototype validated. |
| Codegen pipeline (schema → typed models) | **Built** | quicktype (Kotlin/Swift/TS), jsonschema2pojo (Java — legacy fallback) |
| Android renderer (Compose) | **Built** | Section router + 8 semantic section renderers + AtomicRouter with 12 atomic primitives incl. `LiveClock` and `OverlayContainer`. `IconTokenResolver` + bottom navigation shell resolve `sdui:*` icon tokens to Material Symbols. |
| Web renderer (React) | **Built** | React section router + 8 semantic section renderers + AtomicRouter with 12 atomic primitives incl. `LiveClock`, `OverlayContainer`, and live data wrappers. `IconTokenResolver` + Material Symbols font for top navigation bar. |
| iOS renderer (SwiftUI) | **Built** | Swift Package (`ios/`) with SwiftUI section router + 8 semantic section views + AtomicRouter with 12 atomic primitives incl. `LiveClock` and `OverlayContainer`. Server-declared bottom `SduiNavigationShell` resolves `sdui:*` icon tokens to SF Symbols. Real-time via Ably (`AblyChannelManager` actor) + `PollingDriver`. `SectionVisibilityTracker` wired. Demo app (`SduiDemo`, XcodeGen, `make ios-run`) bootstraps `nba://for-you`. |
| Data binding (SSE/poll, field-level) | **Built** | Ably for SSE, direct-URL polling, DataBindingResolver class exists but live updates use hardcoded mapping |
| Action system (navigate, fireAndForget, mutate) | **Built** | All clients dispatch the 6 action types with canonical wire fields (`targetUri`/`webUrl`, `target`/`operation`/`value`). Trigger-alignment follow-up tracked in `docs/plans/plan-action-trigger-and-mutate-alignment.md`. |
| Screen state management (tabs, toggles) | **Built** | StateManager, TabGroup wired |
| Composition service (server-side) | **Built** | Spring Boot, demo + live mode, A/B variants |
| Accessibility descriptors | **Built** | Schema `accessibility` field on Section, Subsection, AtomicElement. Android Compose `semantics{}`, web ARIA attributes. All renderers wired. |
| Conditional rendering / visibility | **Partial** | Cross-platform: settled (server-side composition). Client-side visibility expressions deferred (server handles show/hide). Within-family responsive: gap |
| Error handling & fallbacks | **Partial** | Server `ErrorState` (AtomicComposite) built. Client `SectionErrorBoundary` built on Android, web, and iOS (catch-at-dispatch + pre-validation). `SectionSkeleton` built on Android, web, and iOS. `hideOnError`, `retryAction`, retry budget (client-side, default 5) implemented. §12-compliant logging. Contract §13 updated. Gap: tvOS/Fire TV not started. |
| Section lifecycle & lazy loading | **Partial** | Visibility-gated refresh built (poll/SSE pause when off-screen, app background pause, `pauseWhenOffScreen` schema field). Eager/lazy initial-load trigger still gap. |
| Caching & offline | **Gap** | Stale-while-revalidate, cold start optimization |
| Schema versioning protocol | **Built** | Server version routing, field stripping, and force-upgrade signal (`X-Schema-Version-Mismatch: upgrade-required`) implemented. All clients detect header and display upgrade prompt. Version format: major.minor. |
| Composition ownership model (SDUI composer as source of truth) | **Partial** | Architecture intent clear; transitional CoreAPI-derived composition still in use |
| Request context envelope for composition | **Built** | `SduiRequestContext` POJO + `BracketParamResolver` (bracket-notation GET, POST fallback). Android, iOS, and web `RequestEnvelopeBuilder`. All fields optional with defaults. |
| Composition API contract (auth, method, cacheability) | **Built** | GET-first with bracket-notation params; POST fallback >8192 chars; `Authorization` header only; Cache-Control per D7 route mapping; `X-Trace-Id` header for observability |
| Actions at subsection level | **Partial** | Supported conceptually; needs explicit schema examples and conformance tests |
| Form-factor layout manager | **Partial** | Cross-platform: settled (server-side composition). Within-family margins route through `Section.surface.margin`; section outer chrome via `Section.surface` and `SectionContainer`. Within-family responsive layout still requires design (see §9b). |
| Ad support as first-class primitive | **Gap** | Needs ad primitive definition and fallback behavior |
| Theming / dark mode | **Built** | Three-layer design system (inline primitives, variants, token registries). Layer 1 inline primitives now flow through a single per-platform `AtomicBox` helper so margin, opacity, shadow, corner clip, background, border, padding, sizing, and badge semantics apply consistently across atomic primitives. Variants ship per-primitive with per-OS-tier realization and override matrices (`schema/style-tokens.json`). Token families are codified across color (`schema/color-tokens.json`), spacing (`schema/spacing-tokens.json`), radius (`schema/corner-radius-tokens.json`), typography (`schema/typography-tokens.json`), motion (`schema/motion-tokens.json`), and shadow (`schema/shadow-tokens.json`), with font metadata in `schema/font-tokens.json`; clients resolve these registries locally against form factor and OS theme. Server composers emit token constants with startup consistency checks against registries. Figma export pipeline remains deferred (downstream integration target). |
| Animation hints | **Gap** | Entry/exit + data-change animations |
| Impression deduplication | **Partial** | Visibility infrastructure (`SectionVisibilityTracker`, `onVisible` dispatch, dedup registry) built on all platforms. Server does not yet compose `fireAndForget` impression beacons on `onVisible` triggers. ADR-009 accepted. |
| A/B testing integration | **Built** | Fully client-authoritative (ADR-006 Accepted). `experiments` map replaces `variant` param. Kill switch is client-side. Exposure tracking via `fireAndForget` actions. Amplitude SDK integration deferred. |
| Pagination / infinite scroll | **Gap** | Cursor-based, server-defined |
| Debugging / observability | **Partial** | traceId in responses; structured Logcat; no dashboards |
| Contract testing | **Gap** | No automated contract tests yet. Contract tests verify cross-platform conformance (schema ↔ server ↔ clients) and are distinct from per-requirement unit tests. All other requirements should have appropriate unit and integration tests when productionized. |
| Internationalization (i18n) | **Built** | Section-level `stringTable` stamped by server per locale. Server pre-translates initial text. Clients consume `stringTable` from each section. Parameterized strings via atomic decomposition. `stringKeys` on data bindings deferred to production server requirements. |
| Tabular data sections (BoxscoreTable) | **Built** | Semantic table type with domain-typed data, client-side sort, frozen column/totals row. Built on Android, iOS, and web. |
| Form section (generic) | **Built** | Extensible field types (picker, segmented, toggle, datePicker, text), parameterized refresh on submit. Built on Android, iOS, and web. |
| Section-level refresh (`sectionEndpoint`) | **Built** | Schema field on `RefreshPolicy`; `GET/POST /v1/sdui/section/{sectionId}` via `SectionRefreshService` prefix registry; all three clients poll via `fetchSection`, replace section in place, re-evaluate new `refreshPolicy` (poll→SSE transition); mutual exclusivity with non-static screen policy enforced |
| Screen-level `defaultRefreshPolicy` handler | **Built** | `type: poll` + `intervalMs` triggers full-screen re-fetch on all three platforms; `GameDetailComposer` emits `type: static` (sections own refresh independently) |
| Parameterized refresh (Action extension) | **Built** | `endpoint` + `paramBindings` resolved from screen state at action time. Working via Form submit. |
| ErrorState section | **Built** | Server-composed error sections with title, message, icon, retry action. Built on Android, iOS, and web. |
| SectionStates (runtime error/loading) | **Partial** | Schema + codegen done. Web and iOS: `SectionErrorBoundary` + `SectionSkeleton` built. Server emits on live sections. Android wiring pending. |
| Atomic rendering layer | **Built** | `AtomicRouter` over 12 schema `AtomicElement` types on Android, iOS, and web. `AtomicComposite` bridges section and atomic layers. `AtomicCompositeBuilder` composes stateless layout surfaces (headers, rails, hero panels, promo banners, stat lines, video carousels, schedule layouts, error states, live-score cards) as server-composed atomic trees. Performance contract: depth 6, children 20, nodes 50. |

---

## 11. Non-Normative Context: Risks, Rationale, and Next Steps

The following context supports planning and prioritization. It is informative and not a normative requirement set.

1. **Schema evolution complexity** — Every change must be backward-compatible. You need version negotiation, deprecation strategy, and governance that doesn't go away.

2. **Debugging difficulty** — Bug could be in composition service, schema, codegen, state manager, action executor, renderer, or design system component. Five layers of indirection. Requires strong observability with trace IDs.

3. **Cold start performance** — SDUI screens add 50–150ms vs. hardcoded native screens (JSON parse + model decode + channel setup). Caching mitigates this on repeat visits.

4. **Testing surface area** — Every combination of section type × data shape × binding config × action config × platform needs testing. Contract tests are essential, not optional.

5. **DataBinding complexity** — Field-level binding with JSONPath, nested dot-path patching, and transforms is a custom runtime on every client. Edge cases (null values, type mismatches, missing paths) must behave identically across platforms. §3a (Binding Path Resilience) defines the normative behavior for missing paths, including the consecutive-miss counter for production observability.

6. **Organizational resistance** — Platform teams become execution engines for server-defined layouts. Some engineers embrace it (less bikeshedding), others resist it (less creative control). Requires leadership buy-in before platform teams prioritize renderer work.

---

### 11a. Why Build It Anyway

The alternative — maintaining five parallel native implementations of the game detail page that slowly drift apart — is more expensive than building and maintaining the SDUI infrastructure. During the NBA season, the ability to rearrange the game detail page in an hour instead of a sprint is a competitive advantage. The shared design system, existing devops org, and well-scoped beachhead (game detail page with clear real-time requirements) make this a strong position to build from.

---

## Revision History

| Date | Summary |
|---|---|
| 2026-05-24 | Doc consistency audit. Removed resolved Prototype Concession (variant selector — migration path completed: client-side picker removed, server now composes variant chip sections). Terminology: `fire-and-forget` → `fireAndForget` (×2). ADR Status Summary: ADR-011 and ADR-012 `Proposed (draft)` → `Proposed` (matches actual ADR file headers). | Doc consistency audit: updated URL namespace across requirements summary, README, envelope spec, client contract, and technical proposal — all screen paths now use `/v1/sdui/screen/` prefix, section paths `/v1/sdui/section/`, data paths `/v1/api/`. Fixed stale `refresh/{screenId}` → `screen/refresh/{handlerId}`. Added 3-tier URL namespace table, parameterized refresh contract, mock-data SSE policy rule, and GameDetail section refresh policies. Fixed stale color tokens in JSON fixtures (`nba.text.secondary` → `nba.label.secondary`, `nba.brand.live` → `nba.label.accent.live`). | Doc consistency audit: synced the action-system docs to canonical wire fields (`targetUri`/`webUrl`, `target`/`operation`/`value`), added the current trigger-hosting matrix, and noted the required manual appendix sweep. |
| 2026-05-05 | Doc consistency audit. Schema versioning protocol: Partial → Built (server version routing, field stripping, force-upgrade signal implemented across all platforms). |
| 2026-04-28 | Requirements audit: onTap → onActivate in all diagrams/tables. §9g theming and §9s Figma trimmed to requirement statements (implementation detail moved to sdui-design-system.md). §9i impression status corrected (visibility infra built all platforms; server analytics composition gap). §7 codegen diagram fixed (quicktype → Kotlin). §6 system count corrected (3 → 4). §9b/§9c stale "Decision required" tags resolved. Dead plan references removed (7 files). §11b next steps removed. Appendix A removed (dead reference). Footer date removed. |
| 2026-04-27 | Doc consistency audit: trigger counts (6 → 8, all in schema), onActivate + onSubmit added, "Future (TV)" distinction removed, terminology sync. |
| 2026-04-27 | Envelope-only platform (`platform[name]`, `schemaVersion`). §9o bullet and §10 atomic row aligned. Client contract: C11, §11.5, architecture diagram. |
| 2026-04-26 | Doc consistency audit. Stripped historical migration narrative — Key Schema Decisions, §9r classification, §10 renderer rows, and Atomic rendering layer row all describe current state without "former section types" or "migrated to atomic" framing. Atomic element count corrected 11 → 12 (`OverlayContainer` added). §9r "Migrated to atomic" tier renamed to "Atomic surfaces" with example list rather than enumerated former-types. Stray legacy `Row` reference removed. |
| 2026-04-25 | Doc consistency audit. Atomic rendering layer row: added LiveClock, updated migrated count 9 → 10 (GamePanel added). §9g theming: override-matrix description qualified as hardcoded in resolvers (not read from `style-tokens.json` at runtime); `variant_override_blocked` coverage qualified as Android-only verified. |
| 2026-04-24 | Doc consistency audit. Corrected the variant-selector follow-up to the request-envelope `experiments` map; updated request-envelope support to include iOS; added the AtomicBox note to the theming row; and aligned current ErrorState / BoxscoreTable / Form status text with iOS runtime parity. |
| 2026-04-21 | Doc consistency audit. Section count 9 → 10 (added `VideoPlayer` as a semantic section — platform video SDK lifecycle: AVPlayer / ExoPlayer / HLS.js, PiP, AirPlay / Chromecast, background audio). Semantic-section inventory updated. ADR Status Summary adds ADR-011 (data classification and freshness, Proposed draft), ADR-012 (client data architecture, Proposed draft), ADR-013 (style tokens for atomic primitives, Accepted). §10 renderer rows updated to describe `IconTokenResolver` + server-declared navigation shells on Android, iOS, and web. |
| 2026-04-20 | iOS runtime parity with Android landed. §10 status updates: iOS renderer (SwiftUI) Designed → Built; Error handling & fallbacks (iOS `SectionErrorBoundary` + `SectionSkeleton`); Impression deduplication (iOS `ImpressionTracker` actor); Atomic rendering layer (iOS AtomicRouter + 9 primitives). |
| 2026-04-01 | Doc consistency audit. ADR Status Summary: renamed from "ADR Approvals Pending", added ADR-001 (Proposed) and ADR-010 (Proposed). §10 status: Accessibility descriptors Gap → Built. |
| 2026-03-30 | Doc consistency audit. ADR Approvals table: ADR-006 Proposed → Accepted. §10 status updated: Internationalization Gap → Built (section-level stringTable). |
| 2026-03-24 | Doc consistency audit. `FormRenderer` → `Form` aligned with schema enum in §9r classification table. ADR Approvals table: ADR-008 Proposed → Accepted (Option C), ADR-009 Proposed → Accepted. |
| 2026-03-14 | Added §9r (Section vs. Atomic Classification — implementation-level criteria: network-driven lifecycle, platform SDK integration, client-owned interaction state. Full classification inventory with concrete examples: GamePanel Ably/poll lifecycle, SubscribeHero/SubscribeBanner billing SDK, AdSlot ad SDK, BoxscoreTable scroll/sort state). Added §9s (Figma Design Token Integration — token mapping file, client-side resolution, three-level CI validation pipeline). |
| 2026-03-13 | Atomic rendering layer. Updated Key Schema Decisions (dual-layer model: 9 section types in schema (8 permanent + AtomicComposite) + 10 atomic element types; 9 former types migrated to server-composed AtomicComposite). Added Grid vs. Section Decision Tree to §9q. Added atomic rendering layer row to requirement status matrix. |
| 2026-03-04 | Added `parentUri` to Screen contract. Updated status matrix for composition API contract (Gap → Partial). Added Prototype Concessions subsection. |
| 2026-03-04 | Added gap section 9q: Tabular Data Sections and Forms. New semantic section types (`BoxscoreTable`, `Form`), parameterized refresh on actions, sort/form state conventions. Updated status matrix. |
| 2026-02-27 | Cross-document consistency review. Replaced `entitlements` references with `device` in governance, schema decisions, and 9o to align with Technical Proposal. |
| 2026-02-25 | Established platform-aware composition as settled architectural position: shared schema, shared data pipeline, per-platform-family composition responses. Renamed `fallbackUrl` → `webUrl` in action contract. Updated platform coverage, 9b, 9m. |
| 2026-02-25 | Added `stringKeys` to binding contract JSON example in section 3. Added JSON snippet with explanation to i18n section (9p). Added revision history. |
| 2026-02-24 | Added i18n requirement (9p) with two-layer strategy (server-resolved default + `stringKeys` on data bindings). Added locale transport policy to 9o (`locale` query param on GET, body on POST; `Accept-Language` rejected). |
| 2026-02-20 | Added gap sections 9m (form-factor layout manager), 9n (ad support), 9o (composition input and request model). Aligned governance and locked decisions with ADR-driven approach. |
| 2026-02-16 | Aligned with latest platform direction. Updated schema/runtime/model touchpoints, removed outdated references. |
| 2026-02-12 | Initial version — consolidated from design sessions. Separated implementation code into reference document. Core architecture, schema, data binding, action system, screen state, codegen, platform coverage, and gap analysis (9a–9l). |

---


