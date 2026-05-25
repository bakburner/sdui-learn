# SDUI Design System

Reference for the token registries, variant vocabularies, and Figma-to-wire
mapping that power SDUI atomic composition.

**Audience:** designers maintaining the Figma source of truth and engineers
syncing tokens between Figma and the schema registries.

> **Maintenance discipline:** Changes to `schema/*-tokens.json` and
> `docs/sdui-design-system.md` ship in the same PR.

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
| `schema/typography-tokens.json` | Typography categories + semantic variants per form factor |
| `schema/motion-tokens.json` | Easing + duration scales per form factor |
| `schema/shadow-tokens.json` | Shadow tiers in wire `Shadow` shape |
| `schema/font-tokens.json` | Font-family registry + per-platform source metadata |
| `schema/icon-tokens.json` | Icon name → per-platform symbol mapping |
| `schema/style-tokens.json` | Variant definitions + override matrices |
| `schema/sdui-schema.json` | Wire contract (enum values, element types) |
| `server/.../LayoutTokens.java` | Server-side wire-form token constants (spacing + radius) |
| `server/.../IconTokens.java` | Server-side wire-form icon token constants |
| `server/src/main/java/com/nba/sdui/service/TypographyTokens.java` | Server-side wire-form typography constants |
| `server/src/main/java/com/nba/sdui/service/MotionTokens.java` | Server-side wire-form motion constants |
| `server/src/main/java/com/nba/sdui/service/ShadowTokens.java` | Server-side wire-form shadow constants |
| `android/sdui-core/src/main/java/com/nba/sdui/core/generated/LayoutTokenRegistry.kt` | Android codegen-baked token registry |
| `ios/Sources/SduiCore/Generated/LayoutTokenRegistry.swift` | iOS codegen-baked token registry |
| `web/src/generated/LayoutTokenRegistry.ts` | Web codegen-baked token registry |

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

## 2. Box-model cascade

The token and variant vocabulary (§§3–4) describes *what* styling values are
available. This section describes *where* in the render tree each layer of
chrome is applied — the spatial nesting order from screen to element.

### Cascade diagram

```
Screen.contentInsets        (scroll feed insets — screen-level)
  Section.surface           (section outer chrome — SectionContainer wrapper)
    AtomicElement box model  (element-level chrome — AtomicBox)
      Nested AtomicElement   (recursive — same rules)
```

| Level | What it owns | Applied by |
|---|---|---|
| `Screen.contentInsets` | Horizontal and vertical insets on the scroll feed; declared once per screen | Screen shell — not by individual sections |
| `Section.surface` (`SectionSurface`) | Section-level margin, padding, background, `cornerRadius`, shadow, border | Shared `SectionContainer` wrapper, before the section's content is rendered |
| `AtomicElement` box model (`AtomicBox`) | Per-element margin, opacity, shadow, background, border, `cornerRadius`, backdrop-filter, padding, sizing, and content | Shared `AtomicBox` helper — same implementation on web, Android, and iOS |
| Nested `AtomicElement` | Same fields; recursive to depth 6 | Same `AtomicBox` path |

### Additive composition rule

Each level wraps the next. No level overrides or cancels a parent level's
chrome. Visual properties from adjacent levels stack in their natural nesting
order: `Screen.contentInsets` creates the scroll feed inset; `surface.margin`
positions the section within that feed; `surface.padding` separates the
section's content from its surface edge; `AtomicElement.padding` adds interior
spacing inside that content area. All four accumulate — none cancels another.

Because each level is physically distinct in the render tree, there is no
precedence question between levels: `surface.padding` and a root `Container`'s
`padding` are not in conflict — they are two separate boxes, one wrapping the
other.

### What each level owns

**`Screen.contentInsets`** applies horizontal and vertical insets to the scroll
feed as a whole. These insets are declared once at the screen level and affect
every section uniformly, as if the scroll area has interior padding. The screen
shell applies them; individual section renderers must not replicate this effect
by adding their own horizontal margin.

**`Section.surface` (`SectionSurface`)** is the section's outer chrome frame.
It owns:

- `margin` — space between the section and adjacent sections or the screen edge
- `padding` — space between the surface boundary and the section's content tree
- `background` — the surface fill (solid, gradient, or image)
- `cornerRadius` — applied to the surface with overflow clip
- `shadow` — drop shadow on the surface shape
- `border` — outer stroke around the surface

`SectionContainer` — the shared wrapper that every renderer is routed through
by `SectionRouter` — reads these fields and applies them before the section
content is rendered. Semantic-section renderers (`BoxscoreTable`, `Form`,
`SeasonLeadersTable`, and all others) must not set their own outer padding,
margin, corner radius, shadow, border, or background. That responsibility
belongs exclusively to `SectionContainer` (see AGENTS.md §4.2).

**`AtomicElement` box model (`AtomicBox`)** applies per-element chrome in a
fixed order, outer to inner:

```
margin                ← sibling-to-sibling spacing
  └─ opacity          ← applied once; affects everything below
       └─ shadow      ← casts from the background shape
            └─ corner clip + background + border + backdrop-filter
                 └─ padding   ← interior padding; background extends to its edge
                      └─ sizing (width / height / sizing modes / min-max)
                           └─ content
```

This order is identical on web, Android, and iOS. Shadow casts from the
background shape, not from the padded content area. Padding lives inside the
corner clip so variant fills (e.g. `hero`) paint flush to the rounded frame.
Every rendering primitive — `Container`, `Text`, `Image`, `Button`, `Divider`,
`DisplayGrid`, `ScrollContainer`, `OverlayContainer` — applies its chrome
through `AtomicBox`. Pure layout devices — `Spacer`, `Conditional`,
`SectionSlot` — bypass it because they render no chrome of their own.

### Level-preference guidance

`Section.surface` and an `AtomicElement` root `Container` both accept `padding`
and `background`. Authoring mistakes at this boundary are the most common source
of double-chrome bugs.

**Padding:**

- Use `surface.padding` for framing that is uniform regardless of the section's
  internal layout — for example, a 16pt inset applied identically around an
  entire promotional module.
- Use the root `Container`'s `padding` for spacing that interacts with the
  section's internal layout — for example, padding that a horizontal row of
  chips or a column of rows needs to vary per edge based on content structure.
- Do not set both. Padding on `surface` and padding on the root `Container`
  nest inside one another, producing double whitespace with no single owner.

**Background:**

- Use `surface.background` for the module-frame background — the card, strip,
  or tile surface that contains the section's content tree.
- Use an inner element's `background` for a contrasting fill inside the module —
  for example, a chip, a colored tag, or an image fill on a nested container.
- Do not set the same background on both `surface` and the root `Container`.
  The inner background covers the surface's `cornerRadius` clip area and
  occludes the surface's `shadow` at the corners, producing flat paint where a
  card treatment was intended.

**The guiding distinction:** `surface` frames the module; the root `Container`
arranges content inside it. Properties that belong to the frame go on `surface`;
properties that belong to content arrangement go on the root element.

### Inter-section dividers

There is no dedicated divider primitive on `Section`. Section-boundary
separators are expressed through atomic composition, consistent with the
expressibility rule in AGENTS.md §11.1: a composer that wants a visible divider
between two sections either sets a top `border` on the lower section's
`surface.border` or emits a 1pt `Container` element at the section boundary in
the adjacent section's content tree. This keeps the chrome path single —
`SectionContainer` remains the one owner of section-level chrome — and avoids
a separate field that would create a second chrome path at the same level.

### Counterexample: double-chrome

The most common cascade error is placing the card treatment on both `surface`
and the root `Container`.

**Wrong — background set at both levels:**

```json
{
  "type": "AtomicComposite",
  "surface": {
    "background": { "color": "token:nba.bg.tertiary" },
    "cornerRadius": "token:nba.radius.lg",
    "shadow": "token:nba.shadow.md",
    "margin": { "top": 8, "bottom": 8, "start": 16, "end": 16 }
  },
  "data": {
    "ui": {
      "type": "Container",
      "background": { "color": "token:nba.bg.tertiary" },
      "padding": { "top": 16, "bottom": 16, "start": 16, "end": 16 },
      "children": ["..."]
    }
  }
}
```

What goes wrong: the root `Container`'s background paints on top of the surface
paint. The surface's `cornerRadius` clip no longer rounds the fill — the inner
background is a rectangle that extends to the container's own edges. The
surface's `shadow` is occluded at the corners by the inner rectangle. The
module renders flat instead of card-framed.

**Right — background only on surface:**

```json
{
  "type": "AtomicComposite",
  "surface": {
    "background": { "color": "token:nba.bg.tertiary" },
    "cornerRadius": "token:nba.radius.lg",
    "shadow": "token:nba.shadow.md",
    "margin": { "top": 8, "bottom": 8, "start": 16, "end": 16 },
    "padding": { "top": 16, "bottom": 16, "start": 16, "end": 16 }
  },
  "data": {
    "ui": {
      "type": "Container",
      "children": ["..."]
    }
  }
}
```

The surface owns the full card treatment — fill, radius, shadow, and framing
insets. The root `Container` owns only internal layout. Corner clip and shadow
compose correctly.

---

## 3. Token registries

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
    "web": 16
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

### 3.1 Color tokens

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
- **Team** — `nba.team.bg`, `nba.team.label`, `nba.team.accent`,
  `nba.team.accent-label`. Resolved locally from bundled palettes in
  `schema/color-tokens.json` (see **Team color resolution** below).

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

**Team color resolution:** Team brand colors are bundled in the `team`
section of `schema/color-tokens.json`. The structure is:

- **`palettes`** — hex values for each team's `primary`, `secondary`,
  and optional `tertiary` colors (30 teams total).
- **`modes`** — six mode objects (`team-background`, `team-label`,
  `team-accent--dark`, `team-accent--light`, `team-accent-label--dark`,
  `team-accent-label--light`). Each mode has a `_default` plus per-team
  overrides. Values are a palette role string, a `{ "ref": ... }` to a
  global color token, or a `{ "value": "#HEX" }` literal.
- **`semantic`** — maps the four wire tokens (`nba.team.bg`, etc.) to
  their mode lookup, with `nba.team.accent` and `nba.team.accent-label`
  splitting by dark/light theme.

Clients resolve team tokens locally from the bundled JSON using
`(teamId, theme)` — no network call. `TeamColorRegistry` on each
platform owns this resolution.

Color tokens are **not** form-factor-aware — only light/dark.

> **Workstream B note:** Color tokens remain theme-only (light/dark
> resolution). The backgrounds/shadows array work does not change the color
> token registry — it uses existing color token references inline.

### 3.2 Spacing tokens

**File:** `schema/spacing-tokens.json` (v1.0.0-kinetic) · **Server constants:** `LayoutTokens.java` ·
**Figma connection:** Figma auto-layout spacing values should use these semantic names.

Sourced from the Kinetic Design System. Phone is the base scale;
form-factor multipliers are applied by each client's `LayoutTokenResolver`.

The wire vocabulary is semantic-only:
`nba.spacing.{xs,sm,md,lg,xl,2xl}`.

| Wire token | Phone | Tablet | TV | Web |
|---|---|---|---|---|
| `nba.spacing.xs` | 2 | 2 | 4 | 2 |
| `nba.spacing.sm` | 4 | 6 | 6 | 4 |
| `nba.spacing.md` | 12 | 15 | 18 | 12 |
| `nba.spacing.lg` | 16 | 20 | 24 | 16 |
| `nba.spacing.xl` | 32 | 40 | 48 | 32 |
| `nba.spacing.2xl` | 40 | 48 | 56 | 40 |

Used for `padding`, `gap`, and `spacing` properties on atomic elements.
Composers emit these via `LayoutTokens.SPACING_*` constants.

### 3.3 Size tokens (planned)

> **Status:** Awaiting validated design input. The speculative
> `schema/size-tokens.json` was removed — values were engineering
> guesses with no design-team sign-off.

When rebuilt, this registry will cover icon sizes, logo sizes, avatar
sizes, and thumbnail dimensions with per-form-factor values.

### 3.4 Typography tokens

**File:** `schema/typography-tokens.json` · **Server constants:** `TypographyTokens.java`

The wire typography vocabulary is 16 semantic variants:

**Schema `TextVariant` enum** (used on `Text` and `LiveClock` elements):

`displayLarge`, `displayMedium`, `displaySmall`, `headlineLarge`,
`headlineMedium`, `headlineSmall`, `titleLarge`, `titleMedium`,
`titleSmall`, `bodyLarge`, `bodyMedium`, `bodySmall`, `labelLarge`,
`labelMedium`, `labelSmall`, `score`

The registry also carries 9 internal categories used to compose those
wire variants: `nba.typography.{display,headline,title,body,label,data,score,button,caption}`.
These categories are server-side design metadata and are not wire token names.

Typography size values are form-factor-aware (`phone`, `tablet`, `tv`, `web`).
The `web` column supports either a scalar or a fluid envelope
`{min, max, minVw, maxVw}` for viewport interpolation.

The `score` variant carries monospaced/tabular-numeral typography for live
scores and clocks.

**`TextWeight` values:** `regular`, `medium`, `semiBold`, `bold`

### 3.5 Corner radius tokens

**File:** `schema/corner-radius-tokens.json` (v1.0.0-kinetic) · **Server constants:** `LayoutTokens.java` ·
**Figma connection:** Figma corner radius variables should use these names.

Sourced from the Kinetic Design System.
The wire vocabulary is semantic-only:
`nba.radius.{xs,sm,md,lg,xl,2xl,full}`.

| Wire token | Phone | Tablet | TV | Web |
|---|---|---|---|---|
| `nba.radius.xs` | 2 | 2 | 2 | 2 |
| `nba.radius.sm` | 4 | 4 | 4 | 4 |
| `nba.radius.md` | 12 | 12 | 12 | 12 |
| `nba.radius.lg` | 16 | 16 | 16 | 16 |
| `nba.radius.xl` | 24 | 24 | 24 | 24 |
| `nba.radius.2xl` | 32 | 32 | 32 | 32 |
| `nba.radius.full` | 9999 | 9999 | 9999 | 9999 |

`nba.radius.full` produces a pill/circle shape. The `shape: "circle"` schema
field remains available as a shorthand for `cornerRadius: "token:nba.radius.full"`.
Composers emit these via `LayoutTokens.RADIUS_*` constants.

### 3.6 Shadow tokens

**File:** `schema/shadow-tokens.json` · **Server constants:** `ShadowTokens.java`

Four semantic tiers are built and available on the wire:
`nba.shadow.{sm,md,lg,xl}`.

Wire fields `shadow` and `shadows[]` accept either:

- A structured `Shadow` object (`type`, `color`, `radius`, `offsetX`, `offsetY`)
- A token string (`token:nba.shadow.*`)

Clients normalize both forms to a concrete `Shadow` object through
`LayoutTokenResolver.resolveShadowOrToken(...)` before rendering.

### 3.7 Motion tokens

**File:** `schema/motion-tokens.json` · **Server constants:** `MotionTokens.java`

Easing tokens are form-factor-flat:

- `nba.motion.easing.default` → `cubic-bezier(0.16, 1, 0.3, 1)`
- `nba.motion.easing.linear` → `linear`

Duration tokens are form-factor-aware:

| Wire token | Phone | Tablet | TV | Web |
|---|---|---|---|---|
| `nba.motion.duration.fast` | 150 | 180 | 250 | 200 |
| `nba.motion.duration.default` | 200 | 250 | 350 | 300 |
| `nba.motion.duration.slow` | 400 | 500 | 700 | 600 |
| `nba.motion.duration.hero` | 500 | 600 | 900 | 800 |

### 3.8 Font registry

**File:** `schema/font-tokens.json`

The font registry defines three semantic families:

- `nba.font.knockout`
- `nba.font.roboto`
- `nba.font.roboto.condensed`

Platform resolution contract:

- **Android:** bundled asset font files where required (for example Knockout),
  with system fallbacks for Roboto families.
- **iOS:** bundled or system fonts resolved by PostScript name metadata.
- **Web:** Google Fonts for Roboto families; local/bundled sources for
  Knockout where licensed.

### 3.9 Icon tokens

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

## 4. Variants

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

### 4.1 Container variants

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

### 4.2 Image variants

**`thumbnail`** — Cropped media tile with platform-native cross-fade and
placeholder reservation. All axes allow override.

### 4.3 Button variants

`primary`, `secondary`, `tertiary`, `text` — each platform maps to its
native button style.

### 4.4 Select variants (Form fields)

`dropdown` (default platform menu), `chips` (horizontal capsule row).
Applies when `FormField.fieldType == "select"`.

### 4.5 Form-factor adaptation

Variants adapt per form factor. Example for `hero` on Android 15+:

| Form factor | Adaptation |
|---|---|
| Phone | 16dp radius, 12dp padding |
| Tablet | 20dp radius, 24dp padding, larger type |
| TV | 24dp radius, 32dp padding, focusable, focus halo, 10-foot type |
| Web narrow | 16px radius, 12px padding |
| Web wide | 20px radius, 24px padding, hover lift |

---

## 5. Figma-to-wire mapping

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

## 6. Accessibility checklist for designers

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

## 7. Internationalization

**i18n core (implemented):**

- All text is server-translated per locale.
- Locale travels in the request envelope (`locale` param).
- Server-side string tables support en/es/fr; sections carry `stringTable` maps.
- Date/time formatting uses locale-aware `DateTimeFormatter` on the server.
- Edge insets use `start`/`end` (not `left`/`right`) to prepare for RTL.

**RTL layout support (planned, deferred):**

RTL layout support — including directional icon auto-mirroring (`rtl: "mirror"` in the icon registry), row container child-order reversal in RTL locales, and start/end inset flipping — is planned but not yet implemented.

---

## 8. Form factors

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
> described in §3. In practice today, spacing tokens remain theme-independent
> (they use `"*"` for the theme axis) and color UI tokens remain
> form-factor-independent (they use `"*"` for the form-factor axis). However,
> the resolver handles both axes, so future tokens that need simultaneous
> theme and form-factor variation (e.g. a surface shadow that changes between
> light/dark *and* scales with device class) are supported without further
> infrastructure changes.

---

## 9. Diagnostics

Clients emit four diagnostics for design-system mismatches:

| Diagnostic | Meaning |
|---|---|
| `variant_resolver_missing` | Variant string not in the enum for this element type |
| `variant_override_blocked` | Inline property set on a locked axis |
| `token_resolver_missing` | `token:` reference not found in registry |
| `section_decode_failed` | Section payload did not match schema |

All are non-fatal. The renderer falls back to defaults and logs the issue.

---

## 10. Gaps and completion checklist

### Figma integration

- [ ] Figma token library published with all color, spacing, size, radius,
      and typography tokens matching registry names
- [ ] Figma variables connected to `color-tokens.json` light/dark values
- [ ] Figma text styles mapped 1:1 to `TextVariant` enum
- [ ] Figma icon component library matching `icon-tokens.json` names
- [ ] Automated Figma → JSON token export pipeline (e.g. Tokens Studio)

### Token coverage

- [x] Typography token registry built (`schema/typography-tokens.json`) with
      wire `TextVariant` parity (16 semantic variants)
- [x] Shadow token registry built (`schema/shadow-tokens.json`) with 4 tiers
      (`nba.shadow.{sm,md,lg,xl}`)
- [x] Motion token registry built (`schema/motion-tokens.json`) with easing
      + duration tiers
- [x] Font registry built (`schema/font-tokens.json`) with cross-platform
      family metadata
- [x] Size token registry intentionally absent — speculative
      `schema/size-tokens.json` was removed in commit `172eb65` (2026-04-29)
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
