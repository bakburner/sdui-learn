# Plan: Close Design Token Coverage Gaps Using Sitebuilder Data

**Status:** Proposed
**Created:** 2026-05-08
**Scope:** Typography, motion, shadow, and font token registries
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

- [ ] `schema/typography-tokens.json` exists in `2.0.0-matrix` format with entries for all 16 `TextVariant` enum values plus `data`, `button`, `caption`
- [ ] Each typography entry includes `familyRef`, `weight`, `lineHeight`, `textCase`, and `size` with per-form-factor values
- [ ] Web column values match sitebuilder's `brand-nba.yml` Kinetic typography system exactly
- [ ] Phone/tablet/tv columns have documented placeholder values with `$placeholder: true` annotation
- [ ] `schema/motion-tokens.json` exists with `easing.default` and 4 duration tiers (`fast`, `default`, `slow`, `hero`)
- [ ] `schema/shadow-tokens.json` exists with at least 3 elevation tiers (`sm`, `md`, `lg`)
- [ ] `schema/font-tokens.json` exists with Knockout, Roboto, Roboto Condensed entries and platform resolution hints
- [ ] `LayoutTokenRegistry.java` loads and validates all new files at startup
- [ ] `docs/sdui-design-system.md` updated to reflect new registries (§2 additions, §9 gap items checked off)
- [ ] Server composers can reference tokens via constants (e.g. `TypographyTokens.HEADLINE_LARGE`)
- [ ] `tokens_to_sdui_export()` in sitebuilder produces output compatible with new registry shapes (validation test)

---

## Non-Goals

- Migrating existing composers to use typography/motion tokens (separate PR per composer)
- Client-side rendering changes (clients already resolve `TextVariant`; new registries add data backing)
- Replacing `TextVariant` enum — it stays as the wire value; the registry provides the spec behind each variant

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

Update `schema/sdui-schema.json` → add 3 new enum values → run `make codegen` → update client renderers with fallback handling.

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
