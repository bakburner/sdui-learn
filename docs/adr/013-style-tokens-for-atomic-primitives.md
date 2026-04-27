# ADR-013: Style Tokens for Atomic Primitives

- Status: Accepted
- Date: 2026-04-20 (proposed); 2026-04-22 (accepted)
- Decision owners: Adrian Robinson (interim), platform leads, design systems lead
- Related requirements: `docs/sdui-requirements-summary.md` (design system alignment)
- Related ADRs: ADR-005 (Action Scope and Precedence — precedence vocabulary and
  the contrast that variants do **not** cascade parent→child the way actions
  do screen→section→subsection→atomic), ADR-008 (Layout Manager — `layoutHints`
  pattern precedent)

## Decision

Introduce **typed per-primitive variant enums** on atomic primitives
(`Container`, `Image`, `Divider`, and — already present — `Text`,
`Button`) as the carrier for design-system style tokens. A variant is
a named design-system preset that does **both** of the following:

> **Wire shape.** The wire property is uniformly `variant: string` on
> every atomic element. The per-primitive enum names (`TextVariant`,
> `ButtonVariant`, `ContainerVariant`, `ImageVariant`) document the
> vocabulary — which enum applies is determined by the element's
> `type`. A `Text` element's `variant` is parsed against `TextVariant`;
> a `Container`'s against `ContainerVariant`; etc. An unrecognized
> value logs a `variant_resolver_missing` diagnostic and renders the
> primitive's default.


1. **Aggregates inline-expressible properties** into a single named
   concept (a `card` carries one consistent padding + corner radius +
   background + shadow across every screen that uses it).
2. **Owns properties inline props cannot cleanly express** (gradients,
   platform materials, multi-layer shadows, press/focus state,
   theme-adaptive colors).

Inline style properties are retained as per-property overrides,
governed by a **per-variant override matrix** declared in the token
registry. Each visual axis is marked either `allow` (inline value
wins) or `lock` (inline value ignored with a client-side warning),
with optional per-platform granularity (an axis may be `lock` on iOS
while `allow` on Android and web). A non-normative token registry
at `schema/style-tokens.json` is the source-of-truth spec for
per-platform intent, override policy, OS-version tiering, and
dark-mode realization.

**Variants resolve to platform-native realizations.** A `heroCard`
is Liquid Glass on iOS 26+, `.ultraThinMaterial` on iOS 17–25,
Material 3 Expressive on Android 15+, Material You on Android
12–14, and a CSS surface mixin on the web. Cross-platform visual
divergence is expected and desirable — users expect an iOS app to
look like iOS and an Android app to look like Android. See
[Platform-native realization](#platform-native-realization) below.

## Context

Atomic primitives today carry styling as a flat collection of inline
properties: `color`, `background`, `padding`, `cornerRadius`, `shadow`,
`gap`, etc. This has served the prototype well but has four structural
limits:

1. **Semantic drift across composers.** Every card-shaped container
   is currently expressed as a fresh bag of padding + radius +
   background + shadow. Ten composers diverge subtly over time; there
   is no single source of truth for what "a card" is. Adding,
   renaming, or restyling a design-system concept requires hunting
   down every instance in server code.
2. **Expressive ceiling.** Gradients, multi-layer shadows, platform
   materials (iOS `.ultraThinMaterial`, Android `Surface` tonal
   elevation, web `backdrop-filter`), press/focus state styling, and
   shimmer animations cannot be expressed with the current atomic prop
   set without introducing a proliferation of nested object types.
3. **Theme adaptation is hostile.** `"color": "#FFFFFF"` is a hex
   literal baked into the payload. It does not adapt to dark mode,
   high-contrast, or reduced-transparency modes. The server cannot
   know the user's effective color scheme at compose time, and the
   client has no signal that a given literal was meant to be
   theme-aware.
4. **Design-engineering handoff friction.** Figma design tokens
   ("Card/Hero", "Surface/Subtle") do not round-trip cleanly into a
   bag of six inline properties per component. Cross-platform QA
   currently relies on comparing individual property values rather
   than verifying a named design-system concept.

The codebase already validates this pattern in two places:

- **`TextVariant`** (schema enum: `titleMedium`, `bodyLarge`, `score`,
  etc.) — each client owns the platform-native realization via a
  `font(for variant:)` lookup. Server emits the token; client resolves.
- **`ButtonVariant`** (`primary | secondary | tertiary | text`) —
  same pattern at a different primitive.

This ADR generalizes the pattern to the remaining atomic primitives
that own a visual surface. It treats aggregation and inexpressibles
as **co-equal drivers**, not as competing framings: both are first-class
reasons a variant may exist.

## Decision Drivers

- Centralize design-system concepts: a `card` should exist in one
  place (the token registry), not be duplicated across N composers.
- Preserve SDUI Rule 14 (renderers are presentation-only): variant ↔
  concrete-style is a pure client-side lookup, not business logic.
- Preserve Rule 13 (schema enums cover server output): per-primitive
  typed enums keep `Codable` / jsonschema2pojo decoders strict.
- Keep the server payload semantic ("this is a hero card"), not
  physical ("32px padding, 2-layer shadow, #FF6B6B → #D94A4A gradient").
- Enable dark-mode / theme adaptation without server knowledge of
  user environment.
- Align with existing Figma token taxonomy for design-engineering
  round-trip.
- Accept release-coupling as a governance property: new tokens
  require a client update, which is the intended forcing function
  for design-system discipline.
- **Platform-native realization is the premise, not a trade-off.**
  An iOS app is expected to use iOS idioms (platform materials,
  liquid glass on supported OS versions, native controls); an
  Android app is expected to use Material (tonal elevation, ripples,
  Material 3 Expressive where available); a web app is expected to
  look like web. A token named `card` resolves to whichever
  realization is idiomatic for the client — visual divergence across
  platforms is expected and desirable. This is the same premise as
  the existing `TextVariant` / `ButtonVariant` pattern, where
  `titleMedium` already resolves to distinct platform-native fonts,
  and where `primary` buttons already render as iOS-styled on iOS
  and Material-styled on Android. This ADR generalizes that premise
  to container surfaces.

## Options Considered

### Option A: Status quo — inline properties only

Pros:
- No schema churn.
- Server can express any styling the current prop set covers.

Cons:
- Expressive ceiling stays where it is. Gradients, materials, layered
  shadows require new nested schema types per concept.
- Theme adaptation remains impossible with hex literals.
- Design-system tokens have no first-class representation.
- Composers repeat the same 6-property bag for every card-shaped
  instance, diverging over time.

### Option B: Freeform `className: string` on every primitive

A single untyped string field that the client maps to a concrete
style preset, à la Tailwind or Bootstrap class names.

Pros:
- Maximum flexibility.
- One field covers every primitive.
- Familiar pattern from web.

Cons:
- Violates Rule 13: strict Codable / jsonschema2pojo decoders cannot
  validate against an open string field. Typos ship to production.
- No autocomplete in server composers.
- Name collisions across primitives (`"card"` on a Text means what?).
- Class definitions become schema-adjacent untyped docs, drifting
  from reality.

### Option C: Typed per-primitive variants, aggregation scope only

Variants exist solely to DRY up common property bags. A `card` token
expands to a fixed set of inline-expressible properties; the schema
stays at its current expressive ceiling.

Pros:
- Simple mental model (variant = named preset of known properties).
- Easier to govern (tokens are just fixed inline-prop bundles).
- Payload-size reduction without introducing new schema concepts.

Cons:
- Does not solve theme adaptation. Hex literals remain hex literals;
  only their point of definition moves from composer to token registry.
- Gradients, materials, multi-layer shadows, press state still require
  new nested schema types — the expressive-ceiling problem is
  unchanged.
- The most impactful use cases (dark-mode, brand-specific polish) never
  benefit.

### Option D: Typed per-primitive variants, full scope (chosen)

Each atomic primitive gets its own variant enum, following the
existing `TextVariant` / `ButtonVariant` precedent. Variants do
**both**:

- Aggregate inline-expressible properties into named presets.
- Own inexpressibles — the client's variant resolver can apply a
  gradient, wrap in a platform material, adapt to dark mode, attach
  a press interaction — none of which are inline-reachable.

Schema additions:

- **`ContainerVariant`** — e.g. `card`, `heroCard`, `subtleCard`,
  `inset`, `panel`, `surface`
- **`ImageVariant`** — e.g. `avatar`, `hero`, `thumbnail`, `logo`,
  `teamLogo`
- **`DividerVariant`** — e.g. `hairline`, `emphasized`, `inset`
  (add only if design owns ≥2 meaningful presets — see Open Questions)

Existing `TextVariant` and `ButtonVariant` remain unchanged.
`AtomicComposite` (a section type, not a primitive) does **not** gain
a variant — it is a host for an atomic tree, not a visual surface in
its own right.

Pros:
- Type-safe decoding across all three platforms.
- Autocomplete for server composers.
- No cross-primitive name collisions (impossible by construction).
- Extensible: adding `ImageVariant` later does not affect `Container`.
- Aligns with SwiftUI `ButtonStyle` / Compose `ButtonDefaults` idioms.
- Mirrors the pattern already validated for Text and Button.
- Captures both aggregation savings **and** expressive-ceiling relief
  in one mechanism.

Cons:
- Schema grows by N enums instead of 1 field.
- Adding a new variant requires client releases on every platform.
  (Intended governance property — see §Governance.)
- Override-proliferation risk (see §Failure Modes) requires explicit
  policy, not just good intentions.

## Decision Outcome

**Option D adopted (draft).** Typed per-primitive variant enums
serving as named design-system presets. Variants are first-class for
both aggregation (DRY design-system concepts) and inexpressibles
(gradients, materials, theme adaptation). Inline props remain as
per-property overrides governed by a per-variant override matrix.

### Precedence (variant + inline props)

Style precedence is defined **per visual axis**, gated by the variant's
declared override matrix. The resolution order for any given axis on
a single atomic primitive is:

1. **Platform default** — the baseline the client would render for the
   primitive with no variant and no inline props (lowest precedence).
2. **Variant value** — the registry's definition for this variant on
   this axis. Inexpressible axes (gradient, material, shadow stack,
   press state, theme bindings) end resolution here regardless of
   what inline props are present.
3. **Inline prop** — wins only when the variant's override matrix
   marks the axis as `allow` **for this platform**. The override
   matrix supports both scalar policies (`"lock"` / `"allow"` applies
   everywhere) and per-platform objects (`{ "ios": "lock", "android":
   "allow", "web": "allow" }`) — see [Registry shape](#registry-shape).
   When the axis is marked `lock`, the inline value is ignored and
   the client logs a `variant_override_blocked` warning (scoped to
   section + atomic path, surfaced in debug builds; folded into
   staleness / diagnostics metrics in release).
4. **Runtime theme resolution** — dark-mode / high-contrast / reduced
   transparency adjustments run last on whatever value won steps 2–3.
   The variant defines how it adapts; inline props, if they won step 3,
   are treated as literals that do not adapt (a known trade-off of
   using inline props — use variants for theme-aware surfaces).

This deliberately mirrors the ADR-005 vocabulary ("precedence") for
consistency, but the resolution shape is different: actions cascade
across **scopes** (screen → section → subsection → atomic), while
style precedence resolves **within a single atomic primitive** across
the sources listed above. See §No Parent-Child Cascade below.

Example:

```json
{
  "type": "Container",
  "variant": "heroCard",
  "padding": { "all": 32 }
}
```

- `padding` → `32` (axis marked `allow` in registry for `heroCard`).
- `background` inline: ignored on iOS (where `heroCard` marks
  `background: "lock"` to preserve liquid-glass / `.ultraThinMaterial`);
  honored on Android and web (where the matrix allows it). The iOS
  client logs `variant_override_blocked` for the blocked override.
- Liquid glass / material surface / gradient fill, 2-layer shadow,
  press animation, dark-mode resolution → from `heroCard`
  unconditionally, with OS-tier resolution applied per platform.

### No parent-child cascade

Unlike actions (ADR-005), **style variants do not propagate from a
parent atomic primitive down to its children**. A `Container` with
`variant: "heroCard"` does not tint, size, or otherwise style
the `Text` nodes inside it. Each atomic primitive resolves its own
style independently from its own variant enum (or the absence of one).

Rationale:

- Per-primitive variant enums (`ContainerVariant` vs. `TextVariant`)
  are already type-disjoint — there is no cross-primitive meaning for
  "inherit the parent's card-ness."
- Cascading would leak visual semantics across primitive boundaries
  and make renderers stateful about ancestor context, violating
  Rule 14 (renderers are presentation-only).
- Design-system tokens that need coordinated parent-child styling
  (e.g. "card with a specific title color") are expressed by pairing
  a `Container` variant with a `Text` variant at composition time on
  the server, not by runtime cascade.

The contrast with action precedence (which *does* cascade) is
deliberate: actions are a behavior model where inherited defaults
are useful; styles are a presentation model where cascading creates
opaque coupling that is hard to audit.

### Platform-native realization

Each variant resolves to a **platform-native realization** at render
time. The registry describes *intent* per platform; the client
resolver owns the concrete mapping, including OS-version tiering.

- **iOS** variants use SwiftUI materials, platform shapes, and
  native animation curves. On iOS 26+ a `heroCard` may use the
  Liquid Glass effect; on iOS 17–25 it falls back to
  `.ultraThinMaterial` with a gradient overlay; on older OSes it
  falls back to a solid gradient. The variant name is stable across
  tiers.
- **Android** variants use Material surfaces and tokens. On
  Android 15+ with Material 3 Expressive, a `heroCard` may lean
  into larger tonal elevation and expressive shapes; on Material
  You (Android 12–14) it uses the standard Material 3 surface
  tokens; on older OSes it falls back to a solid Material surface.
- **Web** variants use CSS surface mixins with `backdrop-filter`
  feature detection, gracefully degrading to solid backgrounds
  where the client lacks support.

Consequences:

- The registry's `ios` / `android` / `web` fields are **intent
  statements** keyed by OS-version tier (see [Registry shape](#registry-shape)
  below), not a single pixel spec. Each tier is a separate contract
  between the design-system team and that platform's client team.
- Figma frames document design *intent* per platform, not a per-
  pixel contract that clients must reproduce identically. A `card`
  token may look materially different on iOS vs. Android and both
  be correct.
- Cross-platform screenshot diffing is **not** a meaningful
  regression signal for variant output. Per-platform screenshot
  tests against that platform's design-system spec are.
- QA is per-platform: each client team owns visual review against
  its own platform's current design language. Cross-platform QA is
  reserved for *semantic* parity (the same token appears in the
  same composition positions with the same override matrix), not
  visual parity.
- This policy inherits from and reinforces Rule 14 (renderers are
  presentation-only): the platform-native realization lives in the
  renderer; the server has no opinion about `.ultraThinMaterial`
  vs. Material tonal elevation.

See also the pixel-parity escape hatch in [Open Questions](#open-questions)
for scenarios (brand campaigns, launch moments) where design does
want visual consistency across platforms.

### Background / gradient axis collision

Resolution: **treat `background` as a single visual axis**, whether
the underlying representation is a solid color, gradient, or material.
The variant's override matrix controls whether an inline `background`
is allowed to replace the variant's background definition in its
entirety. A variant that defines a gradient but marks `background`
as `allow` accepts inline `background: "#FFFFFF"` as a full
replacement (the gradient is dropped). A variant that marks it `lock`
rejects the override with a warning and renders its own gradient.

Rationale: splitting `background` into "solid-color axis" vs.
"gradient-or-material axis" would leak variant implementation detail
into the server composer's mental model. The registry is the right
place to express "this variant's background is off-limits."

## Evidence

- `TextVariant` (22 cases across Material3 + legacy + `score`) is the
  existing precedent. The iOS `AtomicTextView.font(for:)` switch,
  Android `AtomicText.mapTypographyVariant`, and web `variantStyles`
  lookup table all implement the exact pattern this ADR generalizes.
- `ButtonVariant` is the second precedent, at a different primitive.
- `schema/icon-tokens.json` + `IconTokenResolver` demonstrate the
  token-registry + client-resolver pattern working in production.
- Server composers currently emit ad-hoc property bags for
  visually-rich containers (see `HeroPanel` composition in
  `GameDetailComposer.java` — multiple `put("color", "#...")`,
  `put("cornerRadius", 16)` calls that a `heroCard` variant would
  consolidate).

### Cross-platform differences / risks

- **Platform capability asymmetry.** A variant resolves to iOS
  materials on iOS, Material surfaces on Android, and CSS mixins on
  web — by design (see [Platform-native realization](#platform-native-realization)).
  These realizations are not expected to be pixel-equivalent. Governance
  requires the token registry to document each variant's **intent** and
  each platform's **realization per OS-version tier**, so semantic drift
  (a variant losing its design-system meaning on one platform) is
  surfaced at review, while visual divergence across platforms is
  accepted as normal.
- **OS-version tiering adds a QA axis.** Each platform × OS-tier pair
  is a separate realization that needs to be validated against that
  platform's design language at that OS version. The testing surface
  grows with tier count; in practice, design owns "current" and
  "one back" on each platform as the tested matrix, with older OSes
  receiving a well-defined fallback rather than bespoke design work.
- **Gradient / material rendering nuances.** CSS `backdrop-filter`,
  Compose `Surface` tonal elevation, SwiftUI `.ultraThinMaterial`, and
  Liquid Glass have materially different rendering models. The registry
  captures the platform-idiomatic choice; per-platform screenshot
  tests (against that platform's spec, not against each other) catch
  regressions within a platform.
- **Dark-mode drift.** Because variants own theme adaptation, each
  platform × tier's implementation must explicitly provide a dark
  spec. Missing dark coverage is a governance issue, not a renderer
  bug.
- **Inline overrides can destroy platform idioms.** An inline
  `background: "#FFFFFF"` on a `heroCard` is idiomatic on web but
  would flatten the iOS liquid-glass effect. This is why the
  override matrix supports **per-platform granularity** (see
  [Registry shape](#registry-shape)): a variant may mark
  `background` as `lock` on iOS while leaving it `allow` on Android
  and web if that matches design intent.

### Operational impact

- **Schema size:** small (N enums, small initial case counts).
- **Client size:** per-platform lookup tables grow by ~20–40 LOC per
  new variant family. Bounded.
- **Payload size:** reduces for visually-rich containers (single
  token string replaces 4–6 inline properties) and stays flat for
  others.
- **Release cadence:** adding a new variant requires a client
  release. Expected cadence: a handful per quarter, batched.

## Failure Modes

These are specific to the variants-with-overrides model and motivate
the governance rules below.

1. **Override proliferation turns variants into labels.** If the
   typical instance is `variant: "card"` on a `Container` plus four
   overrides, the variant stops being a design-system concept and
   becomes a taxonomy field over the exact same bag of inline props.
2. **Almost-card invisible drift.** Ten composers each use `card`
   with slightly different overrides. Design refreshes `card` in the
   registry. Nine composers still render "a card" but each one
   diverged subtly from design intent in an invisible way.
3. **Background/gradient ambiguity.** Inline `background: "#FFFFFF"`
   on a variant whose background is a gradient is ambiguous without
   an explicit policy. Resolved above via the override matrix +
   single-axis treatment.
4. **Token rename / removal is a published-API break.** Old clients
   in the app-store long tail encounter unknown variants and fall
   back to defaults — usually ugly. Requires deprecation windows
   sized to app-update reality, not release-cycle convenience.
5. **New-variant sprawl.** Without a proof-of-need bar, the registry
   grows linearly with design opinions rather than with de-duplicated
   server patterns.

## Consequences

- **Short term:**
  - Schema adds `ContainerVariant` enum (+ any others started in the
    first slice).
  - Token registry file (`schema/style-tokens.json`) added as the
    source-of-truth spec for design/engineering alignment, including
    per-variant override matrices.
  - One composer migration serves as the pilot (candidate:
    `HeroPanel` in `GameDetailComposer`).
  - Client-side override-blocked warning plumbed into existing
    binding/staleness diagnostics.

- **Medium term:**
  - `ImageVariant` and `DividerVariant` added as design authors
    inventory reusable visual presets.
  - Dark-mode and high-contrast variants become a first-class client
    responsibility tied to each token, not an ad-hoc per-screen
    concern.
  - Figma ↔ SDUI token parity becomes an explicit governance
    artifact.
  - Quarterly override-pattern audit established.

- **Long term:**
  - Visual design changes at the token level propagate to every
    screen using the token via a single client release — no server
    composer changes required.
  - The token registry becomes the durable contract between design
    and platform engineering, reducing one class of cross-platform
    visual drift.

## Implementation Notes

### Rollout approach

1. Accept this ADR with a minimal initial token set
   (`ContainerVariant` with ~3 tokens: `card`, `heroCard`, `inset`).
2. Define `schema/style-tokens.json` registry shape (see example
   below), including per-variant override matrix.
3. Implement the three initial tokens on iOS, Android, and web,
   including dark-mode specs and override-blocked diagnostic.
4. Migrate one composer (`HeroPanel` in `GameDetailComposer`) to emit
   `variant: "heroCard"` on the hero `Container` and remove the inline
   property bag.
5. Capture before/after payload size and visual screenshots per
   platform as ADR acceptance evidence.

### Implementation status (2026-04-22)

The initial rollout landed with a smaller **wire** vocabulary than the
Option D examples above. The authoritative enum is `schema/sdui-schema.json`
(`TextVariant`, `ButtonVariant`, `ContainerVariant`, `ImageVariant`,
`SelectVariant`). As of the Phase 1 (2026-04-25) audit, that contract is:

- **`ContainerVariant`**: `hero`, `grouped`.
- **`ImageVariant`**: `thumbnail`.
- `TextVariant` and `ButtonVariant` unchanged; `SelectVariant` applies to
  `FormField` when `fieldType == "select"`.
- `schema/style-tokens.json` documents `ContainerVariant.hero`,
  `ContainerVariant.grouped`, and `ImageVariant.thumbnail` with full
  per-platform specs. Older ADR text that listed `elevated`, `banner`,
  `logo`, etc. described superseded exploration, not the current wire enum.
- `DividerVariant` remains deferred — `thickness` + `color` inline props
  cover current composers.

Artifacts:

- `schema/style-tokens.json` — the per-variant, per-platform,
  per-OS-tier spec, including dark-mode treatments and the
  per-axis override matrix. The JSON file is **non-normative
  documentation**: no client loads it at runtime. It is the
  review surface for design + platform engineering when a
  variant is added, renamed, or restyled.
- `scripts/validate-style-tokens.js` — structural CI validator.
  Fails the build when a variant entry omits a required tier,
  an override-matrix axis, or an evidence block.
- Client resolvers hand-encode the spec:
  - iOS: `ios/Sources/SduiCore/Rendering/ContainerVariantResolver.swift`
    and `ImageVariantResolver.swift` (Liquid Glass / `.thinMaterial` /
    semantic grouped-background tiers via `#available`).
  - Android: `android/sdui-core/src/main/java/com/nba/sdui/core/renderer/ContainerVariantResolver.kt`
    and `ImageVariantResolver.kt` (Material 3 surface roles, tonal
    elevation via `Build.VERSION.SDK_INT` checks).
  - Web: `web/src/utils/ContainerVariantResolver.ts` and
    `ImageVariantResolver.ts` (CSS custom properties with
    `backdrop-filter` feature detection).
- Server builder helpers: `AtomicCompositeBuilder` exposes
  `heroContainer()`, `elevatedContainer()`, `bannerContainer()`,
  `subtleContainer()`, `groupedContainer()`, `overlayContainer()`
  and `heroImage()`, `thumbnailImage()`, `logoImage()`.
- Pilot composer migrations: `HeroPanel`, `PromoBanner`,
  `ContentRail`'s card, and `VideoCarousel`'s card now emit
  variants in place of inline background / corner-radius prop
  bags. The full composer sweep is tracked as follow-up work.

Unknown values on the wire log `variant_resolver_missing` and fall
through to the primitive default; inline overrides on a locked axis
log `variant_override_blocked` and the variant default wins.

### Registry shape

Each platform field is an object keyed by OS-version tier. Each
override-matrix entry is either a scalar policy (applies to all
platforms) or an object keyed by platform when policies diverge.
Dark-mode specs live alongside their tier so the two resolve
together at render time.

```json
{
  "ContainerVariant": {
    "heroCard": {
      "description": "Prominent hero surface for featured content.",
      "intent": "Celebratory, press-forward surface conveying importance.",
      "ios": {
        "26+":   "Liquid Glass + 2-layer shadow + spring press; dark: glass tint shifts warm",
        "17-25": ".ultraThinMaterial + gradient overlay #FF6B6B→#D94A4A + spring press; dark: overlay #9B3535→#6B1F1F",
        "<17":   "Solid gradient #FF6B6B→#D94A4A + shadow (fallback)"
      },
      "android": {
        "15+":   "Material 3 Expressive: expressive shape + tonal elevation 6 + ripple; dark: tonal shift",
        "12-14": "Material You: Surface + tonal elevation 6 + ripple; dark: surfaceContainerHigh",
        "<12":   "Flat Material surface + elevation (fallback)"
      },
      "web": {
        "modern":   "--hero-card mixin: gradient + shadow + transition + backdrop-filter",
        "fallback": "Solid gradient + shadow (no backdrop-filter)"
      },
      "overrideMatrix": {
        "padding":      "allow",
        "cornerRadius": "allow",
        "background":   { "ios": "lock", "android": "allow", "web": "allow" },
        "shadow":       "lock",
        "color":        "allow"
      }
    }
  }
}
```

Notes on interpreting the registry:

- **`ios` / `android` / `web` are intent statements**, not pixel
  specs (see [Platform-native realization](#platform-native-realization)).
  The client resolver picks the right tier at runtime based on OS
  version / feature detection.
- **Tier keys are platform-conventional**: iOS uses numeric ranges
  matching iOS major versions; Android uses API-level major versions;
  web uses capability flags (`modern` / `fallback`) keyed to
  feature-detection rather than browser versions.
- **Design-system team owns tier boundaries** in the registry;
  platform teams own the concrete implementation within each tier.
- **Per-platform override matrix** prevents inline properties from
  accidentally destroying platform-idiomatic effects. In the example
  above, `background` is locked on iOS (because the liquid-glass or
  material effect would be flattened by a solid color) while allowed
  on Android and web where a solid override is visually acceptable.

### Compatibility strategy

- Variants are **additive** — existing composers continue to work.
- Clients that encounter an unknown variant value fall back to the
  primitive's default visual. Schema must carry every emitted value
  (Rule 13); an older client may simply not yet have a new variant's
  implementation wired.
- Inline props remain on the schema indefinitely. This ADR does not
  propose their removal.

### Governance

Roles:

- **Design systems team** owns token naming, visual spec, and Figma
  alignment.
- **Platform engineering** owns each platform's realization, including
  dark-mode specs.
- **Schema governance** owns the `*Variant` enums, the registry file
  (`schema/style-tokens.json`), and the override-matrix schema.

Rules (enforced by PR review and CI where possible):

1. **Addition bar (proof of need).** A new variant requires (a) a
   pointer to ≥2 existing composer patterns this variant replaces,
   (b) design-system sign-off, (c) all three client implementations
   covering at minimum the **current** and **one-back** OS tier per
   platform, including dark-mode spec at each tier, (d) registry
   entry with override matrix (scalar or per-platform) and
   per-platform realization notes keyed by OS-version tier,
   (e) codegen regeneration, (f) per-platform screenshot review
   against each platform's design-system spec (not cross-platform
   pixel diffs — see [Platform-native realization](#platform-native-realization)).
2. **Override-rate SLO.** Any variant with >30% override rate on any
   single axis (measured across server composer call sites) is flagged
   for review. Either the default is wrong, or the variant should be
   split.
3. **Audit cadence.** Quarterly audit of server composer call sites:
   any override pattern appearing 3+ times becomes a new-token
   candidate. Runs as a script over the composers, output reviewed
   with design.
4. **Deprecation policy.** Renaming or removing a variant requires a
   minimum two-release-cycle alias window. Schema carries the old name
   as an alias; client renderers resolve both old and new during the
   window. Removal from schema happens only after telemetry confirms
   the old name is no longer emitted for one full release cycle.
5. **Override-matrix changes** (flipping an axis from `allow` to
   `lock` or vice versa) are semantically-breaking and follow the
   same deprecation policy as rename/remove.
6. **Dark-mode coverage is mandatory.** A variant PR without a
   dark-mode spec at every declared tier on all three platforms
   does not merge.
7. **Tier fallback is mandatory.** A variant PR must define
   behavior for OS versions below the lowest declared "supported"
   tier — usually a reasonable flat / non-material fallback. Silent
   rendering failures on old OSes are not acceptable.

Metrics:

- **Override rate per variant per axis** — feed from server logs,
  reviewed quarterly.
- **Override-blocked warnings per variant per section** — surface in
  client diagnostics; review spikes.
- **Per-variant instance count** — sanity check against registry size
  vs. actual usage.

## Open Questions

- Should `DividerVariant` exist at all, or is the current `thickness` +
  `color` enough for ≥95% of uses?
- Does `Spacer` need a variant? (Initial position: no — no visual
  surface.)
- How do we handle **pressed / focused / hovered** state definitions?
  Per-variant definition in the registry, or a separate
  `InteractionState` token type composable onto a variant?
- Should there be a "variant composition" mechanism (e.g.
  `heroCard + error`) or do we require each meaningful combination
  to be its own token? (Initial position: each its own token — keeps
  the registry flat and auditable; revisit if combinatorial explosion
  bites.) If composition ever lands, **precedence between composed
  variants** becomes an open design question — left-to-right, most-
  specific-wins, or explicit layer order in the registry.
- Is `AtomicComposite` (section-level) genuinely out of scope, or
  should *sections* also gain a variant for things like section
  background / container chrome? (Initial position: out of scope;
  sections already have `layoutHints` per ADR-008 and `sectionStates`
  for runtime state. A third knob is over-provisioning.)
- Override-matrix granularity: do we need per-axis `allow` / `lock`,
  or does a simpler per-variant "strict" / "permissive" toggle cover
  real cases? (Initial position: per-axis, because
  background-lock-but-padding-allow is a real pattern. Per-platform
  values within an axis also required — see [Registry shape](#registry-shape).)
- **Pixel-parity escape hatch.** The default stance is that platform-
  native realization wins and cross-platform visual divergence is
  accepted (see [Platform-native realization](#platform-native-realization)).
  Brand-campaign takeovers, launch/onboarding moments, or partner-
  branded surfaces sometimes require identical visuals across
  platforms — a sponsor's logo treatment is not negotiable per
  platform. Options to explore: (a) a companion `parity` family of
  variants (`heroCardParity`, `cardParity`) that use flat non-
  material realizations everywhere; (b) a bespoke section renderer
  per Rule 15 when the screen genuinely owns unique visual state;
  (c) inline props with `overrideMatrix: allow` on every axis used
  intentionally as the escape hatch, accepting loss of theme
  adaptation. The answer likely depends on how frequent these
  moments actually are — revisit after the `HeroPanel` pilot and
  after surveying design for known upcoming takeover surfaces.

## Follow-ups

- [ ] Finalize the initial `ContainerVariant` token set with design.
- [ ] Create `schema/style-tokens.json` with initial entries,
      cross-platform specs, and per-variant override matrices.
- [ ] Implement the `ContainerVariant` vocabulary on the schema
      (emitted on the wire via the uniform `variant` property) and
      regenerate codegen.
- [ ] Pilot implementation on iOS, Android, web for the initial
      tokens, including dark-mode and the
      `variant_override_blocked` diagnostic.
- [ ] Migrate `HeroPanel` composition in `GameDetailComposer` as the
      pilot and capture payload-size + visual-parity evidence.
- [ ] Write the quarterly override-rate audit script over the
      composers (identify candidate new tokens + outlier override
      patterns).
- [ ] Document the governance workflow (PR template, review
      checklist, dark-mode requirement).
- [ ] Revisit open questions above after pilot lands.
