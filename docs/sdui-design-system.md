# SDUI Design System

This document describes the SDUI (Server-Driven UI) design system end-to-end
for anyone integrating with or extending it. It is self-contained: you can
read it cold, without access to any other repository, and leave with a
working mental model of how screens are composed on the server, serialized
across the wire, and realized natively on each client.

This document is the contract for the **atomic composition layer**. The
parallel **typed semantic section** layer is summarized in §1 and described
in detail in `sdui-technical-proposal.md` §2a–§2b.

---

## 0. Goal and scope

### What this system is for

The SDUI architecture exists to **reduce the share of UI/UX changes that
require a client release**. Every change that ships via server deploy is a
change that does not wait on the app-store long tail (multi-week submission
window, fragmented client-version distribution, locked rollback timelines).
That share — measured as a percentage — is the primary KPI of the system.

The architecture has two complementary mechanisms:

| Mechanism | Purpose | Velocity profile |
|---|---|---|
| **Atomic composition** (this document) | Server-composed trees of generic primitives — containers, text, images, buttons — rendered idiomatically per platform. | Changes ship with server deploys. Used for content surfaces where the server already owns the data and the layout is presentation of that data. |
| **Typed semantic sections** (separate spec) | Named contracts (`BoxscoreTable`, `TabGroup`, `VideoPlayer`, `AdSlot`, `SubscribeHero`, …) with domain-typed data, client-owned state, and per-platform implementations. | Changes ship with client releases. Used where the client owns network-driven lifecycle, platform-SDK integration, complex gestures, focus management, or interaction state that the wire format cannot express. |

Atomic composition targets **the 60% of feed/list/editorial/marketing/promo
surfaces** where future changes are reordering, copy edits, layout tweaks,
new modules in a known vocabulary, and cohort-scoped variants. Typed
sections handle the remaining 40% — live game state, video playback, ad
SDKs, billing flows, and dense interactive controls — because pushing them
into atomic composition would either bloat the wire contract beyond
usefulness or produce a worse experience than native code.

A given screen mixes both. The For You feed is a mix of typed
`ScoreboardCarousel` (live scores, focus-aware on TV) plus atomic
`AtomicComposite` editorial promos (server-emitted layout, swap-in-a-deploy).

### What this system is *not* for

- It is **not** a system for rendering identical UI across platforms. iOS
  must look like iOS. Android must look like Android. Web must look like
  web. TV must look like TV. Cross-platform visual divergence is the
  design system working correctly, not a bug in it.
- It is **not** a webview wrapper. There is no shared rendering engine.
  Each platform parses the wire contract and renders with its own native
  UI stack (SwiftUI, Jetpack Compose, React, etc.).
- It is **not** a way to escape native development. Typed sections, focus
  management, accessibility, localization, and platform-SDK integrations
  remain native work. SDUI just stops the server from being the bottleneck
  for the surfaces that don't need that work.

### How to evaluate every architectural decision in this document

> Does this decision protect or expand the share of changes that ship via
> server deploy?

If yes, it earns its place. If no, it goes into typed sections or gets cut.

---

## 1. The two layers, the three sub-layers

The system has two top-level layers: **atomic composition** (this document)
and **typed semantic sections** (briefly summarized below; full spec
elsewhere).

Atomic composition itself is stratified into three sub-layers, ordered
from most expressive to most semantic:

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

A design requirement is satisfied by the lowest sub-layer that can express
it. The decision rule is in §3.

### Typed semantic sections (out of scope for this doc, in for context)

A typed section has a named `type` (e.g. `BoxscoreTable`, `Form`,
`VideoPlayer`, `AdSlot`, `TabGroup`, `SubscribeHero`, `ScoreboardCarousel`).
The wire format carries domain-typed data. Each platform ships a renderer
that owns that section's state model, animation, focus behavior, and
platform-SDK integration. Typed sections handle:

- **Network-driven lifecycle** — SSE subscriptions, polling cadence, live
  reconnect logic, swapping UI variants based on live data state.
- **Platform-SDK integration** — billing (Google Play / StoreKit), ads
  (GAM), video (AVPlayer / ExoPlayer / HLS.js), maps, sensors.
- **Complex client-owned interaction state** — sort, frozen-column scroll
  sync, form input accumulation, tab selection, pagination.
- **TV focus management** — focus groups, focus-traversal order, focus
  visuals, D-pad navigation between non-linear content.
- **Section containers** — sections that nest other sections (`TabGroup`).
- **Custom gestures and animation** — pinch-to-zoom, swipe-to-dismiss,
  drag-to-reorder, custom transitions.

A surface is **atomic composition** when it has none of the above — when
it is stateless presentation of server-owned content, with linear focus
behavior fine for TV remotes, and visual variants that are deterministic
from server-sent data fields alone. The decision tree lives in
`sdui-technical-proposal.md` §2b.

The rest of this document is about atomic composition only.

---

## 2. Server-driven vs client-realized work split

A decision in the system lives in exactly one of two places: on the
server, or in the client's renderer. The table below is the rule for
where each kind of decision goes.

| The server is choosing… | Where | Mechanism | Examples |
|---|---|---|---|
| **Content selection** — which sections, copy, or CTAs appear on this screen | Server | Compose per-platform-family using the `X-Platform` and `X-Form-Factor` headers | Show a subscribe hero on phone, a subscribe banner on web. Show a "download the app" card only on web. Hide a Live Activity CTA on non-iOS. |
| **Asset-format selection** — the client needs a concrete URL or asset and only the server knows which format the platform can consume | Server | Server emits the per-platform URL or asset in the payload | iOS gets an HLS manifest; Android gets a DASH manifest. iOS gets an APNs push target; Android gets FCM. |
| **Presentation of a known semantic intent** — how a thing looks or feels once the client already knows what it is | Client | Schema carries a neutral semantic token; each platform resolves to its native idiom | `variant: "titleMedium"`, `variant: "primary"`, `variant: "hero"`, `color: "token:color.brand.nba"`, `onActivate` action binding |
| **Form-factor and input-modality realization** — what something looks and behaves like on phone vs tablet vs TV vs web | Client | Schema carries semantic intent; each client resolves tokens against its own form-factor and input-modality classification | Same `spacing.md` resolves to 12 on phone, 16 on tablet, 24 on TV. Same `onActivate` resolves to tap on touch, click on web, D-pad center on TV. |

**Default posture: server.** If the question is *which* or *whether*, the
server decides. If the question is *how it looks once the client already
knows what it is*, the client decides.

**Why the split matters.** Everything the server decides can be changed
in a server deploy — minutes to hours. Everything the client decides
requires a client release and the app-store long tail — weeks to months,
and older versions persist in the wild for longer. Pushing decisions to
the client without a concrete reason is how an app ends up needing a
client release every time marketing changes a headline.

---

## 3. Atomic composition: the three sub-layers

### Layer 1 — Inline style primitives

Granular properties declared inline on an `AtomicElement`:

- `padding` — edge insets, `{ start, end, top, bottom }` or uniform.
  All edge insets are RTL-aware (`start`/`end` flip to right/left in
  right-to-left locales).
- `cornerRadius` — corner rounding (semantic token or number; see §3.6).
- `shadow` — `{ color, radius, offsetX, offsetY }`.
- `gap` — child spacing inside a container (semantic token or number).
- `opacity` — 0.0–1.0.
- `border` — `{ color, width, style }`.
- `background` — a solid color, gradient, or image background descriptor.
- `badge` — a child element anchored to a corner of the parent.
- `aspectRatio` — width/height constraint for images/containers.
- `size` — semantic size token; see §3.6.
- `maxLines`, `textAlign`, `monospacedDigits` — text-specific.

Inline primitives are orthogonal and composable. `padding` and
`cornerRadius` combine cleanly; neither knows about the other. They
serialize as JSON primitives (numbers, short objects, or token
references) and carry no behavioral state.

Inline primitives are the default site for visual decisions. If a design
requirement can be expressed with inline primitives alone, it should be.

### Layer 2 — Variants

A variant is a named preset applied to an atomic primitive. Every element
carries a single string field:

```json
{ "type": "Container", "variant": "hero", "children": [ ... ] }
```

The variant vocabulary is per-primitive:

| Primitive | Field source | Values |
|---|---|---|
| Text | `TextVariant` | `displayLarge`, `displayMedium`, `displaySmall`, `headlineLarge`, `headlineMedium`, `headlineSmall`, `titleLarge`, `titleMedium`, `titleSmall`, `bodyLarge`, `bodyMedium`, `bodySmall`, `labelLarge`, `labelMedium`, `labelSmall`, `score`. |
| Button | `ButtonVariant` | `primary`, `secondary`, `tertiary`, `text`. |
| Container | `ContainerVariant` | `hero`, `grouped`. |
| Image | `ImageVariant` | `thumbnail`. |
| LiveClock | `TextVariant` (shared) | Any `TextVariant` value; defaults to score typography (tabular digits) when absent. |

Each vocabulary is deliberately small. A value earns its place by
clearing the **inexpressibility bar**: the treatment cannot be produced
by inline primitives (padding + cornerRadius + shadow + background +
fit) alone. `ContainerVariant.hero` and `ContainerVariant.grouped` both
carry platform-native materials or list idioms that inline properties
cannot reproduce; `ImageVariant.thumbnail` carries a platform-tuned
cross-fade / placeholder-reservation pattern that is likewise
inexpressible inline. Values that fail this bar stay inline — see §3.5
decision rule.

Variants also exist on one non-atomic carrier — the `select` flavor of
`FormField`. It obeys the same rules as primitive variants
(strict-decode at the typed field, renderer-native realization, absent
value falls through to the default) but the field is declared on the
field-data object, not on `AtomicElement`:

| Carrier | Field source | Values | Notes |
|---|---|---|---|
| `FormField` (when `fieldType == "select"`) | `SelectVariant` | `dropdown`, `chips` | `dropdown` is the platform menu (default). `chips` is a horizontally-scrollable row of tappable capsules — iOS uses `ScrollView + Capsule()` buttons, Android uses `LazyRow<FilterChip>`, web uses a pill row. Absent value renders as `dropdown`. |

`SelectVariant.chips` coexists with `FormField.fieldType: "radio"`. They
look superficially similar but are distinct: `radio` is a vertical
stack of labeled choices (multi-option, all visible), `chips` is a
horizontal capsule row (usually 2–5 options). No migration is planned
between them; they target different layout densities.

**What variants carry that inline primitives can't:**

- **Platform materials.** iOS `.ultraThinMaterial`, Liquid Glass (iOS
  26+), Material 3 Expressive tonal surfaces, CSS `backdrop-filter`.
  These are not serializable as JSON values; they are API calls into
  the platform.
- **Multi-layer composites.** Two- or three-layer shadows stacked for
  depth, gradient overlays on top of materials, inner + outer strokes.
- **Interaction state.** Spring press animation on tap, ripple on
  Android, `:active` transform on web. Stateful effects bound to
  gesture lifecycles.
- **OS-adaptive surfaces.** `.secondarySystemGroupedBackground` on iOS
  (auto-adapts light/dark), `MaterialTheme.colorScheme.surfaceContainer`
  on Android. Colors that are resolved by the OS at render time, not
  picked by the server.
- **OS-version tiering.** A `hero` surface renders as Liquid Glass on
  iOS 26+, `.ultraThinMaterial` on iOS 17–25, and a solid background
  with a layered shadow on older OSes — all declared under one
  `variant: "hero"` token.
- **Form-factor tiering.** A `hero` surface adapts touch-target sizing
  on phone, larger insets on tablet, focusable + overscan-safe on TV
  — all declared under one `variant: "hero"` token. See §5.5.

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
3. Select **light** or **dark** based on the OS color scheme
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
  sponsor palettes, semantic accents (error, warning, success).

#### A note on text-on-surface tokens (`text.inverse`, `text.onBrand`)

Some text tokens — `color.text.inverse`, `color.text.onBrand` — resolve
to the *same* hex in both light and dark modes (typically `#FFFFFF` or
`#000000`). This is intentional. They are not "opposite of light/dark";
they are "the right text color when sitting on a surface that is
already a particular brightness." When a composer emits a `Text` inside
a `hero` container with a Liquid Glass / dark-tinted surface, the
correct token is `text.inverse` (white in both modes) because the
surface itself is dark in both modes.

This is *compositional reasoning by the composer*, not a parent→child
style cascade. Renderers do not inspect ancestor context. The composer
decides which token applies based on the surface it knows it is
emitting onto.

Tokens that do flip light/dark (`text.primary`, `text.secondary`,
`surface.canvas`) are documented as such in `color-tokens.json`.

#### Team brand colors

Team brand colors are not design-system tokens. Each team's primary
color is a brand asset owned by that team (e.g. BOS green `#007A33`,
GSW blue `#1D428A`). Those values live inline in composer code and are
cited against the NBA team style guide, not the color-token registry.
Adding them to the registry would falsely imply that the design system
governs them; it does not.

### Layer 1 (revisited): Semantic size and spacing tokens

This is the section that protects the 60% from collapsing across form
factors. **Servers do not emit raw pixel numbers for size, spacing, or
radius.** The schema accepts:

- **Semantic tokens** for the common cases:
  - `size: "avatar.lg"` — resolves to per-platform per-form-factor px.
  - `spacing: "md"` or `padding: "md"` — resolves to 12dp on phone,
    16dp on tablet, 24dp on TV (per the `spacing-tokens.json` registry).
  - `cornerRadius: "lg"` — resolves to a token, not a pixel value.
  - `shape: "circle"` — clients compute `radius = width/2` automatically.
- **Aspect ratios** for media: `aspectRatio: "16:9"` instead of
  `width: 80, height: 45`.
- **Constraint primitives** for breathing layouts:
  - `size: { width: "fill", height: "wrap" }`
  - `flex: 1`, `minSize: { width: "spacing.xxl" }`, `maxSize: …`
- **Raw numbers** — accepted only as a temporary escape hatch during
  migration. Lint emits a warning. Production payloads must use tokens.

Semantic size tokens are realized per-form-factor by the client (see
§5.5). The server does not know whether a TV or a phone is rendering
the payload; it just emits `size: "avatar.lg"` and trusts the client to
pick the right pixel value.

**The size and spacing token registries** (`size-tokens.json`,
`spacing-tokens.json`) ship alongside `color-tokens.json` and follow
the same two-tier structure (palette primitives + semantic aliases).
Each token has per-form-factor entries:

```json
{
  "spacing.md": {
    "phone":  { "value": 12 },
    "tablet": { "value": 16 },
    "tv":     { "value": 24 },
    "web.narrow": { "value": 12 },
    "web.wide":   { "value": 16 }
  }
}
```

### 3.5 Decision rule: prefer expressible over named

A design requirement is satisfied by the lowest layer that can express
it. In order:

1. **Expressibility check.** Can the treatment be produced by the
   existing inline primitives (with semantic size/spacing tokens)? If
   yes → use inline primitives.
2. **Abstraction check.** If not expressible today, could one more
   orthogonal inline primitive — usable across many atomic types,
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
5. **Section promotion check.** If a treatment requires
   network-driven lifecycle, platform-SDK integration, complex
   client-owned state, TV-grade focus management, or custom gesture
   handling — promote it out of atomic composition into a typed
   semantic section. Do not extend the variant vocabulary to cover
   stateful behavior.

Variants are expensive because they must be realized on every platform
at every supported OS tier *and* every supported form factor. Tokens
are expensive because they expand the brand-palette surface and must
be removed through a deprecation window once apps ship. Inline
primitives are cheap because they compose without coordination.

### 3.6 Lint rules (enforced in CI)

The schema validation pipeline rejects:

- Bare integers for `width`, `height`, `cornerRadius`, `padding.*`,
  `gap`, `spacing.*` outside the migration-escape-hatch window.
- Hex colors in any field that has a semantic-token alternative.
- Variant values not in the per-primitive enum.
- Token references not present in the bundled registry.
- `onTap` triggers (deprecated; use `onActivate` — see §4).

CI also surfaces (warns, does not fail):

- Variants whose override matrix shows >30% override rate over the
  last quarter (see §10).
- Tokens that have been referenced fewer than 5 times across all
  composers in the last quarter.

---

## 4. Action vocabulary (platform-neutral)

Atomic primitives carry zero or more `actions`. Each action has a
`trigger`, a `type`, and type-specific fields.

### Triggers

The action vocabulary is **platform-neutral**. Triggers describe user
intent, not input device. Each client maps each trigger to its
appropriate platform input(s).

| Trigger | Fires when (per platform) |
|---|---|
| `onActivate` | iOS: tap, VoiceOver double-tap, Switch Control activation. Android: tap, TalkBack double-tap. Web: click, Enter, Space. TV: D-pad center / select button. |
| `onLongPress` | iOS/Android: long press. Web: contextmenu. TV: D-pad long press (where supported; otherwise no-op). |
| `onFocus` | TV: focus enters element. Web: keyboard focus enters element. Phone/tablet: no-op (touch has no hover). |
| `onBlur` | TV: focus leaves element. Web: keyboard focus leaves element. Phone/tablet: no-op. |
| `onVisible` | All platforms: element enters viewport (impression tracking). |
| `onSwipe` | iOS/Android: directional swipe. Web: not bound. TV: not bound. |
| `onSubmit` | All platforms: form submission via Enter key, return key, or submit button. |

#### Migration note: `onTap` → `onActivate`

`onTap` is the legacy trigger name. It is deprecated and CI emits a
warning when servers emit it. Clients accept `onTap` as an alias for
`onActivate` during the deprecation window (two release cycles); after
that, `onTap` is rejected at decode.

The reason for the rename: `onTap` is a touch-only verb. TV remotes
don't tap. Web users don't tap. Switch Control users don't tap.
`onActivate` is platform-neutral and lets each client map the abstract
intent to its native input vocabulary. This is the same model the
schema already uses for visual treatments (server emits intent, client
realizes it natively).

### Action types

Type, fields, and semantics are unchanged from the existing spec — see
`sdui-technical-proposal.md` §4. The types are: `navigate`,
`fireAndForget`, `mutate`, `dismiss`, `refresh`, `toast`.

### Precedence

Nested/subsection > section > screen-default. A single trigger can
execute multiple actions in sequence. Failure semantics
(`onFailure`, `failureFeedback`) are unchanged.

---

## 5. Precedence & resolution stack

Every visual axis on every atomic primitive resolves through the same
five-step stack. Higher-numbered steps override lower unless the
variant locks the axis.

```
5. Runtime theme resolution       ← light/dark adjustment runs last on
                                    whatever value won steps 2–4
──────────────────────────────────
4. Form-factor token resolution   ← semantic tokens resolve to the
                                    form-factor-appropriate value
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

**No parent→child renderer cascade.** A `Container` variant does not
style its `Text` children. Each atomic primitive resolves its own style
from its own variant (or the absence of one) plus its own inline props
plus runtime theme. Cascading would force renderers to be stateful
about ancestor context, which breaks the "renderers are
presentation-only" contract.

Action behavior (`onActivate`, `onVisible`, etc.) does cascade
screen → section → subsection → atomic. Behavior inherits;
presentation does not.

### Override matrix

Every variant declares an override matrix: for each visual axis, does
an inline property override the variant's default, or does the
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

- `"allow"` — inline value overrides variant default on every platform.
- `"lock"` — variant default always wins; inline values are dropped
  (and a `variant_override_blocked` diagnostic is logged — see §7).
- **Per-platform object** — different policy per client. Above,
  `background` is locked on iOS (because the Liquid Glass /
  `.ultraThinMaterial` realization owns it and an inline solid color
  would break the material) but allowed on Android and web (where the
  variant paints a solid or gradient background that can be safely
  replaced).

Locks exist because some variants own axes that cannot be surgically
overridden without breaking the effect. You can't put a flat solid
color *into* Liquid Glass; you have to not use Liquid Glass. So
`background` is locked on iOS under `hero`.

Override matrices are hardcoded in each platform's variant resolver
(`ContainerVariantResolver.kt`, `.swift`, `.ts`). The JSON file
(`style-tokens.json`) serves as the authoritative design-system
specification and CI validation input; resolvers mirror the matrix
values in platform code.

---

## 5.5. Form factor and input modality

Form factor is a first-class axis of the design system, treated the
same way as platform and OS tier: **the server owns intent; the client
owns realization.** Without this axis, the 60% atomic-composition
target on phone collapses to ~40% on tablet and ~20% on TV.

### Form-factor classes

The system supports the following form-factor classes. Each client
classifies itself at render time.

| Class | Example device | Primary input | Notable constraints |
|---|---|---|---|
| `phone` | iPhone, Android phone (portrait) | touch | 320–430pt width; one-handed reach matters; no hover |
| `phone.landscape` | Same in landscape orientation | touch | Wide aspect; landscape-aware layouts |
| `tablet` | iPad, Android tablet, foldable unfolded | touch + occasional pointer | 600–1300pt width; multi-column layouts; split-screen possible |
| `tv` | tvOS, Fire TV, Android TV, Roku | D-pad remote | 1920×1080+ at 10-foot viewing distance; focus-driven; no touch; overscan-safe regions |
| `web.narrow` | Browser <768px | mouse + touch + keyboard | Mobile-web breakpoint; URL bar present; no native gestures |
| `web.wide` | Browser ≥768px | mouse + keyboard + occasional touch | Hover states meaningful; right-click; scroll wheel |

Foldable devices in their *folded* state classify as `phone`; in their
*unfolded* state classify as `tablet`. Connected devices (Apple TV, Fire
TV stick, Roku) all classify as `tv`. Future classes (watch, car)
follow the same pattern when added.

### What the client owns per form factor

Each client, knowing its own classification, owns:

- **Token resolution.** `spacing.md` resolves to 12 on phone, 16 on
  tablet, 24 on TV. `typography.bodyMedium` resolves to 14pt on phone,
  16pt on tablet, 22pt on TV. The atomic tree from the server stays
  the same; the client picks values from the form-factor-aware
  registries.
- **Touch-target sizing.** Phone 44pt minimum. Tablet 44pt. TV
  focusable elements scale up under focus. Web hover/click targets
  follow platform conventions.
- **Focus management for the linear case.** Atomic composition
  guarantees focusability: every interactive element is reachable by
  D-pad in document order. *Non-linear* focus (jumping between cards
  in a grid, focus trapping in modals, custom focus-traversal order)
  is a typed-section concern — atomic composites with grids on TV
  fall back to row-major D-pad traversal.
- **Overscan-safe regions on TV.** TV clients add a 5% safe-area
  inset to the root atomic tree automatically. Servers do not emit
  TV-specific padding.
- **Input-modality adaptation.** Web hover effects, TV focus halos,
  iOS press-down animation — all client-owned.
- **Responsive layout decisions** within an atomic primitive. A
  `ScrollContainer direction="row"` on a phone may render as a grid
  on web.wide if the variant declares responsive behavior (see
  §5.5.1). The wire payload does not branch.

### What the server owns per form factor

The server owns **content selection** — *which* sections appear, in
*what* order, and *what* copy they carry. Form-factor-aware composition
is signaled via the request envelope:

```json
{
  "platform": {
    "name": "android",
    "deviceClass": "tv",
    "formFactor": "tv"
  }
}
```

The composition service routes to the appropriate composer (mobile,
web, TV) based on `formFactor`. The TV composer:

- Drops surfaces that don't make sense in 10-foot UI (dense data
  tables, paginated forms with many fields).
- Adds surfaces that *do* make sense for TV (large-art carousels,
  hero modules with high-contrast typography).
- Reorders content to favor focus-friendly linear flow.
- Selects different action targets where TV requires a different
  destination (e.g. "open in browser" doesn't exist on Roku).

The server does **not** emit form-factor-specific pixel values, fonts,
or layouts within an atomic tree. The client resolves tokens.

### 5.5.1 Variant realization across form factors

Variants extend the per-platform per-OS-tier realization matrix in §6
with a form-factor dimension. A `hero` ContainerVariant on Android 15+
realizes:

| Form factor | Realization |
|---|---|
| `phone` | Material 3 Expressive surface, tonalElevation 6dp, ripple, 16dp radius, 12dp padding |
| `tablet` | Same surface; 24dp padding, 20dp radius, larger typography variants |
| `tv` | Focusable surface; focus halo overlay, 32dp padding, 24dp radius, focused-state scale 1.05, 10-foot type sizes |
| `web.narrow` | Linear gradient + backdrop-filter; 16px radius, 12px padding |
| `web.wide` | Same; 24px padding, 20px radius, hover lift effect |

These are realization details owned by the client. The server emits
`variant: "hero"` and stops.

### 5.5.2 Token registries are form-factor-aware

The size, spacing, typography, and corner-radius registries each have
per-form-factor values. The color registry is *not* form-factor-aware
— colors don't change between phone and tablet (only between light
and dark, which is OS-mode, not form-factor).

```
size-tokens.json         ← per-form-factor
spacing-tokens.json      ← per-form-factor
typography-tokens.json   ← per-form-factor
corner-radius-tokens.json ← per-form-factor
shadow-tokens.json       ← per-form-factor
color-tokens.json        ← per-OS-color-scheme (light/dark)
```

---

## 6. Accessibility

Atomic primitives must carry sufficient semantic information for each
platform to produce a usable accessible experience. **Floor
requirements** (must be in the schema) are owned by atomic
composition; **stateful a11y** (live regions, custom focus-traversal
order, custom rotors, dynamic announcements) is owned by typed
sections.

### `a11y` block on `AtomicElement`

Every atomic element accepts an optional `a11y` object:

```json
{
  "type": "Image",
  "src": "https://cdn.nba.com/logos/.../1610612747.png",
  "size": "logo.team.md",
  "a11y": {
    "label": "Los Angeles Lakers logo",
    "hidden": false,
    "role": "image"
  }
}
```

Fields:

| Field | Purpose | Default |
|---|---|---|
| `label` | Accessible name. Required on interactive elements; required on images that carry information. | None |
| `hint` | Supplementary description. | None |
| `role` | Semantic role: `button`, `link`, `image`, `header`, `text`, `none`. Each platform maps to its native a11y role vocabulary. | Inferred from element type and presence of `actions` |
| `hidden` | If `true`, the element is hidden from a11y tree (decorative imagery, redundant text). | `false` |
| `liveRegion` | `polite` or `assertive`. Informs the platform that this text may update and the update should be announced. Use sparingly. | None |

### Inferred defaults

If `a11y` is absent, clients infer:

- `Text` → role `text`, label = `content`.
- `Image` with no `a11y` → role `image`, label = `alt` if present, else
  `hidden: true` (treated as decorative). **Composers must set
  `a11y.label` on any image that conveys information.** CI lints for
  `Image` elements with `actions` but no `a11y.label`.
- `Button`, or any element with `actions` → role `button`, label =
  `label` field if `Button`, else requires `a11y.label`.
- `Container` → role `none` unless it has `actions`, in which case
  `button` and `a11y.label` is required.

### What atomic composition does *not* cover

- **Live region announcements for dynamic content** (live scores
  ticking up). This requires typed sections that own the data binding
  and announce changes through platform a11y APIs.
- **Custom focus-traversal order** for non-linear layouts. Atomic
  composition guarantees document-order traversal; anything else is
  a typed section.
- **Custom rotor / explore-by-touch behavior.** Typed section.
- **Accessibility-driven content variants.** If the surface needs to
  show different content under VoiceOver / TalkBack, that's a typed
  section decision (or a server composition decision based on a
  client-advertised capability).

### Platform realization

| Platform | Realization |
|---|---|
| iOS | `accessibilityLabel`, `accessibilityHint`, `accessibilityTraits`, `accessibilityElementsHidden`, `accessibilityLiveRegion` |
| Android | `Modifier.semantics { contentDescription = …; role = …; liveRegion = … }` |
| Web | `aria-label`, `aria-describedby`, `role`, `aria-hidden`, `aria-live` |

---

## 7. Internationalization

The composition server pre-translates all text in the initial response
based on locale. Atomic composition adds two capabilities on top of
the existing typed-section i18n model:

### 7.1 Locale-aware composition

The request envelope carries `locale` (e.g. `en-US`, `es-MX`, `ar-SA`).
The composition service:

- Selects localized strings from the content database for each `Text`
  in the atomic tree.
- Emits localized image assets where applicable (`src` fields can
  vary per locale).
- Selects locale-appropriate number, date, and time formats for
  any pre-formatted strings the server emits.

### 7.2 RTL (right-to-left) layout

Atomic composition is RTL-aware by default:

- All edge insets use `start`/`end` rather than `left`/`right`.
  Clients flip these per layout direction.
- All `direction: "row"` containers reverse child order in RTL
  locales (clients handle this; servers do not emit different
  payloads per direction).
- `ScrollContainer direction: "row"` scrolls right-to-left in RTL.
- Icons that are *directional* (back chevron, forward arrow,
  external-link icon, undo) ship with an `rtl: "mirror"` flag in
  the icon registry; clients automatically mirror them. Icons that
  are *not* directional (logos, glyphs, NBA brand marks) do not
  mirror.
- Bidi text (mixed LTR/RTL within a single string) is handled by
  platform text rendering. Composers do not insert directional
  overrides.

### 7.3 String binding updates

When a typed-section data binding delivers updated text from an
external source (Ably SSE, CDN poll), the server attaches a
`stringKeys` map per the existing i18n contract. Atomic composition
inherits this — bound `Text.content` fields can carry a string key for
client-side resolution. See `sdui-technical-proposal.md` §9p for the
binding-level i18n contract.

### 7.4 What atomic composition does *not* cover

- **Pluralization rules.** Server selects the correct plural form
  during composition.
- **Gendered translations.** Server selects during composition.
- **Locale-specific layouts** beyond LTR/RTL (e.g. Japanese vertical
  text). Out of scope; would be a typed section.

---

## 8. Platform-native realization

Variants declare intent; clients realize that intent. The registry
spells out the realization per platform per OS tier per form factor.

### OS-version tier matrix

| Platform | Tiers |
|---|---|
| iOS | 26+, 17–25, <17 |
| Android | 15+, 12–14, <12 |
| Web | modern, fallback (capability-based via `backdrop-filter` and related feature detection — not browser-version based) |

Coverage floor: **current plus one back is the tested floor on each
platform**. Older OSes get a documented flat fallback. Silent rendering
failures on older OSes are not acceptable; every tier must have a
concrete realization.

**Dark-mode coverage is mandatory** at every declared tier on every
platform. A variant that only works in light mode is a bug, not a
feature.

**Form-factor coverage is mandatory** at every declared form factor
the client supports. A variant that only works on phone is a bug.

### Example: `hero` ContainerVariant realization (phone)

| Platform / tier | Light | Dark |
|---|---|---|
| iOS 26+ | Liquid Glass + 2-layer shadow (6/3, 12/6) + spring press; 16pt radius | Liquid Glass (dark tint) + same layered shadow |
| iOS 17–25 | `.ultraThinMaterial` + gradient overlay + shadow 0.12@6/3 + spring press; 16pt | `.ultraThinMaterial` (dark) + stronger overlay + shadow 0.35@8/4 |
| iOS <17 | Solid `.secondarySystemGroupedBackground` + overlay + shadow | Same; semantic auto-adapts |
| Android 15+ | Material 3 Expressive surface + tonalElevation 6dp + expressive shape + ripple + elevation 8dp | Same; surfaceContainer auto-shifts for dark |
| Android 12–14 | Material You + tonalElevation 6dp + 16dp radius + ripple | Same; dynamic color handles dark |
| Android <12 | Flat Material surface + elevation 6dp + 16dp radius + ripple | Same; static M3 dark scheme |
| Web modern | Linear gradient + `backdrop-filter: blur(20px) saturate(140%)` + `box-shadow: var(--shadow-lg)` + 16px | Same tokens resolve dark via `prefers-color-scheme` |
| Web fallback | Solid raised surface + large shadow + 16px, no `backdrop-filter` | Same |

For TV and tablet realizations of the same variant, see §5.5.1.

All of this sits behind one server-side token: `{ "type": "Container",
"variant": "hero" }`.

---

## 9. Strict decoders + renderer-layer fallback

The wire shape is strictly decoded on every client. If the server
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

**Section-level decode failure is recoverable.** When a section's
payload fails to decode (unknown required field, malformed structure),
the client logs `section_decode_failed` and skips *that section*; the
rest of the screen renders. The fail-the-whole-screen policy is the
wrong tradeoff for a feed.

**Why the variant split.** Hard-typing `variant` at decode would make
older clients crash on newer variant names — the exact regression we
want to avoid as the server rolls out new variants. Keeping it as a
string at decode and parsing at render time gives clients a graceful
fallback path: an older client simply misses the new treatment, it
doesn't crash out of the section.

---

## 10. Diagnostics

Every client emits four named diagnostics with a stable payload shape.
Severity is `debug` in development builds; in release they fold into
the platform's existing binding-warning / staleness-warning channel.

| Diagnostic | When | Payload |
|---|---|---|
| `variant_resolver_missing` | `variant: "someString"` does not match any known value for this primitive's enum. | `{ variant, elementType, sectionId, atomicPath }` |
| `variant_override_blocked` | An inline property was set on an axis the variant's override matrix marks as `lock` for this platform. | `{ variant, axis, platform, sectionId, atomicPath, attemptedValue }` |
| `token_resolver_missing` | A `token:...` reference did not resolve to any entry in the bundled registry (color, size, spacing, typography, etc.). | `{ token, registry, elementId?, axis }` |
| `section_decode_failed` | A section's payload did not decode against the schema. | `{ sectionId, sectionType, error, schemaVersion }` |

All four are non-fatal at the screen level. In every case the renderer
produces something rather than nothing — the variant falls through to
the primitive default, the token falls back to the caller's default
value, the override-blocked inline prop is silently used as the
variant default, and decode-failed sections are skipped.

The diagnostics are tools for composer authors: they point at places
the server is trying to do something the variant / token vocabulary
does not support, which is usually the signal to either fix the
server-side emission or extend the vocabulary.

---

## 11. Registries (six files, one model)

The design system ships six machine-readable registries:

| Registry | Owns | Form-factor-aware? | OS-mode-aware? |
|---|---|---|---|
| `style-tokens.json` | Variant definitions, override matrices, per-platform per-OS-tier per-form-factor realization specs | Yes | Yes |
| `color-tokens.json` | Two-tier color registry (palette + semantic, light + dark) | No | Yes |
| `size-tokens.json` | Semantic sizes (`avatar.lg`, `logo.team.md`, `icon.sm`) per form factor | Yes | No |
| `spacing-tokens.json` | Semantic spacing (`xs`, `sm`, `md`, `lg`, `xl`, `xxl`) per form factor | Yes | No |
| `typography-tokens.json` | Per-form-factor type scales for each `TextVariant` | Yes | No |
| `corner-radius-tokens.json` | Semantic corner radii (`sm`, `md`, `lg`, `full`) per form factor | Yes | No |

### `style-tokens.json` shape

Per variant (keyed by primitive: `ContainerVariant.hero`,
`ImageVariant.thumbnail`, etc.):

- `description` and `intent` — human-readable prose.
- Per-platform, per-OS-tier, per-form-factor realization — `ios.26+.phone`,
  `ios.26+.tablet`, `ios.26+.tv`, etc., each with `light` and `dark`
  specs that read as intent statements ("Liquid Glass + 2-layer shadow
  + spring press"), not as pixel-level specifications.
- `overrideMatrix` — per-axis policy: `"allow"`, `"lock"`, or
  `{ "ios": "lock", "android": "allow", "web": "allow" }`.
- `evidence` — the composer patterns or reference-app surfaces this
  variant replaces. Documentation of why this variant exists, for the
  quarterly audit.

### `color-tokens.json` shape

Two-tier color registry:

- `palette` — named primitives keyed as `color.<family>.<step>` (e.g.
  `color.grey.50`, `color.blue.30`). Each entry carries a literal
  `{ light, dark }` hex pair.
- `semantic` — aliases keyed by semantic path (e.g. `color.primary.50`,
  `color.brand.nba`, `color.text.primary`). Each entry carries
  `{ aliasOf: "<palette or semantic name>" }`. Aliases may chain
  through other aliases up to a bounded depth before ultimately
  resolving to a palette primitive.
- `diagnostics` — the `token_resolver_missing` schema used by clients.

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

### `size-tokens.json` and `spacing-tokens.json` shape

Two-tier registry mirroring color:

```json
{
  "palette": {
    "spacing.unit.4":  { "phone": 4, "tablet": 4, "tv": 8, "web.narrow": 4, "web.wide": 4 },
    "spacing.unit.8":  { "phone": 8, "tablet": 12, "tv": 16, "web.narrow": 8, "web.wide": 12 },
    "spacing.unit.16": { "phone": 16, "tablet": 20, "tv": 32, "web.narrow": 16, "web.wide": 20 }
  },
  "semantic": {
    "spacing.xs":  { "aliasOf": "spacing.unit.4" },
    "spacing.sm":  { "aliasOf": "spacing.unit.8" },
    "spacing.md":  { "aliasOf": "spacing.unit.16" }
  }
}
```

### Interaction between the registries

Variants control platform-native surface colors (container backgrounds,
image backdrops, elevation shadows). Those colors resolve through each
client's platform semantic palette — UIKit semantic colors, Android
`MaterialTheme.colorScheme`, CSS custom properties under
`prefers-color-scheme`. They do **not** flow through `color-tokens.json`;
the variant registry owns them.

The color-token registry controls **brand and content colors** — the
text color of a headline, the background of a promo banner, the LIVE
badge pill. These are the colors a composer emits per-element via
`color`, `background`, `shadow.color`, `Divider.color`, and similar
properties.

There is no overlap. A composer never routes a variant's surface color
through a color token; a composer never routes a brand accent through
a variant. Each registry owns a disjoint slice of the color decisions
on the screen.

The size, spacing, typography, and corner-radius registries are purely
form-factor-resolution registries — they have no interaction with
variants beyond being the values variants reference internally.

---

## 12. Worked example: a featured game hero

To tie the layers together, here is a single section — a featured game
hero card — traveling from the server to the screen on each platform
and form factor.

### Server emission

```json
{
  "type": "Container",
  "variant": "hero",
  "direction": "row",
  "alignment": "center",
  "crossAlignment": "center",
  "padding": "spacing.md",
  "cornerRadius": "radius.lg",
  "actions": [
    { "trigger": "onActivate", "type": "navigate", "targetUri": "nba://game/0022500001" }
  ],
  "a11y": {
    "label": "Lakers vs Celtics, Final 118 to 112",
    "role": "button"
  },
  "children": [
    {
      "type": "Image",
      "src": "https://cdn.nba.com/logos/nba/1610612747/primary/L/logo.svg",
      "size": "logo.team.md",
      "fit": "contain",
      "a11y": { "label": "Los Angeles Lakers" }
    },
    {
      "type": "Container",
      "direction": "column",
      "flex": 1,
      "children": [
        {
          "type": "Text",
          "variant": "score",
          "weight": "bold",
          "content": "118 - 112",
          "color": "token:color.text.inverse",
          "textAlign": "center"
        },
        {
          "type": "Text",
          "variant": "labelMedium",
          "content": "Final",
          "color": "token:color.text.secondary",
          "textAlign": "center"
        }
      ]
    },
    {
      "type": "Image",
      "src": "https://cdn.nba.com/logos/nba/1610612738/primary/L/logo.svg",
      "size": "logo.team.md",
      "fit": "contain",
      "a11y": { "label": "Boston Celtics" }
    }
  ]
}
```

Three sub-layers visible in the payload, plus the platform-neutral
action vocabulary and a11y:

- **Inline primitives** — `padding`, `cornerRadius`, `direction`,
  `alignment`, `flex`, `size`, `fit`, `textAlign`. All sizes and
  spacings are semantic tokens.
- **Variants** — `variant: "hero"` on the row (carries the
  platform-native material / tonal surface), `variant: "score"` on
  the game score (carries tabular-numeral typography),
  `variant: "labelMedium"` on the status line.
- **Color tokens** — `color: "token:color.text.inverse"` and
  `color: "token:color.text.secondary"` on the two text elements.
- **Action** — `onActivate` (platform-neutral, not `onTap`).
- **a11y** — labels on the container (the whole card is a tappable
  unit) and on each information-bearing image.

### iOS render (SwiftUI), iPhone, iOS 26+, light mode

The `hero` container resolves to Liquid Glass + 2-layer spring shadow
+ 16pt corner radius. Under the override matrix `padding` is `allow`,
so `spacing.md` resolves to 12 (phone form factor), the 12pt insets
apply on top of the material. `background` is locked on iOS under
`hero`; this payload does not attempt to override it.

`logo.team.md` resolves to 48pt on phone (per `size-tokens.json`).
The team-logo images render with `.resizable().aspectRatio(contentMode:
.fit)` inside a 48×48 frame. No corner rounding.

The score text variant realizes as `Font.system(.headline)` weight bold
with `.monospacedDigit()` so "118 - 112" keeps column alignment. Its
color is `token:color.text.inverse` → `color.grey.0` → `#FFFFFF` in
both light and dark (white content against the Liquid Glass surface).

`onActivate` binds to `.onTapGesture` plus VoiceOver activation. The
`a11y.label` becomes `accessibilityLabel`.

### iOS render, iPad, same OS

Same wire payload. `spacing.md` resolves to 16 on tablet. `logo.team.md`
resolves to 64pt on tablet. `score` typography scales up. Everything
else identical.

### tvOS render

Same wire payload. `spacing.md` resolves to 24. `logo.team.md`
resolves to 96pt. `score` typography scales to 10-foot type.
The container becomes focusable; focus halo overlay applies. D-pad
center triggers `onActivate`. Overscan-safe inset added at root.

### Android render (Jetpack Compose), phone, Android 15+

`hero` resolves to a Material 3 Expressive Surface with `tonalElevation
= 6.dp`, expressive shape, ripple, and elevation 8dp. The inline
`padding` (resolved 12dp) and `cornerRadius` (resolved 16dp) apply per
the override matrix.

Team-logo images render with `ContentScale.Fit` inside a 48×48 dp box.
`score` maps to `MaterialTheme.typography.headlineSmall` with the NBA
brand font and a tabular-digit feature. Color token resolves via
`ColorTokenResolver`, with `isSystemInDarkTheme()` selecting the dark
value.

`onActivate` binds to `Modifier.clickable` plus TalkBack activation.
`a11y.label` becomes `Modifier.semantics { contentDescription = … }`.

### Web render (React + CSS), wide breakpoint

`hero` resolves to a modern-tier CSS class: linear gradient background,
`backdrop-filter: blur(20px) saturate(140%)`, large `box-shadow`, 20px
radius (web.wide form factor). `spacing.md` resolves to 20px.

Team-logo images render inside a fixed-size `<img>` with `object-fit:
contain` and no `border-radius`. `score` resolves to the CSS typography
class for tabular-digit headline text. The color token resolves via a
utility hook that reads `prefers-color-scheme` and picks the correct
hex.

`onActivate` becomes `role="button"` + click handler + Enter/Space
keyboard handler. `a11y.label` becomes `aria-label`.

### What the reader should take away

The same wire payload produces five valid platform-form-factor renders
(iPhone, iPad, tvOS, Android phone, web wide), and would produce
several more for unlisted permutations. None is "wrong" relative to
the others. Each one is idiomatic for its platform and form factor.
The composer wrote the screen once; the clients did the rest.

---

## 13. Governance

Both registries are published contracts. Additions, renames, and
removals follow an explicit bar rather than a PR vibe-check.

### Adding a new variant value

Required before the value ships:

1. **At least two evidenced call sites.** Composer patterns or
   reference-app surfaces that the variant demonstrably replaces. The
   evidence is *inline-primitive bags* the variant absorbs — two
   places where composers wrote the same shadow + material + radius
   combination by hand. One site is an inline-property bag; two is a
   pattern worth a name.
2. **Design-system sign-off.** The variant joins a published
   vocabulary; the design system owns whether it's warranted.
3. **Per-platform per-OS-tier per-form-factor realization notes.**
   Every tier × form-factor declared in §8 and §5.5 must have a
   concrete realization. "TBD on iOS 17 tablet" is not a shipping
   state.
4. **Dark-mode spec at every declared tier on every platform.**
5. **Override-matrix entry.** Per-axis policy scalar or per-platform
   object.
6. **Codegen regeneration.** Every client's typed enum updated.
7. **Per-platform per-form-factor screenshot review.** Each client is
   evaluated against its own platform's design-system spec at each
   form factor it supports. No cross-platform pixel comparison.

### Adding a new color, size, or spacing token

1. **Semantic or brand justification.** "Designer wants this hex
   here" is not a token; "we need a second feedback error for a
   less-alarming state" is.
2. **Light and dark values** (color tokens). Both mandatory.
3. **All form-factor values** (size, spacing, typography, corner
   radius tokens). All mandatory.
4. **Registry placement.** Palette primitive for new raw values;
   semantic alias for new named roles. If it's a one-off with no
   reuse, it stays inline and does not become a token.

### Removal and deprecation

- **Two-release deprecation window** for any rename, removal, or
  override-matrix flip. Older clients running against a newer server
  must keep rendering something sensible; a removed token or variant
  value silently logs the appropriate diagnostic during the window.
- **Strict decoders make removal asymmetric.** Adding a value is
  cheap; removing one means every client in the field needs to have
  moved past the deprecation first. Design accordingly.

### Audits

- **Override-rate SLO.** Any override-matrix axis with >30% override
  rate gets reviewed. Either the variant default is wrong (fix the
  variant) or the variant needs to split into two (one that owns that
  axis, one that doesn't).
- **Quarterly audit.** Composer call sites are diffed against
  registry usage. Three or more repeated inline patterns become
  new-variant or new-token candidates — the expressibility check in
  §3.5 runs again with the new evidence in hand.
- **Atomic-share KPI.** Track the percentage of UI changes shipping
  via server deploy vs. client release. The atomic composition layer
  exists to move this number; if it isn't moving, the system is
  failing its purpose.

### Why the bar is this high

Every variant value, every token, and every form-factor row is a
client-release coupling point. Each one expands the wire-level
contract. Strict decoders and the app-store long tail make removal a
deprecation exercise, not a code change. Keeping the vocabularies
tight is what keeps the server deploy — not the client release — the
mechanism for most design changes. **That is the primary KPI of the
whole system.**
