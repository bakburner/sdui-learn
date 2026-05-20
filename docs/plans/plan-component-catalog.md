# Plan: SDUI Component Catalog and Reference Implementations

**Status:** Proposed
**Created:** 2026-05-08
**Scope:** Component catalog with reference implementations, Figma integration, and cross-platform validation
**Depends on:** plan-design-token-coverage-from-sitebuilder.md (token registries), plan-figma-to-sdui-pipeline.md (extraction tooling)
**Reference:** Sitebuilder `assets/components/index.yml` + `assets/components/references/` (32 HTML reference files), `assets/component-registry.yml`

---

## Objectives

1. Create a component catalog (`schema/components/index.yml`) that enumerates every SDUI component pattern with its wire shape, token usage, variants, and form-factor behavior
2. Create reference implementations as canonical SDUI JSON files — one per component — that serve as golden examples for composers, tests, and Figma extraction validation
3. Map catalog entries to existing sitebuilder HTML references where overlap exists, documenting the translation
4. Provide a validation harness that checks reference JSON against `sdui-schema.json` and token registries
5. Support catalog-driven prototyping: given a catalog entry name, emit a complete SDUI section skeleton with correct tokens and structure

---

## Acceptance Criteria

- [ ] `schema/components/index.yml` exists with entries for all atomic composite patterns currently built by `AtomicCompositeBuilder`
- [ ] Each catalog entry specifies: name, category, description, token usage, variants, form-factor notes, reference JSON path
- [ ] `schema/components/references/` contains at least 15 reference JSON files covering core patterns
- [ ] Each reference JSON validates against `sdui-schema.json`
- [ ] Each reference JSON uses only tokens that exist in the token registries
- [ ] `make validate-components` runs schema + token validation on all reference files
- [ ] Sitebuilder component overlap documented: for each of the 32 sitebuilder HTML references, the catalog entry notes whether an SDUI equivalent exists, is planned, or is out of scope
- [ ] Figma extraction pipeline (`plan-figma-to-sdui-pipeline.md`) can use catalog entries to validate extracted output structure

---

## Non-Goals

- Rendering the catalog as a visual storybook (future work per platform)
- Defining new SDUI primitives — catalog documents what exists
- Replacing sitebuilder's HTML references — those serve the web site generator; this catalog serves SDUI

---

## Phase 1: Catalog Index

### 1.1 Create `schema/components/index.yml`

Structure per entry:

```yaml
components:
  - name: game-card
    category: content
    description: >
      Game matchup card with team logos, score/time, broadcast info,
      and tap action to game detail. Supports pregame/live/final states.
    composer: AtomicCompositeBuilder.gameCard()
    variants:
      - default (vertical stack: matchup + score + broadcast)
      - compact (horizontal: logos + score only)
      - expanded (vertical: matchup + leaders + recap links)
    tokens:
      colors: [nba.bg.primary, nba.bg.secondary, nba.label.primary, nba.label.secondary, nba.label.accent.brand]
      spacing: [nba.spacing.sm, nba.spacing.md, nba.spacing.lg]
      radius: [nba.radius.lg]
      typography: [titleMedium, bodySmall, labelSmall, score]
    form_factor:
      phone: single column, full-width card
      tablet: 2-column grid
      tv: horizontal rail with focus scaling
      web: 3-4 column grid
    sitebuilder_ref: references/game-card.html
    reference: references/game-card.json
```

### 1.2 Catalog coverage

Extract component patterns from `AtomicCompositeBuilder.java` method inventory. Expected entries:

| Category | Components |
|---|---|
| **Content** | game-card, hero-banner, promo-card, content-card, overlay-card, player-card, feature-promo |
| **Data** | stats-row, standings-row, schedule-row, boxscore-header |
| **Navigation** | section-heading, tab-bar, action-bar |
| **Media** | video-thumbnail, highlight-card, program-card |
| **Layout** | carousel-container, grid-container, list-container, divider, spacer |
| **Interactive** | button, toggle, badge |

### 1.3 Sitebuilder overlap mapping

| Sitebuilder component | SDUI equivalent | Status |
|---|---|---|
| hero-banner (3 variants) | hero-banner atomic composite | Exists — document wire shape |
| game-card (3 states) | game-card atomic composite | Exists — document wire shape |
| scoreboard | BoxscoreTable semantic section | Exists — semantic, not atomic |
| standings-table | SeasonLeadersTable semantic section | Exists — semantic |
| stats-table | stats-row atomic composite | Partial — row-level only |
| accordion | Not in SDUI | Out of scope (web-only pattern) |
| tabs | TabGroup semantic section | Exists — semantic |
| carousel | carousel-container layout | Exists — Container with horizontal scroll |
| video-player | VideoPlayer semantic section | Exists — semantic |
| modal | Not in SDUI | Out of scope (native platform dialogs) |
| card | content-card atomic composite | Exists |
| search-bar | Form semantic section | Exists — semantic |
| article-content | Not in SDUI | Out of scope (web long-form) |
| page-layout | Screen-level composition | Handled by screen composers |
| header-nav / footer | Not in SDUI | Platform-native chrome |
| button | Button primitive | Exists |
| dropdown | Not in SDUI | Platform-native control |
| toggle | toggle component | Exists |
| player-card | player-card atomic composite | Exists |
| player-stats-card | stats-row variant | Partial |
| section-heading | section-heading atomic composite | Exists |
| impact-stats | Not in SDUI | Evaluate if needed |
| news-grid | grid-container + content-card | Composition pattern |
| schedule | schedule-row atomic composite | Exists |
| overlay-card | overlay-card atomic composite | Exists |
| feature-promo | feature-promo atomic composite | Exists |
| program-card | program-card atomic composite | Exists |

**15 direct overlaps, 6 semantic sections, 7 out of scope, 4 composition patterns.**

---

## Phase 2: Reference JSON Files

### 2.1 Create `schema/components/references/`

One JSON file per catalog entry. Each file is a complete SDUI section payload.

Example `references/game-card.json`:

```json
{
  "$comment": "Reference: game-card (pregame state)",
  "type": "Container",
  "direction": "vertical",
  "padding": "token:nba.spacing.md",
  "cornerRadius": "token:nba.radius.lg",
  "backgrounds": [{ "color": "token:nba.bg.secondary" }],
  "actions": {
    "onActivate": { "type": "navigate", "uri": "nba://game/{gameId}" }
  },
  "children": [
    {
      "type": "Container",
      "direction": "horizontal",
      "gap": "token:nba.spacing.md",
      "children": [
        { "type": "Image", "src": "{{team.away.logo}}", "size": { "width": 40, "height": 40 } },
        { "type": "Text", "text": "{{team.away.tricode}}", "variant": "labelMedium", "color": "token:nba.label.primary" },
        { "type": "Text", "text": "{{gameStatus}}", "variant": "bodySmall", "color": "token:nba.label.secondary" },
        { "type": "Text", "text": "{{team.home.tricode}}", "variant": "labelMedium", "color": "token:nba.label.primary" },
        { "type": "Image", "src": "{{team.home.logo}}", "size": { "width": 40, "height": 40 } }
      ]
    },
    {
      "type": "Text",
      "text": "{{broadcast}}",
      "variant": "caption",
      "color": "token:nba.label.secondary"
    }
  ]
}
```

`{{placeholder}}` syntax indicates data-bound fields that composers populate at runtime. Not part of the wire schema — stripped during validation.

### 2.2 Priority reference files (Phase 2 deliverable: 15 files)

1. `game-card.json` — pregame, live, final variants
2. `hero-banner.json` — full-bleed with gradient overlay
3. `content-card.json` — image + title + description + CTA
4. `section-heading.json` — title + "See All" action
5. `stats-row.json` — player name + stat columns
6. `schedule-row.json` — date + matchup + time/result
7. `player-card.json` — headshot + name + position + team
8. `overlay-card.json` — background image + text overlay
9. `feature-promo.json` — split layout with CTA
10. `program-card.json` — thumbnail + title + duration
11. `highlight-card.json` — video thumbnail + play icon + title
12. `carousel-container.json` — horizontal scroll container with child cards
13. `grid-container.json` — responsive grid layout
14. `button.json` — primary/secondary/text variants
15. `badge.json` — live indicator, count badge

---

## Phase 3: Validation Harness

### 3.1 Schema validation

```bash
make validate-components
```

For each `references/*.json`:
1. Strip `{{placeholder}}` values (replace with dummy strings)
2. Validate against `sdui-schema.json`
3. Report violations

### 3.2 Token validation

For each `references/*.json`:
1. Collect all `token:*` references
2. Check each exists in the corresponding registry (`color-tokens.json`, `spacing-tokens.json`, `corner-radius-tokens.json`, `typography-tokens.json`, `shadow-tokens.json`, `motion-tokens.json`)
3. Report unknown tokens

### 3.3 CI integration

Add `validate-components` to `Makefile` and CI pipeline. Fail on schema violations. Warn on unknown tokens (allows forward-referencing planned tokens).

---

## Phase 4: Catalog-Driven Prototyping

### 4.1 Skeleton generator

```bash
make component-skeleton NAME=game-card VARIANT=default
```

Reads the catalog entry for `game-card`, loads `references/game-card.json`, outputs a starter `AtomicCompositeBuilder` Java call chain:

```java
section("game-card")
  .container(vertical(), padding(SPACING_MD), cornerRadius(RADIUS_LG), bg(BG_SECONDARY))
    .container(horizontal(), gap(SPACING_MD))
      .image("{{team.away.logo}}", 40, 40)
      .text("{{team.away.tricode}}", LABEL_MEDIUM, LABEL_PRIMARY)
      .text("{{gameStatus}}", BODY_SMALL, LABEL_SECONDARY)
      .text("{{team.home.tricode}}", LABEL_MEDIUM, LABEL_PRIMARY)
      .image("{{team.home.logo}}", 40, 40)
    .end()
    .text("{{broadcast}}", CAPTION, LABEL_SECONDARY)
  .end()
```

### 4.2 Figma pipeline integration

The Figma extraction pipeline (plan-figma-to-sdui-pipeline.md) uses catalog entries to:
1. Validate extracted output matches expected structure for known components
2. Fall back to catalog reference JSON when extraction quality is low
3. Report structural diff between extracted JSON and reference JSON

---

## Phase 5: Cross-Platform Snapshot Testing

### 5.1 Golden snapshots per platform

Each reference JSON rendered by each client (iOS, Android, web) produces a screenshot. Store as golden snapshots:

```
schema/components/snapshots/
  game-card/
    ios-phone.png
    ios-tablet.png
    android-phone.png
    android-tablet.png
    web-narrow.png
    web-wide.png
    tv.png
```

### 5.2 Snapshot CI

On PR that modifies a reference JSON or a client renderer:
1. Re-render affected components on all platforms
2. Diff against golden snapshots
3. Flag visual regressions

This is future work — requires client-side rendering harness per platform. Document the target architecture now; implement when client CI supports it.
