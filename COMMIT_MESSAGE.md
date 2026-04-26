Add feed atomic composite foundations

Adds schema-first overlay and paged-carousel affordances so feed-style
modules can stay server-composed instead of becoming new permanent sections,
plus the live-clock binding transform and strict-decode guards that enforce
server authority over those new contract surfaces.

--- Contract + client renderers ---

- Add `OverlayContainer` atomic support with schema/codegen coverage.
- Add optional `ScrollContainer.pageIndicator` for server-declared dots.
- Add `DataBindingPath.transform` with `liveClockSnapshot` so server-declared
  bindings normalize live clock payloads into `{ snapshotSeconds, snapshotAt,
  isRunning }`.
- Implement overlay rendering and page dots on web, iOS, and Android.
- Implement live-clock binding normalization on web, iOS, and Android.
- Render page dots only when `paging` is true and more than one child exists.
- Size iOS paged children to page bounds so dots and paging align with web and
  Android.
- Remove web `AtomicImage`'s branded hardcoded fallback URL; use only
  server-provided placeholders.

--- Strict-decode hardening ---

- Add `AtomicCompositeBuilder.validateTransform` and call it from
  `SduiUtils.bindingPath(...)` so unknown `transform` values fail at compose
  time instead of at client decode.
- Make web `DataBindingApplier` skip the write when an unknown transform is
  encountered, surfacing a warning rather than silently downgrading server
  semantics to the raw value.
- Document `AtomicCompositeBuilder.DEMO_INITIAL_CLOCK_RUNNING` as a demo-only
  override of the contractual `isRunning=false` initial state, with a single
  rollback point for when real Ably linescore frames are wired up.

--- Server composition ---

- Add reusable `AtomicCompositeBuilder` helpers for story rails, editorial
  overlay rails, featured live game hero carousels, utility grids, league rails,
  section header composites, and compact game schedule rows/lists.
- Keep those patterns as atomic-composite helper output, not new section types.
- Add live-capable overloads for hero and schedule helpers so callers can attach
  `refreshPolicy` and `dataBinding` when live data is available.
- Seed initial server LiveClock snapshots as paused; local ticking starts only
  when a bound live channel message sets `isRunning: true`.
- Replace hardcoded hex/RGBA literals in the `DemoScreenComposer` subscribe
  hero, tier UI, and subscribe banner with semantic `ColorTokens` so the demo
  composer matches the rest of the server composition's token discipline.
- Update demo/schedule composition and schedule fixtures to exercise the new
  feed-style atomic patterns.

--- For You parity rewrite ---

- Rewrite `ForYouComposer` to mirror the Kitchen demo's atomic-composite
  vocabulary. Each section uses the same helper the demo exercises so any
  composite that renders correctly in `/kitchen` renders correctly here too.
- Drop the legacy compact `GamePanel` "Upcoming Games" carousel. Replace it
  with a single paged `FeaturedLiveGameHero` carousel that combines the live
  game (when present) with the next few upcoming games — bigger key-art-led
  cards, server-paged with dots, and a per-card linescore binding on the live
  card so SSE updates keep the score and clock fresh.
- Stop wrapping card-chromed composites in `gamePanelSurface()`. The hero
  card now owns its own background, corner radius, and shadow, so the section
  envelope only needs `railSurface()` margins. This removes the card-around-
  card double chrome that was leaving the hero floating inside an empty
  surface band on iOS and web.
- Switch the "Around the League" utility grid from `cardSurface()` to
  `railSurface()` — the cells are individually card-chromed, so the section
  doesn't need an outer card.
- Restrict story-circle imagery to square sources (team logo PNGs and square
  loremflickr stock). The previous mix of 260×190 player headshots interacted
  poorly with the circular crop + overlay-pill stacking on mobile and pushed
  the LIVE/NEW badge out of position.
- Replace expired `cdn.nba.com/manage/...` editorial / sponsor URLs in the
  For You composer with `loremflickr.com` placeholders so the screen always
  renders with imagery during demos. NBA team logos still come from
  `SduiUtils.teamLogoUrl(...)` (PNG variants) so iOS Kingfisher decodes them
  natively.

--- Atomic primitive housekeeping ---

- `featuredLiveGameHeroCard` now takes a `fillWidth` flag. Single-card hero
  sections emit the card with `fillWidth: true` and skip the paged scroll
  wrapper; multi-card sections keep the fixed 338pt width plus paged scroll
  with dots. The single-card branch wraps the card in a flush
  padded-by-16 column so the card aligns with its surface horizontally.
- `utilityCard` now sets `height: 132` and `fillWidth: true`. The grid was
  letting cells size to their content on iOS, which produced uneven cell
  heights across rows. Fixing height + fillWidth keeps the grid rhythm
  visually stable on phones; web already balanced via flex.
- `buildNbaTvSlot` (Today's Schedule rows) now sets `flex: 1` on the
  title/subtitle column. `fillWidth: true` alone wasn't enough on iOS or
  Android: the AtomicContainer flex stack only treats a child as "claims
  remaining main-axis space" when it has a non-null `flex` weight. Without
  it, the row sized to natural content width and the outer fillWidth frame
  centered the entire stack, so the time + title rendered visually centered
  on phones and the LIVE badge floated next to the title instead of pinning
  to the trailing edge of the surface. With `flex: 1`, time hugs the
  leading edge, the LIVE badge hugs the trailing edge, and the title /
  subtitle stay left-justified in between on all three platforms.

--- Tests ---

- Add `web/src/runtime/DataBindingApplier.test.ts` covering the
  `liveClockSnapshot` transform and the unknown-transform skip behavior.
- Add `android/sdui-core/src/test/java/com/nba/sdui/core/data/DataBindingResolverTest.kt`
  mirroring the iOS/web `liveClockSnapshot` cases. Mocks `android.util.Log`
  with `mockkStatic` so the test runs under the existing JUnit 4 runner without
  changing project-wide test infrastructure.
- Extend `DataBindingApplierTests` (iOS) with `liveClockSnapshot` coverage.

--- Fixtures + docs ---

- Add `schema/examples/feed-screen-composite.json` and synced iOS fixture
  coverage for `OverlayContainer` plus paged `ScrollContainer.pageIndicator`.
- Update existing `game-panel-composite` fixtures to use `sourcePath` per the
  schema's `DataBindingPath` shape.
- Refresh the feed parity plan as a pre-implementation checkpoint:
  overlay containers and page dots are now available, so the next pass should
  focus on server-composed visual fidelity rather than new primitives.
- Capture screenshot-derived follow-up targets, including larger story images
  with red live rings, yellow active tab accents, asymmetric score layouts,
  separated broadcast strips, static date chips, headline cards, full-bleed
  media cards, and light-mode accent behavior.
- Add an Atomic Precedence Assessment table to the plan that classifies every
  visible feed module (story rail, hero card, editorial rail, utility grid,
  league rail, schedule row, date strip, promo banner, app shell) against the
  AGENTS.md exception classes.
- Add `make server-test`.

--- For You parity follow-ups ---

- Fix web `AtomicOverlayContainer.overlayStyle` to treat `inset` as an
  *offset from the aligned base bounds* (matching iOS/Android), instead of
  overriding the alignment-derived edge anchors. With the prior code, a
  story-circle LIVE pill emitted as `bottomCenter` + `inset = {0,0,0,0}`
  ended up anchored to all four edges and translated, which on web rendered
  the pill clipped in the upper right of the avatar (looking like a small
  red triangle) instead of at the bottom-center as on iOS.
- Pin the iOS `TabGroupView` strip's scroll offset to the active tab via
  `ScrollViewReader` + `.scrollTo(activeId, anchor: .center)` on appear and
  on every selection change. Previously, tapping a trailing tab on Watch
  ("League Pass") let SwiftUI's implicit bring-focused-button-into-view
  behavior settle the inner horizontal ScrollView at an offset that cropped
  the leading "Featured" tab off the screen, and the strip would snap back
  to that offset even after manual scrolling. Centering on the active tab
  lets the ScrollView clamp naturally at its bounds, so leading-tab
  selections stay clamped at the leading edge, trailing-tab selections
  clamp at the trailing edge, and all three Watch tabs remain visible at
  every selection state.
- Always emit the red "story" ring on `storyCircleItem` avatars, regardless
  of whether the item carries a notification badge. The ring is the
  rail-wide visual signature (matches the real NBA app's Following rail
  and the parity plan's "red circle around the images in the top rail"
  target). The badge ("LIVE", "NEW", …) remains an independent overlay
  layered on the bottom-center of the avatar. Previously the ring was
  gated on `badgeText != null`, so only Celtics / Lakers carried it on the
  For You Following rail and Warriors / Thunder / News / Social rendered
  as ringless avatars.
- Drop the loremflickr broadcaster placeholder from the For You hero
  cards. The `FeaturedLiveGameHero` composite still renders a sponsor row
  when its `sponsorLogoUrlsCsv` slot is populated, but the demo doesn't
  have real broadcaster art and the placeholder rendered as a broken
  thumbnail next to the score strip on web. Broadcaster mention is
  carried in the subtitle ("Live now on NBA TV" / "Tonight on NBA TV").
  Re-introduce sponsor logos once we can source actual broadcaster art
  server-side.

--- Verification ---

- `make codegen`
- `make server-test`
- `node scripts/validate-color-tokens.js`
- `node scripts/validate-style-tokens.js`
- `npm --prefix web run build`
- `npm --prefix web test`
- `cd android && ./gradlew :sdui-core:test --tests com.nba.sdui.core.data.DataBindingResolverTest`
- `cd android && ./gradlew :app:compileDebugKotlin`
- `make ios-build`
- `git diff --check`
