Add real-app atomic composite foundations

Adds schema-first overlay and paged-carousel affordances so real-app-style feed
modules can stay server-composed instead of becoming new permanent sections.

--- Contract + client renderers ---

- Add `OverlayContainer` atomic support with schema/codegen coverage.
- Add optional `ScrollContainer.pageIndicator` for server-declared dots.
- Implement overlay rendering and page dots on web, iOS, and Android.
- Render page dots only when `paging` is true and more than one child exists.
- Size iOS paged children to page bounds so dots and paging align with web and
  Android.
- Remove web `AtomicImage`'s branded hardcoded fallback URL; use only
  server-provided placeholders.

--- Server composition ---

- Add reusable `AtomicCompositeBuilder` helpers for story rails, editorial
  overlay rails, featured live game hero carousels, utility grids, league rails,
  real-app section headers, and compact game schedule rows/lists.
- Keep those patterns as atomic-composite helper output, not new section types.
- Add live-capable overloads for hero and schedule helpers so callers can attach
  `refreshPolicy` and `dataBinding` when live data is available.
- Update demo/schedule composition and schedule fixtures to exercise the new
  real-app-style atomic patterns.

--- Fixtures + docs ---

- Add `schema/examples/real-app-feed-composite.json` and synced iOS fixture
  coverage for `OverlayContainer` plus paged `ScrollContainer.pageIndicator`.
- Update the real-app feed parity plan with the executed architecture and
  anti-proliferation guidance.
- Add `make server-test`.

--- Verification ---

- `make codegen`
- `make server-test`
- `node scripts/validate-color-tokens.js`
- `node scripts/validate-style-tokens.js`
- `npm --prefix web run build`
- `cd android && ./gradlew :app:compileDebugKotlin`
- `make ios-build`
- `git diff --check`

