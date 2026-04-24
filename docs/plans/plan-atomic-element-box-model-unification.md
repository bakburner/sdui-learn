# Plan: Unified `AtomicElement` Box Model at the Router

> **Status**: Shipped across web, iOS, and Android. Phase 0 resolver
> refactor, the three platform phases (§ Phase 1–3), and the § Phase 4
> closeout are complete. Residual work, if any, is captured under §
> Follow-ups at the bottom of this plan.
>
> **Scope**: All three client platforms (iOS, Android, Web). No schema change
> and no server-side change.

## Summary

Today, `element.padding` is honoured through two parallel code paths on every
client:

1. **Container-like primitives** (`Container`, `ScrollContainer`,
   `Conditional`, `SectionSlot`) apply `element.padding` **internally**, so
   the element's own `background` / `cornerRadius` / `shadow` clip _outside_
   the padded frame — padding lives inside the element's visual chrome.
2. **Leaf primitives** (`Text`, `Image`, `Button`, `Spacer`, `Divider`,
   `DisplayGrid`) have `element.padding` applied at the **router** via a
   wrapper (SwiftUI modifier, Compose `Modifier.padding`, or a wrapping
   `<div>`) — there is no chrome to sit inside, so the wrapper is purely
   additive.

This plan proposes collapsing those two paths into a **single router-owned
box model** that applies the full `margin → background+corner → padding →
content` stack uniformly for every element type. The element renderer would
shrink to just rendering its content.

This is **not a drive-by fix**. It changes the `background` / `cornerRadius`
/ `shadow` clip semantics of every `Container` in the system. It needs its
own PR, its own screenshot diff pass per platform, and its own rollout.

## Why it matters

Three things drive this work:

1. **Schema-runtime drift is only half-closed.** The April fix made every
   leaf honour `element.padding`, but it did so by adding a wrapper at the
   router for leaves only. The semantic inconsistency between "padding is
   inside the container's bg" and "padding is inside a no-op wrapper around
   the leaf" is now codified in the router, not fixed.
2. **New leaf properties will replicate the same drift.** The moment a leaf
   primitive grows a `background`, `cornerRadius`, or `shadow` property
   (`Image` already has `cornerRadius` handled internally, `DisplayGrid` is
   a likely candidate for row bg), we'll be back to a per-renderer
   duplication of the container box-model logic. Centralising it once is
   cheaper than replaying the fix N times.
3. **Reviewing "did this renderer correctly honour margin + opacity +
   padding + bg + cornerRadius + shadow + badge + actions?" is O(N primitives
   × M platforms).** Unifying it reduces the audit surface to one router
   implementation per platform plus a flat list of content renderers.

## Current state

| Primitive          | iOS                                | Android                                                      | Web                                           |
|--------------------|------------------------------------|--------------------------------------------------------------|-----------------------------------------------|
| `Container`        | internal `.padding` inside bg/corner | `AtomicContainer` applies its own `Modifier.padding` + bg  | `AtomicContainer` applies internal `padding`  |
| `ScrollContainer`  | internal `.padding` on inner stack | internal                                                     | internal                                      |
| `Conditional`      | internal (forwards to child)       | internal                                                     | internal                                      |
| `SectionSlot`      | internal                           | internal                                                     | internal                                      |
| `Text`             | router `.padding(edgeInsets(...))`   | router `.padding(...)` in `leafPaddedModifier`             | router `<div style={paddingStyle}>` wrapper   |
| `Image`            | router `.padding`                  | router `leafPaddedModifier`                                  | router `<div>`                                |
| `Button`           | router `.padding`                  | router `leafPaddedModifier`                                  | router `<div>`                                |
| `Divider`          | router `.padding`                  | router `leafPaddedModifier`                                  | router `<div>`                                |
| `Spacer`           | router `.padding`                  | router `leafPaddedModifier`                                  | router `<div>`                                |
| `DisplayGrid`      | internal `.padding` on `gridBody`    | router `leafPaddedModifier`                                  | router `<div>`                                |

Notes:

- iOS `DisplayGrid` is an outlier — it applies padding internally rather than
  at the router, because the grid currently wants to size its own rows against
  the padded frame. That inconsistency would be resolved by unification.
- iOS `AtomicContainerView` applies `.padding` on the `Group { HStack/VStack }`
  _before_ `.applyContainerVariant`, so the background paints the padded
  frame. This "bg extends to the padding edge" is the load-bearing semantic
  that unification must preserve.

## Problem statement

Concretely, the routers currently contain a hard-coded primitive allowlist
that determines whether padding is applied at the router or deferred to the
renderer:

```64:74:android/sdui-core/src/main/java/com/nba/sdui/core/renderer/atomic/AtomicRouter.kt
    val leafPaddedModifier = when (element.type) {
        "Container", "ScrollContainer", "Conditional", "SectionSlot" -> opacityModifier
        else -> element.padding?.let {
            opacityModifier.padding(
                start = (it.start ?: 0L).toInt().dp,
                end = (it.end ?: 0L).toInt().dp,
                top = (it.top ?: 0L).toInt().dp,
                bottom = (it.bottom ?: 0L).toInt().dp
            )
        } ?: opacityModifier
    }
```

```82:95:web/src/components/atomic/AtomicRouter.tsx
  const isLeaf = element.type !== 'Container'
    && element.type !== 'ScrollContainer'
    && element.type !== 'Conditional'
    && element.type !== 'SectionSlot';
  if (isLeaf && element.padding) {
    const p = element.padding;
    const paddingStyle: React.CSSProperties = {
      paddingTop: p.top,
      paddingRight: p.end,
      paddingBottom: p.bottom,
      paddingLeft: p.start,
    };
    rendered = <div style={paddingStyle}>{rendered}</div>;
  }
```

Any new container-like primitive (for example a future `Stack`,
`ConstraintLayout`, or `StateDrivenContainer`) must be added to that
allowlist on every client or it silently ends up double-padded.

## Proposed design

### The unified box model

Every `AtomicElement` is rendered through the following router-owned stack
(outer → inner):

```
margin           ← sibling-to-sibling spacing, already router-applied today
│
├─ shadow        ← applied outside bg so the shadow renders on the bg shape
│
└─ bg + cornerRadius + border
   │
   └─ padding    ← interior padding
      │
      └─ content ← whatever the element renders
```

Semantic contract:

- `background`, `cornerRadius`, `shadow`, `border` paint / clip the box that
  **includes** `padding` but **excludes** `margin`.
- `padding` sits **inside** the bg/corner clip.
- Content is positioned **inside** padding.
- `margin` sits **outside** everything.

This matches the current `Container` semantic and matches CSS box-model
intuition. Leaves gain the full box model for free; they just rarely use it
today.

### Option analysis

**Option A — Router owns the full box model, renderers render only content.**

- Router applies `margin → shadow → bg+corner → padding → opacity` uniformly
  for every `AtomicElement.type`.
- `AtomicContainerView` / `AtomicContainer.kt` / `AtomicContainer.tsx` stop
  calling their `applyContainerVariant` equivalents. They just lay out
  children in an `HStack` / `VStack` / flex row/column.
- Leaves lose their per-primitive padding hook; router already handles it.
- **Pro**: single audit surface; adding a new primitive requires zero
  box-model thought; the "drift" class of bug is designed away.
- **Con**: invasive refactor on every platform; needs screenshot coverage on
  every section that renders a Container with bg/cornerRadius to catch clip
  regressions.
- **Con**: container **variant** resolution (ADR-013) currently happens
  inside `AtomicContainerView` via `ContainerVariantResolver`. Variants
  _are_ box-model semantics (they emit inline `background` / `cornerRadius`
  / `shadow` hints), so variant resolution would need to move to a shared
  router step or to an "expand `element.variant` into concrete inline
  props" pre-pass. See "Open questions" below.

**Option B — Keep dual path, document the semantics explicitly.**

- Accept the router-vs-renderer split. Document in `AGENTS.md` Rule 14 (or
  a new rule) that container-like primitives own their interior padding
  because of the bg/corner-clip semantic.
- Add a contributor note next to the router allowlist reminding whoever
  adds a new primitive to register it.
- **Pro**: zero behavioural risk.
- **Con**: punts the audit surface permanently.

**Option C — Introduce a shared box-model renderer, wrap the current ones.**

- New `AtomicBox` / `AtomicBoxModifier` on each platform that applies the
  unified stack. Element renderers continue to exist but internally wrap
  their content with `AtomicBox`.
- Router calls element renderer as today; element renderer delegates
  box-model application to `AtomicBox`.
- **Pro**: smaller per-platform diff than Option A; element renderers don't
  need to be rewritten.
- **Con**: two code paths still exist (the router's wrapper and the
  element-local `AtomicBox` call) — doesn't actually unify.

**Recommendation**: **Option A**, staged.

Option B is a retreat. Option C adds a layer without removing one. Option A
is the only one that delivers the single audit surface that motivates this
plan.

Stage it as:

1. Land Option A on **web first** (lowest-risk renderer, best
   screenshot/test coverage, easy to roll back).
2. Land on **iOS**.
3. Land on **Android**.

Each platform lands in its own PR so regressions are localised.

## Tasks

### Phase 0 — Shared prework (before any platform PRs)

- [x] Write a fixture that exercises `element.padding` on every leaf type
      and on `Container` with bg+cornerRadius+shadow. Added as
      `schema/examples/box-model-leaves.json`; the drift round-trip
      test (`ios/Tests/SduiCoreTests/SduiModelsRoundTripTests.swift`,
      web `vitest` suite) exercises it on every platform.
- [x] Factor `ContainerVariantResolver` on each platform so it returns a
      "resolved inline props" bag (`{ background, cornerRadius, shadow,
      border, gradient overlay, fillWidth, overrideMatrix }`) rather
      than a SwiftUI/Compose/React modifier. The router then reads
      that bag like any other element's inline props. Shipped:
      - iOS: `ContainerVariantSpec` + surfaceColor(for:) + the
        `applyContainerVariant` ViewModifier that consumes it.
      - Android: `ContainerVariantSpec` data class consumed directly
        by `AtomicBox.kt`.
      - Web: `ContainerVariantSpec` TS interface consumed by
        `AtomicBox.tsx`.
- [ ] Screenshot-baseline fixtures for composers that emit `Container`
      with `background` / `cornerRadius` / `shadow` — deferred;
      per-platform snapshot / preview coverage was relied on instead,
      see platform phases. Revisit if we add more tiers to the variant
      catalog.

### Phase 1 — Web (first platform) — done

- [x] Introduced `AtomicBox` helper in
      `web/src/components/atomic/AtomicBox.tsx` that takes the resolved
      box-model props (margin, bg, cornerRadius, shadow, border,
      padding, opacity, fillWidth, variant chrome, badge) and wraps
      children.
- [x] `AtomicRouter.tsx` reduced to a pure dispatcher — no padding,
      margin, or opacity handling. Every primitive routes its content
      through `AtomicBox`.
- [x] Leaf-skip allowlist removed; router no longer wraps leaves with a
      padding `<div>`.
- [x] `AtomicContainer.tsx`, `AtomicScrollContainer.tsx`,
      `AtomicDisplayGrid.tsx`, `AtomicConditional.tsx`,
      `AtomicSectionSlot.tsx` stripped of local box-model handling;
      they only own layout semantics. `AtomicSpacer.tsx` bypasses
      `AtomicBox` (pure layout).
- [x] Vitest suite passes including the new
      `box-model-leaves.json` fixture.

### Phase 2 — iOS — done

- [x] Introduced `AtomicBoxModifier` in
      `ios/Sources/SduiCore/Rendering/Atomic/AtomicBoxModifier.swift`
      applying the unified stack
      (`padding → frame → applyContainerVariant → badge → opacity → padding(margin)`).
- [x] `AtomicRouter.swift` reduced to a pure dispatcher. Every
      primitive view ends its chain with
      `.atomicBox(element, screenState:, onAction:)`.
- [x] `.applyContainerVariant` and internal padding removed from
      `AtomicContainerView.swift`, `AtomicScrollContainerView.swift`.
- [x] Per-leaf `.padding(edgeInsets(from: element.padding))` calls in
      `AtomicTextView`, `AtomicImageView`, `AtomicButtonView`,
      `AtomicDividerView`, `AtomicDisplayGridView` removed — the
      modifier owns padding now.
- [x] SduiCore compile + test suite pass against iPhone 15 Pro Max
      simulator with `SDUI_DISABLE_ABLY=1`.

### Phase 3 — Android — done

- [x] Introduced `AtomicBox` composable + `Modifier.buildAtomicBox`
      extension in
      `android/sdui-core/src/.../atomic/AtomicBox.kt` that composes
      the unified stack in Compose-idiomatic order
      (`padding(margin) → alpha → shadow(shape) → clip(shape) →
      background → gradientOverlay → border → padding(inner) →
      sizing`) and renders the optional badge overlay.
- [x] `AtomicRouter.kt` reduced to a pure dispatcher; the
      `leafPaddedModifier` allowlist is gone, no `Modifier` parameter
      is threaded.
- [x] Box-model handling removed from `AtomicContainer.kt`,
      `AtomicScrollContainer.kt`, `AtomicDisplayGrid.kt`,
      `AtomicText.kt`, `AtomicImage.kt`, `AtomicButton.kt`,
      `AtomicDivider.kt`. `AtomicConditional.kt` / `AtomicSectionSlot.kt`
      / `AtomicSpacer.kt` bypass the box model by design
      (pure-layout / pure-delegation primitives).
- [x] `parseColor` / `resolveAxis` / `resolveSurfaceRole` retained as
      `internal` helpers in `AtomicContainer.kt` and reused by
      `AtomicBox.kt` + `GamePanelRenderer.kt`.

### Phase 4 — Closeout — done

- [x] Deleted the "Container / ScrollContainer / Conditional /
      SectionSlot apply padding internally" comments on all three
      routers (they were obsolete once the router became a pure
      dispatcher).
- [x] `docs/client-implementors-contract.md` §4 + new §4a document the
      unified box model and its canonical modifier order; the
      Conformance Checklist gained row `C14a`.
- [ ] `docs/sdui-design-system.md` sweep — deferred to the next
      docs-audit pass; the design-system doc currently describes
      per-primitive box model at a conceptual level and is not
      contradicted by the unified implementation.

## Risks & migration concerns

- **Clip regressions on Container.** The SwiftUI / Compose / CSS modifier
  orders that produce "bg extends to padding edge" are subtly different on
  each platform; moving them behind a shared `AtomicBox` requires each
  platform to verify the modifier order it generates is equivalent to the
  one it currently inlines. Screenshot fixtures from Phase 0 are the safety
  net.
- **Shadow stacking order.** On iOS, `.shadow` after `.cornerRadius` on a
  background-painted view produces the shape-hugging shadow we want;
  applied before `.background` it renders under the content. The unified
  modifier must pick the right order. Same consideration on Android with
  `Modifier.shadow` / `graphicsLayer`, and on web with `box-shadow`
  interaction with `border-radius` and `overflow`.
- **`element.variant` semantics.** Today, variants resolve to a
  platform-native bundle of view modifiers (`.ultraThinMaterial`, Material
  tonal elevation, CSS `backdrop-filter`). To centralise box-model
  application, variants need to resolve to _data_ the router can apply
  (`{ background, cornerRadius, shadow, border }`), not directly to view
  modifiers. That's a non-trivial refactor on all three platforms and is
  the single largest risk in this plan. It may need its own precursor plan.
- **Behavioural drift where a renderer currently "cheats".** Anywhere a
  renderer applies padding / bg / corner outside the box model (e.g.,
  `AtomicImageView` applying its own `cornerRadius` to clip the image
  before the container's corner clip takes over), the unified box model
  will render differently. Each such site must be caught in the audit.
- **`DisplayGrid` on iOS currently applies padding internally**, diverging
  from the other clients. Phase 2 is where that gets normalised.
- **Pull-to-refresh / scroll containers.** Verify the unified box model
  doesn't change `ScrollContainer` scroll-indicator inset behaviour —
  putting padding on the outside of the scroll view is not the same as
  content insets inside it. This may force `ScrollContainer` to keep a
  special-case handling.

## Open questions

1. Does the unified box model force variant resolution to become "inline
   props" on every platform, or can the router accept a "variant modifier"
   bundle that the renderer passes through? The latter keeps per-platform
   variant realization (ADR-013) intact but reintroduces a code path the
   router can't fully inspect.
2. Should this plan also fold `element.badge` (the overlay badge
   application modifier on iOS) into the box model? It currently lives in
   `AtomicContainerView`'s modifier chain. Leaving it out preserves the
   current scope; folding it in completes the symmetry but widens the
   audit.
3. On web, does moving the bg to a router-level `<div>` wrapper change how
   CSS cascades into child elements (especially `color` inheritance for
   `color: 'inherit'` siblings)? Needs verification before Phase 1 lands.
4. Do we want a feature flag / A-B per platform to land this under
   (default-off, flip on after screenshot sign-off), or do we land it
   straight? Given the visual-risk surface, a flag is the safer play but
   adds cleanup work.

## Out of scope

- Schema changes — `element.padding`, `element.margin`, `element.background`,
  etc. already exist on every `AtomicElement`. This is purely a client-side
  implementation refactor.
- Server composer changes — composers continue to emit the same JSON.
- Adding new leaf-level properties (e.g., `Text.background`) — that's a
  separate proposal and should wait until this refactor lands so it lands
  into a clean box model.
