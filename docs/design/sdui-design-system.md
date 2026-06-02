# SDUI Design System

Reference for the token registries, variant vocabularies, and Figma-to-wire
mapping that power SDUI atomic composition.

**Audience:** designers maintaining the Figma source of truth and engineers
syncing tokens between Figma and the schema registries.

### Where this doc fits

The SDUI architecture has two complementary layers:

| Layer | What it covers | Change velocity |
|---|---|---|
| **Atomic composition** (this document) | Server-composed trees of generic primitives — containers, text, images, buttons — rendered idiomatically per platform. | Ships with server deploys. |
| **Typed semantic sections** (separate spec) | Named contracts (`BoxscoreTable`, `VideoPlayer`, `AdSlot`, `Form`, etc.) with domain-typed data, client-owned state, and platform-SDK integration. | Ships with client releases. |

Atomic composition targets the surfaces where future changes are copy edits,
layout tweaks, and cohort-scoped variants — all deployable without a client
release. Typed sections handle live game state, video, ads, billing, and dense
interactive controls where native code is required. A given screen mixes both;
this document covers the atomic layer only.

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
and spacings use semantic tokens, not raw pixels.

**Layer 2 — Variants.** A named preset that carries platform-native
treatments inline properties cannot express (materials, multi-layer shadows,
press animations, OS-adaptive surfaces). Each platform resolves variants to
its own native idiom.

**Layer 3 — Color tokens.** A `token:nba.color.*` or `token:nba.label.*`
reference that resolves to light/dark hex at render time.

### Decision rule

1. Can inline primitives (with semantic tokens) express it? → Use inline.
2. Can an ordered stack of inline primitives express it? → Use an inline
   array (`backgrounds`, `shadows`).
3. Could one more orthogonal inline property close the gap? → Extend inline.
4. Does it need a platform SDK, OS-version API, or runtime interaction
   effect? → Variant.
5. Does the color need light/dark adaptation or a stable semantic name?
   → Color token. Otherwise inline hex is fine.

---

## 2. Box-model cascade

The vocabulary in §§3–4 describes *what* styling values exist. This section
describes *where* in the render tree each layer of chrome applies.

### Cascade diagram

```
Screen.contentInsets        (scroll feed insets — screen-level)
  Section.surface           (section outer chrome)
    AtomicElement box model  (element-level chrome)
      Nested AtomicElement   (recursive — same rules)
```

| Level | What it owns |
|---|---|
| `Screen.contentInsets` | Horizontal and vertical insets on the scroll feed, declared once per screen |
| `Section.surface` | Section-level margin, padding, background, `cornerRadius`, shadow, border |
| `AtomicElement` box model | Per-element margin, opacity, shadow, background, border, `cornerRadius`, backdrop-filter, padding, sizing, content |
| Nested `AtomicElement` | Same fields; recursive |

### Additive composition rule

Each level wraps the next. No level overrides a parent. Visual properties
stack in nesting order: `Screen.contentInsets` insets the scroll feed;
`surface.margin` positions the section within that feed; `surface.padding`
separates section content from the surface edge; `AtomicElement.padding`
adds interior spacing. All accumulate — none cancels another.

### What each level owns

**`Screen.contentInsets`** applies horizontal and vertical insets to the
scroll feed as a whole. Individual section renderers must not replicate this
effect.

**`Section.surface`** is the section's outer chrome frame. It owns:

- `margin` — space between the section and its neighbors
- `padding` — space between the surface boundary and the section's content
- `background` — surface fill (solid, gradient, or image)
- `cornerRadius` — applied with overflow clip
- `shadow` — drop shadow on the surface shape
- `border` — outer stroke around the surface

Semantic-section renderers must not set their own outer padding, margin,
corner radius, shadow, border, or background; that belongs exclusively to
the shared section wrapper.

**`AtomicElement` box model** applies per-element chrome in a fixed order,
outer to inner, identical on every platform:

```
margin                ← sibling-to-sibling spacing
  └─ opacity          ← applied once; affects everything below
       └─ shadow      ← casts from the background shape
            └─ corner clip + background + border + backdrop-filter
                 └─ padding   ← interior padding; background extends to its edge
                      └─ sizing (width / height / sizing modes / min-max)
                           └─ content
```

Shadow casts from the background shape, not from the padded content area.
Padding lives inside the corner clip so variant fills paint flush to the
rounded frame. Pure layout devices (`Spacer`, `Conditional`, `SectionSlot`)
bypass the box model because they render no chrome.

### Level-preference guidance

`Section.surface` and an `AtomicElement` root `Container` both accept
`padding` and `background`. Authoring mistakes at this boundary are the most
common source of double-chrome bugs.

**Padding:**

- Use `surface.padding` for uniform framing around the entire section.
- Use the root container's `padding` for spacing that interacts with the
  section's internal layout.
- Do not set both — they nest and produce double whitespace.

**Background:**

- Use `surface.background` for the module-frame background.
- Use an inner element's `background` for a contrasting fill inside the
  module (chip, tag, image fill).
- Do not set the same background on both: the inner fill covers the
  surface's corner clip and occludes its shadow at the corners.

**Guiding distinction:** `surface` frames the module; the root container
arranges content inside it.

### Inter-section dividers

There is no dedicated divider primitive on `Section`. Section-boundary
separators are expressed through atomic composition — a top `border` on the
lower section's surface, or a 1pt container at the section boundary. This
keeps section-level chrome flowing through a single owner.

### Counterexample: double-chrome

The most common cascade error is placing the card treatment on both
`surface` and the root container.

**Wrong — background set at both levels:**

```jsonc
{
  "surface": {
    "background": { "color": "token:nba.bg.tertiary" },
    "cornerRadius": "token:nba.radius.lg",
    "shadow": "token:nba.shadow.md"
  },
  "data": {
    "ui": {
      "type": "Container",
      "background": { "color": "token:nba.bg.tertiary" },
      "padding": { "top": "token:nba.spacing.lg", "...": "..." }
    }
  }
}
```

The inner background paints over the surface, defeating the corner clip
and occluding the shadow at the corners.

**Right — surface owns the frame; the root container owns layout:**

```jsonc
{
  "surface": {
    "background": { "color": "token:nba.bg.tertiary" },
    "cornerRadius": "token:nba.radius.lg",
    "shadow": "token:nba.shadow.md",
    "padding": { "top": "token:nba.spacing.lg", "...": "..." }
  },
  "data": {
    "ui": { "type": "Container", "children": ["..."] }
  }
}
```

---

## 3. Token registries

All registries share a common matrix shape:

```jsonc
{
  "<tokenName>": {
    "<theme>": { "<formFactor>": <value> }
  }
}
```

Either axis may use `"*"` to indicate the value is independent of that
dimension. Spacing tokens are theme-independent; color UI tokens are
form-factor-independent.

**Resolution algorithm (4-step fallback):**

1. Exact match — `registry[token][theme][formFactor]`
2. Theme-wildcard — `registry[token]["*"][formFactor]`
3. Form-factor-wildcard — `registry[token][theme]["*"]`
4. Universal — `registry[token]["*"]["*"]`

Theme takes priority over form factor because brand-color correctness is a
hard constraint while form-factor adaptation is a progressive enhancement.

```jsonc
// Spacing — theme-independent, form-factor-variable
"nba.spacing.lg": {
  "*": { "phone": 16, "tablet": 20, "tv": 24, "web": 16 }
}

// Color UI token — form-factor-independent, theme-variable
"nba.bg.primary": {
  "light": { "*": "#FFFFFF" },
  "dark":  { "*": "#121212" }
}
```

### 3.1 Color tokens

Multi-tier structure:

- **Primitives** — `nba.color.<family>.<step>` with `{ light, dark }` hex
  pairs. Families: `grey`, `blue`, `red`, `green`, `orange`, `yellow`,
  `t-black`, `t-white`.
- **Semantic aliases** — named color scales pointing to primitives
  (`nba.color.primary.*`, `nba.color.secondary.*`, `nba.color.tertiary.*`,
  `nba.color.feedback.*`).
- **Labels** — purpose-defined text/icon colors
  (`nba.label.primary`, `nba.label.accent.brand`, `nba.label.accent.live`, …).
- **UI / Backgrounds** — `nba.bg.primary`, `nba.bg.secondary`, dark and
  inverted variants.
- **Button** — `nba.button.primary.label`, `.secondary.label`, `.tint.label`,
  `.focus-ring`.
- **Team** — `nba.team.bg`, `nba.team.label`, `nba.team.accent`,
  `nba.team.accent-label`, resolved locally from a bundled team palette by
  `(teamId, theme)`.

**Light/dark resolution:** clients select `light` or `dark` from the palette
primitive at render time based on OS color scheme. Inverted labels (e.g.
`onBrand`) resolve to the same hex in both modes — they mean "right text
color on a known-brightness surface," not "opposite of current mode."

Color tokens are theme-only, not form-factor-aware.

### 3.2 Spacing tokens

Wire vocabulary: `nba.spacing.{xs,sm,md,lg,xl,2xl}`. Used for `padding`,
`gap`, and `spacing` properties.

| Wire token | Phone | Tablet | TV | Web |
|---|---|---|---|---|
| `nba.spacing.xs` | 2 | 2 | 4 | 2 |
| `nba.spacing.sm` | 4 | 6 | 6 | 4 |
| `nba.spacing.md` | 12 | 15 | 18 | 12 |
| `nba.spacing.lg` | 16 | 20 | 24 | 16 |
| `nba.spacing.xl` | 32 | 40 | 48 | 32 |
| `nba.spacing.2xl` | 40 | 48 | 56 | 40 |

### 3.3 Size tokens (planned)

Awaiting validated design input. When defined, this registry will cover
icon, logo, avatar, and thumbnail dimensions per form factor.

### 3.4 Typography tokens

The wire `TextVariant` enum has 16 semantic variants (used on `Text` and
`LiveClock` elements):

`displayLarge`, `displayMedium`, `displaySmall`, `headlineLarge`,
`headlineMedium`, `headlineSmall`, `titleLarge`, `titleMedium`,
`titleSmall`, `bodyLarge`, `bodyMedium`, `bodySmall`, `labelLarge`,
`labelMedium`, `labelSmall`, `score`.

Sizes are form-factor-aware. The `web` column supports either a scalar or
a fluid envelope (`{min, max, minVw, maxVw}`) for viewport interpolation.
The `score` variant carries monospaced/tabular-numeral typography for live
scores and clocks.

**`TextWeight` values:** `regular`, `medium`, `semiBold`, `bold`.

### 3.5 Corner radius tokens

Wire vocabulary: `nba.radius.{xs,sm,md,lg,xl,2xl,full}`.

| Wire token | All form factors |
|---|---|
| `nba.radius.xs` | 2 |
| `nba.radius.sm` | 4 |
| `nba.radius.md` | 12 |
| `nba.radius.lg` | 16 |
| `nba.radius.xl` | 24 |
| `nba.radius.2xl` | 32 |
| `nba.radius.full` | 9999 (pill/circle) |

`shape: "circle"` is shorthand for `cornerRadius: "token:nba.radius.full"`.

### 3.6 Shadow tokens

Four semantic tiers: `nba.shadow.{sm,md,lg,xl}`.

Wire fields `shadow` and `shadows[]` accept either:

- A structured `Shadow` object (`type`, `color`, `radius`, `offsetX`, `offsetY`)
- A token string (`token:nba.shadow.*`)

Clients normalize both forms to a concrete `Shadow` object before rendering.

### 3.7 Motion tokens

Easing tokens are form-factor-flat:

- `nba.motion.easing.default` → `cubic-bezier(0.16, 1, 0.3, 1)`
- `nba.motion.easing.linear` → `linear`

Duration tokens are form-factor-aware (ms):

| Wire token | Phone | Tablet | TV | Web |
|---|---|---|---|---|
| `nba.motion.duration.fast` | 150 | 180 | 250 | 200 |
| `nba.motion.duration.default` | 200 | 250 | 350 | 300 |
| `nba.motion.duration.slow` | 400 | 500 | 700 | 600 |
| `nba.motion.duration.hero` | 500 | 600 | 900 | 800 |

### 3.8 Font registry

Three semantic families:

- `nba.font.knockout`
- `nba.font.roboto`
- `nba.font.roboto.condensed`

Each platform resolves these to bundled assets, system fonts, or hosted
families as appropriate for the platform.

### 3.9 Icon tokens

Icon tokens use the `sdui:` prefix and map to platform-native symbol
systems (SF Symbols on iOS, Material on Android, equivalent webfont names
on the web). The registry covers playback, navigation, status, social,
and utility icons (e.g. `sdui:play`, `sdui:back`, `sdui:live`,
`sdui:favorite`, `sdui:share`, `sdui:close`, `sdui:calendar`).

Directional icons (`back`, `forward`) auto-mirror in RTL locales.

---

## 4. Variants

Variants carry platform-native treatments that inline properties cannot
express: materials, interaction states, OS-adaptive surfaces, and
form-factor-adaptive sizing.

### Variant escalation boundary

**Stays variant-only:**

| Treatment | Why |
|---|---|
| Blend modes | Inconsistent platform support |
| Platform materials (Liquid Glass, ultra-thin material, Material You tonal surfaces) | Require OS-version-gated APIs |
| Backdrop filter (blur, saturation) | CSS-only; no equivalent native semantic |
| Runtime interaction effects (press, ripple, hover lift, focus halo) | Require platform animation/interaction APIs |

**Expressed inline:**

| Treatment | Wire representation |
|---|---|
| Solid color fill | `background` |
| Linear gradient | `backgroundGradient` |
| Image fill + overlay | `backgroundImage` |
| Multi-layer fill stack | `backgrounds` array |
| Multi-layer drop shadow | `shadows` array, `type: "drop"` |
| Inner shadow | `shadows` array, `type: "inner"` |

### 4.1 Container variants

**`hero`** — Featured content surface. Each platform realizes this with its
most expressive available surface treatment (platform material with
fallbacks, gradient + backdrop blur on the web), plus a multi-layer shadow
and a native press affordance.

Override matrix:

| Axis | Policy |
|---|---|
| `padding`, `cornerRadius`, `color`, `gap`, `opacity`, `border` | allow |
| `background` | locked on platforms where material APIs cannot be substituted with a solid fill; allow elsewhere |
| `shadow` | lock (subject to product review now that multi-layer shadows are expressible inline) |

**`grouped`** — Inset-grouped list surface. All axes allow override.

### 4.2 Image variants

**`thumbnail`** — Cropped media tile with platform-native cross-fade and
placeholder reservation.

### 4.3 Button variants

`primary`, `secondary`, `tertiary`, `text` — each platform maps to its
native button style.

### 4.4 Select variants (Form fields)

`dropdown` (platform menu), `chips` (horizontal capsule row).

### 4.5 Form-factor adaptation

Variants adapt per form factor. A `hero` surface, for example, scales radius
and padding up from phone → tablet → TV, gains a focus halo and 10-foot
type on TV, and gains hover lift on wide web.

---

## 5. Figma-to-wire mapping

### How Figma structures map to the wire format

| Figma construct | Wire equivalent |
|---|---|
| Frame with auto-layout | `Container` with `direction`, `gap`, `padding` |
| Clickable frame | `Container` with `actions` (`onActivate`) |
| Text layer | `Text` with `variant` + `weight` + `color` |
| Image fill / placed image | `Image` with `src`, `size`, `fit` |
| Component instance (Button) | `Button` with `variant` + `label` |
| Icon component | `Icon` with `token` + `size` |
| Color style | `token:nba.color.*` or `token:nba.label.*` reference |
| Spacing value | Semantic token (`token:nba.spacing.md`) |
| Corner radius | Semantic token (`token:nba.radius.lg`) or `shape: "circle"` |
| Text style | `TextVariant` enum value |
| Multiple fills (stacked) | `backgrounds` array — ordered bottom-to-top |
| Drop shadow (single or stacked) | `shadows` array with `type: "drop"` |
| Inner shadow | `shadows` array with `type: "inner"` |

### Figma naming conventions

For tokens to round-trip between Figma and the wire:

- **Color styles** use the `nba.` prefix path (`nba.label.primary`,
  `nba.bg.primary`, `nba.color.primary.50`).
- **Text styles** use the `TextVariant` name (`bodyMedium`, `titleLarge`,
  `score`).
- **Spacing variables** use the `nba.spacing` prefix.
- **Corner radius variables** use the `nba.radius` prefix.
- **Icon components** use the `sdui:` prefix.

### What Figma files should contain

1. A token library with color styles, text styles, spacing variables, and
   corner radius variables matching the registry names above.
2. Atomic primitive components with variant props matching the schema enums.
3. Composed examples showing how primitives assemble into common patterns
   (hero card, list row, promo banner).

### What to avoid in Figma

- Raw hex colors where a semantic token exists.
- Pixel values where a spacing/radius token exists.
- Custom one-off text styles not in the `TextVariant` enum.
- New variant names without a schema change first.

---

## 6. Accessibility checklist for designers

Every interactive or informational element needs an `a11y.label`.
Decorative images should be marked `hidden: true`.

| Field | Purpose | Required when |
|---|---|---|
| `label` | Screen reader name | Interactive elements, informational images |
| `hint` | Supplementary description | Complex interactions |
| `role` | `button`, `link`, `image`, `header`, `text`, `none` | Auto-inferred; override when needed |
| `hidden` | Hide from screen readers | Decorative imagery |

The full `AccessibilityProperties` schema also carries `headingLevel`,
`liveRegion`, and `sortOrder`.

---

## 7. Internationalization

**i18n core (implemented):**

- All text is server-translated per locale.
- Locale travels in the request envelope.
- Sections carry locale-scoped string tables.
- Date/time formatting is locale-aware and server-resolved.
- Edge insets use `start`/`end` (not `left`/`right`) to prepare for RTL.

**RTL layout (planned):** directional icon auto-mirroring, row container
child-order reversal, and start/end inset flipping.

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

The matrix-shaped resolver supports joint theme × form-factor variation.
In practice today, spacing tokens vary only on form factor and color tokens
vary only on theme, but tokens that need to vary on both axes (e.g. a
surface shadow that scales with device class *and* changes between
light/dark) are supported without further infrastructure changes.

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
