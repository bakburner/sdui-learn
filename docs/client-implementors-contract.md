# SDUI Client Implementor's Contract

Platform-agnostic specification for building a conforming SDUI client in any
language or framework. This document describes **what to build** and **in what
order**. For invariant rules that every client must obey, see `AGENTS.md`.

---

## 1. Architecture Blueprint

Every conforming client implements five layers. Each layer has a single
responsibility and a well-defined interface to the layers above and below it.

```
┌─────────────────────────────────────────────┐
│                  Screen Shell                │  App lifecycle, navigation host
├─────────────────────────────────────────────┤
│            Screen ViewModel / Store          │  Fetch, state, refresh orchestration
├──────────────┬──────────────────────────────┤
│ SectionRouter│→ AtomicRouter (recursive)    │  Type dispatch
├──────────────┴──────────────────────────────┤
│   Section Renderers (8) + Atomic Renderers (12)  │  Platform-native UI
├─────────────────────────────────────────────┤
│  Runtime Services                           │  ActionDispatcher, DataBindingResolver,
│  (cross-cutting)                            │  RealTimeManager, RefreshOrchestrator,
│                                             │  ImpressionTracker, OfflineCache
└─────────────────────────────────────────────┘
```

**Data flow:**

```
Server  ─── HTTP GET ───→  ScreenViewModel
                              │
                    sections[] with refreshPolicy
                              │
            ┌─────────────────┼──────────────────┐
            ▼                 ▼                  ▼
      SectionRouter     RefreshOrchestrator  RealTimeManager
            │                 │                  │
            ▼                 ▼                  ▼
     Renderer(section)   poll / fetch      Ably channel
            │                 │                  │
            └─────────────────┼──────────────────┘
                              ▼
                    DataBindingResolver
                    (merge into section data)
                              │
                              ▼
                       Re-render section
```

---

## 2. Build Checklist

Implement in this order. Each phase produces a testable artifact. The server
at `localhost:8080` is the reference implementation; hit it with
`curl -H "X-Schema-Version: 1.0" http://localhost:8080/sdui/demos` to get a
42-section kitchen-sink response.

### Phase 1 — Static Rendering (render a screen from JSON)

| # | Component | What it does |
|---|-----------|--------------|
| 1 | **Models** | Use the generated models from the platform's authoritative output location (see the table in "Shared Infrastructure" above), or regenerate from `schema/sdui-schema.json` for a new language. Deserialize `SduiScreen`, `Section`, `AtomicElement`, `Action`, `RefreshPolicy`, `DataBinding`. |
| 2 | **SduiRepository.fetchScreen** | Single fetch primitive every composition request routes through. Builds the request envelope as bracket-notation query params; falls back to POST with the same shape in the JSON body when the query exceeds 8192 chars. Sends `X-Trace-Id` on every request. See §11 for the full transport contract. Returns `SduiScreen`. |
| 3 | **UriResolver.resolveEndpoint** | Convert `nba://{path}` → `/sdui/{path}`. Pure string prefix swap, no branching. |
| 4 | **SectionRouter** | Switch on `section.type` → dispatch to renderer. Unknown types → log + skip. |
| 5 | **AtomicRouter** | Switch on `element.type` → dispatch to atomic renderer. Depth guard at 6. |
| 6 | **AtomicComposite bridge** | When SectionRouter sees `type: "AtomicComposite"`, parse `section.data.ui` and hand to AtomicRouter. Renderers read live fields via `bindRef` (see §4b) against `section.data.content`. |
| 7 | **12 atomic renderers** | Container, Text, Image, Button, Spacer, Divider, ScrollContainer, Conditional, DisplayGrid, SectionSlot, LiveClock, OverlayContainer. See §4c for LiveClock tick-loop contract. |
| 8 | **ScreenShell** | Fetch screen via repository, iterate `sections[]`, pass each to SectionRouter. |

**Milestone:** You can render the kitchen-sink demo screen as a scrollable page
of styled content. No interactivity yet.

### Phase 2 — Actions & State (make things tappable)

| # | Component | What it does |
|---|-----------|--------------|
| 9 | **StateManager** | Key-value store for screen state. `setState(key, value)`, `getState(key)`, `removeState(key)`. Observable/reactive. |
| 10 | **ActionDispatcher** | Execute a list of actions in sequence. Six types: `navigate`, `fireAndForget`, `mutate`, `refresh`, `dismiss`, `toast`. |
| 11 | **Failure policies** | Per-action `onFailure`: `halt` (stop sequence), `continue` (log + proceed), `silent` (swallow). Navigate success always halts. |
| 12 | **Conditional element** | Evaluate `condition` string against screen state to select `thenElement` or `elseElement`. |

**Milestone:** Tapping buttons triggers navigation or state changes.
Conditional elements show/hide based on state.

### Phase 3 — Live Data (polling & real-time)

| # | Component | What it does |
|---|-----------|--------------|
| 13 | **RefreshOrchestrator** | For each section with `refreshPolicy.type == "poll"`: schedule interval, fetch from `refreshPolicy.url` (or re-fetch screen), merge result. |
| 14 | **fetchRawJson** | Fetch arbitrary JSON URL, optionally extract nested data via `refreshPolicy.dataPath` dot-notation. |
| 15 | **RealTimeManager** | Connect to Ably. Token-based auth via `authUrl`. Subscribe to channels from `refreshPolicy.channel`. Parse messages as opaque `Map<String, Any>`. |
| 16 | **DataBindingResolver** | Apply bindings from real-time or poll messages to section data. Algorithm in §4. |
| 17 | **Surgical section merge** | When refreshing, replace only the affected section's data — don't re-render the entire screen. |
| 17a | **Visibility-gated refresh** | Pause poll/SSE for off-screen sections; resume on scroll-back. Respect `pauseWhenOffScreen` field. Algorithm in §8a. |
| 17b | **App lifecycle pause** | Pause all refresh when app is backgrounded; resume on foreground. Algorithm in §8a. |

**Milestone:** Live scores update in `AtomicComposite` game-card sections
via `bindRef` resolution against `data.content` (see §4b). Boxscore stats
poll and refresh. Sections with `refreshPolicy.type == "static"` never
refresh.

### Phase 4 — Section Renderers (domain-specific)

| # | Component | Why it needs client code |
|---|-----------|------------------------|
| 18 | **BoxscoreTable** | Real-time data binding, expandable rows |
| 19 | **SeasonLeadersTable** | Sort/filter interaction state |
| 20 | **TabGroup** | Tab selection state, nested section hosting |
| 21 | **Form** | Validation state, platform keyboard integration |
| 22 | **SubscribeHero** | Platform IAP SDK integration |
| 23 | **SubscribeBanner** | Platform IAP SDK integration |
| 24 | **AdSlot** | Platform ad SDK lifecycle |
| 24a | **VideoPlayer** | Platform video SDK (HLS/DASH playback, PiP, AirPlay/Chromecast, background audio, fullscreen rotation). `playerType` discriminator maps to the right SDK entry point. |

**Milestone:** All 8 semantic sections render with full interactivity.
Live-score surfaces render as server-composed `AtomicComposite` trees
driven by `bindRef` + SSE data bindings.

### Phase 5 — Production Hardening

| # | Component | What it does |
|---|-----------|--------------|
| 26 | **OfflineCache** | Cache screen responses keyed by endpoint. Serve stale on network failure. |
| 27 | **ImpressionTracker** | Fire `onVisible` + `fireAndForget` actions with dedup. Algorithm in §7. |
| 28 | **ErrorState handling** | Server composes `ErrorState` as an `AtomicComposite`. Client renders it like any section. On network failure, client may compose its own. |
| 29 | **Accessibility** | Map `accessibilityLabel`, `accessibilityRole`, `accessibilityHint` from schema to platform APIs. |
| 30 | **SectionSlot bridge** | AtomicRouter encounters `SectionSlot` → delegates back to SectionRouter. Recursion guard at depth 2. |

### Contract Drift Test (required for every client)

Every client must ship a round-trip test against `schema/examples/`.
Failure means wire-contract drift.

- Web: `web/src/__tests__/schemaRoundTrip.test.ts`
- Android: `android/sdui-core/src/test/java/com/nba/sdui/core/SchemaRoundTripTest.kt`
- iOS: `ios/Tests/SduiCoreTests/SduiModelsRoundTripTests.swift`

---

## 3. Section Router Algorithm

```
FUNCTION SectionRouter(section, screenState, onAction, onStateChange):
    SWITCH section.type:
        "TabGroup"           → TabGroupRenderer(section, screenState, onAction, onStateChange)
        "BoxscoreTable"      → BoxscoreTableRenderer(section, onAction)
        "Form"               → FormRenderer(section, screenState, onAction, onStateChange)
        "SubscribeBanner"    → SubscribeBannerRenderer(section, onAction)
        "SubscribeHero"      → SubscribeHeroRenderer(section, onAction)
        "AdSlot"             → AdSlotRenderer(section, onAction)
        "SeasonLeadersTable" → SeasonLeadersTableRenderer(section, onAction)
        "VideoPlayer"        → VideoPlayerRenderer(section, onAction)
        "AtomicComposite"    →
            compositeData = section.data
            IF compositeData.ui IS NULL:
                RETURN null
            // Provide compositeData.content to descendants so leaf primitives
            // can resolve bindRef paths (see §4b).
            AtomicRouter(compositeData.ui, screenState, onAction, onStateChange,
                         depth=0, compositeContent=compositeData.content)
        DEFAULT →
            LOG_WARNING("Unknown section type: " + section.type)
            RETURN null   // Skip gracefully — never crash
```

**Supported section types (9):**
`TabGroup`, `BoxscoreTable`, `Form`, `SubscribeBanner`, `SubscribeHero`,
`AdSlot`, `SeasonLeadersTable`, `VideoPlayer`, `AtomicComposite`.
Live-score / game-card surfaces are composed as `AtomicComposite`
trees (see §4b for the `bindRef` → `content` resolution that drives
live updates).

---

## 4. Atomic Router Algorithm

The router is a **pure dispatcher**. It does not apply any styling of its own.
Every box-model concern — `margin`, `padding`, `background`, `cornerRadius`,
`cornerRadii`, `shadow`, `border`, `opacity`, `width`, `height`, `fillWidth`,
variant chrome, and `badge` overlay — is applied by a single helper called
`AtomicBox` (see §4a) that every primitive routes its output through.
Primitives own only the content they render (typography, scroll layout,
image scaling, flex arrangement); they never re-implement box-model logic.

```
CONSTANT MAX_DEPTH = 6

FUNCTION AtomicRouter(element, screenState, onAction, onStateChange, depth):
    IF depth > MAX_DEPTH:
        LOG_WARNING("Max tree depth exceeded — skipping element")
        RETURN null

    childDepth = depth + 1

    SWITCH element.type:
        "Container"        → AtomicContainer(element, screenState, onAction, onStateChange, childDepth)
        "Text"             → AtomicText(element)
        "Image"            → AtomicImage(element)
        "Button"           → AtomicButton(element, screenState, onAction)
        "Spacer"           → AtomicSpacer(element)
        "Divider"          → AtomicDivider(element)
        "ScrollContainer"  → AtomicScrollContainer(element, screenState, onAction, onStateChange, childDepth)
        "Conditional"      → AtomicConditional(element, screenState, onAction, onStateChange, childDepth)
        "DisplayGrid"      → AtomicDisplayGrid(element)
        "OverlayContainer" → AtomicOverlayContainer(element, screenState, onAction, onStateChange, childDepth)
        "SectionSlot"      → AtomicSectionSlot(element, screenState, onAction, onStateChange)
        "LiveClock"        → AtomicLiveClock(element)   // see §4c
        DEFAULT →
            LOG_WARNING("Unknown atomic type: " + element.type)
            RETURN null
```

**Container children rendering:**
```
FUNCTION AtomicContainer(element, state, onAction, onStateChange, depth):
    // element.direction = "row" | "column"
    // element.children  = list of AtomicElement
    // element.gap, element.alignment, element.crossAlignment, element.breakpoint
    // element.flex (layout weight on children, main-axis only)
    //
    // Box-model concerns (margin/padding/bg/cornerRadius/shadow/border/badge)
    // are handled by AtomicBox — this function only arranges children.

    WRAP IN AtomicBox(element):
        RowOrColumn(direction=element.direction, gap=element.gap, ...):
            FOR child IN element.children:
                AtomicRouter(child, state, onAction, onStateChange, depth)
```

### 4a. AtomicBox — the unified box-model helper

`AtomicBox` is the **single site** on every client where an `AtomicElement`'s
box model is realized. Every primitive — `Container`, `ScrollContainer`,
`Text`, `Image`, `Button`, `Divider`, `DisplayGrid`, `OverlayContainer` —
wraps its rendered content through `AtomicBox`. Primitives that are pure
layout devices — `Spacer`, `Conditional`, `SectionSlot` — bypass it because
they render no chrome of their own (the chosen child / hosted section
carries the box model).

**The canonical modifier order (outer → inner):**

```
margin                ← sibling-to-sibling spacing
  └─ opacity          ← applied once, affects everything below
       └─ shadow      ← casts from the bg shape
            └─ corner clip
                 └─ background (variant or inline) + optional gradient overlay
                      └─ border
                           └─ padding   ← interior padding; bg extends to its edge
                                └─ sizing (width / height / fillWidth)
                                     └─ content (what the primitive actually renders)
```

Invariants this order guarantees:

- `margin` is outside everything — never clipped by the element's own
  corner clip or tinted by its bg.
- `padding` lives **inside** the bg + corner clip, so variants like `hero`
  or `grouped` paint to the padded frame (matches CSS box-model intuition
  and the historical `Container` semantic).
- `shadow` renders on the bg shape, not on the padded frame.
- `fillWidth` is applied to the sized frame (border-box semantics); when
  `width` is also set, `width` wins.

**Variant integration.** Before applying the stack, `AtomicBox` resolves
`element.variant` against the platform's `ContainerVariantResolver` / 
`ImageVariantResolver`. The resolver returns **data** (a spec with
background role, cornerRadius, shadow, border, fillWidth, gradient overlay,
and an `overrideMatrix`), never a platform-specific modifier. `AtomicBox`
then merges the spec with inline `element.*` props per each axis's
override policy (`allow` → inline wins; `lock` → variant wins, inline
attempt is logged). This keeps variant realization client-native while
keeping box-model application uniform.

**Badge overlay.** When `element.badge` is present, `AtomicBox` renders the
badge element through `AtomicRouter` and positions it according to
`element.badge.alignment`. The badge uses the same box model as any other
atomic element.

**What `AtomicBox` deliberately does not own:**

- **Accessibility labels.** Each primitive provides its own semantic
  fallback (image `alt`, button `label`, text `content`) so the router
  cannot make a generic choice.
- **Action triggers (`actions[]`, `onActivate`, `onTap`, `onLongPress`, `onVisible`, `onSwipe`, `onFocus`, `onBlur`, `onSubmit`).**
  Primitives integrate actions into their native control (SwiftUI
  `Button`, Compose `Modifier.clickable`, `<button>` element) because the
  gesture surface is primitive-specific.
- **Layout direction / flex / gap / alignment on `Container`.** These are
  flex-layout concerns owned by `AtomicContainer`.

**Why this matters for client release cadence.** New box-model schema
fields (a future `outline`, `elevation`, or `backdrop` property) are
implemented in `AtomicBox` once per platform and every primitive picks
them up for free. Variant values added to the catalog (e.g. extending
`ContainerVariant` or `ImageVariant`) reach every primitive the same
way. A new primitive type ships with a complete box model without
re-implementing the stack.

**Pseudocode:**

```
FUNCTION AtomicBox(element, screenState, onAction, content):
    variantSpec = ContainerVariantResolver.resolve(element.variant)

    // Merge inline + variant per override policy
    effectiveCornerRadius = resolveAxis("cornerRadius", element.cornerRadius, variantSpec?.cornerRadius, variantSpec?.overrideMatrix)
    effectiveShadow       = resolveAxis("shadow",       element.shadow,       variantSpec?.shadow,       variantSpec?.overrideMatrix)
    useVariantBackground  = (element.background == null) OR overrideMatrix["background"] == "lock"

    shape = buildShape(element.cornerRadii, effectiveCornerRadius)
    fillW = element.fillWidth ?? variantSpec?.fillWidth

    RETURN
      APPLY margin(element.margin)
      APPLY opacity(element.opacity)
      APPLY shadow(effectiveShadow, shape)
      APPLY clipToShape(shape)
      APPLY background(useVariantBackground ? variantSpec.background : element.background)
      APPLY gradientOverlay(variantSpec?.gradientOverlay)   // hero variant only
      APPLY border(variantSpec?.border, shape)
      APPLY padding(element.padding)
      APPLY sizing(element.width, element.height, fillW)
      WRAP WITH badge(element.badge) IF present
      CONTAINS content
```

**SectionSlot bridge (atomic → section):**
```
CONSTANT MAX_SLOT_DEPTH = 2

FUNCTION AtomicSectionSlot(element, state, onAction, onStateChange, currentSlotDepth):
    // slotDepth is a runtime counter tracked by the client, not a schema field.
    // Increment on each SectionSlot → Section → AtomicComposite → SectionSlot cycle.
    IF currentSlotDepth >= MAX_SLOT_DEPTH:
        LOG_WARNING("SectionSlot recursion limit — skipping")
        RETURN null

    embeddedSection = element.section
    SectionRouter(embeddedSection, state, onAction, onStateChange, currentSlotDepth + 1)
```

**Performance limits (server-enforced, client-guarded):**
- Max depth: 6
- Max children per container: 20
- Max total nodes per AtomicComposite: 50

### 4b. `bindRef` — leaf-level data binding inside AtomicComposite

Inside an `AtomicComposite`, leaf primitives may carry an optional
`bindRef: string` dot-path that resolves against the enclosing
`AtomicCompositeData.content` object at render time. Placing the
binding identifier on the consuming leaf (rather than in a centrally-
declared path-into-tree on the section envelope) lets composers
reshape the `ui` tree without breaking real-time updates. Data
bindings on the section envelope (SSE / poll) continue to write into
`content.*`; leaf `bindRef` reads back out.

**Canonical field per leaf type:**

| Leaf type | Field populated from `bindRef` | Fallback when path missing |
|---|---|---|
| `Text` | `content` (string) | inline `content` |
| `Button` | `label` (string) | inline `label` |
| `Image` | `src` (string) | inline `src` |
| `LiveClock` | an object `{ snapshotSeconds, snapshotAt, isRunning }` | inline `snapshotSeconds` / `snapshotAt` / `isRunning` |

Clients implement `bindRef` resolution once (typically as
`BindRefResolver.resolve(element, compositeContent)`) and each leaf
renderer calls it before picking inline fallbacks.

**Propagation.** `compositeContent` is threaded from
`SectionRouter(AtomicComposite)` down to every descendant via the
platform's implicit-context primitive: SwiftUI `@Environment`, Compose
`CompositionLocal`, React context. No leaf ever takes
`compositeContent` as an explicit parameter.

**Resolution algorithm:**

```
FUNCTION resolveBindRef(element, compositeContent):
    IF element.bindRef IS NULL OR element.bindRef IS EMPTY:
        RETURN null
    IF compositeContent IS NULL:
        RETURN null      // leaf is outside an AtomicComposite

    current = compositeContent
    FOR segment IN SPLIT(element.bindRef, "."):
        IF current IS NOT OBJECT OR segment NOT IN current:
            RETURN null   // missing path → fall back to inline
        current = current[segment]

    RETURN current
```

**Missing-path semantics.** When `bindRef` does not resolve, the leaf
falls back to its inline value. A resolved-to-null `bindRef` is a
live-data hole and **does not** overwrite the inline fallback — this
mirrors the keep-previous-value rule in §5 `applyBindings`.

**What `bindRef` does *not* do.** It is a read-only lookup at render
time. It does not fire analytics, it does not register bindings
centrally, and it is not a substitute for `section.dataBindings`
(which writes SSE / poll payloads into `content.*`). Think of it as
the read side of a read/write pair where `dataBindings` is the write
side.

### 4c. `LiveClock` — client-owned tick animation

`LiveClock` is an atomic primitive that renders a ticking clock driven
entirely from server-provided snapshot fields. It exists because the
tick animation is client state (a local timer advances the displayed
value between SSE updates); the snapshot that anchors the clock is
server state.

**Schema fields (see `schema/sdui-schema.json` → `AtomicElement`):**

| Field | Type | Purpose |
|---|---|---|
| `snapshotSeconds` | integer | Authoritative clock value, in seconds, at `snapshotAt`. |
| `snapshotAt` | string (ISO 8601) | Server wall-clock instant the snapshot was valid (second precision). |
| `isRunning` | boolean | Whether the clock is ticking (drives local animation). |
| `tickDirection` | enum `"down" \| "up"` | `down` decrements from `snapshotSeconds` toward `stopAtSeconds` (default 0); `up` increments with no upper bound unless `stopAtSeconds` is set. Named `tickDirection` to avoid collision with `AtomicElement.direction` (flex axis). |
| `stopAtSeconds` | integer? | Optional clamp. Clock holds at this value once reached. |
| `format` | enum `"m:ss" \| "mm:ss" \| "h:mm:ss"` | Display format, default `"m:ss"`. |
| `bindRef` | string? | See §4b. Resolves to an object `{ snapshotSeconds, snapshotAt, isRunning }`. |

**Display contract.** Render with the platform's tabular-numeral
typography (same treatment as `TextVariant.score`) so digits do not
jitter when values change. The clock is a leaf; it obeys the
`AtomicBox` stack only insofar as any text leaf does.

**Tick-loop algorithm:**

```
FUNCTION AtomicLiveClock(element, compositeContent):
    resolved = resolveBindRef(element, compositeContent) OR {
        snapshotSeconds: element.snapshotSeconds,
        snapshotAt:      element.snapshotAt,
        isRunning:       element.isRunning
    }

    format       = element.format        OR "m:ss"
    tickDir      = element.tickDirection OR "down"
    stopAt       = element.stopAtSeconds                 // may be null
    snapshot     = resolved.snapshotSeconds
    snapshotAt   = PARSE_ISO8601(resolved.snapshotAt)
    isRunning    = resolved.isRunning

    ON_RENDER and every 100ms WHILE isRunning:
        elapsedSec = (NOW() - snapshotAt) / 1000
        raw = (tickDir == "down")
                ? snapshot - elapsedSec
                : snapshot + elapsedSec

        IF stopAt IS NOT NULL:
            IF tickDir == "down" AND raw < stopAt: raw = stopAt
            IF tickDir == "up"   AND raw > stopAt: raw = stopAt

        IF tickDir == "down" AND raw < 0: raw = 0

        DISPLAY formatSeconds(raw, format)

    WHEN NOT isRunning:
        DISPLAY formatSeconds(snapshot, format)
```

**Tick cadence.** 10Hz (100ms) is the baseline; clients may coalesce
to the platform's display refresh (e.g. iOS `TimelineView` at
`.animation(.none)`, Compose `withFrameMillis`, web
`requestAnimationFrame`) so long as the displayed value advances
smoothly.

**Authoritative snapshots.** Every SSE / poll update that reaches the
enclosing `AtomicComposite` and rewrites `content.*` fields causes
`bindRef` to re-resolve on next render, which resets the tick loop's
anchor. Clients should therefore not accumulate drift across ticks;
each frame's displayed value is `snapshotSeconds ± (now − snapshotAt)`.

---

## 5. Data Binding Algorithm

Data bindings apply real-time or polled data into section state. This is the
**only** code path for merging live updates — no duplicate logic elsewhere.

### Binding Model

```
DataBinding:
    bindings: list of BindingPath
    stringKeys: map<targetPath, i18nKey>  (optional)

BindingPath:
    sourcePath: string   // JSONPath-like: "$.homeTeam.score" or "homeTeam.score"
    targetPath: string   // Dot-notation: "homeTeam.score"
```

### Apply Bindings

```
FUNCTION applyBindings(currentData, incomingMessage, dataBinding):
    result = DEEP_CLONE(currentData)

    FOR binding IN dataBinding.bindings:
        TRY:
            sourceValue = resolveSourcePath(incomingMessage, binding.sourcePath)

            IF sourceValue IS NULL:
                CONTINUE   // Keep previous value — do not overwrite with null

            setTargetPath(result, binding.targetPath, sourceValue)
            LOG_DEBUG("Applied: " + binding.sourcePath + " → " + binding.targetPath)
        CATCH error:
            LOG_WARNING("Failed to apply binding: " + binding.sourcePath +
                        " → " + binding.targetPath + ": " + error)
            CONTINUE   // Never halt on a single binding failure

    RETURN result
```

### Source Path Resolution

Source paths use JSONPath-like dot-notation with optional `$.` prefix and
array index syntax.

```
FUNCTION resolveSourcePath(data, path):
    cleanPath = REMOVE_PREFIX(path, "$.")
    RETURN navigatePath(data, cleanPath)

FUNCTION navigatePath(node, path):
    IF path IS EMPTY:
        RETURN node

    segments = SPLIT(path, ".")
    current = node

    FOR segment IN segments:
        IF current IS NULL:
            RETURN null

        IF segment MATCHES pattern "(\w+)\[(\d+)\]":
            // Array index: "items[0]"
            fieldName = match.group(1)
            index = match.group(2) AS INTEGER
            current = current[fieldName]
            IF current IS NOT ARRAY OR index >= LENGTH(current):
                RETURN null
            current = current[index]
        ELSE:
            current = current[segment]
            IF current IS MISSING:
                RETURN null

    RETURN current
```

### Target Path Setting

Target paths use dot-notation. Intermediate objects are auto-created when
missing.

```
FUNCTION setTargetPath(root, path, value):
    segments = SPLIT(path, ".")

    IF LENGTH(segments) == 1:
        root[segments[0]] = value
        RETURN

    current = root
    FOR i = 0 TO LENGTH(segments) - 2:
        child = current[segments[i]]
        IF child IS MISSING OR child IS NOT OBJECT:
            child = NEW_EMPTY_OBJECT()
            current[segments[i]] = child
        current = child

    current[LAST(segments)] = value
```

### Key Decisions

| Decision | Behavior |
|----------|----------|
| Null source value | Keep previous — do not overwrite |
| Missing intermediate target | Auto-create empty object |
| Array indexing | `"items[0].name"` supported in source paths |
| Single binding failure | Log warning, continue to next binding |
| i18n stringKeys | Optional map per binding; resolution deferred to renderer |

### Binding Path Resilience

Bindings may reference paths that no longer exist in the data shape due to
schema evolution, message format changes, or cached-layout drift. See §3a
of `sdui-requirements-summary.md` for the full requirement. The runtime
behavior is already implemented by the null-guard in `applyBindings()` above
(`IF sourceValue IS NULL: CONTINUE`). Clients additionally MUST track
consecutive misses for observability:

```
// Module-level state — persists across calls, cleared when section is removed
consecutiveMissCounts: map<string, int>   // key = "{sectionId}:{sourcePath}"
MISS_THRESHOLD: int = 3

FUNCTION applyBindings(sectionId, currentData, incomingMessage, dataBinding):
    result = DEEP_CLONE(currentData)

    FOR binding IN dataBinding.bindings:
        key = sectionId + ":" + binding.sourcePath
        TRY:
            sourceValue = resolveSourcePath(incomingMessage, binding.sourcePath)

            IF sourceValue IS NULL:
                consecutiveMissCounts[key] = (consecutiveMissCounts[key] ?? 0) + 1
                IF consecutiveMissCounts[key] >= MISS_THRESHOLD:
                    LOG_WARNING("Binding path missing for " + MISS_THRESHOLD +
                                " consecutive cycles: sectionId=" + sectionId +
                                ", sourcePath=" + binding.sourcePath +
                                ", misses=" + consecutiveMissCounts[key])
                    // TODO: emit binding_path_missing analytics event
                CONTINUE   // Keep previous value — do not overwrite with null

            // Source resolved successfully — reset miss counter
            consecutiveMissCounts[key] = 0
            setTargetPath(result, binding.targetPath, sourceValue)

        CATCH error:
            consecutiveMissCounts[key] = (consecutiveMissCounts[key] ?? 0) + 1
            LOG_WARNING("Failed to apply binding: " + binding.sourcePath +
                        " → " + binding.targetPath + ": " + error)
            CONTINUE

    RETURN result

FUNCTION resetBindingCounters(sectionId):
    REMOVE all keys from consecutiveMissCounts where key starts with sectionId + ":"
```

**Mandatory rules:**
- Clients MUST NOT crash, show error UI, or clear a field on a missing
  binding path. Keep-previous-value is the only acceptable behavior.
- Clients MUST call `resetBindingCounters(sectionId)` when a section is
  removed from the screen to prevent counter map memory leaks.

---

## 6. Action Dispatch Algorithm

Actions are attached to sections and elements. They execute in declared order
with failure-policy semantics.

### Action Model

```
Action:
    type: "navigate" | "fireAndForget" | "mutate" | "refresh" | "dismiss" | "toast"
    trigger: "onActivate" | "onTap" | "onLongPress" | "onVisible" | "onSwipe" | "onFocus" | "onBlur" | "onSubmit"
    targetUri: string?          // For navigate
    webUrl: string?             // Fallback URL for navigate
    presentation: "push" | "modal" | "fullscreen" | "replace" | "external"?
    modalHeight: "compact" | "half" | "full"?     // For modal presentation
    event: string?              // For fireAndForget (analytics event name)
    params: map<string, any>?   // For fireAndForget (analytics params)
    stateKey: string?           // For mutate
    stateValue: any?            // For mutate (null = remove key)
    mutateOperation: "set" | "toggle" | "increment" | "append"?  // Default: "set"
    sectionId: string?          // For refresh (which section to refresh)
    endpoint: string?           // For parameterized refresh
    paramBindings: map?         // For refresh: key → "{{stateKey}}" template
    onFailure: "halt" | "continue" | "silent"?
    failureFeedback: FailureFeedback?  // UI feedback on failure
    impression: ImpressionPolicy?

FailureFeedback:
    message: string             // User-facing error message
    style: "snackbar" | "toast" | "inline"
```

### Sequence Execution

```
FUNCTION executeActionSequence(actions, stateManager):
    results = []

    FOR action IN actions:
        result = dispatchSingleAction(action, stateManager)
        APPEND result TO results

        // Navigate success always halts the sequence
        IF result.type == "navigate" AND result.success:
            RETURN SequenceResult(results, halted=true)

        IF result.isFailure:
            policy = action.onFailure
                     OR DEFAULT_FAILURE_POLICY[action.type]
                     OR "continue"

            SWITCH policy:
                "halt"     → RETURN SequenceResult(results, halted=true)
                "continue" → LOG_WARNING(action.type + " failed, continuing")
                "silent"   → // No log
```

### Default Failure Policies

| Action Type | Default Policy |
|-------------|---------------|
| navigate | halt |
| fireAndForget | silent |
| mutate | continue |
| refresh | continue |
| dismiss | silent |
| toast | silent |

### Single Action Dispatch

```
FUNCTION dispatchSingleAction(action, stateManager):
    SWITCH action.type:
        "navigate":
            uri = action.targetUri OR action.webUrl
            IF uri IS NULL:
                RETURN failure("No target URI")
            presentation = action.presentation OR "push"
            // Hand to platform navigation system
            RETURN NavigateResult(uri, presentation, action.modalHeight)

        "fireAndForget":
            event = action.event OR "unnamed_event"
            params = action.params OR {}
            // Hand to analytics dispatcher
            RETURN FireAndForgetResult(event, params)

        "mutate":
            IF action.stateKey IS NULL:
                RETURN failure("No state key")
            operation = action.mutateOperation OR "set"
            SWITCH operation:
                "set":
                    IF action.stateValue IS NOT NULL:
                        stateManager.setState(action.stateKey, action.stateValue)
                    ELSE:
                        stateManager.removeState(action.stateKey)
                "toggle":
                    current = stateManager.getState(action.stateKey)
                    stateManager.setState(action.stateKey, NOT current)
                "increment":
                    current = stateManager.getState(action.stateKey) AS NUMBER OR 0
                    delta = action.stateValue AS NUMBER OR 1
                    stateManager.setState(action.stateKey, current + delta)
                "append":
                    current = stateManager.getState(action.stateKey) AS LIST OR []
                    stateManager.setState(action.stateKey, APPEND(current, action.stateValue))
            RETURN MutateResult(action.stateKey, stateManager.getState(action.stateKey))

        "refresh":
            IF action.paramBindings IS NOT EMPTY AND action.endpoint IS NOT NULL:
                // Resolve param templates from screen state. The action handler
                // does NOT build a URL — that's the transport's job. Hand off
                // (endpoint, sectionId, resolvedParams) and route through the
                // shared fetch primitive (§11) so the request inherits the
                // canonical envelope contract: bracket-notation envelope
                // params, GET/POST length fallback, RFC-3986 percent-encoding,
                // and X-Trace-Id propagation from the parent screen.
                resolvedParams = {}
                FOR key, template IN action.paramBindings:
                    stateKey = STRIP_DELIMITERS(template, "{{", "}}")
                    value = stateManager.getState(stateKey)
                    IF value IS NOT NULL AND value != "":
                        resolvedParams[key] = value
                RETURN ParameterizedRefreshResult(action.endpoint, action.sectionId, resolvedParams)
            RETURN RefreshResult(action.sectionId)

        "dismiss":
            RETURN DismissResult()

        "toast":
            RETURN ToastResult(action.message)

        DEFAULT:
            LOG_WARNING("Unknown action type: " + action.type)
            RETURN UnknownResult(action.type)
```

---

## 7. Refresh & Polling Algorithm

Each section carries a `refreshPolicy` that tells the client how and when
to update its data. The client must not hardcode intervals or decide which
sections to poll.

### RefreshPolicy Model

```
RefreshPolicy:
    type: "static" | "poll" | "sse"
    intervalMs: integer?       // Poll interval in milliseconds
    url: string?               // Direct poll URL (bypass SDUI endpoint)
    dataPath: string?          // Dot-path to extract nested data from poll response
    channel: string?           // Ably channel name for SSE
```

### Polling Orchestration

```
FUNCTION setupPolling(screen):
    stopAllActivePolls()

    FOR section IN screen.sections:
        policy = section.refreshPolicy
        IF policy IS NULL OR policy.type != "poll" OR policy.intervalMs IS NULL:
            CONTINUE

        // Schedule repeating poll
        SCHEDULE_REPEATING(intervalMs = policy.intervalMs):
            TRY:
                IF policy.url IS NOT NULL:
                    // Direct URL poll (e.g., CDN feed)
                    data = fetchRawJson(policy.url, policy.dataPath)
                    updateSectionData(section.id, data)
                ELSE:
                    // Re-fetch entire screen, merge surgically
                    updatedScreen = fetchScreen(currentEndpoint)
                    mergeSections(updatedScreen)
            CATCH error:
                LOG_ERROR("Poll failed for section " + section.id + ": " + error)
                // Do NOT remove the section or stop polling on transient failure
```

### fetchRawJson (Direct Data Poll)

```
FUNCTION fetchRawJson(url, dataPath):
    response = HTTP_GET(url)

    IF NOT response.ok:
        THROW "HTTP " + response.status

    json = PARSE_JSON(response.body)

    IF dataPath IS NULL OR dataPath IS EMPTY:
        RETURN json

    // Navigate dot-path to extract nested data
    current = json
    FOR segment IN SPLIT(dataPath, "."):
        IF current IS MAP:
            current = current[segment]
            IF current IS NULL:
                THROW "Path segment '" + segment + "' not found"
        ELSE:
            THROW "Cannot navigate '" + segment + "' — not an object"

    RETURN current
```

### Surgical Section Merge

When a poll response arrives (either from `fetchRawJson` or a full screen
re-fetch), update **only** the affected section's data — do not re-render
the entire screen.

```
FUNCTION updateSectionData(sectionId, newData):
    existingSections = currentScreen.sections
    FOR i IN 0..LENGTH(existingSections) - 1:
        IF existingSections[i].id == sectionId:
            existingSections[i].data = MERGE(existingSections[i].data, newData)
            TRIGGER_RE_RENDER(existingSections[i])
            RETURN
    LOG_WARNING("Section not found for update: " + sectionId)
```

---

## 8. Real-Time (Ably SSE) Algorithm

Sections with `refreshPolicy.type == "sse"` receive live updates via Ably
channels.

### Connection Lifecycle

```
FUNCTION initializeRealTime(tokenUrl):
    IF client IS ALREADY_INITIALIZED:
        RETURN

    client = NEW AblyClient(
        authCallback = () → fetchToken(tokenUrl),
        autoConnect = true
    )

    client.onConnectionStateChange(state):
        SWITCH state:
            connected    → LOG_INFO("Ably connected")
            disconnected → LOG_WARNING("Ably disconnected — SDK will auto-reconnect")
            failed       → LOG_ERROR("Ably connection failed: " + state.reason)

FUNCTION fetchToken(tokenUrl):
    response = HTTP_GET(tokenUrl, timeout=10s)
    IF NOT response.ok:
        THROW "Token fetch failed: HTTP " + response.status

    json = PARSE_JSON(response.body)

    // Token may be wrapped: {"data": {"accessToken": "..."}}
    jwt = json["data"]["accessToken"]
    IF jwt IS NOT NULL AND jwt IS NOT EMPTY:
        RETURN jwt

    // Or raw token string
    RETURN response.body
```

### Channel Subscription

```
FUNCTION subscribeToChannel(channelName, onMessage):
    channel = client.channels.get(channelName)
    activeChannels[channelName] = channel

    channel.subscribe(message):
        data = message.data
        parsed = NULL

        IF data IS STRING:
            parsed = PARSE_JSON(data) AS Map<String, Any>
        ELSE IF data IS MAP:
            parsed = data
        ELSE:
            LOG_WARNING("Unexpected message format — skipping")
            RETURN

        IF parsed IS NOT NULL:
            onMessage(parsed)

    ON_CLEANUP:
        channel.unsubscribe()
        REMOVE activeChannels[channelName]
```

### Wiring SSE to Data Bindings

```
FUNCTION setupRealTimeForScreen(screen):
    FOR section IN screen.sections:
        policy = section.refreshPolicy
        IF policy IS NULL OR policy.type != "sse" OR policy.channel IS NULL:
            CONTINUE

        subscribeToChannel(policy.channel, message):
            IF section.dataBindings IS NOT NULL:
                updatedData = applyBindings(section.data, message, section.dataBindings)
                updateSectionData(section.id, updatedData)
            ELSE:
                LOG_WARNING("SSE message received but no dataBindings for section " + section.id)
```

### Key Decisions

| Decision | Behavior |
|----------|----------|
| Auth mechanism | Token callback via app-level `tokenUrl` config — SDK handles refresh |
| Message format | Always parse as opaque `Map<String, Any>` — never as typed models |
| Reconnection | Ably SDK handles automatic reconnection; client logs state changes |
| Channel cleanup | Unsubscribe on screen exit or section removal |
| Missing bindings | Log warning; do not crash or discard message |

---

## 8a. Visibility-Gated Refresh

Pause polling and SSE processing for sections scrolled out of the viewport.
Resume immediately when the section re-enters. This prevents unnecessary
network traffic, battery drain, and wasted CPU on data-binding updates the
user cannot see.

### RefreshPolicy Extension

`RefreshPolicy` includes a `pauseWhenOffScreen` boolean (default `true`).
When `true`, the client pauses this section's refresh when it leaves the
viewport. When `false`, the client keeps refreshing regardless. The server
sets `false` on critical live sections (e.g. live-score composites feeding
a `LiveClock`) that should never go stale.

### App Background / Foreground (Phase 0)

Before scroll-based visibility, gate **all** refresh on app lifecycle:

```
FUNCTION onAppBackgrounded():
    isAppForeground = false
    // All poll timers effectively pause (gated at loop top)
    // SSE messages are buffered, not applied

FUNCTION onAppForegrounded():
    isAppForeground = true
    // Resume poll loops — each fires an immediate poll on next tick
    // Apply any buffered SSE messages for visible sections
```

| Platform  | Background signal             | Foreground signal           |
|-----------|-------------------------------|-----------------------------|
| Android   | `Lifecycle.Event.ON_STOP`     | `Lifecycle.Event.ON_START`  |
| iOS       | `ScenePhase.background`       | `ScenePhase.active`         |
| Web       | `visibilitychange` → `hidden` | `visibilitychange` → `visible` |

### Viewport Visibility Detection

Use a single 1.5× viewport lookahead with 500ms debounce on exit:

```
FUNCTION isSectionNearViewport(sectionElement):
    // Platform-specific visibility detection
    //   Web:     IntersectionObserver with rootMargin "50% 0px"
    //   Android: LazyListState.layoutInfo with buffer zone
    //   iOS:     LazyVStack .onAppear/.onDisappear (default ~1 screen lookahead)

    ON_ENTER (section within 1.5× viewport):
        cancelExitTimer(section.id)
        setNearViewport(section.id, true)   // immediate

    ON_EXIT (section leaves 1.5× viewport):
        startExitTimer(section.id, 500ms):
            setNearViewport(section.id, false)  // debounced
```

### Gating Poll Loops

```
FUNCTION setupPolling(screen):
    FOR section IN screen.sections:
        policy = section.refreshPolicy
        IF policy.type != "poll": CONTINUE

        shouldPause = policy.pauseWhenOffScreen ?? true

        SCHEDULE_LOOP:
            // Gate 1: app must be in foreground
            AWAIT isAppForeground == true

            // Gate 2: section must be near viewport (unless opt-out)
            IF shouldPause:
                AWAIT isSectionNearViewport(section.id)

            data = FETCH(policy.url)
            updateSectionData(section.id, data)

            // Re-check before sleeping — if section left viewport during
            // fetch, loop back to the gate instead of sleeping a full interval
            IF shouldPause AND NOT isSectionNearViewport(section.id):
                CONTINUE  // → loops back to AWAIT

            DELAY(policy.intervalMs)
```

### Gating SSE Processing

```
FUNCTION onSseMessage(section, message):
    shouldPause = section.refreshPolicy.pauseWhenOffScreen ?? true
    isForeground = isAppForeground
    isVisible = NOT shouldPause OR isSectionNearViewport(section.id)

    IF NOT isForeground OR NOT isVisible:
        // Buffer the latest message — only the most recent is kept
        sseBuffer[section.id] = message
        RETURN

    applyBindings(section, message)

FUNCTION onSectionBecameVisible(sectionId):
    buffered = sseBuffer.remove(sectionId)
    IF buffered IS NOT NULL:
        applyBindings(section, buffered)
```

### Key Decisions

| Decision | Behavior |
|----------|----------|
| SSE on pause | Stay subscribed, stop calling `applyBindings()`. Buffer latest message. |
| SSE on resume | Apply only the most recent buffered message (not full backlog). |
| Poll on pause | Cancel pending timer. Do not fire in-flight request. |
| Poll on resume | Fire immediately, then resume interval timer. |
| Paused ≠ stale | A paused section shows last-known data — no stale indicator. |
| Inactive tabs | TabGroup: inactive tab's sections always paused; active tab follows viewport rules. |

---

## 9. Impression Tracking Algorithm

Fire analytics events when sections become visible, with configurable
deduplication.

### ImpressionPolicy Model

```
ImpressionPolicy:
    dedup: "once-per-screen" | "once-per-session" | "once-per-interval" | "none"
    intervalMs: integer?              // For once-per-interval only
    threshold: ImpressionThreshold?

ImpressionThreshold:
    visibility: float = 0.5           // 50% of element visible (0.0–1.0)
    dwellMs: integer = 1000           // Must be visible for this duration
```

### Algorithm

```
FUNCTION trackImpression(sectionId, actions, isVisible, visibilityRatio):
    // Filter to onVisible + fireAndForget actions
    impressionActions = FILTER actions WHERE
        trigger == "onVisible" AND type == "fireAndForget"

    IF impressionActions IS EMPTY:
        RETURN

    threshold = action.impression.threshold OR DEFAULT_THRESHOLD
    IF NOT isVisible OR visibilityRatio < (threshold.visibility OR 0.5):
        cancelDwellTimer(sectionId)
        RETURN

    // Start dwell timer
    startDwellTimer(sectionId, threshold.dwellMs OR 1000):
        FOR action IN impressionActions:
            dedupKey = sectionId + ":" + action.event
            policy = action.impression.dedup OR "once-per-screen"

            SWITCH policy:
                "once-per-screen":
                    IF hasFired(dedupKey): CONTINUE
                    markFired(dedupKey)

                "once-per-session":
                    IF hasFiredSession(dedupKey): CONTINUE
                    markFiredSession(dedupKey)

                "once-per-interval":
                    IF NOT canFireInterval(dedupKey, action.impression.intervalMs): CONTINUE
                    markFiredInterval(dedupKey)

                "none":
                    // Always fire

            dispatchSingleAction(action, stateManager)
```

---

## 10. Screen Fetch Lifecycle

The complete lifecycle for loading and maintaining a screen.

```
FUNCTION loadScreen(uri):
    // 1. Resolve URI
    endpoint = resolveEndpoint(uri)    // "nba://for-you" → "/sdui/for-you"

    // 2. Check offline cache
    cached = offlineCache.get(endpoint)
    IF cached IS NOT NULL:
        renderScreen(cached)           // Show stale data immediately

    // 3. Fetch from server
    TRY:
        screen = repository.fetchScreen(endpoint, variant)
        offlineCache.put(endpoint, screen)
        renderScreen(screen)
    CATCH networkError:
        LOG_ERROR("Screen fetch failed: " + networkError)
        IF cached IS NULL:
            renderErrorState(networkError)   // Compose client-side ErrorState
        // If cached was shown, keep it visible

    // 4. Setup live data channels
    setupPolling(screen)
    setupRealTimeForScreen(screen)

    // 5. Initialize screen state
    IF screen.state IS NOT NULL:
        FOR key, value IN screen.state:
            stateManager.setState(key, value)

FUNCTION resolveEndpoint(uri):
    // Pure prefix swap — no special-case branching
    IF uri STARTS_WITH "nba://":
        RETURN "/sdui/" + REMOVE_PREFIX(uri, "nba://")
    RETURN uri    // Already a path
```

---

## 11. Request Envelope

Every composition request — initial loads, navigation, pull-to-refresh,
action-driven refresh (including parameterized refresh with
`paramBindings`), and any other future fetch — routes through one shared
fetch primitive (`SduiRepository.fetchScreen` on iOS/Android,
`fetchSduiScreen` on web). That primitive owns the entire transport
contract; bespoke `fetch` / `URLRequest` / `OkHttp` calls for composition
data are prohibited.

### 11.1 Wire shape

The envelope is serialized as **bracket-notation query parameters** for
GET requests and as a **JSON body of the same shape** for POST requests:

| Query Param | JSON Path | Purpose |
|-------------|-----------|---------|
| `locale=en` | `locale` | Locale for i18n |
| `schemaVersion=1.0` | `schemaVersion` | Schema version |
| `gameState=live` | `gameState` | Optional state filter |
| `platform[name]=android` | `platform.name` | Platform identifier |
| `platform[appVersion]=8.3.0` | `platform.appVersion` | App version |
| `platform[osVersion]=14` | `platform.osVersion` | OS version |
| `platform[deviceClass]=phone` | `platform.deviceClass` | Form factor |
| `platform[capabilities][sse]=true` | `platform.capabilities.sse` | Capability declaration |
| `device[countryCode]=US` | `device.countryCode` | Device context |
| `device[zipCode]=10001` | `device.zipCode` | Device context |
| `experiments[exp_id]=variant_b` | `experiments.exp_id` | Experiment assignment |

The server resolves both shapes into one typed `SduiRequestContext` via a
shared `BracketParamResolver`. The client and server therefore see the
same fields regardless of HTTP method.

### 11.2 GET-first, POST fallback at 8192 chars

The shared fetch primitive uses GET by default and switches to POST when
the encoded envelope query string exceeds 8192 chars. Every composition
endpoint is dual-mounted (`@GetMapping` + `@PostMapping`) on the server
and dispatches to the same handler — there are no GET-only or POST-only
composition routes.

### 11.3 User-supplied filter params (Form / refresh `paramBindings`)

User-supplied filter params (e.g. a Form's `paramBindings` resolved into
`{perMode: "Totals", season: "2025-26"}`) **always ride the URL query
string regardless of HTTP method**, so the server reads them through
`@RequestParam` on either side. They participate in the GET/POST length
decision alongside the envelope.

| Where they live | GET | POST |
|-----------------|-----|------|
| Envelope params (`platform[*]`, `device[*]`, `experiments[*]`, `locale`, `schemaVersion`, `gameState`) | URL query (bracket notation) | JSON body (same shape) |
| User filter params (`perMode=Totals`, `season=2025-26`) | URL query | URL query |
| `X-Trace-Id` | Header | Header |

Encoding rules (apply uniformly to both halves of the query string):

- **RFC-3986 percent-encoding.** Spaces become `%20`, brackets `%5B` / `%5D`,
  ampersands `%26`, etc. Hand-rolled string concatenation
  (`"$key=$value"`) is prohibited because it silently corrupts spaces,
  ampersands, and non-ASCII bytes.
- **Deterministic key ordering.** User params are sorted by key; envelope
  ordering is fixed by the builder. Identical inputs produce
  byte-identical URLs across platforms — the CDN cache key depends on it.

### 11.4 `X-Trace-Id`

`X-Trace-Id` travels as an HTTP header on every request. Parameterized
refresh inherits its parent screen's trace ID so server logs correlate
the refresh response with the screen that triggered it.

### 11.5 Headers

| Header | Required | Purpose |
|--------|----------|---------|
| `Authorization` | Yes (when authenticated) | Bearer token |
| `X-Trace-Id` | Yes | Request correlation |
| `Content-Type: application/json` | POST only | Envelope body |

`platform[name]` and `schemaVersion` are part of every composition request
(query on GET, same fields in the JSON body on POST). Clients do not send
`X-Platform` or `X-Schema-Version` headers.

---

## 12. Offline Cache Strategy

Cache complete screen responses keyed by endpoint. Serve stale data on
network failure.

```
FUNCTION cacheGet(endpoint):
    entry = cache[endpoint]
    IF entry IS NULL:
        RETURN null
    RETURN entry.payload    // Return regardless of age — stale is better than blank

FUNCTION cachePut(endpoint, screen):
    cache[endpoint] = CacheEntry(
        payload = screen,
        cachedAt = NOW()
    )
```

### Key Decisions

| Decision | Behavior |
|----------|----------|
| Cache key | Endpoint path (e.g., `/sdui/scoreboard?variant=A`) |
| Stale policy | Always serve stale — a stale screen is better than a blank screen |
| Invalidation | Overwrite on successful fetch |
| Storage | Platform-appropriate: SQLite/Room, Core Data, IndexedDB, file system |
| TTL | No explicit expiry — server controls freshness via `refreshPolicy` |

---

## 13. Error Handling

### Two Error Surfaces

Every client must support two distinct error surfaces:

| Surface | Origin | When |
|---------|--------|------|
| **Server-composed ErrorState** | Server response (`AtomicComposite`) | Anticipated failures: bad game ID, missing data, upstream timeout. Rendered like any other section. |
| **Client-composed error card** | Client render pipeline | Unanticipated failures: renderer crash, data validation failure, deserialization failure after response accepted. |

### Network Errors

```
IF fetchScreen fails:
    IF offlineCache HAS cached response:
        Show cached response (stale data)
    ELSE:
        Render client-composed ErrorState:
            title: "Unable to Load"
            message: error.localizedMessage
            retryAction: action(type="refresh", trigger="onTap")
```

### Parse Errors

```
IF JSON deserialization fails:
    LOG_ERROR("Parse failed for endpoint: " + endpoint + " — " + error)
    Render ErrorState with diagnostic info
```

### Section-Level Errors — Catch-at-Dispatch + Pre-Validation

The isolation pattern for section render failures. This catches ~95% of real-world
failures (data problems: unexpected nulls, type mismatches, missing nested objects).
True rendering engine crashes (framework-level) are handled by app-level crash
telemetry (e.g., New Relic), not per-section isolation.

```
FOR each section in screen.sections:
    // Pre-validation: check section data before rendering
    validationError = validateSection(section)
    IF validationError:
        LOG_ERROR("Section validation failed: id=" + section.id
                  + " type=" + section.type + " error=" + validationError)
        reportToObservability(validationError, section.id, section.type)
        IF section.sectionStates.error.hideOnError:
            SKIP section (render nothing)
        ELSE:
            renderErrorCard(section.sectionStates, validationError)
        CONTINUE to next section

    // Dispatch to renderer (platform-specific isolation where available)
    TRY:
        renderSection(section)
    CATCH error:
        LOG_ERROR("Section render failed: id=" + section.id
                  + " type=" + section.type, error)
        reportToObservability(error, section.id, section.type)
        IF section.sectionStates.error.hideOnError:
            SKIP section (render nothing)
        ELSE:
            renderErrorCard(section.sectionStates, error)
        // Other sections continue rendering normally
```

**Full-skip policy**: When a section fails, replace the **entire section** with the
error card (or collapse it if `hideOnError`). Never show partial render output.

### Error Card Rendering

```
FUNCTION renderErrorCard(sectionStates, error):
    message = sectionStates?.error?.message ?? error.localizedMessage ?? "Something went wrong"
    retryAction = sectionStates?.error?.retryAction

    RENDER:
        icon: "⚠"
        text: message
        IF retryAction AND retryCount < maxRetries:
            button: "Try Again" → fire retryAction through action handler, increment retryCount
        // After maxRetries exhausted, show permanent error card (no retry button)
```

### Retry Budget

Max retry count is a **client-side configuration** (default: 5), not a schema or
server field. Per-section, per-screen-visit — navigating away and back resets the
counter. After exhausting retries, the error card becomes permanent (retry button
removed).

### Error Telemetry

Section render failures must be reported as **non-fatal errors** to the app's
observability platform (e.g., New Relic) using existing instrumentation. This does
NOT use SDUI `fireAndForget` actions — client-side crash telemetry is a client
infrastructure concern, not server-driven composition. The error report must include:
section ID, section type, platform, app version, and stack trace.

### Platform-Specific Isolation Notes

| Platform | Technology | Isolation Mechanism |
|---|---|---|
| Web (React) | `ErrorBoundary` / `getDerivedStateFromError()` | Built-in framework support — catches recomposition crashes |
| Android / Fire TV (Compose) | Catch-at-dispatch + pre-validation | `try/catch` around `SectionRouter` dispatch. Compose does not support `try/catch` around `@Composable` invocations, so pre-validation catches data problems before rendering. |
| iOS / tvOS (SwiftUI) | Catch-at-dispatch + pre-validation | Same pattern as Compose — SwiftUI has no `ErrorBoundary` equivalent. |

### Invariant: Never Swallow Exceptions

Every catch block must log with enough context to diagnose (§12-compliant):
- Class/component name
- Section ID
- Section type
- Error message and stack trace

Silent `catch { null }` patterns are prohibited.

---

## 14. Accessibility Contract

The schema includes accessibility properties on sections and atomic elements.
Map them to platform-native accessibility APIs.

```
AccessibilityProperties:
    label: string?        // Screen reader text
    role: string?         // Semantic role ("button", "heading", "image", "link", "tab")
    hint: string?         // Additional context for screen reader
    hidden: boolean?      // Hide from accessibility tree
```

Map to:
- **Android**: Compose `Modifier.semantics { }`, `contentDescription`, `role`
- **iOS**: `.accessibilityLabel()`, `.accessibilityAddTraits()`, `.accessibilityHint()`
- **Web**: `aria-label`, `role`, `aria-hidden`, `aria-describedby`
- **Other platforms**: Equivalent accessibility APIs

---

## 15. Image Loading Contract

All image URLs come from the server response. Clients must never construct
image URLs from patterns.

```
FUNCTION loadImage(imageElement):
    url = imageElement.url                     // Primary URL from server
    fallbackUrl = imageElement.fallbackUrl     // Optional fallback
    alt = imageElement.alt                      // Accessibility text

    TRY:
        LOAD_AND_DISPLAY(url)
    CATCH:
        IF fallbackUrl IS NOT NULL:
            TRY:
                LOAD_AND_DISPLAY(fallbackUrl)
            CATCH:
                DISPLAY_PLACEHOLDER(alt)
        ELSE:
            DISPLAY_PLACEHOLDER(alt)
```

Platform SDK recommendations (not mandated):
- **Android**: Coil, Glide
- **iOS**: Kingfisher, SDWebImage, Nuke
- **Web**: Native `<img>` with `onError` handler
- **Other**: Any async image loader with caching

---

## 16. Codegen Quick Reference

Generated models are written directly into each client's source tree where
they are actually consumed; there is no intermediate copy step for any
client. Regenerate everything with `make codegen` (or
`cd codegen && ./generate.sh`).

| Language   | Output                                                                        | Consumer                                                      |
|------------|-------------------------------------------------------------------------------|---------------------------------------------------------------|
| Java       | `codegen/build/generated-sources/jsonschema2pojo/`                            | Spring server (on the classpath)                              |
| Kotlin     | `android/sdui-core/src/main/java/com/nba/sdui/core/models/generated/SduiModels.kt` | Android `sdui-core` module (quicktype + Jackson)         |
| Swift      | `ios/Sources/SduiCore/Models/SduiModels.swift`                                | iOS `SduiCore` SwiftPM target                                 |
| TypeScript | `web/src/generated/SduiModels.ts`                                             | Web client via the `@sdui/models` Vite / tsconfig path alias  |

For other languages, use [quicktype](https://quicktype.io) to generate models
from `schema/sdui-schema.json`. Or write your own deserializer — the JSON
schema is the contract, not the generated code.

---

## 17. Renderer Animation Guidelines

These visual effects are **renderer responsibilities**, not schema properties.
Implementations should follow platform-native animation patterns.

| Effect | Server Sends | Renderer Does |
|---|---|---|
| **Live pulse** | An element whose `bindRef` resolves to a truthy `isLive` / `badgeText == "LIVE"` entry in `content`, or an explicit opacity-animated leaf in the composite tree | Animate opacity 0.3→1.0 on the indicator element, repeating with autoreversal. **iOS:** `.animation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true))`. **Compose:** `infiniteTransition.animateFloat()`. **Web:** CSS `@keyframes pulse`. |
| **Numeric transitions** | Updated score / clock field in `content.*` via SSE data binding; leaf `Text` reads it through `bindRef` | Apply content transition on value change. **iOS:** `.contentTransition(.numericText())`. **Compose:** `AnimatedContent`. **Web:** CSS `transition` on the value container. |
| **Clock interpolation** | `LiveClock` primitive with `isRunning: true`, `snapshotSeconds`, `snapshotAt`, `tickDirection`, `stopAtSeconds`, `format` (see §4c) | Run the tick loop in §4c. Each frame's displayed value is `snapshotSeconds ± (now − snapshotAt)`, clamped by `stopAtSeconds`. When an SSE update rewrites `content.*`, `bindRef` re-resolves on next render and the tick loop re-anchors — no drift accumulation. Set `isRunning: false` to freeze on the snapshot value. |
| **Color mixing** | Pre-computed hex colors | Server computes gradient tints from team colors at composition time. No runtime color math on clients. |
| **Image load states** | `src` + `placeholder` on Image elements | Show loading placeholder → success image, or fall back to `placeholder` URL on failure. **iOS:** `AsyncImage { phase in }`. **Compose:** Coil `AsyncImage`. **Web:** `<img>` with `onError` fallback. |
| **Pull-to-refresh** | `refreshPolicy: { type: "poll" }` on screen | Add platform pull-to-refresh gesture. **iOS:** `.refreshable {}`. **Compose:** `pullRefresh()`. **Web:** custom gesture or library. |
| **Opacity-based overlays** | `opacity: 0.7` on a Container element | Apply the opacity value directly. Used for duration badge backgrounds and faded states. |
| **Shadow rendering** | `shadow: { color, radius, offsetX, offsetY }` on elements | **SwiftUI:** `.shadow(color:radius:x:y:)` — exact match. **Compose:** `Modifier.shadow(elevation = radius * 1.5)` — approximation (offset not supported). **Web:** `box-shadow` — exact match. |
| **Badge positioning** | `badge: { element, alignment }` on parent | **SwiftUI:** `.overlay(alignment:) { ... }`. **Compose:** `Box { content; Box(Modifier.align(...)) { ... } }`. **Web:** `position: relative` parent + `position: absolute` child. |

**Timing recommendations:**
- Content transitions: 300ms ease-in-out
- Pulse animations: 800ms ease-in-out, repeat forever with autoreversal
- `LiveClock` tick cadence: 10Hz (100ms) baseline, may coalesce to the
  platform's display-refresh loop (see §4c).

---

## 17a. Non-atomic variant tokens (FormField select)

Most variant tokens live on atomic primitives and carry the vocabularies
documented in `docs/sdui-design-system.md` §3 Layer 2. One vocabulary
lives on a non-atomic carrier — a field-data object (`FormField` when
`fieldType == "select"`). It obeys the same rules (strict-decode on the
typed field, renderer realizes natively, absent value falls through to
the default) but is exposed to the renderer through the field-data
model, not the `AtomicElement.variant` string.

### `SelectVariant`

Declared on `FormField.variant` and meaningful only when
`fieldType == "select"`. Values: `dropdown` (default), `chips`.
Absent value renders as `dropdown`.

| Platform | `dropdown` | `chips` |
|---|---|---|
| iOS (SwiftUI) | `Menu { Picker(selection:) { ... } }` | Horizontal `ScrollView(.horizontal, showsIndicators: false)` of `Capsule()` `Button`s with selected state styled via color tokens |
| Android (Compose) | Material 3 `ExposedDropdownMenuBox` + `DropdownMenu` | `LazyRow` of Material 3 `FilterChip` with correct `selected` state |
| Web (React/CSS) | Native `<select>` | Horizontal scroll container of pill-styled `<button>`s with a `selected` class |

**Not a `radio` migration.** `FormField.fieldType: "radio"` coexists
with `fieldType: "select" + variant: "chips"`. `radio` is a vertical
stack of labeled choices (multi-line, all options visible); `chips` is
a horizontal capsule row (usually 2–5 options). No migration between
them is planned.

---

## 18. Conformance Checklist

A conforming client satisfies all of the following. This maps directly to
the rules in `AGENTS.md`.

| # | Requirement | AGENTS.md Section |
|---|-------------|-------------------|
| C1 | Models derived from schema (generated or hand-matched) | §1.2 |
| C2 | No hardcoded server paths — all from server response or URI resolution | §3.1 |
| C3 | No `ScreenType` enum — generic `fetchScreen(endpoint)` | §3.1 |
| C4 | Ably messages treated as opaque maps — never typed data classes | §4.4 |
| C5 | All image URLs from server — no client-constructed URLs | §3.2 |
| C6 | ErrorState rendered as a first-class section | §8.0 |
| C7 | Unknown section/atomic types skipped with log | §10.2 |
| C8 | Single generic fetch method — no `getGameDetail()` | §4.1 |
| C9 | Refresh driven by `refreshPolicy` — no hardcoded intervals | §3.3 |
| C10 | URI resolution is a simple prefix swap | §3.1 |
| C11 | `platform[name]` and `schemaVersion` on every composition request (envelope) | `AGENTS.md` §3.4; §11 |
| C12 | Exceptions logged with context — never silently swallowed | §10.1 |
| C13 | Schema is the wire contract — strict decoders; new enum values go into schema first, then regen | §1.3 |
| C14 | Renderers are presentation-only — no business logic | §5 |
| C14a | Every atomic primitive routes its content through a single `AtomicBox` helper that applies `margin → opacity → shadow → corner-clip → background → gradient overlay → border → padding → sizing → badge` in that order. Primitives own only their content (typography, flex layout, scroll behaviour, image scaling). See §4a | §5 |
| C15 | Prefer AtomicComposite over new section type for stateless UI | §6 |
| C16 | Platform-native realization of semantic variant tokens — `TextVariant`, `ButtonVariant`, `ContainerVariant`, `ImageVariant` on atomic elements, `SelectVariant` on `FormField.select` — emitted on the wire as a `variant` string property; map to the platform's current design language; no pixel-parity target. See §17a for the `SelectVariant` renderer mapping table | §7 |
| C17 | Code comments describe invariants and cite business constraints; do not cite internal rule numbers or AI-coding guidelines | §10.3 |
| C18 | Per-platform decisions default to the server (content, capability gating, asset-format). Client-realized vocabularies are the named exception (§7 list) and must be pure presentation | §1.1 |
| C19 | `LiveClock` primitive ticks client-side from server-provided snapshot fields (`snapshotSeconds`, `snapshotAt`, `isRunning`, `tickDirection`, `stopAtSeconds`, `format`). Implements the tick-loop contract in §4c at ≥10Hz baseline, re-anchors on every SSE / poll update that rewrites `content.*`, renders with tabular-numeral typography | — |
| C20 | Inside an `AtomicComposite`, leaf primitives with a `bindRef: string` dot-path resolve their canonical live field (Text → `content`, Button → `label`, Image → `src`, LiveClock → snapshot object) from `section.data.content`. `compositeContent` is threaded to descendants via the platform's implicit-context primitive (Environment / CompositionLocal / React context); missing paths fall back to inline values and never overwrite with null. See §4b | — |
