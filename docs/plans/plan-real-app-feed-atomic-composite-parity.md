# Plan: Real App Feed Atomic Composite Parity

> Source inputs: real NBA app screenshots provided 2026-04-25, `AGENTS.md`, `docs/client-implementors-contract.md`, `docs/sdui-design-system.md`, ADR-013, `schema/sdui-schema.json`, `schema/color-tokens.json`, `schema/style-tokens.json`, and current atomic renderers/composers.

## Summary

Add the missing atomic-composite capabilities needed to emulate the real NBA app feed look and feel: circular live story rails, dark editorial image cards with text overlays, featured live game hero cards, section headers with CTA affordances, snap/paged carousels with optional page indicators, and sponsor/logo rows. Keep global app chrome, real video playback, ads, and SDK mounts outside atomic content.

The guiding principle is server-authored structure and semantics, with clients only realizing platform-native presentation and runtime-local scroll/theme state.

## Phase 0: Documentation Discovery

| Source | Findings |
|--------|----------|
| `AGENTS.md` | Stateless feed UI should default to `AtomicComposite`. New permanent sections require client-owned state, SDK hosting, or runtime lifecycle. Section outer chrome belongs to `section.surface` / `SectionContainer`. Clients must not invent copy, URLs, routes, or business meaning. |
| `docs/client-implementors-contract.md` | `AtomicComposite` uses `data.ui` plus optional `data.content`. `bindRef` updates leaf values from live content. Shared `AtomicBox` owns box model uniformly. Renderer-local animation/clock behavior is allowed when driven by server data. |
| `docs/sdui-design-system.md` | Styling has three layers: inline atomic props, per-primitive variants, and color tokens. No parent-to-child style cascade. Team colors are not design-system tokens. |
| `docs/adr/013-style-tokens-for-atomic-primitives.md` | Variant additions must be stable, reusable, platform-owned presentation semantics. `AtomicComposite` is a host for an atomic tree, not a visual variant host. |
| `schema/sdui-schema.json` | Current atomic primitives cover row/column layout, image, text, button, scroll, divider, spacer, conditional, display grid, section slot, and live clock. Missing first-class overlay layout and page indicator semantics. |
| Current composers | `AtomicCompositeBuilder` already supports content rails, following rails, game panels, video cards, VOD playlists, section headers, promo banners, and NbaTvSchedule-style hero image with gradient content. These are close but not enough for the real-app image-overlay cards and carousel page dots. |

## Current State

| Capability | Status | Notes |
|------------|--------|-------|
| Circular story/live rail | Partial | `buildFollowingRail` has circular image + label; live badge treatment can be composed but needs a reusable builder for real-app story tiles. |
| Section header + More CTA | Built | `buildSectionHeader` supports title/subtitle and optional text button. Needs real-app dark style tuning and “More + circular arrow” composition. |
| Basic content rail | Built | `buildContentRail` creates 16:9 cards with headline below image. Real app needs tall image cards with text over image. |
| Featured live game card | Partial | `buildGamePanelComposite` handles logos/scores/clock/live badge. It does not handle full key art, layered image/header/body/sponsor areas, or paging dots. |
| Image overlay / scrim card | Partial | Background/image overlay types exist in the model, but mobile atomic background images are intentionally constrained out. Current row/column layout cannot express general Z-stack overlays cleanly. |
| Carousel snap/paging | Partial | `ScrollContainer` schema includes `paging` and `snapAlignment`; composers rarely emit it. |
| Carousel page dots | Missing | Requires scroll state on clients and server-declared intent. |
| Sponsor/logo strip | Partial | Can be composed from `Image` and `Text`; real SDK ads remain `AdSlot`. |
| Global nav / bottom bar | Built as app chrome | Should remain `screen.navigation` / native shell, not atomic content. |
| Video playback | Reserved SDK section | Actual player remains `VideoPlayer`, not atomic content. |

## Requirements Addressed

- [ ] Emulate the real app’s feed modules using server-composed atomic trees.
- [ ] Add missing layout/presentation capabilities only where current atomic primitives cannot express the structure.
- [ ] Keep server authority over content, assets, routing, refresh policy, and action semantics.
- [ ] Keep client-owned state limited to platform rendering, scrolling, theme, and SDK/runtime mechanics.
- [ ] Add cross-platform verification so web, iOS, and Android all support the same wire contract.

## Non-Goals

- Do not re-create the full real NBA app shell, bottom nav, account/cast icons, or status bar as atomic content.
- Do not implement real video playback inside atomics. Use `VideoPlayer` for SDK mounting.
- Do not implement real ads or sponsor auction logic inside atomics. Use `AdSlot` for ad SDK boundaries.
- Do not introduce client hardcoded image URLs, labels, fallback routes, team IDs, or product copy.
- Do not pursue cross-platform pixel parity. Each client should realize the same semantics in its platform style.

## Tasks

### Phase 1: Contract And Documentation Alignment

- [ ] Resolve doc/schema drift around current variant values before adding any new variant.
  - Check `schema/sdui-schema.json`, `schema/style-tokens.json`, and `docs/sdui-design-system.md`.
  - If any doc claims variants not in schema, update the doc or add the schema value via the proper codegen path.
- [ ] Document the “real app feed” target as atomic-composite patterns, not permanent sections.
  - Pattern names: `StoryCircleRail`, `EditorialOverlayRail`, `FeaturedLiveGameHero`, `ScoreStrip`, `SponsorLogoRow`.
  - These names should be server builder/helper names, not new section types.
- [ ] Decide which gaps are schema gaps versus composer-only gaps.
  - Composer-only: story rails, sponsor logo rows, section header styling, featured game card layout.
  - Schema/client gap: generalized overlay layout, page indicator tied to scroll state.

Verification:
- [ ] `rg "ContentRail|GamePanel|EditorialOverlay" docs/ AGENTS.md schema/` confirms docs describe these as atomic-composite patterns, not new permanent sections.
- [ ] `node scripts/validate-color-tokens.js` and `node scripts/validate-style-tokens.js` pass if touched.

Anti-pattern guards:
- Do not add a `RealAppFeed` permanent section.
- Do not add a catch-all `className`, `styleName`, or platform-specific raw CSS escape hatch.
- Do not add speculative variants just to reduce repeated inline prop bags.

### Phase 2: Add General Overlay Layout Support

Current row/column `Container` cannot cleanly express “image fills card, gradient scrim overlays bottom, title/badge sits over image.” The closest existing mechanism is `badge`, but `badge` is semantically too narrow for full editorial overlays.

- [ ] Add a schema-supported atomic overlay primitive or equivalent orthogonal layout field.
  - Preferred shape: new atomic type `OverlayContainer` with:
    - `base`: `AtomicElement`
    - `overlays`: array of `{ alignment, inset, element }`
  - Alternative shape: add `layout: "overlay"` to `Container` only if it remains clean across clients.
- [ ] Run `make codegen`.
- [ ] Implement web renderer using positioned children inside a relative container.
- [ ] Implement iOS renderer using `ZStack(alignment:)`.
- [ ] Implement Android renderer using `Box` with `align(...)`.
- [ ] Ensure `AtomicBox` remains the only owner of margin, padding, background, radius, shadow, width/height, and opacity.
- [ ] Add diagnostics for unsupported overlay alignment values after strict decode.

Verification:
- [ ] Fixture with image base + bottom scrim + text overlay renders on all three clients.
- [ ] No renderer constructs image URLs or fallback copy.
- [ ] `rg "background image is decoded but constrained" ios android` still reflects the policy that arbitrary atomic background images are not reintroduced as the primary overlay path.

Anti-pattern guards:
- Do not use `Container.background.imageUrl` as the primary solution for mobile image cards unless the mobile policy is deliberately reopened.
- Do not make overlay layout a permanent section renderer.

### Phase 3: Add Scroll Paging And Page Indicator Semantics

The real app hero carousel has dots and paging. `ScrollContainer` already has `paging` and `snapAlignment`; it lacks an optional server-declared page indicator.

- [ ] Extend `ScrollContainer` schema with an optional page indicator property, for example:
  - `pageIndicator: { style: "dots", alignment: "bottomCenter", color?, activeColor? }`
- [ ] Run `make codegen`.
- [ ] Web: wire `paging`/`snapAlignment` to CSS scroll snap and use scroll position to update dots.
- [ ] iOS: use native scroll target behavior where available and local scroll state for dots.
- [ ] Android: use pager/snap implementation already introduced for paging where possible, and local pager state for dots.
- [ ] Ensure indicators are presentation of a server-declared scroll affordance, not invented per client.

Verification:
- [ ] A fixture with `paging: true` and `pageIndicator.style: "dots"` shows correct active dot after scrolling.
- [ ] A fixture without `pageIndicator` has no dots.
- [ ] Android/iOS/Web builds pass.

Anti-pattern guards:
- Do not add dots automatically to every carousel.
- Do not infer page count from screen identity.
- Do not make dots content; they are scroll affordance presentation.

### Phase 4: Server Builder Patterns For Real-App Feed Modules

Add reusable `AtomicCompositeBuilder` helpers that compose real-app-like surfaces from server data.

- [ ] `buildStoryCircleRail(...)`
  - Horizontal `ScrollContainer`.
  - Circular `Image`, live/new badge overlay, label below.
  - Server provides image URL, label, badge text, action URI.
- [ ] `buildEditorialOverlayRail(...)`
  - Horizontal `ScrollContainer`.
  - Tall aspect-ratio image cards.
  - `OverlayContainer` base image + gradient scrim + title/subtitle/badges.
  - Optional “NEW” or “LIVE” badge from server payload.
- [ ] `buildFeaturedLiveGameHero(...)`
  - Paged `ScrollContainer` for hero cards when more than one.
  - Key art image area using overlay support.
  - Score strip with team logos, scores, clock/status, series text.
  - Sponsor/logo row composed as atomic `Image` elements.
  - Live values use `data.content` + `bindRef` + existing `dataBinding`.
- [ ] `buildRealAppSectionHeader(...)`
  - Title row with optional “More” CTA and arrow affordance.
  - Use existing `Button`/`Text`/`Container`, not hardcoded client chrome.
- [ ] Add a demo/kitchen-sink or `for-you` variant that assembles:
  - Story circle rail
  - Featured live game hero carousel
  - Editorial overlay rail

Verification:
- [ ] JSON snapshots assert each helper emits `AtomicComposite` with `data.ui`, not a new permanent section.
- [ ] Golden fixtures added under `schema/examples/` for each new pattern.
- [ ] iOS fixtures sync via `make ios-fixtures-sync` or `make ios-build`.

Anti-pattern guards:
- Do not build team logo URLs client-side.
- Do not invent copy or default assets in clients.
- Do not put outer section margin/background inside permanent renderers.

### Phase 5: Client Styling Fidelity Pass

Once the new composition primitives exist, tune platform renderers for the real-app feel using existing token and variant discipline.

- [ ] Define or reuse existing color tokens for:
  - dark feed canvas
  - raised card surface
  - scrim overlays
  - live red
  - CTA gold/yellow
- [ ] Prefer existing semantic tokens and palette aliases before adding new ones.
- [ ] If a new stable variant is truly needed, add it schema-first with `style-tokens.json` evidence and all three client resolvers.
- [ ] Add or tune reusable inline properties in server helpers:
  - corner radii
  - shadow
  - aspect ratios
  - padding/gap
  - max lines
  - text weight/color
- [ ] Ensure text over image always has a server-declared scrim/overlay layer for contrast.

Verification:
- [ ] Light and dark mode screenshots for all new patterns on web/iOS/Android.
- [ ] `rg "semanticDirect|SEMANTIC_DIRECT"` remains empty unless deliberately reintroduced with a documented reason.
- [ ] No new hardcoded client image URLs.

Anti-pattern guards:
- Do not make clients decide whether text should be light/dark based on image content.
- Do not use client runtime color math for team or scrim decisions; server emits the overlay colors/tokens.

### Phase 6: Testing And Regression Coverage

- [ ] Add schema validation fixtures for:
  - story rail
  - editorial overlay rail
  - featured live game hero
  - paged carousel with dots
  - sponsor logo row
- [ ] Add web tests for:
  - overlay DOM structure and accessible labels
  - scroll page indicator state
  - no dots when omitted
- [ ] Add iOS tests or snapshot-friendly fixtures for model round trip and rendering smoke checks.
- [ ] Add Android compile/render smoke checks where current test infrastructure supports it.
- [ ] Add anti-pattern greps to the verification checklist:
  - no new permanent section type for real-app feed modules
  - no client-constructed CDN URLs
  - no client-invented fallback copy
  - no raw style/class escape hatch

Verification:
- [ ] `make codegen` after schema changes.
- [ ] `node scripts/validate-color-tokens.js`.
- [ ] `node scripts/validate-style-tokens.js`.
- [ ] `npm --prefix web run build`.
- [ ] `./gradlew :app:compileDebugKotlin`.
- [ ] `make ios-build`.

## Dependencies

- Schema/codegen pipeline must be used before any new atomic type or `ScrollContainer` field is emitted by the server.
- Overlay support depends on client atomic routers gaining a renderer on all three platforms.
- Page dots depend on local client scroll state, which is allowed as runtime presentation state but must be triggered by server-declared `pageIndicator`.
- Real video playback and ad SDK integrations remain outside this plan.

## Open Questions

- [ ] Should the overlay primitive be a new `OverlayContainer` atomic type, or should `Container` gain an explicit overlay layout mode?
- [ ] Should `pageIndicator` live on `ScrollContainer`, or should it be a sibling primitive bound to scroll state by ID?
- [ ] Do we need a first-class “scrim” semantic token, or are existing overlay/color tokens sufficient?
- [ ] Should the real-app feed demo live as a Kitchen Sink section group, a new demo screen variant, or a `for-you` experiment option?
- [ ] Should page dots be supported only for paged carousels, or also for normal scroll rails?

## Completion Criteria

- The real-app screenshot modules can be represented as server-emitted `AtomicComposite` trees.
- New capabilities are schema-first, codegen-backed, and implemented on web/iOS/Android.
- Global app chrome, ads, and video SDK hosts remain outside atomic content.
- Clients remain presentation-only: no invented assets, copy, routes, or business policy.
- All verification commands pass.
