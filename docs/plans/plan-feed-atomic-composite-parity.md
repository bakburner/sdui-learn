# Plan: Feed Atomic Composite Parity

> Source inputs: real NBA app screenshots provided 2026-04-25, `AGENTS.md`, `docs/client-implementors-contract.md`, `docs/sdui-design-system.md`, ADR-013, `schema/sdui-schema.json`, `schema/color-tokens.json`, `schema/style-tokens.json`, and current atomic renderers/composers.

## Status

> **Phases 1–6 complete.** Remaining work: Phase 5 token blockers (scrim alias,
> brand.nba color), unchecked verification commands, open questions on demo
> hosting / overflow actions / icon strategy, and `buildStaticDateStrip` /
> `buildHeadlineListCard` helpers (deferred from Phase 4).

## Summary

Use the atomic-composite capabilities needed to emulate the real NBA app look and feel across For You, Games, Discover, and adjacent content screens: circular live story rails, dark editorial image cards with text overlays, featured live game hero cards, schedule game rows, utility card grids, league rails, section headers with CTA affordances, snap/paged carousels with optional page indicators, and sponsor/logo rows. Keep global app chrome, real video playback, ads, and SDK mounts outside atomic content.

`OverlayContainer` and `ScrollContainer.pageIndicator` have since been introduced across the schema, generated models, and web/iOS/Android renderers. The next step is therefore not a broad schema pass; it is a server-composition and visual-fidelity pass that uses the feed atomic helpers already present in `AtomicCompositeBuilder`.

The guiding principle is server-authored structure and semantics, with clients only realizing platform-native presentation and runtime-local scroll/theme state.

## Phase 0: Documentation Discovery

| Source | Findings |
|--------|----------|
| `AGENTS.md` | Stateless feed UI should default to `AtomicComposite`. New semantic sections require client-owned state, SDK hosting, or runtime lifecycle. Section outer chrome belongs to `section.surface` / `SectionContainer`. Clients must not invent copy, URLs, routes, or business meaning. |
| `docs/client-implementors-contract.md` | `AtomicComposite` uses `data.ui` plus optional `data.content`. `bindRef` updates leaf values from live content. Shared `AtomicBox` owns box model uniformly. Renderer-local animation/clock behavior is allowed when driven by server data. |
| `docs/sdui-design-system.md` | Styling has three layers: inline atomic props, per-primitive variants, and color tokens. No parent-to-child style cascade. Team colors are not design-system tokens. |
| `docs/adr/013-style-tokens-for-atomic-primitives.md` | Variant additions must be stable, reusable, platform-owned presentation semantics. `AtomicComposite` is a host for an atomic tree, not a visual variant host. |
| `schema/sdui-schema.json` | Atomic primitives now cover row/column layout, image, text, button, scroll, divider, spacer, conditional, display grid, section slot, live clock, `OverlayContainer`, and server-declared page dots on paged scroll containers. |
| Current composers | `AtomicCompositeBuilder` supports legacy content rails plus feed helpers for story circle rails, editorial overlay rails, featured live game hero cards, section header composites, utility card grids, league rails, and game schedule rows/lists. These helpers are demoed, but the production-style For You composer still mostly uses older rails/headers/game panels. |

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
| Section header + More CTA | Built, needs fidelity tuning | `buildSectionHeaderComposite` exists. Needs uppercase title rhythm and a yellow/gold `More` arrow affordance where the screenshots use it. |
| Basic content rail | Built | `buildContentRail` creates 16:9 cards with headline below image. Real app needs tall image cards with text over image. |
| Featured live game card | Built, needs fidelity tuning | `buildFeaturedLiveGameHero` exists with key art, score strip, sponsor logos, live bindings, paging, and dots. Current private card helper is narrower and more symmetric than the screenshots. |
| Image overlay / scrim card | Built | `OverlayContainer` exists and is rendered on web/iOS/Android. Use it for image cards, hero art, story badge overlap, and scrimmed text. |
| Carousel snap/paging | Built | `ScrollContainer` supports `paging` and `snapAlignment`; feed helpers can emit it for hero carousels. |
| Carousel page dots | Built | `pageIndicator.style: "dots"` is supported when server-declared on a paged scroll container with multiple children. |
| Sponsor/logo strip | Partial | Can be composed from `Image` and `Text`; real SDK ads remain `AdSlot`. |
| Discover utility grid | Built, needs fidelity tuning | `buildUtilityCardGrid` exists. Needs screenshot-like tile height, icon treatment, two-column rhythm, and light/dark raised surfaces. |
| League card rail | Built, needs fidelity tuning | `buildLeagueCardRail` exists. Needs larger logo emphasis and card sizing closer to the Discover screenshots. |
| Games schedule row | Built, needs fidelity tuning | `buildGameScheduleRow` and `buildGameScheduleList` exist. Current team row is symmetric; screenshots need left/right score ordering, a center live status with red dot, separated broadcast strip, and overflow action. |
| Date selector strip | Needs classification | Static date chips can be atomic. Calendar picker/current-date state likely belongs to a semantic section or app-level interaction, unless every date change is represented as a server action. |
| Global nav / bottom bar | Built as app chrome | Should remain `screen.navigation` / native shell, not atomic content. |
| Video playback | Reserved SDK section | Actual player remains `VideoPlayer`, not atomic content. |

## Screenshot-Specific Visual Targets

These are the concrete treatments from the supplied real NBA app screenshots that should drive the next implementation pass. They are not pixel-perfect requirements, but they are the details that make the feed read as NBA-app-like.

| Area | Screenshot cue | SDUI application |
|------|----------------|------------------|
| Top feed tabs | Latest / Following / Catch Up / Playoffs sit on a black bar with muted labels and a yellow active underline. | Model as `TabGroup` or server-authored action chips. Server owns tab labels/actions and selected state; clients only render the selected underline/accent. |
| Story/live rail | Circular images are larger than ordinary avatars, have red rings, red `LIVE` pills, and short labels. | Tune `storyCircleItem`: larger diameter, red border/ring, `OverlayContainer` badge overlap, tighter rail spacing, one-line labels. |
| Hero game carousel | Cards are nearly full feed width, image-dominant, and paged with dots below. | Tune `featuredLiveGameHeroCard`: wider card, taller art, page dots visible only via `pageIndicator`, enough horizontal inset to hint at adjacent cards when practical. |
| Hero image area | Live badge appears over image, overflow menu appears at top-right, key art dominates the card. | Compose image as `OverlayContainer` with top-left badge, optional top-right overflow action, and bottom scrim/title when needed. |
| Score layout | Left team logo/name sits to the left of the left score; right score sits before right team logo/name. Center shows red live dot plus period/clock. | Split symmetric helpers into left/right variants for hero and schedule rows. Add a small red dot element before live status text. |
| Game metadata | Series text is quiet and centered below the score/status row. | Keep `seriesText` as subdued `labelSmall` / tertiary text in the game body, not in the sponsor row. |
| Sponsor/broadcast row | Peacock/NBC logos sit in a distinct bottom strip; overflow dots trail at the right. | Compose a separated bottom row with a subtle divider/surface, centered logo group, and server-declared overflow action. |
| Games date strip | Month label, weekday row, horizontal dates, selected day in a blue circle. | If static, compose as atomic chips/actions; if it owns local calendar state, document as a semantic-section exception. Use blue selected date token, not the feed-tab yellow. |
| Promo banners | Wide, short image/text banners appear in Discover and Games. | Use atomic promo/banner composition for marketing content. Paid ad inventory remains `AdSlot`. |
| Editorial/video cards | Large portrait or landscape media cards use bold white text over bottom scrims, often with `NEW`/`LIVE` labels. | Prefer `EditorialOverlayRail` over generic `ContentRail` for screenshot-like modules. |
| Full-bleed video hero | Some feed modules are large image/video stills with title, short dek, and an outlined CTA over the media. | Compose as `OverlayContainer` with scrim and an outlined/button-like CTA if no real playback is mounted. Use `VideoPlayer` when playback is required. |
| Headline list card | A raised dark card can contain a branded header row and compact story rows with thumbnails. | Can be built as an atomic composite list using raised surface, dividers, row thumbnails, and server actions. No new semantic section required. |
| Discover utility tiles | Two-column dark raised tiles use simple white outline icons and labels. | `UtilityCardGrid` should allow either server image URLs or icon tokens, with generous tile height and left/top icon alignment tuned by module. |
| Other leagues rail | Horizontal cards with large league logos and labels sit on raised surfaces. | Tune `LeagueCardRail` card width/height, logo sizing, and scroll padding. |
| BAL/live media card | Badge at top-left, audio icon over image, large title/dek below, share affordance at lower right. | Treat as an editorial/media overlay card plus server-declared trailing action/share icon if action semantics exist. |
| Dark dividers | Cards often use thin separators between image/body/footer areas. | Add atomic `Divider` elements or border fields inside card composition where the screenshot has clear body/footer separation. |
| Typography | Titles are condensed-looking, bold, often uppercase; secondary text is small and muted. | Use current text variants/weights first. Do not add a typography variant unless existing variants cannot approximate the hierarchy. |
| Light mode | Preserve the real NBA app layout but invert the canvas/cards with semantic tokens. | Use light values of `color.surface.canvas`, `color.surface.raised`, and text tokens; keep red live rings, blue date selection, yellow tab/CTA accents, and explicit dark scrims over imagery. |

## Atomic Precedence Assessment

| Candidate | Decision | Flag |
|-----------|----------|------|
| Circular story/live rail | Use atomic composite | No violation. Existing primitives plus overlay support are enough. |
| Featured live game hero card | Use atomic composite | No violation if video playback is not embedded. Use `VideoPlayer` only when real playback is required. |
| Editorial image card with title over art | Use existing `OverlayContainer` | Avoid one-off `EditorialCard` primitive. Text over image must carry server-declared scrim/overlay. |
| Carousel dots | Use existing `ScrollContainer.pageIndicator` | Acceptable because it is scroll presentation state. Flag if clients auto-add dots without server declaration. |
| Discover utility cards | Use atomic composite / `DisplayGrid` | No new primitive needed. Flag any proposal for a dedicated `DiscoverGrid` section as unnecessary proliferation. |
| League card rail | Use atomic composite / `ScrollContainer` | No new primitive needed. |
| Games compact schedule rows | Use atomic composite helper | No new semantic section unless row interaction requires client-owned state beyond actions. |
| Date strip with selectable days | Use existing `TabGroup` or atomic action chips for simple navigation | Flag as potentially violating if implemented as client screen-specific logic. A rich calendar picker may justify a semantic section because it owns date interaction state. |
| Promo/ad banners | Split responsibilities | Marketing promo can be atomic. Paid ad SDK inventory remains `AdSlot`. Flag any atomic recreation of real ad SDK behavior. |
| App header and bottom nav | Keep as app shell | Flag any plan to reproduce shell chrome as atomic feed content. |
| Account, cast, overflow icons | Server action/icon tokens or app shell | Flag if clients invent routes or behavior. Overflow menu may need action semantics if server-driven. |

## Requirements Addressed

- [ ] Emulate the real NBA app’s feed modules using server-composed atomic trees.
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

- [x] Resolve doc/schema drift around current variant values before adding any new variant.
  - Check `schema/sdui-schema.json`, `schema/style-tokens.json`, and `docs/sdui-design-system.md`.
  - If any doc claims variants not in schema, update the doc or add the schema value via the proper codegen path.
- [x] Document the “feed” target as atomic-composite patterns, not semantic sections.
  - Pattern names: `StoryCircleRail`, `EditorialOverlayRail`, `FeaturedLiveGameHero`, `GameScheduleRow`, `UtilityCardGrid`, `LeagueCardRail`, `ScoreStrip`, `SponsorLogoRow`.
  - These names should be server builder/helper names, not new section types.
- [x] Decide which gaps are schema gaps versus composer/style gaps.
  - Composer/style gaps: story rail ring/badge treatment, sponsor logo strip styling, section header composite affordance, featured game score layout, Discover utility tile density, league rail sizing, compact schedule row layout, static date action strip.
  - Schema/client gaps should be avoided unless implementation proves existing atomics cannot express the screenshot cue. `OverlayContainer` and `pageIndicator` are already available.
- [x] Classify the Games date selector before implementation, using the AGENTS.md §6.1 four-question test:
  - Does it own interactive client state that cannot be expressed as server actions (month navigation, native date picker, scroll-to-load future dates)?
  - Does it host a runtime that the server cannot drive directly (e.g. native calendar SDK)?
  - Does it require client-only lifecycle the atomic tree cannot express?
  - Is its visual chrome unstable enough that variants alone cannot capture it?
  - If any answer is yes, it is a candidate `DatePickerStrip` semantic section and the exception must be documented.
  - If all answers are no, compose it as atomic action chips or `TabGroup`. Static date strips where each chip is an enumerated server action default to the atomic path.

Verification:
- [ ] `rg "ContentRail|GamePanel|EditorialOverlay" docs/ AGENTS.md schema/` confirms docs describe these as atomic-composite patterns, not new semantic sections.
- [ ] `node scripts/validate-color-tokens.js` and `node scripts/validate-style-tokens.js` pass if touched.

Anti-pattern guards:
- Do not add a `FeedScreen` semantic section.
- Do not add `StoryCircleRail`, `EditorialOverlayRail`, `FeaturedLiveGameHero`, `SectionHeaderComposite`, `DiscoverGrid`, `UtilityCardGrid`, `LeagueCardRail`, `GameScheduleRow`, `GameScheduleList`, `HeadlineListCard`, `MediaOverlayCard`, `StaticDateStrip`, `SponsorLogoRow`, or `ScoreStrip` as semantic section types while they remain stateless atomic compositions. They are server builder/helper names, not section types.
- Do not add a catch-all `className`, `styleName`, or platform-specific raw CSS escape hatch.
- Do not add speculative variants just to reduce repeated inline prop bags.

#### Phase 1 Outcome: Gap Classification

Work remaining for feed parity, using the Atomic Precedence Assessment table plus Screenshot-Specific Visual Targets modules not already listed there. Classifications: **`composer/style`** — server helper tuning, inline atomic props, or token alignment; **`schema/client`** — would require a new schema field, primitive, client renderer change, or new variant; **`exception`** — intentional semantic section, app shell, or reserved SDK surface (`TabGroup`, `AdSlot`, `VideoPlayer`, app shell, or a future justified `DatePickerStrip`).

| Module | Classification | Notes |
|--------|----------------|-------|
| Circular story/live rail | composer/style | Rings, badges, spacing, `OverlayContainer`; no new wire vocabulary. |
| Featured live game hero card | composer/style | Layout, paging, dots, sponsor row; embed real playback only via `VideoPlayer` (**exception**), not fake video in atomics. |
| Editorial image card (title over art) | composer/style | `OverlayContainer` + scrim + text; helper `buildEditorialOverlayRail`. |
| Carousel page dots | composer/style | `ScrollContainer.pageIndicator` + tuning; clients must not invent dots. |
| Discover utility cards | composer/style | `DisplayGrid` / `buildUtilityCardGrid` density and surfaces. |
| League card rail | composer/style | `buildLeagueCardRail` sizing and scroll padding. |
| Games compact schedule rows | composer/style | Asymmetric score/broadcast row via composition; `buildGameScheduleRow` / list. |
| Date strip (selectable days) | composer/style | Default: server-authoritative selection on action chips or `TabGroup`-like model; see **Phase 1 Outcome: Games Date Selector Classification**. Native calendar / un-round-trippable local state → **exception** candidate (`DatePickerStrip`), not `schema/client` by default. |
| Promo vs paid ad banners | composer/style + exception | Marketing promo art = atomic; real ad inventory = **`AdSlot`**. |
| App header and bottom nav | exception | App shell / `screen.navigation`; not feed atomics. |
| Account, cast, overflow icons | composer/style + exception | Prefer server actions/icon tokens; routes from shell stay shell-owned. |
| Top feed tabs (Latest / Following / …) | exception or composer/style | **`TabGroup`** when swapping server section lists; else server-owned action chips — both are composition, not new schema. |
| Hero image area (badges, overflow, key art) | composer/style | `OverlayContainer` stacking; already in scope for Phase 2–4. |
| Asymmetric score layout (hero + schedule) | composer/style | Left/right team + center status; composer helpers. |
| Game metadata (series text) | composer/style | Typography and placement; no new primitive. |
| Sponsor/broadcast logo row | composer/style | Composed `Image`/`Text`/`Divider` row; not a section type. |
| Full-bleed video/editorial hero | composer/style + exception | Still + CTA = atomics; real playback = **`VideoPlayer`**. |
| Headline list card | composer/style | Raised list + thumbnails; optional `buildHeadlineListCard` (Phase 4). |
| BAL/live media card | composer/style | Overlay + actions; optional `buildMediaOverlayCard` (Phase 4). |
| Dark dividers / typography / light mode | composer/style | Tokens, weights, `Divider`, theme resolution. |

Schema/client items requiring foreman attention: none.

Remaining gaps are addressable with existing atomics (`OverlayContainer`, `pageIndicator`, `ScrollContainer`, `DisplayGrid`, etc.) and composer tuning. If a future product decision requires a **native** calendar or client-computed “today” that cannot be server-round-tripped, reclassify the date UI as **`exception`** (semantic section) per AGENTS.md §6.1; that is not a schema gap until an explicit proposal is made.

#### Phase 1 Outcome: Games Date Selector Classification

Four questions (restated from Phase 1 checklist; aligned with AGENTS.md §6.1 decision test for semantic sections):

| Question | Answer | Rationale |
|----------|--------|-----------|
| Does it own interactive client state that cannot be expressed as server actions (month navigation, native date picker, scroll-to-load future dates)? | No (for the screenshot target path) | The **Games date strip** row describes a month label, weekday row, horizontal dates, and a **server-indicated** selected day (blue circle). That model can use **navigate/refresh actions** per chip and a **server-authoritative `selected`** flag; the client does not need to invent “today” from the device clock if the server marks selection (per plan: clients must not compute selected from local timezone). |
| Does it host a runtime that the server cannot drive directly (e.g. native calendar SDK)? | No (for the default path) | The strip is horizontal labels + **actions**, not an embedded `DatePicker` / platform calendar UI. A **future** native calendar would answer **yes** here. |
| Does it require client-only lifecycle the atomic tree cannot express? | No | Horizontal `ScrollContainer`, `Text`/`Button` chips, optional `Conditional`, and normal scroll affordances suffice; no SSE or SDK mount implied for the strip itself. |
| Is its visual chrome unstable enough that variants alone cannot capture it? | No | Blue selected state uses existing token guidance (primary blue / brand primary); chip geometry is inline props and composition, not a new **variant** value. |

**Decision:** **atomic action chips** (with server-owned selection and actions), or **`TabGroup`** if product models the strip as tab-like section swapping. **`DatePickerStrip`** as a semantic section remains a **conditional** path only if product later requires native calendar UX or client-local date state that cannot be round-tripped—then document under AGENTS.md §6.6 and the §6.1 tests.

**Rationale:** The screenshot-backed target is a **stateless, composable** strip: each date is a declarative control with server-driven selected styling. That matches the default atomic path and server authority (AGENTS.md §1.1). TabGroup is appropriate when the UX is literally switching among server-provided section lists, not because the date row is a new wire type.

**Follow-up:** If **`DatePickerStrip`** is ever chosen, later documentation must add: registry entry in **AGENTS.md §6.6** with **which §6.1 exception class** applies; explicit statement that outer chrome uses `section.surface` / `SectionContainer`; payload contract for reservation or inner content; and that clients do not invent dates, labels, or “selected” without server declaration.

### Phase 2: Use And Validate General Overlay Layout Support

`OverlayContainer` now exists in the schema, generated models, and web/iOS/Android renderers. Use it as the primary way to express “image fills card, gradient scrim overlays bottom, title/badge sits over image.”

- [x] Validate existing `OverlayContainer` support with the feed fixture and demo helpers.
- [x] Use `OverlayContainer` for:
  - story rail badge overlap
  - top-left live/new badges on image cards
  - bottom scrims with title/dek
  - optional hero-card overflow affordances over image art
  - full-bleed video/editorial stills with outlined CTA
- [x] Ensure `AtomicBox` remains the only owner of **wire-emitted** `margin`, `padding`, `background`, `cornerRadius`, `shadow`, `width`/`height`, and `opacity` from `AtomicElement`. Variant chrome applied by variant resolvers (per ADR-013) and platform-required content clipping (e.g., image content clip-to-radius, button label content padding) are the documented exceptions and do not constitute AtomicBox ownership violations.
- [x] Add diagnostics only if current unsupported overlay alignment behavior is insufficient after strict decode.

Verification:
- [ ] Fixture with image base + bottom scrim + text overlay renders on all three clients. _(deferred: requires user-run builds; static fixture structure confirmed in Phase 2 Task 1.)_
- [x] No renderer constructs image URLs or fallback copy. _(static spot-check in Phase 2 Task 4 — `AtomicImage` and `AtomicOverlayContainer` paths PASS on web/iOS/Android.)_
- [x] `rg "background image is decoded but constrained" ios android` still reflects the policy that arbitrary atomic background images are not reintroduced as the primary overlay path. _(2 hits intact: `android/sdui-core/.../AtomicBox.kt:136`, `ios/.../ContainerVariantResolver.swift:212` and `:241`.)_

Anti-pattern guards:
- Do not use `Container.background.imageUrl` as the primary solution for mobile image cards unless the mobile policy is deliberately reopened.
- Do not make overlay layout a semantic section renderer.

#### Phase 2 Outcome: OverlayContainer Validation

**Task 1 — Fixture coverage (schema/examples)**

Only one fixture under `schema/examples/` references `OverlayContainer`: `schema/examples/feed-screen-composite.json` (editorial image card, three featured paged `OverlayContainer` children). No other example JSON in that folder uses it.

| Target use (Phase 2 list) | Covered by fixture? | Notes |
|----------------------------|---------------------|--------|
| Story rail badge overlap | No | **Phase 4 follow-up:** no story rail / `ScrollContainer` + circular items with overlay badge in any fixture. |
| Top-left live/new badges on image cards | Partial | `feed-screen-composite` places a **HIGHLIGHT** strip + title/dek in a **bottom** overlay column; featured carousel items have **no** top-left badge, only bottom scrim + title. **Phase 4 follow-up:** top-start LIVE/NEW overlay fixture when composer parity needs it. |
| Bottom scrims with title/dek | Yes | Editorial card and all three featured carousel pages: gradient scrim + text. |
| Optional hero-card overflow affordances over image art | No | **Phase 4 follow-up:** no overflow/action overlay layer in the fixture. |
| Full-bleed video/editorial stills with outlined CTA | No | **Phase 4 follow-up:** no outlined over-media CTA in the fixture. |

**Task 2 — `AtomicCompositeBuilder` helper adoption**

Searched `server/.../AtomicCompositeBuilder.java` for `overlayContainer(` call sites. Helpers `buildHeadlineListCard`, `buildMediaOverlayCard`, and `buildStaticDateStrip` are **not** present in the server codebase (Phase 4 scope).

| Helper | `OverlayContainer` used today? | Should use (per overlay target list)? | Phase 4 follow-up? |
|--------|--------------------------------|----------------------------------------|--------------------|
| `buildStoryCircleRail` | Yes — `storyCircleItem` wraps the avatar with `overlayContainer` when `badgeText` is set | Yes (badge overlap) | Tune fidelity only; already emits overlay. |
| `buildEditorialOverlayRail` | Yes — `editorialOverlayCard` → `overlayContainer` + scrim column | Yes | None for wiring. |
| `buildFeaturedLiveGameHero` | Yes — `featuredLiveGameHeroCard` → `overlayContainer` for key art + title/scrim | Yes | Top-right overflow / extra overlay layers not yet in helper (composer Phase 4). |
| `buildLeagueCardRail` | No | No — raised icon/text cards, not image scrims | None unless product wants image-dominant league cards. |
| `buildUtilityCardGrid` | No | No — icon + label tiles | None unless product wants overlay on tiles. |
| `buildGameScheduleRow` / `buildGameScheduleList` | No | No — list rows, not full-bleed image overlays | None for overlay wiring. |
| `buildSectionHeaderComposite` | No | No — row header, not an image overlay | None. |
| `buildHeadlineListCard` / `buildMediaOverlayCard` / `buildStaticDateStrip` | N/A (not defined) | `buildMediaOverlayCard` (when added) should use overlays per Phase 4 plan; others as designed | Implement/named in Phase 4. |

**Task 3 — `AtomicBox` sole-owner verification (read-only Grep + spot reads)**

| Platform | Result | Findings (foreman) |
|----------|--------|----------------------|
| Web | PASS (variant chrome) | `web/src/components/atomic/AtomicButton.tsx` applies variant padding, borderRadius, and backgroundColor on the inner `<button>` via `variantStyles` / `baseButtonStyle` — that is variant chrome (ADR-013), not wire-emitted `AtomicBox` ownership. Outer `AtomicBox` still owns margin/width/opacity from the wire. |
| iOS | PASS (variant + content-clip) | `AtomicButtonView` label padding and `SduiButtonStyle` background/cornerRadius are variant chrome. `AtomicImageView` cornerRadius / `clipShape` is the SwiftUI-required content-clip path; `AtomicBoxModifier` still applies the same value to the wrapper background/shadow. |
| Android | PASS (variant chrome) | `AtomicButton.kt` Material3 Button/OutlinedButton/TextButton own button chrome; KDoc L21–23 explicitly documents the box-model-on-`AtomicBox` / Material-on-button-chrome split. `AtomicImage.kt` and `AtomicOverlayContainer.kt` route wire box-model through `AtomicBox`; overlay `Modifier.padding` is layer-positioning inset, not box-model. |

**Conclusion (foreman, opus-4-7 audit):** the wire-emitted `AtomicBox` ownership rule **holds** on all three platforms. The cited locations are not box-model bypasses:

- `AtomicButton` (web/iOS/Android) applies button chrome — internal label padding, surface background, button corner radius — through **variant chrome** (ButtonVariant resolver), which is the documented ADR-013 pattern. Wire-emitted `AtomicBox` props on a `Button` element still flow through `AtomicBox` as the outer wrapper.
- `AtomicImageView` (iOS) applies `cornerRadius`/`clipShape` to clip the image content because SwiftUI does not auto-clip child content to the wrapper's corner radius. The same `cornerRadius` value is **also** applied by `AtomicBoxModifier` for the wrapper background/shadow shape; the dual application is required by SwiftUI semantics, not a duplicate ownership.
- `AtomicDisplayGridView`, `AtomicScrollContainerView` page-indicator dots, `AtomicDividerView` thickness — all primitive-internal layout chrome, not wire-emitted box-model props.

**Architecture pattern preserved.** No Phase 2 fix required. If a future audit wants to consolidate variant chrome into a `BoxModifier`-style pipeline (e.g., have variant resolvers emit `AtomicBox`-shaped overrides instead of applying chrome themselves), that is a separate refactor proposal and out of scope here.

**Task 4 — Strict-decode and image-URL / overlay spot-check**

1. **Policy grep** (`rg "background image is decoded but constrained"` per checklist — `rg` not available in the agent environment; equivalent workspace search): **2** matches — `android/sdui-core/.../renderer/atomic/AtomicBox.kt` (L136); `ios/.../ContainerVariantResolver.swift` (L212, L241). Policy string intact.
2. **OverlayContainer renderers** (`web` / `ios` / `android` paths for `OverlayContainer`): no hardcoded `https://`, `nba.com/`, or placeholder **copy** in `AtomicOverlayContainer` sources; routing delegates to `AtomicBox` + child `AtomicRouter`. **PASS** (no client-fabricated media URLs in overlay path).
3. **AGENTS.md §3.2 (server-provided image URLs):** `AtomicImage` on web (bindRef + `element.src` + `placeholder` only), iOS (`URL(string: src)` from payload/bindRef, optional `placeholder` on failure), and Android (`AsyncImage` `model = currentSrc` from bindRef/src, optional `placeholder` retry) do **not** construct CDN URLs. **PASS**

**Phase 4 follow-ups identified by Phase 2**

- **Fixtures:** Add coverage for story-rail overlay badges; top-left LIVE/NEW on cards; hero overflow affordance; full-bleed + outlined CTA (when `buildMediaOverlayCard` or equivalent exists) — all deferred; no JSON added in Phase 2.
- **Composers:** Extend `buildFeaturedLiveGameHero` (or payload) for optional top-right overflow when product requires; optional `buildMediaOverlayCard` / `buildHeadlineListCard` / `buildStaticDateStrip` as in Phase 4 list.
- **Renderer architecture:** `AtomicButton` three-platform variant-chrome split and iOS `AtomicImageView` dual clip/corner path are confirmed as **documented architecture patterns** (ADR-013 + SwiftUI semantics), not violations. No follow-up needed unless a future foreman audit proposes consolidating variant chrome into the `AtomicBox` pipeline.

**Open questions / blockers for foreman**

- (none)

### Phase 3: Use And Validate Scroll Paging And Page Indicators

The real NBA app hero carousel has dots and paging. `ScrollContainer.pageIndicator` now exists and supports `style: "dots"` when the server declares it on a paged scroll container.

- [x] Ensure feed hero carousels emit `paging: true`, `snapAlignment: "center"`, and `pageIndicator.style: "dots"` when multiple cards are present.
- [ ] Tune carousel card width/insets so the card feels nearly full-width while preserving platform scroll behavior.
- [x] Keep indicators as presentation of a server-declared scroll affordance, not invented per client.
- [x] Do not add dots to normal story/editorial rails unless the server explicitly declares them and the rail is meant to page.
- [x] Explicitly do not declare `pageIndicator` on these screenshot modules: `buildStoryCircleRail`, `buildEditorialOverlayRail`, `buildLeagueCardRail`, `buildGameScheduleList`, `buildUtilityCardGrid`, sponsor/broadcast logo rows, and the static date strip. Their `ScrollContainer`s are continuous, not paged.

Verification:
- [ ] A fixture with `paging: true` and `pageIndicator.style: "dots"` shows correct active dot after scrolling.
- [ ] A fixture without `pageIndicator` has no dots.
- [ ] Android/iOS/Web builds pass.

Anti-pattern guards:
- Do not add dots automatically to every carousel.
- Do not infer page count from screen identity.
- Do not make dots content; they are scroll affordance presentation.

#### Phase 3 Outcome: Paged Carousel and Page Indicator Validation

**Task 1 — Schema field shape (read-only, `schema/sdui-schema.json`)**

- **Result:** **Confirmed** — not a foreman blocker.
- `paging` on `ScrollContainer` / `AtomicElement`: **boolean** (`"paging": { "type": "boolean" }`).
- `snapAlignment` enum: **`"start"`**, **`"center"`**, **`"end"`**.
- `PageIndicator` (object): **`style` enum: `"dots"` only**; **`alignment`** → `BadgeAlignment` (`"topStart"`, `"topCenter"`, `"topEnd"`, `"centerStart"`, `"center"`, `"centerEnd"`, `"bottomStart"`, `"bottomCenter"`, `"bottomEnd"`); optional **`color`**, **`activeColor`** (color tokens). Schema description: clients render the indicator only when declared.

**Task 2 — Fixture audit (positive list)**

| Fixture | paging | snapAlignment | pageIndicator.style | Comment |
|---------|--------|---------------|---------------------|---------|
| `schema/examples/feed-screen-composite.json` | `true` | `"center"` | `"dots"` | All three present on `id: "featured-paged-carousel"`; three `OverlayContainer` children (`featured-page-giannis`, `featured-page-curry`, `featured-page-jokic`). **Only** `schema/examples/` file referencing `paging` / `snapAlignment` / `pageIndicator` (grep, Phase 3). |

**Gaps / Phase 4 follow-up:** No other example JSON in `schema/examples/` includes these fields — add golden fixtures when story/editorial/league rails are snapshotted to JSON if broader paging coverage is needed.

**Task 3 — Fixture audit (negative list, continuous rails must not page)**

`schema/examples/` contains **a single** `ScrollContainer` node in **one** file (`feed-screen-composite.json` — the featured paged hero). There are **no** non-hero `ScrollContainer` instances in the examples tree to scan; story/editorial/league/utility patterns are not represented as `ScrollContainer` JSON in this folder at Phase 3.

| Fixture | Negative-list scroll container path | pageIndicator declared? | Notes |
|---------|-------------------------------------|-------------------------|--------|
| — | — | — | **0** fixture-level violations. No `ScrollContainer` in `schema/examples/` besides the intended paged featured carousel. |

**Task 4 — Server helper audit (`AtomicCompositeBuilder.java`)**

| Helper | paging? | snapAlignment? | pageIndicator? | Compliant with Phase 3 rule? | Phase 4 follow-up? |
|--------|---------|----------------|------------------|-----------------------------|------------------------|
| `buildStoryCircleRail` | No (`scrollRow` only) | No | No | Yes | None |
| `buildEditorialOverlayRail` | No | No | No | Yes | None |
| `buildLeagueCardRail` | No | No | No | Yes | None |
| `buildGameScheduleList` | No (column of rows; no `ScrollContainer`) | No | No | Yes | None |
| `buildUtilityCardGrid` | No (`DisplayGrid`-style row layout; no `ScrollContainer`) | No | No | Yes | None |
| `buildSectionHeaderComposite` | No (single row `Container`) | No | No | Yes | None |
| Sponsor/broadcast **logo row** (private `logoRow` + `container("row",…)` in `featuredLiveGameHeroCard` and schedule row) | No | No | No | Yes (not a `ScrollContainer`) | None |
| `buildStaticDateStrip` | **Not defined** in server codebase | — | — | N/A | Name/classify in Phase 4; when implemented, must not put `pageIndicator` on a continuous date strip. |
| `buildFeaturedLiveGameHero` | **Yes** when more than one valid card | **Yes** `"center"` when paged | **Yes** `style: "dots"` + alignment/colors when paged | Yes for multi-card carousel; single-card path omits paging/indicator (expected) | None |

**Task 5 — Client renderer audit (no client-side invention)**

| Platform | Renders dots only when server-declared? | Notes (file:line) |
|----------|----------------------------------------|----------------------|
| Web | **PASS** | `web/src/components/atomic/AtomicScrollContainer.tsx` **L24**: `hasPageIndicator` requires `paging === true` **and** `pageIndicator?.style === 'dots'` **and** `children.length > 1`. Dots path **L80–101**. `activePage` from scroll listener **L48–52** (runtime scroll state). If `paging` is true but `snapAlignment` is omitted, child snap uses `element.snapAlignment ?? 'start'` **L66** — servers should emit `snapAlignment` explicitly for paged carousels to avoid ambiguity. |
| iOS | **PASS** | `ios/Sources/SduiCore/Rendering/Atomic/AtomicScrollContainerView.swift` **L22–23**: `shouldShowPageIndicator` requires `paging` and `pageIndicator?.style == .dots` and **>1** child. Dots only in that branch **L26–35**. `activePage` from scroll position **L57** / `ScrollPositionModifier`. **L74–76**: warning when `snapAlignment != .start` — iOS may not fully realize non-start snap; not a wire-shape blocker for Phase 3. |
| Android | **PASS** | `android/sdui-core/src/main/java/com/nba/sdui/core/renderer/atomic/AtomicScrollContainer.kt` **L63**: `showPageDots` requires `paging == true` **and** `pageIndicator?.style == Style.Dots` **and** `children.size > 1`. Dots with `HorizontalPager` use **`pagerState.currentPage`** **L91–100** (runtime). Branches that overlay dots on `LazyRow` without paging are unreachable given **L63** (dead code) — no dots without `paging`. |

**Phase 4 follow-ups identified by Phase 3**

- **Fixtures:** Only `feed-screen-composite.json` exercises `paging` / `snapAlignment` / `pageIndicator`. Add/extend `schema/examples/` when other composed modules (story rail, editorial rail, etc.) are snapshotted, if the team wants static coverage beyond the single hero `ScrollContainer`.
- **`buildStaticDateStrip`:** Not present; paging and `pageIndicator` policy apply when the helper is introduced.
- **Web snap default:** When `paging: true` and `snapAlignment` is omitted, web defaults per-child snap align to `start` (`AtomicScrollContainer.tsx:66`); **prefer explicit server emission** of `snapAlignment` for paged carousels (the featured helper already emits `"center"` when paged).
- **iOS snap realization:** `snapAlignment` values other than `start` log a warning and may not match design intent on iOS until scroll-target behavior is extended — track as renderer fidelity, not schema drift.

**Open questions / blockers for foreman**

- (none)

### Phase 4: Server Builder Patterns For Feed Modules

Add reusable `AtomicCompositeBuilder` helpers that compose feed-style surfaces from server data.

- [x] `buildStoryCircleRail(...)`
  - Horizontal `ScrollContainer`.
  - Larger circular `Image`, red live ring/border, live/new badge overlay, label below.
  - Server provides image URL, label, badge text, action URI.
- [x] `buildEditorialOverlayRail(...)`
  - Horizontal `ScrollContainer`.
  - Tall/larger aspect-ratio image cards.
  - `OverlayContainer` base image + gradient scrim + title/subtitle/badges.
  - Optional “NEW” or “LIVE” badge from server payload.
- [x] `buildFeaturedLiveGameHero(...)`
  - Paged `ScrollContainer` for hero cards when more than one.
  - Wider, image-dominant key art area using overlay support.
  - Score strip with asymmetric left/right team layouts: left logo/name then score, center red-dot status/clock, score then right logo/name.
  - Quiet centered series text.
  - Separated sponsor/broadcast logo row composed as atomic `Image` elements, with optional overflow action. Broadcaster image URLs and the overflow action target must be server-declared; clients must never derive them from `gameId` or carrier identity (AGENTS.md §1.1, §3.2).
  - Live values use `data.content` + `bindRef` + existing `dataBinding`.
- [x] `buildSectionHeaderComposite(...)`
  - Uppercase title row with optional “More” CTA and yellow/gold arrow affordance where the module calls for it.
  - Use existing `Button`/`Text`/`Container`, not hardcoded client chrome.
- [x] `buildUtilityCardGrid(...)`
  - Two-column `DisplayGrid` or wrapped row/column composition.
  - Raised dark/light card surfaces, server-provided icon token or image, label, generous tile height, and action.
  - Use for Discover "Around The League" style modules.
- [x] `buildLeagueCardRail(...)`
  - Horizontal `ScrollContainer`.
  - Raised cards with larger league logo image, label, tuned width/height, and horizontal peeking.
  - Server owns league IDs, labels, logos, and destination actions.
- [x] `buildGameScheduleRow(...)`
  - Compact vertical list row for Games screen.
  - Team logos/names/seeds, score or start time, red-dot period/clock, series text, sponsor/broadcast logo row, and overflow action.
  - Match the left/right score ordering used by the real NBA app instead of a single symmetric team helper.
  - Sponsor/broadcast logos and overflow action target are server-declared, never inferred client-side from game/team identity.
  - Live values use `data.content` + `bindRef` when the row is live.
- [ ] `buildStaticDateStrip(...)` or reuse `TabGroup`/chips if classified as atomic. _(Deferred: Phase 1 classifies the Games date row as atomic chips or `TabGroup` unless a future `DatePickerStrip` is justified; no `buildStaticDateStrip` in this dispatch.)_
  - Month label, weekday/date row, selected date circle, server-owned date actions.
  - Selection is a presentation reflection: clients render `selected=true` for the chip the server marked. Clients must never compute "today is selected" from a local clock or device timezone.
  - Use blue selected-date treatment as seen in Games, not the yellow feed-tab underline.
- [ ] `buildHeadlineListCard(...)` if needed for the Home/Latest feed.
  - Raised card with branded header row and compact story rows with thumbnails/dividers.
- [x] `buildMediaOverlayCard(...)` if needed for full-bleed video/editorial stills without playback.
  - Large media base, scrimmed title/dek, outlined CTA, optional top-left badge, and server-declared action.
  - Optional audio-state and share affordances rendered as overlay icons backed by server-declared actions only.
  - Playback decision: if the card is a tap-to-open editorial preview, stay on the atomic image + overlay path. If real audio/video must mount inside the card region, that area becomes a `VideoPlayer` reservation rectangle (AGENTS.md §6.4) with payload-owned dimensions, not an atomic substitute for playback.
- [ ] Add a demo/kitchen-sink, `for-you`, `games`, and `discover` variant that assembles:
  - Story circle rail
  - Featured live game hero carousel
  - Editorial overlay rail
  - Utility card grid
  - League card rail
  - Compact game schedule rows
  - Static date strip, headline list card, and media overlay card where appropriate

Verification:
- [x] JSON snapshots assert each helper emits `AtomicComposite` with `data.ui`, not a new semantic section.
- [x] Golden fixtures added under `schema/examples/` for each new pattern.
- [ ] iOS fixtures sync via `make ios-fixtures-sync` or `make ios-build`.

Helper consistency:
- Any helper that needs paged-carousel behavior (today: `buildFeaturedLiveGameHero`; tomorrow: a possible media-overlay carousel) must call a single shared internal helper (e.g. `pagedHorizontalScroll(...)`) rather than re-emit `paging`, `snapAlignment`, and `pageIndicator` inline. This keeps snap behavior, dot style, and accessibility aligned across paged rails without inventing a new wire-level concept.
- **Update (Phase 4):** `pagedHorizontalScroll(...)` is implemented as the shared private helper; `buildFeaturedLiveGameHero` calls it and does not inline `paging` / `snapAlignment` / `pageIndicator`.

Anti-pattern guards:
- Do not build team logo URLs client-side.
- Do not invent copy or default assets in clients.
- Do not put outer section margin/background inside permanent renderers.
- Do not model bottom nav, profile, cast, or status bar as feed atomics.
- Do not implement paid ad behavior as atomics; only server-authored promo art/content belongs here.
- Do not introduce a `PageableRail` semantic section, atomic primitive, `ScrollContainer` variant, or schema synonym. The four `ScrollContainer` fields (`direction`, `paging`, `snapAlignment`, `pageIndicator`) are the contract for paged carousels.

#### Phase 4 Outcome

- **Files changed:** `server/src/main/java/com/nba/sdui/service/AtomicCompositeBuilder.java`, `server/src/main/java/com/nba/sdui/service/DemoScreenComposer.java`, `server/src/main/java/com/nba/sdui/service/ScheduleComposer.java`, `server/src/test/java/com/nba/sdui/service/AtomicCompositeBuilderFeedModulesTest.java`, `schema/examples/feed-screen-composite.json`, `schema/examples/schedule-2024-25.json`, `docs/plans/plan-feed-atomic-composite-parity.md`.
- **Helpers refactored / added:** `pagedHorizontalScroll` (private); `buildFeaturedLiveGameHero` refactored to use it; optional column 16 `heroOverflowUri` on hero card rows for top-right overflow; `buildMediaOverlayCard` added; `buildGameScheduleRow` contract documented; `ScheduleComposer` passes `targetUri` from mock data only (no `nba://game/` string build from `gameId`).
- **Fixtures:** Extended `feed-screen-composite.json` (story rail with badge, NEW image card, media overlay with scrim + secondary CTA + share/audio, hero overflow on first featured page, pageIndicator tokens); `schedule-2024-25.json` gains `targetUri` per game for schedule composer passthrough.
- **Tests:** `AtomicCompositeBuilderFeedModulesTest` — `buildMediaOverlayCard` envelope; multi/single hero paging; server URL passthrough for broadcast/overflow/main actions.
- **Composer call sites:** `DemoScreenComposer` hero cards include overflow URI; `ScheduleComposer` uses `targetUri` + `overflowUri` + `broadcastLogoUrls` from JSON.
- **Open questions / blockers:** None for foreman. **NICE:** `buildStaticDateStrip`, `buildHeadlineListCard`, and full kitchen-sink screen variant are Phase 5+ follow-ups.

### Phase 5: Client Styling Fidelity Pass

Once the new composition primitives exist, tune platform renderers for the feed look-and-feel using existing token and variant discipline.

- [x] Define or reuse existing color tokens for:
  - dark feed canvas
  - raised card surface
  - scrim overlays
  - live red
  - CTA gold/yellow
- [x] Prefer existing semantic tokens and palette aliases before adding new ones.
- [x] If a new stable variant is truly needed, add it schema-first with `style-tokens.json` evidence and all three client resolvers. _(N/A this pass — no new variants.)_
- [x] Add or tune reusable inline properties in server helpers:
  - corner radii
  - shadow
  - aspect ratios
  - padding/gap
  - max lines
  - text weight/color
  - borders/rings for live story tiles
  - dividers between game-card body/footer areas
  - asymmetric score/team layout widths
- [x] Ensure text over image always has a server-declared scrim/overlay layer for contrast.
- [x] Express outlined-over-media CTAs (e.g. `Go To Game` over Saturday's Stacked Slate hero) via inline `border` + transparent `background` + on-media text color on `Button`. Do not introduce an `outlinedOverMedia` button variant; this is composition, not a new variant value (AGENTS.md §11). _(See Phase 5 Outcome: `AtomicElement` has no wire `border`; `secondary` + `TEXT_INVERSE` + transparent `background` only.)_
- [~] Default accent token resolution for this iteration (provisional, no new tokens added yet):
  - active feed tab + `More` CTA arrow → `color.brand.nba` _(blocked at registry per Phase 5 Outcome BLOCKER 2 — `color.brand.nba` currently aliases `color.blue.50`, not gold; composer emits the token, but resolved color is navy)_
  - live rings, live pills, live status dots → `color.brand.live` ✓
  - selected Games date chip → existing primary blue palette alias (`color.palette.blue.50` or current `color.brand.primary`, whichever the tokens file already exposes)
  - scrim overlays → existing `OVERLAY_SCRIM` token _(blocked at registry per Phase 5 Outcome BLOCKER 1 — alias resolves to `#FFFFFF` in dark mode and brightens media)_
  - Promote any of these to a dedicated stable accent token only if a styling pass shows real fragmentation across surfaces, per AGENTS.md §11.4.
- [~] Preserve accent semantics in light mode:
  - yellow/gold for active feed tab and `More` affordances _(blocked: composer emits the right token; registry alias is wrong — see BLOCKER 2)_
  - red for live rings, live pills, and live status dots ✓
  - blue for selected Games date chips
  - dark scrims over imagery in both light and dark modes _(blocked in dark mode per BLOCKER 1)_

Verification:
- [ ] Light and dark mode screenshots for all new patterns on web/iOS/Android.
- [x] `rg "semanticDirect|SEMANTIC_DIRECT"` remains empty unless deliberately reintroduced with a documented reason. _(Workspace grep: only this plan line mentions the pattern.)_
- [x] No new hardcoded client image URLs.

Anti-pattern guards:
- Do not make clients decide whether text should be light/dark based on image content.
- Do not use client runtime color math for team or scrim decisions; server emits the overlay colors/tokens.

#### Phase 5 Outcome

- **Files changed:** `server/src/main/java/com/nba/sdui/service/AtomicCompositeBuilder.java`, `docs/plans/plan-feed-atomic-composite-parity.md`.

- **Task A — Token alignment (server `ColorTokens` + `schema/color-tokens.json`):**

| Phase 5 use-case | Mapped constant / token | Notes |
|------------------|---------------------------|--------|
| Dark feed canvas | `SURFACE_CANVAS` → `token:color.surface.canvas` | OK |
| Raised card surface | `SURFACE_RAISED` | OK |
| Scrim gradient end-stop | `OVERLAY_SCRIM` → `token:color.overlay.scrim` | **Registry issue:** semantic aliases `color.grey.100` → light `#000000`, dark `#FFFFFF` — end stop becomes **lightening** in dark mode, not a dark scrim. See BLOCKERS. |
| Live red | `BRAND_LIVE` | OK → `color.red.50` palette |
| CTA / “More” accent | `BRAND_NBA` | **Registry issue:** `color.brand.nba` aliases **`color.blue.50`**, not yellow/gold (`color.secondary.*` / `color.yellow.*`). Plan narrative says gold for tab/More; token file says blue. See BLOCKERS. |
| Selected Games date chip | `PALETTE_BLUE_50` / `color.blue.50` | **Not used** in in-scope helpers (`buildStaticDateStrip` deferred). No composer change. |

- **Helpers tuned (server composition only):**

  - **`storyCircleItem` / `buildStoryCircleRail`:** Larger avatar (70dp), tighter scroll `gap` (14), live **ring** via nested `Container` + `BRAND_LIVE` stroke band when `badgeText` is set; label `maxLines` unchanged (1).
  - **`editorialOverlayCard` / `buildEditorialOverlayRail`:** Card width 200, radius 18, taller bottom scrim padding, `maxLines` 3 on title/subtitle; scrim uses **`mediaBottomScrimGradient()`** (`#00000000` → `OVERLAY_SCRIM`); per-corner radii on scrim layer bottom; rail `gap` 14.
  - **`featuredLiveGameHeroCard` / `buildFeaturedLiveGameHero`:** Card width 338, default **`shadow`**, key art aspect ratio unchanged; title overlay scrim uses **`mediaBottomScrimGradient()`**; **hairline `Divider`** above sponsor row when present; **asymmetric hero row:** away = name stack then score, home = score then name stack; wider side/center slots; **live dot** + status row when hero has a top badge column.
  - **`buildSectionHeaderComposite`:** Title **uppercase** (`Locale.ROOT`); optional action uses **`TEXT` button + `BRAND_NBA`** on `color` (gold intent per plan; see token audit).
  - **`utilityCard` / `buildUtilityCardGrid`:** Heavier vertical padding, **`shadow`** on tiles.
  - **`leagueCard` / `buildLeagueCardRail`:** Card width 160, larger logo frame with **`aspectRatio` 4:3, `shadow`**.
  - **`gameScheduleRowElement` / `buildGameScheduleRow` & list:** **`shadow`** on card; **hairline `Divider`** before broadcast/overflow row; **asymmetric `scheduleTeam`:** away = logo → labels → score, home = score → labels → logo; unequal team column widths (124 vs 108); **live dot** when status string contains `"LIVE"` (case-insensitive).
  - **`buildMediaOverlayCard`:** CTA **`secondary`** + `TEXT_INVERSE` + explicit **`background: "#00000000"`**; scrim unchanged (`mediaBottomScrimGradient`).
  - **`pagedHorizontalScroll`:** Unchanged (Phase 5 did not reshape).

- **Scrim coverage (Task C):**

| Helper | Overlay over image? | Scrim / gradient under text? |
|--------|---------------------|-------------------------------|
| `storyCircleItem` | Badge-only overlay on avatar | N/A for full-bleed text |
| `editorialOverlayCard` | Yes | Yes — bottom column `mediaBottomScrimGradient()` |
| `featuredLiveGameHeroCard` | Yes | Yes — `mediaBottomScrimGradient()` on title block |
| `buildMediaOverlayCard` | Yes | Yes — copy column + `mediaBottomScrimGradient()` |
| `buildGameScheduleRow` / league / utility / header | No full-bleed image overlays in scope | N/A |

- **Outlined-over-media CTA (Task D):**

| Location | Approach | Status |
|----------|----------|--------|
| `buildMediaOverlayCard` CTA | `variant: "secondary"`, `color: TEXT_INVERSE`, `background: "#00000000"` | **Partial:** `AtomicElement` has **no** `border` in `schema/sdui-schema.json`; cannot wire explicit inverse border on `Button`. `secondary` supplies outline via client variant chrome (theme-dependent). **BLOCKER** if product requires token-driven border on the control itself. |

- **Light-mode token resolution audit (Task E):**

| Token / usage | Light | Dark | Plan intent vs actual |
|---------------|-------|------|------------------------|
| `color.brand.nba` | `color.blue.50` → deep blue | lighter blue | Plan: gold tab/More — **mismatch** (see BLOCKERS). |
| `color.brand.live` | `color.red.50` | `color.red.50` | Red live accents — OK. |
| `color.overlay.scrim` | `#000000` via grey.100 | `#FFFFFF` via grey.100 | Dark scrim in both modes — **fails in dark** (see BLOCKERS). |
| `color.text.inverse` | grey.0 light | grey.0 dark | Appropriate for on-media copy when paired with darkening scrim. |

- **BLOCKERS for foreman**

  1. **`color.overlay.scrim` / `color.grey.100`:** Dark mode resolves to **white**. Image scrims that use `OVERLAY_SCRIM` as the gradient end-stop risk **inverting** the scrim. Fix options: repoint `color.overlay.scrim` at a dark-alpha primitive that is stable across schemes, or introduce a dedicated always-dark scrim alias (schema/token pipeline — out of scope for Phase 5 agent).
  2. **`color.brand.nba` vs gold CTA narrative:** Registry maps `brand.nba` to **blue**, not `color.secondary` / yellow. Align naming or add a composer-legal constant for gold (e.g. `token:color.secondary.50`) if marketing tokens must match copy in Open Questions.
  3. **Outlined CTA border:** No `border` on `AtomicElement` in schema; `buildMediaOverlayCard` cannot emit server-declared stroke on `Button` without schema/codegen + renderer work or a container wrapper (would add nesting).

- **NICE follow-ups (Phase 6+):** Snapshot `schema/examples/feed-screen-composite.json` / iOS fixtures to match tuned dimensions; `buildStaticDateStrip` with `PALETTE_BLUE_50` selected chip; verify three platforms honor `Button` inline `background` hex for true transparency on `secondary` (iOS `SduiButtonStyle` uses inline background override — confirm Android `OutlinedButton` + custom `containerColor`).

- **Open Questions / plan doc:** Non-Goals and Open Questions unchanged; Phase 5 does not resolve the `color.brand.nba` naming vs palette mismatch — flagged above for foreman.

### Phase 6: Testing And Regression Coverage

- [x] Add schema validation fixtures for:
  - story rail
  - editorial overlay rail
  - featured live game hero
  - paged carousel with dots
  - sponsor logo row
  - Discover utility card grid
  - league card rail
  - Games schedule row
  - static date action strip, if selected as atomic _(deferred per Phase 4 — not selected)_
  - headline list card, if added _(deferred per Phase 4 — not added)_
  - full-bleed media overlay card, if added
- [x] Add web tests for:
  - overlay DOM structure and accessible labels
  - scroll page indicator state
  - no dots when omitted
  - text-over-image overlay paths always carry a server-declared scrim layer (assert by fixture: any `OverlayContainer` with text in an overlay layer has a scrim/gradient layer below it) — **JUnit** on `AtomicCompositeBuilder` output; **web** `overlayScrimWireContract.test.ts` for the same contract on static element trees
- [x] Add iOS tests or snapshot-friendly fixtures for model round trip and rendering smoke checks.
- [x] Add Android compile/render smoke checks where current test infrastructure supports it. _(read-only audit; no new Kotlin test file)_
- [x] Add ErrorState fixtures for live modules: featured live game hero with empty/failed live payload, story rail with empty content, schedule list with empty schedule. Each fixture must surface a server-emitted `ErrorState` (or empty state) section, not client-invented fallback copy or default assets (AGENTS.md §8.0). _(AtomicComposite empty-state screens mirroring `buildErrorState` composition; see Phase 6 Outcome)_
- [x] Add anti-pattern greps to the verification checklist:
  - no new semantic section type for feed modules
  - no new semantic section type for Discover utility cards or league rails
  - no `PageableRail` semantic section, primitive, or variant introduced
  - no client-constructed CDN URLs
  - no client-invented fallback copy
  - no raw style/class escape hatch
  - no `outlinedOverMedia` (or similar) button variant introduced for outlined CTAs over imagery

Verification:
- [ ] `make codegen` after schema changes.
- [ ] `node scripts/validate-color-tokens.js`.
- [ ] `node scripts/validate-style-tokens.js`.
- [ ] `npm --prefix web run build`.
- [ ] `./gradlew :app:compileDebugKotlin`.
- [ ] `make ios-build`.

#### Phase 6 Outcome

- **Files changed (Phase 6 only):**
  - `schema/examples/sponsor-logo-row-composite.json` (new — minimal `AtomicComposite` broadcast/sponsor `Image` row; example.com URLs only)
  - `schema/examples/utility-card-grid-composite.json` (new in Phase 6)
  - `schema/examples/league-card-rail-composite.json` (new in Phase 6)
  - `schema/examples/game-schedule-row-composite.json` (new in Phase 6)
  - `schema/examples/error-empty-featured-live-hero.json` (new in Phase 6)
  - `schema/examples/error-empty-story-rail.json` (new in Phase 6)
  - `schema/examples/error-empty-schedule-list.json` (new in Phase 6)
  - `server/src/test/java/com/nba/sdui/service/AtomicCompositeBuilderFeedModulesTest.java` (extended — `assertNoOutlinedOverMediaVariant` on single-card featured hero and broadcast-URL test)
  - `web/src/components/atomic/AtomicScrollContainer.test.tsx` (extended — no dots for single child even with `pageIndicator` + `paging`)
  - `web/src/components/atomic/AtomicOverlayContainer.test.tsx` (extended — `getByLabelText` for `Text` `accessibility.label` under `AtomicText`)
  - `web/src/components/atomic/overlayScrimWireContract.test.ts` (new — static tree walk: text over `Image` must have scrim or solid backing in the same overlay element subtree; mirrors JUnit)
  - `ios/Tests/SduiCoreTests/SduiModelsRoundTripTests.swift` (extended — `sponsor-logo-row-composite` in Phase 6 list; `containsOverlayContainerWithLayers` walks `badge.element`)
  - `ios/Tests/SduiCoreTests/Fixtures/*.json` — copies of the Phase 6 `schema/examples/` screen payloads listed under **Task A** (including the new `sponsor-logo-row-composite.json`)
  - `docs/plans/plan-feed-atomic-composite-parity.md` (this subsection; Phase 6 checkboxes and Task C bullet refined)

- **Phase 5 BLOCKER files not modified (confirmation):** `schema/color-tokens.json`, `schema/sdui-schema.json`, `web/src/utils/ColorTokenResolver.ts`, `ios/Sources/SduiCore/Rendering/ColorTokenResolver.swift`, and Android `ColorTokenResolver.kt` were **not** touched in Phase 6.

- **Task A — Schema validation fixtures**

| Module | Coverage |
|--------|-----------|
| Story rail | Covered by existing `schema/examples/feed-screen-composite.json` |
| Editorial overlay rail | Covered by existing `feed-screen-composite.json` |
| Featured live game hero | Covered by existing `feed-screen-composite.json` |
| Paged carousel + dots | Covered by existing `feed-screen-composite.json` (`featured-paged-carousel`) |
| Sponsor / broadcast logo row | `feed-screen-composite.json` has composed logo/broadcast content on featured cards; **additionally** `schema/examples/sponsor-logo-row-composite.json` (minimal stand-alone `Image` strip) for schema/validation and Android `SchemaRoundTripTest` |
| Full-bleed media overlay card | Covered by existing `feed-screen-composite.json` |
| Discover utility card grid | **New** `schema/examples/utility-card-grid-composite.json` |
| League card rail | **New** `schema/examples/league-card-rail-composite.json` |
| Games schedule row | **New** `schema/examples/game-schedule-row-composite.json` |
| Static date strip / headline list | Omitted per plan (Phase 4 unchecked helpers) |

- **Task B — Server snapshot tests (`AtomicCompositeBuilderFeedModulesTest`)**

  - Each in-scope helper has coverage for: `AtomicComposite` envelope + `data.ui`, no legacy `badge` field, no invented `cdn.nba.com` / `nba.com/` / `https://nba` in output when tests use only `https://example.com` (and similar) media inputs plus `nba://` actions, no `outlinedOverMedia` on `Button` (including single-card `buildFeaturedLiveGameHero` and broadcast-URL path).
  - Overlay helpers (`buildEditorialOverlayRail`, `buildFeaturedLiveGameHero`, `buildMediaOverlayCard`, `buildStoryCircleRail`) assert readable backing for `Text` nodes in `OverlayContainer` overlays over an `Image` base (gradient end-stop `token:color.overlay.scrim` or solid `background` on an ancestor in the same overlay `element` subtree).
  - `buildSectionHeaderComposite`, `buildUtilityCardGrid`, `buildLeagueCardRail`, `buildGameScheduleRow`, `buildGameScheduleList` assert envelope + anti-patterns (no scrim walk where there is no image-overlay copy path).

- **Task C — Web (Vitest)**

  - `AtomicScrollContainer`: dots render when `paging` + multi-child + `pageIndicator.style: "dots"`; no dot row when `pageIndicator` omitted despite `paging: true`; no dots when only one child even if `paging` + `pageIndicator` (matches `hasPageIndicator` guard).
  - `AtomicOverlayContainer`: render asserting multiple `position: absolute` layers; `Text` with `accessibility.label` is exposed via `AtomicText` / `getByLabelText` (per-overlay `div` wrappers are still inert; NICE: labels on each overlay hit-target if product requires it).
  - `overlayScrimWireContract.test.ts`: static `OverlayContainer` + `Image` + `Text` shape checks (contrast path within the same overlay `element` tree), aligned with JUnit `assertOverlayImageStacksHaveReadableTextBacking` (sibling scrim+text in separate `overlays[]` entries are not in that tree — server helpers nest copy under scrim in one layer, as in the tests).

- **Task D — iOS**

  - `testPhase6SchemaExampleScreensDecode()` loads the Phase 6 screen fixtures (including `feed-screen-composite`, `sponsor-logo-row-composite`, three error-empty screens, and utility/league/schedule row composites), asserts `AtomicComposite` + non-nil `data.ui`, and asserts overlay-bearing coverage on `feed-screen-composite` via `containsOverlayContainerWithLayers` (helper also descends `badge.element`).

- **Task E — Android (audit)**

  - `android/sdui-core/src/test/java/com/nba/sdui/core/SchemaRoundTripTest.kt` already round-trips **every** `schema/examples/*.json` screen payload (including new Phase 6 examples) through `SduiModels`; no dedicated `OverlayContainer`/`ScrollContainer` unit tests exist, and Phase 6 did **not** add a new Kotlin file per plan rules.

- **Task F — Error / empty-state fixtures**

  - `buildErrorState` in the server still emits **`AtomicComposite`** (not a distinct `ErrorState` section `type` on the wire). Phase 6 added three **screen** fixtures that mirror that composition pattern with **server-authored** titles, body copy, emojis, and `Try Again` `navigate` actions — no client fallback copy.
  - **NICE:** Have feed composers emit these (or a shared empty-state helper) for empty live hero / empty story payload / empty schedule list.

- **Task G — Anti-pattern `rg` results (read-only; run from repo root)**

  | Check | Result |
  |-------|--------|
  | `rg 'type":\s*"PageableRail"' schema/ server/src` | **No matches** |
  | `rg 'type":\s*"(FeedScreen\|StoryCircleRail\|EditorialOverlayRail\|FeaturedLiveGameHero)"' schema/ server/src` | **No matches** (helper names appear only in Java method names / demo labels, not as JSON section `type`) |
  | `rg 'outlinedOverMedia' schema/ server/src/main web/src ios android` | **No matches** in `schema/`, `server/src/main/`, `web/src/`, `ios/`, `android/` — **excluded:** `server/src/test/` (JUnit `assert` message text), `docs/plans/` (this plan) |
  | `rg 'https://' server/src/main/java/com/nba/sdui/service/AtomicCompositeBuilder.java` | **1 match:** `DEFAULT_PLACEHOLDER` constant (`https://cdn.nba.com/...`) used by the **4-arg** `image(...)` overload; in-scope helpers in Phase 6 tests use the **5-arg** `image(..., null)` path so tests do not emit that placeholder |
  | `rg 'className\|styleName\|rawStyle' schema/sdui-schema.json` | **No matches** |

- **BLOCKERS for foreman**

  - **None** uncovered by Phase 6 test authoring (no wire shape regressions requiring schema/token edits in this phase).

- **NICE follow-ups**

  - Web: pipe or test `accessibility.label` on per-overlay layers if product requires it.
  - Server: consider replacing or gating `DEFAULT_PLACEHOLDER` if product forbids any hardcoded CDN default in the codebase (even when not emitted by feed helpers).
  - Composers: wire empty-state / ErrorState-shaped `AtomicComposite` for empty live hero, story rail, and schedule list using the new fixtures as reference.
  - Duplicate scrim/assertion coverage on web via fixture-driven test if the team wants client-side parity with the JUnit tree walk.

## Dependencies

- Schema/codegen pipeline must be used before any new atomic type or `ScrollContainer` field is emitted by the server. Current planned work should not require this because `OverlayContainer` and `pageIndicator` already exist.
- Overlay support depends on client atomic routers; current renderers support `OverlayContainer` on web/iOS/Android.
- Page dots depend on local client scroll state, which is allowed as runtime presentation state but must be triggered by server-declared `pageIndicator`.
- Real video playback and ad SDK integrations remain outside this plan.

## Open Questions

- [x] Should the overlay primitive be a new `OverlayContainer` atomic type, or should `Container` gain an explicit overlay layout mode? Decision: use `OverlayContainer`.
- [x] Should `pageIndicator` live on `ScrollContainer`, or should it be a sibling primitive bound to scroll state by ID? Decision: `pageIndicator` lives on `ScrollContainer`.
- [x] Do we need first-class semantic tokens for yellow/gold CTA, selected-date blue, and live ring colors, or are existing palette/brand aliases sufficient? Decision (provisional, this iteration): reuse `color.brand.nba` for yellow/gold accents, `color.brand.live` for live red, the existing primary blue palette alias for selected dates, and `OVERLAY_SCRIM` for scrims. Promote to dedicated stable accent tokens only if a real styling pass shows fragmentation, per AGENTS.md §11.4.
- [x] Should page dots be supported only for paged carousels, or also for normal scroll rails? Decision: paged carousels only. Continuous rails (story circle, editorial overlay, league, schedule list, utility grid, sponsor strip, static date strip) must not declare `pageIndicator`. See Phase 3 negative list.
- [x] Should we introduce a `PageableRail` primitive, semantic section, or `ScrollContainer` variant for paged hero/media carousels? Decision: no. The four `ScrollContainer` fields (`direction`, `paging`, `snapAlignment`, `pageIndicator`) are the contract. Server helpers route paged-carousel composition through a single shared internal helper instead of new wire-level vocabulary.
- [ ] Should the feed demo live as a Kitchen Sink section group, a new demo screen variant, or a `for-you` experiment option?
- [x] Should the Games date selector be modeled as atomic action chips, `TabGroup`, or a justified permanent date-picker section? Decision (Phase 1 outcome): atomic action chips with server-owned selection, or `TabGroup` if the strip swaps section lists. `DatePickerStrip` semantic section is reserved as a conditional exception only if product later requires native calendar UX or client-local date state that cannot be round-tripped. See **Phase 1 Outcome: Games Date Selector Classification**.
- [ ] Should overflow menu actions be added as server-declared action groups, or stay as existing app-shell behavior where applicable?
- [ ] Should utility card icons use server-provided image URLs, an icon token registry, or both?
- [ ] Should headline list cards and full-bleed media CTA cards become named server helper patterns, or remain one-off demo/feed compositions until repeated?

## Completion Criteria

- The feed modules visible in the real NBA app screenshots that fit atomic precedence can be represented as server-emitted `AtomicComposite` trees.
- Any module that does not fit atomic precedence is explicitly classified as app shell, reserved SDK section, `TabGroup`, `AdSlot`, `VideoPlayer`, or a separately justified semantic section.
- New capabilities, if any are still required after the styling pass, are schema-first, codegen-backed, and implemented on web/iOS/Android.
- Global app chrome, ads, and video SDK hosts remain outside atomic content.
- Clients remain presentation-only: no invented assets, copy, routes, or business policy.
- All verification commands pass.
