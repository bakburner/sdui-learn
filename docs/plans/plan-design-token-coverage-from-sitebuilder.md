# Plan: Close Design Token Coverage Gaps Using Sitebuilder Data

**Status:** Proposed
**Created:** 2026-05-08
**Scope:** Typography, motion, shadow, and font token registries + client resolver updates + example fixture audit
**Depends on:** plan-design-token-adoption.md (spacing/radius/color already covered there)
**Source:** Sitebuilder `assets/brand-nba.yml` + `backend/src/services/design_tokens.rs`

---

## Objectives

1. Create a typography token registry (`schema/typography-tokens.json`) backed by Kinetic Design System data already extracted in sitebuilder's brand spec
2. Create a motion token registry (`schema/motion-tokens.json`) with easing and duration tiers
3. Create a shadow token registry (`schema/shadow-tokens.json`) with elevation tiers extracted from sitebuilder component references
4. Create a font family registry (`schema/font-tokens.json`) with the 3 Kinetic font families and per-platform resolution metadata
5. Populate all registries with web values from sitebuilder as the baseline, with form-factor columns as documented placeholders pending design validation

---

## Acceptance Criteria

### Token registries (Phases 1–4)

- [ ] `schema/typography-tokens.json` exists in `2.0.0-matrix` format with entries for all 16 `TextVariant` enum values plus `data`, `button`, `caption`
- [ ] Each typography entry includes `familyRef`, `weight`, `lineHeight`, `textCase`, and `size` with per-form-factor values
- [ ] Web column values match sitebuilder's `brand-nba.yml` Kinetic typography system exactly
- [ ] Phone/tablet/tv columns have documented placeholder values with `$placeholder: true` annotation
- [ ] `schema/motion-tokens.json` exists with `easing.default` and 4 duration tiers (`fast`, `default`, `slow`, `hero`)
- [ ] `schema/shadow-tokens.json` exists with at least 3 elevation tiers (`sm`, `md`, `lg`)
- [ ] `schema/font-tokens.json` exists with Knockout, Roboto, Roboto Condensed entries and platform resolution hints
- [ ] `LayoutTokenRegistry.java` loads and validates all new files at startup
- [ ] Server composers can reference tokens via constants (e.g. `TypographyTokens.HEADLINE_LARGE`)
- [ ] `tokens_to_sdui_export()` in sitebuilder produces output compatible with new registry shapes (validation test)

### Client resolver updates (Phase 6)

- [ ] All three `LayoutTokenResolver` implementations (iOS, Android, Web) resolve `nba.typography.*`, `nba.motion.*`, and `nba.shadow.*` namespaces
- [ ] Shadow token shorthand (`"token:nba.shadow.md"`) resolves to the full structured `Shadow` object on each platform
- [ ] Motion token strings resolve to platform-native animation values (easing curves, duration milliseconds)
- [ ] `TextVariant` enum updated on all platforms to include `data`, `button`, `caption`; renderers map them to platform fonts
- [ ] iOS `LayoutTokenResolver` has a test file (`LayoutTokenResolverTests.swift`) with parity to existing Android/Web tests
- [ ] All three platforms have token resolver tests covering the new namespaces (typography, motion, shadow)
- [ ] No legacy aliases or backward-compat shims — resolvers use the canonical `nba.*` token names only

### Example fixture audit (Phase 7)

- [ ] All 27 `schema/examples/*.json` files use `"token:nba.spacing.*"` / `"token:nba.radius.*"` for padding, gap, and cornerRadius values (no raw integers where a token exists)
- [ ] Shadow values in example files use `"token:nba.shadow.*"` shorthand where applicable
- [ ] All 27 `ios/Tests/SduiCoreTests/Fixtures/*.json` files synced with updated `schema/examples/`
- [ ] Round-trip tests on all platforms pass with the updated fixtures

### Documentation updates (Phase 8)

- [ ] `docs/sdui-design-system.md` updated: §2.4 typography populated, §2.6 shadow populated, new §2.7 motion registry, new §2.8 font registry, §9 gap items checked off
- [ ] `docs/client-implementors-contract.md` updated: token resolver contract documented, new `TextVariant` values listed
- [ ] `docs/sdui-requirements-summary.md` updated: §9s token integration status reflects new registries and client resolver work

---

## Non-Goals

- Migrating existing composers to use typography/motion tokens (separate PR per composer)
- Replacing `TextVariant` enum — it stays as the wire value; the registry provides the spec behind each variant
- Legacy aliases or backward-compat bridges — all clients and fixtures update to canonical `nba.*` tokens directly

---

## Phase 1: Typography Token Registry

### 1.1 Extract web values from sitebuilder

Source: `brand-nba.yml` → `typography.categories` (9 categories with family_ref, weight, lineHeight, textCase, steps)

Map to SDUI `TextVariant` enum values:

| TextVariant | Sitebuilder category | Family | Weight | textCase | Web size (from Figma steps) |
|---|---|---|---|---|---|
| `displayLarge` | display | Knockout 395 | 395 | uppercase | step xxl (160px) → pick contextual |
| `displayMedium` | display | Knockout 395 | 395 | uppercase | step 10 (72px) |
| `displaySmall` | display | Knockout 395 | 395 | uppercase | step 08 (48px) |
| `headlineLarge` | headline | Knockout 360 | 360 | uppercase | step 10 (48px) |
| `headlineMedium` | headline | Knockout 360 | 360 | uppercase | step 07 (28px) |
| `headlineSmall` | headline | Knockout 360 | 360 | uppercase | step 04 (20px) |
| `titleLarge` | title | Roboto | 500 | none | 22px |
| `titleMedium` | title | Roboto | 500 | none | 16px |
| `titleSmall` | title | Roboto | 500 | none | 14px |
| `bodyLarge` | body | Roboto | 400 | none | 16px |
| `bodyMedium` | body | Roboto | 400 | none | 14px |
| `bodySmall` | body | Roboto | 400 | none | 12px |
| `labelLarge` | label | Roboto | 400 | uppercase | 14px |
| `labelMedium` | label | Roboto | 400 | uppercase | 12px |
| `labelSmall` | label | Roboto | 400 | uppercase | 11px |
| `score` | score | Knockout 360 | 360 | uppercase | 32px |
| `data` | data | Roboto Condensed | 400 | uppercase | 14px |
| `button` | button | Roboto | 700 | none | 14px |
| `caption` | caption | Roboto | 400 | none | 12px |

### 1.2 Create `schema/typography-tokens.json`

Shape:

```json
{
  "$version": "2.0.0-matrix",
  "$source": "sitebuilder/assets/brand-nba.yml (Kinetic Design System)",
  "families": {
    "nba.font.knockout.360": { "family": "Knockout 360", "fallback": "Arial Black, Impact, sans-serif" },
    "nba.font.knockout.395": { "family": "Knockout 395", "fallback": "Arial Black, Impact, sans-serif" },
    "nba.font.roboto": { "family": "Roboto", "fallback": "Helvetica Neue, Arial, sans-serif" },
    "nba.font.roboto.condensed": { "family": "Roboto Condensed", "fallback": "Arial Narrow, sans-serif" }
  },
  "variants": {
    "nba.typography.headlineLarge": {
      "familyRef": "nba.font.knockout.360",
      "weight": 360,
      "textCase": "uppercase",
      "lineHeight": 1.0,
      "size": { "phone": 24, "phone.landscape": 24, "tablet": 32, "tv": 48, "web.narrow": 24, "web.wide": 48 }
    }
  }
}
```

Phone/tablet/tv sizes: use 0.5x/0.67x/1.0x of web.wide as initial placeholders. Mark with `$placeholder` comment.

### 1.3 Add `data`, `button`, `caption` to `TextVariant` enum

Update `schema/sdui-schema.json` → add 3 new enum values → run `make codegen` → update client renderers on all platforms (see Phase 6.4 for platform-specific details).

### 1.4 Create `TypographyTokens.java`

Constants class with `HEADLINE_LARGE = "token:nba.typography.headlineLarge"` etc. Used by composers.

### 1.5 Update `LayoutTokenRegistry.java`

Load `typography-tokens.json`, validate family references resolve, validate all `TextVariant` enum values have entries.

---

## Phase 2: Motion Token Registry

### 2.1 Create `schema/motion-tokens.json`

Source: `brand-nba.yml` → `motion.easing` + `motion.duration`

```json
{
  "$version": "2.0.0-matrix",
  "$source": "sitebuilder/assets/brand-nba.yml (NBA Brand Guidelines)",
  "palette": {
    "nba.motion.easing.default": { "*": "cubic-bezier(0.16, 1, 0.3, 1)" },
    "nba.motion.easing.linear": { "*": "linear" },
    "nba.motion.duration.fast": { "phone": 150, "tablet": 180, "tv": 250, "web.narrow": 150, "web.wide": 200 },
    "nba.motion.duration.default": { "phone": 200, "tablet": 250, "tv": 350, "web.narrow": 200, "web.wide": 300 },
    "nba.motion.duration.slow": { "phone": 400, "tablet": 500, "tv": 700, "web.narrow": 400, "web.wide": 600 },
    "nba.motion.duration.hero": { "phone": 500, "tablet": 600, "tv": 900, "web.narrow": 500, "web.wide": 800 }
  },
  "semantic": {
    "nba.motion.transition.default": { "easing": "nba.motion.easing.default", "duration": "nba.motion.duration.default" },
    "nba.motion.transition.fast": { "easing": "nba.motion.easing.default", "duration": "nba.motion.duration.fast" },
    "nba.motion.transition.slow": { "easing": "nba.motion.easing.default", "duration": "nba.motion.duration.slow" }
  }
}
```

TV/phone scaling rationale: TV animations traverse larger viewport distances (1.25-1.5x duration). Phone is tighter (0.75-0.85x of web).

### 2.2 Wire schema support

Add `MotionToken` as valid value for animation-related properties in `sdui-schema.json`. Clients resolve to platform animation APIs (UIView.animate on iOS, Compose animateXAsState on Android, CSS transition on web).

### 2.3 Create `MotionTokens.java`

Constants class: `DURATION_FAST = "token:nba.motion.duration.fast"` etc.

---

## Phase 3: Shadow Token Registry

### 3.1 Extract shadow patterns from sitebuilder components

Source: `assets/components/references/*.html` CSS `box-shadow` values

| Component | Shadow value | Proposed tier |
|---|---|---|
| carousel button | `0 2px 8px rgba(0,0,0,0.15)` | `md` |
| card hover | `0 4px 16px rgba(0,0,0,0.12)` | `lg` |
| modal overlay | `0 8px 32px rgba(0,0,0,0.25)` | `xl` |
| dropdown | `0 1px 3px rgba(0,0,0,0.12)` | `sm` |

### 3.2 Create `schema/shadow-tokens.json`

```json
{
  "$version": "2.0.0-matrix",
  "palette": {
    "nba.shadow.sm": {
      "*": { "type": "drop", "x": 0, "y": 1, "blur": 3, "spread": 0, "color": "rgba(0,0,0,0.12)" }
    },
    "nba.shadow.md": {
      "*": { "type": "drop", "x": 0, "y": 2, "blur": 8, "spread": 0, "color": "rgba(0,0,0,0.15)" }
    },
    "nba.shadow.lg": {
      "*": { "type": "drop", "x": 0, "y": 4, "blur": 16, "spread": 0, "color": "rgba(0,0,0,0.12)" }
    },
    "nba.shadow.xl": {
      "*": { "type": "drop", "x": 0, "y": 8, "blur": 32, "spread": 0, "color": "rgba(0,0,0,0.25)" }
    }
  }
}
```

Shadows are form-factor-flat (same visual spec everywhere). The `"*"` wildcard handles this.

### 3.3 Wire schema alignment

Ensure `Shadow` struct in `sdui-schema.json` matches the token shape. Add `"token:nba.shadow.md"` as valid shorthand that clients expand to the full struct.

---

## Phase 4: Font Family Registry

### 4.1 Create `schema/font-tokens.json`

```json
{
  "$version": "2.0.0-matrix",
  "$source": "sitebuilder/assets/brand-nba.yml",
  "families": {
    "nba.font.knockout.360": {
      "family": "Knockout 360",
      "fallback": "Arial Black, Impact, sans-serif",
      "platform": {
        "ios": { "bundled": true, "postscriptName": "Knockout-HTF360FlyWeight" },
        "android": { "bundled": true, "assetPath": "fonts/knockout_360.otf" },
        "web": { "src": "assets/fonts/Knockout-360.woff2", "format": "woff2" }
      }
    },
    "nba.font.knockout.395": {
      "family": "Knockout 395",
      "fallback": "Arial Black, Impact, sans-serif",
      "platform": {
        "ios": { "bundled": true, "postscriptName": "Knockout-HTF395FlyWeight" },
        "android": { "bundled": true, "assetPath": "fonts/knockout_395.otf" },
        "web": { "src": "assets/fonts/Knockout-395.woff2", "format": "woff2" }
      }
    },
    "nba.font.roboto": {
      "family": "Roboto",
      "fallback": "Helvetica Neue, Arial, sans-serif",
      "platform": {
        "ios": { "system": true },
        "android": { "system": true },
        "web": { "googleFonts": true }
      }
    },
    "nba.font.roboto.condensed": {
      "family": "Roboto Condensed",
      "fallback": "Arial Narrow, sans-serif",
      "platform": {
        "ios": { "system": true },
        "android": { "system": true },
        "web": { "googleFonts": true }
      }
    }
  }
}
```

Platform resolution is informational — clients already bundle fonts. This registry documents the mapping so token resolution is unambiguous.

---

## Phase 5: Doc Sync and Validation

### 5.1 Update `docs/sdui-design-system.md`

- Add §2.6 Typography token registry
- Add §2.7 Motion token registry
- Add §2.8 Shadow token registry
- Add §2.9 Font family registry
- Check off §9 gap items: typography, shadow, Figma integration items

### 5.2 Startup validation

`LayoutTokenRegistry.java` loads all 4 new files. Fail-fast on:
- Missing `TextVariant` enum values in typography registry
- Invalid easing curve syntax in motion registry
- Missing shadow struct fields
- Unknown family references in typography entries

### 5.3 Cross-validate with sitebuilder

Add a test that loads `brand-nba.yml`, runs `yaml_brand_spec_to_tokens()`, and asserts the web column values in SDUI's typography registry match sitebuilder's extracted values. Prevents drift between the two systems.

---

## Phase 6: Client Token Resolver Updates and Tests

All three client `LayoutTokenResolver` implementations currently handle only `nba.spacing.*` and `nba.radius.*`. They need to resolve the new namespaces introduced in Phases 1–3 and support the 3 new `TextVariant` enum values from Phase 1.3.

### 6.1 Add typography token resolution

Each platform's `LayoutTokenResolver` adds the `nba.typography.*` palette and semantic entries from `schema/typography-tokens.json`. Typography tokens resolve to structured objects (not single integers), so each resolver needs a new `resolveTypography(tokenName, formFactor)` method that returns `{ familyRef, weight, textCase, lineHeight, size }`.

| Platform | File | Method to add |
|---|---|---|
| Web | `web/src/utils/LayoutTokenResolver.ts` | `resolveTypography(token, formFactor): TypographySpec` |
| Android | `android/sdui-core/src/.../LayoutTokenResolver.kt` | `typography(token, formFactor): TypographySpec` |
| iOS | `ios/Sources/SduiCore/Rendering/LayoutTokenResolver.swift` | `typography(_ token:, formFactor:) -> TypographySpec` |

### 6.2 Add shadow token resolution

Shadow tokens resolve to structured `Shadow` objects, not scalars. Each resolver adds a `resolveShadow(tokenName)` method that expands `"token:nba.shadow.md"` into the full `{ type, x, y, blur, spread, color }` struct. Shadows are form-factor-flat (`"*"` wildcard).

| Platform | File | Method to add |
|---|---|---|
| Web | `web/src/utils/LayoutTokenResolver.ts` | `resolveShadowToken(token): Shadow \| undefined` |
| Android | `android/sdui-core/src/.../LayoutTokenResolver.kt` | `shadowSpec(token): ShadowSpec?` |
| iOS | `ios/Sources/SduiCore/Rendering/LayoutTokenResolver.swift` | `shadowSpec(_ token:) -> ShadowSpec?` |

Wire integration: `AtomicBox` on each platform checks whether a `shadow` value is a `"token:…"` string and routes it through the shadow resolver before applying.

### 6.3 Add motion token resolution

Motion tokens resolve to platform-native animation values. Duration tokens resolve to milliseconds (integer). Easing tokens resolve to platform-native curve representations.

| Platform | Duration resolves to | Easing resolves to |
|---|---|---|
| Web | `number` (ms) for CSS `transition-duration` | `string` for CSS `transition-timing-function` |
| Android | `Int` (ms) for `tween(durationMillis)` | `Easing` for `tween(easing)` |
| iOS | `TimeInterval` (seconds) for `withAnimation(.easeInOut(duration:))` | `Animation` curve |

### 6.4 Add `data`, `button`, `caption` to `TextVariant` on all platforms

After `make codegen` regenerates the models, update each platform's `TextVariant` renderer to map the 3 new values to native fonts:

| Platform | File(s) to update |
|---|---|
| iOS | `ios/Sources/SduiCore/Rendering/TextVariantResolver.swift` — add `case data, button, caption` with font mappings |
| Android | Codegen handles the enum; renderer's `when` block adds the 3 new branches |
| Web | Codegen handles the type; renderer's switch adds the 3 new branches |

**Pre-existing bug fix (Android):** `AtomicText.kt` is missing a mapping for the existing `score` variant — it currently falls back to `bodyMedium` instead of monospaced/tabular-numeral typography. Fix this in the same pass: add `score` to Android's `mapTypographyVariant()` with bold tabular-numeral treatment matching the iOS (48pt bold rounded) and Web (28px/800, tabular nums) implementations.

### 6.5 Create iOS `LayoutTokenResolverTests.swift`

iOS is the only platform missing a `LayoutTokenResolver` test file. Create `ios/Tests/SduiCoreTests/LayoutTokenResolverTests.swift` with parity to the existing Android (`LayoutTokenResolverTest.kt`) and Web (`LayoutTokenResolver.test.ts`) tests:

- Numeric scalar passes through unchanged
- `null` resolves to 0
- Semantic spacing/radius tokens resolve per form factor
- Unknown tokens return 0 and log `token_resolver_missing`
- Alias chain resolves (e.g. `nba.radius.full` → `nba.radius.raw.9999`)

### 6.6 Add tests for new token namespaces on all platforms

Extend tests on all three platforms to cover the new namespaces:

**Typography resolver tests:**
- `resolveTypography("token:nba.typography.headlineLarge", "phone")` returns correct size, family, weight
- `resolveTypography("token:nba.typography.data", "web.wide")` returns Roboto Condensed, 400, uppercase, 14px
- Unknown typography token returns `null`/`undefined`

**Shadow resolver tests:**
- `resolveShadowToken("token:nba.shadow.md")` returns `{ type: "drop", x: 0, y: 2, blur: 8, spread: 0, color: "rgba(0,0,0,0.15)" }`
- Unknown shadow token returns `null`/`undefined`
- Non-`token:` string returns `null`/`undefined`

**Motion resolver tests:**
- `resolveMotionDuration("token:nba.motion.duration.fast", "phone")` returns 150
- `resolveMotionEasing("token:nba.motion.easing.default")` returns the platform-native curve value
- Unknown motion token returns fallback

**`TextVariant` renderer tests:**
- `data`, `button`, `caption` render with the correct font family and weight on each platform

---

## Phase 7: Example JSON Fixture Audit and Update

27 example files in `schema/examples/` and 27 mirrored iOS test fixtures in `ios/Tests/SduiCoreTests/Fixtures/` contain a mix of raw integer values and token strings. All tokenizable values must be updated to use the canonical `nba.*` token strings.

### 7.1 Audit scope

Current state (from codebase search):

| Property | Files with raw integers | Files with tokens | Action |
|---|---|---|---|
| `padding` (`top`/`bottom`/`start`/`end`) | 15+ files, ~500 raw values | 6 files, ~64 token refs | Replace raw integers with `"token:nba.spacing.*"` where a semantic token exists |
| `gap` | 15+ files | Included in above | Same |
| `cornerRadius` | 10+ files | 0 | Replace with `"token:nba.radius.*"` where a semantic tier matches |
| `shadow` | 9 files (structured objects) | 0 | Replace with `"token:nba.shadow.*"` shorthand where a tier matches |

### 7.2 Replacement rules

| Raw value | Token replacement | Notes |
|---|---|---|
| `0` | Keep as `0` | No semantic value for zero (AGENTS.md §3.6 exception) |
| `2` | `"token:nba.spacing.xs"` | |
| `4` | `"token:nba.spacing.sm"` | |
| `8` | Keep raw or map to `nba.space.raw.8` | No semantic alias; evaluate per-usage |
| `12` | `"token:nba.spacing.md"` | |
| `16` | `"token:nba.spacing.lg"` | |
| `32` | `"token:nba.spacing.xl"` | |
| `40` | `"token:nba.spacing.2xl"` | |
| Corner radius `4` | `"token:nba.radius.sm"` | |
| Corner radius `12` | `"token:nba.radius.md"` | |
| Corner radius `16` | `"token:nba.radius.lg"` | |
| Corner radius `9999` | `"token:nba.radius.full"` | |
| Component-specific dimensions (card widths, image sizes) | Keep raw | Per AGENTS.md §3.6 exceptions |

### 7.3 Shadow shorthand migration

Replace structured shadow objects in example files with token shorthand where a tier matches:

```json
// Before
"shadow": { "color": "rgba(0,0,0,0.15)", "radius": 8, "offsetX": 0, "offsetY": 2 }

// After
"shadow": "token:nba.shadow.md"
```

Shadows that don't match a tier (custom values) remain as structured objects.

### 7.4 Sync iOS test fixtures

After updating `schema/examples/`, copy all 27 files to `ios/Tests/SduiCoreTests/Fixtures/` to keep them in sync. Verify round-trip tests pass on all platforms:

- Web: `web/src/__tests__/schemaRoundTrip.test.ts`
- Android: `android/sdui-core/src/test/java/com/nba/sdui/core/SchemaRoundTripTest.kt`
- iOS: `ios/Tests/SduiCoreTests/SduiModelsRoundTripTests.swift`

### 7.5 Validate no regressions

Run the full test suite on each platform after fixture updates to confirm:
- All round-trip decode/re-encode tests pass with token strings in place of raw integers
- `LayoutTokenResolver` correctly resolves the token strings that now appear in fixtures
- No visual regressions (dev server renders identically)

---

## Phase 8: Documentation Updates

### 8.1 Update `docs/sdui-design-system.md`

| Section | Change |
|---|---|
| Related files table | Add `schema/typography-tokens.json`, `schema/motion-tokens.json`, `schema/shadow-tokens.json`, `schema/font-tokens.json`, `TypographyTokens.java`, `MotionTokens.java` |
| "Planned" callout | Remove — typography, shadow, and motion registries are no longer awaiting design validation |
| §2.4 Typography tokens | Replace "planned/awaiting" status with full registry reference: the 19 `TextVariant` values (16 original + `data`, `button`, `caption`), family references, weights, text cases, and per-form-factor size table |
| §2.6 Shadow tokens | Replace "planned/awaiting" status with the 4-tier registry (`sm`, `md`, `lg`, `xl`) and the `"token:nba.shadow.*"` shorthand expansion contract |
| New §2.7 Motion token registry | Add section documenting easing curves and 4 duration tiers with per-form-factor values |
| New §2.8 Font family registry | Add section documenting Knockout 360/395, Roboto, Roboto Condensed with per-platform resolution hints |
| §4 Figma-to-wire mapping | Add rows for shadow tokens (`"token:nba.shadow.md"`) and motion tokens |
| §9 Gaps checklist | Check off: typography token registry, shadow token registry, size token disposition |

### 8.2 Update `docs/client-implementors-contract.md`

| Section | Change |
|---|---|
| §16 Codegen Quick Reference | Note the 3 new `TextVariant` values (`data`, `button`, `caption`) |
| §17 Renderer Animation Guidelines | Reference motion tokens for timing values instead of hardcoded milliseconds (e.g. "Content transitions: `token:nba.motion.duration.default`" instead of "300ms") |
| §18 Conformance Checklist | Add item: `C21` — Client `LayoutTokenResolver` resolves all token namespaces: `nba.spacing.*`, `nba.radius.*`, `nba.typography.*`, `nba.motion.*`, `nba.shadow.*` |
| New subsection under §4a or standalone | Document the shadow token shorthand expansion contract: `AtomicBox` receives `"token:nba.shadow.md"` as a shadow value → resolver expands to the full `Shadow` struct before applying |

### 8.3 Update `docs/sdui-requirements-summary.md`

| Section | Change |
|---|---|
| §9g Theming & Dark Mode | Add mention of typography, motion, and shadow token registries alongside the existing color/variant description |
| §9s Figma Design Token Integration | Update status from "Deferred" to "Partial" — note that typography, motion, shadow, and font registries are now built from Kinetic/sitebuilder data; spacing and radius already covered by sibling plan; Figma export pipeline remains deferred |
| §9s requirements list | Expand from "Two machine-readable token registries" to list all 8 token families with their registry files |
| Implementation status table (§10) | Update "Theming / dark mode" row to reference all token families and the new client resolver work |
