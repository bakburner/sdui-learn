# SDUI Design System

This document describes the SDUI (Server-Driven UI) design system end-to-end
for anyone integrating with or extending it. It is self-contained: you can
read it cold, without access to any other repository, and leave with a working
mental model of how screens are composed on the server, serialized across the
wire, and realized natively on each client.

---

## 1. Purpose & premise

The server composes screens. Clients render them.

The server emits a tree of semantic building blocks — containers, text,
images, buttons, dividers, spacers — along with named style presets and
color references. Each client takes that tree and renders it using the
platform's native UI stack: SwiftUI on iOS, Jetpack Compose on Android,
React/CSS on web.

**An iOS app is expected to look like iOS. An Android app is expected to
look like Android. A web app is expected to look like the web.** Material
surfaces, SF Pro typography, Liquid Glass on iOS 26+, Material 3 Expressive
on Android 15+, CSS `backdrop-filter` on modern browsers — these are not
accidents of client drift, they are the system working correctly.

Cross-platform visual divergence is the **point** of the design system,
not a bug in it. Pixel parity across platforms is not a goal. Screenshot
diffs across platforms are not a meaningful regression signal. Each
platform is evaluated against its own design language.

What is portable across platforms is **semantic intent**: "this is a hero
surface," "this is body-medium text," "this color is the NBA brand blue."
How that intent looks on screen is owned by the client.

---

## 2. Server-driven vs client-realized work split

A decision in the system lives in exactly one of two places: on the server,
or in the client's renderer. The table below is the rule for where each
kind of decision goes.

| The server is choosing… | Where | Mechanism | Examples |
|---|---|---|---|
| **Content selection** — which sections, copy, or CTAs appear on this screen | Server | Compose per-platform using the `X-Platform` header | Show a subscribe hero on mobile, a subscribe banner on web. Show a "download the app" card only on web. Hide a Live Activity CTA on non-iOS. |
| **Capability gating** — does this client's runtime support the delivery mechanism, SDK, or OS feature the section requires | Server | Read `capabilities.*` and `osVersion` from the request envelope and compose a compatible response | If `capabilities.sse == false`, set `refreshPolicy.type = "interval"`. Omit a section that needs iOS 17 when `osVersion < 17`. |

> **Implementation status (2026-04-25):** Content selection via
> `X-Platform` and asset-format selection are implemented. Capability
> gating is **plumbed but not yet consumed**: all three clients send
> `capabilities.sse`, `capabilities.onFocus`, and `osVersion` in the
> request envelope, and the server deserializes them into
> `SduiRequestContext`, but no composer currently reads these fields to
> branch composition. See §11 (Future Enhancements).
| **Asset-format selection** — the client needs a concrete URL or asset and only the server knows which format the platform can consume | Server | Server emits the per-platform URL or asset in the payload | iOS gets an HLS manifest; Android gets a DASH manifest. iOS gets an APNs push target; Android gets FCM. |
| **Presentation of a known semantic intent** — how a thing *looks* or *feels* once the client already knows what it is | Client | Schema carries a neutral semantic token; each platform resolves to its native idiom | `variant: "titleMedium"`, `variant: "primary"`, `variant: "hero"`, `color: "token:color.brand.nba"`, `onTap` gesture binding |

**Default posture: server.** If the question is *which* or *whether*, the
server decides. If the question is *how it looks* once the client already
knows what it is, the client decides.

**Why the split matters.** Everything the server decides can be changed in
a server deploy — minutes to hours. Everything the client decides requires
a client release and the app-store long tail — weeks to months, and older
versions persist in the wild for longer. Pushing decisions to the client
without a concrete reason is how an app ends up needing a client release
every time marketing changes a headline.

---

## 3. The three layers

The design system has three layers stacked from most expressive to most
semantic. A design requirement is satisfied by the lowest layer that can
express it.

```
┌─────────────────────────────────────────────────────────┐
│ Layer 3: Color tokens     (token:color.brand.nba)       │  ← brand & content
├─────────────────────────────────────────────────────────┤
│ Layer 2: Variants         (variant: "hero")             │  ← platform-native preset
├─────────────────────────────────────────────────────────┤
│ Layer 1: Inline primitives (padding, cornerRadius, …)   │  ← orthogonal values
├─────────────────────────────────────────────────────────┤
│ Platform default          (renderer baseline)           │  ← floor
└─────────────────────────────────────────────────────────┘
```

### Layer 1 — Inline style primitives

Granular properties declared inline on an `AtomicElement`:

- `padding` — edge insets, `{ start, end, top, bottom }` or uniform.
- `cornerRadius` — corner rounding (number).
- `shadow` — `{ color, radius, offsetX, offsetY }`.
- `gap` — child spacing inside a container.
- `opacity` — `0.0`–`1.0`.
- `border` — `{ color, width, style }`.
- `background` — a solid color, gradient, or image background descriptor.
- `badge` — a child element anchored to a corner of the parent.
- `aspectRatio` — width/height constraint for images/containers.
- `maxLines`, `textAlign`, `monospacedDigits` — text-specific.

Inline primitives are **orthogonal and composable**. `padding` and
`cornerRadius` combine cleanly; neither knows about the other. They
serialize as JSON primitives (numbers, short objects) and carry no
behavioral state.

Inline primitives are the default site for visual decisions. If a design
requirement can be expressed with inline primitives alone, it should be.

### Layer 2 — Variants

A **variant** is a named preset applied to an atomic primitive. Every
element carries a single string field:

```json
{ "type": "Container", "variant": "hero", "children": [ ... ] }
```

The variant vocabulary is per-primitive:

| Primitive | Field source | Values |
|---|---|---|
| `Text` | `TextVariant` | `displayLarge`, `displayMedium`, `displaySmall`, `headlineLarge`, `headlineMedium`, `headlineSmall`, `titleLarge`, `titleMedium`, `titleSmall`, `bodyLarge`, `bodyMedium`, `bodySmall`, `labelLarge`, `labelMedium`, `labelSmall`, `score`. |
| `Button` | `ButtonVariant` | `primary`, `secondary`, `tertiary`, `text`. |
| `Container` | `ContainerVariant` | `hero`, `grouped`. |
| `Image` | `ImageVariant` | `thumbnail`. |
| `LiveClock` | `TextVariant` (shared) | Any `TextVariant` value; defaults to `score` typography (tabular digits) when absent. |

Each vocabulary is deliberately small. A value earns its place by
clearing the inexpressibility bar: the treatment cannot be produced by
inline primitives (padding + cornerRadius + shadow + background + fit)
alone. `ContainerVariant.hero` and `ContainerVariant.grouped` both
carry platform-native materials or list idioms that inline properties
cannot reproduce; `ImageVariant.thumbnail` carries a platform-tuned
cross-fade / placeholder-reservation pattern that is likewise
inexpressible inline. Values that fail this bar stay inline — see §3
decision rule.

Variants also exist on one non-atomic carrier — the `select` flavor of
`FormField`. It obeys the same rules as primitive variants
(strict-decode at the typed field, renderer-native realization, absent
value falls through to the default) but the field is declared on the
field-data object, not on `AtomicElement`:

| Carrier | Field source | Values | Notes |
|---|---|---|---|
| `FormField` (when `fieldType == "select"`) | `SelectVariant` | `dropdown`, `chips` | `dropdown` is the platform menu (default). `chips` is a horizontally-scrollable row of tappable capsules — iOS uses `ScrollView` + `Capsule()` buttons, Android uses `LazyRow<FilterChip>`, web uses a pill row. Absent value renders as `dropdown`. |

`SelectVariant.chips` coexists with `FormField.fieldType: "radio"`.
They look superficially similar but are distinct: `radio` is a vertical
stack of labeled choices (multi-option, all visible), `chips` is a
horizontal capsule row (usually 2–5 options). No migration is planned
between them; they target different layout densities.

**What variants carry that inline primitives can't:**

- **Platform materials.** iOS `.ultraThinMaterial`, Liquid Glass (iOS 26+),
  Material 3 Expressive tonal surfaces, CSS `backdrop-filter`. These are
  not serializable as JSON values; they are API calls into the platform.
- **Multi-layer composites.** Two- or three-layer shadows stacked for
  depth, gradient overlays on top of materials, inner + outer strokes.
- **Interaction state.** Spring press animation on tap, ripple on
  Android, `:active` transform on web. Stateful effects bound to
  gesture lifecycles.
- **OS-adaptive surfaces.** `.secondarySystemGroupedBackground` on iOS
  (auto-adapts light/dark), `MaterialTheme.colorScheme.surfaceContainer`
  on Android. Colors that are resolved by the OS at render time, not
  picked by the server.
- **OS-version tiering.** A hero surface renders as Liquid Glass on iOS
  26+, `.ultraThinMaterial` on iOS 17–25, and a solid background with
  a layered shadow on older OSes — all declared under one `variant:
  "hero"` token.

### Layer 3 — Color tokens

Any color-valued property accepts either a literal hex string
(`#RRGGBB` or `#RRGGBBAA`) or a token reference:

```
"token:color.brand.nba"
"token:color.text.primary"
"token:color.feedback.error.50"
```

Tokens are two-tier:

- **Palette primitives** (`color.grey.50`, `color.blue.30`,
  `color.red.50`, …) carry a literal `{ light, dark }` hex pair.
- **Semantic aliases** (`color.brand.nba`, `color.text.primary`,
  `color.surface.canvas`) point to a palette primitive by name.

Clients resolve a token at render time:

1. If the value starts with `token:`, strip the prefix.
2. Follow the semantic alias to its palette primitive.
3. Select `light` or `dark` based on the OS color scheme
   (`prefers-color-scheme` on web, `@Environment(\.colorScheme)` on
   iOS, `isSystemInDarkTheme()` on Android).
4. Parse the hex into a native color.

**What tokens carry that inline hex can't:**

- **Light/dark pairing.** One token resolves differently depending on
  the active OS color scheme.
- **Design-system round-trip.** Figma (or whichever design source is
  canonical) exports the token registry; server and clients consume
  the same export.
- **Brand and content palettes.** Team colors, NBA brand blue,
  sponsor palettes, semantic accents (`error`, `warning`, `success`).

**Team brand colors are not design-system tokens.** Each team's primary
color is a brand asset owned by that team (e.g. BOS green #007A33, GSW
blue #1D428A). Those values live inline in composer code and are cited
against the NBA team style guide, not the color-token registry. Adding
them to the registry would falsely imply that the design system governs
them; it does not.

### Decision rule: prefer expressible over named

A design requirement is satisfied by the lowest layer that can express
it. In order:

1. **Expressibility check.** Can the treatment be produced by the
   existing inline primitives? If yes → use inline primitives.
2. **Abstraction check.** If not expressible today, could **one more
   orthogonal inline primitive** — usable across many atomic types,
   not a one-off — close the gap? If yes → extend the inline
   vocabulary.
3. **Inexpressibility check.** Only when (1) and (2) both fail —
   because the treatment needs a platform SDK, an OS-version-specific
   API, multi-layer compositing, runtime theme resolution, or
   animated interaction state — is a new variant or variant value a
   candidate.
4. **Color intent check.** For color values specifically: if the
   color has a stable semantic name (brand, content, feedback,
   surface) that should adapt to light/dark, use a token; otherwise
   inline hex is fine.

Variants are expensive because they must be realized on every platform
at every supported OS tier. Tokens are expensive because they expand
the brand-palette surface and must be removed through a deprecation
window once apps ship. Inline primitives are cheap because they
compose without coordination.

---

## 4. Precedence & resolution stack

Every visual axis on every atomic primitive resolves through the same
four-step stack. Higher-numbered steps override lower unless the
variant locks the axis.

```
4. Runtime theme resolution       ← light/dark adjustment runs last on
                                    whatever value won steps 2–3
──────────────────────────────────
3. Inline property                ← wins only if the variant's override
                                    matrix marks the axis `allow` for
                                    the current platform
──────────────────────────────────
2. Variant value                  ← inexpressible axes (material,
                                    multi-layer shadow, press animation,
                                    tier-adaptive surface) end here
──────────────────────────────────
1. Platform default               ← the baseline the client renders with
                                    no variant and no inline props
```

**No parent→child style cascade.** A `Container` variant does not style
its `Text` children. Each atomic primitive resolves its own style from
its own variant (or the absence of one) plus its own inline props plus
runtime theme. Cascading would force renderers to be stateful about
ancestor context, which breaks the "renderers are presentation-only"
contract.

Action behaviour (`onTap`, `onVisible`, etc.) *does* cascade screen →
section → subsection → atomic. Behaviour inherits; presentation does
not.

### Override matrix

Every variant declares an **override matrix**: for each visual axis,
does an inline property override the variant's default, or does the
variant's default win?

```json
"hero": {
  "overrideMatrix": {
    "padding":      "allow",
    "cornerRadius": "allow",
    "background":   { "ios": "lock", "android": "allow", "web": "allow" },
    "shadow":       "lock",
    "color":        "allow",
    "gap":          "allow",
    "opacity":      "allow",
    "border":       "allow"
  }
}
```

- **`"allow"`** — inline value overrides variant default on every
  platform.
- **`"lock"`** — variant default always wins; inline values are
  silently dropped (and a diagnostic is logged, see §7).
- **Per-platform object** — different policy per client. Above,
  `background` is locked on iOS (because the Liquid Glass /
  `.ultraThinMaterial` realization owns it and an inline solid
  color would break the material) but allowed on Android and web
  (where the variant paints a solid or gradient background that
  can be safely replaced).

Locks exist because some variants own axes that cannot be surgically
overridden without breaking the effect. You can't put a flat solid
color "into" Liquid Glass; you have to not use Liquid Glass. So
`background` is locked on iOS under `hero`.

> **Implementation status (2026-04-25):** Override matrices are
> **hardcoded in each platform's variant resolver** (e.g.
> `ContainerVariantResolver.kt`, `.swift`, `.ts`). Clients do not read
> `style-tokens.json` at runtime — the JSON file serves as the
> authoritative design-system specification and CI validation input,
> while resolvers mirror the matrix values in platform code. A future
> enhancement would have clients read the registry at runtime so matrix
> changes propagate without a client release. See §11.

### Worked example: `hero` container with inline `background`

Server emits:

```json
{
  "type": "Container",
  "variant": "hero",
  "background": "token:color.surface.promo",
  "children": [ ... ]
}
```

Per-platform resolution:

- **iOS 26+.** `hero` → Liquid Glass + 2-layer shadow + 16pt corner
  radius. `background` is `lock` on iOS, so
  `color.surface.promo` is dropped and
  `variant_override_blocked` is logged with
  `{ axis: "background", variant: "hero", platform: "ios" }`.
  The user sees the Liquid Glass surface.
- **Android 15+.** `hero` → Material 3 Expressive surface +
  tonalElevation 6dp + shape + ripple. `background` is
  `allow` on Android, so `color.surface.promo` paints over
  the tonal surface at its resolved value (grey.10 light /
  blue.10 dark, per the registry).
- **Web (modern).** `hero` → gradient raised surface +
  `backdrop-filter: blur(20px) saturate(140%)` + 16px radius +
  large box-shadow. `background` is `allow`, so the gradient
  is replaced by a solid `var(--color-surface-promo)` that
  resolves under `prefers-color-scheme`.

Same wire payload, three valid platform realizations, one
diagnostic on iOS telling the composer it tried to override
a locked axis. That diagnostic is the signal to remove the
inline property server-side — not a signal to change the lock.

---

## 5. Platform-native realization

Variants declare intent; clients realize that intent. The registry
spells out the realization per platform per OS tier.

### OS-version tier matrix

| Platform | Tiers |
|---|---|
| iOS | `26+`, `17–25`, `<17` |
| Android | `15+`, `12–14`, `<12` |
| Web | `modern`, `fallback` (capability-based via `backdrop-filter` and related feature detection — **not** browser-version based) |

**Coverage floor:** current plus one back is the tested floor on each
platform. Older OSes get a documented flat fallback. Silent rendering
failures on older OSes are not acceptable; every tier must have a
concrete realization.

**Dark-mode coverage is mandatory at every declared tier** on every
platform. A variant that only works in light mode is a bug, not a
feature.

### Example: `hero` ContainerVariant realization

| Platform / tier | Light | Dark |
|---|---|---|
| iOS 26+ | Liquid Glass + 2-layer shadow (6/3, 12/6) + spring press; 16pt radius | Liquid Glass (dark tint) + same layered shadow |
| iOS 17–25 | `.ultraThinMaterial` + gradient overlay + shadow 0.12@6/3 + spring press; 16pt | `.ultraThinMaterial` (dark) + stronger overlay + shadow 0.35@8/4 |
| iOS <17 | Solid `.secondarySystemGroupedBackground` + overlay + shadow | Same; semantic auto-adapts |
| Android 15+ | Material 3 Expressive surface + tonalElevation 6dp + expressive shape + ripple + elevation 8dp | Same; `surfaceContainer` auto-shifts for dark |
| Android 12–14 | Material You + tonalElevation 6dp + 16dp radius + ripple | Same; dynamic color handles dark |
| Android <12 | Flat Material surface + elevation 6dp + 16dp radius + ripple | Same; static M3 dark scheme |
| Web modern | Linear gradient + `backdrop-filter: blur(20px) saturate(140%)` + `box-shadow: var(--shadow-lg)` + 16px | Same tokens resolve dark via `prefers-color-scheme` |
| Web fallback | Solid raised surface + large shadow + 16px, no `backdrop-filter` | Same |

All of this sits behind one server-side token:

```json
{ "type": "Container", "variant": "hero" }
```

---

## 6. Strict decoders + renderer-layer fallback

The wire shape is **strictly decoded** on every client. If the server
sends a field the schema does not describe, or an enum value the
schema does not enumerate, decode fails loudly. This is a
forward-compatibility safety: contract violations surface as
test-visible crashes, not as silently degraded renders in production.

The one nuance: the `variant` property is typed as `string`, not as a
per-primitive enum, on the wire. Decode accepts any string; the
renderer parses it against the primitive's enum and, on an unknown
value, logs a `variant_resolver_missing` diagnostic and falls through
to the primitive default. Same for `token:` references — decode
accepts any token-shaped string; the renderer logs
`token_resolver_missing` if the registry doesn't have it and renders
a neutral fallback.

**Why the split.** Hard-typing `variant` at decode would make older
clients crash on newer variant names — the exact regression we want
to avoid as the server rolls out new variants. Keeping it as a string
at decode and parsing at render time gives clients a graceful
fallback path: an older client simply misses the new treatment, it
doesn't crash out of the section.

---

## 7. Diagnostics

Every client emits three named diagnostics with a stable payload shape.
Severity is `debug` in development builds; in release they fold into
the platform's existing binding-warning / staleness-warning channel.

| Diagnostic | When | Payload |
|---|---|---|
| `variant_resolver_missing` | `variant: "someString"` does not match any known value for this primitive's enum. | `{ variant, elementType, sectionId, atomicPath }` |
| `variant_override_blocked` | An inline property was set on an axis the variant's override matrix marks as `lock` for this platform. | `{ variant, axis, platform, sectionId, atomicPath, attemptedValue }` |
| `token_resolver_missing` | A `token:...` reference did not resolve to any entry in the bundled color-token registry. | `{ token, elementId?, axis }` |

All three are non-fatal. In every case the renderer produces something
rather than nothing — the variant falls through to the primitive
default, the token falls back to the caller's default color, the
override-blocked inline prop is simply ignored while the variant
default is used.

The diagnostics are tools for composer authors: they point at places
the server is trying to do something the variant / token vocabulary
does not support, which is usually the signal to either fix the
server-side emission or extend the vocabulary.

> **Implementation status (2026-04-25):**
> - `variant_resolver_missing` — emitted on all three platforms.
> - `variant_override_blocked` — emitted on Android; web and iOS have
>   override matrices in their resolvers but diagnostic emission needs
>   verification.
> - `token_resolver_missing` — emitted on web; Android and iOS coverage
>   needs verification.

---

## 8. Registries (two files, one model)

The design system ships two machine-readable registries:

### `style-tokens.json`

Per variant (keyed by primitive: `ContainerVariant.hero`,
`ImageVariant.thumbnail`, etc.):

- **`description` and `intent`** — human-readable prose.
- **Per-platform, per-tier realization** — `ios.26+`, `android.15+`,
  `web.modern`, each with `light` and `dark` specs that read as
  intent statements ("Liquid Glass + 2-layer shadow + spring press"),
  not as pixel-level specifications.
- **`overrideMatrix`** — per-axis policy: `"allow"`, `"lock"`, or
  `{ "ios": "lock", "android": "allow", "web": "allow" }`.
- **`evidence`** — the composer patterns or reference-app surfaces
  this variant replaces. Documentation of why this variant exists,
  for the quarterly audit.

Consumed by:

- **Clients** — to pick the platform realization at render time.
- **Server composers** — to know which inline properties the override
  matrix locks (so composers don't emit properties that will be
  silently dropped).
- **Design-system tooling** — the source of truth for what the
  vocabulary is, what each value means, and what each platform
  does with it.

### `color-tokens.json`

Two-tier color registry:

- **`palette`** — named primitives keyed as
  `color.<family>.<step>` (e.g. `color.grey.50`,
  `color.blue.30`). Each entry carries a literal `{ light, dark }`
  hex pair.
- **`semantic`** — aliases keyed by semantic path
  (e.g. `color.primary.50`, `color.brand.nba`,
  `color.text.primary`). Each entry carries `{ aliasOf: "<palette or
  semantic name>" }`. Aliases may chain through other aliases up to
  a bounded depth before ultimately resolving to a palette primitive.
- **`diagnostics`** — the `token_resolver_missing` schema used by
  clients.

Semantic layers currently shipped include:

- `color.primary.*`, `color.secondary.*`, `color.tertiary.*` — the
  neutral / brand-accent palette scales.
- `color.feedback.success.*`, `color.feedback.error.*`,
  `color.feedback.warning.*` — feedback palettes.
- `color.brand.nba`, `color.brand.live` — brand anchors.
- `color.surface.canvas`, `color.surface.raised`,
  `color.surface.sunken`, `color.surface.promo` — layered surface
  roles.
- `color.text.primary`, `color.text.secondary`, `color.text.tertiary`,
  `color.text.inverse`, `color.text.onBrand` — typography colors.
- `color.border.default`, `color.border.subtle` — border treatments.
- `color.overlay.scrim` — overlay/scrim anchor.

### Interaction between the two registries

Variants control **platform-native surface colors** (container
backgrounds, image backdrops, elevation shadows). Those colors resolve
through each client's platform semantic palette — UIKit semantic
colors, Android `MaterialTheme.colorScheme`, CSS custom properties
under `prefers-color-scheme`. They do **not** flow through
`color-tokens.json`; the variant registry owns them.

The color-token registry controls **brand and content colors** —
the text color of a headline, the background of a promo banner, the
LIVE badge pill. These are the colors a composer emits per-element
via `color`, `background`, `shadow.color`, `Divider.color`, and
similar properties.

There is no overlap. A composer never routes a variant's surface
color through a color token; a composer never routes a brand accent
through a variant. Each registry owns a disjoint slice of the color
decisions on the screen.

---

## 9. Worked example: a featured game hero

To tie the layers together, here is a single section — a featured
game hero card — traveling from the server to the screen on each
platform.

### Server emission

```json
{
  "type": "Container",
  "variant": "hero",
  "direction": "row",
  "alignment": "center",
  "crossAlignment": "center",
  "padding": { "start": 16, "end": 16, "top": 12, "bottom": 12 },
  "cornerRadius": 16,
  "actions": [
    { "trigger": "onTap", "type": "navigate", "targetUri": "nba://game/0022500001" }
  ],
  "children": [
    { "type": "Image",
      "src": "https://cdn.nba.com/logos/nba/1610612747/primary/L/logo.svg",
      "width": 48, "height": 48, "fit": "contain" },
    { "type": "Container", "direction": "column", "flex": 1,
      "children": [
        { "type": "Text", "variant": "score", "weight": "bold",
          "content": "118 - 112", "color": "token:color.text.inverse",
          "textAlign": "center" },
        { "type": "Text", "variant": "labelMedium",
          "content": "Final", "color": "token:color.text.secondary",
          "textAlign": "center" }
      ] },
    { "type": "Image",
      "src": "https://cdn.nba.com/logos/nba/1610612738/primary/L/logo.svg",
      "width": 48, "height": 48, "fit": "contain" }
  ]
}
```

Three layers visible in the payload:

- **Inline primitives** — `padding`, `cornerRadius`, `direction`,
  `alignment`, `flex`, `width`, `height`, `fit`, `textAlign`.
- **Variants** — `variant: "hero"` on the row (carries the
  platform-native material / tonal surface), `variant: "score"` on
  the game score (carries tabular-numeral typography), `variant:
  "labelMedium"` on the status line.
- **Color tokens** — `color: "token:color.text.inverse"` and
  `color: "token:color.text.secondary"` on the two text elements.

### iOS render (SwiftUI)

The `hero` container resolves to Liquid Glass on iOS 26+
(`.ultraThinMaterial` on iOS 17–25, `.secondarySystemGroupedBackground`
on older OSes), a 2-layer spring shadow, and a 16pt corner radius.
Under the override matrix `padding` is allow, so the inline
16/16/12/12 insets apply on top. `background` is locked on iOS under
`hero`, so any inline background would log
`variant_override_blocked` — this payload does not attempt one.

The team-logo images carry no variant. The inline
`width: 48, height: 48, fit: "contain"` drive a plain `Image` with
`.resizable().aspectRatio(contentMode: .fit)` inside a fixed frame;
no corner rounding is applied because no `cornerRadius` inline
property is set and no variant locks one in.

The `score` text variant realizes as `Font.system(.headline)` weight
bold with `.monospacedDigit()` so "118 - 112" keeps column alignment.
Its `color` is `token:color.text.inverse` → `color.grey.0` → `#FFFFFF`
in both light and dark (white content against the Liquid Glass
surface).

### Android render (Jetpack Compose)

`hero` resolves to a Material 3 Expressive `Surface` with
`tonalElevation = 6.dp`, expressive shape, ripple, and elevation 8dp
on Android 15+ (Material You + tonalElevation on 12–14, flat Material
surface on <12). The inline `padding` and `cornerRadius` apply per
the override matrix; `background` on Android is allow, though this
payload does not set one.

Team-logo images render with `ContentScale.Fit` inside a fixed
48×48 dp box, no clipping. `score` maps to
`MaterialTheme.typography.headlineSmall` with the NBA brand font and
a tabular-digit feature. Color token `color.text.inverse` →
`color.grey.0` → `#FFFFFF` via `ColorTokenResolver`, with
`isSystemInDarkTheme()` selecting the dark value.

### Web render (React + CSS)

`hero` resolves to a modern-tier CSS class: linear gradient
background, `backdrop-filter: blur(20px) saturate(140%)`, large
`box-shadow`, 16px radius. Fallback browsers without
`backdrop-filter` get a solid raised surface with the same shadow
tier. The inline `padding` becomes `padding: 12px 16px`. `onTap`
becomes a `role="button"` + click handler.

Team-logo images render inside a fixed-size `<img>` with `object-fit:
contain` and no border-radius. `score` resolves to the CSS typography
class for tabular-digit headline text. The color token resolves via
a utility hook that reads `prefers-color-scheme` and picks the
correct hex.

### What the reader should take away

The same wire payload produces three valid platform renders. None of
them is "wrong" relative to the others. Each one is idiomatic for its
platform. The composer wrote the screen once; the clients did the
rest.

---

## 10. Governance

Both registries are published contracts. Additions, renames, and
removals follow an explicit bar rather than a PR vibe-check.

### Adding a new variant value

Required before the value ships:

- **At least two evidenced call sites.** Composer patterns or
  reference-app surfaces that the variant demonstrably replaces. One
  site is an inline-property bag; two is a pattern worth a name.
- **Design-system sign-off.** The variant joins a published
  vocabulary; the design system owns whether it's warranted.
- **Per-platform realization notes keyed by OS-version tier.**
  Every tier declared in §5 must have a concrete realization.
  "TBD on iOS 17" is not a shipping state.
- **Dark-mode spec at every declared tier on every platform.**
- **Override-matrix entry.** Per-axis policy scalar or per-platform
  object.
- **Codegen regeneration.** Every client's typed enum updated.
- **Per-platform screenshot review.** Each client is evaluated
  against its own platform's design-system spec. No cross-platform
  pixel comparison.

### Adding a new color token

- **Semantic or brand justification.** "Designer wants this hex
  here" is not a token; "we need a second feedback error for a
  less-alarming state" is.
- **Light and dark values.** Both mandatory.
- **Registry placement.** Palette primitive for new raw values;
  semantic alias for new named roles. If it's a one-off hex with
  no reuse, it stays inline and does not become a token.

### Removal and deprecation

- **Two-release deprecation window** for any rename, removal, or
  override-matrix flip. Older clients running against a newer server
  must keep rendering something sensible; a removed token or
  variant value silently logs the appropriate diagnostic during the
  window.
- **Strict decoders make removal asymmetric.** Adding a value is
  cheap; removing one means every client in the field needs to
  have moved past the deprecation first. Design accordingly.

### Audits

- **Override-rate SLO.** Any override-matrix axis with >30%
  override rate gets reviewed. Either the variant default is wrong
  (fix the variant) or the variant needs to split into two (one
  that owns that axis, one that doesn't).
- **Quarterly audit.** Composer call sites are diffed against
  registry usage. Three or more repeated inline patterns become
  new-variant or new-token candidates — the expressibility check
  in §3 runs again with the new evidence in hand.

### Why the bar is this high

Every variant value and every token is a **client-release coupling
point**. Each one expands the wire-level contract. Strict decoders
and the app-store long tail make removal a deprecation exercise,
not a code change. Keeping the vocabularies tight is what keeps the
server deploy — not the client release — the mechanism for most
design changes. That is the primary KPI of the whole system.

---

## 11. Future Enhancements

The following design-system capabilities are architecturally planned
but not yet implemented in code. They are listed here to prevent
misinterpretation as shipped features.

| Enhancement | Current state | What ships today | What's needed |
|---|---|---|---|
| **Server-side capability gating** | Plumbed end-to-end | Clients send `capabilities.sse`, `capabilities.onFocus`, and `osVersion` in the request envelope; server deserializes into `SduiRequestContext.Platform.Capabilities` | Composers need to read these fields to branch composition (e.g. fall back to `refreshPolicy.type = "interval"` when `sse == false`, omit sections requiring a minimum OS version) |
| **Server-side `osVersion` branching** | Plumbed end-to-end | Clients send `osVersion`; server deserializes it | Composers need to read `osVersion` to gate sections or choose OS-tier-appropriate payloads |
| **Runtime-read override matrices** | Hardcoded in resolvers | Each platform's variant resolver (`ContainerVariantResolver`, `ImageVariantResolver`) hardcodes the matrix from `style-tokens.json` | Clients could read `style-tokens.json` (bundled or fetched) at runtime so override-matrix changes propagate without a client release |
| **Full diagnostic coverage** | Partial | `variant_resolver_missing` on all 3 platforms; `variant_override_blocked` on Android; `token_resolver_missing` on web | Verify and ship `variant_override_blocked` on web and iOS; verify `token_resolver_missing` on Android and iOS |
| **Figma export pipeline** | Spec'd, not built | Color-token and style-token registries are ref-app-seeded with a Kinetic-compatible shape | Design-system team ships Figma exports that replace `color-tokens.json` and `style-tokens.json` wholesale |
| **Override-rate SLO telemetry** | Spec'd, not built | `variant_override_blocked` diagnostic exists (Android) | Production telemetry pipeline to compute per-axis override rates for the quarterly audit (§10) |

When any of these enhancements ship, remove the corresponding row from
this table and update the implementation-status callouts in the
relevant sections above.
