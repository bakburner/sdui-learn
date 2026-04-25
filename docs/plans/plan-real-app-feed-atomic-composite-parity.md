# Plan: Real App Feed Atomic Composite Parity

> Source inputs: real NBA app screenshots provided 2026-04-25, `AGENTS.md`, `docs/client-implementors-contract.md`, `docs/sdui-design-system.md`, ADR-013, `schema/sdui-schema.json`, `schema/color-tokens.json`, `schema/style-tokens.json`, and current atomic renderers/composers.

## Summary

Use the atomic-composite capabilities needed to emulate the real NBA app look and feel across For You, Games, Discover, and adjacent content screens: circular live story rails, dark editorial image cards with text overlays, featured live game hero cards, schedule game rows, utility card grids, league rails, section headers with CTA affordances, snap/paged carousels with optional page indicators, and sponsor/logo rows. Keep global app chrome, real video playback, ads, and SDK mounts outside atomic content.

`OverlayContainer` and `ScrollContainer.pageIndicator` have since been introduced across the schema, generated models, and web/iOS/Android renderers. The next step is therefore not a broad schema pass; it is a server-composition and visual-fidelity pass that uses the real-app atomic helpers already present in `AtomicCompositeBuilder`.

The guiding principle is server-authored structure and semantics, with clients only realizing platform-native presentation and runtime-local scroll/theme state.

## Phase 0: Documentation Discovery

| Source | Findings |
|--------|----------|
| `AGENTS.md` | Stateless feed UI should default to `AtomicComposite`. New permanent sections require client-owned state, SDK hosting, or runtime lifecycle. Section outer chrome belongs to `section.surface` / `SectionContainer`. Clients must not invent copy, URLs, routes, or business meaning. |
| `docs/client-implementors-contract.md` | `AtomicComposite` uses `data.ui` plus optional `data.content`. `bindRef` updates leaf values from live content. Shared `AtomicBox` owns box model uniformly. Renderer-local animation/clock behavior is allowed when driven by server data. |
| `docs/sdui-design-system.md` | Styling has three layers: inline atomic props, per-primitive variants, and color tokens. No parent-to-child style cascade. Team colors are not design-system tokens. |
| `docs/adr/013-style-tokens-for-atomic-primitives.md` | Variant additions must be stable, reusable, platform-owned presentation semantics. `AtomicComposite` is a host for an atomic tree, not a visual variant host. |
| `schema/sdui-schema.json` | Atomic primitives now cover row/column layout, image, text, button, scroll, divider, spacer, conditional, display grid, section slot, live clock, `OverlayContainer`, and server-declared page dots on paged scroll containers. |
| Current composers | `AtomicCompositeBuilder` supports legacy content rails plus real-app helpers for story circle rails, editorial overlay rails, featured live game hero cards, real-app headers, utility card grids, league rails, and game schedule rows/lists. These helpers are demoed, but the production-style For You composer still mostly uses older rails/headers/game panels. |

## Screens Considered

| Screen | Real-app pattern | Atomic fit | Notes |
|--------|------------------|------------|-------|
| For You / Home | Top tabs, live circular story rail, featured live game hero carousel, page dots, bottom nav | Mixed | Story rail and hero card should be `AtomicComposite`. Top tabs are `TabGroup` if they swap server section lists. Bottom nav and app header are app shell. |
| Games | Date selector, ad/promo banner, vertical list of game rows, sponsor broadcast row, more menu | Mixed | Promo banner and game rows can be atomic composites. Date selector likely needs client-owned calendar/date state unless server sends a static date strip plus actions. Ads remain `AdSlot`. |
| Discover | Promo banner, "Around The League" utility card grid, "Other Leagues" horizontal card rail | Strong | Utility grid and league rail fit `AtomicComposite` using `DisplayGrid`, `ScrollContainer`, `Image`, `Text`, and `Button`/actions. |
| App shell | NBA logo, profile/cast icons, bottom navigation, status bar | Not atomic | Keep as client shell / `screen.navigation`. Do not model this as content primitives unless the architecture intentionally moves shell chrome into the server contract. |

## Current State

| Capability | Status | Notes |
|------------|--------|-------|
| Circular story/live rail | Built, needs fidelity tuning | `buildStoryCircleRail` exists. Current private item helper uses 64pt circular images and a badge, but the screenshots need larger images, red circular rings, overlapping `LIVE` pills, and short labels. |
| Section header + More CTA | Built, needs fidelity tuning | `buildRealAppSectionHeader` exists. Needs uppercase title rhythm and a yellow/gold `More` arrow affordance where the screenshots use it. |
| Basic content rail | Built | `buildContentRail` creates 16:9 cards with headline below image. Real app needs tall image cards with text over image. |
| Featured live game card | Built, needs fidelity tuning | `buildFeaturedLiveGameHero` exists with key art, score strip, sponsor logos, live bindings, paging, and dots. Current private card helper is narrower and more symmetric than the screenshots. |
| Image overlay / scrim card | Built | `OverlayContainer` exists and is rendered on web/iOS/Android. Use it for image cards, hero art, story badge overlap, and scrimmed text. |
| Carousel snap/paging | Built | `ScrollContainer` supports `paging` and `snapAlignment`; real-app helpers can emit it for hero carousels. |
| Carousel page dots | Built | `pageIndicator.style: "dots"` is supported when server-declared on a paged scroll container with multiple children. |
| Sponsor/logo strip | Partial | Can be composed from `Image` and `Text`; real SDK ads remain `AdSlot`. |
| Discover utility grid | Built, needs fidelity tuning | `buildUtilityCardGrid` exists. Needs screenshot-like tile height, icon treatment, two-column rhythm, and light/dark raised surfaces. |
| League card rail | Built, needs fidelity tuning | `buildLeagueCardRail` exists. Needs larger logo emphasis and card sizing closer to the Discover screenshots. |
| Games schedule row | Built, needs fidelity tuning | `buildGameScheduleRow` and `buildGameScheduleList` exist. Current team row is symmetric; screenshots need left/right score ordering, a center live status with red dot, separated broadcast strip, and overflow action. |
| Date selector strip | Needs classification | Static date chips can be atomic. Calendar picker/current-date state likely belongs to a permanent section or app-level interaction, unless every date change is represented as a server action. |
| Global nav / bottom bar | Built as app chrome | Should remain `screen.navigation` / native shell, not atomic content. |
| Video playback | Reserved SDK section | Actual player remains `VideoPlayer`, not atomic content. |

## Screenshot-Specific Visual Targets

These are the concrete treatments from the supplied real-app screenshots that should drive the next implementation pass. They are not pixel-perfect requirements, but they are the details that make the feed read as NBA-app-like.

| Area | Screenshot cue | SDUI application |
|------|----------------|------------------|
| Top feed tabs | Latest / Following / Catch Up / Playoffs sit on a black bar with muted labels and a yellow active underline. | Model as `TabGroup` or server-authored action chips. Server owns tab labels/actions and selected state; clients only render the selected underline/accent. |
| Story/live rail | Circular images are larger than ordinary avatars, have red rings, red `LIVE` pills, and short labels. | Tune `storyCircleItem`: larger diameter, red border/ring, `OverlayContainer` badge overlap, tighter rail spacing, one-line labels. |
| Hero game carousel | Cards are nearly full feed width, image-dominant, and paged with dots below. | Tune `featuredLiveGameHeroCard`: wider card, taller art, page dots visible only via `pageIndicator`, enough horizontal inset to hint at adjacent cards when practical. |
| Hero image area | Live badge appears over image, overflow menu appears at top-right, key art dominates the card. | Compose image as `OverlayContainer` with top-left badge, optional top-right overflow action, and bottom scrim/title when needed. |
| Score layout | Left team logo/name sits to the left of the left score; right score sits before right team logo/name. Center shows red live dot plus period/clock. | Split symmetric helpers into left/right variants for hero and schedule rows. Add a small red dot element before live status text. |
| Game metadata | Series text is quiet and centered below the score/status row. | Keep `seriesText` as subdued `labelSmall` / tertiary text in the game body, not in the sponsor row. |
| Sponsor/broadcast row | Peacock/NBC logos sit in a distinct bottom strip; overflow dots trail at the right. | Compose a separated bottom row with a subtle divider/surface, centered logo group, and server-declared overflow action. |
| Games date strip | Month label, weekday row, horizontal dates, selected day in a blue circle. | If static, compose as atomic chips/actions; if it owns local calendar state, document as a permanent-section exception. Use blue selected date token, not the feed-tab yellow. |
| Promo banners | Wide, short image/text banners appear in Discover and Games. | Use atomic promo/banner composition for marketing content. Paid ad inventory remains `AdSlot`. |
| Editorial/video cards | Large portrait or landscape media cards use bold white text over bottom scrims, often with `NEW`/`LIVE` labels. | Prefer `EditorialOverlayRail` over generic `ContentRail` for screenshot-like modules. |
| Full-bleed video hero | Some feed modules are large image/video stills with title, short dek, and an outlined CTA over the media. | Compose as `OverlayContainer` with scrim and an outlined/button-like CTA if no real playback is mounted. Use `VideoPlayer` when playback is required. |
| Headline list card | A raised dark card can contain a branded header row and compact story rows with thumbnails. | Can be built as an atomic composite list using raised surface, dividers, row thumbnails, and server actions. No new permanent section required. |
| Discover utility tiles | Two-column dark raised tiles use simple white outline icons and labels. | `UtilityCardGrid` should allow either server image URLs or icon tokens, with generous tile height and left/top icon alignment tuned by module. |
| Other leagues rail | Horizontal cards with large league logos and labels sit on raised surfaces. | Tune `LeagueCardRail` card width/height, logo sizing, and scroll padding. |
| BAL/live media card | Badge at top-left, audio icon over image, large title/dek below, share affordance at lower right. | Treat as an editorial/media overlay card plus server-declared trailing action/share icon if action semantics exist. |
| Dark dividers | Cards often use thin separators between image/body/footer areas. | Add atomic `Divider` elements or border fields inside card composition where the screenshot has clear body/footer separation. |
| Typography | Titles are condensed-looking, bold, often uppercase; secondary text is small and muted. | Use current text variants/weights first. Do not add a typography variant unless existing variants cannot approximate the hierarchy. |
| Light mode | Preserve the real-app layout but invert the canvas/cards with semantic tokens. | Use light values of `color.surface.canvas`, `color.surface.raised`, and text tokens; keep red live rings, blue date selection, yellow tab/CTA accents, and explicit dark scrims over imagery. |

## Atomic Precedence Assessment

| Candidate | Decision | Flag |
|-----------|----------|------|
| Circular story/live rail | Use atomic composite | No violation. Existing primitives plus overlay support are enough. |
| Featured live game hero card | Use atomic composite | No violation if video playback is not embedded. Use `VideoPlayer` only when real playback is required. |
| Editorial image card with title over art | Use existing `OverlayContainer` | Avoid one-off `EditorialCard` primitive. Text over image must carry server-declared scrim/overlay. |
| Carousel dots | Use existing `ScrollContainer.pageIndicator` | Acceptable because it is scroll presentation state. Flag if clients auto-add dots without server declaration. |
| Discover utility cards | Use atomic composite / `DisplayGrid` | No new primitive needed. Flag any proposal for a dedicated `DiscoverGrid` section as unnecessary proliferation. |
| League card rail | Use atomic composite / `ScrollContainer` | No new primitive needed. |
| Games compact schedule rows | Use atomic composite helper | No new permanent section unless row interaction requires client-owned state beyond actions. |
| Date strip with selectable days | Use existing `TabGroup` or atomic action chips for simple navigation | Flag as potentially violating if implemented as client screen-specific logic. A rich calendar picker may justify a permanent section because it owns date interaction state. |
| Promo/ad banners | Split responsibilities | Marketing promo can be atomic. Paid ad SDK inventory remains `AdSlot`. Flag any atomic recreation of real ad SDK behavior. |
| App header and bottom nav | Keep as app shell | Flag any plan to reproduce shell chrome as atomic feed content. |
| Account, cast, overflow icons | Server action/icon tokens or app shell | Flag if clients invent routes or behavior. Overflow menu may need action semantics if server-driven. |

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
  - Pattern names: `StoryCircleRail`, `EditorialOverlayRail`, `FeaturedLiveGameHero`, `GameScheduleRow`, `UtilityCardGrid`, `LeagueCardRail`, `ScoreStrip`, `SponsorLogoRow`.
  - These names should be server builder/helper names, not new section types.
- [ ] Decide which gaps are schema gaps versus composer/style gaps.
  - Composer/style gaps: story rail ring/badge treatment, sponsor logo strip styling, real-app section header affordance, featured game score layout, Discover utility tile density, league rail sizing, compact schedule row layout, static date action strip.
  - Schema/client gaps should be avoided unless implementation proves existing atomics cannot express the screenshot cue. `OverlayContainer` and `pageIndicator` are already available.
- [ ] Classify the Games date selector before implementation.
  - If it is a static date strip where each chip navigates or refreshes by server action, compose it atomically.
  - If it owns interactive calendar state, month navigation, local selection, or native date picker behavior, treat it as a candidate permanent section and document the exception.

Verification:
- [ ] `rg "ContentRail|GamePanel|EditorialOverlay" docs/ AGENTS.md schema/` confirms docs describe these as atomic-composite patterns, not new permanent sections.
- [ ] `node scripts/validate-color-tokens.js` and `node scripts/validate-style-tokens.js` pass if touched.

Anti-pattern guards:
- Do not add a `RealAppFeed` permanent section.
- Do not add `DiscoverGrid`, `LeagueRail`, or `GameScheduleRow` permanent sections while they remain stateless compositions.
- Do not add a catch-all `className`, `styleName`, or platform-specific raw CSS escape hatch.
- Do not add speculative variants just to reduce repeated inline prop bags.

### Phase 2: Use And Validate General Overlay Layout Support

`OverlayContainer` now exists in the schema, generated models, and web/iOS/Android renderers. Use it as the primary way to express “image fills card, gradient scrim overlays bottom, title/badge sits over image.”

- [ ] Validate existing `OverlayContainer` support with the real-app fixture and demo helpers.
- [ ] Use `OverlayContainer` for:
  - story rail badge overlap
  - top-left live/new badges on image cards
  - bottom scrims with title/dek
  - optional hero-card overflow affordances over image art
  - full-bleed video/editorial stills with outlined CTA
- [ ] Ensure `AtomicBox` remains the only owner of margin, padding, background, radius, shadow, width/height, and opacity.
- [ ] Add diagnostics only if current unsupported overlay alignment behavior is insufficient after strict decode.

Verification:
- [ ] Fixture with image base + bottom scrim + text overlay renders on all three clients.
- [ ] No renderer constructs image URLs or fallback copy.
- [ ] `rg "background image is decoded but constrained" ios android` still reflects the policy that arbitrary atomic background images are not reintroduced as the primary overlay path.

Anti-pattern guards:
- Do not use `Container.background.imageUrl` as the primary solution for mobile image cards unless the mobile policy is deliberately reopened.
- Do not make overlay layout a permanent section renderer.

### Phase 3: Use And Validate Scroll Paging And Page Indicators

The real app hero carousel has dots and paging. `ScrollContainer.pageIndicator` now exists and supports `style: "dots"` when the server declares it on a paged scroll container.

- [ ] Ensure real-app hero carousels emit `paging: true`, `snapAlignment: "center"`, and `pageIndicator.style: "dots"` when multiple cards are present.
- [ ] Tune carousel card width/insets so the card feels nearly full-width while preserving platform scroll behavior.
- [ ] Keep indicators as presentation of a server-declared scroll affordance, not invented per client.
- [ ] Do not add dots to normal story/editorial rails unless the server explicitly declares them and the rail is meant to page.

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
  - Larger circular `Image`, red live ring/border, live/new badge overlay, label below.
  - Server provides image URL, label, badge text, action URI.
- [ ] `buildEditorialOverlayRail(...)`
  - Horizontal `ScrollContainer`.
  - Tall/larger aspect-ratio image cards.
  - `OverlayContainer` base image + gradient scrim + title/subtitle/badges.
  - Optional “NEW” or “LIVE” badge from server payload.
- [ ] `buildFeaturedLiveGameHero(...)`
  - Paged `ScrollContainer` for hero cards when more than one.
  - Wider, image-dominant key art area using overlay support.
  - Score strip with asymmetric left/right team layouts: left logo/name then score, center red-dot status/clock, score then right logo/name.
  - Quiet centered series text.
  - Separated sponsor/broadcast logo row composed as atomic `Image` elements, with optional overflow action.
  - Live values use `data.content` + `bindRef` + existing `dataBinding`.
- [ ] `buildRealAppSectionHeader(...)`
  - Uppercase title row with optional “More” CTA and yellow/gold arrow affordance where the module calls for it.
  - Use existing `Button`/`Text`/`Container`, not hardcoded client chrome.
- [ ] `buildUtilityCardGrid(...)`
  - Two-column `DisplayGrid` or wrapped row/column composition.
  - Raised dark/light card surfaces, server-provided icon token or image, label, generous tile height, and action.
  - Use for Discover "Around The League" style modules.
- [ ] `buildLeagueCardRail(...)`
  - Horizontal `ScrollContainer`.
  - Raised cards with larger league logo image, label, tuned width/height, and horizontal peeking.
  - Server owns league IDs, labels, logos, and destination actions.
- [ ] `buildGameScheduleRow(...)`
  - Compact vertical list row for Games screen.
  - Team logos/names/seeds, score or start time, red-dot period/clock, series text, sponsor/broadcast logo row, and overflow action.
  - Match the left/right score ordering used by the real app instead of a single symmetric team helper.
  - Live values use `data.content` + `bindRef` when the row is live.
- [ ] `buildStaticDateStrip(...)` or reuse `TabGroup`/chips if classified as atomic.
  - Month label, weekday/date row, selected date circle, server-owned date actions.
  - Use blue selected-date treatment as seen in Games, not the yellow feed-tab underline.
- [ ] `buildHeadlineListCard(...)` if needed for the Home/Latest feed.
  - Raised card with branded header row and compact story rows with thumbnails/dividers.
- [ ] `buildMediaOverlayCard(...)` if needed for full-bleed video/editorial stills without playback.
  - Large media base, scrimmed title/dek, outlined CTA, and server-declared action.
- [ ] Add a demo/kitchen-sink, `for-you`, `games`, and `discover` variant that assembles:
  - Story circle rail
  - Featured live game hero carousel
  - Editorial overlay rail
  - Utility card grid
  - League card rail
  - Compact game schedule rows
  - Static date strip, headline list card, and media overlay card where appropriate

Verification:
- [ ] JSON snapshots assert each helper emits `AtomicComposite` with `data.ui`, not a new permanent section.
- [ ] Golden fixtures added under `schema/examples/` for each new pattern.
- [ ] iOS fixtures sync via `make ios-fixtures-sync` or `make ios-build`.

Anti-pattern guards:
- Do not build team logo URLs client-side.
- Do not invent copy or default assets in clients.
- Do not put outer section margin/background inside permanent renderers.
- Do not model bottom nav, profile, cast, or status bar as feed atomics.
- Do not implement paid ad behavior as atomics; only server-authored promo art/content belongs here.

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
  - borders/rings for live story tiles
  - dividers between game-card body/footer areas
  - asymmetric score/team layout widths
- [ ] Ensure text over image always has a server-declared scrim/overlay layer for contrast.
- [ ] Preserve accent semantics in light mode:
  - yellow/gold for active feed tab and `More` affordances
  - red for live rings, live pills, and live status dots
  - blue for selected Games date chips
  - dark scrims over imagery in both light and dark modes

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
  - Discover utility card grid
  - league card rail
  - Games schedule row
  - static date action strip, if selected as atomic
  - headline list card, if added
  - full-bleed media overlay card, if added
- [ ] Add web tests for:
  - overlay DOM structure and accessible labels
  - scroll page indicator state
  - no dots when omitted
- [ ] Add iOS tests or snapshot-friendly fixtures for model round trip and rendering smoke checks.
- [ ] Add Android compile/render smoke checks where current test infrastructure supports it.
- [ ] Add anti-pattern greps to the verification checklist:
  - no new permanent section type for real-app feed modules
  - no new permanent section type for Discover utility cards or league rails
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

- Schema/codegen pipeline must be used before any new atomic type or `ScrollContainer` field is emitted by the server. Current planned work should not require this because `OverlayContainer` and `pageIndicator` already exist.
- Overlay support depends on client atomic routers; current renderers support `OverlayContainer` on web/iOS/Android.
- Page dots depend on local client scroll state, which is allowed as runtime presentation state but must be triggered by server-declared `pageIndicator`.
- Real video playback and ad SDK integrations remain outside this plan.

## Open Questions

- [x] Should the overlay primitive be a new `OverlayContainer` atomic type, or should `Container` gain an explicit overlay layout mode? Decision: use `OverlayContainer`.
- [x] Should `pageIndicator` live on `ScrollContainer`, or should it be a sibling primitive bound to scroll state by ID? Decision: `pageIndicator` lives on `ScrollContainer`.
- [ ] Do we need first-class semantic tokens for yellow/gold CTA, selected-date blue, and live ring colors, or are existing palette/brand aliases sufficient?
- [ ] Should the real-app feed demo live as a Kitchen Sink section group, a new demo screen variant, or a `for-you` experiment option?
- [ ] Should page dots be supported only for paged carousels, or also for normal scroll rails?
- [ ] Should the Games date selector be modeled as atomic action chips, `TabGroup`, or a justified permanent date-picker section?
- [ ] Should overflow menu actions be added as server-declared action groups, or stay as existing app-shell behavior where applicable?
- [ ] Should utility card icons use server-provided image URLs, an icon token registry, or both?
- [ ] Should headline list cards and full-bleed media CTA cards become named server helper patterns, or remain one-off demo/feed compositions until repeated?

## Completion Criteria

- The real-app screenshot modules that fit atomic precedence can be represented as server-emitted `AtomicComposite` trees.
- Any module that does not fit atomic precedence is explicitly classified as app shell, reserved SDK section, `TabGroup`, `AdSlot`, `VideoPlayer`, or a separately justified permanent section.
- New capabilities, if any are still required after the styling pass, are schema-first, codegen-backed, and implemented on web/iOS/Android.
- Global app chrome, ads, and video SDK hosts remain outside atomic content.
- Clients remain presentation-only: no invented assets, copy, routes, or business policy.
- All verification commands pass.
