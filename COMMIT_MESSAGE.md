refactor: unify AtomicElement box model via a single AtomicBox per client

## Summary

Collapses the two parallel `element.padding` / `element.margin` / `element.background`
code paths (router-applied for leaves, renderer-applied for containers) into a single
`AtomicBox` helper on every client. The router becomes a pure dispatcher; every
primitive routes its content through `AtomicBox`, which applies the full box model in
one canonical order:

    margin → opacity → shadow → corner-clip → background → gradient overlay →
    border → padding → sizing (width / height / fillWidth) → badge overlay

Primitives keep only content concerns — flex layout, typography, image scaling,
scroll behaviour, grid cells. The `leafPaddedModifier` allowlist and the dual
"bg-inside-padding vs bg-inside-leaf-wrapper" semantic are gone.

## Motivation

- `element.padding` was honoured through two code paths per client; any new leaf
  property (`Text.background`, row bg on `DisplayGrid`, …) would replicate the
  drift. Centralizing once is cheaper than replaying the fix N primitives × M
  platforms times.
- New atomic schema fields and new variant values now flip to a single audit
  surface per platform (`AtomicBox.kt` / `AtomicBoxModifier.swift` /
  `AtomicBox.tsx`) rather than re-implementation in each primitive. Client
  release cost drops from N × M to N + M.
- Renderer → presentation-only boundary gets sharper: primitives cannot
  accidentally diverge on margin / padding / bg / corner / shadow behaviour
  because they no longer own the code that applies any of it.

## Android (`android/sdui-core/.../atomic/`)

- New `AtomicBox.kt`: `AtomicBox(element, screenState, onAction) { boxModifier -> … }`
  wrapper plus `Modifier.buildAtomicBox(element)` extension. Consumes
  `ContainerVariantResolver` spec data (no ViewModifier), merges with inline
  props via `overrideMatrix`, renders the optional badge overlay.
- `AtomicRouter.kt`: stripped to a pure dispatcher. Removed `leafPaddedModifier`
  allowlist, removed the threaded `Modifier` parameter, removed outer
  `margin`/`opacity` application (now in `AtomicBox`).
- `AtomicContainer.kt`: owns only flex layout (direction, gap, alignment,
  cross-alignment, breakpoint, child flex weights). Shared helpers `parseColor`,
  `resolveAxis`, `resolveSurfaceRole` retained as `internal` and reused by
  `AtomicBox.kt` + `GamePanelRenderer.kt`.
- `AtomicText.kt` / `AtomicImage.kt` / `AtomicButton.kt` / `AtomicDivider.kt` /
  `AtomicDisplayGrid.kt` / `AtomicScrollContainer.kt`: wrapped in
  `AtomicBox { boxModifier -> … }`; local padding / cornerRadius / bg handling
  removed.
- `AtomicSpacer.kt` / `AtomicConditional.kt` / `AtomicSectionSlot.kt`: bypass
  `AtomicBox` by design (pure-layout / pure-delegation primitives — chrome
  belongs to the chosen child or hosted section, matches web/iOS).

## iOS (`ios/Sources/SduiCore/Rendering/Atomic/`)

- New `AtomicBoxModifier.swift`: `ViewModifier` applying the canonical stack
  via `.padding(inner) → .frame(…) → .applyContainerVariant(spec) →
  .applyBadge(…) → .opacity → .padding(margin)`. `ContainerVariantResolver`
  continues to return a `ContainerVariantSpec` data bag (unchanged from prior
  Phase 0 refactor); the modifier is the only consumer.
- `AtomicRouter.swift`: pure dispatcher. Every primitive view now ends with
  `.atomicBox(element, screenState:, onAction:)`.
- `AtomicContainerView.swift` / `AtomicScrollContainerView.swift`: removed
  internal `.applyContainerVariant` + `.padding` calls.
- `AtomicTextView.swift` / `AtomicImageView.swift` / `AtomicButtonView.swift` /
  `AtomicDividerView.swift` / `AtomicDisplayGridView.swift`: removed per-leaf
  `.padding(edgeInsets(from: element.padding))` calls.
- Accessibility labels + action triggers remain primitive-specific (each
  renderer supplies its own semantic fallback: image `alt`, button `label`,
  text `content`).

## Web (`web/src/components/atomic/`)

- New `AtomicBox.tsx`: React wrapper that builds the full CSS box model
  (margin via outer `<div>`, `box-shadow` + `border-radius` + `background` +
  `backdrop-filter` on the chrome layer, padding inside the clip,
  `width`/`height`/`fillWidth` on the sized frame, positioned badge overlay).
  Consumes `resolveContainerVariant` spec data.
- `AtomicRouter.tsx`: pure dispatcher. Leaf-skip allowlist and wrapper-`<div>`
  padding are gone.
- `AtomicContainer.tsx` / `AtomicScrollContainer.tsx` / `AtomicDisplayGrid.tsx`
  / `AtomicText.tsx` / `AtomicImage.tsx` / `AtomicButton.tsx` /
  `AtomicDivider.tsx`: wrapped in `<AtomicBox …>`; layout-owning primitives
  pass their flex / scroll CSS as `layoutStyle`.
- `AtomicSpacer.tsx`: bypasses `AtomicBox` (pure spacing primitive).

## Variant resolvers (`ContainerVariantResolver` per platform)

- All three platforms already expose a data-only `ContainerVariantSpec`
  (cornerRadius, background role / CSS, shadow, border, gradient overlay,
  fillWidth, override matrix). `AtomicBox` / `AtomicBoxModifier` is now the
  sole consumer.
- Merge semantics: inline `element.*` wins on `allow` axes; variant default
  wins on `lock` axes and the inline attempt is logged via
  `variant_override_blocked`.

## Fixture & drift tests

- New `schema/examples/box-model-leaves.json` (plus iOS test copy under
  `ios/Tests/SduiCoreTests/Fixtures/`): screen-shaped fixture exercising
  `padding` / `margin` / `background` / `cornerRadius` on every leaf type
  (`Text`, `Image`, `Button`, `Divider`, `DisplayGrid`, `Spacer`) and on
  `Container` + `ScrollContainer` with bg + cornerRadius + shadow.
- Web `vitest` round-trip suite + iOS `SduiModelsRoundTripTests` pick it up
  automatically via the drift harness.

## Docs

- `docs/client-implementors-contract.md` §4 rewritten: router is a pure
  dispatcher; new §4a documents `AtomicBox` canonical modifier order, variant
  integration, badge handling, and the deliberate exclusions (accessibility,
  actions, flex layout on Container). Conformance Checklist gains row `C14a`.
- `docs/plans/plan-atomic-element-box-model-unification.md` marked Shipped
  with per-phase task checkboxes resolved; deferrals (screenshot-baseline
  fixtures, `sdui-design-system.md` sweep) called out explicitly.

## Client-release-cadence impact

Work that no longer requires a client release:

- Padding / margin / bg / cornerRadius / shadow / border / opacity / fillWidth
  / badge added to any atomic primitive in a composer — server-only change.
- Styling a previously un-styled primitive (e.g. `Text` with a bg + radius) —
  already works without touching renderers.
- Adjusting a variant's platform-native realization on one platform — single
  file, no wire-contract impact.

Work that still requires a client release, now cheaper:

- A new variant value that must render correctly on first emission — one file
  per platform (`ContainerVariantResolver`), not every primitive.
- A new box-model inline prop (e.g. `element.outline`, `element.elevation`) —
  one file per platform (`AtomicBox`). Previously this would have been N
  primitives × M platforms.
- A new atomic element type — router + one primitive per platform; the box
  model is free.

## Files

Added:
- `android/sdui-core/src/main/java/com/nba/sdui/core/renderer/atomic/AtomicBox.kt`
- `ios/Sources/SduiCore/Rendering/Atomic/AtomicBoxModifier.swift`
- `web/src/components/atomic/AtomicBox.tsx`
- `ios/Tests/SduiCoreTests/Fixtures/box-model-leaves.json`

Modified (all three platforms):
- `AtomicRouter`, `AtomicContainer`, `AtomicScrollContainer`, `AtomicText`,
  `AtomicImage`, `AtomicButton`, `AtomicDivider`, `AtomicDisplayGrid`,
  `AtomicSpacer`, `AtomicConditional`, `AtomicSectionSlot` / their iOS
  `*View` and web `*.tsx` equivalents.
- `schema/examples/box-model-leaves.json`
- `docs/client-implementors-contract.md`
- `docs/plans/plan-atomic-element-box-model-unification.md`
