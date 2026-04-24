# Plan: Variant Prune, `LiveClock` Primitive, and GamePanel → `AtomicComposite`

> **Status**: Implemented.
>
> **Scope**: schema, codegen, all three clients (iOS / Android / web),
> server composers, docs. Breaking change by design — this is a
> governance cleanup pass plus a section-to-composite migration; the
> user has explicitly waived backward compatibility for this iteration.

## Summary

Three related changes land together in one plan because they unlock each
other:

1. **Prune Rule 19–weak variant values.** Several values in
   `ContainerVariant`, `ImageVariant`, `SelectVariant`, and the legacy
   aliases in `TextVariant` are either inline-expressible, single-site,
   or zero-site. Migrate each call site to inline style props and remove
   the enum values.
2. **Introduce a new atomic primitive `LiveClock`.** A tiny leaf whose
   only job is to carry a server-provided clock snapshot + `isRunning`
   flag and interpolate locally between Ably updates. Each platform
   realizes the tick loop natively (`TimelineView`, Compose frame
   callbacks, `requestAnimationFrame`). The wire payload is temporal and
   platform-neutral — `snapshotSeconds`, `snapshotAt`, `isRunning`,
   `tickDirection`, `stopAtSeconds`, `format`. (The field is named
   `tickDirection` rather than `direction` because `AtomicElement`
   already owns a `direction` field for flex-axis layout.)
3. **Migrate `GamePanel` from a permanent section to a server-composed
   `AtomicComposite`.** The only remaining client-owned state on
   GamePanel today is the ticking-clock interpolation; once that lives
   in `LiveClock`, GamePanel has no Rule 15 justification to remain a
   section. Server emits a composite built from `Container` / `Text` /
   `Image` / `LiveClock`. Ably `dataBindings` continue to flow through
   `refreshPolicy.type = "sse"` on the composite envelope.

The end state collapses one section renderer per platform, shrinks the
variant vocabulary, and replaces the `GamePanelVariant` enum with inline
numeric deltas emitted by the composer.

## Why it matters

- **Rule 19.** Six of `ContainerVariant`'s values have 0–1 emission
  sites today; two `ImageVariant` values have 0–1 sites; one
  `SelectVariant` value (`segmented`) has zero sites; six `TextVariant`
  legacy aliases duplicate the Material 3 scale. Rule 19 is explicit:
  values proposed speculatively *must be removed before they ship*, and
  duplicate values are a prune signal. The wire contract is
  asymmetrically expensive — trivial to add, costly to remove after
  apps ship. Taking the prune now, before the ref-apps adopt this
  schema, is cheaper than deprecation later.
- **Rule 15 / Rule 18.** The permanent-section list exists for UI that
  owns state the server cannot compose: Ably subscriptions, platform
  SDKs, interaction state machines. GamePanel's entry in that list
  cites "Ably SSE subscription, live score state machine." SSE already
  works on any section via `refreshPolicy` + `dataBindings`; the
  "state machine" is a clock-tick loop that belongs in a primitive, not
  a section. Removing the section-level justification with a
  primitive-level solution is exactly how the rule is supposed to
  bend — toward composition, away from client-owned UI types.
- **Client release cadence.** Today, changing the visual treatment of
  GamePanel (say, a new featured style, a new badge placement, a
  different chrome for pregame vs. live) requires a client release
  because the renderer hardcodes the branches. Once GamePanel is an
  `AtomicComposite`, the server can change its composition without
  touching any client. That's the headline KPI of the architecture.

## Current state (for context)

### Variant emission inventory

| Vocabulary | Values emitted today | Values with ≥ 2 server emission sites | Prune candidates |
|---|---|---|---|
| `TextVariant` (22) | All M3 values + `score` + six legacy aliases | M3 subset, `score` | `heading1`, `heading2`, `heading3`, `body`, `caption`, `label` (duplicates of M3) |
| `ButtonVariant` (4) | `primary`, `secondary`, `tertiary`, `text` | All four | — |
| `ContainerVariant` (6) | `hero` (1), `grouped` (1), `banner` (1), `elevated` (0), `subtle` (0), `overlay` (0) | None strictly; `hero` + `grouped` are inexpressible SDK idioms and kept on that ground | `elevated`, `subtle`, `overlay`, `banner` |
| `ImageVariant` (3) | `thumbnail` (3), `hero` (1), `logo` (0) | `thumbnail` | `hero`, `logo` |
| `SelectVariant` (3) | `dropdown` (implicit), `chips` (4), `segmented` (0) | `dropdown`, `chips` | `segmented` |
| `GamePanelVariant` (2) | `standard` (implicit), `featured` (1) | None | Entire enum (removed with the section migration) |

Emission sites are in `server/src/main/java/com/nba/sdui/service/AtomicCompositeBuilder.java` (helper factories
`heroContainer`, `bannerContainer`, `groupedContainer`, `heroImage`, `thumbnailImage`) and in the per-surface composers (`ForYouComposer`, `ScheduleComposer`, `GameDetailComposer`).

### GamePanel client surface area

| Responsibility in `GamePanelView.swift` / `GamePanelRenderer.kt` / `GamePanel.tsx` | Post-migration home |
|---|---|
| Team logos | `Image` primitive |
| Tricode + score text | `Text` (with `variant: "score"` for tabular numerals) |
| Score count-up animation | Client-local behaviour of `Text` when `variant == "score"` (already in iOS via `.contentTransition(.numericText())`); reuse cross-platform |
| Status text / badge text | `Text` primitive |
| Period / clock badge chrome | `Container` (inline `background` + `cornerRadius`) wrapping a `LiveClock` |
| Live-region accessibility trait | Derived client-side from `variant == "score"` or from `refreshPolicy.type == "sse"` on the composite envelope |
| `onTap` navigation | `onTap` action on the root `Container` |
| Ably binding for scores / clock | `refreshPolicy` + `dataBindings` on the composite envelope — unchanged mechanism |
| `GamePanelVariant.featured` card sizing | Inline `padding`, `cornerRadius`, `shadow` deltas emitted from the composer |

### Plan does **not** change

- The server wire vocabulary stays platform-neutral. Inline style
  props replacing variants (`padding`, `cornerRadius`, `background`,
  `shadow`, color tokens) already exist and mean the same thing on
  every platform. `LiveClock` fields are temporal/semantic, not
  platform-specific. See Rule 18 — realization is client-side, intent
  is on the wire.
- `refreshPolicy` + `dataBindings` are untouched — they already work
  on any section including `AtomicComposite`.
- AtomicComposite depth / node / child caps (6 / 50 / 20) are
  sufficient for the GamePanel layout and do not need to move.

---

## Phase A — Variant prune

### A.1 `TextVariant` legacy aliases → Material 3 names

Emission sites (all in `server/src/main/java/com/nba/sdui/service/`):

| Alias | File:Line | Replacement |
|---|---|---|
| `heading1` | `GameDetailComposer.java:658` | `headlineLarge` |
| `heading2` | `GameDetailComposer.java:670`, `ScheduleComposer.java:218` | `headlineMedium` |
| `body` | `GameDetailComposer.java:684`, `ScheduleComposer.java:236` | `bodyMedium` |
| `caption` | `GameDetailComposer.java:859`, `ScheduleComposer.java:228` | `labelSmall` |
| `label` | (no current sites; enum-only) | `labelMedium` |
| `heading3` | (no current sites; enum-only) | `headlineSmall` |

Steps:

1. Migrate every server emission site listed above to the M3 equivalent.
2. Remove `heading1`, `heading2`, `heading3`, `body`, `caption`, `label`
   from the `TextVariant` enum in
   [schema/sdui-schema.json](../../schema/sdui-schema.json).
3. Remove the corresponding entries from each client's `TextVariant`
   resolver:
   - `ios/Sources/SduiCore/Rendering/Atomic/AtomicTextView.swift`
     (`font(for:)` lookup).
   - `android/.../renderer/atomic/AtomicText.kt`
     (`mapTypographyVariant`).
   - `web/src/components/atomic/AtomicText.tsx` (`variantStyles`).
4. Run `make codegen`, update existing round-trip fixtures that
   reference the removed aliases, and add a `DEPRECATED` entry to
   [docs/sdui-design-system.md](../sdui-design-system.md) that names
   the removed aliases and their M3 replacements.

### A.2 `ContainerVariant`: drop `elevated`, `subtle`, `overlay`, `banner`

Emission sites:

| Value | Site | Migration |
|---|---|---|
| `banner` | `AtomicCompositeBuilder.buildPromoBanner` (`:114`) | Replace `bannerContainer(...)` with `container(...)` + inline `fillWidth: true`, `padding`, `background` from the caller's surface description. |
| `elevated` | none | Remove the `elevatedContainer` helper. |
| `subtle` | none | Remove the `subtleContainer` helper. |
| `overlay` | none | Remove the `overlayContainer` helper. |

Steps:

1. In `AtomicCompositeBuilder.buildPromoBanner`, switch the root to a
   plain `container(...)` call and set `fillWidth`, `padding`, and
   `background` inline from the promo-banner's existing surface spec.
2. Remove the four helpers (`bannerContainer`, `elevatedContainer`,
   `subtleContainer`, `overlayContainer`) from
   `AtomicCompositeBuilder.java`.
3. Remove the four values from the `ContainerVariant` enum in the
   schema.
4. Remove the four branches from each client's
   `ContainerVariantResolver`:
   - `ios/Sources/SduiCore/Rendering/ContainerVariantResolver.swift`
   - `android/.../renderer/ContainerVariantResolver.kt`
   - `web/src/utils/ContainerVariantResolver.ts`
5. Keep `hero` and `grouped`. Each has only one emission site today;
   the justification for keeping them is inexpressibility (Liquid
   Glass / `.ultraThinMaterial` / M3 Expressive for `hero`;
   `.insetGrouped` / Material `ListItem` grouping for `grouped`). Add
   a short note to
   [docs/sdui-design-system.md](../sdui-design-system.md) flagging
   that each must grow a second evidenced emission site in the next
   content-surface work or be re-evaluated for pruning.

### A.3 `ImageVariant`: drop `hero` and `logo`

Emission sites:

| Value | Site | Migration |
|---|---|---|
| `hero` | `AtomicCompositeBuilder.buildHeroPanel` (`:415`) | Replace `heroImage(url)` with `image(url, width, height, "cover")` plus inline `fillWidth` / `cornerRadius` as the caller requires. |
| `logo` | none | Remove the `logoImage` helper. |

Steps:

1. In `buildHeroPanel`, replace `heroImage(thumbnailUrl)` with a plain
   `image(...)` call carrying inline width / height / objectFit that
   matches today's resolved behaviour for `ImageVariant.hero`. Capture
   today's resolved values from the three `ImageVariantResolver`
   implementations as the migration target.
2. Remove `heroImage` and `logoImage` helpers from
   `AtomicCompositeBuilder.java`.
3. Remove `hero` and `logo` from the `ImageVariant` enum in the schema.
4. Remove the two branches from each client's `ImageVariantResolver`
   (iOS / Android / web).

### A.4 `SelectVariant`: drop `segmented`

Emission sites: none.

Steps:

1. Remove `segmented` from the `SelectVariant` enum in the schema.
2. Remove the `.segmented` branch from each client's Form select
   renderer:
   - `ios/Sources/SduiCore/Rendering/Sections/FormSectionView.swift`
     (delete the `.pickerStyle(.segmented)` branch).
   - `android/.../renderer/sections/FormRenderer.kt` (delete the
     `SegmentedButton` branch).
   - `web/src/components/sections/Form.tsx` (delete the pill-button
     segmented branch).
3. Update [docs/sdui-design-system.md](../sdui-design-system.md): drop
   `segmented` from the variant catalogue.
4. Update
   [docs/plans/ios-led-ux-rebuild.md](ios-led-ux-rebuild.md) to note
   the `segmented` value has been pruned; no other plan tasks depend
   on it.

### A.5 Codegen + round-trip

1. Run `make codegen` after the schema edits land.
2. Regenerate `codegen/output/{kotlin,swift,typescript}/SduiModels.*`.
3. Update `ios/Tests/SduiCoreTests/Fixtures/*.json` to remove any
   reference to the pruned variant values. Update
   `schema/examples/*.json` similarly.
4. Confirm round-trip tests pass on iOS; fix any fixture that
   references a removed value.

---

## Phase B — `LiveClock` atomic primitive

### B.1 Schema

Add a new `type` value `LiveClock` to the `AtomicElement.type` enum
in [schema/sdui-schema.json](../../schema/sdui-schema.json) (currently
on schema line 427), and add the following fields to `AtomicElement`:

```jsonc
{
  "snapshotSeconds": {
    "type": "integer",
    "minimum": 0,
    "description": "Clock value in seconds at the moment captured by snapshotAt. Clients interpolate from this anchor while isRunning == true."
  },
  "snapshotAt": {
    "type": "string",
    "format": "date-time",
    "description": "Wall-clock instant (ISO-8601) at which snapshotSeconds was captured. Clients compute elapsed = now - snapshotAt and derive the displayed value."
  },
  "isRunning": {
    "type": "boolean",
    "default": false,
    "description": "Whether the clock is actively ticking. When true, clients run a local tick loop at their platform-native refresh cadence (e.g. ~10Hz) and update the displayed value. When false, clients render snapshotSeconds verbatim."
  },
  "tickDirection": {
    "type": "string",
    "enum": ["down", "up"],
    "default": "down",
    "description": "Tick direction. 'down' decrements from snapshotSeconds toward stopAtSeconds (default 0); 'up' increments from snapshotSeconds with no upper bound unless stopAtSeconds is set. Named tickDirection to avoid collision with AtomicElement.direction (flex-axis layout)."
  },
  "stopAtSeconds": {
    "type": "integer",
    "description": "Optional clamp. For tickDirection 'down', clock holds at this value once reached. For tickDirection 'up', clock holds once reached. Omit to disable the clamp."
  },
  "format": {
    "type": "string",
    "enum": ["m:ss", "mm:ss", "h:mm:ss"],
    "default": "m:ss",
    "description": "Display format. Clients realize using their platform's tabular-numerals typography (equivalent to TextVariant.score)."
  }
}
```

All six fields are optional at the schema level except that
`snapshotSeconds` and `snapshotAt` become required when
`type == "LiveClock"`. Encode the requirement via the element's
`oneOf` branch (the same pattern already used to gate fields like
`src` on `Image`).

Keep these fields on `AtomicElement` directly rather than behind a
nested object so inline style props (`variant`, `color`, `padding`,
...) continue to apply uniformly — a `LiveClock` is a `Text`-shaped
leaf from the renderer's point of view.

### B.2 Codegen

Run `make codegen`. Verify:

- `codegen/output/swift/SduiModels.swift` grows a `LiveClock` case on
  the `AtomicElement.type` enum and the six fields.
- `codegen/output/kotlin/SduiModels.kt` and
  `codegen/output/typescript/SduiModels.ts` likewise.
- Strict decoders on each platform treat unknown values as decode
  errors (Rule 13 unchanged).

### B.3 iOS realization

Add `ios/Sources/SduiCore/Rendering/Atomic/AtomicLiveClockView.swift`.

Sketch:

```swift
struct AtomicLiveClockView: View {
    let element: AtomicElement
    var body: some View {
        TimelineView(.animation(minimumInterval: 0.1)) { context in
            Text(display(at: context.date))
                .sduiTextVariant(element.variant ?? .score)
        }
        .atomicBox(for: element)
    }

    private func display(at date: Date) -> String {
        // derive remaining = snapshotSeconds ± elapsed, clamped to stopAtSeconds,
        // format per element.format.
    }
}
```

Dispatch from `AtomicRouter.swift`: add a `case .liveClock:` branch.

Lifecycle: `TimelineView` naturally pauses when the view is off-screen
(SwiftUI suspends updates). No additional background-pause plumbing
needed for the phone client; Scene-phase handling is automatic.

### B.4 Android realization

Add
`android/sdui-core/src/main/java/com/nba/sdui/core/renderer/atomic/AtomicLiveClock.kt`.

Sketch:

```kotlin
@Composable
fun AtomicLiveClock(element: AtomicElement) {
    val now by produceState(initialValue = SystemClock.elapsedRealtime()) {
        while (isActive) {
            value = SystemClock.elapsedRealtime()
            withFrameNanos { }  // align to vsync, ~16ms
            delay(100L)         // coarser tick for battery
        }
    }
    AtomicBox(element) {
        Text(
            text = displayFor(element, now),
            style = AtomicText.mapTypographyVariant(element.variant ?: "score"),
        )
    }
}
```

Dispatch from `AtomicRouter.kt`: add a `"LiveClock" ->` branch.

Lifecycle: `produceState`'s coroutine is cancelled when the composable
leaves composition, which covers navigation-away. For background pause
(process in background), rely on the existing `ActivityLifecycle`
hookup used by other live-data primitives — if there is no equivalent
today, file a follow-up and ship with the coroutine cancellation path
only.

### B.5 Web realization

Add `web/src/components/atomic/AtomicLiveClock.tsx`.

Sketch:

```tsx
function AtomicLiveClock({ element }: { element: AtomicElement }) {
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    let raf = 0;
    let cancelled = false;
    const tick = () => {
      if (cancelled) return;
      setNow(Date.now());
      raf = window.requestAnimationFrame(tick);
    };
    const onVisibility = () => {
      if (document.hidden) cancelAnimationFrame(raf);
      else tick();
    };
    tick();
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      cancelled = true;
      cancelAnimationFrame(raf);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, []);
  return (
    <AtomicBox element={element}>
      <span className={variantClass(element.variant ?? "score")}>
        {displayFor(element, now)}
      </span>
    </AtomicBox>
  );
}
```

Dispatch from `web/src/components/atomic/AtomicRouter.tsx`: add a
`case "LiveClock":` branch.

Lifecycle: pause on `document.hidden` so a backgrounded tab doesn't
burn CPU.

### B.6 Accessibility cadence

A 10Hz visual tick must not drive assistive-technology announcements.
Each client wires the underlying Text such that:

- iOS: the `Text` does **not** get
  `.accessibilityAddTraits(.updatesFrequently)` from the tick — only
  from `isRunning` transitions. Screen-reader users hear the clock
  when the binding updates, not every frame.
- Android: likewise — do not set `liveRegion = POLITE` on the
  tick-driven Text; set it on the parent composite when the server
  indicates a live region is warranted.
- Web: no `aria-live="polite"` on the tick output; the enclosing
  composite decides.

### B.7 Validation fixture

Add `schema/examples/live-clock-leaf.json` exercising:

1. `isRunning: true`, `tickDirection: "down"`, `stopAtSeconds: 0`
   (typical live-game clock).
2. `isRunning: false`, `tickDirection: "down"` (timeout / review /
   period-break).
3. `tickDirection: "up"` (elapsed-time counter, e.g. pre-tip countdown
   inversion).

Add round-trip coverage in iOS (and Android / web if those suites
exist).

---

## Phase C — `GamePanel` → `AtomicComposite`

### C.1 New composer helper

Add `buildGamePanelComposite(...)` to
`server/src/main/java/com/nba/sdui/service/AtomicCompositeBuilder.java`.

Shape: a root `container(direction: "column")` with:

- **Header row**: optional `badgeText` as a small `Container` +
  `Text`; optional `visualLabel` as a muted `Text`.
- **Body row**: `container(direction: "row")` with the three
  sub-columns (away team / status + clock / home team):
  - Each team column: `Image` (logo), `Text` (tricode, `variant:
    "labelMedium"`), `Text` (score, `variant: "score"`).
  - Center column: `Text` (`gameStatusText`) + `LiveClock`
    (`snapshotSeconds`, `snapshotAt`, `isRunning`,
    `tickDirection: "down"`, `stopAtSeconds: 0`, `format: "m:ss"`).
    The `LiveClock` is only
    emitted while the game is in progress; for pre-game and final the
    center column emits `Text` only.
- **Root action**: `onTap navigate` to `nba://game/{gameId}`.
- **Refresh policy**: `refreshPolicy.type = "sse"` with the existing
  Ably channel configuration; `dataBindings` point at
  `snapshotSeconds` / `isRunning` / per-team `score` / `gameStatusText`
  paths inside the composite.
- **Featured treatment** (replaces `GamePanelVariant.featured`):
  inline numeric deltas — `padding` `20` instead of `16`,
  `cornerRadius` `16` instead of `12`, `shadow` opacity `0.18` instead
  of `0.08`, `shadow` radius `8` instead of `3`. Composer passes a
  boolean and these become the applicable inline props.

Verify the composite stays within the caps documented in AGENTS.md
Rule 15 (depth ≤ 6, ≤ 20 children per container, ≤ 50 nodes total);
the sketch above is ~15 nodes at depth 3.

### C.2 Migrate emission sites

Emission sites today (all in
`server/src/main/java/com/nba/sdui/service/`):

| File | Methods that emit a GamePanel section |
|---|---|
| `ForYouComposer.java` | `buildFeaturedFromLive`, `buildMockFeaturedGame`, `buildGamePanelSection`, `buildMockGamePanel` |
| `ScheduleComposer.java` | `buildGameCard` (currently a Composite — verify whether it's actually emitting `GamePanel` or already a composite; if composite, no migration needed here) |
| `LiveComposer.java` | confirm during implementation |
| `GameDetailComposer.java` | confirm during implementation |
| `DemoScreenComposer.java` | confirm during implementation |
| `SduiUtils.java` | confirm during implementation (helper only, may not emit) |

Steps:

1. Re-walk every GamePanel emitter above, map their today-output field
   set onto the new composite helper's parameters.
2. Replace each call with a call to `buildGamePanelComposite(...)`.
3. Confirm every replaced site still exposes the same `id` and
   `analyticsId` shape so downstream analytics don't drop.
4. Verify `dataBindings` paths on each composite match the Ably
   message shape; the pointed-at leaf fields are now
   `snapshotSeconds`, `isRunning`, `children[...].text` (for scores),
   etc. — the binding mechanism doesn't change, only the targets.

### C.3 Schema cleanup

1. Remove `GamePanel` from the `sectionType` enum
   ([schema/sdui-schema.json:886](../../schema/sdui-schema.json)).
2. Remove the `GamePanelData` definition.
3. Remove the `GamePanelVariant` definition and its `variant` field on
   GamePanel.
4. Remove `{ "$ref": "#/definitions/GamePanelData" }` from the
   `oneOf` on section data
   ([schema/sdui-schema.json:919](../../schema/sdui-schema.json)).

### C.4 Client cleanup

Delete the section renderer on each platform and remove the
SectionRouter branch:

| Platform | Files to delete | Router edit |
|---|---|---|
| iOS | `ios/Sources/SduiCore/Rendering/Sections/GamePanelView.swift` | Remove `case "GamePanel":` in `SectionRouter.swift` |
| Android | `android/sdui-core/src/main/java/com/nba/sdui/core/renderer/sections/GamePanelRenderer.kt` | Remove the `"GamePanel"` branch in `SectionRouter.kt` and from the `SUPPORTED_SECTION_TYPES` set |
| Web | `web/src/components/sections/GamePanel.tsx` | Remove `case "GamePanel":` in `SectionRouter.tsx` |

Also delete anything that read `GamePanelVariant` on the client side
— there is nothing left to read after `GamePanelData` is removed.

### C.5 Fixture + round-trip

1. Replace
   `ios/Tests/SduiCoreTests/Fixtures/game-detail-live.json` (and any
   other GamePanel-carrying fixtures) with the new composite shape
   produced by `buildGamePanelComposite`.
2. Add a `schema/examples/game-panel-composite.json` fixture
   exercising the three lifecycle states (pre-game, live-with-clock,
   final).
3. Update round-trip tests to decode the composite without the
   removed section type appearing anywhere.

### C.6 Analytics continuity

`buildGamePanelComposite` MUST preserve the existing analytics
identity of each emitter (`section.id`, `section.analyticsId`). The
composite envelope is the section envelope, not a new `AtomicComposite`
wrapping a section — the section type moves from `"GamePanel"` to
`"AtomicComposite"`, but the envelope fields downstream systems key
off do not change.

---

## Phase D — Docs

### D.1 AGENTS.md Rule 15

- Remove the `GamePanel` row from the permanent-sections table.
- Update the rationale paragraph beneath it: the list drops to eight
  permanent sections. Note that clock/score ticking moved to the
  `LiveClock` primitive rather than a section concern.

### D.2 Client implementors' contract

Add a `LiveClock` entry under the atomic-primitive catalogue in
[docs/client-implementors-contract.md](../client-implementors-contract.md):

- Fields (mirroring schema).
- Tick-loop algorithm (pseudo-code for the snapshot + elapsed +
  clamp derivation).
- Platform-realization table (iOS `TimelineView`, Compose
  `produceState`, web `requestAnimationFrame`).
- Lifecycle / pause semantics (off-screen, backgrounded, tab
  hidden).
- Accessibility cadence rule (no per-frame announcements).

Add conformance checklist items:

- `C17. LiveClock: new-in-this-plan atomic primitive. Clients MUST
  implement a platform-native tick loop when isRunning == true, MUST
  clamp at stopAtSeconds, MUST NOT drive accessibility announcements
  from the tick.`

### D.3 Design system catalogue

Update [docs/sdui-design-system.md](../sdui-design-system.md):

- `TextVariant`: remove the legacy-alias rows; document the M3 scale
  + `score` as the canonical set. Add a one-paragraph migration note
  naming the removed aliases.
- `ContainerVariant`: reduce to `hero` + `grouped`. Add a note that
  inline chrome (`background` + `cornerRadius` + `padding` + `shadow`)
  carries the rest.
- `ImageVariant`: reduce to `thumbnail`.
- `SelectVariant`: reduce to `dropdown` + `chips`.
- Add a new section for `LiveClock`: explain it's a primitive, not a
  variant, and link to the client-implementors' contract entry.

### D.4 Related plan updates

- [docs/plans/ios-led-ux-rebuild.md](ios-led-ux-rebuild.md): mark
  the `SelectVariant.segmented` bullet as superseded by this plan.
- [docs/plans/sdui-refapp-implementation-plan.md](sdui-refapp-implementation-plan.md):
  note that GamePanel is no longer a section type. Any ref-app
  integration step that wired in a GamePanel section renderer is
  replaced by `AtomicComposite` support.

---

## Validation

1. `make codegen` clean after the schema edits.
2. `make ios-build`, `make ios-test`, `make dev-server`, `make dev-web`,
   `make dev-android` all green.
3. `curl http://localhost:8080/sdui/for-you` and
   `curl http://localhost:8080/sdui/game-detail/<id>` return
   composites with a `LiveClock` leaf during a live game; with the
   center column collapsed to a `Text` only for final games.
4. iOS `SduiDemo`: open `/sdui/for-you`, confirm carousel renders;
   open a live game, confirm the clock ticks when `isRunning == true`,
   holds when `false`, and snaps to each Ably-delivered snapshot
   without flicker.
5. Android demo and web dev client: same confirmation.
6. `ios/Tests/SduiCoreTests` round-trip suite passes against the
   new fixture shapes.
7. Accessibility smoke: enable VoiceOver on iOS, confirm the clock
   is announced on Ably-update cadence, not per tick.

## Risk register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| An emission site currently depends on a pruned variant value (e.g. a layout fixture references `ImageVariant.hero` that we haven't audited) | Medium | Low | Codegen + round-trip tests fail loudly on the first compile — strict decoders catch unknown values. Run locally before push. |
| Ably `dataBindings` paths that used to target `section.data.clockRunning` on the envelope now need to target a leaf `isRunning` field inside the composite tree | High | Medium | Explicit part of C.2: each emitter's binding paths are rewritten together with the composite shape. Document the before/after path mapping in the migration commit. |
| `TimelineView` / `produceState` / `rAF` implementation drifts across the three platforms, producing visibly different tick cadences | Low | Low | Accept the divergence per Rule 18 — each platform realizes natively. Document expected cadence bands (≥ 5Hz, ≤ 30Hz) in the client-implementors' contract; not a regression if one platform is 10Hz and another 15Hz. |
| Accessibility announcement cadence regresses (screen reader reads every tick) | Medium | Medium | Explicit B.6 requirement — do not wire `updatesFrequently` / `liveRegion` / `aria-live` from the tick. Include a manual VoiceOver / TalkBack / screen-reader check in validation. |
| Composite exceeds the Rule 15 caps (50 nodes / depth 6) for some game-state combination | Low | High | Count nodes in the worst-case shape (live game with clock + all optional rows) during C.1 implementation; server-side validator catches it at compose time if we miss. |
| `featured` treatment, now inline, diverges subtly from the prior variant realization on one platform | Medium | Low | Snapshot the variant's resolved values on each platform *before* the migration; emit those exact numeric values inline for the first iteration. Iterate visual tweaks separately after the migration lands. |
| Ref-app integration currently in flight assumes `GamePanel` as a section type | Low | Medium | Callout in D.4. The ref-app integration plan is not on the critical path of this change; it adjusts after this lands. |
| `AtomicComposite` caching / measurement path has a bug that doesn't trigger on today's simpler composites but does on GamePanel's shape | Low | Medium | Add the live-game composite fixture to round-trip tests before migrating emission sites. Any decoding/rendering bug surfaces in the test suite, not in prod. |

---

## Todos

1. **prune-text-variants** — Phase A.1: migrate every server emission site off `heading1/2/3`, `body`, `caption`, `label` to M3 equivalents; remove enum values + resolver branches on all three clients.
2. **prune-container-variants** — Phase A.2: migrate `buildPromoBanner` to inline chrome; remove `elevated/subtle/overlay/banner` enum values, helper methods, and resolver branches.
3. **prune-image-variants** — Phase A.3: migrate `buildHeroPanel` to inline image props; remove `hero/logo` enum values, helpers, and resolver branches.
4. **prune-select-variant** — Phase A.4: remove `SelectVariant.segmented` from schema + all three client Form renderers.
5. **codegen-prune** — Phase A.5: `make codegen`; update fixtures; round-trip green.
6. **liveclock-schema** — Phase B.1: add `LiveClock` to `AtomicElement.type` enum + six new fields with oneOf gating.
7. **liveclock-codegen** — Phase B.2: run codegen; verify generated models on all three platforms.
8. **liveclock-ios** — Phase B.3: implement `AtomicLiveClockView` with `TimelineView`; dispatch from router.
9. **liveclock-android** — Phase B.4: implement `AtomicLiveClock` with `produceState`; dispatch from router.
10. **liveclock-web** — Phase B.5: implement `AtomicLiveClock` with `rAF` + `visibilitychange`; dispatch from router.
11. **liveclock-accessibility** — Phase B.6: confirm no per-tick announcement regression on all three platforms.
12. **liveclock-fixture** — Phase B.7: `schema/examples/live-clock-leaf.json` + round-trip coverage.
13. **gamepanel-composite-builder** — Phase C.1: `AtomicCompositeBuilder.buildGamePanelComposite(...)`; verify node / depth caps.
14. **gamepanel-migrate-emitters** — Phase C.2: migrate every GamePanel emitter (ForYou / Schedule / Live / GameDetail / Demo / Utils) to the new helper; rewrite `dataBindings` paths.
15. **gamepanel-schema-cleanup** — Phase C.3: remove `GamePanel` from `sectionType`, `GamePanelData`, `GamePanelVariant`, oneOf reference.
16. **gamepanel-client-cleanup** — Phase C.4: delete `GamePanelView.swift`, `GamePanelRenderer.kt`, `GamePanel.tsx` and their SectionRouter branches.
17. **gamepanel-fixture** — Phase C.5: replace GamePanel-carrying fixtures with composite shape; add `schema/examples/game-panel-composite.json`.
18. **gamepanel-analytics-check** — Phase C.6: diff `section.id` / `analyticsId` before/after for each emitter.
19. **docs-agents** — Phase D.1: update AGENTS.md Rule 15.
20. **docs-contract** — Phase D.2: `docs/client-implementors-contract.md` LiveClock entry + C17 conformance.
21. **docs-design-system** — Phase D.3: `docs/sdui-design-system.md` variant catalogue + LiveClock entry.
22. **docs-related-plans** — Phase D.4: update `ios-led-ux-rebuild.md` and `sdui-refapp-implementation-plan.md`.
23. **validation** — Phase Validation: make targets green; curl + demo-app smoke tests on all three platforms; VoiceOver/TalkBack cadence check.
