# Plan: Design Token System — End-to-End Adoption and Coverage

**Status:** Proposed (revised)
**Created:** 2026-05-24 · **Revised:** 2026-05-24
**Supersedes:** prior version of this file (and the deleted `plan-design-token-adoption.md`)
**Scope:** Schema, server registries, client resolvers, fixtures, docs, doctrine
**Sources:** Kinetic Design System (Figma export) for web; Material Design 3 (Android); Apple HIG / Dynamic Type (iOS); tvOS HIG + Google TV Material You (TV)
**Depends on:** AGENTS.md §3.6

---

## Problem

The design-token system is partially built. Color, icon, and style/variant are complete end-to-end. Spacing and radius have working infrastructure but only `AtomicCompositeBuilder` has heavy adoption. Typography, motion, shadow, and font have no registries at all. The previous version of this plan over-engineered the data shape (6-column form-factor matrix, two parallel typography wire vocabularies, multiple alias layers) in ways that fragmented the cache, multiplied resolver paths, and committed the project to envelope changes that were never justified.

This revision closes the registry gaps using platform-native conventions, collapses the matrix to the four canonical deviceClasses, drops every alias layer that wasn't doing work, and bakes the registry into each client build so routine form-factor changes never trigger a network call.

## Goals

1. Close the registry gaps for **typography, motion, shadow, font**, using the conventions each platform actually publishes (Material 3, Apple HIG, tvOS HIG, Google TV) rather than inventing values or scaling one platform from another.
2. Finish server composer adoption for **spacing, radius, icon** so the wire never emits raw pixels where a token applies.
3. Extend the client `LayoutTokenResolver` on all three platforms to resolve every namespace from a **codegen-baked registry**, with parity tests.
4. Migrate example fixtures to canonical token strings so they exercise the same code path as production payloads.
5. Update docs and AGENTS.md to reflect what was actually built.

Out of scope (split or deferred):
- **Color bug fixes** (scrim flip in dark mode, `brand.nba` vs. gold copy) — own PR, can land before, after, or alongside this one.
- **Android `score` variant rendering bug** (`AtomicText.kt`) — same own-PR as the color bugs; pre-existing, unrelated to coverage.
- **`schema/size-tokens.json` cleanup** — file was already deleted on 2026-04-29 (commit `172eb65`); no-op.
- **Figma export pipeline** — separate plan.

## Design posture

### 1. Cross-platform first, sitebuilder-compatible

`sitebuilder/assets/brand-nba.yml` is a **capability target for the web surface**, not a wire contract for the system. Phone, tablet, and connected-TV are peer platforms whose values come from the conventions those platforms actually publish:

| Column | Source | Why |
|---|---|---|
| `phone` | Material 3 type scale + Apple HIG Dynamic Type baseline | What native Android/iOS phone UIs expect |
| `tablet` | Material 3 large window class + Apple HIG iPad | Native tablet conventions |
| `tv` | tvOS HIG + Google TV Material You | 10-foot UI conventions |
| `web` | Kinetic step scale | Kinetic-faithful surface |

We do not compute mobile values as multipliers of web, or TV as multipliers of mobile.

### 2. Token wire vocabulary is single-layer, with one intentional exception

| Family | Wire form | Layers |
|---|---|---|
| Color | `nba.color.text.primary` (semantic), `nba.color.blue.50` (primitive escape) | **2** (primitive + semantic — theming swaps the primitive) |
| Spacing | `nba.spacing.lg` | 1 |
| Radius | `nba.radius.md` | 1 |
| Typography | `nba.typography.headlineLarge` (Material 3 semantic names) | 1 |
| Motion | `nba.motion.duration.fast` + `nba.motion.easing.default` (independent atoms) | 1 |
| Shadow | `nba.shadow.md` | 1 |
| Font | `nba.font.knockout` (referenced from a typography entry via `familyRef`; never standalone on the wire) | data only |
| Icon | `sdui:play` | 1 |

Raw / step-indexed sub-vocabularies (`nba.space.raw.*`, `nba.radius.raw.*`, `nba.typography.headline.10`, `nba.motion.transition.*`) **do not ship on the wire**. They are eliminated from the schema; semantic entries hold their matrices inline.

### 3. Bundled registry, zero round-trips on resize

The token registry is a **client build artifact**, not a runtime fetch. Routine form-factor changes (rotation, browser resize, foldable expand within the same `deviceClass`, split-screen, TV overscan) resolve **entirely on the client**:

- `make codegen` bakes the registry into each client (typed maps; not parsed JSON at runtime).
- Form-factor signals come from platform-native observers; framework reactivity (SwiftUI `Environment`, Compose `CompositionLocal`, React `Context`) invalidates downstream views when the form factor changes.
- The wire payload is stable across every routine form-factor change; only the resolved pixel value changes.
- **No fetch** is triggered by rotation, resize, or split-screen. The client never appends size/orientation parameters to composition requests.
- **DeviceClass transitions** (e.g. foldable expand from `phone` to `tablet`) are the only re-fetch case, because composition can legitimately differ. The re-fetch routes through the standard `SduiRepository.fetchScreen()` with the new `platform[deviceClass]`, not a bespoke endpoint.

### 4. Form factor is client-local, not a wire field

`formFactor` (orientation, web breakpoint, viewport scaling) is intentionally **not** added to the request envelope. Adding it would multiply the CDN cache keyspace by 4–6× for zero composition benefit, because:

- The server does not branch composition on formFactor today
- The token registry is bundled into every client and resolves form factor locally
- `deviceClass` already carries the only platform input composition needs

The earlier "roadmap" framing in AGENTS.md §3.4 is retired in Phase 10 of this plan.

### 5. Token matrix shape: 4 columns, with a fluid web envelope

The matrix uses the four native deviceClasses as columns: **`phone`, `tablet`, `tv`, `web`**. Native platforms handle intra-column fragmentation through their own systems (iOS `pt` + Dynamic Type, Android `sp` + `WindowSizeClass`, tvOS semantic ramp).

`web` is the exception. Because CSS units are absolute and the viewport is continuous, the `web` value of a token may be either:

- A scalar (`"web": 14`), **or**
- A fluid envelope (`"web": { "min": 14, "max": 20, "minVw": 320, "maxVw": 1440 }`) that the web resolver renders as CSS `clamp()`.

Earlier columns (`phone.landscape`, `web.narrow`, `web.wide`) were speculative and are removed everywhere. Composition-level orientation or breakpoint decisions stay in composer logic keyed off `deviceClass`, not in token columns.

---

## Naming convention

Every token name carries the `nba.` prefix. Icons retain `sdui:` because they're platform-neutral, not NBA-league-specific.

| Family | Namespace | Wire form | Server constant |
|---|---|---|---|
| Color | `nba.color.*` | `"token:nba.color.text.primary"` | `ColorTokens.TEXT_PRIMARY` |
| Spacing | `nba.spacing.*` | `"token:nba.spacing.lg"` | `LayoutTokens.SPACING_LG` |
| Radius | `nba.radius.*` | `"token:nba.radius.md"` | `LayoutTokens.RADIUS_MD` |
| Typography | `nba.typography.<variant>` | `"token:nba.typography.headlineLarge"` | `TypographyTokens.HEADLINE_LARGE` |
| Motion (duration) | `nba.motion.duration.*` | `"token:nba.motion.duration.fast"` | `MotionTokens.DURATION_FAST` |
| Motion (easing) | `nba.motion.easing.*` | `"token:nba.motion.easing.default"` | `MotionTokens.EASING_DEFAULT` |
| Shadow | `nba.shadow.*` | `"token:nba.shadow.md"` | `ShadowTokens.MD` |
| Font | `nba.font.<family>` | referenced via `familyRef` only | n/a |
| Icon | `sdui:*` | `"sdui:play"` | `IconTokens.PLAY` |

---

## Current state inventory

| Family | Registry | Server constants | Composer adoption | Client resolver | Status |
|---|---|---|---|---|---|
| Color | `schema/color-tokens.json` | `ColorTokens.java` | Heavy | iOS / Android / Web | Complete (2 bugs in separate PR) |
| Icon | `schema/icon-tokens.json` | `IconTokens.java` | Active | iOS / Android / Web | Complete |
| Style / Variant | `schema/style-tokens.json` | inline | Active | iOS / Android / Web | Complete |
| Spacing | `schema/spacing-tokens.json` (Kinetic, 6-col matrix) | `LayoutTokens.SPACING_*` | `AtomicCompositeBuilder` heavy; others partial | iOS / Android / Web | Migrate matrix to 4-col; finish composers |
| Radius | `schema/corner-radius-tokens.json` (Kinetic, 6-col matrix) | `LayoutTokens.RADIUS_*` | `AtomicCompositeBuilder` heavy; others partial | iOS / Android / Web | Migrate matrix to 4-col; finish composers |
| Typography | none | none | n/a | Partial (`TextVariant` enum only) | Build registry |
| Shadow | none | none | n/a | none | Build registry |
| Motion | none | none | n/a | none | Build registry |
| Font | none | n/a | n/a | Bundled fonts only | Build registry |

---

## Acceptance criteria

### Schema and registries

- [ ] `schema/typography-tokens.json` exists with a `variants` map for the existing `TextVariant` enum values plus any new values added per the Phase 1 audit; each variant has a `categoryRef` and a 4-column `size` matrix (`phone`, `tablet`, `tv`, `web`); web entries may be a scalar or a `{min, max, minVw, maxVw}` envelope
- [ ] Typography categories (Kinetic-faithful: `headline`, `display`, `title`, `body`, `label`, `data`, `score`, `button`, `caption`) live in the same file as server-internal spec data with `familyRef`, `weight`, `textCase`, `lineHeight`; they are **not** addressable on the wire
- [ ] `schema/motion-tokens.json` exists with `nba.motion.easing.default` + `nba.motion.easing.linear` and 4 duration tiers (`fast`, `default`, `slow`, `hero`); each duration is a 4-column matrix
- [ ] `schema/shadow-tokens.json` exists with 4 tiers (`sm`, `md`, `lg`, `xl`); shadows are form-factor-flat (`"*"` wildcard)
- [ ] `schema/font-tokens.json` exists with one entry per typeface (`nba.font.knockout`, `nba.font.roboto`, `nba.font.roboto.condensed`); weight map keyed by the values typography references
- [ ] `schema/spacing-tokens.json` and `schema/corner-radius-tokens.json` migrated from 6-column to 4-column matrix; raw layer (`nba.space.raw.*`, `nba.radius.raw.*`) deleted; semantic entries hold matrices inline
- [ ] `LayoutTokenRegistry.java` loads all token files at startup and fails fast on: missing variant categories, unresolved `familyRef`, invalid easing curves, malformed shadow structs, unknown aliases

### Server constants

- [ ] `TypographyTokens.java` exists with semantic-variant constants only (no category+step shortcuts)
- [ ] `MotionTokens.java` exists with duration + easing constants (no `transition.*` semantic bundles)
- [ ] `ShadowTokens.java` exists with `SM`, `MD`, `LG`, `XL`

### Composer adoption

- [ ] Every composer (`AtomicCompositeBuilder`, `ForYouComposer`, `ScheduleComposer`, `LiveComposer`, `HomeComposer`, `WatchComposer`, `GameDetailComposer`, `DemoScreenComposer`, `BoxscoreComposer`, `ScoreboardComposer`) emits `LayoutTokens.SPACING_*` / `RADIUS_*` constants for every padding/gap/cornerRadius value that maps to a semantic tier
- [ ] Raw integers remain only where AGENTS.md §3.6 exceptions apply (`0`, calculated values, non-responsive component dimensions, no semantic mapping)
- [ ] `IconTokens.java` covers every icon string emitted by any composer; no inline `"sdui:..."` literals remain in composer code

### Client resolvers and codegen

- [ ] `make codegen` emits typed `LayoutTokenRegistry` per platform (`LayoutTokenRegistry.swift`, `LayoutTokenRegistry.kt`, `LayoutTokenRegistry.ts`) covering every namespace; hand-written token tables in current resolvers are deleted
- [ ] All three `LayoutTokenResolver` implementations resolve every namespace using a single wire form per family
- [ ] Web resolvers translate the fluid envelope (`{min, max, minVw, maxVw}`) into CSS `clamp()`; iOS and Android resolvers treat web as opaque (not their column)
- [ ] Shadow shorthand (`"token:nba.shadow.md"`) resolves to the full structured shadow object on each platform
- [ ] Motion tokens resolve to platform-native animation values
- [ ] Each resolver reads form factor from platform-native signals and exposes it through the framework's reactivity primitive
- [ ] **Zero network requests** issued by any resolver during rotation, resize, or split-screen transitions within the same `deviceClass`; asserted by tests that stub the HTTP layer
- [ ] iOS gains a `LayoutTokenResolverTests.swift` with parity to existing Android/Web tests
- [ ] All three platforms test typography, motion, and shadow resolution

### Example fixtures

- [ ] All 27 `schema/examples/*.json` files use canonical `"token:nba.*"` strings for padding, gap, cornerRadius, shadow, and typography values where a semantic tier applies
- [ ] All 27 `ios/Tests/SduiCoreTests/Fixtures/*.json` files are synced byte-for-byte with `schema/examples/`
- [ ] Round-trip tests pass on all three platforms with the updated fixtures
- [ ] No fixture references legacy columns (`phone.landscape`, `web.narrow`, `web.wide`)

### Documentation

- [ ] `docs/sdui-design-system.md`, `docs/client-implementors-contract.md`, `docs/sdui-requirements-summary.md` updated to reflect the 4-column matrix, semantic-only wire vocabularies, and the four new registries
- [ ] Standing rule recorded: changes to `schema/*-tokens.json` and `docs/sdui-design-system.md` ship in the same PR
- [ ] AGENTS.md doctrine updated per Phase 10 (lands last)

---

## Phase 1 — Typography registry

The typography registry has two parts in the same file:

1. **Categories** (server-internal spec sheet): the Kinetic-faithful per-category base spec — `familyRef`, `weight`, `textCase`, `lineHeight`. Categories are **not** addressable on the wire.
2. **Variants** (wire vocabulary): one entry per `TextVariant` enum value. Each variant points at a category for its base spec and supplies a 4-column `size` matrix sourced from platform conventions.

### 1.1 Categories (internal spec)

Imported as-is from `sitebuilder/assets/brand-nba.yml` → `typography.categories`. Categories are form-factor-flat — the spec doesn't change per device, only the variant's chosen size does.

| Category | familyRef | weight | textCase | lineHeight |
|---|---|---|---|---|
| `nba.typography.headline` | `nba.font.knockout` | 360 | uppercase | 0.8 |
| `nba.typography.display` | `nba.font.knockout` | 395 | uppercase | 0.8 |
| `nba.typography.title` | `nba.font.roboto` | 500 | none | 1.2 |
| `nba.typography.body` | `nba.font.roboto` | 400 | none | 1.2 |
| `nba.typography.label` | `nba.font.roboto` | 400 | uppercase | 1.0 |
| `nba.typography.data` | `nba.font.roboto.condensed` | 400 | uppercase | 1.0 |
| `nba.typography.score` | `nba.font.knockout` | 360 | uppercase | 0.8 |
| `nba.typography.button` | `nba.font.roboto` | 700 | none | 1.0 |
| `nba.typography.caption` | `nba.font.roboto` | 400 | none | 1.2 |

### 1.2 Variants (wire vocabulary)

Each existing `TextVariant` enum value gets a variant entry. Sizes per column come from native conventions:

| Variant | categoryRef | phone | tablet | tv | web |
|---|---|---|---|---|---|
| `displayLarge` | `display` | 57 | 64 | 96 | `{min: 45, max: 96, minVw: 320, maxVw: 1440}` |
| `displayMedium` | `display` | 45 | 56 | 80 | `{min: 36, max: 80, minVw: 320, maxVw: 1440}` |
| `displaySmall` | `display` | 36 | 48 | 64 | `{min: 32, max: 64, minVw: 320, maxVw: 1440}` |
| `headlineLarge` | `headline` | 32 | 40 | 64 | `{min: 28, max: 64, minVw: 320, maxVw: 1440}` |
| `headlineMedium` | `headline` | 28 | 32 | 48 | `{min: 24, max: 48, minVw: 320, maxVw: 1440}` |
| `headlineSmall` | `headline` | 24 | 28 | 40 | `{min: 22, max: 40, minVw: 320, maxVw: 1440}` |
| `titleLarge` | `title` | 22 | 24 | 32 | `{min: 20, max: 28, minVw: 320, maxVw: 1440}` |
| `titleMedium` | `title` | 16 | 18 | 24 | 16 |
| `titleSmall` | `title` | 14 | 16 | 20 | 14 |
| `bodyLarge` | `body` | 16 | 18 | 24 | `{min: 16, max: 18, minVw: 320, maxVw: 1440}` |
| `bodyMedium` | `body` | 14 | 16 | 20 | `{min: 14, max: 18, minVw: 320, maxVw: 1440}` |
| `bodySmall` | `body` | 12 | 14 | 18 | 12 |
| `labelLarge` | `label` | 14 | 14 | 18 | 14 |
| `labelMedium` | `label` | 12 | 12 | 16 | 12 |
| `labelSmall` | `label` | 11 | 11 | 14 | 11 |
| `score` | `score` | 32 | 48 | 80 | `{min: 28, max: 56, minVw: 320, maxVw: 1440}` |

Notes:
- TV values reflect 10-foot-UI viewing distance (Material You / tvOS HIG envelope), not arithmetic scaling.
- Web envelopes give designers fluid behavior between 320px and 1440px viewport widths; scalar entries are used for variants where designers explicitly want no fluid scaling.
- Native platforms handle intra-deviceClass fragmentation (iPad mini vs iPad Pro, 1080p vs 4K TV) through their own density and font-scale systems; no SDUI-level intervention is needed.

### 1.3 Schema file shape

```json
{
  "$version": "3.0.0-matrix",
  "$sources": [
    "sitebuilder/assets/brand-nba.yml (Kinetic — informs `web` column)",
    "Material Design 3 type scale (`phone`, `tablet`)",
    "Apple HIG / Dynamic Type (`phone`, `tablet`)",
    "tvOS HIG + Google TV Material You (`tv`)"
  ],
  "categories": {
    "nba.typography.headline": {
      "familyRef": "nba.font.knockout",
      "weight": 360,
      "textCase": "uppercase",
      "lineHeight": 0.8
    }
  },
  "variants": {
    "nba.typography.headlineLarge": {
      "categoryRef": "nba.typography.headline",
      "size": {
        "phone":  32,
        "tablet": 40,
        "tv":     64,
        "web":    { "min": 28, "max": 64, "minVw": 320, "maxVw": 1440 }
      }
    }
  }
}
```

### 1.4 TextVariant enum audit

The current `TextVariant` enum has 16 values. The Kinetic categories include three intents (`data`, `button`, `caption`) that don't currently have wire enum values.

**Per AGENTS.md §11 (variant discipline), do not pre-add wire enum values.** Audit composer callsites first:

1. Grep every composer for `fontFamily` / `fontWeight` / `textCase` inline emissions and for `TextVariant` enum usage.
2. For each intent (`data`, `button`, `caption`), document whether an existing variant can express the design intent.
3. Add a wire enum value only where the audit produces a concrete composer that cannot use an existing variant.

Likely outcomes (subject to the audit):
- `data` — likely required for `BoxscoreTable` / `SeasonLeadersTable` stat columns (Roboto Condensed Uppercase has no existing variant). Add if audit confirms.
- `button` — likely expressible via `labelLarge` + the Button atom's existing weight field. Hold.
- `caption` — overlaps heavily with `bodySmall`. Hold.

Any addition follows the §1.2 sequence: schema → `make codegen` → renderer fallback on every platform → composer emits.

### 1.5 Create `TypographyTokens.java`

One constant group — semantic variants only. No category+step shortcuts ship on the wire.

### 1.6 Update `LayoutTokenRegistry.java`

Load `typography-tokens.json` at startup. Validate:

- Every `TextVariant` enum value has a matching variant entry
- Every variant's `categoryRef` resolves
- Every variant's `size.web` envelope has `min ≤ max` and `minVw < maxVw`
- Every category's `familyRef` resolves against `font-tokens.json`

---

## Phase 2 — Motion registry

### 2.1 Create `schema/motion-tokens.json`

```json
{
  "$version": "3.0.0-matrix",
  "$source": "sitebuilder/assets/brand-nba.yml",
  "easing": {
    "nba.motion.easing.default": "cubic-bezier(0.16, 1, 0.3, 1)",
    "nba.motion.easing.linear":  "linear"
  },
  "duration": {
    "nba.motion.duration.fast":    { "phone": 150, "tablet": 180, "tv": 250, "web": 200 },
    "nba.motion.duration.default": { "phone": 200, "tablet": 250, "tv": 350, "web": 300 },
    "nba.motion.duration.slow":    { "phone": 400, "tablet": 500, "tv": 700, "web": 600 },
    "nba.motion.duration.hero":    { "phone": 500, "tablet": 600, "tv": 900, "web": 800 }
  }
}
```

TV durations are longer than phone because animations traverse a larger viewport at a greater viewing distance. Easing is form-factor-flat. No `transition.*` semantic bundles — composers emit duration and easing as independent fields.

### 2.2 Wire integration

Add motion-token strings as valid values for animation-related properties in `sdui-schema.json`. Clients resolve to platform animation APIs (`UIView.animate` / Compose `tween` / CSS `transition-*`).

### 2.3 Create `MotionTokens.java`

Constants for the 2 easings + 4 durations.

---

## Phase 3 — Shadow registry

### 3.1 Extract shadow tiers from sitebuilder

Source: `assets/components/references/*.html` `box-shadow` values.

| Component | Shadow | Tier |
|---|---|---|
| dropdown | `0 1px 3px rgba(0,0,0,0.12)` | `sm` |
| carousel button | `0 2px 8px rgba(0,0,0,0.15)` | `md` |
| card hover | `0 4px 16px rgba(0,0,0,0.12)` | `lg` |
| modal overlay | `0 8px 32px rgba(0,0,0,0.25)` | `xl` |

### 3.2 Create `schema/shadow-tokens.json`

```json
{
  "$version": "3.0.0-matrix",
  "shadows": {
    "nba.shadow.sm": { "type": "drop", "x": 0, "y": 1, "blur": 3,  "spread": 0, "color": "rgba(0,0,0,0.12)" },
    "nba.shadow.md": { "type": "drop", "x": 0, "y": 2, "blur": 8,  "spread": 0, "color": "rgba(0,0,0,0.15)" },
    "nba.shadow.lg": { "type": "drop", "x": 0, "y": 4, "blur": 16, "spread": 0, "color": "rgba(0,0,0,0.12)" },
    "nba.shadow.xl": { "type": "drop", "x": 0, "y": 8, "blur": 32, "spread": 0, "color": "rgba(0,0,0,0.25)" }
  }
}
```

Shadows are form-factor-flat — same visual spec on every device class.

### 3.3 Wire integration

Add `"token:nba.shadow.*"` as a valid shorthand at every `shadow` field. Clients expand the shorthand to the full structured shadow at resolve time.

### 3.4 Create `ShadowTokens.java`

Constants for `SM`, `MD`, `LG`, `XL`.

---

## Phase 4 — Font registry

### 4.1 Create `schema/font-tokens.json`

```json
{
  "$version": "3.0.0-matrix",
  "$source": "sitebuilder/assets/brand-nba.yml",
  "families": {
    "nba.font.knockout": {
      "family": "Knockout",
      "fallback": "Arial Black, Impact, sans-serif",
      "weights": {
        "360": { "platform": { "ios": { "bundled": true, "postscriptName": "Knockout-HTF360FlyWeight" },
                                "android": { "bundled": true, "assetPath": "fonts/Knockout-49Liteweight.otf" },
                                "web":     { "src": "assets/fonts/Knockout/Knockout-49Liteweight.otf", "format": "opentype" } } },
        "395": { "platform": { "ios": { "bundled": true, "postscriptName": "Knockout-HTF395FlyWeight" },
                                "android": { "bundled": true, "assetPath": "fonts/Knockout-67FullBantamwt.otf" },
                                "web":     { "src": "assets/fonts/Knockout/Knockout-67FullBantamwt.otf", "format": "opentype" } } }
      }
    },
    "nba.font.roboto": {
      "family": "Roboto",
      "fallback": "Helvetica Neue, Arial, sans-serif",
      "weights": { "400": { "platform": { "ios": { "system": true }, "android": { "system": true }, "web": { "googleFonts": true } } },
                   "500": { "platform": { "ios": { "system": true }, "android": { "system": true }, "web": { "googleFonts": true } } },
                   "700": { "platform": { "ios": { "system": true }, "android": { "system": true }, "web": { "googleFonts": true } } },
                   "900": { "platform": { "ios": { "system": true }, "android": { "system": true }, "web": { "googleFonts": true } } } }
    },
    "nba.font.roboto.condensed": {
      "family": "Roboto Condensed",
      "fallback": "Arial Narrow, sans-serif",
      "weights": { "400": { "platform": { "ios": { "system": true }, "android": { "system": true }, "web": { "googleFonts": true } } },
                   "700": { "platform": { "ios": { "system": true }, "android": { "system": true }, "web": { "googleFonts": true } } } }
    }
  }
}
```

The typography registry's `weight` field selects which entry under `families.<familyRef>.weights` to load. Font names are never standalone wire values; they're only referenced via `familyRef`.

### 4.2 Asset bundling

- **iOS** — `.otf` files in `ios/Sources/SduiCore/Resources/Fonts/`; registered via `UIFont.register(from:)` at launch; PostScript names align with `postscriptName`
- **Android** — `.otf` files in `android/app/src/main/assets/fonts/`; surfaced via `Font(assetPath, weight)` for Compose
- **Web** — `.woff2` / `.otf` files served from `assets/fonts/Knockout/`; declared via `@font-face` at build time

Roboto and Roboto Condensed remain platform-provided (system fonts on iOS/Android, Google Fonts on web).

---

## Phase 5 — Spacing / radius: migrate to 4-column, drop raw layer

### 5.1 Schema migration

Rewrite `schema/spacing-tokens.json` and `schema/corner-radius-tokens.json`:

- **Drop** the `nba.space.raw.*` / `nba.radius.raw.*` palette layer entirely
- **Drop** the `phone.landscape`, `web.narrow`, `web.wide` columns
- **Inline** the 4-column matrix directly under each semantic entry

Result for spacing:

```json
{
  "$version": "3.0.0-matrix",
  "$source": "Kinetic Design System (Figma)",
  "spacing": {
    "nba.spacing.xs":  { "phone": 2,  "tablet": 2,  "tv": 4,  "web": 2  },
    "nba.spacing.sm":  { "phone": 4,  "tablet": 6,  "tv": 6,  "web": 4  },
    "nba.spacing.md":  { "phone": 12, "tablet": 15, "tv": 18, "web": 12 },
    "nba.spacing.lg":  { "phone": 16, "tablet": 20, "tv": 24, "web": 16 },
    "nba.spacing.xl":  { "phone": 32, "tablet": 40, "tv": 48, "web": 32 },
    "nba.spacing.2xl": { "phone": 40, "tablet": 48, "tv": 56, "web": 40 }
  }
}
```

Same shape for radius. No alias chain to walk; one lookup per token.

### 5.2 Confirm `LayoutTokens.java` coverage

Verify the constants exposed today still cover what composers actually emit. The Kinetic palette (`xs/sm/md/lg/xl/2xl` for spacing; `xs/sm/md/lg/xl/2xl/full` for radius) is the source of truth.

---

## Phase 6 — Server composer migration (spacing / radius / icon)

### 6.1 Audit each composer

For each of `ForYouComposer`, `ScheduleComposer`, `LiveComposer`, `HomeComposer`, `WatchComposer`, `GameDetailComposer`, `DemoScreenComposer`, `BoxscoreComposer`, `ScoreboardComposer`: enumerate every padding / gap / cornerRadius literal and every inline `"sdui:..."` icon string. Produce a per-file before/after table.

### 6.2 Replace literals with constants

Replace each tokenizable literal with the matching `LayoutTokens.*` or `IconTokens.*` constant. Values that don't fit stay raw and earn a one-line comment citing the AGENTS.md §3.6 exception class.

### 6.3 Verify

Run `./gradlew test` plus composer-output goldens. Round-trip tests on the client side (Phase 8) catch fixture regressions.

---

## Phase 7 — Client token resolver + codegen-baked registry

All three client `LayoutTokenResolver` implementations currently handle only `nba.spacing.*` and `nba.radius.*` and embed their data as hand-written tables in code. This phase replaces the hand-written tables with codegen-baked typed registries, extends every resolver to cover typography / motion / shadow, and wires the resolvers into platform-native form-factor change signals.

### 7.1 Codegen the registry into each client

`codegen/generate.sh` is extended to emit typed registry data per platform from `schema/*-tokens.json`:

| Platform | Output | Shape |
|---|---|---|
| iOS | `ios/Sources/SduiCore/Generated/LayoutTokenRegistry.swift` | `enum LayoutTokenRegistry { static let spacing: [String: FormFactorMatrix<Int>] = … }` |
| Android | `android/sdui-core/src/main/java/com/nba/sdui/core/generated/LayoutTokenRegistry.kt` | `object LayoutTokenRegistry { val spacing: Map<String, FormFactorMatrix<Int>> = … }` |
| Web | `web/src/generated/LayoutTokenRegistry.ts` | `export const LayoutTokenRegistry = { spacing: { … }, radius: { … }, typography: { … }, motion: { … }, shadow: { … } } as const;` |

Hand-written tables in today's resolvers are deleted. Generated files are checked in (same as `SduiModels`) so the build is reproducible.

### 7.2 Form-factor observers

Each resolver exposes a `currentFormFactor()` accessor backed by platform-native signals. Framework reactivity (SwiftUI `Environment`, Compose `CompositionLocal`, React `Context`) invalidates downstream views when form factor changes.

| Platform | Signals |
|---|---|
| iOS | `UITraitCollection.horizontalSizeClass`, `verticalSizeClass`, `UIDevice.orientationDidChangeNotification` |
| Android | `androidx.window.WindowSizeClass`, `Configuration.orientation` via `LocalConfiguration.current` |
| Web | `window.matchMedia` listeners + `ResizeObserver` on `<body>` |

**Prohibition:** no resolver may issue a network request as part of form-factor change handling. Tests in 7.6 assert zero outbound calls during simulated rotation / resize.

### 7.3 Typography resolution

Each platform's resolver adds a `typography(token, formFactor) -> TypographySpec` method:

1. Look up the variant
2. Read the `size` column for the current form factor
3. If the column is a scalar, use it directly
4. If the column is a fluid envelope (`web` only; other columns are always scalars), interpolate — `clamp()` on web, no-op elsewhere
5. Combine with the variant's `categoryRef` to produce the full spec (`familyRef`, `weight`, `textCase`, `lineHeight`, resolved size)

### 7.4 Shadow resolution

Each resolver adds a method that expands `"token:nba.shadow.md"` into the full structured shadow object. Shadows are form-factor-flat. `AtomicBox` on each platform routes `shadow` values through this method before applying.

### 7.5 Motion resolution

Each resolver adds two methods (or one with branching) to resolve `nba.motion.duration.*` to a platform-native duration (`Int` ms / `TimeInterval` seconds) and `nba.motion.easing.*` to a platform-native curve.

### 7.6 Tests

**Typography:**
- `resolveTypography("token:nba.typography.headlineLarge", "phone")` returns 32, Knockout, weight 360, uppercase
- `resolveTypography("token:nba.typography.bodyMedium", "web")` at viewport 768px returns interpolated value from the envelope; at viewport 1440px returns the max
- Unknown variant returns `null` / `undefined` and logs `token_resolver_missing`

**Form-factor reactivity (no round-trips):**
- Stub the HTTP layer
- Resolve a token, simulate rotation / resize / split-screen, resolve again
- Assert resolved value reflects the new form factor; HTTP layer received zero calls

**Shadow:**
- `resolveShadowToken("token:nba.shadow.md")` returns the full structured shadow
- Unknown shadow token returns `null` / `undefined`

**Motion:**
- `resolveMotionDuration("token:nba.motion.duration.fast", "phone")` returns 150
- `resolveMotionEasing("token:nba.motion.easing.default")` returns the platform-native curve

**iOS parity:**
- Create `ios/Tests/SduiCoreTests/LayoutTokenResolverTests.swift` with the same coverage Android and Web already have

---

## Phase 8 — Example fixture audit

27 example files in `schema/examples/` and 27 mirrored iOS test fixtures in `ios/Tests/SduiCoreTests/Fixtures/`. All tokenizable values updated to canonical `nba.*` strings; any legacy column references removed.

### 8.1 Replacement rules

| Raw value | Token |
|---|---|
| `0` | keep as `0` (§3.6 exception) |
| `2`, `4`, `12`, `16`, `32`, `40` (padding/gap) | `"token:nba.spacing.{xs|sm|md|lg|xl|2xl}"` |
| `2`, `4`, `12`, `16`, `24`, `32`, `9999` (cornerRadius) | `"token:nba.radius.{xs|sm|md|lg|xl|2xl|full}"` |
| Structured shadow matching a tier | `"token:nba.shadow.{sm|md|lg|xl}"` |
| Component-specific dimensions, calculated values | keep raw (§3.6 exception, document) |

### 8.2 Sync iOS fixtures

After updating `schema/examples/`, mirror all 27 files into `ios/Tests/SduiCoreTests/Fixtures/`. The two directories stay byte-identical.

### 8.3 Validate

Run the full test suite on each platform:

- Web: `web/src/__tests__/schemaRoundTrip.test.ts`
- Android: `android/sdui-core/src/test/java/com/nba/sdui/core/SchemaRoundTripTest.kt`
- iOS: `ios/Tests/SduiCoreTests/SduiModelsRoundTripTests.swift`

---

## Phase 9 — Documentation updates

### 9.1 `docs/sdui-design-system.md`

| Section | Change |
|---|---|
| Related-files table | Add the four new schema files and three new constant classes |
| §2.2 Spacing tokens | Update to 4-column matrix; reaffirm semantic-only wire |
| §2.4 Typography tokens | Replace "planned" with the full registry; semantic variants only on the wire |
| §2.5 Corner radius tokens | Update to 4-column matrix |
| §2.6 Shadow tokens | Replace "planned" with the 4-tier registry + shorthand expansion contract |
| New §2.7 Motion tokens | Document easing + 4 duration tiers with per-column values |
| New §2.8 Font registry | Document Knockout / Roboto / Roboto Condensed with per-platform resolution |
| §9 Gaps checklist | Check off typography, shadow, motion, font, size-tokens disposition |

### 9.2 `docs/client-implementors-contract.md`

| Section | Change |
|---|---|
| §16 Codegen Quick Reference | Note the codegen-baked `LayoutTokenRegistry` per platform |
| §17 Animation Guidelines | Reference motion tokens instead of hardcoded milliseconds |
| §18 Conformance Checklist | New item: `LayoutTokenResolver` resolves all token namespaces; zero network on rotation/resize |
| New subsection | Shadow shorthand expansion contract |

### 9.3 `docs/sdui-requirements-summary.md`

| Section | Change |
|---|---|
| §9g Theming & Dark Mode | Add typography / motion / shadow registries alongside color |
| §9s Figma Design Token Integration | Update status from "Deferred" to "Partial"; list all 8 families with their registry files; note Figma export pipeline remains deferred |
| §10 Implementation status table | Update theming/dark-mode row to reference all families |

### 9.4 Standing doc-sync rule

Record in `docs/sdui-design-system.md` and AGENTS.md: changes to `schema/*-tokens.json` and `docs/sdui-design-system.md` ship in the same PR.

---

## Phase 10 — AGENTS.md doctrine update (lands last)

After implementation lands, update AGENTS.md to codify what was built. Principle-first; specifics belong in `docs/sdui-requirements-summary.md`.

### 10.1 §3.4 — replace the `formFactor` roadmap paragraph

Replace lines 205–209 with:

> **`formFactor` is client-local, not a wire field.** Orientation, viewport, and breakpoint are resolved on the client. They are intentionally not in the request envelope: doing so would fragment the cache keyspace without composition benefit, because the server does not branch on them. `deviceClass` remains the only platform input the server reads.

### 10.2 §3.6 — rewrite for current state

Replace the existing §3.6 with:

> ### 3.6 Composers emit design-system tokens, not raw pixels
>
> - Composed payloads emit semantic token strings (e.g. `"token:nba.spacing.lg"`) for spacing, radius, typography, motion, shadow, color, and icon — never raw pixels or platform-native names.
> - One wire vocabulary per family. Color is the only family with two layers (primitive + semantic, because theming swaps the primitive); other families expose semantic names only. Raw or step-indexed sub-vocabularies stay server-internal.
> - Token resolution is fully client-local. Clients carry the registry in their build; no runtime fetch. Routine form-factor changes — rotation, resize, split-screen, same-`deviceClass` foldable transitions — re-resolve locally with no network call.
> - Raw integers remain acceptable only for: `0`; runtime-calculated values; intentionally non-responsive component dimensions; values with no semantic token (cite the gap in a comment).
> - Applies to all layout scalars and the structured fields `shadow`, `typography`, `animation`, `color`.

### 10.3 New §3.7 — token form-factor matrix

Insert after §3.6:

> ### 3.7 Token form-factor matrix
>
> The token registry uses the four native deviceClasses as columns: `phone`, `tablet`, `tv`, `web`. Native platforms handle intra-deviceClass fragmentation through their own density, font-scale, and size-class systems. The `web` column is the exception — because CSS units are absolute and the viewport is continuous — and may declare a fluid envelope (`min` / `max` bounded by viewport width) alongside or instead of a single value.
>
> Earlier breakpoint-style columns (orientation suffixes, narrow/wide splits) were speculative and are not canonical. Composition-level orientation or viewport decisions stay in composer logic keyed off `deviceClass`, not in the token shape.

### 10.4 §1.3 — short addendum

Append to §1.3:

> - Adding a new token name follows the same client-release sequence as adding a new enum value (§1.2): schema first, codegen second, client resolvers third, composer last. Until the registry update ships, the new token resolves to a neutral default; the composer must not emit it.

---

## Phase 11 — Cross-validation

### 11.1 Sitebuilder ↔ SDUI web parity (web column only)

Add a test that loads `brand-nba.yml`, runs `yaml_brand_spec_to_tokens()`, and asserts that every Kinetic step referenced by a SDUI variant's `web` envelope or scalar resolves to the same pixel value sitebuilder produces. Mobile and TV columns are not asserted — those have different sources of truth and are intentionally allowed to diverge.

### 11.2 Startup registry validation

`LayoutTokenRegistry.java` loads all 8 token files and fails fast on:

- Missing `TextVariant` enum values in the typography variant map
- Variants whose `categoryRef` doesn't resolve
- `web` envelopes with `min > max` or `minVw ≥ maxVw`
- Categories whose `familyRef` doesn't resolve against `font-tokens.json`
- Invalid easing curve syntax
- Missing shadow struct fields
- Unknown aliases in color tokens

### 11.3 Final regression sweep

1. `./gradlew compileJava` — server compiles with new constant classes
2. `./gradlew test` — server tests pass including new registry tests
3. `make codegen` — emits per-platform registries; regenerated outputs commit cleanly
4. `npx tsc --noEmit` in `web/` — no client-side regressions
5. Web / Android / iOS test suites pass with updated fixtures, new resolver tests, zero-network-on-resize assertions
6. Visual smoke: rotate device / resize browser / open split-screen; confirm typography, spacing, shadow reflow without a fetch
7. Network spot-check: composed payloads contain canonical token strings (`"token:nba.spacing.lg"`, `"token:nba.typography.headlineLarge"`, `"token:nba.shadow.md"`)
8. Doc consistency: `docs/sdui-design-system.md` tables match the corresponding `schema/*-tokens.json` files

---

## Resolved decisions

1. **Cross-platform posture:** sitebuilder/Kinetic is a capability target for the web surface; phone, tablet, TV are peer platforms sourced from native conventions.
2. **Wire vocabulary:** one layer per family, except color (primitive + semantic for theming). Raw and step-indexed sub-vocabularies are not on the wire.
3. **Typography wire form:** semantic variants only (`nba.typography.headlineLarge`). Categories exist as server-internal spec data, not addressable on the wire.
4. **Bundled registry:** all token data ships in the client build via `make codegen`. No runtime fetch.
5. **Client-local form factor:** rotation, resize, split-screen, same-`deviceClass` foldable transitions resolve entirely on-device. Re-fetch only when `deviceClass` changes.
6. **`formFactor` not on the envelope:** keeps the envelope cache keyspace from fragmenting; `deviceClass` is sufficient because the server doesn't branch on finer-grained form factor.
7. **Matrix shape:** 4 columns (`phone`, `tablet`, `tv`, `web`). Earlier columns retired. `web` supports a fluid envelope.
8. **New `TextVariant` enum values:** deferred — audit composer needs first, add only where existing variants can't express the design intent.
9. **`schema/size-tokens.json`:** already deleted; no plan work.
10. **Color bug fixes (scrim, brand.nba) and Android `score` variant bug:** out of scope; ship in a separate PR.
11. **Legacy aliases / back-compat:** none. All code and fixtures update directly to canonical `nba.*` tokens.
12. **Doc-sync rule:** mandatory — token JSON and `docs/sdui-design-system.md` change in the same PR.
13. **AGENTS.md doctrine update:** lands last, principle-first, codifies what was built.

---

## Relevant files

| File | Role |
|---|---|
| **Schema — existing** | |
| `schema/color-tokens.json` | Design-sourced (Kinetic) |
| `schema/spacing-tokens.json` | Design-sourced; migrate to 4-column in Phase 5 |
| `schema/corner-radius-tokens.json` | Design-sourced; migrate to 4-column in Phase 5 |
| `schema/icon-tokens.json` | Code-derived |
| `schema/style-tokens.json` | Code-derived |
| **Schema — new** | |
| `schema/typography-tokens.json` | Phase 1 |
| `schema/motion-tokens.json` | Phase 2 |
| `schema/shadow-tokens.json` | Phase 3 |
| `schema/font-tokens.json` | Phase 4 |
| **Server — existing** | |
| `ColorTokens.java`, `LayoutTokens.java`, `IconTokens.java` | Constants |
| `AtomicCompositeBuilder.java` | Primary migration target — heavily adopted |
| `LayoutTokenRegistry.java` | Runtime resolver + validation |
| **Server — composers to migrate** | |
| `ForYouComposer`, `ScheduleComposer`, `LiveComposer`, `HomeComposer`, `WatchComposer`, `GameDetailComposer`, `DemoScreenComposer`, `BoxscoreComposer`, `ScoreboardComposer` | All in `server/src/main/java/com/nba/sdui/service/` |
| **Server — new constants** | |
| `TypographyTokens.java`, `MotionTokens.java`, `ShadowTokens.java` | Phases 1–3 |
| **Client resolvers** | |
| `web/src/utils/LayoutTokenResolver.ts` | Phase 7 |
| `android/sdui-core/src/main/java/com/nba/sdui/core/renderer/LayoutTokenResolver.kt` | Phase 7 |
| `ios/Sources/SduiCore/Rendering/LayoutTokenResolver.swift` | Phase 7 |
| **Client tests** | |
| `web/src/utils/LayoutTokenResolver.test.ts` | Existing — extend |
| `android/sdui-core/src/test/java/com/nba/sdui/core/renderer/LayoutTokenResolverTest.kt` | Existing — extend |
| `ios/Tests/SduiCoreTests/LayoutTokenResolverTests.swift` | New (Phase 7) |
| **Codegen outputs (Phase 7)** | |
| `ios/Sources/SduiCore/Generated/LayoutTokenRegistry.swift` | New |
| `android/sdui-core/src/main/java/com/nba/sdui/core/generated/LayoutTokenRegistry.kt` | New |
| `web/src/generated/LayoutTokenRegistry.ts` | New |
| **Fixtures (Phase 8)** | |
| `schema/examples/*.json` (27 files) | Source of truth |
| `ios/Tests/SduiCoreTests/Fixtures/*.json` (27 files) | Mirror — stay synced |
| **Documentation (Phase 9)** | |
| `docs/sdui-design-system.md`, `docs/client-implementors-contract.md`, `docs/sdui-requirements-summary.md` | Sync |
| **Doctrine (Phase 10)** | |
| `AGENTS.md` §1.3, §3.4, §3.6, §3.7 | Final update after implementation |
