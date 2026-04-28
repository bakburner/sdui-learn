# SDUI Design System

Reference for the token registries, variant vocabularies, and Figma-to-wire
mapping that power SDUI atomic composition.

**Audience:** designers maintaining the Figma source of truth and engineers
syncing tokens between Figma and the schema registries.

### Where this doc fits

The SDUI architecture has two complementary layers:

| Layer | What it covers | Change velocity |
|---|---|---|
| **Atomic composition** (this document) | Server-composed trees of generic primitives — containers, text, images, buttons — rendered idiomatically per platform. | Changes ship with server deploys (minutes to hours). |
| **Typed semantic sections** (separate spec) | Named contracts (`BoxscoreTable`, `VideoPlayer`, `AdSlot`, `Form`, etc.) with domain-typed data, client-owned state, and platform-SDK integration. | Changes ship with client releases (weeks+). |

Atomic composition targets the ~60% of surfaces (feeds, editorial, promos,
marketing modules) where future changes are copy edits, layout tweaks, and
cohort-scoped variants — all deployable without a client release. Typed
sections handle the remaining ~40% (live game state, video, ads, billing,
dense interactive controls) where native code is required.

A given screen mixes both. This document covers the atomic layer only.

**Related files:**

| File | Purpose |
|---|---|
| `schema/color-tokens.json` | Color palette + semantic aliases (light/dark) |
| `schema/spacing-tokens.json` | Spacing scale per form factor |
| `schema/size-tokens.json` | Semantic sizes per form factor |
| `schema/typography-tokens.json` | Type scale per form factor |
| `schema/corner-radius-tokens.json` | Corner radii per form factor |
| `schema/shadow-tokens.json` | Elevation tiers per form factor |
| `schema/icon-tokens.json` | Icon name → per-platform symbol mapping |
| `schema/style-tokens.json` | Variant definitions + override matrices |
| `schema/sdui-schema.json` | Wire contract (enum values, element types) |

---

## 1. System overview

The design system has three layers. Every visual decision uses the lowest
layer that can express it.

```
┌─────────────────────────────────────────────────────────┐
│ Layer 3: Color tokens     (token:color.brand.nba)       │  ← brand & content color
├─────────────────────────────────────────────────────────┤
│ Layer 2: Variants         (variant: "hero")             │  ← platform-native preset
├─────────────────────────────────────────────────────────┤
│ Layer 1: Inline primitives (padding, cornerRadius, …)   │  ← orthogonal values
├─────────────────────────────────────────────────────────┤
│ Platform default          (renderer baseline)           │  ← floor
└─────────────────────────────────────────────────────────┘
```

**Layer 1 — Inline primitives.** Granular properties on each element:
`padding`, `cornerRadius`, `shadow`, `gap`, `opacity`, `border`,
`background`, `aspectRatio`, `size`, `maxLines`, `textAlign`. All sizes
and spacings use semantic tokens (not raw pixels).

**Layer 2 — Variants.** A named preset that carries platform-native
treatments inline properties cannot express (materials, multi-layer shadows,
press animations, OS-adaptive surfaces). Each platform resolves variants to
its own native idiom.

**Layer 3 — Color tokens.** A `token:color.*` reference that resolves to
light/dark hex at render time. Used for brand, content, and surface colors.

### Decision rule

1. Can inline primitives (with semantic tokens) express it? → Use inline.
2. Could one more orthogonal inline property close the gap? → Extend inline.
3. Does it need a platform SDK, OS-version API, or multi-layer compositing?
   → Variant.
4. Does the color need light/dark adaptation or a stable semantic name?
   → Color token. Otherwise inline hex is fine.

---

## 2. Token registries

### 2.1 Color tokens

**File:** `schema/color-tokens.json` · **Figma connection:** Figma
color styles should match semantic alias names 1:1.

Two-tier structure:

- **Palette primitives** — `color.<family>.<step>` with `{ light, dark }`
  hex pairs.
- **Semantic aliases** — named roles pointing to palette primitives.

**Palette families:**

| Family | Steps | Purpose |
|---|---|---|
| `grey` | 0–100 | Neutral surfaces, text, borders |
| `blue` | 0–100 | Tertiary accent, links |
| `red` | 0–100 | Error feedback |
| `green` | 0–100 | Success feedback |
| `orange` | 0–100 | Warning feedback |
| `yellow` | 0–100 | Secondary accent |

**Semantic aliases (current inventory):**

| Group | Tokens |
|---|---|
| Primary scale | `color.primary.{0,10,20,30,40,50,60,70,80,90,95,99,100}` |
| Secondary scale | `color.secondary.{0–100}` |
| Tertiary scale | `color.tertiary.{0–100}` |
| Success feedback | `color.feedback.success.{0–100}` |
| Error feedback | `color.feedback.error.{0–100}` |
| Warning feedback | `color.feedback.warning.{0–100}` |
| Brand | `color.brand.nba`, `color.brand.live` |
| Surfaces | `color.surface.canvas`, `.raised`, `.sunken`, `.promo` |
| Text | `color.text.primary`, `.secondary`, `.tertiary`, `.inverse`, `.onBrand` |
| Borders | `color.border.default`, `.subtle` |
| Overlay | `color.overlay.scrim` |

**Light/dark resolution:** clients select `light` or `dark` from the
palette primitive at render time based on OS color scheme.

**`text.inverse` and `text.onBrand`** resolve to the same hex in both
modes (typically white). They mean "right text color on a known-brightness
surface," not "opposite of current mode."

**Team brand colors** are not in this registry. They are inline brand assets
owned per-team (e.g. BOS `#007A33`, GSW `#1D428A`).

Color tokens are **not** form-factor-aware — only light/dark.

### 2.2 Spacing tokens

**File:** `schema/spacing-tokens.json` · **Figma connection:** Figma
auto-layout spacing values should use these semantic names.

| Semantic token | Phone | Tablet | TV | Web narrow | Web wide |
|---|---|---|---|---|---|
| `spacing.xs` | 4 | 4 | 8 | 4 | 4 |
| `spacing.sm` | 8 | 12 | 16 | 8 | 12 |
| `spacing.md` | 16 | 20 | 32 | 16 | 20 |
| `spacing.lg` | 24 | 24 | 32 | 24 | 24 |
| `spacing.xl` | 24 | 24 | 32 | 24 | 24 |
| `spacing.xxl` | 32 | 32 | 32 | 32 | 32 |

Used for `padding`, `gap`, and `spacing` properties on atomic elements.

### 2.3 Size tokens

**File:** `schema/size-tokens.json` · **Figma connection:** Figma
component sizing constraints should reference these tokens.

| Semantic token | Phone | Tablet | TV | Web narrow | Web wide |
|---|---|---|---|---|---|
| `icon.sm` | 20 | 20 | 32 | 20 | 20 |
| `icon.md` | 32 | 32 | 48 | 32 | 32 |
| `icon.lg` | 40 | 48 | 64 | 40 | 48 |
| `logo.team.sm` | 32 | 40 | 56 | 32 | 40 |
| `logo.team.md` | 48 | 64 | 96 | 48 | 64 |
| `logo.team.lg` | 64 | 72 | 96 | 64 | 72 |
| `avatar.sm` | 32 | 40 | 48 | 32 | 40 |
| `avatar.md` | 48 | 56 | 72 | 48 | 56 |
| `avatar.lg` | 64 | 72 | 96 | 64 | 72 |
| `thumbnail.sm` | 40 | 48 | 64 | 40 | 48 |
| `thumbnail.md` | 56 | 64 | 96 | 56 | 64 |

### 2.4 Typography tokens

**File:** `schema/typography-tokens.json` · **Figma connection:** Figma
text styles should map to these semantic names.

**Semantic tokens:** `type.body`, `type.bodyEm`, `type.title`,
`type.headline` — each with per-form-factor values.

**Schema `TextVariant` enum** (used on `Text` and `LiveClock` elements):

`displayLarge`, `displayMedium`, `displaySmall`, `headlineLarge`,
`headlineMedium`, `headlineSmall`, `titleLarge`, `titleMedium`,
`titleSmall`, `bodyLarge`, `bodyMedium`, `bodySmall`, `labelLarge`,
`labelMedium`, `labelSmall`, `score`

The `score` variant carries monospaced/tabular-numeral typography for live
scores and clocks.

**`TextWeight` values:** `regular`, `medium`, `semiBold`, `bold`

### 2.5 Corner radius tokens

**File:** `schema/corner-radius-tokens.json` · **Figma connection:** Figma
corner radius values should use these names.

| Semantic token | Phone | Tablet | TV | Web narrow | Web wide |
|---|---|---|---|---|---|
| `radius.sm` | 4 | 4 | 8 | 4 | 4 |
| `radius.md` | 8 | 8 | 12 | 8 | 8 |
| `radius.lg` | 12 | 16 | 16 | 12 | 16 |
| `radius.xl` | 16 | 16 | 24 | 16 | 16 |
| `radius.full` | 999 | 999 | 999 | 999 | 999 |

`shape: "circle"` is also available — clients compute `radius = width/2`.

### 2.6 Shadow tokens

**File:** `schema/shadow-tokens.json`

| Semantic token | Purpose |
|---|---|
| `shadow.sm` | Subtle lift (cards) |
| `shadow.md` | Medium elevation (modals, popovers) |
| `shadow.lg` | High elevation (hero surfaces) |

### 2.7 Icon tokens

**File:** `schema/icon-tokens.json` · **Figma connection:** Figma icon
components should use the `sdui:` prefix name as the component name.

Each token maps to platform-specific symbols:

| Token | SF Symbols (iOS) | Material Icons (Android/Web) |
|---|---|---|
| `sdui:play` | `play.fill` | `play_arrow` |
| `sdui:pause` | `pause.fill` | `pause` |
| `sdui:back` | `chevron.left` | `arrow_back` |
| `sdui:forward` | `chevron.right` | `arrow_forward` |
| `sdui:settings` | `gearshape` | `settings` |
| `sdui:home` | `house` | `home` |
| `sdui:basketball` | `basketball` | `sports_basketball` |
| `sdui:video` | `play.rectangle` | `smart_display` |
| `sdui:leaderboard` | `chart.bar` | `leaderboard` |
| `sdui:grid` | `square.grid.2x2` | `grid_view` |
| `sdui:expand` | `chevron.down` | `expand_more` |
| `sdui:collapse` | `chevron.up` | `expand_less` |
| `sdui:check` | `checkmark` | `check` |
| `sdui:warning` | `exclamationmark.triangle` | `warning` |
| `sdui:live` | `dot.radiowaves.left.and.right` | `sensors` |
| `sdui:person` | `person` | `person` |
| `sdui:close` | `xmark` | `close` |
| `sdui:search` | `magnifyingglass` | `search` |
| `sdui:share` | `square.and.arrow.up` | `share` |
| `sdui:favorite` | `heart` | `favorite_border` |
| `sdui:favorited` | `heart.fill` | `favorite` |
| `sdui:fullscreen` | `arrow.up.left.and.arrow.down.right` | `fullscreen` |
| `sdui:pip` | `pip` | `picture_in_picture_alt` |
| `sdui:cast` | `tv` | `cast` |
| `sdui:info` | `info.circle` | `info` |
| `sdui:calendar` | `calendar` | `calendar_today` |
| `sdui:refresh` | `arrow.clockwise` | `refresh` |

Directional icons (`back`, `forward`) auto-mirror in RTL locales.

---

## 3. Variants

Variants carry platform-native treatments that inline properties cannot
express: materials, multi-layer shadows, interaction states, OS-adaptive
surfaces, and form-factor-adaptive sizing.

### 3.1 Container variants

**`hero`** — Featured content surface.

| Platform | Realization |
|---|---|
| iOS 26+ | Liquid Glass + 2-layer shadow + spring press |
| iOS 17–25 | `.ultraThinMaterial` + gradient overlay + shadow |
| iOS <17 | Solid `.secondarySystemGroupedBackground` + overlay + shadow |
| Android 15+ | Material 3 Expressive surface + tonalElevation 6dp + ripple |
| Android 12–14 | Material You + tonalElevation 6dp + ripple |
| Android <12 | Flat Material surface + elevation 6dp + ripple |
| Web modern | Linear gradient + `backdrop-filter: blur(20px)` + box-shadow |
| Web fallback | Solid raised surface + box-shadow (no backdrop-filter) |

Override matrix:

| Axis | Policy |
|---|---|
| `padding` | allow |
| `cornerRadius` | allow |
| `background` | **iOS: lock** · Android/Web: allow |
| `shadow` | lock |
| `color`, `gap`, `opacity`, `border` | allow |

`background` is locked on iOS because Liquid Glass /
`.ultraThinMaterial` cannot be replaced with an inline solid color.

**`grouped`** — Inset-grouped list surface. All axes allow override.

### 3.2 Image variants

**`thumbnail`** — Cropped media tile with platform-native cross-fade and
placeholder reservation. All axes allow override.

### 3.3 Button variants

`primary`, `secondary`, `tertiary`, `text` — each platform maps to its
native button style.

### 3.4 Select variants (Form fields)

`dropdown` (default platform menu), `chips` (horizontal capsule row).
Applies when `FormField.fieldType == "select"`.

### 3.5 Form-factor adaptation

Variants adapt per form factor. Example for `hero` on Android 15+:

| Form factor | Adaptation |
|---|---|
| Phone | 16dp radius, 12dp padding |
| Tablet | 20dp radius, 24dp padding, larger type |
| TV | 24dp radius, 32dp padding, focusable, focus halo, 10-foot type |
| Web narrow | 16px radius, 12px padding |
| Web wide | 20px radius, 24px padding, hover lift |

---

## 4. Figma-to-wire mapping

### How Figma structures map to the wire format

| Figma construct | Wire equivalent |
|---|---|
| Frame with auto-layout | `Container` with `direction`, `gap`, `padding` |
| Text layer | `Text` with `variant` + `weight` + `color` |
| Image fill / placed image | `Image` with `src`, `size`, `fit` |
| Component instance (Button) | `Button` with `variant` + `label` |
| Icon component | `Icon` with `token` (e.g. `sdui:play`) + `size` |
| Color style | `token:color.*` reference |
| Spacing value | Semantic token (`spacing.md`) |
| Corner radius | Semantic token (`radius.lg`) or `shape: "circle"` |
| Shadow effect | Semantic token (`shadow.md`) |
| Text style | `TextVariant` enum value |

### Figma naming conventions

For tokens to round-trip between Figma and the wire:

- **Color styles** use the semantic alias path: `color/text/primary`,
  `color/brand/nba`, `color/surface/canvas`.
- **Text styles** use the `TextVariant` name: `bodyMedium`, `titleLarge`,
  `score`.
- **Spacing variables** use the semantic name: `spacing/xs`, `spacing/md`.
- **Size variables** use the semantic name: `icon/md`, `avatar/lg`.
- **Corner radius variables** use: `radius/sm`, `radius/lg`, `radius/full`.
- **Icon components** use the `sdui:` prefix: `sdui:play`, `sdui:close`.

### What Figma files should contain

1. **A token library** with color styles, text styles, spacing/size
   variables, and corner radius variables matching the registry names above.
2. **Atomic primitive components** — Container, Text, Image, Button, Icon,
   Divider, Spacer, Badge — with variant props matching the schema enums.
3. **Composed examples** showing how primitives assemble into common
   patterns (hero card, list row, promo banner).

### What to avoid in Figma

- Raw hex colors where a semantic token exists.
- Pixel values where a spacing/size/radius token exists.
- Custom one-off text styles not in the `TextVariant` enum.
- New variant names without a schema change (schema → codegen → client
  implementation must happen first).

---

## 5. Accessibility checklist for designers

Every interactive or informational element needs an `a11y.label`. Decorative
images should be marked `hidden: true`.

| Field | Purpose | Required when |
|---|---|---|
| `label` | Screen reader name | Interactive elements, informational images |
| `hint` | Supplementary description | Complex interactions |
| `role` | `button`, `link`, `image`, `header`, `text`, `none` | Auto-inferred; override when needed |
| `hidden` | Hide from screen readers | Decorative imagery |

The full `AccessibilityProperties` schema defines 7 fields: `label`, `hint`, `role`, `hidden`, `headingLevel`, `liveRegion`, `sortOrder` — see `schema/sdui-schema.json` for the authoritative definition.

**CI lint:** images with actions but no `a11y.label` are flagged.

---

## 6. Internationalization

**i18n core (implemented):**

- All text is server-translated per locale.
- Locale travels in the request envelope (`locale` param).
- Server-side string tables support en/es/fr; sections carry `stringTable` maps.
- Date/time formatting uses locale-aware `DateTimeFormatter` on the server.
- Edge insets use `start`/`end` (not `left`/`right`) to prepare for RTL.

**RTL layout support (planned, deferred):**

RTL layout support — including directional icon auto-mirroring (`rtl: "mirror"` in the icon registry), row container child-order reversal in RTL locales, and start/end inset flipping — is planned but not yet implemented.

---

## 7. Form factors

All token registries (except color) have per-form-factor values.

| Class | Width range | Input | Notes |
|---|---|---|---|
| `phone` | 320–430pt | Touch | One-handed reach |
| `phone.landscape` | — | Touch | Landscape-aware layouts |
| `tablet` | 600–1300pt | Touch + pointer | Multi-column |
| `tv` | 1920×1080+ | D-pad remote | 10-foot, focus-driven, overscan-safe |
| `web.narrow` | <768px | Mouse + touch + keyboard | Mobile web |
| `web.wide` | ≥768px | Mouse + keyboard | Hover states, scroll wheel |

---

## 8. Diagnostics

Clients emit four diagnostics for design-system mismatches:

| Diagnostic | Meaning |
|---|---|
| `variant_resolver_missing` | Variant string not in the enum for this element type |
| `variant_override_blocked` | Inline property set on a locked axis |
| `token_resolver_missing` | `token:` reference not found in registry |
| `section_decode_failed` | Section payload did not match schema |

All are non-fatal. The renderer falls back to defaults and logs the issue.

---

## 9. Gaps and completion checklist

### Figma integration

- [ ] Figma token library published with all color, spacing, size, radius,
      and typography tokens matching registry names
- [ ] Figma variables connected to `color-tokens.json` light/dark values
- [ ] Figma text styles mapped 1:1 to `TextVariant` enum
- [ ] Figma icon component library matching `icon-tokens.json` names
- [ ] Automated Figma → JSON token export pipeline (e.g. Tokens Studio)

### Token coverage

- [ ] Typography registry has only 4 semantic tokens (`type.body`,
      `type.bodyEm`, `type.title`, `type.headline`) — needs full coverage
      matching the 16-value `TextVariant` enum
- [ ] Shadow registry has only 3 tokens — evaluate if more tiers are needed
- [ ] Size registry missing tokens for larger hero/banner sizes
- [ ] No `opacity` token registry — decide if one is needed

### Variant coverage

- [ ] `ContainerVariant` has 2 values (`hero`, `grouped`) — evaluate if
      additional surface treatments are needed
- [ ] `ImageVariant` has 1 value (`thumbnail`) — evaluate hero image, avatar
      circle, and background image patterns
- [ ] TV and tablet realization specs need validation against device testing
- [ ] Dark mode screenshot review across all variant × platform × tier
      combinations

### Tooling

- [ ] CI pipeline validating Figma export against JSON registries
- [ ] Override-rate monitoring (>30% override rate flags review)
- [ ] Token usage tracking across composers
