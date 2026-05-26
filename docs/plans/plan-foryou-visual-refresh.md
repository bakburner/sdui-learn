---
name: ForYou visual refresh
overview: Rework the ForYou server-side composer to produce a feed that visually matches the real NBA app's home screen — full-bleed hero, Knockout headlines, ghost CTA, article list, better content density — all through server composition changes only (no client release).
todos: []
isProject: false
---

# ForYou Screen Visual Refresh (Server-Only)

All changes are server-side composition changes. The client should render the refreshed surface using existing atomic primitives, shared action handling, shared token resolvers, and shared `SectionContainer` chrome. Do not add client renderers, screen-specific routes, or client-side fallback content.

## Goal

Rework the For You feed so it feels closer to the NBA app home screen:

- A stronger top-of-feed visual hierarchy
- A full-bleed / image-led hero treatment
- Knockout-style headline semantics via existing `TextVariant` / typography tokens
- Ghost/text CTA treatments driven by server payloads
- Denser article/video modules below the hero
- Fewer nested card wrappers and less generic demo chrome

The result should still be a generic SDUI feed assembled from atomic composites and shared section surfaces.

## Current State

`ForYouComposer` currently emits:

1. `StoryCircleRail` for following avatars
2. `SectionHeaderComposite` for Tonight's Games
3. `FeaturedLiveGameHero`
4. `SectionHeaderComposite` + `EditorialOverlayRail` for Top Stories
5. Ad slot
6. Other Leagues rail
7. Trending rail
8. Ad slot
9. League Pass Picks rail
10. Ad slot
11. Around the League utility grid
12. VOD playlist

The composer already:

- Uses `AtomicCompositeBuilder`
- Derives stable section IDs from `contentSourceId`
- Adds `contentSourceId` to every section
- Uses server-owned `surface` values via `SduiUtils`
- Preserves live score binding for the hero through `dataBinding`
- Uses `nba://` navigation URIs rather than hardcoded client routes

## Constraints

- **Server authority:** visual/content choices are emitted by `ForYouComposer`.
- **No client release:** use existing atomic primitives, variants, actions, image handling, and surfaces.
- **No invented client assets:** image URLs must stay server-provided (`DemoImageUrls` is acceptable for demo fallback payloads because the server emits them).
- **No per-renderer chrome:** outer section spacing/background stays in `section.surface` via `SduiUtils`.
- **No new wire vocab unless required:** prefer existing variants (`headlineSmall`, `titleMedium`, `bodyMedium`, button `text` / `primary`) before schema changes.
- **Use existing shared actions:** `nba://...` URIs and action arrays remain builder-owned, not renderer-specific.

## Implementation Plan

### Phase 1 — Reorder and Tighten Feed Structure

Update `composeForYou()` to produce a more app-like feed order:

1. Following story rail
2. A cinematic / featured top hero
3. Top Stories article list / rail
4. Ad slot
5. Tonight's Games module
6. Trending Now article/video rail
7. League Pass Picks rail
8. Other Leagues / Utility destinations
9. VOD playlist

Keep ads in server-declared positions and preserve all `contentSourceId` values or update them deliberately if a module meaning changes.

Acceptance:

- Section IDs remain derived through `SectionIdDeriver`.
- Every section has `contentSourceId`.
- The screen remains valid SDUI and keeps `contentInsets`.

### Phase 2 — Promote a Full-Bleed Hero

Use `AtomicCompositeBuilder.buildCinematicHeroCarousel(...)` for an **editorial-first** top hero. The live game module remains present, but moves lower in the feed as the Tonight's Games module after the first ad.

Use server-provided data:

- Badge: `LIVE`, `UP NEXT`, `BREAKING`, or `FEATURED`
- Title: high-impact headline
- Subtitle: short supporting line
- CTA label: e.g. `Watch`, `Read More`, `Game Details`
- Target URI: `nba://...`

Do not invent client-side fallback routes or assets.

Acceptance:

- Hero has no outer card chrome unless emitted as `section.surface`.
- Hero overlay text has a readable scrim/background emitted by the server.
- Hero tap/CTA uses shared action payloads.
- Live game score binding is preserved if the live game hero remains live-data-driven.

### Phase 3 — Refresh Section Headers and CTAs

Use `buildSectionHeaderComposite(...)` for headers, but tune payload choices:

- Short uppercase labels where appropriate (`TOP STORIES`, `TRENDING`)
- CTA labels closer to product language (`See Schedule`, `More`, `Browse League Pass`)
- Text/ghost button treatment using existing button variants; do not introduce a new `ghost` variant unless schema/client support already exists.

Acceptance:

- CTA action semantics remain server-declared.
- CTA styling uses existing schema-supported variants/tokens.
- Accessibility heading metadata remains intact through `AccessibilityHelper`.

### Phase 4 — Improve Article and Video Density

Refresh the data arrays in:

- `buildTopStoriesEditorialRail()`
- `buildTrendingRail()`
- `buildLeaguePassPicksRail()`
- `buildVodPlaylistSection()`

Target outcome:

- More realistic headlines
- Better split between articles and videos
- Shorter subheads
- Less repeated placeholder wording
- More NBA-app-like content rhythm

Keep this as payload composition, not new renderer behavior.

Acceptance:

- Article/video cards still use existing atomic card builders.
- Durations and badges remain server data.
- `nba://article/...` and `nba://video/...` targets remain server-provided.

### Phase 5 — Surface and Spacing Audit

Review the section surfaces after the reorder:

- Hero and rails should generally use `utils.railSurface()` unless they need a different existing surface.
- Headers should use `utils.sectionHeaderSurface()`.
- Ads should keep `utils.adSlotSurface()`.
- Do not add outer padding/background inside semantic renderers.

Acceptance:

- No section is wrapped in obsolete `gamePanelSurface()`.
- Outer spacing/chrome remains centralized in `section.surface`.
- Raw layout values are not introduced in `ForYouComposer`; use existing builder helpers / token constants.

### Phase 6 — Tests

Extend `ForYouSectionIdDerivationTest` or add a focused `ForYouVisualCompositionTest`.

Test coverage:

- The first non-story content module is the refreshed hero.
- Every section has `contentSourceId` and derived ID format.
- The hero section is `AtomicComposite`.
- The hero contains server-emitted image/scrim/overlay text sufficient for readable copy.
- CTA actions are present for modules with action labels.
- No section ID depends on positional index.
- Existing `contentInsets` assertions remain.

Tonight's Games remains live-data-capable even though it is no longer the top hero:

- Assert `refreshPolicy.type == "sse"` when a live game is present.
- Assert `dataBinding.bindings` target the hero card score/status paths.

### Phase 7 — Verification

Run preferred repo targets:

- `make test-server` if available
- Otherwise `./gradlew test` from repo root only if no Make target exists

Manual smoke:

- Start the dev stack with the Make target.
- Load the For You screen.
- Verify hierarchy: story rail → strong hero → dense content modules.
- Verify tap targets navigate through `nba://` actions.
- Verify no blank image states unless server fallback URLs are emitted.

## Non-Goals

- Client renderer work
- New semantic section types
- New screen enums/routes
- Schema changes for a new button variant
- Real CMS integration
- Replacing demo image sources with production CDN contracts
- Changing tab/root navigation behavior

## Files

- `server/src/main/java/com/nba/sdui/service/ForYouComposer.java`
- `server/src/main/java/com/nba/sdui/service/AtomicCompositeBuilder.java` (only if an existing helper needs a narrow server-side enhancement)
- `server/src/test/java/com/nba/sdui/service/ForYouSectionIdDerivationTest.java`
- Optional: `server/src/test/java/com/nba/sdui/service/ForYouVisualCompositionTest.java`

## Resolved Decisions

- Top hero is **editorial-first**, using `buildCinematicHeroCarousel(...)`.
- Tonight's Games moves **after the first ad**.
- Current `DemoImageUrls` placeholders are acceptable for this visual pass.
