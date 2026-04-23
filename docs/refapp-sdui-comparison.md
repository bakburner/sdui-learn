# Video SDK Reference Apps × SDUI Comparison

**Cross-platform analysis of the iOS and Android video SDK reference apps against the SDUI prototype's capabilities, identifying replacement opportunities, gaps, and shared server-side consolidation.**

---

## 1. How the Ref Apps Differ

The iOS and Android video SDK reference apps serve the same purpose — demonstrate NBA Video SDK integration and CIAM authentication — but take fundamentally different architectural approaches.

### Structural Comparison

| Dimension | iOS Ref App (SwiftUI) | Android Ref App (Compose) |
|---|---|---|
| **App pattern** | Single-entry `@main` SwiftUI App, `NavigationStack` | Single Activity + NavHost + separate `GamePlayerActivity` |
| **Home experience** | Visual content feed (game carousels, VOD rails, section headers) | Developer button dashboard (4 hardcoded user profiles) |
| **Content discovery** | CMS API integration (NBA Core API) → game cards, VOD thumbnails, featured feeds | SDK schedule API → hierarchical date drill-down (Season → Year → Month → Day → Games) |
| **Visual richness** | Team logos, scores, live badges, animations, gradient backgrounds, image thumbnails | Plain text + Material buttons, no images, no visual cards |
| **Video player** | `NBAVideoKit` singleton, custom player chrome, rotation/fullscreen | 5 SDK Composables (`GamePlayerWithStreamSelector`, `VodPlayerWithStreamSelector`, `EventPlayerWithStreamSelector`, `NbaTvPlayer`, `VideoPlayer`) |
| **Player features** | Fullscreen via rotation, couch rights half-sheet | PiP, Chromecast, fullscreen, custom action bars, couch rights bottom sheet |
| **Auth** | Local Swift Package (`LiteweightCIAMService`) | Retrofit `CiamApi` + Hilt DI + SharedPreferences cache |
| **DI** | None (manual init) | Hilt (`@HiltViewModel`, `@Inject`, `@Module`) |
| **Data persistence** | None (logout on close) | SharedPreferences (cached user per email/environment) |
| **Navigation** | Typed `NavigationStack` destinations | String-based NavHost routes + Intent-based Activity launch |
| **Real-time data** | Timer-based client-side clock tick on live game cards | None |
| **Test data** | Hardcoded game/event IDs in config sheet, API keys in source | Hardcoded game/event IDs in buttons, hardcoded season list, hardcoded user location |

### Key Observation

The iOS ref app is closer to a **consumer product** — it has a visual feed, content carousels, and image-rich game cards. The Android ref app is closer to a **developer test harness** — it exposes SDK APIs through a button-based UI with raw data display. This means SDUI replaces different things on each platform:

- **iOS:** SDUI replaces the content discovery and feed composition layer (~1,200 LOC)
- **Android:** SDUI replaces the hardcoded button/navigation layer and schedule browser (~800 LOC)

Both share the same video player boundary — SDK integration stays native on both platforms.

---

## 2. File-by-File Replacement Analysis

### 2a. iOS Ref App

#### Fully Replaceable by SDUI (AtomicComposite + Semantic Sections)

These files are replaced by SDUI's server-composed screens. The client needs only the generic SDUI runtime (SectionRouter, AtomicRouter, ~10 atomic renderers) instead of these purpose-built components.

| File | LOC | What It Does | SDUI Replacement | How |
|---|---|---|---|---|
| `FeedView.swift` | 40 | Routes modules to carousel/list views by type | `SduiScreenView` + `SectionRouter` | Server decides section types and ordering; client renders in order |
| `FeedViewModel.swift` | 50 | Fetches from CMS, combines game + featured feeds | `SduiRepository.fetchScreen()` | Single generic fetch replaces dual-source architecture |
| `GameCarouselView.swift` | 40 | Horizontal scrolling game cards | AtomicComposite `ScrollContainer` (horizontal) | Server composes scroll container with game card children |
| `GameListView.swift` | 40 | Vertical list of game cards | AtomicComposite `Container` (vertical) | Server composes column of game items |
| `VODCarouselView.swift` | 100 | Horizontal VOD thumbnails with duration/LIVE badges | AtomicComposite `ScrollContainer` with video card children | Server composes VideoCarousel pattern |
| `VODCardView.swift` | 80 | VOD row: thumbnail, title, duration, LIVE badge | AtomicComposite content card elements | Server provides Image + Text + badge layout |
| `VODPlaylistView.swift` | 50 | Vertical VOD list with dividers | AtomicComposite `Container` + `Divider` | Server composes list with dividers |
| `RefAppFeed.swift` | 80 | Feed/Module/Card data models | Codegen `SduiModels` | Generated from schema, not hand-written |
| `RefAppGameCard.swift` | 60 | Game card model + tricode→teamId mapping (30 teams) | Codegen `GamePanelData` + `TeamData` | Server provides `teamLogoUrl` directly; mapping eliminated |
| `RefAppVODCard.swift` | 20 | VOD card model | Codegen models | Generated |
| `CMSService.swift` | 20 | CMS protocol definition | `SduiRepository` protocol | One generic fetch method |
| `LiveCMSService.swift` | 80 | NBA Core API client (hardcoded API keys, endpoints) | `SduiRepository` | Keys and endpoints move server-side |
| `MockCMSService.swift` | 30 | Bundled JSON fallback | Server mock mode or bundled SDUI response | Server owns data; offline via HTTP cache |
| `CoreAPIModels.swift` | 100 | Core API DTOs + conversion to app models | Codegen models | Entire conversion pipeline eliminated |
| `FeaturedFeedModels.swift` | 100 | Featured feed DTOs + conversion | Codegen models | Eliminated |
| **Subtotal** | **890** | | | |

#### Mostly Replaceable (Thin Native Shell Remains)

| File | LOC | What It Does | What SDUI Replaces | What Stays Native |
|---|---|---|---|---|
| `HomeView.swift` | 120 | Screen container: loading/error states, toolbar, navigation routing | Loading/error/content body (~80 LOC) → `SduiScreenView` | Toolbar buttons (profile, settings, video config sheets) remain as app-shell chrome (~40 LOC) |
| `GameCardView.swift` | 200 | Game card: team logos, tricodes, scores, status banner, live clock timer, pulse animation | Card layout → `GamePanel` semantic section (~120 LOC of layout). Server provides displayConfig, team logos, scores. | Clock tick timer (~40 LOC), pulse animation (~15 LOC), content transition animation → client rendering within GamePanel renderer |
| **Subtotal** | **320** | **~200 LOC replaced, ~120 LOC stays** | | |

#### Stays Native (SDK / Security / Dev Tooling)

| File | LOC | Reason It Stays |
|---|---|---|
| `VideoView.swift` | 150 | Platform SDK integration (NBAVideoKit player lifecycle, fullscreen, rotation) |
| `VideoViewModel.swift` | 80 | SDK event handler protocol (`NBAVideoKitEventHandler`) |
| `StreamsListView.swift` | 80 | SDK data source (`nbaVideoKit.availableStreams()`) |
| `StreamsListRow.swift` | 30 | SDK model types (`NBAStream`) |
| `CouchRightsSheet.swift` | 80 | SDK callback trigger (`handleStreamSelectionCouchRights`) — display could be AtomicComposite but trigger is native |
| `VideoConfigSheet.swift` | 120 | Dev tooling (manual media ID entry, experience picker) |
| `SettingsView.swift` | 100 | Dev tooling (environment/data source toggle) |
| `ProfileView.swift` | 100 | Auth boundary (JWT display, login/logout) |
| `LoginFormView.swift` | 60 | Security boundary (credential entry) |
| `LoginViewModel.swift` | 120 | Auth state management + SDK JWT sync |
| `CustomizedVideoCTAExample.swift` | 30 | SDK CTA customization |
| `RefAppComponents.swift` | 100 | Reusable UI components (used by settings/profile — stays) |
| Helpers (3 files) | 60 | Rotation, lazy view, extensions |
| `NBAVideoKit_ReferenceApp.swift` | 30 | App entry + SDK init |
| `AppDelegate.swift` | 30 | Orientation lock |
| **Subtotal** | **1,250** | |

#### iOS Summary

| Category | LOC | % of App |
|---|---|---|
| **Fully replaced by SDUI** | 890 | 36% |
| **Mostly replaced** (thin shell remains) | 200 of 320 | 8% |
| **Stays native** | 1,250 | 51% |
| **Client rendering in semantic sections** | 120 | 5% |
| **Total app** | ~2,460 | 100% |

**Net: ~1,090 LOC of purpose-built iOS code replaced by ~850 LOC of generic SDUI runtime** (SectionRouter, AtomicRouter, 10 atomic renderers, repository, action dispatcher) that also serves every future SDUI screen with zero additional client code.

---

### 2b. Android Ref App

#### Fully Replaceable by SDUI

| File | LOC | What It Does | SDUI Replacement | How |
|---|---|---|---|---|
| `UserInfoScreen.kt` (button section) | 250 | 8 hardcoded buttons for game/VOD/event/NBA TV with hardcoded IDs | AtomicComposite `Container` with `Button` elements + `navigate` actions | Server composes button list dynamically; IDs come from server; add/remove options without app release |
| `ScheduleScreen.kt` | 400 | 4-level date drill-down (Season → Year → Month → Day → Games) with LazyRow pill selectors | `Form` (season picker) + AtomicComposite (pill rows) + parameterized `refresh` | Server provides picker options; selection triggers refresh with `paramBindings`; server recomposes with filtered data |
| `ScheduleViewModel.kt` | 30 | Calls `GameRepository.getGamesForSeason/Date()` | `SduiRepository.fetchScreen()` | Single generic fetch |
| `MainScreenViewModel.kt` | 20 | Holds user state | Eliminated — server manages session | |
| Models: `AuthRequest.kt`, `AuthResponse.kt`, `RefreshTokenRequest.kt`, `RefreshTokenResponse.kt`, `StreamInfoState.kt` | 80 | Network DTOs | Codegen `SduiModels` (non-auth models) | Generated from schema |
| **Subtotal** | **780** | | | |

#### Conditionally Replaceable (Depends on Server Stream Access)

| File | LOC | What It Does | Condition | If Server Can Access Streams | If Not |
|---|---|---|---|---|---|
| `StreamInfoScreen.kt` | 350 | Displays stream groups from SDK, each row clickable → launches player | Can the SDUI server call the same stream resolution APIs that the SDK calls client-side? | AtomicComposite list of stream cards with `navigate` actions | Stays native — data source is SDK |
| `StreamInfoViewModel.kt` | 80 | Calls `StreamRepository.getStreamInfoForGame/Vod/NbaTv()` | Same | `SduiRepository` | Stays native |
| `GameInfoTopAppBar.kt` | 30 | App bar for stream info | Same | AtomicComposite section | Stays native |
| **Subtotal** | **460** | | | **460 replaced** | **460 stays** |

#### Stays Native (SDK / Security / Dev Tooling)

| File | LOC | Reason It Stays |
|---|---|---|
| `GamePlayerScreen.kt` | 250 | SDK Composables (`GamePlayerWithStreamSelector`, `VodPlayerWithStreamSelector`), highlights list, custom action bars, entitlement gating |
| `GamePlayerViewModel.kt` | 20 | SDK call (`getHighlightsForGame`) |
| `VideoPlayerActivity.kt` | 100 | 4 player modes via Intent dispatch (Game, Stream, NbaTv, Event) |
| `StreamPlayerScreen.kt` | 100 | `VideoPlayer` Composable, PiP, Chromecast, fullscreen config |
| `NbaTvPlayerScreen.kt` | 30 | `NbaTvPlayer` SDK wrapper |
| `EventPlayerScreen.kt` | 80 | `EventPlayerWithStreamSelector` SDK wrapper |
| `VideoBottomSheet.kt` | 8 | Entitlement state enum (sealed class) |
| `VideoPlayerBottomSheet.kt` | 50 | ModalBottomSheet for 3 entitlement states |
| `CiamApi.kt` | 20 | Retrofit auth interface |
| `UserRepository.kt` | 60 | Login, token refresh, SharedPreferences cache |
| `UserInfoScreen.kt` (display + auth) | 100 | User info display + refresh token button |
| `UserInfoViewModel.kt` | 50 | Login/cache logic |
| `DefaultUsers.kt` | 30 | Hardcoded test user credentials |
| `MainScreen.kt` | 50 | Dev user selection buttons |
| `MainActivity.kt` | 30 | Activity + NavHost setup |
| `ReferenceApplication.kt` | 10 | Hilt app class |
| `ReferenceAppModule.kt` | 40 | Hilt DI module |
| `JsonWrapper.kt` | 20 | Moshi JSON helpers |
| Theme files (3) | 60 | Colors, typography, theme |
| `User.kt` | 20 | User model |
| **Subtotal** | **1,208** | |

#### Android Summary

| Category | LOC | % of App |
|---|---|---|
| **Fully replaced by SDUI** | 780 | 32% |
| **Conditionally replaced** (if server has stream access) | 460 | 19% |
| **Stays native** | 1,208 | 49% |
| **Total app** | ~2,448 | 100% |

**Net: ~780 LOC (minimum) to ~1,240 LOC (if streams move server-side) of purpose-built Android code replaced.** Unlike iOS, the Android ref app already uses Compose — the SDUI prototype's existing `sdui-core` library (AtomicRouter + 10 atomic renderers + 9 section renderers) can be integrated directly as a module dependency.

---

## 3. Cross-Platform Replacement Impact

### What Gets Eliminated on BOTH Platforms

| Eliminated Pattern | iOS Instance | Android Instance | Shared Server Replacement |
|---|---|---|---|
| **Hardcoded content IDs** | Game/event IDs in `VideoConfigSheet` | Game IDs (`0022301146`), event IDs (`0092490074`, `0022390033`) in `UserInfoScreen` | Server provides IDs in `navigate` action `targetUri` |
| **Hardcoded API keys** | `LiveCMSService` (Azure APIM keys in source) | N/A (uses SDK) | Server owns upstream API keys |
| **Hardcoded season/option lists** | N/A | `["2021-22", "2022-23", "2023-24", "2024-25"]` in `ScheduleScreen` | Server provides `Form` picker options dynamically |
| **Hardcoded user location** | `"90011"` postal, `"US"` country in app init | `"90001"` postal, `"US"` country in `UserStreamParameters` | Server reads from `device` in request envelope |
| **Client-side URL construction** | `tricodeToTeamId` map (30 teams) → CDN URL pattern | N/A (no images) | Server provides `teamLogoUrl` directly |
| **Content type routing** | `FeedView` routes by `moduleType × cardData` | `StreamInfoScreen` routes by `type` (game/vod/nbaTv) | Server decides section types; `SectionRouter` dispatches |
| **Dual data source toggle** | `LiveCMSService` vs `MockCMSService` | N/A | Single `SduiRepository.fetchScreen()` — server owns data pipeline |
| **Model conversion pipelines** | CoreAPI → FeaturedFeed → RefAppCard (3 layers) | SDK types → UI display (1 layer) | Codegen models from schema — zero conversion |

### What the Server Gains (Shared Logic, Write-Once)

With SDUI, these capabilities are implemented **once on the server** and serve both platforms:

| Server Capability | Replaces iOS Logic | Replaces Android Logic | Net Benefit |
|---|---|---|---|
| **Game feed composition** | `FeedViewModel` + `LiveCMSService` + `CoreAPIModels` + `FeaturedFeedModels` (~330 LOC) | N/A (Android has no feed) | Eliminates iOS-only feed pipeline; adds feed to Android for free |
| **Schedule composition** | N/A (iOS has no schedule) | `ScheduleScreen` + `ScheduleViewModel` (~430 LOC) | Eliminates Android-only schedule logic; adds schedule to iOS for free |
| **Video content rails** | `VODCarouselView` + `VODCardView` + `VODPlaylistView` (~230 LOC) | N/A (Android has no VOD UI) | Eliminates iOS-only VOD views; adds video browsing to Android for free |
| **Game card layout** | `GameCardView` layout portion (~120 LOC) | N/A (Android shows raw text) | Rich game cards on both platforms from one server template |
| **Section ordering / A-B testing** | Not possible (hardcoded module routing) | Not possible (hardcoded button list) | Server controls section order, can run variants (A/B/C/D) |
| **Error states** | Custom error view in `HomeView` (~20 LOC) | Inline error text (~10 LOC per screen) | Consistent `ErrorState` AtomicComposite across platforms |
| **Navigation targets** | Hardcoded game state mapping in `HomeView` | Hardcoded route construction in `NavController` extensions | Server provides `navigate` action URIs |

**The biggest cross-platform win: features that exist on one platform but not the other become available on both for free.** The iOS ref app has rich game cards and VOD carousels that the Android ref app lacks. The Android ref app has a schedule browser that the iOS ref app lacks. With SDUI, one server composer produces both — no platform-specific implementation needed.

---

## 4. SDUI Prototype Gaps

These are capabilities that one or both ref apps demonstrate that the SDUI prototype cannot currently express.

### Gap 1: VideoPlayer Semantic Section Type

**Severity: High — confirmed by both platforms**

| | iOS | Android |
|---|---|---|
| **SDK** | NBAVideoKit (Bitmovin-backed) | NBA Video SDK (ExoPlayer-backed) |
| **Player variants** | 1 (`VideoView` wrapper) | 5 (`GamePlayerWithStreamSelector`, `VodPlayerWithStreamSelector`, `EventPlayerWithStreamSelector`, `NbaTvPlayer`, `VideoPlayer`) |
| **PiP** | Not in ref app (iOS handles via AVPlayer) | Yes (`supportsPip`, `shouldEnterPip`) |
| **Chromecast** | No | Yes (`supportsChromecast`) |
| **Fullscreen** | Rotation-based | `enterFullScreenOnRotation`, `forceLandscapeOnFullScreen` |
| **Custom chrome** | Custom CTA (`ConfigurablePlayerCTA`) | Custom action bar (stream selector controller Composable) |

**What's missing:** No `VideoPlayer` section type in the schema. SDUI can compose content discovery (carousels, rails, cards) that navigates to video via `nba://video/{id}`, but cannot embed a video player inline within a server-composed screen. A game detail screen cannot have GamePanel + VideoPlayer + BoxscoreTable all in one SDUI response.

**Proposed section:** `VideoPlayer` with server-provided `displayConfig`:
- `playerType`: `game` | `vod` | `event` | `nbaTv` | `stream`
- `contentId`: single content identifier — interpreted by `playerType` (gameId for game, mediaId for vod, eventId for event, streamUrl for stream)
- `capabilities`: `["pip", "chromecast", "airplay", "backgroundAudio", "fullscreenRotation"]` — extensible string-enum array, not per-capability booleans. Server includes only capabilities relevant to the requesting platform.
- `autoplay`: Boolean
- `displayConfig.height`, `displayConfig.aspectRatio`: layout hints

**Justification per §2b decision framework:** Platform SDK integration (video SDK owns player lifecycle, DRM, HLS, ad insertion). Same category as SubscribeHero (StoreKit/Play Billing) and AdSlot (GAM).

### Gap 2: Overlay / Modal Composition

**Severity: Medium — confirmed by both platforms**

| | iOS | Android |
|---|---|---|
| **Pattern** | `CouchRightsSheet` — half-sheet with image, text, button | `VideoBottomSheet` — `ModalBottomSheet` with 3 states |
| **States** | `.enabled(remaining)`, `.expired`, `.disabled` | `CouchRightsWarning(timeLeft)`, `CouchRightsExpired`, `Unentitled` |
| **Trigger** | SDK callback (`handleStreamSelectionCouchRights`) | SDK callback (`onStreamSelected` → check `isEntitled` + `couchRightsTimeLeftMs`) |

**What's missing:** No pattern for pre-composed modal/overlay content that the client shows when a condition arises. The display content (text + button) is stateless and server-composable; the trigger is SDK-native.

**Proposed addition:** Optional `overlays` map on `Screen`:
```json
{
  "overlays": {
    "couchRightsWarning": { "type": "AtomicComposite", "data": { "ui": { ... } } },
    "couchRightsExpired": { "type": "AtomicComposite", "data": { "ui": { ... } } },
    "unentitled": { "type": "AtomicComposite", "data": { "ui": { ... } } }
  }
}
```
Client shows the overlay keyed by state name when the SDK triggers. Server controls the display content; client controls when to show it.

### Gap 3: Cross-Platform Icon Vocabulary

**Severity: Low — confirmed by both platforms**

| iOS Uses | Android Uses | SDUI Has |
|---|---|---|
| SF Symbols: `play.fill`, `antenna.radiowaves.left.and.right`, `person.circle`, `gearshape`, `chevron.right`, `exclamationmark.triangle` | Material Icons: `Icons.AutoMirrored.Filled.ArrowBack`, `Icons.Default.Check`, `Icons.Default.KeyboardArrowDown/Up` | `icon` field on `AtomicElement` and `NavigationItem` — but no defined cross-platform vocabulary |

**Proposed addition:** A shared icon token vocabulary (`sdui:play`, `sdui:back`, `sdui:settings`, `sdui:expand`, `sdui:check`, `sdui:warning`, `sdui:live`) with per-platform mapping files. Each client maps `sdui:play` → SF Symbol `play.fill` (iOS) or Material Icon `PlayArrow` (Android) or SVG (web).

### Gap 4: Client-Side Clock Interpolation

**Severity: Low — iOS only**

The iOS ref app's `GameCardView` has ~40 LOC of timer logic: parses "Q3 4:22" into seconds, ticks a `Timer.publish(every: 1)`, and animates `contentTransition(.numericText())`. Provides visual continuity between SSE updates (1-3 second intervals).

The Android ref app has no equivalent (no live game cards). The SDUI prototype's Android `GamePanelRenderer` does not implement this either.

**Recommendation:** Document as a **client rendering guideline** for the `GamePanel` renderer, not a schema change. The schema already has `gameClock` on `GamePanelData`. Add `clockRunning: Boolean` if not present. Renderer ticks locally when `clockRunning == true` between SSE updates. This is "client owns rendering quality" per SDUI philosophy.

### Gap 5: Animation / Transition Rendering Guidelines

**Severity: Low — iOS only**

The iOS ref app uses:
- Pulsing red dot (`.opacity` + `.repeatForever`) for live indicator
- `.contentTransition(.numericText())` for score changes
- `.snappy(duration: 0.3)` for clock tick animation

The Android ref app has no animations beyond Material defaults.

**Recommendation:** Publish a **rendering guideline document** (not schema changes) specifying expected platform-appropriate animations for live data transitions. "When a bound numeric value changes, apply platform-appropriate content transition. When a live indicator is present, apply a pulse animation." Ensures platform parity without server control of animations.

### Gap 6: Player-Adjacent Content (Highlights List)

**Severity: Low — Android only**

The Android `GamePlayerScreen` shows a video player at the top with a highlight list below. Tapping a highlight switches the player to VOD mode without navigating away.

SDUI can express this via `VideoPlayer` section (Gap 1) + AtomicComposite (highlight list) + `mutate` action (update `selectedHighlight` in screen state) + `Conditional` element (render appropriate player variant). But it requires Gap 1 to be resolved first.

---

## 5. What the SDUI Prototype Already Has (Android Advantage)

The SDUI prototype includes a **complete Android rendering library** (`sdui-core`) that the Android ref app can import directly:

| Component | Status | What It Provides |
|---|---|---|
| `SectionRouter.kt` | Built | Routes 10 section types to renderers (9 permanent + `AtomicComposite`) |
| `AtomicRouter.kt` | Built | Dispatches 10 atomic element types |
| `AtomicContainer.kt` | Built | HStack/VStack flex layout with responsive breakpoints |
| `AtomicText.kt` | Built | Typography variant + weight + color mapping |
| `AtomicImage.kt` | Built | Coil `AsyncImage` with fallback |
| `AtomicButton.kt` | Built | Primary/secondary/text variants + action dispatch |
| `AtomicSpacer.kt` | Built | Fixed-size spacer |
| `AtomicDivider.kt` | Built | Horizontal/vertical separator |
| `AtomicScrollContainer.kt` | Built | LazyRow/LazyColumn + HorizontalPager |
| `AtomicConditional.kt` | Built | State-driven branching |
| `AtomicDisplayGrid.kt` | Built | Display-only grid with striped rows |
| `AtomicSectionSlot.kt` | Built | Bridge back to SectionRouter |
| `GamePanelRenderer.kt` | Built | Live game card with SSE + displayConfig |
| `BoxscoreTableRenderer.kt` | Built | Sortable stats table with frozen columns |
| `TabGroupRenderer.kt` | Built | Tab selection + dynamic content |
| `FormRenderer.kt` | Built | Picker/segmented/toggle fields + parameterized refresh |
| `SeasonLeadersTableRenderer.kt` | Built | Sortable leaders table |
| `SubscribeHeroRenderer.kt` | Built | Subscription upsell hero |
| `SubscribeBannerRenderer.kt` | Built | Inline subscription banner |
| `AdSlotRenderer.kt` | Built | GAM ad placement |

**For Android, the SDUI integration is a library import**, not a from-scratch build. The ref app adds `sdui-core` as a dependency, wires up `SduiRepository`, and replaces hardcoded screens with `SduiScreenView`.

**For iOS, the rendering library must be built from scratch** using the codegen Swift models (`SduiModels.swift`, 2,966 lines, already generated). The Android renderers serve as the reference implementation — same algorithms, same routing logic, same failure semantics, translated to SwiftUI.

---

## 6. Consolidated Impact

### LOC Replaced

| Platform | Fully Replaced | Mostly Replaced | Total Eliminated | SDUI Runtime Added | Net Change |
|---|---|---|---|---|---|
| **iOS** | 890 | 200 | 1,090 | ~850 (new SDUI runtime) | −240 LOC + generic runtime that serves all future screens |
| **Android** | 780–1,240 | 0 | 780–1,240 | ~50 (integration glue — library already exists) | −730 to −1,190 LOC |
| **Server** | N/A | N/A | N/A | ~200 (new composers for schedule, stream info if applicable) | +200 LOC that serves both platforms |

### What Changes

| Before SDUI | After SDUI |
|---|---|
| iOS has game cards, Android doesn't | Both have game cards — server composes `GamePanel` |
| Android has schedule browser, iOS doesn't | Both have schedule — server composes `Form` + game list |
| iOS has VOD carousels, Android doesn't | Both have VOD rails — server composes `VideoCarousel` AtomicComposite |
| Adding a new section type to iOS = iOS code change | Adding a new AtomicComposite section = server change only |
| Adding a new section type to Android = Android code change | Same — server only for AtomicComposites |
| Updating test game IDs = app release on both platforms | Updating IDs = server config change, zero app releases |
| Content ordering changes = code change per platform | Content ordering = server composition change |
| A/B testing layout variants = not possible | A/B variants = server-driven (already built: variants A–F) |
| API keys in client source code | API keys in server only |
| 30-team tricode→ID mapping in iOS source | Server provides `teamLogoUrl` directly |

### Cross-Platform Feature Parity

| Feature | iOS Today | Android Today | With SDUI |
|---|---|---|---|
| Game feed with visual cards | Yes | No | **Both** — GamePanel + AtomicComposite |
| VOD/content carousels | Yes | No | **Both** — VideoCarousel AtomicComposite |
| Schedule browser | No | Yes | **Both** — Form + parameterized refresh |
| Live score updates (SSE) | Client-side timer only | None | **Both** — GamePanel with Ably SSE |
| Entitlement error display | Half-sheet (hardcoded) | Bottom sheet (hardcoded) | **Both** — Server-composed overlay (Gap 2) |
| Promo banners | No | No | **Both** — PromoBanner AtomicComposite |
| Ad slots | No | No | **Both** — AdSlot semantic section |
| Subscription upsells | No | No | **Both** — SubscribeHero/SubscribeBanner |
| Content rails (articles + videos) | No | No | **Both** — ContentRail AtomicComposite |

---

## 7. Gap Priority Matrix

| Gap | Severity | Platforms | Effort | Impact | Priority |
|---|---|---|---|---|---|
| **VideoPlayer section** | High | Both | Medium (schema + 2 platform renderers) | Enables video-inline SDUI screens; closes the "navigate away to play" limitation | **P0** |
| **Overlay composition** | Medium | Both | Low (schema addition + client overlay host) | Consistent entitlement/restriction UX from server | **P1** |
| **Icon vocabulary** | Low | Both | Low (mapping file + client resolver) | Enables server-composed navigation + button icons | **P2** |
| **Clock interpolation guideline** | Low | iOS only | Minimal (documentation) | Visual polish for live games on iOS | **P3** |
| **Animation guidelines** | Low | iOS only | Minimal (documentation) | Platform consistency for live data transitions | **P3** |
| **Player-adjacent content** | Low | Android only | Depends on Gap 1 | Highlights list + inline player switching | **P3** |

---

## 8. SwiftUI ↔ Jetpack Compose: Why the iOS SDUI Runtime Is a Direct Translation

Both SwiftUI and Jetpack Compose are declarative UI frameworks with nearly identical layout models. The SDUI prototype's existing Android Compose renderers translate to SwiftUI with minimal structural change — same logic, different syntax.

### Atomic Element Mapping

| SDUI Atomic Element | Android Compose (built in `sdui-core`) | SwiftUI Equivalent | Notes |
|---|---|---|---|
| **Container** (row) | `Row(horizontalArrangement, verticalAlignment) { children }` | `HStack(alignment:, spacing:) { children }` | 1:1 |
| **Container** (column) | `Column(verticalArrangement, horizontalAlignment) { children }` | `VStack(alignment:, spacing:) { children }` | 1:1 |
| **Flex weight** | `Modifier.weight(flex)` inside Row/Column scope | `.frame(maxWidth: .infinity)` or `Layout` protocol (iOS 16+) | SwiftUI lacks direct `weight()`; `.frame(maxWidth: .infinity)` handles equal distribution; `Layout` protocol needed for true proportional splits |
| **Text** | `Text(content, style = mapVariant(), color, maxLines, overflow = Ellipsis)` | `Text(content).font(mapVariant()).foregroundColor(color).lineLimit(maxLines).truncationMode(.tail)` | 1:1 — variant mapping adapts to platform type ramp |
| **Image** | `AsyncImage(model, contentScale)` via Coil | `AsyncImage(url:) { phase in ... }` (native iOS 15+) | Both support async loading; fallback pattern identical (swap URL on error) |
| **Button** | `Button(onClick) { Text(label) }` / `OutlinedButton` / `TextButton` | `Button(label) { action }.buttonStyle(...)` | Variant mapped via `.buttonStyle(.bordered)`, `.borderless`, custom styles |
| **Spacer** | `Spacer(Modifier.size(dp))` / `.width()` / `.height()` | `Spacer().frame(width:, height:)` | 1:1 |
| **Divider** | `HorizontalDivider(thickness, color)` / `VerticalDivider` | `Divider()` + `.frame(height:)` for thickness, `.overlay()` for color | SwiftUI `Divider()` is horizontal-only; vertical needs `Rectangle().frame(width:)` |
| **ScrollContainer** | `LazyRow` / `LazyColumn` / `HorizontalPager` | `ScrollView(.horizontal) { LazyHStack }` / `LazyVStack` / `TabView(.page)` | Paging: Compose `HorizontalPager` → SwiftUI `TabView` with `.tabViewStyle(.page)` |
| **Conditional** | `if (evaluateCondition()) { trueChild } else { falseChild }` | `if evaluateCondition() { trueChild } else { falseChild }` | Identical |
| **DisplayGrid** | `Column { Row { cells } }` with `weight()` columns | `Grid` (iOS 16+) or `LazyVGrid` with `GridItem(.flexible())` | SwiftUI `Grid` is more natural for fixed-row grids |
| **SectionSlot** | `SectionRouter(section, ...)` | `SectionRouter(section:, ...)` | Identical recursive delegation |

### Layout Modifier Mapping

| Concept | Compose | SwiftUI |
|---|---|---|
| **Background (solid)** | `Modifier.background(parseColor(hex))` | `.background(Color(hex:))` |
| **Background (gradient)** | `Modifier.background(Brush.verticalGradient(colors))` | `.background(LinearGradient(colors:, startPoint: .top, endPoint: .bottom))` |
| **Background (image)** | `AsyncImage` behind content in `Box` | `AsyncImage` in `.background()` or `ZStack` |
| **Padding** | `Modifier.padding(start, end, top, bottom)` | `.padding(EdgeInsets(top:, leading:, bottom:, trailing:))` |
| **Corner radius** | `Modifier.clip(RoundedCornerShape(dp))` | `.clipShape(RoundedRectangle(cornerRadius:))` |
| **Fixed size** | `Modifier.width(dp).height(dp)` | `.frame(width:, height:)` |
| **Fill width** | `Modifier.fillMaxWidth()` | `.frame(maxWidth: .infinity)` |
| **Tap action** | `Modifier.clickable { onAction(action) }` | `.onTapGesture { onAction(action) }` |
| **Accessibility label** | `Modifier.semantics { contentDescription = label }` | `.accessibilityLabel(label)` |
| **Accessibility heading** | `Modifier.semantics { heading() }` | `.accessibilityAddTraits(.isHeader)` |
| **Accessibility hidden** | `Modifier.clearAndSetSemantics {}` | `.accessibilityHidden(true)` |
| **Depth guard** | `if (depth > MAX_DEPTH) { Log.w(); return }` | `if depth > maxDepth { return EmptyView() }` |

### Structural Pattern (Identical on Both Platforms)

The `AtomicRouter` on both platforms follows the same structure:

```
AtomicRouter(element, screenState, onAction, depth):
    if depth > 6: return empty          // Depth guard
    switch element.type:
        Container  → AtomicContainer(element, screenState, onAction, depth)
        Text       → AtomicText(element, ...)
        Image      → AtomicImage(element, ...)
        Button     → AtomicButton(element, ...)
        Spacer     → AtomicSpacer(element, ...)
        Divider    → AtomicDivider(element, ...)
        ScrollContainer → AtomicScrollContainer(element, ..., depth)
        Conditional     → AtomicConditional(element, ..., depth)
        DisplayGrid     → AtomicDisplayGrid(element, ...)
        SectionSlot     → SectionRouter(element.section, ...)
        unknown         → log warning, return empty    // Graceful skip
```

`SectionRouter` follows the same pattern dispatching on `section.type`. The algorithm, routing logic, fallback behavior, and failure semantics are identical — the iOS implementation is a **transliteration** of the Android implementation, not a redesign.

### Key Differences

| Concern | Compose Approach | SwiftUI Approach | Impact |
|---|---|---|---|
| **Proportional flex** | `Modifier.weight(float)` in Row/Column scope — built-in | No built-in equivalent; use `.frame(maxWidth: .infinity)` for equal split, `Layout` protocol for custom ratios | Minor — most SDUI layouts use equal flex or fixed sizing |
| **Vertical divider** | `VerticalDivider()` — first-class composable | No SwiftUI equivalent; use `Rectangle().frame(width: thickness)` | Trivial workaround |
| **Paging carousel** | `HorizontalPager(state, pageCount)` — dedicated component | `TabView(.page)` or `ScrollView` with `.scrollTargetBehavior(.paging)` (iOS 17+) | Functional equivalent, different API surface |
| **Image loading** | Coil `AsyncImage` (third-party) | Native `AsyncImage` (iOS 15+) — no third-party needed | SwiftUI is simpler |
| **State observation** | `MutableStateFlow` + `collectAsState()` | `@Observable` or `@StateObject` + `@Published` | Both reactive; SwiftUI's `@Observable` (iOS 17+) is closer to Compose's snapshot state |
| **Typography mapping** | `MaterialTheme.typography.bodyMedium` etc. | `.font(.body)`, `.font(.headline)` etc. | Different type ramps; mapping table needed per platform |

### Implication for Implementation

The iOS SDUI runtime (~850 LOC) is not a novel design exercise. Every renderer file can be written by reading the corresponding Android Compose renderer and translating the syntax:

- `AtomicContainer.kt` (Compose) → `AtomicContainer.swift` (SwiftUI): `Row` → `HStack`, `Column` → `VStack`, `weight()` → `.frame(maxWidth: .infinity)`, `Modifier.background()` → `.background()`
- `AtomicText.kt` → `AtomicText.swift`: `Text()` → `Text()`, `style = mapVariant()` → `.font(mapVariant())`
- `AtomicImage.kt` → `AtomicImage.swift`: Coil `AsyncImage` → native `AsyncImage`

This is mechanical translation, not architectural decision-making. The architectural decisions are already made and proven in the Android + Web implementations.

---

## 9. Appendix: SDUI Runtime Components Needed Per Platform

### iOS (Build from Scratch — Translated from Android)

| Component | Est. LOC | Dependencies | Android Source Reference |
|---|---|---|---|
| `SduiModels.swift` (codegen) | 2,966 | Already generated — in `ios/Sources/SduiCore/Models/` | `ios/Sources/SduiCore/Models/SduiModels.swift` |
| `SduiRepository.swift` | 60 | URLSession | `SduiRepository.kt` |
| `UriResolver.swift` | 10 | None | `UriResolver.kt` |
| `SduiScreenView.swift` | 80 | SwiftUI | `SduiScreenComposable.kt` |
| `SectionRouter.swift` | 60 | SwiftUI | `SectionRouter.kt` |
| `AtomicRouter.swift` | 40 | SwiftUI | `AtomicRouter.kt` |
| `AtomicContainer.swift` | 120 | SwiftUI (HStack/VStack) | `AtomicContainer.kt` |
| `AtomicText.swift` | 50 | SwiftUI (Text) | `AtomicText.kt` |
| `AtomicImage.swift` | 60 | SwiftUI (AsyncImage) | `AtomicImage.kt` |
| `AtomicButton.swift` | 50 | SwiftUI (Button) | `AtomicButton.kt` |
| `AtomicSpacer.swift` | 15 | SwiftUI (Spacer) | `AtomicSpacer.kt` |
| `AtomicDivider.swift` | 25 | SwiftUI (Divider) | `AtomicDivider.kt` |
| `AtomicScrollContainer.swift` | 50 | SwiftUI (ScrollView/LazyStack) | `AtomicScrollContainer.kt` |
| `AtomicConditional.swift` | 30 | SwiftUI | `AtomicConditional.kt` |
| `AtomicDisplayGrid.swift` | 80 | SwiftUI (Grid) | `AtomicDisplayGrid.kt` |
| `AtomicSectionSlot.swift` | 15 | SwiftUI | `AtomicSectionSlot.kt` |
| `ActionDispatcher.swift` | 80 | Foundation | `ActionHandler.kt` |
| `StateManager.swift` | 50 | SwiftUI (@Observable) | `ScreenStateHolder.kt` |
| `SduiHelpers.swift` | 40 | UIKit (color parsing) | `ColorUtils.kt`, `AccessibilityExt.kt` |
| **Total new runtime** | **~850** | | |

### Android (Import Existing Library)

| Component | Status | Action |
|---|---|---|
| `sdui-core` library | Built | Add as module dependency |
| Integration glue (SduiRepository wiring, NavHost integration) | ~50 LOC | Wire existing library to ref app's Hilt/NavHost |
| `VideoPlayer` section renderer (Gap 1) | Not built | New renderer wrapping existing SDK Composables |
