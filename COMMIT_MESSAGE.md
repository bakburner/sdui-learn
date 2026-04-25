Add real-app atomic composite foundations

Adds schema-first overlay and paged-carousel affordances so real-app-style feed
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
  real-app section headers, and compact game schedule rows/lists.
- Keep those patterns as atomic-composite helper output, not new section types.
- Add live-capable overloads for hero and schedule helpers so callers can attach
  `refreshPolicy` and `dataBinding` when live data is available.
- Seed initial server LiveClock snapshots as paused; local ticking starts only
  when a bound live channel message sets `isRunning: true`.
- Replace hardcoded hex/RGBA literals in the `DemoScreenComposer` subscribe
  hero, tier UI, and subscribe banner with semantic `ColorTokens` so the demo
  composer matches the rest of the server composition's token discipline.
- Update demo/schedule composition and schedule fixtures to exercise the new
  real-app-style atomic patterns.

--- Tests ---

- Add `web/src/runtime/DataBindingApplier.test.ts` covering the
  `liveClockSnapshot` transform and the unknown-transform skip behavior.
- Add `android/sdui-core/src/test/java/com/nba/sdui/core/data/DataBindingResolverTest.kt`
  mirroring the iOS/web `liveClockSnapshot` cases. Mocks `android.util.Log`
  with `mockkStatic` so the test runs under the existing JUnit 4 runner without
  changing project-wide test infrastructure.
- Extend `DataBindingApplierTests` (iOS) with `liveClockSnapshot` coverage.

--- Fixtures + docs ---

- Add `schema/examples/real-app-feed-composite.json` and synced iOS fixture
  coverage for `OverlayContainer` plus paged `ScrollContainer.pageIndicator`.
- Update existing `game-panel-composite` fixtures to use `sourcePath` per the
  schema's `DataBindingPath` shape.
- Refresh the real-app feed parity plan as a pre-implementation checkpoint:
  overlay containers and page dots are now available, so the next pass should
  focus on server-composed visual fidelity rather than new primitives.
- Capture screenshot-derived follow-up targets, including larger story images
  with red live rings, yellow active tab accents, asymmetric score layouts,
  separated broadcast strips, static date chips, headline cards, full-bleed
  media cards, and light-mode accent behavior.
- Add an Atomic Precedence Assessment table to the plan that classifies every
  visible real-app module (story rail, hero card, editorial rail, utility grid,
  league rail, schedule row, date strip, promo banner, app shell) against the
  AGENTS.md exception classes.
- Add `make server-test`.

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
