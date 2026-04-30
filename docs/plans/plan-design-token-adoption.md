# Plan: Adopt the SDUI Design Token System

**Status:** Proposed  
**Created:** 2026-04-29  
**Scope:** Full design token system — all 8 token families, server + client  
**Depends on:** AGENTS.md §3.6 governance rule (Phase 0)

---

## Problem Statement

We built a full design token system — 8 token families with definition files,
wire schema support, and client resolvers on all three platforms — but only 3 of
8 families are actually adopted end-to-end. Worse, the non-color token files
(spacing, radius, size, shadow, typography) were **engineering speculation** —
fabricated values with invented form-factor multipliers that nobody validated
against real design intent. The values in the JSON files, the design system doc,
and the plan's reference tables all disagree with each other and with what the
code actually emits.

**Only `color-tokens.json` came from the design team.** Everything else was
guessed.

## Goal

1. **Rebuild** the speculative token palettes from actual code usage + validated
   design input on form-factor scaling.
2. **Adopt** the rebuilt tokens in server composers so the wire emits token
   strings.
3. **Sync** `docs/sdui-design-system.md` whenever token files change — one
   source of truth, zero drift.

---

## Ground Truth: What the Code Actually Uses

Extracted from `AtomicCompositeBuilder.java` (the only place that matters):

### Padding values (actual usage)

| Value | Sites | Notes |
|-------|-------|-------|
| 0 | 97 | Keep as literal |
| 16 | 77 | Dominant inset |
| 12 | 31 | Secondary inset |
| 8 | 23 | Tight inset |
| 4 | 23 | Minimal inset |
| 14 | 13 | Between 12 and 16 |
| 2 | 11 | Hairline |
| 6 | 10 | Between 4 and 8 |
| 10 | 7 | Between 8 and 12 |
| 32 | 2 | Section-level |
| 24 | 1 | Large |
| 20 | 1 | One-off |
| 18 | 1 | One-off |

### Corner radius values (actual usage)

| Value | Sites | Notes |
|-------|-------|-------|
| 12 | 5 | Card radius |
| 4 | 4 | Small chip |
| 8 | 3 | Medium |
| 6 | 2 | Between sm/md |
| 20 | 2 | Large pill |
| 28 | 1 | Near-full pill |
| 10 | 1 | One-off |
| 3 | 1 | One-off |

### Gap values (actual usage)

| Value | Sites | Notes |
|-------|-------|-------|
| 12 | 7 | Dominant |
| 8 | 6 | Secondary |
| 10 | 2 | Between 8/12 |
| 16 | 1 | Large |
| 6 | 1 | Small |
| 4 | 1 | Tight |
| 2 | 1 | Minimal |

### Width/height values (actual usage)

Mostly component-specific: 80, 200, 240, 180, 160, 88, 82, 338, 52, 132, 6.
**Not candidates for semantic tokens** — these are design-specific dimensions.

---

## Current State

| Token Family | Definition | Wire Schema | Server Infra | Server Adoption | Client Resolvers | Status |
|---|---|---|---|---|---|---|
| **Color** | ✅ design-sourced | ✅ `ColorToken` | ✅ `ColorTokens.java` (19 constants) | ✅ Heavy | ✅ All 3 | 🟢 **Complete** |
| **Icon** | ✅ code-derived | ✅ `sdui:` prefix | ✅ Inline strings | ✅ Active (~11 sites) | ✅ All 3 | 🟢 **Complete** |
| **Style/Variant** | ✅ code-derived | ✅ `variant` enum | ✅ Inline strings | ✅ Active | ✅ All 3 | 🟢 **Complete** |
| **Spacing** | ⚠️ fabricated | ✅ `LayoutScalar` | ⚠️ Registry only | ❌ Zero | ✅ All 3 ready | 🟡 **Needs rebuild from usage** |
| **Corner Radius** | ⚠️ fabricated | ✅ `LayoutScalar` | ⚠️ Registry only | ❌ Zero | ✅ All 3 ready | 🟡 **Needs rebuild from usage** |
| **Size** | ⚠️ fabricated | ✅ `LayoutScalar` | ⚠️ Registry only | ❌ Zero | ✅ All 3 ready | 🟡 **Needs rebuild from usage** |
| **Shadow** | ⚠️ fabricated | ⚠️ Wire mismatch | ⚠️ Registry only | ❌ Zero | ⚠️ Partial | 🔴 **Design gap** |
| **Typography** | ⚠️ fabricated | ⚠️ Parallel system | ⚠️ Registry only | ❌ Zero | ⚠️ Unused | 🔴 **Redundant — TextVariant owns this** |

### What's real and working

**Color** (design-sourced), **Icon** (code-derived), and **Style/Variant**
(code-derived) are fully adopted end-to-end. The model works.

### What's fabricated and needs rebuilding

**Spacing, Corner Radius, and Size** have complete client infrastructure but
the token definitions are speculative. The JSON files, the design system doc,
and the plan all had different values — none sourced from real design work or
derived from actual code. The architecture is sound; the data is wrong.

### What has design issues

**Shadow** — tier-integer tokens don't match the structured wire shape.  
**Typography** — dead infra; `TextVariant` is the real system.

---

## Approach: Rebuild from Usage + Design Input

### Principle

> **Derive the palette from what the code actually does**, then get design input
> on how those values should scale across form factors. Don't invent a palette
> and force the code to conform to it.

### Steps

1. Define semantic token names that cover 90%+ of actual usage
2. Get design input on form-factor scaling (phone → tablet → TV → web.wide)
3. Rebuild the JSON files with validated values
4. Update `docs/sdui-design-system.md` to match
5. Then migrate composers to emit token strings

---

## Phases

### Phase 0: Governance rule ✅ (done)

AGENTS.md §3.6: "Composers emit design-system tokens, not raw pixels."

### Phase 1: Derive spacing palette from usage (design input needed)

Proposed semantic tokens based on actual usage clusters:

| Token name | Phone value | Design question |
|---|---|---|
| `spacing.2xs` | 2 | 11 usage sites — keep or merge into xs? |
| `spacing.xs` | 4 | 23 sites |
| `spacing.sm` | 8 | 23 sites |
| `spacing.md` | 12 | 31 sites |
| `spacing.lg` | 16 | 77 sites (dominant) |
| `spacing.xl` | 24 | 1 site — is this needed? |
| `spacing.xxl` | 32 | 2 sites |

**Values that don't fit cleanly:** 6 (10 sites), 10 (7 sites), 14 (13 sites).

Design questions to resolve:
- Do we need tokens for 6, 10, 14 or are they component-specific raw values?
- What's the tablet/TV/web.wide multiplier for each? (1.0x? 1.25x? 1.5x?)
- Should we add `spacing.3xs` = 1 or `spacing.xxxl` = 48+?

### Phase 2: Derive radius palette from usage (design input needed)

| Token name | Phone value | Design question |
|---|---|---|
| `radius.sm` | 4 | 4 sites |
| `radius.md` | 8 | 3 sites |
| `radius.lg` | 12 | 5 sites (dominant card radius) |
| `radius.xl` | 20 | 2 sites (pill shapes) |
| `radius.full` | 999 | Circles / full-pill |

**Values that don't fit:** 3, 6, 10, 28.

Design questions:
- Is radius form-factor-responsive at all? (Material Design keeps it flat)
- Should 28 become `radius.xl` or stay raw?

### Phase 3: Size token disposition (design input needed)

The width/height values in the builder (80, 200, 240, 180, etc.) are mostly
**component-specific fixed dimensions** — card carousel widths, image ratios,
specific layout rectangles. These are poor candidates for semantic tokens.

Real candidates for size tokens:
- Icon sizes: server doesn't set icon width/height directly (it's in the icon element spec)
- Avatar sizes: if they appear, derive from usage
- Logo sizes: same

**Recommendation:** Don't force size tokens on the builder's width/height
values. If icon/avatar/logo sizing becomes a pattern, derive tokens then.
Consider deprecating `size-tokens.json` until there's real demand.

### Phase 4: Rebuild JSON files + sync docs

Once design questions are answered:

| Step | Description |
|------|-------------|
| 1 | Rewrite `schema/spacing-tokens.json` with validated values |
| 2 | Rewrite `schema/corner-radius-tokens.json` with validated values |
| 3 | Either rewrite or deprecate `schema/size-tokens.json` |
| 4 | Update `docs/sdui-design-system.md` §2.2, §2.3, §2.5 to match new JSON |
| 5 | Run `LayoutTokenRegistry` startup validation to confirm JSON loads |

### Phase 5: Create LayoutTokens.java + helper signatures

| Step | Description |
|------|-------------|
| 1 | Create `LayoutTokens.java` constants class with validated `SPACING_*`, `RADIUS_*` |
| 2 | Change `padding()` helper to accept `Object` (String token or int) |
| 3 | Change `cornerRadii()` helper to accept `Object` |
| 4 | Add `gap()` helper that accepts token strings |

### Phase 6: Migrate AtomicCompositeBuilder

Replace raw integers with token constants. Values that map to tokens get
migrated; values that don't (component-specific one-offs) stay raw per §3.6
exceptions.

### Phase 7: Migrate other composers

After builder is stable:
- `ForYouComposer`
- `ScheduleComposer`
- `LiveComposer`
- `NbaTvScheduleComposer`
- `GameDetailComposer`
- `DemoScreenComposer`

### Phase 8: Icon token hardening

Icons are already adopted via inline `"sdui:..."` strings. Harden with:

| Step | Description |
|------|-------------|
| 1 | Create `IconTokens.java` constants class |
| 2 | Replace inline strings with constants |
| 3 | Add missing `sdui:lock` to `schema/icon-tokens.json` |
| 4 | Add startup validation |

### Phase 9: Fix color token bugs

| Bug | Fix |
|-----|-----|
| `color.overlay.scrim` → `grey.100` → white in dark mode | Remap to dark-stable value |
| `color.brand.nba` → `blue.50` (may need gold) | Verify with brand team |

---

## Design Decisions (require sign-off, not implementation)

### Shadow tokens

Current state: tier-integer tokens (1/2/3) don't match the wire `Shadow` struct.
Shadows ARE used in code — 4+ sites emit raw `{offsetX, offsetY, radius, color}`.

Options:

| Option | Description |
|--------|-------------|
| A | Expand `shadow-tokens.json` with structured specs per tier |
| B | Deprecate shadow tokens; `style-tokens.json` variants own shadows |
| C | Add `token:shadow.md` shorthand that clients expand to platform shadow |

**Recommendation:** B — variants already own this. Deprecate the standalone file.

### Typography tokens

Current state: `typography-tokens.json` provides font sizes only. The wire uses
`TextVariant` enums that bundle full type specs. The token file is unused.

**Recommendation:** Deprecate `typography-tokens.json`. Document `TextVariant`
as the typography system. Remove the dead `LayoutTokenRegistry` loading of
this file.

---

## Exceptions (values that stay as raw literals)

| Category | Rationale |
|----------|-----------|
| `0` | No semantic value for zero |
| Component-specific dimensions (card widths, image sizes) | Design-intentional, not responsive |
| Calculated values (`width/2` for circles) | Runtime-computed |
| Hex alpha compositing values (`#000000CC`) | Opacity effects |
| One-off values with no pattern (3, 18, 20 used once) | Not worth tokenizing |
| Values explicitly excluded from palette after design review | Documented |

---

## Doc Sync Rule

**Whenever a `schema/*-tokens.json` file changes, `docs/sdui-design-system.md`
must be updated in the same PR.** The design system doc is the human-readable
view of the token registries. Drift between them is how we got fabricated
tables in the first place.

Sections that must stay in sync:
- §2.2 Spacing tokens ↔ `schema/spacing-tokens.json`
- §2.3 Size tokens ↔ `schema/size-tokens.json`
- §2.4 Typography tokens ↔ `schema/typography-tokens.json`
- §2.5 Corner radius tokens ↔ `schema/corner-radius-tokens.json`
- §2.6 Shadow tokens ↔ `schema/shadow-tokens.json`
- §2.7 Icon tokens ↔ `schema/icon-tokens.json`

---

## Relevant Files

| File | Role |
|------|------|
| **Server — existing infra** | |
| `server/src/.../service/ColorTokens.java` | Gold-standard pattern (19 constants) |
| `server/src/.../service/AtomicCompositeBuilder.java` | Primary migration target |
| `server/src/.../tokens/LayoutTokenRegistry.java` | Runtime resolver |
| `server/src/.../tokens/TokenRegistryConsistencyCheck.java` | Startup validation pattern |
| **Schema — token definitions (to be rebuilt)** | |
| `schema/spacing-tokens.json` | Spacing — needs rebuild from usage |
| `schema/corner-radius-tokens.json` | Radius — needs rebuild from usage |
| `schema/size-tokens.json` | Size — candidate for deprecation |
| `schema/shadow-tokens.json` | Shadow — candidate for deprecation |
| `schema/typography-tokens.json` | Typography — candidate for deprecation |
| `schema/color-tokens.json` | Color — real, design-sourced ✅ |
| `schema/icon-tokens.json` | Icon — real, code-derived ✅ |
| **Documentation** | |
| `docs/sdui-design-system.md` | Must sync with token JSON changes |
| **Client — resolvers (all working, no changes needed)** | |
| `web/src/utils/LayoutTokenResolver.ts` | Web layout resolver |
| `ios/Sources/.../LayoutTokenResolver.swift` | iOS layout resolver |
| `android/sdui-core/src/.../LayoutTokenResolver.kt` | Android layout resolver |

---

## Verification

1. `./gradlew compileJava` — server compiles with new helpers
2. `npx tsc --noEmit` in `web/` — no client regressions
3. Visual regression: dev server renders identically
4. Network payload: `"token:spacing.lg"` appears in padding fields
5. Form-factor adaptation: resize browser, verify scaled values
6. Doc consistency: `docs/sdui-design-system.md` tables match JSON files
7. Startup validation: `TokenRegistryConsistencyCheck` passes

---

## Resolved Decisions

1. ~~Should older builders adopt tokens in same pass?~~ **Yes**, same PR.
2. ~~Values 6, 10, 14 — add palette entries or leave raw?~~ **Leave raw** unless
   design review says otherwise.
3. ~~Other composers adopt tokens?~~ **Yes**, separate follow-up pass.
4. ~~Layout-only or all tokens?~~ **All tokens** — one system.
5. ~~Fabricated palettes: map code to them or rebuild?~~ **Rebuild from usage.**
   Only color came from design; everything else was speculation.
6. ~~Doc sync rule?~~ **Mandatory** — token JSON and design system doc update in
   same PR.
