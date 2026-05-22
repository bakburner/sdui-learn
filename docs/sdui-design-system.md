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
| `schema/spacing-tokens.json` | Spacing scale per form factor (Kinetic v1.0.0) |
| `schema/corner-radius-tokens.json` | Corner radii — flat across form factors (Kinetic v1.0.0) |
| `schema/icon-tokens.json` | Icon name → per-platform symbol mapping |
| `schema/style-tokens.json` | Variant definitions + override matrices |
| `schema/sdui-schema.json` | Wire contract (enum values, element types) |
| `server/.../LayoutTokens.java` | Server-side wire-form token constants (spacing + radius) |
| `server/.../IconTokens.java` | Server-side wire-form icon token constants |

> **Planned (awaiting design validation):** Size tokens, typography tokens,
> and shadow tokens are not yet validated by the design team. The speculative
> registry files were removed. These will be rebuilt from Kinetic design
> exports when the design team provides validated scales.

---

## 1. System overview

The design system has three layers. Every visual decision uses the lowest
layer that can express it.

```
┌─────────────────────────────────────────────────────────┐
│ Layer 3: Color tokens     (token:nba.label.accent.brand)│  ← brand & content color
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

**Layer 3 — Color tokens.** A `token:nba.color.*` or `token:nba.label.*`
reference that resolves to light/dark hex at render time. Used for brand,
content, and surface colors.

### Decision rule

1. Can inline primitives (with semantic tokens) express it? → Use inline.
2. Can an ordered stack of existing inline primitives express it? → Use
   inline array (`backgrounds`, `shadows`).
3. Could one more orthogonal inline property close the gap? → Extend inline.
4. Does it need a platform SDK, OS-version API, or runtime interaction
   effect? → Variant.
5. Does the color need light/dark adaptation or a stable semantic name?
   → Color token. Otherwise inline hex is fine.

---

## 2. Token registries

**Registry version:** `2.0.0-matrix`

All token registries share a common matrix shape:

```json
{
  "<tokenName>": {
    "<theme>": {
      "<formFactor>": <value>
    }
  }
}
```

Either axis may use the `"*"` wildcard to indicate the value is independent
of that dimension. For example, spacing tokens are theme-independent
(use `"*"` for theme), while color UI tokens are form-factor-independent
(use `"*"` for form factor).

**Resolution algorithm (4-step fallback):**

1. **Exact match** — `registry[token][theme][formFactor]`
2. **Theme-wildcard** — `registry[token]["*"][formFactor]`
3. **Form-factor-wildcard** — `registry[token][theme]["*"]`
4. **Universal** — `registry[token]["*"]["*"]`

Theme takes priority over form factor in the fallback order because
color/brand correctness is a hard constraint (wrong color = wrong brand),
while form-factor adaptation is a progressive enhancement (slightly
off-size spacing still renders correctly).

**Examples:**

```jsonc
// Spacing — theme-independent, form-factor-variable
"nba.spacing.lg": {
  "*": {
    "phone": 16,
    "tablet": 20,
    "tv": 24,
    "web.narrow": 16,
    "web.wide": 20
  }
}

// Color UI token — form-factor-independent, theme-variable
"nba.bg.primary": {
  "light": { "*": "#FFFFFF" },
  "dark":  { "*": "#121212" }
}
```

The matrix is intentionally sparse — most tokens vary on only one axis
and use `"*"` for the other. The resolver's fallback chain means registries
need not enumerate every combination.

### 2.1 Color tokens

**File:** `schema/color-tokens.json` · **Figma connection:** Figma
color styles should match semantic alias names 1:1.

Multi-tier structure sourced from the Kinetic Design System:

- **Primitives** — `nba.color.<family>.<step>` with `{ light, dark }` hex
  pairs. Families: `grey`, `blue`, `red`, `green`, `orange`, `yellow`,
  `t-black` (transparent black), `t-white` (transparent white).
- **Semantic aliases** — named color scales pointing to primitives
  (`nba.color.primary.*`, `nba.color.secondary.*`, `nba.color.tertiary.*`,
  `nba.color.feedback.*`).
- **Labels** — purpose-defined text/icon colors per context:
  `nba.label.primary`, `nba.label.secondary`, `nba.label.interactive`,
  `nba.label-dark.*`, `nba.label-inverted.*`, `nba.label-tint.*`,
  `nba.label.accent.brand`, `nba.label.accent.live`.
- **UI / Backgrounds** — `nba.bg.primary`, `nba.bg.secondary`,
  `nba.bg-dark.*`, `nba.bg-inverted.*`, `nba.bg-tint.*`.
- **Button** — `nba.button.primary.label`, `nba.button.secondary.label`,
  `nba.button.tint.label`, `nba.button.focus-ring`.
- **Team** — team brand colors injected at composition time (e.g.
  BOS `#007A33`, GSW `#1D428A`).

**Palette families:**

| Family | Steps | Purpose |
|---|---|---|
| `grey` | 0–100 | Neutral surfaces, text, borders |
| `blue` | 0–100 | Tertiary accent, links |
| `red` | 0–100 | Error feedback |
| `green` | 0–100 | Success feedback |
| `orange` | 0–100 | Warning feedback |
| `yellow` | 0–100 | Secondary accent |
| `t-black` | 5–90 | Transparent black overlays |
| `t-white` | 5–90 | Transparent white overlays |

**Semantic aliases (current inventory):**

| Group | Tokens |
|---|---|
| Primary scale | `nba.color.primary.{0,10,20,30,40,50,60,70,80,90,95,99,100}` |
| Secondary scale | `nba.color.secondary.{0–100}` |
| Tertiary scale | `nba.color.tertiary.{0–100}` |
| Success feedback | `nba.color.feedback.success.{0–100}` |
| Error feedback | `nba.color.feedback.error.{0–100}` |
| Warning feedback | `nba.color.feedback.warning.{0–100}` |
| Labels | `nba.label.primary`, `.secondary`, `.tertiary`, `.interactive`, `.selection` |
| Label dark | `nba.label-dark.primary`, `.secondary`, `.tertiary`, `.quaternary`, `.interactive` |
| Label inverted | `nba.label-inverted.primary`, `.secondary`, `.tertiary`, `.quaternary`, `.link` |
| Brand accents | `nba.label.accent.brand`, `nba.label.accent.live`, `nba.label.accent.splash-screen` |
| Backgrounds | `nba.bg.primary`, `.secondary`, `.tertiary`, `.quaternary`, `.selection`, `.badge`, `.disabled` |
| Background dark | `nba.bg-dark.primary`, `.secondary`, `.tertiary`, `.quaternary` |
| Buttons | `nba.button.primary.label`, `.secondary.label`, `.tint.label`, `.focus-ring` |

**Light/dark resolution:** clients select `light` or `dark` from the
palette primitive at render time based on OS color scheme.

**`text.inverse` and `text.onBrand`** resolve to the same hex in both
modes (typically white). They mean "right text color on a known-brightness
surface," not "opposite of current mode."

**Team brand colors** are not in this registry. They are inline brand assets
owned per-team (e.g. BOS `#007A33`, GSW `#1D428A`).

Color tokens are **not** form-factor-aware — only light/dark.

> **Workstream B note:** Color tokens remain theme-only (light/dark
> resolution). The backgrounds/shadows array work does not change the color
> token registry — it uses existing color token references inline.

### 2.2 Spacing tokens

**File:** `schema/spacing-tokens.json` (v1.0.0-kinetic) · **Server constants:** `LayoutTokens.java` ·
**Figma connection:** Figma auto-layout spacing values should use these semantic names.

Sourced from the Kinetic Design System. Phone is the base scale;
form-factor multipliers are applied by each client's `LayoutTokenResolver`.

| Wire token | Phone | Tablet | TV | Web narrow | Web wide |
|---|---|---|---|---|---|
| `nba.spacing.xs` | 2 | 2 | 4 | 2 | 2 |
| `nba.spacing.sm` | 4 | 6 | 6 | 4 | 6 |
| `nba.spacing.md` | 12 | 15 | 18 | 12 | 15 |
| `nba.spacing.lg` | 16 | 20 | 24 | 16 | 20 |
| `nba.spacing.xl` | 32 | 40 | 48 | 32 | 40 |
| `nba.spacing.2xl` | 40 | 48 | 56 | 40 | 48 |

Used for `padding`, `gap`, and `spacing` properties on atomic elements.
Composers emit these via `LayoutTokens.SPACING_*` constants.

### 2.3 Size tokens (planned)

> **Status:** Awaiting validated design input. The speculative
> `schema/size-tokens.json` was removed — values were engineering
> guesses with no design-team sign-off.

When rebuilt, this registry will cover icon sizes, logo sizes, avatar
sizes, and thumbnail dimensions with per-form-factor values.

### 2.4 Typography tokens (planned)

> **Status:** Awaiting validated design input. The speculative
> `schema/typography-tokens.json` was removed.

Typography is currently expressed through the `TextVariant` enum on the
schema. The future token registry will provide per-form-factor type
scales that variants can reference.

**Schema `TextVariant` enum** (used on `Text` and `LiveClock` elements):

`displayLarge`, `displayMedium`, `displaySmall`, `headlineLarge`,
`headlineMedium`, `headlineSmall`, `titleLarge`, `titleMedium`,
`titleSmall`, `bodyLarge`, `bodyMedium`, `bodySmall`, `labelLarge`,
`labelMedium`, `labelSmall`, `score`

The `score` variant carries monospaced/tabular-numeral typography for live
scores and clocks.

**`TextWeight` values:** `regular`, `medium`, `semiBold`, `bold`

### 2.5 Corner radius tokens

**File:** `schema/corner-radius-tokens.json` (v1.0.0-kinetic) · **Server constants:** `LayoutTokens.java` ·
**Figma connection:** Figma corner radius variables should use these names.

Sourced from the Kinetic Design System. Flat across all form factors —
corner radii do not scale with device class.

| Wire token | Value (all form factors) |
|---|---|
| `nba.radius.xs` | 2 |
| `nba.radius.sm` | 4 |
| `nba.radius.md` | 12 |
| `nba.radius.lg` | 16 |
| `nba.radius.xl` | 24 |
| `nba.radius.2xl` | 32 |
| `nba.radius.full` | 9999 |

`nba.radius.full` produces a pill/circle shape. The `shape: "circle"` schema
field remains available as a shorthand for `cornerRadius: "token:nba.radius.full"`.
Composers emit these via `LayoutTokens.RADIUS_*` constants.

### 2.6 Shadow tokens (planned)

> **Status:** Awaiting validated design input. The speculative
> `schema/shadow-tokens.json` was removed.

### 2.7 Icon tokens

**File:** `schema/icon-tokens.json` · **Server constants:** `IconTokens.java` ·
**Figma connection:** Figma icon components should use the `sdui:` prefix name
as the component name.

Each token maps to platform-specific symbols:

| Token | SF Symbols (iOS) | Material (Android) | Web |
|---|---|---|---|
| `sdui:play` | `play.fill` | `PlayArrow` | `play_arrow` |
| `sdui:pause` | `pause.fill` | `Pause` | `pause` |
| `sdui:back` | `chevron.left` | `ArrowBack` | `arrow_back` |
| `sdui:forward` | `chevron.right` | `ArrowForward` | `arrow_forward` |
| `sdui:settings` | `gearshape` | `Settings` | `settings` |
| `sdui:home` | `house` | `Home` | `home` |
| `sdui:basketball` | `basketball.fill` | `SportsBasketball` | `sports_basketball` |
| `sdui:video` | `play.rectangle` | `PlayCircle` | `play_circle` |
| `sdui:leaderboard` | `list.number` | `Leaderboard` | `leaderboard` |
| `sdui:grid` | `square.grid.2x2` | `Widgets` | `widgets` |
| `sdui:expand` | `chevron.down` | `KeyboardArrowDown` | `expand_more` |
| `sdui:collapse` | `chevron.up` | `KeyboardArrowUp` | `expand_less` |
| `sdui:check` | `checkmark` | `Check` | `check` |
| `sdui:warning` | `exclamationmark.triangle` | `Warning` | `warning` |
| `sdui:live` | `antenna.radiowaves.left.and.right` | `Sensors` | `sensors` |
| `sdui:person` | `person.circle` | `AccountCircle` | `account_circle` |
| `sdui:close` | `xmark` | `Close` | `close` |
| `sdui:search` | `magnifyingglass` | `Search` | `search` |
| `sdui:share` | `square.and.arrow.up` | `Share` | `share` |
| `sdui:favorite` | `heart` | `FavoriteBorder` | `favorite_border` |
| `sdui:favorited` | `heart.fill` | `Favorite` | `favorite` |
| `sdui:fullscreen` | `arrow.up.left.and.arrow.down.right` | `Fullscreen` | `fullscreen` |
| `sdui:pip` | `pip` | `PictureInPicture` | `picture_in_picture` |
| `sdui:cast` | `airplayvideo` | `Cast` | `cast` |
| `sdui:info` | `info.circle` | `Info` | `info` |
| `sdui:calendar` | `calendar` | `CalendarToday` | `calendar_today` |
| `sdui:refresh` | `arrow.clockwise` | `Refresh` | `refresh` |
| `sdui:lock` | `lock.fill` | `Lock` | `lock` |

Directional icons (`back`, `forward`) auto-mirror in RTL locales.

---

## 3. Variants

Variants carry platform-native treatments that inline properties cannot
express: materials, interaction states, OS-adaptive surfaces, and
form-factor-adaptive sizing.

### Variant escalation boundary

After Workstream B, the boundary between inline and variant is:

**Stays variant-only:**

| Treatment | Why |
|---|---|
| Blend modes | Inconsistent platform support |
| Platform materials (Liquid Glass, `.ultraThinMaterial`, Material You tonal surfaces) | Require OS-version-gated APIs |
| `backdrop-filter` (blur, saturation) | CSS-only; no equivalent Compose/SwiftUI semantics |
| Runtime interaction effects (press, ripple, hover lift, focus halo) | Require platform animation/interaction APIs |

**Moves inline:**

| Treatment | Wire representation |
|---|---|
| Solid color fill | Single `Background` (existing) |
| Linear gradient | `BackgroundGradient` (existing) |
| Image fill + overlay | `BackgroundImage` (existing) |
| Multi-layer fill stack | `backgrounds` array |
| Multi-layer drop shadow | `shadows` array, `type: "drop"` |
| Inner shadow | `shadows` array, `type: "inner"` |

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

> **Post-Workstream B:** After inline `backgrounds`/`shadows` arrays land,
> `hero` carries only platform materials, backdrop blur, and OS-gated
> interaction effects — treatments that genuinely require platform APIs.
> The shadow `lock` on `hero` is subject to product review (multi-layer
> drop shadows are now expressible inline via the `shadows` array).

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
| Clickable frame | `Container` with `actions` (`onActivate` / `onTap`) |
| Text layer | `Text` with `variant` + `weight` + `color` |
| Image fill / placed image | `Image` with `src`, `size`, `fit` |
| Component instance (Button) | `Button` with `variant` + `label` |
| Icon component | `Icon` with `token` (e.g. `sdui:play`) + `size` |
| Color style | `token:nba.color.*` or `token:nba.label.*` reference |
| Spacing value | Semantic token (`token:nba.spacing.md`) |
| Corner radius | Semantic token (`token:nba.radius.lg`) or `shape: "circle"` |
| Text style | `TextVariant` enum value |
| Multiple fills (stacked) | `backgrounds` array — ordered bottom-to-top |
| Drop shadow (single or stacked) | `shadows` array with `type: "drop"` entries |
| Inner shadow | `shadows` array with `type: "inner"` entry |

### Figma naming conventions

For tokens to round-trip between Figma and the wire:

- **Color styles** use the `nba.` prefix path: `nba.label.primary`,
  `nba.label.accent.brand`, `nba.bg.primary`, `nba.color.primary.50`.
- **Text styles** use the `TextVariant` name: `bodyMedium`, `titleLarge`,
  `score`.
- **Spacing variables** use the `nba.spacing` prefix: `nba.spacing.xs`,
  `nba.spacing.md`.
- **Corner radius variables** use: `nba.radius.sm`, `nba.radius.lg`,
  `nba.radius.full`.
- **Icon components** use the `sdui:` prefix: `sdui:play`, `sdui:close`.

### What Figma files should contain

1. **A token library** with color styles, text styles, spacing/size
   variables, and corner radius variables matching the registry names above.
2. **Atomic primitive components** — Button, Conditional, Container,
   DisplayGrid, Divider, Image, LiveClock, OverlayContainer,
   ScrollContainer, SectionSlot, Spacer, Text — with variant props matching
   the schema enums. (Icon rendering is handled via icon tokens on
   `Button`/`Image`; badge overlays use `OverlayContainer`.)
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

> **Token resolution and form factors (v2.0.0-matrix):** The token resolver
> now supports joint theme × form-factor variation through the matrix shape
> described in §2. In practice today, spacing tokens remain theme-independent
> (they use `"*"` for the theme axis) and color UI tokens remain
> form-factor-independent (they use `"*"` for the form-factor axis). However,
> the resolver handles both axes, so future tokens that need simultaneous
> theme and form-factor variation (e.g. a surface shadow that changes between
> light/dark *and* scales with device class) are supported without further
> infrastructure changes.

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

- [ ] Typography token registry removed (awaiting Kinetic design validation)
      — currently expressed only as `TextVariant` enum (16 values)
- [ ] Shadow token registry removed (awaiting Kinetic design validation)
      — evaluate tiers needed when rebuilt
- [ ] Size token registry removed (awaiting Kinetic design validation)
      — evaluate icon/logo/avatar/thumbnail sizes when rebuilt
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
