# Reserved SDK Sections and the `data.ui` Atomic Tree

> Status: **Adopted** — this document describes the landed pattern.
> Scope: `SubscribeBanner`, `SubscribeHero`, `VideoPlayer`.
> See also: AGENTS.md §15.1 (Section vs AtomicComposite), §15.3 (Section
> surface), §18 (Server-driven vs client-realized).

This document is the load-bearing reference for how "reserved SDK
integration point" sections are modelled on the wire and rendered on
clients. It defines the mental model, the relationship between the
section envelope and its inner composition, how data binding and
refresh interact with the inner tree, and the migration rationale
(Option A vs Option B). It includes trimmed wire-format examples to
ground the model, but no platform renderer code — that lives in the
composers, renderers, and ADRs; this doc explains why those shapes
exist.

## 1. What is a "reserved SDK" section?

Three permanent sections exist in the schema *because of* a future
platform SDK the server cannot own: `SubscribeBanner` and
`SubscribeHero` reserve space for a StoreKit / Google Play Billing
integration; `VideoPlayer` reserves space for the HLS/DASH player
stack (PiP, AirPlay, Chromecast, background audio). None of those
concerns can be expressed as composition — they are lifecycle,
network, and OS integration.

But there is a mismatch. Today, before the SDK lands, the *visible
surface* of each of these sections is pure composition: a banner with
copy and a button; a hero with a feature list and tier cards; a
player with a placeholder glyph. That composition has no client-owned
state, no live data, no platform SDK in the loop — exactly the
characteristics AGENTS.md §15 carves out for server-composed
AtomicComposite.

The reserved-SDK pattern resolves that mismatch without collapsing
the two concepts together. The section envelope stays permanent (so
the SDK integration has somewhere to land), but the inside of the
section is authored the same way an AtomicComposite is.

## 2. The mental model: one envelope, two concerns

A reserved-SDK section's `data` block separates two concerns that
happen to travel together:

- **The visible surface** lives under `data.ui` as an `AtomicElement`
  tree. It is rendered by the same platform walker that renders any
  `AtomicComposite` section. It carries no meaning to the SDK; it is
  pixels.
- **SDK inputs** live at the top level of `data`, parallel to `ui`.
  These are the fields a StoreKit product lookup or an HLS player
  will read directly when it mounts — product identifiers, content
  identifiers, autoplay hints, capability flags, the pre-SDK CTA
  action used as a fallback today. The SDK does not walk the tree.

The outer surface — the margin, corner radius, gradient, shadow, and
inner padding of the section's visual frame — lives on
`section.surface`, the same wrapper every permanent section gets.
The atomic tree inside `data.ui` does not re-declare the outer
frame; it composes content against an already-framed surface.

The result is three layers of concern, each owned by its natural
author:

| Layer          | What it describes                              | Owner                  |
|----------------|------------------------------------------------|------------------------|
| `section.surface` | The section's outer frame                   | Server composer        |
| `data.ui`         | The visible content tree                    | Server composer        |
| Top-level `data`  | Inputs the future SDK will read directly    | Server composer + SDK  |

## 3. Concrete shape on the wire

The three sections share one envelope shape. The examples below are
trimmed for legibility (action payloads and surface blocks are summarised
where they would otherwise repeat) but reflect what the server actually
emits.

### 3.1 `SubscribeBanner`

An inline upsell with a title, subtitle, and a CTA button. Everything
visible lives in the tree; `ctaAction` at the top level is the pre-SDK
fallback the future IAP integration will replace.

```json
{
  "id": "watch-subscribe-inline",
  "type": "SubscribeBanner",
  "refreshPolicy": { "type": "static" },
  "surface": { "/* gradient + radius + padding */": "…" },
  "data": {
    "ui": {
      "type": "Container", "direction": "vertical", "gap": 4,
      "children": [
        { "type": "Text",   "content": "Never Miss a Game",
          "variant": "titleMedium", "weight": "bold", "color": "#FFFFFF" },
        { "type": "Text",   "content": "Stream every out-of-market game live.",
          "variant": "bodySmall", "color": "rgba(255,255,255,0.85)" },
        { "type": "Spacer", "height": 8 },
        { "type": "Button", "label": "Subscribe Now", "variant": "primary",
          "actions": [
            { "trigger": "onTap", "type": "navigate", "targetUri": "nba://subscribe" }
          ]
        }
      ]
    },
    "ctaAction": {
      "trigger": "onTap", "type": "navigate", "targetUri": "nba://subscribe"
    }
  }
}
```

### 3.2 `SubscribeHero`

A full-screen upsell with logo, title, subtitle, a feature list, and two
tier cards, each with its own CTA. Every visible element is an atomic
primitive. The top-level `tiers` array is parallel to `ui` and holds only
what the future IAP SDK needs — product identifiers and the server-
emitted price copy — so the SDK never has to walk the tree.

```json
{
  "id": "lp-subscribe-hero",
  "type": "SubscribeHero",
  "refreshPolicy": { "type": "static" },
  "surface": { "/* navy background + radius */": "…" },
  "data": {
    "ui": {
      "type": "Container", "direction": "vertical", "crossAlignment": "center", "gap": 6,
      "children": [
        { "type": "Image", "src": "…/league-pass-logo.png", "height": 48, "fit": "contain" },
        { "type": "Text",  "content": "NBA League Pass",
          "variant": "headlineSmall", "weight": "bold", "color": "#FFFFFF" },
        { "type": "Text",  "content": "Your courtside seat to every game.",
          "variant": "bodyMedium", "color": "rgba(255,255,255,0.8)" },

        { "type": "Container", "direction": "vertical", "gap": 6,
          "children": [
            { "type": "Container", "direction": "horizontal", "gap": 8,
              "children": [
                { "type": "Text", "content": "✓", "weight": "bold", "color": "#FFFFFF" },
                { "type": "Text", "content": "Watch every out-of-market game",
                  "variant": "bodyMedium", "color": "rgba(255,255,255,0.85)" }
              ]
            }
            /* …more feature rows… */
          ]
        },

        { "type": "Container", "direction": "vertical", "gap": 12,
          "children": [
            { "type": "Container", "direction": "vertical", "gap": 4,
              "background": "rgba(255,255,255,0.1)", "cornerRadius": 12,
              "padding": { "top": 16, "end": 16, "bottom": 16, "start": 16 },
              "children": [
                { "type": "Text",   "content": "MOST POPULAR",  "variant": "labelSmall",
                  "weight": "bold", "color": "#FFDD00" },
                { "type": "Text",   "content": "League Pass",   "variant": "titleMedium",
                  "weight": "bold", "color": "#FFFFFF" },
                { "type": "Text",   "content": "$14.99/mo",     "variant": "titleLarge",
                  "weight": "bold", "color": "#FFFFFF" },
                { "type": "Button", "label": "Subscribe", "variant": "primary",
                  "actions": [
                    { "trigger": "onTap", "type": "navigate",
                      "targetUri": "nba://subscribe/league-pass/standard" }
                  ]
                }
              ]
            }
            /* …and the Premium tier card, same shape… */
          ]
        }
      ]
    },
    "tiers": [
      { "id": "lp-standard", "name": "League Pass",         "price": "$14.99/mo" },
      { "id": "lp-premium",  "name": "League Pass Premium", "price": "$22.99/mo" }
    ]
  }
}
```

Two things worth noting in this example:

- The tier card's inner surface (dark semi-transparent background, 12pt
  radius, 16pt padding) is expressed as inline properties on a
  `Container` node. This is the pattern AGENTS.md §19 describes:
  when a design is expressible as inline props on an existing
  primitive, it does not need a new `ContainerVariant` value.
- The tier card's CTA action is in the button's `actions` array,
  not on the `data.tiers[i]`. Today the button fires directly. When
  the IAP SDK lands the client intercepts the tap and uses
  `data.tiers[i].id` as the product identifier — the button tree
  does not change, only the client's interpretation of that tap
  does.

### 3.3 `VideoPlayer`

The pre-SDK placeholder — a dark column with a play glyph and the
content identity. All visible content is in the tree; the SDK inputs
(`playerType`, `contentId`, `autoplay`, `capabilities`,
`displayConfig`) sit at the top of `data`, ready for the future HLS
player to consume without walking the tree.

```json
{
  "id": "video-player-0022400500",
  "type": "VideoPlayer",
  "refreshPolicy": { "type": "static" },
  "surface": { "cornerRadius": 12 },
  "data": {
    "ui": {
      "type": "Container", "direction": "vertical",
      "alignment": "center", "crossAlignment": "center",
      "fillWidth": true, "height": 220, "background": "#000000",
      "children": [
        { "type": "Text",   "content": "▶",           "variant": "displayLarge",
          "weight": "bold", "color": "#FFFFFF" },
        { "type": "Spacer", "height": 8 },
        { "type": "Text",   "content": "Video Player","variant": "titleMedium",
          "color": "#FFFFFF" },
        { "type": "Text",   "content": "game • 0022400500", "variant": "bodySmall",
          "color": "rgba(255,255,255,0.6)" }
      ]
    },
    "playerType":    "game",
    "contentId":     "0022400500",
    "autoplay":      false,
    "capabilities":  ["pip", "fullscreenRotation"],
    "displayConfig": { "aspectRatio": "16:9" }
  }
}
```

When the HLS player SDK lands, the top-level fields are already
shaped the way the SDK expects — the composer does not change, the
client switches from "walk `data.ui`" to "mount the SDK with these
inputs and fall back to `data.ui` while loading / on error."

## 4. Why `data.ui` (Option A), not typed fields (Option B)

Two shapes were considered before we chose this one:

- **Option A (adopted)**: the entire visible surface is one
  polymorphic field, `data.ui`, carrying an `AtomicElement` tree.
- **Option B (rejected)**: enumerate the visible surface as typed
  fields on `data` — `data.logoUrl`, `data.features[]`,
  `data.tiers[].badgeText`, `data.tiers[].features[]`, etc.

Option A wins on every axis except exact pixel parity with the
renderers we had before this migration:

- **Client release cadence.** Every new visual concept in Option B
  is a new typed field on the wire, and every new typed field is a
  renderer change on every client — which is a client release. That
  is the top-line KPI this architecture exists to reduce (see
  AGENTS.md §18). Option A moves all future visual changes to
  server composition.
- **Contract surface.** Option B grows the Section↔Client contract
  with every new visual concept. Option A adds one polymorphic field
  once and is done; the vocabulary of change is the atomic primitive
  set, which is already governed by AGENTS.md §19 and the
  style-token ADRs.
- **Renderer complexity.** Option A renderers are thin walkers —
  parse `data.ui`, hand it to the atomic router. Option B renderers
  grow linearly with the number of typed fields they consume, and
  each field needs its own accessibility / empty-state /
  internationalisation handling.
- **Consistency with AtomicComposite.** Option A is the same shape
  the rest of the system already uses for server-composed content.
  Sections stop being a special vocabulary with different conventions
  from AtomicComposite, and one platform walker handles both.

The single axis where Option B wins is exact visual fidelity to the
handwritten renderers that existed before the migration — Option A
recomposes the same surface out of atomic primitives, which can land
within visual tolerance but will not be pixel-identical. That cost
is acceptable; the KPI is release cadence, not pixel parity. Visual
polish after migration is a composer-only change and ships without
clients.

### 4.1 Why keep the SDK inputs at the top of `data`?

A StoreKit purchase flow wants product identifier strings. An HLS
player wants a URL, capability flags, and an autoplay boolean.
Walking an atomic tree to find them would be fragile — a composer
refactor that restructures the tree (which is explicitly allowed)
would break the SDK's data lookup (which must not break). Keeping
SDK inputs at the top of `data`, parallel to `ui`, is the cheap
thing that stays right: the renderer walks `ui`; the SDK reads the
top level; neither side cares what the other is doing.

## 5. How the clients render it

Every client uses the same two-step pattern it already uses for
`AtomicComposite`: fetch the `data.ui` sub-tree, hand it to the
platform-native atomic router, thread `screenState` and the action
callbacks through. The renderer itself is a thin delegate — it
carries no per-section layout defaults, no fallback surface, and no
branches on section content.

This means a reserved-SDK renderer contains essentially the same
code on every platform. The body of `SubscribeBanner` on iOS is the
same shape as the body of `VideoPlayerStub` on Android: parse
`data.ui`, delegate to the atomic router. The differences are the
platform's identifier conventions (a SwiftUI `View`, a Compose
`@Composable`, a React function), not behaviour.

Absence of typed-field parsing in these renderers is deliberate. If
a renderer read `data.title` directly, a future composer that moved
the title into a second text line inside the tree would silently
stop rendering the title — the contract would have two sources of
truth. Putting the entire visible surface in `data.ui` collapses
that to one.

## 6. Data binding, refresh, and live updates

Section-level concerns do not move when the visible surface moves
into `data.ui`:

- **Refresh policy** stays on the section envelope. All three
  reserved-SDK sections are `static` today — they are composed once
  and do not update. If an SDK future requires polling or SSE, the
  policy changes on the envelope; the renderer stays a thin walker.
- **Data bindings** still target paths under `data`. They can bind
  into top-level SDK inputs (e.g. a live price update replacing
  `data.tiers[0].price`) or into nodes inside `data.ui` (e.g. the
  badge text on a tier card). Path-into-tree bindings work but are
  brittle if the composer later reshapes the tree.
- **Accessibility** rides on individual atomic elements, not on a
  section-specific schema field. The Text, Image, and Button
  primitives already carry their own accessibility slots, and the
  inherited pipeline from AtomicComposite applies.
- **Impression tracking** (`onVisible`, analytics identifiers) still
  fires at the section level via the outer impression tracker; the
  atomic walker inside does not duplicate it.

### 6.1 `bindRef`: planned escape hatch for stale binding paths

Path-based bindings into a tree work but are fragile. The future
mitigation is a `bindRef` property on atomic primitives: an atomic
`Text`, `Image`, or `Button` can carry a reference to a top-level
`data` field (or, eventually, a `screenState` key) and the renderer
resolves the current value at render time. This inverts the coupling
— the reference lives on the node that consumes it, not in a
centrally-declared binding path — and the composer can restructure
the tree without breaking the binding.

`bindRef` is not required for the three sections migrated today (all
`static`, no live data). It is the planned entry point when live
data arrives.

## 7. What this pattern assumes about the client

For `data.ui` to carry the visible surface, the client already has to
understand the atomic primitive vocabulary: Container, Text, Image,
Button, Spacer, Divider, Conditional, ScrollContainer, DisplayGrid,
SectionSlot, plus the inline properties each one takes. The
AtomicComposite work already shipped that vocabulary on every client.

This is why the migration is cheap: it reuses a surface area that
already exists. No new walker, no new primitive registry, no new
binding infrastructure. The reserved-SDK sections adopt a shape the
rest of the system has already paid for.

The cost of new visual treatments after the migration is therefore
also cheap: a composer change that emits an additional primitive, or
an inline-property tweak on an existing primitive, ships from the
server. No schema change, no client release, no codegen.

## 8. When the SDK lands

The pattern is forward-compatible with SDK integration:

- **Subscribe sections, when StoreKit / Play Billing lands.** The
  top-level `ctaAction` is replaced at the renderer boundary — the
  client intercepts the CTA button's tap and routes it into the IAP
  SDK using product identifiers from `data.tiers`. The `data.ui`
  tree continues to render as the visible surface (tier copy,
  feature list, badges). The SDK owns the purchase flow, not the
  layout.
- **VideoPlayer, when the HLS/DASH player lands.** The client swaps
  the atomic walker for a player view that reads the top-level SDK
  inputs and mounts the platform SDK. While the SDK is loading or
  has failed, the same `data.ui` tree renders as the
  loading / error placeholder. The tree does not disappear; it
  becomes the frame the SDK lives in.

In both cases the server contract does not change. The server keeps
emitting `data.ui` plus the top-level SDK inputs. The client's
behaviour for a particular section type changes from "walk the tree"
to "mount the SDK, fall back to the tree" — a client release, yes,
but a contained one, not a schema migration.

## 9. Relationship to governance rules

| Rule | How this pattern relates |
|------|---------------------------|
| §13 (Schema contract) | The `data.ui` field is part of the Section schema; its strict decoding at the client happens via the same `AtomicElement` model AtomicComposite uses. No new schema vocabulary is introduced by this pattern. |
| §14 (Renderers are presentation-only) | Reserved-SDK renderers become literally presentation-only: they delegate all composition to the atomic walker. |
| §15 (Section vs AtomicComposite) | These sections stay *permanent* (they reserve SDK lifecycle), but their visible surface is *composed as if AtomicComposite*. The section envelope and the inner composition concept are decoupled. |
| §15.3 (Section surface) | The outer surface stays on `section.surface`. `data.ui`'s root may still set its own inner background or padding if the design calls for a secondary frame inside the outer surface. |
| §16 (Semantic tokens) | The atomic tree emits semantic tokens (`variant: "titleMedium"`, `variant: "primary"`); each client resolves them natively. |
| §17 (Code comments) | Renderer files cite their SDK role and point to §15.3 and this document; they do not cite internal rule numbers. |
| §18 (Server-driven vs client-realized) | Content and composition stay server-driven. The only client-realized piece is the atomic-primitive → native-widget mapping, which already exists. |
| §19 (Minimize variant proliferation) | Building the visible surface from existing primitives + inline props is exactly the cheap path this rule prefers. No `SubscribeBannerVariant` / `VideoPlayerVariant` enums were created. |

## 10. Open questions parked for later

- **`bindRef`** (§6.1) is sketched but not implemented. It is not
  blocking today because all three sections are `static`.
- **Container `aspectRatio`.** The video player placeholder wants a
  16:9 frame expressed in primitives. `AtomicImage` supports
  `aspectRatio`; `AtomicContainer` does not yet. The placeholder
  uses `fillWidth` + a fixed height as an interim fix; adding
  `aspectRatio` to Container is a small follow-up.
- **Button variant for white-on-brand surfaces.** The current
  `primary` button variant resolves to a filled button per each
  platform's button-variant registry. The handwritten renderers
  previously used a white-fill / brand-text treatment. Closing that
  gap is either (a) a new `ButtonVariant` value (requires §19
  justification) or (b) inline style overrides in the atomic tree.
  Not in scope for this migration.
